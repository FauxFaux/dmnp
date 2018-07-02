package com.goeswhere.dmnp.doccrap;

import org.junit.jupiter.api.Test;

import static com.goeswhere.dmnp.doccrap.DocCrap.cleanCU;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DocCrapTest {
    @Test
    void testRemovesTwo() {
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

    @Test
    void testGetterReturn() {
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

    @Test
    void testReturnType() {
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

    @Test
    void testDash() {
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

    @Test
    void testTypeDash() {
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


    @Test
    void testReturnEmpty() {
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

    @Test
    void testAtTag() {
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

    @Test
    void testRemovesWholly() {
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

    @Test
    void testNoDoc() {
        final String nodoc = "class A {\n" +
                "    void foo(int a, int b) { }\n" +
                "}\n";
        assertEquals(nodoc, cleanCU(nodoc));
    }

}
