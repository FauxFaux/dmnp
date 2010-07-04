package com.goeswhere.dmnp.trace4jast;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.goeswhere.dmnp.trace4jast.Trace4jAst.NameGenerator;
import com.goeswhere.dmnp.trace4jast.Trace4jAst.Rewriter;

public class Trace4jAstTest {
	private static final String PREFIX = "class A { int foo() { ";
	private static final String BODY = "a(); return b(); ";
	private static final String ENTER = " logger.trace(\"foo: entering\"); ";
	private static final String LEAVE = " logger.trace(\"foo: leaving at 1\"); ";
	private static final String INPUT = PREFIX + BODY + "} }";

	private static final String LOGGER_AT_END = " private static final org.apache.log4j.Logger logger = " +
					"org.apache.log4j.Logger .getLogger(A.class); }";

	@Test public void simpleFinally() {
		assertWashes(Trace4jAst.BuiltInRewriters.FINALLY_REWRITER,
				PREFIX + "try {" + ENTER + BODY + "} finally { " +
						"logger.trace(\"foo: leaving\"); } }" + LOGGER_AT_END, INPUT);
	}

	@Test public void simpleExtractReturn() {
		assertWashes(Trace4jAst.BuiltInRewriters.ENTER_AND_EXTRACT_RETURN,
				PREFIX + ENTER + "a(); { final int rv = b();" + LEAVE + " return rv; } }" + LOGGER_AT_END, INPUT);
	}

	@Test public void testExtractReturnVoid() {
		assertWashes(Trace4jAst.BuiltInRewriters.ENTER_AND_EXTRACT_RETURN,
				"class A { void foo() { " + ENTER + "a(); {" + LEAVE + "return; } }" + LOGGER_AT_END,
				"class A { void foo() { a(); return; } }");
	}

	@Test public void testExtractVoid() {
		assertWashes(Trace4jAst.BuiltInRewriters.ENTER_AND_EXTRACT_RETURN,
				"class A { void foo() { " + ENTER + "a(); logger.trace(\"foo: leaving\"); }" + LOGGER_AT_END,
				"class A { void foo() { a(); } }");
	}

	@Test public void testExtractReturnThrow() {
		assertWashes(Trace4jAst.BuiltInRewriters.ENTER_AND_EXTRACT_RETURN,
				"class A { void foo() { " + ENTER + "a(); { " +
						"final RuntimeException rv = new RuntimeException();" + LEAVE + "throw rv; } }"
						+ LOGGER_AT_END,
				"class A { void foo() { a(); throw new RuntimeException(); } }");
	}

	@Test public void testGetter() {
		assertWashes(Trace4jAst.BuiltInRewriters.ENTER_AND_EXTRACT_RETURN,
				"class A { int f; int foo() { logger.trace(\"foo: called\"); return f; }" + LOGGER_AT_END,
				"class A { int f; int foo() { return f; } }");
	}

	@Test public void testSetter() {
		assertWashes(Trace4jAst.BuiltInRewriters.ENTER_AND_EXTRACT_RETURN,
				"class A { int f; void foo(int j) { logger.trace(\"foo: called\"); f = j; }" + LOGGER_AT_END,
				"class A { int f; void foo(int j) { f = j; } }");
	}


	@Test public void testSetterThis() {
		assertWashes(Trace4jAst.BuiltInRewriters.ENTER_AND_EXTRACT_RETURN,
				"class A { int f; void foo(int f) { logger.trace(\"foo: called\"); this.f = f; }" + LOGGER_AT_END,
				"class A { int f; void foo(int f) { this.f = f; } }");
	}

	@Test public void testNoBlockIf() {
		assertWashes(Trace4jAst.BuiltInRewriters.ENTER_AND_EXTRACT_RETURN,
				"class A { int f; int foo(int f) { logger.trace(\"foo: entering\"); " +
				"if (f > 5) { logger.trace(\"foo: leaving at 1\"); return f; }" +
				" { logger.trace(\"foo: leaving at 2\"); return 0; }} " + LOGGER_AT_END,
				"class A { int f; int foo(int f) { if (f > 5) return f; return 0;} }");
	}

	@Test public void testInnerReturnType() {
		assertWashes(Trace4jAst.BuiltInRewriters.ENTER_AND_EXTRACT_RETURN,
				"class A { int foo() { logger.trace(\"foo: entering\"); " +
				"new Object() { String s(){{" +
				" final String rv = q(); logger.trace(\"foo: leaving at 1\"); return rv; }}}.s(); " +
				"{ logger.trace(\"foo: leaving at 2\"); return 0; }}" + LOGGER_AT_END,
				"class A { int foo() { new Object() { String s(){return q();}}.s(); return 0;} }");
	}

	@Test public void testSwitch() {
		assertWashes(Trace4jAst.BuiltInRewriters.ENTER_AND_EXTRACT_RETURN,
				"class A { int foo(int f) { logger.trace(\"foo: entering\"); " +
				"switch (f) { case 1: { logger.trace(\"foo: leaving at 1\"); return 0; } " +
				"case 2: { final int rv = b(); logger.trace(\"foo: leaving at 2\"); return rv; } " +
				"default: { logger.trace(\"foo: leaving at 3\"); return 9; } } } " + LOGGER_AT_END,
				"class A { int foo(int f) { switch (f) { case 1: return 0; case 2: return b(); " +
					"default: return 9; } } }");
	}

	// TODO currently this lifts the call a few lines up, technically changing the line numbers
	@Test public void testCommentsBefore() {
		assertWashes(Trace4jAst.BuiltInRewriters.ENTER_AND_EXTRACT_RETURN,
				"class A { int foo() {\n" +
				"logger.trace(\"foo: entering\"); { final int rv = b(); logger.trace(\"foo: leaving at 1\"); \n" +
				"return rv; }\n" +
				" }" + LOGGER_AT_END,
				"class A { int foo() {\n" +
				"//before\n" +
				"return b();\n } }");
	}

	@Test public void testCommentsAfter() {
		assertWashes(Trace4jAst.BuiltInRewriters.ENTER_AND_EXTRACT_RETURN,
				"class A { int foo() {\n" +
				"logger.trace(\"foo: entering\"); { final int rv = b(); logger.trace(\"foo: leaving at 1\"); " +
				"return rv;\n" +
				" }\n" +
				" }" + LOGGER_AT_END,
				"class A { int foo() {\n" +
				"return b();\n" +
				"//after\n" +
				" } }");
	}

	/** https://bugs.eclipse.org/bugs/show_bug.cgi?id=317468 causes a trailing comma. */
	@Test public void testEnum() {
		assertWashes(Trace4jAst.BuiltInRewriters.ENTER_AND_EXTRACT_RETURN,
				"enum A { A, B,; " + LOGGER_AT_END,
				"enum A { A, B, }");
	}

	@Test public void testLoopFiddling() {
		assertWashes(Trace4jAst.BuiltInRewriters.ENTER_AND_EXTRACT_RETURN,
				"class A { int foo() { logger.trace(\"foo: entering\");" +
				" while (true) {} " +
				"}" + LOGGER_AT_END,
				"class A { int foo() {" +
				" while (true) {}" +
				" } }");
	}

	private void assertWashes(Rewriter r, String expected, String actual) {
		assertEquals(wash(expected), wash(new Trace4jAst(r, new NameGenerator() {
			@Override public String apply(String prefix) {
				return prefix;
			}
		}).apply(actual)));
	}

	private String wash(String apply) {
		return apply.replaceAll("[\t ]{2,}", " ");
	}
}
