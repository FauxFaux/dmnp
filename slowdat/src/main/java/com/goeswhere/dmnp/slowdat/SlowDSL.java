package com.goeswhere.dmnp.slowdat;

import static com.goeswhere.dmnp.util.FileUtils.consumeFile;
import static com.goeswhere.dmnp.util.FileUtils.writeFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import com.goeswhere.dmnp.util.ASTContainers;
import com.goeswhere.dmnp.util.ASTWrapper;
import com.goeswhere.dmnp.util.ASTWrapper.HadProblems;
import com.goeswhere.dmnp.util.FJava;
import com.goeswhere.dmnp.util.FileUtils;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;


// XXX static {}
public class SlowDSL {

	private static final ImmutableSet<String> BORING_NULLARY_CONSTRUCTORS =
		ImmutableSet.of("java.util.Date",
				"java.util.StringBuilder",
				"java.util.StringBuffer");

	public static void main(String[] args) throws IOException {
		if (3 != args.length) {
			System.err.println("Usage: classpath sourcepath file");
			return;
		}

		final String[] cp = Iterables.toArray(FJava.concatMap(Arrays.asList(sysSplit(args[0])),
				new Function<String, Iterable<String>>() {
					@Override public Iterable<String> apply(String from) {
						if (!from.endsWith("*"))
							return ImmutableList.of(from);
						final String withoutStar = from.substring(0, from.length() - 1);
						return Iterables.transform(FileUtils.filesIn(withoutStar, "jar"), FileUtils.ABSOLUTE_PATH);
					}}), String.class);

		final String[] sourcePath = sysSplit(args[1]);
		final String path = args[2];

		final File f = new File(path);
		for (File file : f.isDirectory() ? FileUtils.javaFilesIn(path) : Arrays.asList(f)) {
			System.out.print("Processing " + file.getName() + ".");
			try {
				fixInPlace(file.getAbsolutePath(), cp, sourcePath);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			System.out.println(".  Done.");
		}
	}

	private static void fixInPlace(final String file, final String[] cp, final String[] sourcePath) throws IOException {
		writeFile(new File(file), new SlowDSL(cp, sourcePath, unitName(file)).go(consumeFile(file)));
	}

	private static String unitName(String path) {
		final String f = new File(path).getName();
		final String ext = ".java";
		Preconditions.checkArgument(f.endsWith(ext));
		return f.substring(0, f.length() - ext.length());
	}

	private static String[] sysSplit(final String string) {
		return string.split(Pattern.quote(File.pathSeparator));
	}

	private static final Function<ITypeBinding, String> TYPEBINDING_QUALIFIED_NAME =
		new Function<ITypeBinding, String>() {
			@Override public String apply(ITypeBinding from) {
				return from.getQualifiedName();
			}
	};

	private final String[] classpath;
	private final String[] sourcepath;
	private final String unitName;

	public SlowDSL(String[] classpath, String[] sourcepath, String unitName) {
		this.classpath = classpath;
		this.sourcepath = sourcepath;
		this.unitName = unitName;
	}

	private CompilationUnit compile(String s) {
		try {
			return ASTWrapper.compile(s, unitName, classpath, sourcepath);
		} catch (HadProblems p) {
			for (IProblem prob : p.cu.getProblems())
				if (!prob.isWarning())
					throw p;
			return p.cu;
		}
	}

	private static boolean doesNothingUseful(final Expression e) {
		if (e instanceof NumberLiteral
				|| e instanceof NullLiteral
				|| e instanceof CharacterLiteral
				|| e instanceof StringLiteral)
			return true;

		if (e instanceof InfixExpression) {
			final InfixExpression inf = (InfixExpression) e;
			return doesNothingUseful(inf.getLeftOperand())
				&& doesNothingUseful(inf.getRightOperand());
		}

		if (e instanceof ClassInstanceCreation) {
			final ClassInstanceCreation cic = (ClassInstanceCreation) e;
			return cic.arguments().isEmpty()
					&& BORING_NULLARY_CONSTRUCTORS.contains(
							cic.getType().resolveBinding().getQualifiedName());
		}

		// Qualified names are a bit more scary,
		// but the majority of the annoying ones should be caught by
		// them being constant expressions; caught by the clause below
		if (e instanceof SimpleName)
			return true;

		if (null != e.resolveConstantExpressionValue())
			return true;

		return false;
	}

	private String go(final String src) {
		return go(src, Collections.<String>emptySet());
	}

	private static class InitialiserStripper extends ASTVisitor {
		private final Set<String> skips;

		private InitialiserStripper(Set<String> skipWhat) {
			this.skips = ImmutableSet.copyOf(skipWhat);
		}

		@Override public boolean visit(VariableDeclarationStatement node) {
			for (VariableDeclarationFragment a : ASTContainers.it(node)) {
				final Expression i = a.getInitializer();
				if (null == i)
					continue;

				if (doesNothingUseful(i)) {
					final IVariableBinding bi = a.resolveBinding();
					if (skips.contains(strignature(bi)))
						continue;
					a.setInitializer(null);
				}
			}
			return true;
		}
	}

	private final Set<Set<String>> seenBrokens = Sets.newHashSet();

	private String go(final String origsrc, final Set<String> skip) {
		final String newsrc = mutilateSource(origsrc, skip);

		System.out.print(",");

		final Set<String> currentlyBroken = Sets.newHashSet();

		try {
			compile(newsrc);
			return newsrc;
		} catch (HadProblems p) {
			final SetMultimap<String, Integer> e = HashMultimap.create();
			for (IProblem a : p.cu.getProblems()) {
				if (!a.isWarning() && IProblem.UninitializedLocalVariable != a.getID())
					throw p;
				e.put(a.getArguments()[0], a.getSourceStart());
			}

			p.cu.accept(new ASTVisitor() {
				@Override public boolean visit(SimpleName node) {
					if (e.get(node.getIdentifier()).remove(node.getStartPosition())) {
						final IVariableBinding vb = (IVariableBinding) node.resolveBinding();
						currentlyBroken.add(strignature(vb));
					}
					return true;
				}
			});
		}

		System.out.print(".");

		if (seenBrokens.contains(currentlyBroken))
			throw new RuntimeException("Didn't work, still have " + currentlyBroken);

		seenBrokens.add(currentlyBroken);

		return go(origsrc, ImmutableSet.copyOf(currentlyBroken));
	}

	private String mutilateSource(final String src, final Set<String> skip) {
		final CompilationUnit cu = compile(src);
		cu.recordModifications();
		cu.accept(new InitialiserStripper(skip));
		return rewrite(src, cu);
	}

	private String rewrite(final String src, final CompilationUnit changes) {
		final Document doc = new Document(src);
		try {
			changes.rewrite(doc, null).apply(doc, TextEdit.UPDATE_REGIONS);
		} catch (MalformedTreeException e) {
			throw new RuntimeException(e);
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
		final String d = doc.get();
		return d;
	}

	private static String strignature(final IVariableBinding vb) {
		return vb.getName() + ":" + vb.getVariableId() + ":" + signature(vb.getDeclaringMethod());
	}

	private static String signature(IMethodBinding from) {
		return from.getDeclaringClass().getName() + "#" + from.getName()
			+ Lists.transform(Arrays.asList(from.getParameterTypes()), TYPEBINDING_QUALIFIED_NAME);
	}
}
