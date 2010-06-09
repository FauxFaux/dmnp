package com.goeswhere.dmnp.inlineret;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.goeswhere.dmnp.util.TestUtils;

public class InlineRetTest {
	@Test public void testSimple() {
		assertIRsTo(
			"class A { int foo() {" +
			" int a = 5;" +
			" if (null != A.class) {" +
			"  return 9;" +
			" }" +
			" return a;" +
			"}}",

			"class A { int foo() {" +
			" int a = 5;" +
			" if (null != A.class) {" +
			"  a = 9;" +
			"  return a;" +
			" }" +
			" return a;" +
			"}}");
	}

	@Test public void testEliminateLocal() {
		assertIRsTo(
			"class A { int foo() {" +
			" if (null != A.class) {" +
			"  return 9;" +
			" }" +
			" return 2;" +
			"}}",

			"class A { int foo() {" +
			" int a;" +
			" if (null != A.class) {" +
			"  a = 9;" +
			"  return a;" +
			" }" +
			" a = 2;" +
			" return a;" +
			"}}");
	}

	@Test public void testEliminateSafeLocal() {
		assertIRsTo(
			"class A { int foo() {" +
			" if (null != A.class) {" +
			"  return 9;" +
			" }" +
			" return 2;" +
			"}}",

			"class A { int foo() {" +
			" int a = 12;" +
			" if (null != A.class) {" +
			"  a = 9;" +
			"  return a;" +
			" }" +
			" a = 2;" +
			" return a;" +
			"}}");
	}

	@Test public void testCantEliminateScaryLocal() {
		assertIRsTo(
			"class A { int foo() {" +
			" int a = foo();" +
			" if (null != A.class) {" +
			"  return 9;" +
			" }" +
			" return 2;" +
			"}}",

			"class A { int foo() {" +
			" int a = foo();" +
			" if (null != A.class) {" +
			"  a = 9;" +
			"  return a;" +
			" }" +
			" a = 2;" +
			" return a;" +
			"}}");
	}

	private void assertIRsTo(String expected, String actual) {
		assertEquals(clean(expected), clean(go(actual)));
	}

	private static String clean(String string) {
		return string.replaceAll("[\\s]+", " ");
	}

	private String go(String src) {
		return new InlineRet(new String[0], new String[0], "A", TestUtils.EMPTY_LOCK).apply(src);
	}
}
