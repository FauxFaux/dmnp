package com.goeswhere.dmnp.util;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;


/**
 * Various utilities for converting source to one of the object representations.
 */
public class ASTWrapperTest {
    @Test
    public void compile() {
        assertTopLevelTypeCalledC("class C { }");
    }

    @Test
    public void compileEnum() {
        assertTopLevelTypeCalledC("enum C { FOO, BAR }");
    }

    @Test(expected = IllegalArgumentException.class)
    public void compileError() {
        ASTWrapper.compile("class C {");
    }

    private static void assertTopLevelTypeCalledC(final String c) {
        final CompilationUnit cu = ASTWrapper.compile(c);
        assertEquals(Arrays.asList("C"),
                Lists.transform(ASTContainers.types(cu),
                        from -> from.getName().getIdentifier()));
    }


    @Test
    public void extractMethod() {
        assertEquals("foo", ASTWrapper.extractSingleMethod("void foo() {}").getName().getIdentifier());
    }

    @Test(expected = IllegalArgumentException.class)
    public void extractMethodNotThere() {
        ASTWrapper.extractSingleMethod("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void extractMethodTwo() {
        ASTWrapper.extractSingleMethod("void foo() {} void bar() {}");
    }

    @Test
    public void signature() {
        final String nam = "private static Class<? extends Object> " +
                "foo(String s, Integer a, Map<String,Long> m)";
        assertEquals(nam, ASTWrapper.signature(ASTWrapper.extractSingleMethod(nam + " {}")));
    }

}
