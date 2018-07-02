package com.goeswhere.dmnp.util;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Various utilities for converting source to one of the object representations.
 */
class ASTWrapperTest {
    @Test
    void compile() {
        assertTopLevelTypeCalledC("class C { }");
    }

    @Test
    void compileEnum() {
        assertTopLevelTypeCalledC("enum C { FOO, BAR }");
    }

    @Test
    void compileError() {
        assertThrows(IllegalArgumentException.class, () ->
                ASTWrapper.compile("class C {"));
    }

    private static void assertTopLevelTypeCalledC(final String c) {
        final CompilationUnit cu = ASTWrapper.compile(c);
        assertEquals(Collections.singletonList("C"),
                ASTContainers.types(cu).stream().map(from -> from.getName().getIdentifier()).collect(Collectors.toList()));
    }


    @Test
    void extractMethod() {
        assertEquals("foo", ASTWrapper.extractSingleMethod("void foo() {}").getName().getIdentifier());
    }

    @Test
    void extractMethodNotThere() {

        assertThrows(IllegalArgumentException.class, () ->
                ASTWrapper.extractSingleMethod(""));
    }

    @Test
    void extractMethodTwo() {
        ASTWrapper.extractSingleMethod("void foo() {} void bar() {}");
    }

    @Test
    void signature() {
        final String nam = "private static Class<? extends Object> " +
                "foo(String s, Integer a, Map<String,Long> m)";
        assertThrows(IllegalArgumentException.class, () -> assertEquals(nam, ASTWrapper.signature(ASTWrapper.extractSingleMethod(nam + " {}"))));
    }

}
