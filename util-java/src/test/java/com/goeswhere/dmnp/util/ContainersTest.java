package com.goeswhere.dmnp.util;
import static com.goeswhere.dmnp.util.Containers.equal;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ContainersTest {
	@Test public void equate() {
		assertTrue(equal(null, null));

		final Object obj = new Object();
		assertFalse(equal(obj, null));
		assertFalse(equal(null, obj));
		assertTrue(equal(obj, obj));

		assertTrue(equal(Integer.valueOf(2837), Integer.valueOf(2837)));
	}
}
