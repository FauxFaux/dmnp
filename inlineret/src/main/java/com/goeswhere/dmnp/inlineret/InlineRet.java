package com.goeswhere.dmnp.inlineret;

import com.goeswhere.dmnp.util.ASTWrapper.*;
import com.goeswhere.dmnp.util.ResolvingFileFixer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.eclipse.jdt.core.dom.*;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

import static com.goeswhere.dmnp.util.ASTWrapper.*;

class InlineRet extends ResolvingFileFixer {
    @VisibleForTesting
    InlineRet(String[] classpath, String[] sourcepath, String unitName, Lock l) {
        super(classpath, sourcepath, unitName, l);
    }

    public static void main(String[] args) throws InterruptedException {
        main(args, (cp, sourcePath, unitName, l) -> new InlineRet(cp, sourcePath, unitName, l));
    }

    @Override
    public String apply(final String src) {
        final CompilationUnit cu = compile(src);
        cu.recordModifications();

        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration md) {
                final Block bd = md.getBody();

                if (returnsVoid(md) || null == bd)
                    return true;
                final Map<IBinding, Integer> refCounts = Maps.newHashMap();
                final Map<IBinding, VariableDeclarationFragment> decls = Maps.newHashMap();

                bd.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(VariableDeclarationFragment node) {
                        final IVariableBinding bin = node.resolveBinding();
                        refCounts.put(bin, 0);
                        decls.put(bin, node);
                        return true;
                    }

                    @Override
                    public boolean visit(SimpleName node) {
                        final IBinding bin = node.resolveBinding();
                        final Integer i = refCounts.get(bin);
                        if (null != i)
                            refCounts.put(bin, i + 1);
                        return true;
                    }
                });

                bd.accept(new ASTVisitor() {

                    @Override
                    public boolean visit(ReturnStatement retur) {

                        final Expression rex = retur.getExpression(); // null check above
                        if (!(rex instanceof Name))
                            return true;

                        final IBinding bin = ((Name) rex).resolveBinding();
                        final Integer refs = refCounts.get(bin);
                        if (null == refs)
                            return true;

                        final ASTNode prev;
                        try {
                            prev = prev(retur);
                        } catch (FirstElementOfBlock e) {
                            return true;
                        }

                        if (!(prev instanceof ExpressionStatement))
                            return true;

                        final ExpressionStatement expstat = (ExpressionStatement) prev;

                        final Expression expr = expstat.getExpression();
                        if (!(expr instanceof Assignment))
                            return true;

                        final Assignment assign = (Assignment) expr;

                        if (!Assignment.Operator.ASSIGN.equals(assign.getOperator()))
                            return true;

                        final Expression lhs = assign.getLeftHandSide();
                        if (!(lhs instanceof Name && ((Name) lhs).resolveBinding().equals(bin)))
                            return true;

                        final Expression rhs = assign.getRightHandSide();
                        assign.setRightHandSide(cu.getAST().newNullLiteral());
                        retur.setExpression(rhs);
                        refCounts.put(bin, refs - 2); // a = 5; return a; == 2

                        removeFromParent(expstat);
                        return true;
                    }

                });

                for (Entry<IBinding, Integer> a : refCounts.entrySet())
                    if (1 == a.getValue()) {
                        final VariableDeclarationFragment decl = decls.get(a.getKey());
                        if (null == decl.getInitializer() || doesNothingUseful(decl.getInitializer()))
                            removeFragment(decl);
                    }

                return true;
            }
        });

        return rewrite(src, cu);
    }
}
