package com.goeswhere.dmnp.util;

import static com.goeswhere.dmnp.util.ASTContainers.isLoggerType;

import java.util.Set;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;


/** Find the names of all the fields with a type of "Logger". */
public class LoggerFieldFinder extends ASTVisitor {
	private final Set<String> fields;

	/** Populate parameter with the names of all the found Loggers. */
	public LoggerFieldFinder(Set<String> fields) {
		this.fields = fields;
	}

	@SuppressWarnings("unchecked")
	@Override public boolean visit(final FieldDeclaration node) {
		if (isLoggerType(node.getType()))
			for (final VariableDeclarationFragment f :
					(Iterable<VariableDeclarationFragment>)node.fragments())
				fields.add(f.getName().getIdentifier());
		return super.visit(node);
	}
}
