package com.goeswhere.dmnp.ue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Type;

/** Run {@link UsefullyUsesException} on each catch clause. */
class VisitCatchClauses extends ASTVisitor {

	private final Reporter reporter;
	private final Set<String> loggerNames;

	private VisitCatchClauses(final Reporter reporter, final Set<String> loggerNames) {
		this.reporter = reporter;
		this.loggerNames = loggerNames;
	}

	/** Accept a {@link UsefullyUsesException} and {@link Reporter} on it's result. */
	@Override public boolean visit(final CatchClause cc) {
		final String name = cc.getException().getName().getIdentifier();
		final UsefullyUsesException uue = new UsefullyUsesException(name, loggerNames);
		cc.accept(uue);
		if (!uue.isUseful())
			reporter.report(cc);
		return true;
	}

	static void accept(final CompilationUnit cu, final Reporter rep) {
		final Set<String> fields = new HashSet<String>();
		cu.accept(new LoggerFieldFinder(fields));
		cu.accept(new VisitCatchClauses(rep, Collections.unmodifiableSet(fields)));
	}

	/** Any {@link SimpleType} named "Logger" */
	static boolean isLoggerType(Type type) {
		return type instanceof SimpleType
			&& UsefullyUsesException.compareIfSimpleNode("Logger", ((SimpleType)type).getName());
	}
}
