package com.goeswhere.dmnp.preplookup;

import com.goeswhere.dmnp.util.TestUtils;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.goeswhere.dmnp.util.TestUtils.cleanWhitespace;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PrepLookupTest {
    private static final String SUFFIX = "import java.util.Date;" +
            "class A {" +
            " void DLookupInt(String q, String r, String s) {}" +
            " void ponyBadger(String s) { }" +
            " String dateFormat(Date d) { return null; }" +
            " String foo(Date d) { return null; }" +
            " String safeSQL(String s) { return null; }" +
            " private static final int QQ = 5;" +
            " int bar() { return 0; }" +
            " void a() { ";
    private static final String[] EMPTY = new String[0];

    @Test
    void trivial() {
        assertPrepsTo(SUFFIX + "int a = 5; DLookupInt(\"\",\"\",\"a=?\", new Object[] { a }); } }",
                SUFFIX + "int a = 5; DLookupInt(\"\",\"\",\"a=\" + a); } }");
    }

    @Test
    void stringNotSafe() {
        assertDoesntChange(SUFFIX + "String a = null; DLookupInt(\"\",\"\",\"a=\" + a); } }");
    }

    @Test
    void ignoreConsts() {
        assertDoesntChange(SUFFIX + "DLookupInt(\"\",\"\",\"a=\" + QQ); } }");
    }


    @Test
    void dateFormat() {
        assertPrepsTo(SUFFIX + "Date d = new Date(); DLookupInt(\"\",\"\",\"a=?\", new Object[] { d }); } }",
                SUFFIX + "Date d = new Date(); DLookupInt(\"\",\"\",\"a=\" + dateFormat(d)); } }");
        assertPrepsTo(SUFFIX + "Date d = new Date(); DLookupInt(\"\",\"\",\"a=?\" + \" AND b=?\", new Object[] { d, d }); } }",
                SUFFIX + "Date d = new Date(); DLookupInt(\"\",\"\",\"a=\" + dateFormat(d) + \" AND b=\" + dateFormat(d)); } }");
    }

    @Test
    void pickyAboutFunctions() {
        assertDoesntChange(SUFFIX + "Date d = new Date(); DLookupInt(\"\",\"\",\"a=\" + foo(d)); } }");
    }

    @Test
    void multiple() {
        assertPrepsTo(SUFFIX + "int a = 5; DLookupInt(\"\",\"\",\"a=?\" + \" AND b<>?\"," +
                        " new Object[] { a, bar() }); } }",
                SUFFIX + "int a = 5; DLookupInt(\"\",\"\",\"a=\" + a + \" AND b<>\" + bar()); } }");
    }

    @Test
    void litOnly() {
        assertPrepsTo(SUFFIX + "String a = null; DLookupInt(\"\",\"\",\"a=?\"," +
                        " new Object[] { a }); } }",
                SUFFIX + "String a = null; DLookupInt(\"\",\"\",\"a='\" + safeSQL(a) + \"'\"); } }");
    }

    @Test
    void wrongSafeSql() {
        assertDoesntChange(SUFFIX + "String a = null; DLookupInt(\"\",\"\",\"a=\" + safeSQL(a)); } }");
        assertDoesntChange(SUFFIX + "String a = null; DLookupInt(\"\",\"\",\"a=\" + safeSQL(a) + \" and b=5\"); } }");
    }

    @Test
    void dontDamageNoChangesQuotes() {
        assertDoesntChange(SUFFIX + "int a = 0; String b = null;"
                + "DLookupInt(\"\",\"\",\"a='\" + a + \"' AND b=\" + b); } }");
    }


    @Test
    void otherFunc() {
        assertPrepsTo(ImmutableMap.of("pony", 0),
                SUFFIX + "String a = null; ponyBadger(\"a=?\"," +
                        " new Object[] { a }); } }",
                SUFFIX + "String a = null; ponyBadger(\"a='\" + safeSQL(a) + \"'\"); } }");
    }

    @Test
    void quotes() {
        assertPrepsTo(SUFFIX + "int a = 5; DLookupInt(\"\",\"\",\"b='b' AND a=?\", new Object[] { a }); } }",
                SUFFIX + "int a = 5; DLookupInt(\"\",\"\",\"b='b' AND a=\" + a); } }");

        assertPrepsTo(SUFFIX + "int a = 5; String b = null; DLookupInt(\"\",\"\",\"c='c'" +
                        " AND b=?\" + \" AND a=?\", new Object[] { b, a }); } }",
                SUFFIX + "int a = 5; String b = null; DLookupInt(\"\",\"\",\"c='c'" +
                        " AND b='\" + safeSQL(b) + \"' AND a=\" + a); } }");
    }

    private void assertDoesntChange(String src) {
        assertPrepsTo(src, src);
    }

    private void assertPrepsTo(String expected, String actual) {
        assertPrepsTo(ImmutableMap.of("DLookup", 2), expected, actual);
    }

    private void assertPrepsTo(Map<String, Integer> func, String expected, String actual) {
        assertEquals(cleanWhitespace(expected), cleanWhitespace(go(func, actual)));
    }

    private String go(Map<String, Integer> func, String src) {
        return new PrepLookup(EMPTY, EMPTY, "A", TestUtils.EMPTY_LOCK, func).apply(src);
    }
}
