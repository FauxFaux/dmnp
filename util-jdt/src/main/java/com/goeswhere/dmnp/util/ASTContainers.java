package com.goeswhere.dmnp.util;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ASTContainers {

    @SuppressWarnings("unchecked")
    public static List<SingleVariableDeclaration> parameters(MethodDeclaration d) {
        return d.parameters();
    }

    @SuppressWarnings("unchecked")
    public static List<Statement> statements(Block d) {
        return d.statements();
    }

    @SuppressWarnings("unchecked")
    public static Iterable<Expression> initializers(ForStatement s) {
        return s.initializers();
    }

    @SuppressWarnings("unchecked")
    public static Iterable<Expression> arguments(SuperConstructorInvocation d) {
        return d.arguments();
    }

    @SuppressWarnings("unchecked")
    public static Iterable<Expression> arguments(ConstructorInvocation d) {
        return d.arguments();
    }

    @SuppressWarnings("unchecked")
    public static List<Expression> arguments(ClassInstanceCreation cic) {
        return cic.arguments();
    }

    public static List<SingleVariableDeclaration> arguments(MethodDeclaration md) {
        return parameters(md);
    }

    @SuppressWarnings("unchecked")
    public static Iterable<CatchClause> catchClauses(TryStatement s) {
        return s.catchClauses();
    }

    @SuppressWarnings("unchecked")
    public static List<VariableDeclarationFragment> fragments(VariableDeclarationStatement s) {
        return s.fragments();
    }

    @SuppressWarnings("unchecked")
    public static Iterable<VariableDeclarationFragment> fragments(VariableDeclarationExpression e) {
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
    public static List<VariableDeclarationFragment> fragments(FieldDeclaration node) {
        return node.fragments();
    }

    @SuppressWarnings("unchecked")
    public static List<IExtendedModifier> modifiers(BodyDeclaration b) {
        return b.modifiers();
    }

    @SuppressWarnings("unchecked")
    public static List<IExtendedModifier> modifiers(VariableDeclarationStatement b) {
        return b.modifiers();
    }

    @SuppressWarnings("unchecked")
    public static List<Type> superInterfaceTypes(TypeDeclaration td) {
        return td.superInterfaceTypes();
    }

    @SuppressWarnings("unchecked")
    public static List<TypeParameter> typeParameters(TypeDeclaration parent) {
        return parent.typeParameters();
    }

    /**
     * Examine the statements between two statements' ancestors,
     * as defined by {@link #sharedParent(ASTNode, ASTNode)}.
     * <p>
     * e.g. <pre> int tis; int bar;
     * try { foo(); }
     * finally { int baz; int that; }</pre>
     * <p>
     * ... for {@code tis} and {@code that} examines just {@code int bar;}.
     */
    public static Iterable<Statement> statementsBetweenFlat(ASTNode one, ASTNode two) {
        final Pair<ASTNode, ASTNode> parents = sharedParent(one, two);
        final List<Statement> lis = statements((Block) clean(parents.t).getParent());
        return BetweenIterable.of(lis, (Statement) parents.t, (Statement) parents.u);
    }

    private static ASTNode clean(ASTNode t) {
        return t.getParent() instanceof IfStatement ? clean(t.getParent()) : t;
    }


    /**
     * Find {@code pair(a,b)} such that {@code a.getParent() == b.getParent()} and:
     *
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

    /**
     * Any {@link SimpleType} named "Logger"
     */
    public static boolean isLoggerType(Type type) {
        return type instanceof SimpleType
                && compareIfSimpleNode("Logger", ((SimpleType) type).getName());
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

    @SuppressWarnings("unchecked")
    public static List<Expression> extendedOperands(InfixExpression ie) {
        return ie.extendedOperands();
    }

    @SuppressWarnings("unchecked")
    public static List<Expression> expressions(final ArrayInitializer ai) {
        return ai.expressions();
    }

    /**
     * Eclipse bug(?) 319448 makes this bad for StringLiterals, don't use it.
     */
    @SuppressWarnings("unchecked")
    public static <T extends ASTNode> T duplicate(T t) {
        return (T) ASTNode.copySubtree(t.getAST(), t);
    }

    /**
     * {@link StringLiteral#setLiteralValue(String) but don't unnecessarily escape ' (Eclipse bug? 319900)
     */
    public static void setLiteralValue(StringLiteral sl, String value) {
        final StringBuilder b = new StringBuilder(value.length() + 10);
        b.append("\"");
        for (char c : value.toCharArray())
            switch (c) {
                case '\b':
                    b.append("\\b");
                    break;
                case '\t':
                    b.append("\\t");
                    break;
                case '\n':
                    b.append("\\n");
                    break;
                case '\f':
                    b.append("\\f");
                    break;
                case '\r':
                    b.append("\\r");
                    break;
                case '\"':
                    b.append("\\\"");
                    break;
                case '\\':
                    b.append("\\\\");
                    break;
                case '\0':
                    b.append("\\0");
                    break;
                case '\1':
                    b.append("\\1");
                    break;
                case '\2':
                    b.append("\\2");
                    break;
                case '\3':
                    b.append("\\3");
                    break;
                case '\4':
                    b.append("\\4");
                    break;
                case '\5':
                    b.append("\\5");
                    break;
                case '\6':
                    b.append("\\6");
                    break;
                case '\7':
                    b.append("\\7");
                    break;
                default:
                    b.append(c);
            }
        b.append("\"");
        sl.setEscapedValue(b.toString());
    }

    public static String normaliseWhitespace(String src) {
        return ASTWrapper.compile(src).toString();
    }
}
