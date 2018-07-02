package com.goeswhere.dmnp.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContainersTest {
    @Test
    public void testHRList() {
        assertEquals("1", Containers.hrList(Collections.singletonList(1)));
        assertEquals("1 and 2", Containers.hrList(Arrays.asList(1, 2)));
        assertEquals("1, 2 and 3", Containers.hrList(Arrays.asList(1, 2, 3)));
        assertEquals("1, 2, 3 and 4", Containers.hrList(Arrays.asList(1, 2, 3, 4)));
    }
}
