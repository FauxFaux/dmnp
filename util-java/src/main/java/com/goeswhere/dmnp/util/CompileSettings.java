package com.goeswhere.dmnp.util;

import static com.goeswhere.dmnp.util.FileUtils.javaFilesIn;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

public class CompileSettings {
	private static final String FILE_SEPARATOR_REPLACE = File.separator.replaceAll("\\\\", "\\\\\\\\");

	static Set<String> find(File root) throws IOException {
		final String cwd = new File("").getAbsolutePath() + File.separator;

		final Set<String> sourcePaths = Sets.newHashSet();
		for (File f : javaFilesIn(root)) {
			String pkg = DumbParsing.packageOf(f);
			String path = f.getAbsoluteFile().getParent();
			if (null == pkg) {
				add(sourcePaths, path, cwd);
				continue;
			}

			final String pkgAsPath = File.separator + pkg.replaceAll("\\.", FILE_SEPARATOR_REPLACE);
			if (!path.endsWith(pkgAsPath))
				System.out.println(path + " has unexpected package " + pkgAsPath + "; not considering");
			else {
				final String pth = path.substring(0, path.length() - pkgAsPath.length());
				add(sourcePaths, pth, cwd);
			}
		}

		return sourcePaths;
	}

	private static void add(Set<String> sourcePaths, String pth, String cwd) {
		if (pth.startsWith(cwd))
			pth = pth.substring(cwd.length());
		sourcePaths.add(pth);
	}

	public static void main(String[] args) throws IOException {
		long start = System.nanoTime();
		final Set<String> cs = find(new File(args[0]));
		System.out.println("\"" + Joiner.on("\"" + File.pathSeparator + "\"").join(cs) + "\"");
		System.out.println(((System.nanoTime()-start)/1e9) + " seconds");
	}
}
