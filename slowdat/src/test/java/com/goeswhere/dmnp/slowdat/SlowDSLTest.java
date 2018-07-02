package com.goeswhere.dmnp.slowdat;

import com.goeswhere.dmnp.util.TestUtils;
import org.junit.jupiter.api.Test;

import static com.goeswhere.dmnp.util.TestUtils.cleanWhitespace;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SlowDSLTest {

    private static final String[] EMPTY = new String[0];

    @Test
    public void unreferenced() {
        assertDSLsTo("class A { void a() { int a = 5; int b;     System.out.println(a); } }",
                "class A { void a() { int a = 5; int b = 6; System.out.println(a); } }");
    }

    @Test
    public void allUsed() {
        assertDoesntChange("class A { void a() { int a = 5; int b = 6; System.out.println(a + b); } }");
    }

    @Test
    public void doubleBrackets() {
        assertDoesntChange("class A { void a() { int a = 5; System.out.println(((a))); } }");
    }

    @Test
    public void flow() {
        assertDoesntChange("class A { void a() { int a = 5; if (Math.random() > 7) a = 9; System.out.println(((a))); } }");
    }

    @Test
    public void flow2() {
        assertDSLsTo("class A { void a() { int a;     if (Math.random() > 7) a = 9; else a = 8; System.out.println(((a))); } }",
                "class A { void a() { int a = 5; if (Math.random() > 7) a = 9; else a = 8; System.out.println(((a))); } }");
    }

    @Test
    public void notSafe() {
        assertDoesntChange("class A { void a() throws Exception { boolean b = new java.io.File(\"\").createNewFile(); } }");
    }

    @Test
    public void notSafeInfixSpam() {
        assertDoesntChange("class A { void a() throws Exception { int a = 1 + 2 + " +
                "(new java.io.File(\"\").createNewFile() ? 1 : 2); } }");
    }

    @Test
    public void notSafeInfixSpam2() {
        assertDoesntChange("class A { void a() throws Exception { int a = 1 + 2 + 3 + 4 + " +
                "(new java.io.File(\"\").createNewFile() ? 5 : 6) + 7; } }");
    }

    @Test
    public void safeInfixSpam() {
        assertDSLsTo("class A { void a() { int a;                 } }",
                "class A { void a() { int a = 1 + 2 + 3 + 4; } }");
    }

    private void assertDoesntChange(String src) {
        assertDSLsTo(src, src);
    }

    private void assertDSLsTo(String expected, String actual) {
        assertEquals(cleanWhitespace(expected), cleanWhitespace(go(actual)));
    }

    private String go(String src) {
        return new SlowDSL(EMPTY, EMPTY, "A", TestUtils.EMPTY_LOCK).apply(src);
    }
}
