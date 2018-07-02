package com.goeswhere.dmnp.preplookup;

import com.goeswhere.dmnp.util.ASTAllVisitor;
import com.goeswhere.dmnp.util.ASTContainers;
import com.goeswhere.dmnp.util.ResolvingFileFixer;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import static com.goeswhere.dmnp.util.ASTContainers.*;
import static com.goeswhere.dmnp.util.ASTWrapper.rewrite;

public class PrepLookup extends ResolvingFileFixer {
    private final Map<String, Integer> functionPrefix;
    private final String safeSQL;
    private final String dateFormat;

    protected PrepLookup(String[] classpath, String[] sourcepath,
                         String unitName, Lock compilerLock, Map<String, Integer> function) {
        this(classpath, sourcepath, unitName, compilerLock, function,
                getProperty("safesql"), getProperty("dateformat"));
    }

    PrepLookup(String[] classpath, String[] sourcepath,
               String unitName, Lock compilerLock, Map<String, Integer> function,
               String safeSQL, String dateFormat) {
        super(classpath, sourcepath, unitName, compilerLock);
        this.functionPrefix = function;
        this.safeSQL = safeSQL;
        this.dateFormat = dateFormat;
    }

    private static ImmutableMap<String, Integer> parseFunction(String function) {
        final Builder<String, Integer> ma = ImmutableMap.builder();
        for (String s : function.split(",")) {
            String[] par = s.split("=");
            if (2 != par.length)
                throw new IllegalArgumentException("Function spec: name=skipargs[,name=skipargs]*");
            ma.put(par[0], Integer.parseInt(par[1]));
        }
        return ma.build();
    }

    public static void main(final String[] args) throws InterruptedException {
        main("functionPrefix", 1, args, new Creator() {
            @Override
            public Function<String, String> create(String[] cp,
                                                   String[] sourcePath, String unitName, Lock compileLock) {
                return new PrepLookup(cp, sourcePath, unitName, compileLock,
                        parseFunction(args[0]));
            }
        });

        System.out.println("edited=" + edited +
                ", noninfix=" + noninfix +
                ", literal=" + literal +
                ", type=" + type +
                ", badstrings=" + badstrings +
                ", nothingtodo=" + nothingtodo);
    }

    private static final AtomicInteger
            edited = a(),
            noninfix = a(),
            literal = a(),
            badstrings = a(),
            type = a(),
            nothingtodo = a();

    private static AtomicInteger a() {
        return new AtomicInteger();
    }

