package com.goeswhere.dmnp.util;

import static com.goeswhere.dmnp.util.FileUtils.consumeFile;
import static com.goeswhere.dmnp.util.FileUtils.sysSplit;
import static com.goeswhere.dmnp.util.FileUtils.sysSplitWithWildcards;
import static com.goeswhere.dmnp.util.FileUtils.writeFile;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.jdt.core.dom.CompilationUnit;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

/**
 * Internal junk for a class which processes compilable files in an {@code String apply(String)} fashion.
 *
 * <b>Overwrites files in-place without back-up</b>.
 */
public abstract class FileFixer implements Function<String, String> {

	private final String[] classpath;
	private final String[] sourcepath;
	private final String unitName;
	private final Lock compilerLock;

	protected FileFixer(String[] classpath, String[] sourcepath, String unitName, Lock compilerLock) {
		this.classpath = classpath;
		this.sourcepath = sourcepath;
		this.unitName = unitName;
		this.compilerLock = compilerLock;
	}

	/** Process parameters for main, then do work.
	 * @throws InterruptedException */
	public static void main(String[] args, FileFixerCreator creator) throws InterruptedException {
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
			throws InterruptedException {
		final File f = new File(path);
		final ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
		final ReadWriteLock r = new ReentrantReadWriteLock(true);
		final Lock wl = r.writeLock();
		final BlockDoer writer = BlockDoer.start(wl);

		try {
			for (final File file : f.isDirectory() ? FileUtils.javaFilesIn(path) : Arrays.asList(f)) {
				es.submit(new Runnable() {
					@Override public void run() {
						System.out.println(Thread.currentThread().getName() + ": Processing " + file.getName() + ".");
						try {
							final String thispath = file.getAbsolutePath();
							final String read;

							final Lock rl = r.readLock();
							rl.lock();
							try {
								read = consumeFile(thispath);
							} finally {
								rl.unlock();
							}

							final String result = creator.create(cp, sourcePath, unitName(thispath), rl).apply(read);

							if (!result.equals(read))
								writer.offer(new Runnable() {
									@Override public void run() {
										System.out.println("Writing " + thispath);
										writeFile(new File(thispath), result);
									}
								});
						} catch (Exception e) {
							System.err.println("While processing " + file + ": ");
							e.printStackTrace(System.err);
						}
					}
				});
			}
			es.shutdown();
			es.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} finally {
			writer.close();
		}
	}

	public static String unitName(String path) {
		final String f = new File(path).getName();
		final String ext = ".java";
		Preconditions.checkArgument(f.endsWith(ext));
		return f.substring(0, f.length() - ext.length());
	}

	protected CompilationUnit compile(String src) {
		compilerLock.lock();
		try {
			return ASTWrapper.compile(src, unitName, classpath, sourcepath);
		} finally {
			compilerLock.unlock();
		}
	}
}
