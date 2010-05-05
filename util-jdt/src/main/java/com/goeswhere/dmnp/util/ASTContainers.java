package com.goeswhere.dmnp.util;

import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class ASTContainers {

	@SuppressWarnings("unchecked")
	public static Iterable<SingleVariableDeclaration> it(MethodDeclaration d) {
		return d.parameters();
	}

	@SuppressWarnings("unchecked")
	public static List<Statement> it(Block d) {
		return d.statements();
	}

	@SuppressWarnings("unchecked")
	public static Iterable<Expression> it(ForStatement s) {
		return s.initializers();
	}

	@SuppressWarnings("unchecked")
	public static Iterable<Expression> it(SuperConstructorInvocation d) {
		return d.arguments();
	}

	@SuppressWarnings("unchecked")
	public static Iterable<Expression> it(ConstructorInvocation d) {
		return d.arguments();
	}

	@SuppressWarnings("unchecked")
	public static Iterable<CatchClause> it(TryStatement s) {
		return s.catchClauses();
	}

	@SuppressWarnings("unchecked")
	public static Iterable<VariableDeclarationFragment> it(VariableDeclarationStatement s) {
		return s.fragments();
	}

	@SuppressWarnings("unchecked")
	public static Iterable<BodyDeclaration> it(AbstractTypeDeclaration s) {
		return s.bodyDeclarations();
	}

	@SuppressWarnings("unchecked")
	public static Iterable<VariableDeclarationFragment> it(VariableDeclarationExpression e) {
		return e.fragments();
	}

	@SuppressWarnings("unchecked")
	public static List<AbstractTypeDeclaration> types(CompilationUnit c) {
		return c.types();
	}

	@SuppressWarnings("unchecked")
	public static List<BodyDeclaration> bodyDeclarations(AbstractTypeDeclaration atd) {
		return atd.bodyDeclarations();
	}

	@SuppressWarnings("unchecked")
	public static List<Expression> arguments(MethodInvocation mi) {
		return mi.arguments();
	}

	@SuppressWarnings("unchecked")
	public static List<ASTNode> fragments(final TagElement te) {
		return te.fragments();
	}

	@SuppressWarnings("unchecked")
	public static List<TagElement> tags(Javadoc javadoc) {
		return javadoc.tags();
	}

	@SuppressWarnings("unchecked")
	public static List<VariableDeclarationFragment> it(FieldDeclaration node) {
		return node.fragments();
	}
}