    @Override
    public String apply(String from) {
        // optimisation
        if (!containsAny(from))
            return from;

        final CompilationUnit cu = compile(from);
        cu.recordModifications();
        final AST ast = cu.getAST();
        cu.accept(new ASTAllVisitor() {

            @Override
            public void visitMethodInvocation(MethodInvocation mi) {
                final Entry<String, Integer> relevant = getRelevant(mi);
                if (null == relevant)
                    return;
                final int targetArg = relevant.getValue();

                final List<Expression> args = ASTContainers.arguments(mi);
                Expression ex = args.get(targetArg);

                if (null != ex.resolveConstantExpressionValue()) {
                    literal.incrementAndGet();
                    return;
                }

                while (ex instanceof ParenthesizedExpression)
                    ex = ((ParenthesizedExpression) ex).getExpression();

                if (ex instanceof InfixExpression) {
                    final InfixExpression ie = (InfixExpression) ex;
                    if (!ie.getOperator().equals(Operator.PLUS)) {
                        noninfix.incrementAndGet();
                        report("Not plus? " + ie, cu, ie);
                        return;
                    }

                    final List<Expression> exop = ASTContainers.extendedOperands(ie);
                    final List<Expression> full = Lists.newArrayListWithCapacity(2 + exop.size());
                    full.add(ie.getLeftOperand());
                    full.add(ie.getRightOperand());
                    full.addAll(exop);

                    final int parts = full.size();

                    final List<Expression> binds = Lists.newArrayListWithCapacity(parts);
                    final List<Expression> newExpression = Lists.newArrayListWithCapacity(parts);

                    boolean flagNextForRemovalOfQuote = false;
                    for (int i = 0; i < parts; ++i) {
                        Expression curr = full.get(i);

                        // call to duplicate() misbehaves with StringLiterals
                        if (curr instanceof StringLiteral) {
                            final StringLiteral nsl = ast.newStringLiteral();
                            nsl.setEscapedValue(((StringLiteral) curr).getEscapedValue());
                            if (flagNextForRemovalOfQuote) {
                                flagNextForRemovalOfQuote = false;
                                removeFirstCharacter(nsl);
                            }
                            newExpression.add(nsl);
                            continue;
                        }

                        final Object conexp = curr.resolveConstantExpressionValue();
                        if (conexp instanceof String) {
                            // may be modified by future iterations
                            newExpression.add(duplicate(curr));
                            continue;
                        }

                        Expression replacement = null;
                        boolean requireQuotes = false;

                        if (curr instanceof MethodInvocation) {
                            final MethodInvocation ci = (MethodInvocation) curr;
                            final String functionName = ci.getName().getIdentifier();
                            final boolean df = isDateFormat(functionName);
                            final boolean ssq = isSQLEscape(functionName);
                            final List<Expression> arg = arguments(ci);

                            if (1 == arg.size() && (df || ssq)) {
                                replacement = duplicate(arg.get(0));
                                requireQuotes = ssq;
                            }
                        }

                        if (null != replacement)
                            curr = replacement;
                        else {
                            final ITypeBinding tb = curr.resolveTypeBinding();
                            if (unsafeTypeBinding(tb)) {
                                if (!(curr instanceof SimpleName))
                                    report("Not safely mappable type '" + tb.getName() + "' for "
                                            + curr, mi, cu, curr);
                                type.incrementAndGet();
                                return;
                            }
                        }

                        if (i == 0) {
                            badstrings.incrementAndGet();
                            return;
                        }

                        final Expression prev = newExpression.get(newExpression.size() - 1);

                        if (i + 1 < parts
                                && stringLiteralEndingInOperatorQuote(prev)
                                && stringLiteralStartingWithQuote(full.get(i + 1))) {
                            StringLiteral sl = (StringLiteral) prev;
                            final String lit = sl.getLiteralValue();
                            setLiteralValue(sl, lit.substring(0, lit.length() - 1) + "?");
                            flagNextForRemovalOfQuote = true;
                        } else {
                            if (requireQuotes) {
                                badstrings.incrementAndGet();
                                Expression that = full.get(i);
                                report("Expecting " + that + " to be surrounded by ''s in " + mi, cu, that);
                                return;
                            }

                            final Expression prevAct = full.get(i - 1);
                            final Object prevValue = prevAct.resolveConstantExpressionValue();

                            if (!stringEndingInOperator(prevValue)) {
                                badstrings.incrementAndGet();
                                return;
                            }

                            if (prev instanceof StringLiteral) {
                                final StringLiteral slprev = (StringLiteral) prev;
                                setLiteralValue(slprev, slprev.getLiteralValue() + "?");
                            } else {
                                final StringLiteral nsl = ast.newStringLiteral();
                                nsl.setLiteralValue("?");
                                newExpression.add(nsl);
                            }
                        }
                        binds.add(curr);
                    }

                    if (binds.isEmpty()) {
                        nothingtodo.incrementAndGet();
                        return;
                    }

                    dieee(ie);
                    if (containsOnlyStringLiterals(newExpression)) {
                        final Iterator<Expression> it = newExpression.iterator();
                        while (it.hasNext())
                            if (((StringLiteral) it.next()).getLiteralValue().isEmpty())
                                it.remove();
                    }

                    if (1 == newExpression.size())
                        args.set(targetArg, newExpression.get(0));
                    else {
                        final InfixExpression nie = ast.newInfixExpression();
                        nie.setOperator(Operator.PLUS);
                        nie.setLeftOperand(newExpression.get(0));
                        nie.setRightOperand(newExpression.get(1));
                        final List<Expression> nieo = ASTContainers.extendedOperands(nie);
                        for (int i = 2; i < newExpression.size(); ++i)
                            nieo.add(newExpression.get(i));

                        args.set(targetArg, nie);
                    }

                    final ArrayCreation ac = ast.newArrayCreation();
                    ac.setType(ast.newArrayType(ast.newSimpleType(ast.newSimpleName("Object"))));
                    final ArrayInitializer ai = ast.newArrayInitializer();
                    ac.setInitializer(ai);
                    expressions(ai).addAll(binds);
                    args.add(ac);
                    edited.incrementAndGet();
                } else if (ex instanceof StringLiteral) {
                    noninfix.incrementAndGet();
                } else if (ex instanceof SimpleName) {
                    noninfix.incrementAndGet();
                } else if (ex instanceof MethodInvocation) {
                    noninfix.incrementAndGet();
                } else {
                    noninfix.incrementAndGet();
                    report("What the: " + mi, cu, mi);
                }
            }
        });

        return rewrite(from, cu);
    }

