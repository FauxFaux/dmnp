package com.goeswhere.dmnp.util;

import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

import javax.tools.SimpleJavaFileObject;

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

	/** Entire contents of the file in a String */
	public static String consumeFile(final String filename) throws IOException {
		return consumeFile(new FileReader(filename));
	}

	/** CLOSES THE FILEREADER */
	public static String consumeFile(final FileReader fileReader) throws IOException {
		final int block = 1024 * 10;
		final StringBuilder fileData = new StringBuilder(block);
		try {
			final BufferedReader reader = new BufferedReader(fileReader);
			try {
				char[] buf = new char[block];
				int numRead = 0;
				while ((numRead = reader.read(buf)) != -1) {
					fileData.append(buf, 0, numRead);
				}
			} finally {
				reader.close();
			}
		} finally {
			fileReader.close();
		}
		return fileData.toString();
	}

	/** Return the Java 5 CU for the string. */
	public static CompilationUnit compile(String c) {
		final ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(c.toCharArray());
		return (CompilationUnit) parser.createAST(null);
	}

	/** Return the Java 5 CU for the string. */
	public static CompilationUnit compile(String c, String[] classpath, String[] source) {
		final ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(c.toCharArray());
		parser.setResolveBindings(true);
		parser.setUnitName("PENIS");
		parser.setEnvironment(classpath, source, null, true);
		parser.setBindingsRecovery(true);
		parser.setStatementsRecovery(true);
		return (CompilationUnit) parser.createAST(null);
	}


	/** Full signature of a method, including annotations, modifiers, name, parameters */
	public static String signature(final MethodDeclaration d) {
		return join(d.modifiers(), " ")
			+ " " + d.getReturnType2()
			+ " " + d.getName().getIdentifier()
			+ "(" + join(d.parameters(), ", ") + ")";
	}

	/** sep = "|" gives: [] = "", [a] = "a", [a,b,c] = "a|b|c". */
	private static String join(Iterable<?> lst, String sep) {
		final Iterator<?> it = lst.iterator();
		if (!it.hasNext())
			return "";

		final StringBuilder sb = new StringBuilder();
		sb.append(it.next());
		while (it.hasNext())
			sb.append(sep).append(it.next());
		return sb.toString();
	}

	private static void recursivelyDelete(final File dir) {
		for (File fi : dir.listFiles())
			if (fi.isDirectory())
				recursivelyDelete(fi);
			else
				logDelete(fi);
		logDelete(dir);
	}

	private static void logDelete(File fi) {
		if (!fi.delete())
			System.err.println("couldn't delete " + fi);
	}

	private static class JavaSourceFromString extends SimpleJavaFileObject {
		final String code;

		JavaSourceFromString(String name, String code) {
			super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
			this.code = code;
		}

		@Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return code;
		}
	}

	public static MethodDeclaration extractSingleMethod(String classBody) {
		CompilationUnit cu = compile("class A { " + classBody + " }");
		final Mutable<MethodDeclaration> mut = new Mutable<MethodDeclaration>();
		cu.accept(new ASTVisitor() {
			@Override public boolean visit(MethodDeclaration m) {
				assertNull(mut.get());
				mut.set(m);
				return super.visit(m);
			}
		});

		return mut.get();
	}
}
