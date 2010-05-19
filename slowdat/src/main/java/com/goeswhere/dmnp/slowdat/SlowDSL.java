package com.goeswhere.dmnp.slowdat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

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
import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;


// XXX static {}
public class SlowDSL {

	private static final ImmutableSet<String> BORING_NULLARY_CONSTRUCTORS =
		ImmutableSet.of("java.util.Date");

	public static void main(String[] args) {
		System.out.println(new SlowDSL(args, args).go(
				"import java.util.Date;\n" +
				"class Const {\n" +
				" public static final String HATE = \"I hate this\";\n" +
				"}\n" +
				"class A {\n" +
				" void foo() {\n" +
				"  int a = 0;\n" +
				"  long b = 7;\n" + // dead store
				"  String c = \"hi\" + \"ho\";\n" + // dead store
				"  Date d = new Date();\n" + // dead store
				"  String e = \"add\" + Const.HATE;\n" + // dead store
				"  e = \"\";\n" +
				"  e.hashCode();\n" +
				"  String f = e;\n" + // dead store
				"  f = \"\";\n" +
				"  f.hashCode();\n" +
				"  d = new Date();\n" +
				"  b = d.getTime();\n" +
				"  System.out.println(a + b + c);\n" +
				"}}"));
		/**
		 * class A {
		 *  void foo() {
		 *  int a = 0;
		 *  int b;
		 *  b = 5;
		 *  System.out.println(a + b);
		 * }}
		 */
	}

	private static final Function<ITypeBinding, String> TYPEBINDING_QUALIFIED_NAME =
		new Function<ITypeBinding, String>() {
			@Override public String apply(ITypeBinding from) {
				return from.getQualifiedName();
			}
	};

	private final String[] classpath;
	private final String[] sourcepath;

	public SlowDSL(String[] classpath, String[] sourcepath) {
		this.classpath = classpath;
		this.sourcepath = sourcepath;
	}

	private CompilationUnit compile(String s) {
		return ASTWrapper.compile(s, classpath, sourcepath);
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

	private String go(final String origsrc, final Set<String> skip) {
		final String newsrc = mutilateSource(origsrc, skip);

		final Set<String> currentlyBroken = Sets.newHashSet();

		try {
			compile(newsrc);
			return newsrc;
		} catch (HadProblems p) {
			final SetMultimap<String, Integer> e = HashMultimap.create();
			for (IProblem a : p.cu.getProblems()) {
				if (IProblem.UninitializedLocalVariable != a.getID())
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

		if (currentlyBroken.equals(skip))
			throw new RuntimeException("Didn't work, still have " + currentlyBroken);

		return go(origsrc, ImmutableSet.copyOf(currentlyBroken));
	}

	private String mutilateSource(final String src, final Set<String> skip) {
		final CompilationUnit cu = compile(src);
		cu.recordModifications();
		cu.accept(new InitialiserStripper(skip));
		final String d = rewrite(src, cu);
		return d;
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
		return signature(vb.getDeclaringMethod()) + ":" + vb.getVariableId();
	}

	private static String signature(IMethodBinding from) {
		return from.getDeclaringClass().getName() + "#" + from.getName()
			+ Lists.transform(Arrays.asList(from.getParameterTypes()), TYPEBINDING_QUALIFIED_NAME);
	}
}
