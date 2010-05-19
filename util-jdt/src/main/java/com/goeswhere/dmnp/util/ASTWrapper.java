package com.goeswhere.dmnp.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;


/** Various utilities for converting source to one of the object representations. */
public class ASTWrapper {

	private ASTWrapper() {
		throw new AssertionError();
	}

	/** Return the Java 5 CU for the string. */
	 public static CompilationUnit compile(String c) {
		return compile(c, null, null);
	}

	private static ASTParser getParser(String c) {
		final ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(c.toCharArray());

		@SuppressWarnings("unchecked")
		final Map<String, String> ops =
			new HashMap<String, String>(JavaCore.getOptions());

		ops.put(JavaCore.COMPILER_COMPLIANCE, "1.6");
		ops.put(JavaCore.COMPILER_SOURCE, "1.6");

		parser.setCompilerOptions(ops);
		return parser;
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
	public static CompilationUnit compile(String c, String[] classpath, String[] source) {
		final ASTParser parser = getParser(c);
		if (null != classpath && null != source) {
			parser.setResolveBindings(true);
			parser.setUnitName("ThisIsApparentlyIrrelevantButCompulsory");
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
}
