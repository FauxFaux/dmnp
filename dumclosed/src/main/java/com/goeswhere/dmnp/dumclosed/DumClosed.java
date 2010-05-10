package com.goeswhere.dmnp.dumclosed;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import com.goeswhere.dmnp.util.ASTContainers;
import com.goeswhere.dmnp.util.ASTWrapper;
import com.goeswhere.dmnp.util.FileUtils;
import com.google.common.collect.Lists;

public class DumClosed {
	private final String type;
	private final String method;

	private class Marker extends ASTVisitor {
		private final Map<String, Badness> names;
		private final Badness level;

		private Marker(Map<String, Badness> names, Badness level) {
			this.names = names;
			this.level = level;
		}

		@Override public boolean visit(MethodInvocation inv) {
			if (isClose(inv)) {
				final Expression exp = inv.getExpression();
				if (exp instanceof SimpleName) {
					final String name = ((SimpleName) exp).getIdentifier();
					if (names.containsKey(name))
						names.put(name, level);
				}
			}
			return true;
		}
	}

	private static enum Badness {
		REALLY, QUITE, NOT
	}

	public DumClosed(String type, String method) {
		this.type = type;
		this.method = method;
	}

	public static void main(String[] args) throws IOException {
		if (3 != args.length) {
			System.out.println("Usage: RecordSet close path/to/src");
			System.exit(1);
			return;
		}

		final List<Problem> prrs = Lists.newArrayList(
				new DumClosed(args[0], args[1]).execute(args[2]));

		Collections.sort(prrs, new Comparator<Problem>() {
			@Override public int compare(Problem o1, Problem o2) {
				return o1.badness.compareTo(o2.badness);
			}
		});

		for (Problem p : prrs)
			System.out.println(p.badness + " bad: " + p);

		System.out.println(prrs.size());
	}

	private List<Problem> execute(String basePath) throws IOException {
		final List<Problem> ret = Lists.newArrayList();

		for (final File f : FileUtils.javaFilesIn(basePath)) {
			final String contents = FileUtils.consumeFile(f);

			// optimisation
			if (!contents.contains(type))
				continue;

			final CompilationUnit cu = ASTWrapper.compile(contents);
			cu.accept(new ASTVisitor() {
				@Override public boolean visit(final MethodDeclaration meth) {
					meth.accept(new ASTVisitor() {
						@Override public boolean visit(VariableDeclarationStatement ty) {
							if (!(ty.getType() instanceof SimpleType) || !isRS((SimpleType) ty.getType()))
								return true;

							final Map<String, Badness> names = new HashMap<String, Badness>();
							for (VariableDeclarationFragment a : ASTContainers.it(ty))
								names.put(a.getName().getIdentifier(), Badness.REALLY);

							meth.accept(new Marker(names, Badness.QUITE));

							meth.accept(new ASTVisitor() {
								@Override public boolean visit(TryStatement tc) {
									final Block fin = tc.getFinally();
									if (null == fin)
										return true;

									fin.accept(new Marker(names, Badness.NOT));
									return true;
								}
							});

							for (Entry<String, Badness> s : names.entrySet())
								if (s.getValue() != Badness.NOT)
									report(ret, f, cu, ty, s.getKey(), s.getValue());

							return true;
						}

					});
					return super.visit(meth);
				}
			});
		}
		return ret;
	}

	private boolean isRS(SimpleType ty) {
		final Name n = ty.getName();
		return n instanceof SimpleName && ((SimpleName) n).getIdentifier().equals(type);
	}

	private static class Problem {
		final String link;
		final String var;
		final String meth;
		final Badness badness;

		Problem(String link, String var, String meth, Badness badness) {
			this.link = link;
			this.var = var;
			this.meth = meth;
			this.badness = badness;
		}

		@Override public String toString() {
			return link + ": " + var + " in " + meth;
		}
	}

	private static void report(Collection<Problem> probs, final File f, final CompilationUnit cu,
			VariableDeclarationStatement ty, String name, Badness badness) {
		final String link = "(" + f.getName() + ":" + cu.getLineNumber(ty.getStartPosition()) + ")";
		final String where = ASTWrapper.methodName(ty);
		probs.add(new Problem(link, name, where, badness));
	}

	private boolean isClose(MethodInvocation inv) {
		return inv.getName().getIdentifier().equals(method);
	}
}
