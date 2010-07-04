package com.goeswhere.dmnp.conclass;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import com.goeswhere.dmnp.util.ASTContainers;
import com.goeswhere.dmnp.util.ASTWrapper;
import com.goeswhere.dmnp.util.FileUtils;
import com.google.common.collect.Sets;

public class ConClass {
	public static void main(String[] args) throws InterruptedException, IOException {
		long start = System.nanoTime();
		System.out.println(go(args[0], args[1]));
		System.out.println((System.nanoTime() - start) / 1e9);
	}

	static String go(final String classname, final String path) throws InterruptedException, IOException {
		final ConcurrentHashMap<TypeDeclaration, File> candidates = new ConcurrentHashMap<TypeDeclaration, File>();
		final Set<String> used = findUses(classname, path, candidates);

		switch (candidates.size()) {
		case 0:
			throw new IllegalArgumentException("Couldn't find class type");
		default:
			throw new IllegalArgumentException("Too many candidate types for that classname: " + candidates.values());
		case 1:
			break;
		}

		final Entry<TypeDeclaration, File> theclass = candidates.entrySet().iterator().next();
		final Set<String> eliminatable = findEliminatables(theclass.getKey(), used);

		final String cus = FileUtils.consumeFile(new FileReader(theclass.getValue()));
		final CompilationUnit cu = ASTWrapper.compile(cus);
		cu.recordModifications();

		final Document doc = new Document(cus);
		cu.accept(new ASTVisitor() {
			@Override public boolean visit(FieldDeclaration node) {
				for (Iterator<VariableDeclarationFragment> it = ASTContainers.fragments(node).iterator(); it.hasNext();)
					if (eliminatable.contains(it.next().getName().getIdentifier()))
						it.remove();
				if (node.fragments().isEmpty())
					node.delete();
				return super.visit(node);
			}
		});

		try {
			cu.rewrite(doc, null).apply(doc, TextEdit.UPDATE_REGIONS);
		} catch (MalformedTreeException e) {
			throw new RuntimeException(e);
		} catch (BadLocationException e) {
			throw new RuntimeException(e);
		}

		return doc.get();
	}

	private static Set<String> findEliminatables(final TypeDeclaration theclass, final Set<String> used) {
		final Set<String> eliminatable = Sets.newHashSet();
		theclass.accept(new ASTVisitor() {
			@Override public boolean visit(FieldDeclaration node) {
				for (VariableDeclarationFragment f : ASTContainers.fragments(node)) {
					final String identifier = f.getName().getIdentifier();
					if (!used.contains(identifier) && !identifier.matches(".*\\d+(?:$|_.*)"))
						eliminatable.add(identifier);
				}
				return super.visit(node);
			}
		});
		return eliminatable;
	}

	/** @param candidates Places where the named class could be declared. */
	private static Set<String> findUses(final String classname, final String path,
			final ConcurrentHashMap<TypeDeclaration, File> candidates) throws InterruptedException {
		final Set<String> used = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

		final ExecutorService ex = Executors.newFixedThreadPool(10);

		for (final File f : FileUtils.javaFilesIn(path))
			ex.submit(new Runnable() {
				@Override public void run() {
					final String contents;
					try {
						contents = FileUtils.consumeFile(new FileReader(f));
					} catch (IOException e) {
						throw new RuntimeException(e);
					}

					if (!contents.contains(classname))
						return;

					final CompilationUnit cu = ASTWrapper.compile(contents);
					cu.accept(new ASTVisitor() {
						@Override public boolean visit(QualifiedName node) {
							final Name qualifier = node.getQualifier();
							if (qualifier instanceof SimpleName) {
								final String identifier = ((SimpleName) qualifier).getIdentifier();
								if (classname.equals(identifier))
									used.add(node.getName().getIdentifier());
							}
							return super.visit(node);
						}

						@Override public boolean visit(TypeDeclaration node) {
							if (classname.equals(node.getName().getIdentifier()))
								candidates.put(node, f);
							return super.visit(node);
						}
					});
				}
			});

		ex.shutdown();
		ex.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		return used;
	}
}
