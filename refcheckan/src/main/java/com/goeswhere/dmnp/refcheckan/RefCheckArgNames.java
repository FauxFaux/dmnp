package com.goeswhere.dmnp.refcheckan;

import static com.goeswhere.dmnp.util.FileUtils.consumeFile;
import static com.goeswhere.dmnp.util.FileUtils.javaFilesIn;
import static com.goeswhere.dmnp.util.FileUtils.sysSplit;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import com.goeswhere.dmnp.util.ASTWrapper;
import com.goeswhere.dmnp.util.Containers;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public class RefCheckArgNames {

	protected static String flatten(MethodInvocation node) {
		return node.getName().getIdentifier() + "#" + node.arguments().size();
	}

	protected static String flatten(MethodDeclaration node) {
		return node.getName().getIdentifier() + "#" + node.parameters().size();
	}

	protected static String flatten(ClassInstanceCreation node) {
		return node.getType().toString() + "#" + node.arguments().size();
	}

	protected static String flatten(ConstructorInvocation node) {
		final MethodDeclaration callingMethod = (MethodDeclaration) node.getParent().getParent();
		return callingMethod.getName().getIdentifier() + "#" + node.arguments().size();
	}

	protected static String flatten(SuperConstructorInvocation node) {
		final TypeDeclaration td = (TypeDeclaration) node.getParent().getParent().getParent();
		return String.valueOf(td.getSuperclassType()) + "#" + node.arguments().size();
	}

	public static void main(String[] args) throws InterruptedException {
		final String[] sourcePath = sysSplit(args[0]);
		final SetMultimap<String, MethodDeclaration> methodLocations =
			Multimaps.synchronizedSetMultimap(HashMultimap.<String, MethodDeclaration>create());
		final Set<String> calledMethods = Containers.newConcurrentHashSet();
		final Map<MethodDeclaration, String> location = Containers.newConcurrentHashMap();

		final ExecutorService ex = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
		for (String srcPath : sourcePath)
			for (final File file : javaFilesIn(srcPath))
				ex.submit(new Callable<Void>() {
					@Override public Void call() throws IOException {
				final CompilationUnit cu = ASTWrapper.compile(consumeFile(file));
				cu.accept(new ASTVisitor() {
					@Override public boolean visit(MethodInvocation node) {
						calledMethods.add(flatten(node));
						return true;
					}

					@Override public boolean visit(ClassInstanceCreation node) {
						calledMethods.add(flatten(node));
						return true;
					}

					@Override public boolean visit(ConstructorInvocation node) {
						calledMethods.add(flatten(node));
						return true;
					}

					@Override public boolean visit(SuperConstructorInvocation node) {
						calledMethods.add(flatten(node));
						return true;
					}

					@Override public boolean visit(MethodDeclaration node) {
						methodLocations.put(flatten(node), node);
						location.put(node, file.getName() + ":" + cu.getLineNumber(node.getName().getStartPosition()));
						return true;
					}
				});
				return null;
			}});

		ex.shutdown();
		ex.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

		final HashSet<String> u = Sets.newHashSet(methodLocations.keys());
		u.removeAll(calledMethods);
		List<MethodDeclaration> dodgy = Lists.newArrayList();
		for (String s : u) {
			final Set<MethodDeclaration> decls = methodLocations.get(s);
			if (1 != decls.size())
				continue;
			MethodDeclaration d = decls.iterator().next();
			dodgy.add(d);
		}

		Collections.sort(dodgy, new Comparator<MethodDeclaration>() {
			@Override public int compare(MethodDeclaration o1, MethodDeclaration o2) {
				return size(o1) - size(o2);
			}
		});

		SetMultimap<String, MethodDeclaration> perClass = HashMultimap.create();
		for (MethodDeclaration d : dodgy) {
			final String cla = classOf(d);
			if (!cla.endsWith("TO") && !cla.endsWith("DAO"))
				perClass.put(cla, d);
		}
		List<Entry<String, Collection<MethodDeclaration>>> l = Lists.newArrayList(perClass.asMap().entrySet());
		Collections.sort(l, new Comparator<Entry<String, Collection<MethodDeclaration>>>() {
			@Override public int compare(Entry<String, Collection<MethodDeclaration>> o1,
					Entry<String, Collection<MethodDeclaration>> o2) {
				return size(o1.getValue()) - size(o2.getValue());
			}
		});

		int q = 0;
		for (Entry<String, Collection<MethodDeclaration>> a : l) {
			for (MethodDeclaration d : sorted(a.getValue())) {
				++q;
				System.out.println(size(d) + " (" + location.get(d) + ") " + ASTWrapper.signature(d));
			}
		}
		System.out.println(q);
	}

	private static Collection<MethodDeclaration> sorted(final Collection<MethodDeclaration> value) {
		List<MethodDeclaration> v = Lists.newArrayList(value);
		Collections.sort(v, new Comparator<MethodDeclaration>() {
			@Override public int compare(MethodDeclaration o1, MethodDeclaration o2) {
				return o1.getStartPosition() - o2.getStartPosition();
			}
		});
		return v;
	}

	private static int size(final Iterable<? extends MethodDeclaration> value) {
		int size = 0;
		for (MethodDeclaration d : value)
			size += size(d);
		return size;
	}

	private static String classOf(MethodDeclaration b) {
		ASTNode n = b;
		while (!((n = n.getParent()) instanceof AbstractTypeDeclaration)) {
			// nothing at all
		}

		return ((AbstractTypeDeclaration)n).getName().getIdentifier();
	}

	private static int size(MethodDeclaration d) {
		return d.getLength();
	}
}

