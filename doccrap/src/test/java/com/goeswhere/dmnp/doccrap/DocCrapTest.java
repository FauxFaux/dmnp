package com.goeswhere.dmnp.doccrap;

import static com.goeswhere.dmnp.doccrap.DocCrap.cleanCU;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DocCrapTest {
	@Test public void testRemovesTwo() {
		assertEquals("class A {\n" +
					"    /**\n" +
					"     * the foo method\n" +
					"     * @param b with some crap\n" +
					"     */\n" +
					"    void foo(int a, int b) { }\n" +
					"}\n",
		cleanCU("class A {\n" +
				"    /**\n" +
				"     * the foo method\n" +
				"     * @param a\n" +
				"     * @param b with some crap\n" +
				"     * @param c doesn't even exist\n" +
				"     */\n" +
				"    void foo(int a, int b) { }\n" +
				"}\n"));
	}

	@Test public void testGetterReturn() {
		assertEquals("class A {\n" +
					"    int getFoo() { }\n" +
					"}\n",
		cleanCU("class A {\n" +
				"    /**\n" +
				"     * @return Returns the value of foo\n" +
				"     */\n" +
				"    int getFoo() { }\n" +
				"}\n"));
	}

	@Test public void testReturnType() {
		assertEquals("class A {\n" +
					"    int getFoo() { }\n" +
					"}\n",
		cleanCU("class A {\n" +
				"    /**\n" +
				"     * @return int\n" +
				"     */\n" +
				"    int getFoo() { }\n" +
				"}\n"));
	}

	@Test public void testDash() {
		assertEquals("class A {\n" +
					"    int getFoo(int a) { }\n" +
					"}\n",
		cleanCU("class A {\n" +
				"    /**\n" +
				"     * @param a -\n" +
				"     * @return -\n" +
				"     */\n" +
				"    int getFoo(int a) { }\n" +
				"}\n"));
	}

	@Test public void testTypeDash() {
		assertEquals("class A {\n" +
					"    int getFoo(int a) { }\n" +
					"}\n",
		cleanCU("class A {\n" +
				"    /**\n" +
				"     * @param a int -\n" +
				"     * @return int -\n" +
				"     */\n" +
				"    int getFoo(int a) { }\n" +
				"}\n"));
	}


	@Test public void testReturnEmpty() {
		assertEquals("class A {\n" +
					"    int getFoo() { }\n" +
					"}\n",
		cleanCU("class A {\n" +
				"    /**\n" +
				"     * @return\n" +
				"     */\n" +
				"    int getFoo() { }\n" +
				"}\n"));
	}

	@Test public void testAtTag() {
		assertEquals("class A {\n" +
					"    int getFoo() { }\n" +
					"}\n",
		cleanCU("class A {\n" +
				"    /**\n" +
				"     * @\n" +
				"     */\n" +
				"    int getFoo() { }\n" +
				"}\n"));
	}

	@Test public void testRemovesWholly() {
		assertEquals("class A {\n" +
					"    void foo(int a, int b) { }\n" +
					"}\n",
		cleanCU("class A {\n" +
				"    /**\n" +
				"     * @param a\n" +
				"     * @param c doesn't even exist\n" +
				"     */\n" +
				"    void foo(int a, int b) { }\n" +
				"}\n"));
	}

	@Test public void testNoDoc() {
		final String nodoc = "class A {\n" +
					"    void foo(int a, int b) { }\n" +
					"}\n";
		assertEquals(nodoc, cleanCU(nodoc));
	}

}
