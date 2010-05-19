package com.goeswhere.dmnp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;

public class FileUtils {

	private final static Charset DEFAULT_ENCODING = Charset.forName("UTF-8");

	private FileUtils() {
		throw new AssertionError();
	}

	/** Entire contents of the file in a String */
	public static String consumeFile(final String filename) throws IOException {
		return consumeFile(new FileReader(filename));
	}

	/** CLOSES THE FILEREADER */
	public static String consumeFile(final FileReader fileReader) throws IOException {
		final int block = 1024 * 10;
		final StringBuilder fileData = new StringBuilder(block);
		try {
			final BufferedReader reader = new BufferedReader(fileReader);
			try {
				char[] buf = new char[block];
				int numRead = 0;
				while ((numRead = reader.read(buf)) != -1) {
					fileData.append(buf, 0, numRead);
				}
			} finally {
				reader.close();
			}
		} finally {
			fileReader.close();
		}
		return fileData.toString();
	}

	public static void recursivelyDelete(final File dir) {
		for (File fi : dir.listFiles())
			if (fi.isDirectory())
				recursivelyDelete(fi);
			else
				logDelete(fi);
		logDelete(dir);
	}

	private static void logDelete(File fi) {
		if (!fi.delete())
			System.err.println("couldn't delete " + fi);
	}

	public static Iterable<File> javaFilesIn(final String path) {
		return new Iterable<File>() {
			@SuppressWarnings("unchecked")
			@Override
			public java.util.Iterator<File> iterator() {
				return org.apache.commons.io.FileUtils.iterateFiles(
						new File(path),
						new String[] { "java" },
						true);
			}
		};
	}

	public static File createTempDir(final String name) throws IOException, FailedException {
		final File tmpdir = File.createTempFile(name, null);
		if (!tmpdir.delete() || !tmpdir.mkdir())
			throw new FailedException("Couldn't make temporary directory");
		return tmpdir;
	}

	public static File createTempDir() throws IOException, FailedException {
		return createTempDir("dmnp");
	}

	public static void writeFile(File file, String string) throws IOException {
		final FileOutputStream fos = new FileOutputStream(file);
		try {
			fos.write(string.getBytes(DEFAULT_ENCODING));
		} finally {
			fos.flush();
			fos.close();
		}
	}

	public static String consumeFile(File f) throws IOException {
		return consumeFile(new FileReader(f));
	}
}
