package com.goeswhere.dmnp.doccrap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

import com.goeswhere.dmnp.util.ASTContainers;
import com.goeswhere.dmnp.util.ASTWrapper;
import com.goeswhere.dmnp.util.FileUtils;
import com.goeswhere.dmnp.util.TerribleImplementation;

public class DocCrap {
	static String cleanCU(final String cus) {
		final CompilationUnit cu = ASTWrapper.compile(cus);
		cu.recordModifications();

		final Document doc = new Document(cus);

		cu.accept(new ASTVisitor() {
			@Override public boolean visit(final MethodDeclaration meth) {
				final Javadoc javadoc = meth.getJavadoc();
				if (null == javadoc)
					return super.visit(meth);

				final List<TagElement> tags = ASTContainers.tags(javadoc);
				final Map<String, String> args = methodArgs(meth);
				for (final Iterator<TagElement> it = tags.iterator(); it.hasNext();) {
					final TagElement te = it.next();
					final String tagName = te.getTagName();
					final List<ASTNode> frags = ASTContainers.fragments(te);

					if ("@param".equals(tagName)) {

						// param with no name?!
						if (frags.isEmpty()) {
							continue;
						}

						// name that's no name
						final ASTNode nameNode = frags.get(0);
						if (!(nameNode instanceof SimpleName))
							continue;

						// arg doesn't even exist
						final String ident = ((SimpleName) nameNode).getIdentifier();
						if (!args.containsKey(ident)) {
							it.remove();
							continue;
						}

						// contains only the name
						if (1 == frags.size()) {
							it.remove();
							continue;
						}

						final ASTNode a = frags.get(1);
						if (a instanceof TextElement) {
							final String text = ((TextElement) a).getText().trim();
							if ("".equals(text)
									|| "-".equals(text)
									|| (args.get(ident) + " -").equals(text)) {
								it.remove();
								continue;
							}
						}
					} else if ("@return".equals(tagName)) {
						if (frags.isEmpty())
							it.remove();
						else if (1 == frags.size()) {
							ASTNode a = frags.get(0);
							if (a instanceof TextElement) {
								final String text = ((TextElement) a).getText();
								final String methodAbout = meth.getName().getIdentifier().replaceFirst("get|is", "");
								final String regex = "(?i)\\s*(?:returns? )?(?:the )?(?:value of )?"
									+ Pattern.quote(methodAbout) + "\\.?";
								final String trimmed = text.trim();
								final String rettype = stringize(meth.getReturnType2());
								if ("".equals(trimmed)
										|| "-".equals(trimmed)
										|| (rettype + " -").equals(trimmed)
										|| trimmed.equals(rettype)
										|| text.matches(regex))
									it.remove();
							}
						}
					}
				}


				if (tags.isEmpty())
					javadoc.delete();
				else if (1 == tags.size()) {
					if ("@".equals(tags.get(0).getTagName()))
							javadoc.delete();
				}
				return super.visit(meth);
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

	private static Map<String, String> methodArgs(MethodDeclaration node) {
		Map<String, String> args = new HashMap<String, String>();
		for (SingleVariableDeclaration a : ASTContainers.it(node))
			args.put(a.getName().getIdentifier(), stringize(a.getType()));
		return args;
	}

	@TerribleImplementation
	private static String stringize(Type t) {
		return String.valueOf(t);
	}

	public static void main(String[] args) throws IOException {
		long start = System.nanoTime();
		for (String s : args)
			processDir(new File(s));
		System.out.println((System.nanoTime() - start) / 1e9);
	}

	private static void processDir(File file) throws IOException {
		for (File child : file.listFiles())
			if (!child.getName().equals(".") && !child.getName().equals(".."))
				if (child.isDirectory())
					processDir(child);
				else if (child.getName().endsWith(".java"))
					processFile(child);
	}

	private static void processFile(File child) throws FileNotFoundException, IOException {
		final String res = cleanCU(FileUtils.consumeFile(new FileReader(child)));
		FileWriter fw = new FileWriter(child);
		try {
			fw.write(res);
		} finally {
			fw.flush();
			fw.close();
		}
	}
}
