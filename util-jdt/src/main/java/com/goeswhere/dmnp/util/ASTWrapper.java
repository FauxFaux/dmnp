package com.goeswhere.dmnp.util;

import static com.goeswhere.dmnp.util.ASTContainers.extendedOperands;
import static com.goeswhere.dmnp.util.ASTContainers.fragments;
import static com.goeswhere.dmnp.util.ASTContainers.statements;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/** Various utilities for converting source to one of the object representations. */
public class ASTWrapper {

	private ASTWrapper() {
		throw new AssertionError();
	}

	private static final ImmutableSet<String> BORING_NULLARY_CONSTRUCTORS =
		ImmutableSet.of("java.util.Date",
				"java.util.StringBuilder",
				"java.util.StringBuffer");


	/** Return the Java 5 CU for the string. */
	 public static CompilationUnit compile(String c) {
		return compile(c, null, null);
	}

	private static ASTParser getParser(String c) {
		final ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(c.toCharArray());

		final Map<String, String> ops =
			new HashMap<String, String>(javaCoreOptions());

		for (Entry<String, String> a : ops.entrySet())
				if ("warning".equals(a.getValue()))
					a.setValue("ignore");

		ops.put(JavaCore.COMPILER_COMPLIANCE, "1.6");
		ops.put(JavaCore.COMPILER_SOURCE, "1.6");
		ops.put(JavaCore.COMPILER_PB_MAX_PER_UNIT, "0");

		parser.setCompilerOptions(ops);
		return parser;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> javaCoreOptions() {
		return JavaCore.getOptions();
	}

	/** This is a bogglingly ugly hack.  On compiler-generated errors, the compile
	 * method will throw this, <b>which contains it's return value</b>. */
	public static class HadProblems extends IllegalArgumentException {
		public final CompilationUnit cu;

		public HadProblems(CompilationUnit cu) {
			super("Compile had problems: " + Arrays.toString(cu.getProblems()));
			this.cu = cu;
		}
	}

	private static CompilationUnit compile(final ASTParser parser) throws HadProblems {
		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		if (cu.getProblems().length != 0)
			throw new HadProblems(cu);
		return cu;
	}

	/** Return the Java 5 CU for the string, with bindings and shiz. */
	public static CompilationUnit compile(String src, String[] classpath, String[] source) {
		return compile(src, "OnlyNecessaryForPublicClasses", classpath, source);
	}

	/** If classpath == source == null, don't resolve bindings. Also ignores filename. */
	public static CompilationUnit compile(String src, String filename, String[] classpath, String[] source) {
		final ASTParser parser = getParser(src);
		if (null != classpath && null != source) {
			parser.setResolveBindings(true);
			parser.setUnitName(filename);
			parser.setEnvironment(classpath, source, null, true);
			parser.setBindingsRecovery(true);
			parser.setStatementsRecovery(true);
		}
		return compile(parser);
	}


	/** Full signature of a method, including annotations, modifiers, name, parameters */
	public static String signature(final MethodDeclaration d) {
		// null on constructors etc.
		final Type rt = d.getReturnType2();
		return FJava.intersperse(d.modifiers(), " ")
			+ (null != rt ? " " + rt : "")
			+ " " + d.getName().getIdentifier()
			+ "(" + FJava.intersperse(d.parameters(), ", ") + ")";
	}


	public static MethodDeclaration extractSingleMethod(final String classBody) {
		return extractSingleMethod(classBody, null, null);
	}

	public static MethodDeclaration extractSingleMethod(final String classBody, String[] classpath, String[] source) {
		return extractMethodImpl(classBody, classpath, source, ExtractMethodMode.SINGLE);
	}

	private static enum ExtractMethodMode {
		SINGLE, LAST
	}

	private static MethodDeclaration extractMethodImpl(final String classBody,
			String[] classpath, String[] source, final ExtractMethodMode single) {
		CompilationUnit cu = compile("class A { " + classBody + " }", classpath, source);
		final Mutable<MethodDeclaration> mut = new Mutable<MethodDeclaration>();
		cu.accept(new ASTVisitor() {
			@Override public boolean visit(MethodDeclaration m) {
				final MethodDeclaration old = mut.get();
				if (single == ExtractMethodMode.SINGLE && null != old)
					throw new IllegalArgumentException(classBody + " contains at least " + old + " and " + m);
				mut.set(m);
				return super.visit(m);
			}
		});

		final MethodDeclaration ret = mut.get();
		if (null == ret)
			throw new IllegalArgumentException(classBody + " should contain only one method");

		return ret;
	}

	public static MethodDeclaration extractLastMethod(final String classBody, String[] classpath, String[] source) {
		return extractMethodImpl(classBody, classpath, source, ExtractMethodMode.LAST);
	}

	public static MethodDeclaration extractLastMethod(final String classBody) {
		return extractLastMethod(classBody, null, null);
	}

	public static String methodName(ASTNode cc) {
		ASTNode n = cc;
		while (n != null && !(n instanceof MethodDeclaration))
			n = n.getParent();

		return n != null
				? ASTWrapper.signature((MethodDeclaration) n)
				: "[unknown method]";
	}


	public static String rewrite(final String src, final CompilationUnit changes) {
		return rewrite(new Document(src), changes);
	}

	public static String rewrite(final Document doc, final CompilationUnit changes) {
		try {
			changes.rewrite(doc, null).apply(doc, TextEdit.UPDATE_REGIONS);
		} catch (MalformedTreeException e) {
			throw new RuntimeException(e);
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
		return doc.get();
	}

	public static boolean returnsVoid(MethodDeclaration node) {
		final Type rt = node.getReturnType2();
		return rt instanceof PrimitiveType
			&& PrimitiveType.VOID.equals(((PrimitiveType) rt).getPrimitiveTypeCode());
	}

	/** Removes a fragment from its Statement.
	 * If this leaves the Statement empty, remove the Statement from <b>its</b> parent. */
	public static void removeFragment(final VariableDeclarationFragment vdf) {
		final VariableDeclarationStatement vds = (VariableDeclarationStatement) vdf.getParent();
		final List<VariableDeclarationFragment> fra = fragments(vds);
		if (fra.size() == 1)
			removeFromParent(vds);
		fra.remove(vdf);
	}

	/** Remove a Statement from the Block that is its parent. */
	public static boolean removeFromParent(final Statement s) {
		return statements((Block)s.getParent()).remove(s);
	}

	public static boolean doesNothingUseful(final Expression e) {
		if (e instanceof NumberLiteral
				|| e instanceof NullLiteral
				|| e instanceof CharacterLiteral
				|| e instanceof StringLiteral)
			return true;

		if (e instanceof InfixExpression) {
			final InfixExpression inf = (InfixExpression) e;
			return doesNothingUseful(inf.getLeftOperand())
				&& doesNothingUseful(inf.getRightOperand())
				&& Iterables.isEmpty(Iterables.filter(extendedOperands(inf), new Predicate<Expression>() {
					@Override public boolean apply(Expression input) {
						return !doesNothingUseful(input);
					}
				}));
		}

		if (e instanceof ClassInstanceCreation) {
			final ClassInstanceCreation cic = (ClassInstanceCreation) e;
			if (!cic.arguments().isEmpty())
				return false;

			final ITypeBinding rb = cic.getType().resolveBinding();
			return null != rb && BORING_NULLARY_CONSTRUCTORS.contains(rb.getQualifiedName());
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
}
