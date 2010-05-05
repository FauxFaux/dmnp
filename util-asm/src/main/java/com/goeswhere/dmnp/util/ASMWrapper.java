package com.goeswhere.dmnp.util;

import static com.goeswhere.dmnp.util.FJava.filter;
import static com.goeswhere.dmnp.util.FJava.only;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.base.Predicate;



/** Various utilities for converting source to one of the object representations. */
public class ASMWrapper {

	private ASMWrapper() {
		// statics only
	}

	/** {@link #compileToClassNodes(String, String)} with a blank name. */
	public static Map<String, ClassNode> compileToClassNodes(String src) throws IOException, FailedException {
		return compileToClassNodes("", src);
	}

	/** javax.Compiler name.java containing the src and return all the ClassNodes output */
	public static Map<String, ClassNode> compileToClassNodes(String name, String src) throws IOException, FailedException {
		return compileToClassNodes(Arrays.asList(new JavaSourceFromString(name, src)));
	}

	public static Map<String, ClassNode> compileToClassNodes(Map<String, String> sources) throws IOException, FailedException {
		final List<JavaSourceFromString> l = new ArrayList<JavaSourceFromString>();
		for (Entry<String, String> src : sources.entrySet())
			l.add(new JavaSourceFromString(src.getKey(), src.getValue()));
		return compileToClassNodes(l);
	}

	private static Map<String, ClassNode> compileToClassNodes(List<JavaSourceFromString> sources) throws IOException, FailedException {

		final File tmpdir = FileUtils.createTempDir("javacdir");
		try {

			final String dir = tmpdir.getAbsolutePath();
			final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			final StringWriter out = new StringWriter();

			final CompilationTask task = compiler.getTask(out, null, null, Arrays.asList("-d", dir), null, sources);

			// Submit the compile
			if (Boolean.TRUE.equals(task.call())) {

				// Read in the files..
				Map<String, ClassNode> ret = new HashMap<String, ClassNode>();
				readFiles(tmpdir, ret);
				return ret;
			}
			throw new FailedException(out.toString());
		} finally {
			FileUtils.recursivelyDelete(tmpdir);
		}
	}

	private static void readFiles(final File dir, Map<String, ClassNode> ret) throws FileNotFoundException,
			IOException {
		for (File fi : dir.listFiles(new FilenameFilter() {
			@Override public boolean accept(File fdir, String fname) {
				return fname.endsWith(".class");
			}
		})) {
			final ClassNode cn = load(fi);
			ret.put(cn.name, cn);
		}

		for (File fi : dir.listFiles(new FileFilter() {
			@Override public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		}))
			readFiles(fi, ret);
	}

	static ClassNode compileSingle(String name, String src) throws IOException, FailedException {
		ClassNode cn = compileToClassNodes(name, src).get(name);
		if (null == cn)
			throw new FailedException("Apparently didn't generate a class of that name..");
		return cn;
	}

	private final static Pattern CLASS_NAME = Pattern.compile("class\\s+(\\w+)");
	static ClassNode compileSingle(String src) throws IOException, FailedException {
		Matcher ma = CLASS_NAME.matcher(src);
		if (!ma.find())
			throw new FailedException("Couldn't match classname");
		return compileSingle(ma.group(1), src);
	}

	private static ClassNode load(File path) throws FileNotFoundException, IOException {
		final FileInputStream is = new FileInputStream(path);
		try {
			return makeCn(new ClassReader(is));
		} finally {
			is.close();
		}
	}

	private static class JavaSourceFromString extends SimpleJavaFileObject {
		final String code;

		JavaSourceFromString(String name, String code) {
			super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
			this.code = code;
		}

		@Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
			return code;
		}
	}

	public static ClassNode makeCn(final ClassReader cr) {
		final ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		return cn;
	}

	public static ClassNode refToCn(Object x) throws IOException {
		return ASMWrapper.makeCn(new ClassReader(x.getClass().getName()));
	}

	public static MethodNode getMethod(final Object o, final String name) throws IOException {
		final ClassNode cn = ASMWrapper.refToCn(o);
		final MethodNode m = only(filter(ASMContainers.methods(cn),
			new Predicate<MethodNode>() {
				@Override
				public boolean apply(MethodNode method) {
					return name.equals(method.name);
				}
			}
		));
		METHOD_TO_CLASS.put(m, cn);
		return m;
	}

	public static ClassNode getClassFromGetMethod(MethodNode m) {
		return METHOD_TO_CLASS.get(m);
	}

	// This acts as an IdentityHashMap as MethodNode has no equals/hashCode.
	private final static Map<MethodNode, ClassNode> METHOD_TO_CLASS = new WeakHashMap<MethodNode, ClassNode>();

}
