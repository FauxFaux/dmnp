package com.goeswhere.dmnp.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;


/** Various utilities for converting source to one of the object representations. */
public class ASTWrapper {

	private ASTWrapper() {
		// statics only
	}

	/** Return the Java 5 CU for the string. */
	 public static CompilationUnit compile(String c) {
		final ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(c.toCharArray());

		@SuppressWarnings("unchecked")
		final Map<String, String> ops =
			new HashMap<String, String>(JavaCore.getOptions());

		ops.put(JavaCore.COMPILER_COMPLIANCE, "1.6");
		ops.put(JavaCore.COMPILER_SOURCE, "1.6");

		parser.setCompilerOptions(ops);

		return compile(parser);
	}

	private static CompilationUnit compile(final ASTParser parser) {
		final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
		if (cu.getProblems().length != 0)
			throw new IllegalArgumentException(Arrays.toString(cu.getProblems()));
		return cu;
	}

	/** Return the Java 5 CU for the string. */
	public static CompilationUnit compile(String c, String[] classpath, String[] source) {
		final ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(c.toCharArray());
		parser.setResolveBindings(true);
		parser.setUnitName("ThisIsApparentlyIrrelevantButCompulsory");
		parser.setEnvironment(classpath, source, null, true);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);
		return compile(parser);
	}


	/** Full signature of a method, including annotations, modifiers, name, parameters */
	public static String signature(final MethodDeclaration d) {
		return FJava.intersperse(d.modifiers(), " ")
			+ " " + d.getReturnType2()
			+ " " + d.getName().getIdentifier()
			+ "(" + FJava.intersperse(d.parameters(), ", ") + ")";
	}


	public static MethodDeclaration extractSingleMethod(final String classBody) {
		CompilationUnit cu = compile("class A { " + classBody + " }");
		final Mutable<MethodDeclaration> mut = new Mutable<MethodDeclaration>();
		cu.accept(new ASTVisitor() {
			@Override public boolean visit(MethodDeclaration m) {
				final MethodDeclaration old = mut.get();
				if (null != old)
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
}
