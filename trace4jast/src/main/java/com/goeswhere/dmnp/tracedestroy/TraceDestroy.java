package com.goeswhere.dmnp.tracedestroy;

import com.goeswhere.dmnp.util.ASTAllVisitor;
import com.goeswhere.dmnp.util.ASTContainers;
import com.goeswhere.dmnp.util.ASTWrapper;
import com.goeswhere.dmnp.util.SimpleFileFixer;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.eclipse.jdt.core.dom.*;

import java.util.List;
import java.util.Set;

import static com.goeswhere.dmnp.util.ASTWrapper.rewrite;

public class TraceDestroy extends SimpleFileFixer {
    public static void main(String[] args) throws InterruptedException {
        main(args, new Creator() {
            @Override
            public Function<String, String> create() {
                return new TraceDestroy();
            }
        });
    }

    private static final Set<String> req = ImmutableSet.of("debug", "trace");

    @Override
    public String apply(String from) {
        final CompilationUnit cu = compile(from);
        final Set<String> loggers = ASTContainers.loggers(cu);
        final List<MethodInvocation> destroy = Lists.newArrayList();
        cu.accept(new ASTAllVisitor() {
            @Override
            public void visitMethodInvocation(MethodInvocation mi) {
                final Expression ex = mi.getExpression();
                if (ex instanceof SimpleName
                        && loggers.contains(((SimpleName) ex).getIdentifier())
                        && req.contains(mi.getName().getIdentifier())
                        && 1 == mi.arguments().size()) {
                    final Expression arg = ASTContainers.arguments(mi).get(0);
                    if (arg instanceof StringLiteral) {
                        final String s = ((StringLiteral) arg).getLiteralValue().toLowerCase();
                        if (s.endsWith("entering")
                                || s.endsWith("enter")
                                || s.endsWith("entered")
                                || s.endsWith("leaving")
                                || s.endsWith("leave")
                                || s.endsWith("called"))
                            destroy.add(mi);
                    }
                }
            }
        });

        cu.recordModifications();
        for (MethodInvocation m : destroy)
            ASTWrapper.removeFromParent((Statement) m.getParent());
        return rewrite(from, cu);
    }
}
