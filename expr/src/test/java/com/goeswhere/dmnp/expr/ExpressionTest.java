package com.goeswhere.dmnp.expr;

import com.google.common.base.Predicate;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class ExpressionTest {

    @Test
    public void testSingleInt() throws IOException {
        assertEquals("(a = 7)",
                Expression.toSQL(new Predicate<FooDTO>() {
                    @Override
                    public boolean apply(final FooDTO t) {
                        return t.a == 7;
                    }
                }));
    }

    @Test
    public void testNotEqualInt1() throws IOException {
        assertEquals("(a <> 7)",
                Expression.toSQL(new Predicate<FooDTO>() {
                    @Override
                    public boolean apply(final FooDTO t) {
                        return t.a != 7;
                    }
                }));
    }

    @Test
    public void testNotEqualInt2() throws IOException {
        assertEquals("(a <> 7)",
                Expression.toSQL(new Predicate<FooDTO>() {
                    @Override
                    public boolean apply(final FooDTO t) {
                        return !(t.a == 7);
                    }
                }));
    }

    @Test
    public void testNotEqualInt3() throws IOException {
        assertEquals("(a <> 7)",
                Expression.toSQL(new Predicate<FooDTO>() {
                    @Override
                    public boolean apply(final FooDTO t) {
                        if (t.a == 7)
                            return false;
                        return true;
                    }
                }));
    }

    @Test
    public void testSingleString() throws IOException {
        assertEquals("(b = 'pony')",
                Expression.toSQL(new Predicate<FooDTO>() {
                    @Override
                    public boolean apply(final FooDTO t) {
                        return t.b.equals("pony");
                    }
                }));
    }

    @Test
    public void testSingleStringRev() throws IOException {
        assertEquals("(b = 'pony')",
                Expression.toSQL(new Predicate<FooDTO>() {
                    @Override
                    public boolean apply(final FooDTO t) {
                        return "pony".equals(t.b);
                    }
                }));
    }

    @Test
    public void testStringRef() throws IOException {
        assertEquals("(b = 'pony')",
                Expression.toSQL(new Predicate<FooDTO>() {
                    @Override
                    public boolean apply(final FooDTO t) {
                        return t.b == "pony";
                    }
                }));
    }

    @Test
    public void testStringRefRev() throws IOException {
        assertEquals("(b = 'pony')",
                Expression.toSQL(new Predicate<FooDTO>() {
                    @Override
                    public boolean apply(final FooDTO t) {
                        return "pony" == t.b;
                    }
                }));
    }

    @Test
    public void testStringNotRef() throws IOException {
        assertEquals("(b <> 'pony')",
                Expression.toSQL(new Predicate<FooDTO>() {
                    @Override
                    public boolean apply(final FooDTO t) {
                        return "pony" != t.b;
                    }
                }));
    }

    @Test
    public void testSingleStringNot() throws IOException {
        assertEquals("(b = 'pony')",
                Expression.toSQL(new Predicate<FooDTO>() {
                    @Override
                    public boolean apply(final FooDTO t) {
                        return "pony".equals(t.b);
                    }
                }));
    }

    @Test
    public void testAnd() throws IOException {
        assertEquals("(a > 7 AND a <= 20)",
                Expression.toSQL(new Predicate<FooDTO>() {
                    @Override
                    public boolean apply(final FooDTO t) {
                        return t.a > 7 && t.a <= 20;
                    }
                }));
    }

    @Test
    public void testAndString() throws IOException {
        assertEquals("(a = 7 AND b = 'pony')",
                Expression.toSQL(new Predicate<FooDTO>() {
                    @Override
                    public boolean apply(final FooDTO t) {
                        return t.a == 7 && "pony".equals(t.b);
                    }
                }));
    }

    @Test
    public void testOrAndSingle() throws IOException {
        assertEquals("(a = 7 AND b = 'pony') OR\n" +
                        "(a = 8 AND b = 'pony') OR\n" +
                        "(a = 9 AND b = 'pony')",
                Expression.toSQL(new Predicate<FooDTO>() {
                    @Override
                    public boolean apply(final FooDTO t) {
                        return (t.a == 7 || t.a == 8 || t.a == 9) && "pony".equals(t.b);
                    }
                }));
    }

    @Test
    public void testOrAndMulti() throws IOException {
        assertEquals("(a = 7 AND c = 5) OR\n" +
                        "(a = 8 AND c = 5) OR\n" +
                        "(a = 7 AND c = 6) OR\n" +
                        "(a = 8 AND c = 6)",
                Expression.toSQL(new Predicate<FooDTO>() {
                    @Override
                    public boolean apply(final FooDTO t) {
                        return (t.a == 7 || t.a == 8) && (t.c == 5 || t.c == 6);
                    }
                }));
    }

    static class FooDTO {
        public int a, c;
        public String b;
    }
}
