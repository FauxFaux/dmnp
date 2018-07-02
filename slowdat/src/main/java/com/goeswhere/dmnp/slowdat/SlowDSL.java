package com.goeswhere.dmnp.slowdat;

import com.goeswhere.dmnp.util.ASTContainers;
import com.goeswhere.dmnp.util.ASTWrapper.HadProblems;
import com.goeswhere.dmnp.util.ResolvingFileFixer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.*;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import static com.goeswhere.dmnp.util.ASTWrapper.doesNothingUseful;
import static com.goeswhere.dmnp.util.ASTWrapper.rewrite;


public class SlowDSL extends ResolvingFileFixer {

    public static void main(String[] args) throws InterruptedException {
        main(args, (cp, sourcePath, name, l) -> new SlowDSL(cp, sourcePath, name, l));
    }

    private static final Function<ITypeBinding, String> TYPEBINDING_QUALIFIED_NAME =
            from -> from.getQualifiedName();

    @VisibleForTesting
    SlowDSL(String[] classpath, String[] sourcepath, String unitName, Lock l) {
        super(classpath, sourcepath, unitName, l);
    }

    @Override
    public String apply(final String src) {
        return go(src, Collections.<String>emptySet());
    }

    private static class InitialiserStripper extends ASTVisitor {
        private final Set<String> skips;

        private InitialiserStripper(Set<String> skipWhat) {
            this.skips = ImmutableSet.copyOf(skipWhat);
        }

        @Override
        public boolean visit(VariableDeclarationStatement node) {
            for (VariableDeclarationFragment a : ASTContainers.fragments(node)) {
                final Expression i = a.getInitializer();
                if (null == i)
                    continue;

                if (doesNothingUseful(i)) {
                    final IVariableBinding bi = a.resolveBinding();
                    if (skips.contains(strignature(bi)))
                        continue;
                    a.setInitializer(null);
                }
            }
            return true;
        }
    }

    private final Set<Set<String>> seenBrokens = Sets.newHashSet();

    private String go(final String origsrc, final Set<String> skip) {
        final String newsrc = mutilateSource(origsrc, skip);

        final Set<String> currentlyBroken = Sets.newHashSet();

        try {
            compile(newsrc);
            return newsrc;
        } catch (HadProblems p) {
            final SetMultimap<String, Integer> e = HashMultimap.create();
            for (IProblem a : p.cu.getProblems()) {
                if (IProblem.UninitializedLocalVariable != a.getID())
                    throw p;
                e.put(a.getArguments()[0], a.getSourceStart());
            }

            p.cu.accept(new ASTVisitor() {
                @Override
                public boolean visit(SimpleName node) {
                    if (e.get(node.getIdentifier()).remove(getRealErrorReportPosition(node))) {
                        final IVariableBinding vb = (IVariableBinding) node.resolveBinding();
                        currentlyBroken.add(strignature(vb));
                    }
                    return true;
                }
            });
        }

        if (seenBrokens.contains(currentlyBroken))
            throw new RuntimeException("Didn't work, still have " + currentlyBroken);

        seenBrokens.add(currentlyBroken);
        return go(origsrc, ImmutableSet.<String>builder()
                .addAll(currentlyBroken)
                .addAll(skip)
                .build());
    }

    /**
     * Problems in {@code ((var))} are reported at
     * the first open bracket, not on {@code var} itself.
     */
    private int getRealErrorReportPosition(final SimpleName sn) {
        ASTNode test = sn;
        while (test.getParent() instanceof ParenthesizedExpression)
            test = test.getParent();
        return test.getStartPosition();
    }

    private String mutilateSource(final String src, final Set<String> skip) {
        final CompilationUnit cu = compile(src);
        cu.recordModifications();
        cu.accept(new InitialiserStripper(skip));
        return rewrite(src, cu);
    }

    private static String strignature(final IVariableBinding vb) {
        final IMethodBinding meth = vb.getDeclaringMethod();
        return vb.getName() + ":" + vb.getVariableId() + ":" +
                (null == meth ? "[outside method]" : signature(meth));
    }

    private static String signature(IMethodBinding from) {
        return from.getDeclaringClass().getName() + "#" + from.getName()
                + Lists.transform(Arrays.asList(from.getParameterTypes()), TYPEBINDING_QUALIFIED_NAME);
    }
}
