package com.goeswhere.dmnp.util;

import static com.goeswhere.dmnp.util.FileUtils.consumeFile;
import static com.goeswhere.dmnp.util.FileUtils.sysSplit;
import static com.goeswhere.dmnp.util.FileUtils.sysSplitWithWildcards;
import static com.goeswhere.dmnp.util.FileUtils.writeFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.eclipse.jdt.core.dom.CompilationUnit;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 * Internal junk for a class which processes compilable files in an {@code String apply(String)} fashion.
 *
 * <b>Overwrites files in-place without back-up</b>.
 *
 * Extensible for convenience only.  Contains no state.
 */
public abstract class FileFixer implements Function<String, String> {

	private final String[] classpath;
	private final String[] sourcepath;
	private final String unitName;

	protected FileFixer(String[] classpath, String[] sourcepath, String unitName) {
		this.classpath = classpath;
		this.sourcepath = sourcepath;
		this.unitName = unitName;
	}

	/** Process parameters for main, then do work. */
	public static void main(String[] args, FileFixerCreator creator) throws IOException {
		if (3 != args.length) {
			System.err.println("Usage: classpath sourcepath file");
			return;
		}

		final String[] cp = sysSplitWithWildcards(args[0], "jar");
		final String[] sourcePath = sysSplit(args[1]);
		final String path = args[2];

		loop(cp, sourcePath, path, creator);
	}

	private static void loop(final String[] cp, final String[] sourcePath, final String path, final FileFixerCreator creator)
			throws IOException {
		final File f = new File(path);
		for (File file : f.isDirectory() ? FileUtils.javaFilesIn(path) : Arrays.asList(f)) {
			System.out.print("Processing " + file.getName() + ".");
			try {
				fixInPlace(file.getAbsolutePath(), cp, sourcePath, creator);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
			System.out.println(".  Done.");
		}
	}

	private static void fixInPlace(final String file, final String[] cp,
			final String[] sourcePath, FileFixerCreator ff) throws IOException {
		writeFile(new File(file), ff.create(cp, sourcePath, unitName(file)).apply(consumeFile(file)));
	}

	private static String unitName(String path) {
		final String f = new File(path).getName();
		final String ext = ".java";
		Preconditions.checkArgument(f.endsWith(ext));
		return f.substring(0, f.length() - ext.length());
	}

	protected CompilationUnit compile(String src) {
		return ASTWrapper.compile(src, unitName, classpath, sourcepath);
	}
}
