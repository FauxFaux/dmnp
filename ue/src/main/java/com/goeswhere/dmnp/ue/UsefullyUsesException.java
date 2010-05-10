package com.goeswhere.dmnp.ue;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;

/** Visit a block and check it does something useful with the exception named in the constructor arguments. */
class UsefullyUsesException extends ASTVisitor {
	/** Something useful is done with the named exception /somewhere/. */
	private boolean useful = false;

	/** At some level of nesting, only one branch of an if uses the exception properly. */
	private boolean notOnAllIfPaths = false;

	/** The name of the local variable we're observing. */
	private final String exceptionName;

	private final Set<String> loggerNames;

	UsefullyUsesException(final String name, Set<String> loggerNames) {
		this.exceptionName = name;
		this.loggerNames = loggerNames;
	}

	/** It is re-thrown, either directly (throw ex) or as an argument
	 *  to a new exception (throw new RuntimeException("bar", ex); )*/
	@Override public boolean visit(ThrowStatement ts) {
		final Expression exp = ts.getExpression();

		for (Expression o : iterateIfClassInstanceCreation(exp))
			setIfMatches(o);

		setIfMatches(exp);

		return super.visit(ts);
	}

	/** It's passed to the second argument of a reasonable level of log4j warning. */
	@SuppressWarnings("unchecked")
	@Override public boolean visit(MethodInvocation mi) {
		final List<Expression> arguments = mi.arguments();
		if (mi.getExpression() instanceof SimpleName
				&& loggerNames.contains(((SimpleName) mi.getExpression()).getIdentifier())
				&& isAcceptableWarnLevel(mi.getName().getIdentifier())
				&& 2 == arguments.size()) {

			setIfMatches(arguments.get(1));
		}
		return super.visit(mi);
	}

	/** Poor man's CFG!  Mark the class as failed if only one branch of an if processes the exception properly. */
	@Override public boolean visit(IfStatement node) {
		final Statement elseS = node.getElseStatement();
		// Either the "then" statement doesn't do anything useful, or the else statement also doesn't.
		if (!usefullyUsesName(node.getThenStatement()) ^ (null == elseS || !usefullyUsesName(elseS))) {
			notOnAllIfPaths = true;
		}
		return super.visit(node);
	}

	/** Helper for recursive calls on a node.
	 *
	 * This is slightly inefficient as we're already visiting the code we visit (possibly multiple times)...
	 */
	private boolean usefullyUsesName(Statement statement) {
		final UsefullyUsesException u = new UsefullyUsesException(exceptionName, loggerNames);
		statement.accept(u);
		return u.isUseful();
	}

	/** The visitor has found something useful to date. */
	public boolean isUseful() {
		return useful && !notOnAllIfPaths;
	}

	/** .fatal(), .error() or .warn() */
	private static boolean isAcceptableWarnLevel(final String identifier) {
		return identifier.equals("fatal") || identifier.equals("error") || identifier.equals("warn");
	}

	static boolean compareIfSimpleNode(String name, ASTNode node) {
		return node instanceof SimpleName
			&& name.equals(((SimpleName) node).getIdentifier());
	}

	/** If the expression is a name equal to the one we're looking for */
	void setIfMatches(final Expression exp) {
		if (compareIfSimpleNode(exceptionName, exp))
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
