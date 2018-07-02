package com.goeswhere.dmnp.util;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.goeswhere.dmnp.util.FJava.cons;
import static com.goeswhere.dmnp.util.FJava.intersperse;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class FJavaTest {

    @Test
    public void testCons() {
        assertEquals(asList("5"), Lists.newArrayList(cons("5", asList())));
        assertEquals(asList("5", "7"), Lists.newArrayList(cons("5", asList("7"))));
        assertEquals(Arrays.asList("5", "6", "7"), Lists.newArrayList(cons("5", asList("6", "7"))));
        assertEquals(Arrays.asList("5", "7", "6"), Lists.newArrayList(cons("5", cons("7", cons("6", asList())))));
    }

    @Test
    public void testIntersperse() {
        assertEquals("a", intersperse(Arrays.asList("a"), ","));
        assertEquals("a,b,c", intersperse(Arrays.asList("a", "b", "c"), ","));
    }

    @Test
    public void testNRepeats() {
        assertEquals(Arrays.asList(1, 4, 4, 4, 4, 2, 2), Lists.newArrayList(
                FJava.concatMap(Arrays.asList(1, 0, 4, 0, 2), from -> {
                    final List<Integer> ret = Lists.newArrayListWithCapacity(from);
                    for (int i = 0; i < from; ++i)
                        ret.add(from);
                    return ret;
                })));
    }


    @Test
    public void testReducer() {
        assertEquals(Arrays.asList('x', 'y', 'z', 'a', 'b', 'c'),
                FJava.reducer('x', from -> {
                    switch (from) {
                        case 'x':
                            return Arrays.asList('y', 'z');
                        case 'z':
                            return Arrays.asList('a');
                        case 'a':
                            return Arrays.asList('b', 'c');
                        default:
                            return Collections.emptyList();
                    }
                }));
    }
}
