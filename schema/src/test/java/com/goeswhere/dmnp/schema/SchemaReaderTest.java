package com.goeswhere.dmnp.schema;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.goeswhere.dmnp.schema.SchemaReader.Outputter;
import com.google.common.collect.Lists;

public class SchemaReaderTest {
	static class ListOutputter implements Outputter {
		private final List<String> res = Lists.newArrayList();

		@Override public void output(String s) {
			res.add(s);
		}
	}

	@Test public void javadoc() {
		final ListOutputter out = new ListOutputter();

		SchemaReader.go("class Foo {" +
				" void dbupdateversion4_0() { if (a <= 1) {} if (a <= 2) {} }" +
				" void dbupdateversion4_1() { if (a <= 1) {} if (a <= 2) {} if (a <= 3) {} }" +
				"}", out);

		assertEquals(out.res, Arrays.asList("dbupdateversion4_0=2", "dbupdateversion4_1=3"));
	}

	@Test public void tryblock() {
		final ListOutputter out = new ListOutputter();

		SchemaReader.go("class Foo {" +
				" void dbupdateversion3_0() {" +
				"  try {" +
				"   if (a <= 1) {}" +
				"   if (a <= 2) {} " +
				"  } catch (Exception e) {" +
				"  }" +
				" }" +
				"}", out);

		assertEquals(out.res, Arrays.asList("dbupdateversion3_0=2"));
	}
}
