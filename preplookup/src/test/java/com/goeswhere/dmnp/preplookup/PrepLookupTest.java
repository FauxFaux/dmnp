package com.goeswhere.dmnp.preplookup;

import static com.goeswhere.dmnp.util.TestUtils.cleanWhitespace;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.goeswhere.dmnp.util.TestUtils;

public class PrepLookupTest {
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

	@Test public void trivial() {
		assertPrepsTo(SUFFIX + "int a = 5; DLookupInt(\"\",\"\",\"a=?\", new Object[] { a }); } }",
					  SUFFIX + "int a = 5; DLookupInt(\"\",\"\",\"a=\" + a); } }");
	}

	@Test public void stringNotSafe() {
		assertDoesntChange(SUFFIX + "String a = null; DLookupInt(\"\",\"\",\"a=\" + a); } }");
	}

	@Test public void ignoreConsts() {
		assertDoesntChange(SUFFIX + "DLookupInt(\"\",\"\",\"a=\" + QQ); } }");
	}


	@Test public void dateFormat() {
		assertPrepsTo(SUFFIX + "Date d = new Date(); DLookupInt(\"\",\"\",\"a=?\", new Object[] { d }); } }",
					  SUFFIX + "Date d = new Date(); DLookupInt(\"\",\"\",\"a=\" + dateFormat(d)); } }");
		assertPrepsTo(SUFFIX + "Date d = new Date(); DLookupInt(\"\",\"\",\"a=?\" + \" AND b=?\", new Object[] { d, d }); } }",
				  SUFFIX + "Date d = new Date(); DLookupInt(\"\",\"\",\"a=\" + dateFormat(d) + \" AND b=\" + dateFormat(d)); } }");
	}

	@Test public void pickyAboutFunctions() {
		assertDoesntChange(SUFFIX + "Date d = new Date(); DLookupInt(\"\",\"\",\"a=\" + foo(d)); } }");
	}

	@Test public void multiple() {
		assertPrepsTo(SUFFIX + "int a = 5; DLookupInt(\"\",\"\",\"a=?\" + \" AND b<>?\"," +
							" new Object[] { a, bar() }); } }",
					  SUFFIX + "int a = 5; DLookupInt(\"\",\"\",\"a=\" + a + \" AND b<>\" + bar()); } }");
	}

	@Test public void litOnly() {
		assertPrepsTo(SUFFIX + "String a = null; DLookupInt(\"\",\"\",\"a=?\"," +
							" new Object[] { a }); } }",
					  SUFFIX + "String a = null; DLookupInt(\"\",\"\",\"a='\" + safeSQL(a) + \"'\"); } }");
	}

	@Test public void wrongSafeSql() {
		assertDoesntChange(SUFFIX + "String a = null; DLookupInt(\"\",\"\",\"a=\" + safeSQL(a)); } }");
		assertDoesntChange(SUFFIX + "String a = null; DLookupInt(\"\",\"\",\"a=\" + safeSQL(a) + \" and b=5\"); } }");
	}

	@Test public void otherFunc() {
		assertPrepsTo("pony", 0,
				SUFFIX + "String a = null; ponyBadger(\"a=?\"," +
							" new Object[] { a }); } }",
					  SUFFIX + "String a = null; ponyBadger(\"a='\" + safeSQL(a) + \"'\"); } }");
	}


	private void assertDoesntChange(String src) {
 		assertPrepsTo(src, src);
 	}

	private void assertPrepsTo(String expected, String actual) {
		assertPrepsTo("DLookup", 2, expected, actual);
	}

	private void assertPrepsTo(String func, int i, String expected, String actual) {
		assertEquals(cleanWhitespace(expected), cleanWhitespace(go(func, i, actual)));
	}

	private String go(String func, int i, String src) {
		return new PrepLookup(EMPTY, EMPTY, "A", TestUtils.EMPTY_LOCK, func, i).apply(src);
	}
}