    private void report(String msg, CompilationUnit cu, ASTNode item) {
        System.err.println(msg + bracketedFileLocation(cu, item));
    }

    private void report(String msg, ASTNode source, CompilationUnit cu, ASTNode item) {
        System.err.println(msg + " in" + bracketedFileLocation(cu, item) + " " + source);
    }

    private String bracketedFileLocation(CompilationUnit cu, ASTNode item) {
        return " (" + fileLocation(cu, item) + ")";
    }

    private boolean isSQLEscape(final String functionName) {
        return safeSQL.equalsIgnoreCase(functionName);
    }

    private boolean isDateFormat(final String functionName) {
        return dateFormat.equalsIgnoreCase(functionName);
    }

    private boolean containsAny(String from) {
        for (String s : functionPrefix.keySet())
            if (from.contains(s))
                return true;
        return false;
    }

    private void removeFirstCharacter(StringLiteral sl) {
        sl.setLiteralValue(sl.getLiteralValue().substring(1));
    }

    private static void dieee(final InfixExpression ie) {
        final AST ast = ie.getAST();
        final StringLiteral left = ast.newStringLiteral();
        left.setLiteralValue("PONIES");
        ie.setLeftOperand(left);
        ie.setRightOperand(ast.newStringLiteral());
        ie.extendedOperands().clear();
    }

    private static boolean stringEndingInOperator(Object o) {
        if (!(o instanceof String))
            return false;
        return ((String) o).matches(".*(?:=|<>|>|<)\\s*-?\\s*$");
    }

    private boolean stringLiteralEndingInOperatorQuote(Expression o) {
        if (!(o instanceof StringLiteral))
            return false;
        return ((StringLiteral) o).getLiteralValue().matches(".*(?:=|<>)\\s*'$");
    }

    private boolean stringLiteralStartingWithQuote(Expression o) {
        if (!(o instanceof StringLiteral))
            return false;
        return ((StringLiteral) o).getLiteralValue().startsWith("'");
    }

    private boolean containsOnlyStringLiterals(Iterable<? extends Expression> newExpression) {
        for (Expression q : newExpression)
            if (!(q instanceof StringLiteral))
                return false;
        return true;
    }

    private @Nullable
    Entry<String, Integer> getRelevant(final MethodInvocation mi) {
        for (Entry<String, Integer> a : functionPrefix.entrySet())
            if (mi.getName().getIdentifier().startsWith(a.getKey())
                    && a.getValue() == mi.arguments().size() - 1)
                return a;
        return null;
    }

    private static boolean unsafeTypeBinding(final ITypeBinding ty) {
        return !ty.isPrimitive() && !ty.getName().equals("java.util.Date");
    }

    private static String getProperty(String name) {
        return System.getProperty(name, name);
    }
}
