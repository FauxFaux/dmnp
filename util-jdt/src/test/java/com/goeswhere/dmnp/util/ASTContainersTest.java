package com.goeswhere.dmnp.util;

import com.google.common.collect.Iterables;
import org.eclipse.jdt.core.dom.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.goeswhere.dmnp.util.ASTContainers.sharedParent;
import static com.goeswhere.dmnp.util.ASTContainers.statements;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ASTContainersTest {
    @Test
    void sharedParentTrivialest() {
        final MethodDeclaration m = ASTWrapper.extractSingleMethod("void a() { int a; int b; }");
        final Block body = m.getBody();
        final List<Statement> sts = statements(body);
        final Statement a = sts.get(0);
        final Statement b = sts.get(1);
        assertEquals(Pair.of(a, b),
                sharedParent(a, b));
    }

    @Test
    void sharedParentDiff() {
        final MethodDeclaration m = ASTWrapper.extractSingleMethod("void a() { int a; if (7 == 9) { int b; } }");
        final Block body = m.getBody();
        final List<Statement> sts = statements(body);
        final Statement a = sts.get(0);
        final IfStatement ifthen = (IfStatement) sts.get(1);
        final Statement b = Iterables.getOnlyElement(statements((Block) ifthen.getThenStatement()));
        assertEquals(Pair.of(a, ifthen),
                sharedParent(a, b));
    }

    @Test
    void statementsBetweenFlat() {
        final MethodDeclaration m = ASTWrapper.extractSingleMethod(
                "void a() { " +
                        "int tis; int bar;" +
                        "try { foo(); }" +
                        "finally { int baz; int that; }}");

        final Block body = m.getBody();
        final List<Statement> sts = statements(body);
        final Statement tis = sts.get(0);
        final Statement that = statements(((TryStatement) sts.get(2)).getFinally()).get(1);
        final Statement bar = sts.get(1);
        assertEquals(bar, Iterables.getOnlyElement(ASTContainers.statementsBetweenFlat(tis, that)));
    }

}
