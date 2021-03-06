package com.goeswhere.dmnp.util;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.goeswhere.dmnp.util.FJava.cons;
import static com.goeswhere.dmnp.util.FJava.intersperse;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

class FJavaTest {

    @Test
    void testCons() {
        assertEquals(Collections.singletonList("5"), Lists.newArrayList(cons("5", Collections.emptyList())));
        assertEquals(asList("5", "7"), Lists.newArrayList(cons("5", Collections.singletonList("7"))));
        assertEquals(Arrays.asList("5", "6", "7"), Lists.newArrayList(cons("5", asList("6", "7"))));
        assertEquals(Arrays.asList("5", "7", "6"), Lists.newArrayList(cons("5", cons("7", cons("6", Collections.emptyList())))));
    }

    @Test
    void testIntersperse() {
        assertEquals("a", intersperse(Collections.singletonList("a"), ","));
        assertEquals("a,b,c", intersperse(Arrays.asList("a", "b", "c"), ","));
    }

    @Test
    void testNRepeats() {
        assertEquals(Arrays.asList(1, 4, 4, 4, 4, 2, 2), Lists.newArrayList(
                FJava.concatMap(Arrays.asList(1, 0, 4, 0, 2), from -> {
                    final List<Integer> ret = Lists.newArrayListWithCapacity(from);
                    for (int i = 0; i < from; ++i)
                        ret.add(from);
                    return ret;
                })));
    }


    @Test
    void testReducer() {
        assertEquals(Arrays.asList('x', 'y', 'z', 'a', 'b', 'c'),
                FJava.reducer('x', from -> {
                    switch (from) {
                        case 'x':
                            return Arrays.asList('y', 'z');
                        case 'z':
                            return Collections.singletonList('a');
                        case 'a':
                            return Arrays.asList('b', 'c');
                        default:
                            return Collections.emptyList();
                    }
                }));
    }
}
