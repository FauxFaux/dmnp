import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.ThrowStatement;

public class UsefulExceptionChecker {

	static AtomicLong count = new AtomicLong();

	/** Visit a block and check it does something useful with the exception named in the constructor arguments. */
	static class UsefullyUsesException extends ASTVisitor {
		private boolean useful = false;
		private final String exceptionName;

		UsefullyUsesException(final String name) {
			this.exceptionName = name;
		}

		/** It is re-thrown, either directly (throw ex) or as an argument
		 *  to a new exception (throw new RuntimeException("bar", ex); )*/
		@Override public boolean visit(ThrowStatement ts) {
			final Expression exp = ts.getExpression();

			for (Expression o : iterateIfClassInstanceCreation(exp))
				setIfMatches(o);

			setIfMatches(exp);

			return true;
		}

		/** It's passed to the second argument of a reasonable level of log4j warning. */
		@SuppressWarnings("unchecked")
		@Override public boolean visit(MethodInvocation mi) {
			final List<Expression> arguments = mi.arguments();
			if (mi.getExpression() instanceof SimpleName
					&& ((SimpleName) mi.getExpression()).getIdentifier().equalsIgnoreCase("logger")
					&& isAcceptableWarnLevel(mi.getName().getIdentifier())
					&& 2 == arguments.size()) {

				setIfMatches(arguments.get(1));
			}
			return true;
		}

		/** The visitor has found something useful to date. */
		public boolean isUseful() {
			return useful;
		}

		/** .fatal(), .error() or .warn() */
		private static boolean isAcceptableWarnLevel(final String identifier) {
			return identifier.equals("fatal") || identifier.equals("error") || identifier.equals("warn");
		}

		/** If the expression is a name equal to the one we're looking for */
		void setIfMatches(final Expression exp) {
			if (exp instanceof SimpleName)
				if (exceptionName.equals(((SimpleName) exp).getIdentifier()))
					useful = true;
		}

		/** Helper to go over the arguments of a new Instance()'s arguments, if it is an instantiation */
		@SuppressWarnings("unchecked")
		private static Iterable<Expression> iterateIfClassInstanceCreation(final Expression exp) {
			if (exp instanceof ClassInstanceCreation) {
				return ((ClassInstanceCreation)exp).arguments();
			}
			return Collections.emptyList();
		}

	}

	/** Where the results go */
	static interface Reporter {
		public void report(final String name, final CatchClause cc);
	}

	/** Run {@link #UsefulExceptionChecker(Reporter)} on each catch clause. */
	static class VisitCatchClauses extends ASTVisitor {
		private final Reporter reporter;

		public VisitCatchClauses(final Reporter reporter) {
			this.reporter = reporter;
		}

		@Override public boolean visit(final CatchClause cc) {
			final String name = cc.getException().getName().getIdentifier();
			final UsefullyUsesException uue = new UsefullyUsesException(name);
			cc.accept(uue);
			if (!uue.isUseful())
				reporter.report(name, cc);
			return true;
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("Usage: dir1 [dir2...]");
			System.exit(-1);
		} else {
			for (String arg : args) {
				processDir(new File(arg));
			}
		}

		System.out.println();
		System.out.println(count + " instances total");
	}

	private static void processDir(File dir) throws IOException {
		for (File child : dir.listFiles())
			if (!child.getName().equals(".") && !child.getName().equals(".."))
				if (child.isDirectory())
					processDir(child);
				else if (child.getName().endsWith(".java"))
					processFile(child.getPath());
	}

	/** Entire contents of the file in a String */
	private static String consumeFile(final String filename) throws IOException {
		final int block = 1024 * 10;
		final StringBuilder fileData = new StringBuilder(block);
		final FileReader fileReader = new FileReader(filename);
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

	static void processFile(final String filename) throws IOException {
		processString(new File(filename).getName(), consumeFile(filename));
	}

	private static void processString(final String filename, final String contents) {
		final CompilationUnit cu = compile(contents);
		cu.accept(new VisitCatchClauses(new Reporter() {

			@Override public void report(String name, CatchClause cc) {
				System.out.println(name + " unused at (" + filename + ":" + cu.getLineNumber(cc.getStartPosition()) + ")");
				count.incrementAndGet();
			}
		}));
	}

	/** Return the Java 5 CU for the string. */
	private static CompilationUnit compile(String c) {
		final ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(c.toCharArray());
		return (CompilationUnit) parser.createAST(null);
	}
}
