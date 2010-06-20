package com.goeswhere.dmnp.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;


public class ASTContainers {

	@SuppressWarnings("unchecked")
	public static List<SingleVariableDeclaration> it(MethodDeclaration d) {
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
	public static List<VariableDeclarationFragment> it(VariableDeclarationStatement s) {
		return s.fragments();
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

	@SuppressWarnings("unchecked")
	public static List<Expression> it(InfixExpression infixExpression) {
		return infixExpression.extendedOperands();
	}

	@SuppressWarnings("unchecked")
	public static List<IExtendedModifier> modifiers(BodyDeclaration b) {
		return b.modifiers();
	}

	@SuppressWarnings("unchecked")
	public static List<IExtendedModifier> modifiers(VariableDeclarationStatement b) {
		return b.modifiers();
	}

	/** Examine the statements between two statements' ancestors,
	 * as defined by {@link #sharedParent(ASTNode, ASTNode)}.
	 *
	 * e.g. <pre> int tis; int bar;
	 * try { foo(); }
	 * finally { int baz; int that; }</pre>
	 *
	 * ... for {@code tis} and {@code that} examines just {@code int bar;}.
	 * */
	public static Iterable<Statement> statementsBetweenFlat(ASTNode one, ASTNode two) {
		final Pair<ASTNode, ASTNode> parents = sharedParent(one, two);
		final List<Statement> lis = ASTContainers.it((Block) clean(parents.t).getParent());
		return BetweenIterable.of(lis, (Statement)parents.t, (Statement)parents.u);
	}

	private static ASTNode clean(ASTNode t) {
		return t.getParent() instanceof IfStatement ? clean(t.getParent()) : t;
	}


	/** Find {@code pair(a,b)} such that {@code a.getParent() == b.getParent()} and:
	 * @param one is, or has an ancestor of, {@code a}
	 * @param two is, or has an ancestor of, {@code b}
	 * @throws IllegalArgumentException if a pair can't be found.
	 */
	public static Pair<ASTNode, ASTNode> sharedParent(ASTNode one, ASTNode two) {
		if (one.getParent().equals(two.getParent()))
			return Pair.of(one, two);
		else
			return sharedParentDeep(one, two);
	}

	private static Pair<ASTNode, ASTNode> sharedParentDeep(ASTNode one, ASTNode two) {
		final List<ASTNode> tree = new ArrayList<ASTNode>();
		ASTNode oee = one;
		tree.add(oee);
		while (null != oee)
			tree.add(oee = oee.getParent());
		ASTNode tee = two;
		while (null != tee) {
			final ASTNode tmp = tee.getParent();
			final int idx = tree.indexOf(tmp);
			if (0 == idx)
				return Pair.of(tmp, tmp);
			if (-1 != idx)
				return Pair.of(tree.get(idx - 1), tee);
			tee = tmp;
		}

		throw new IllegalArgumentException("Nodes don't share a parent! "
				+ Containers.classAndToString(oee) + " // "
				+ Containers.classAndToString(tee));
	}

	/** Any {@link SimpleType} named "Logger" */
	public static boolean isLoggerType(Type type) {
		return type instanceof SimpleType
			&& compareIfSimpleNode("Logger", ((SimpleType)type).getName());
	}

	public static Set<String> loggers(ASTNode n) {
		final Set<String> ret = new HashSet<String>();
		n.accept(new LoggerFieldFinder(ret));
		return ret;
	}

	public static boolean compareIfSimpleNode(String name, ASTNode node) {
		return node instanceof SimpleName
			&& name.equals(((SimpleName) node).getIdentifier());
	}

	@SuppressWarnings("unchecked")
	public static List<Statement> statements(SwitchStatement ss) {
		return ss.statements();
	}

	@SuppressWarnings("unchecked")
	public static List<Comment> comments(CompilationUnit cu) {
		return cu.getCommentList();
	}
}
