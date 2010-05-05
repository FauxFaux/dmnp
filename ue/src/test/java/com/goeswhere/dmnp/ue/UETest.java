package com.goeswhere.dmnp.ue;
import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.Ignore;
import org.junit.Test;

import com.goeswhere.dmnp.util.ASTWrapper;
import com.google.common.collect.ImmutableSet;


public class UETest {


	private static final String SAFE_METHOD = "void a() { " +
			"try { } catch(RuntimeException e) { l.warn(\"\", e);} }";

	// positives:
	@Test public void rethrow() {
		assertEquals(ImmutableSet.of(), go("throw a;"));
	}

	@Test public void logged() {
		assertEquals(ImmutableSet.of(), go("logger.warn(\"\", a);"));
	}

	@Test public void wrapped() {
		assertEquals(ImmutableSet.of(), go("throw new RuntimeException(a);"));
		assertEquals(ImmutableSet.of(), go("throw new RuntimeException(\"\", a);"));
	}

	@Test public void canadalae() {
		assertEquals(ImmutableSet.of(), go(
				"logger.warn(\"\", a);" +
				"throw new RuntimeException(a.toString());"));
	}

	@Test public void caps() {
		assertEquals(ImmutableSet.of(), goClass("class A { Logger loGGeR; void a() { " +
				"try { } catch (RuntimeException e) { loGGeR.warn(\"\", e); } } }"));
	}

	// negatives:
	@Test public void empty() {
		assertEquals(ImmutableSet.of("a"), go(""));
	}

	@Test public void badlogged() {
		assertEquals(ImmutableSet.of("a"), go("logger.warn(a);"));
	}

	@Test public void badthrown() {
		assertEquals(ImmutableSet.of("a"), go("throw new RuntimeException(a.toString());"));
	}

	// The If Hack:
	@Test public void cfg() {
		assertEquals(ImmutableSet.of("a"), go("if (false) logger.warn(\"\", a);"));
	}

	@Test public void cfgelse() {
		assertEquals(ImmutableSet.of("a"), go("if (false) logger.warn(\"\", a); else return;"));
	}

	@Test public void cfgonlyelse() {
		assertEquals(ImmutableSet.of("a"), go("if (false) return; else logger.warn(\"\", a);"));
	}

	@Test public void cfgneither() {
		assertEquals(ImmutableSet.of(), go("if (false) return; else return; logger.warn(\"\", a);"));
	}

	@Test public void fieldbefore() {
		assertEquals(ImmutableSet.of(), goClass("class A { Logger l; " + SAFE_METHOD + "}"));
	}

	@Test public void fieldafter() {
		assertEquals(ImmutableSet.of(), goClass("class A { " + SAFE_METHOD + " Logger l; }"));
	}

	@Test public void noLogger() {
		assertEquals(ImmutableSet.of("e"), goClass("class A { void a() { " +
				"try { } catch (RuntimeException e) { logger.warn(\"\", e); } } }"));
	}


	// Questionable.. shouldn't be a problem so long as all the source compiles;
	// it's not like anything else works on string manipulation anyway:
	@Test public void multiClass() {
		assertEquals(ImmutableSet.of(), goClass("class B { Logger l; } class A { " + SAFE_METHOD + " }"));
	}


	@Ignore("Not done yet")
	@Test public void throworlog() throws Exception {
		assertEquals(ImmutableSet.of(), go("if (false) throw a; logger.warn(\"\", a);}"));
	}

	private static Set<String> go(final String body) {
		return goClass("class A { Logger logger; void b() { try { } catch(RuntimeException a) { " + body + " } } }");
	}

	private static Set<String> goClass(final String contents) {
		final Set<String> res = new HashSet<String>();
		final CompilationUnit cu = ASTWrapper.compile(contents);
		final Reporter rep = new Reporter() {
			@Override public void report(CatchClause cc) {
				res.add(cc.getException().getName().getIdentifier());
			}
		};
		VisitCatchClauses.accept(cu, rep);
		return res;
	}
}
