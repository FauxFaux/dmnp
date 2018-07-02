package com.goeswhere.dmnp.util;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static com.goeswhere.dmnp.util.DumbParsing.packageOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DumbParsingTest {
    @Test
    public void testPackageOfSimple() throws IOException {
        assertPackageOf("com.pony.ponies", "package com.pony.ponies;\nclass A {\n\tint a;\n}\n");
    }

    @Test
    public void testPackageOfComment() throws IOException {
        assertPackageOf("pony", "/** COPYRIGHT */\npackage pony;\nclass A {\n\tint a;\n}\n");
    }

    @Test
    public void testPackageOfLineComment() throws IOException {
        assertPackageOf("pony", "// COPYRIGHT\npackage pony;\nclass A {\n\tint a;\n}\n");
    }

    @Test
    public void testPackageOfu() throws IOException {
        assertPackageOf("pony", "// u\npackage pony;\nclass A {\n\tint a;\n}\n");
    }

    @Test
    public void testPackageOfu1571() throws IOException {
        assertPackageOf("pon\u1571", "package pon\\u1571;\nclass A {\n\tint a;\n}\n");
    }

    @Test
    public void testPackageOfNone() throws IOException {
        assertPackageOf(null, "class A {\n\tint a;\n}\n");
    }

    @Test
    public void testPackageOfCpp() throws IOException {
        assertPackageOf("pony", "//\\u000a/*\n" +
                "c++\n" +
                "//*/\n" +
                "\\u002a\n" +
                "package pony;\n" +
                "class A {\n" +
                "\tint a;\n" +
                "}\n");
    }

    private void assertPackageOf(final String exp, final String inp) throws IOException {
        assertEquals(exp, packageOf(new BufferedReader(new StringReader(inp))));
    }
}
