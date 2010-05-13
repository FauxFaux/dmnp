package com.goeswhere.dmnp.util;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.junit.Test;

import com.google.common.collect.Iterables;

public class ASTContainersTest {
	@Test public void sharedParentTrivialest() {
		final MethodDeclaration m = ASTWrapper.extractSingleMethod("void a() { int a; int b; }");
		final Block body = m.getBody();
		final List<Statement> sts = ASTContainers.it(body);
		final Statement a = sts.get(0);
		final Statement b = sts.get(1);
		assertEquals(Pair.of(a, b),
				ASTContainers.sharedParent(a, b));
	}

	@Test public void sharedParentDiff() {
		final MethodDeclaration m = ASTWrapper.extractSingleMethod("void a() { int a; if (7 == 9) { int b; } }");
		final Block body = m.getBody();
		final List<Statement> sts = ASTContainers.it(body);
		final Statement a = sts.get(0);
		final IfStatement ifthen = (IfStatement)sts.get(1);
		final Statement b = Iterables.getOnlyElement(ASTContainers.it((Block)ifthen.getThenStatement()));
		assertEquals(Pair.of(a, ifthen),
				ASTContainers.sharedParent(a, b));
	}

	@Test public void statementsBetweenFlat() {
		final MethodDeclaration m = ASTWrapper.extractSingleMethod(
				"void a() { " +
				"int tis; int bar;" +
				"try { foo(); }" +
				"finally { int baz; int that; }}");

		final Block body = m.getBody();
		final List<Statement> sts = ASTContainers.it(body);
		final Statement tis = sts.get(0);
		final Statement that = ASTContainers.it(((TryStatement)sts.get(2)).getFinally()).get(1);
		final Statement bar = sts.get(1);
		assertEquals(bar, Iterables.getOnlyElement(ASTContainers.statementsBetweenFlat(tis, that)));
	}

}
