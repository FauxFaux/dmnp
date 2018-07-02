package com.goeswhere.dmnp.util;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.goeswhere.dmnp.util.ASTContainers.*;

/**
 * Various utilities for converting source to one of the object representations.
 */
public class ASTWrapper {

    private ASTWrapper() {
        throw new AssertionError();
    }

    private static final ImmutableSet<String> BORING_NULLARY_CONSTRUCTORS =
            ImmutableSet.of("java.util.Date",
                    "java.util.StringBuilder",
                    "java.util.StringBuffer");

    private static final Function<IBinding, String> NAME_BINDING = from -> from.getName();

    /**
     * Return the Java 5 CU for the string.
     */
    public static CompilationUnit compile(String c) {
        return compile(c, null, null);
    }

    private static ASTParser getParser(String c) {
        final ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(c.toCharArray());

        final Map<String, String> ops =
                new HashMap<>(javaCoreOptions());

        for (Entry<String, String> a : ops.entrySet())
            if ("warning".equals(a.getValue()))
                a.setValue("ignore");

        ops.put(JavaCore.COMPILER_COMPLIANCE, "1.6");
        ops.put(JavaCore.COMPILER_SOURCE, "1.6");
        ops.put(JavaCore.COMPILER_PB_MAX_PER_UNIT, "0");

        parser.setCompilerOptions(ops);
        return parser;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> javaCoreOptions() {
        return JavaCore.getOptions();
    }

    /**
     * This is a bogglingly ugly hack.  On compiler-generated errors, the compile
     * method will throw this, <b>which contains it's return value</b>.
     */
    public static class HadProblems extends IllegalArgumentException {
        public final CompilationUnit cu;

        public HadProblems(CompilationUnit cu) {
            super("Compile had problems " + ASTContainers.types(cu).get(0).getName()
                    + ": " + Arrays.toString(cu.getProblems()));
            this.cu = cu;
        }
    }

    private static CompilationUnit compile(final ASTParser parser) throws HadProblems {
        final CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        if (cu.getProblems().length != 0)
            throw new HadProblems(cu);
        return cu;
    }

    /**
     * Return the Java 5 CU for the string, with bindings and shiz.
     */
    public static CompilationUnit compile(String src, String[] classpath, String[] source) {
        return compile(src, "OnlyNecessaryForPublicClasses", classpath, source);
    }
//
//	private static final Map<List<String>, List<String>> EXPLODED_CLASSPATH = new MapMaker().makeComputingMap(
//		new Function<List<String>, List<String>>() {
//			@Override public List<String> apply(List<String> from) {
//				final List<String> cp = Lists.newArrayList(from);
//				try {
//					File td = FileUtils.createTempDir();
//					final Iterator<String> it = cp.iterator();
//					while (it.hasNext()) {
//						final String s = it.next();
//						if (new File(s).isFile()) {
//							ZipExploder.processFile(s, td);
//							it.remove();
//						}
//					}
//				} catch (IOException e) {
//					throw new RuntimeException(e);
//				} catch (FailedException e) {
//					throw new RuntimeException(e);
//				}
//
//			}
//	});

    /**
     * If classpath == source == null, don't resolve bindings. Also ignores filename.
     */
    public static CompilationUnit compile(String src, String filename, String[] classpath, String[] source) {
        final ASTParser parser = getParser(src);
        if (null != classpath && null != source) {
            parser.setResolveBindings(true);
            parser.setUnitName(filename);
            parser.setEnvironment(classpath, source, null, true);
            parser.setBindingsRecovery(true);
            parser.setStatementsRecovery(true);
        }
        return compile(parser);
    }


    /**
     * Full signature of a method, including annotations, modifiers, name, parameters
     */
    public static String signature(final MethodDeclaration d) {
        // null on constructors etc.
        final Type rt = d.getReturnType2();
        return FJava.intersperse(d.modifiers(), " ")
                + (null != rt ? " " + rt : "")
                + " " + d.getName().getIdentifier()
                + "(" + FJava.intersperse(d.parameters(), ", ") + ")";
    }

    /**
     * Fully qualified signature of a method
     */
    public static String signature(final IMethodBinding d) {
        final ITypeBinding rt = d.getReturnType();

        final List<String> paramTypes = Lists.transform(Arrays.asList(d.getParameterTypes()), NAME_BINDING);
        final int mod = d.getModifiers();
        return
                (Modifier.isPublic(mod) ? " public" : "") +
                        (Modifier.isProtected(mod) ? " protected" : "") +
                        (Modifier.isPrivate(mod) ? " private" : "") +
                        (Modifier.isFinal(mod) ? " final" : "") +
                        (Modifier.isStatic(mod) ? " static" : "") +
                        (!d.isConstructor() ? " " + rt.getName() : "")
                        + " " + d.getName()
                        + "(" + Joiner.on(", ").join(paramTypes) + ")";
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
        final Mutable<MethodDeclaration> mut = new Mutable<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration m) {
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


    public static String rewrite(final String src, final CompilationUnit changes) {
        return rewrite(new Document(src), changes);
    }

    public static String rewrite(final Document doc, final CompilationUnit changes) {
        return edit(doc, changes.rewrite(doc, null));
    }

    public static String rewrite(final String src, final ASTRewrite rewrite) {
        return rewrite(new Document(src), rewrite);
    }

    public static String rewrite(final Document doc, final ASTRewrite rewrite) {
        return edit(doc, rewrite.rewriteAST(doc, null));
    }

    private static String edit(final Document doc, final TextEdit ed) {
        try {
            ed.apply(doc, TextEdit.UPDATE_REGIONS);
        } catch (MalformedTreeException e) {
            throw new RuntimeException(e);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
        return doc.get();
    }

    public static boolean returnsVoid(MethodDeclaration node) {
        final Type rt = node.getReturnType2();
        return rt instanceof PrimitiveType
                && PrimitiveType.VOID.equals(((PrimitiveType) rt).getPrimitiveTypeCode());
    }

    /**
     * Removes a fragment from its Statement.
     * If this leaves the Statement empty, remove the Statement from <b>its</b> parent.
     */
    public static void removeFragment(final VariableDeclarationFragment vdf) {
        final VariableDeclarationStatement vds = (VariableDeclarationStatement) vdf.getParent();
        final List<VariableDeclarationFragment> fra = fragments(vds);
        if (fra.size() == 1)
            removeFromParent(vds);
        fra.remove(vdf);
    }

    /**
     * Remove a Statement from the Block that is its parent.
     */
    public static boolean removeFromParent(final Statement s) {
        return statements((Block) s.getParent()).remove(s);
    }

    public static boolean doesNothingUseful(final Expression e) {
        if (e instanceof NumberLiteral
                || e instanceof NullLiteral
                || e instanceof CharacterLiteral
                || e instanceof StringLiteral)
            return true;

        if (e instanceof InfixExpression) {
            final InfixExpression inf = (InfixExpression) e;
            return doesNothingUseful(inf.getLeftOperand())
                    && doesNothingUseful(inf.getRightOperand())
                    && Iterables.isEmpty(Iterables.filter(extendedOperands(inf), input -> !doesNothingUseful(input)));
        }

        if (e instanceof ClassInstanceCreation) {
            final ClassInstanceCreation cic = (ClassInstanceCreation) e;
            if (!cic.arguments().isEmpty())
                return false;

            final ITypeBinding rb = cic.getType().resolveBinding();
            return null != rb && BORING_NULLARY_CONSTRUCTORS.contains(rb.getQualifiedName());
        }

        // Qualified names are a bit more scary,
        // but the majority of the annoying ones should be caught by
        // them being constant expressions; caught by the clause below
        if (e instanceof SimpleName)
            return true;

        if (null != e.resolveConstantExpressionValue())
            return true;

        return false;
    }

    public static class FirstElementOfBlock extends IllegalArgumentException {
        FirstElementOfBlock(String s) {
            super(s);
        }
    }

    public static class ParentIsntBlock extends IllegalArgumentException {
        public ParentIsntBlock(String s) {
            super(s);
        }
    }

    private static enum MoveDirection {
        BACKWARDS(-1) {
            @Override
            boolean valid(List<Statement> stats, int ind) {
                return ind != 0;
            }
        },
        FORWARDS(1) {
            @Override
            boolean valid(List<Statement> stats, int ind) {
                return ind < stats.size() - 1;
            }
        },;
        final int cnt;

        MoveDirection(int cnt) {
            this.cnt = cnt;
        }

        abstract boolean valid(List<Statement> stats, int ind);
    }

    public static Statement prev(ASTNode a) {
        return move(a, MoveDirection.BACKWARDS);
    }

    public static Statement next(ASTNode a) {
        return move(a, MoveDirection.FORWARDS);
    }

    private static Statement move(ASTNode retur, MoveDirection d) {
        final ASTNode par = retur.getParent();
        if (!(par instanceof Block))
            throw new ParentIsntBlock(Containers.classAndToString(par));
        final List<Statement> stats = ASTContainers.statements((Block) par);
        final int ind = stats.indexOf(retur);
        if (!d.valid(stats, ind))
            throw new FirstElementOfBlock("Statement is in the wrong place in the block");

        if (-1 == ind)
            throw new AssertionError();

        return stats.get(ind + d.cnt);
    }
}
