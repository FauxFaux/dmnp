package com.goeswhere.dmnp.util;

import static com.goeswhere.dmnp.util.Containers.awaitTermination;
import static com.goeswhere.dmnp.util.FileUtils.consumeFile;
import static com.goeswhere.dmnp.util.FileUtils.writeFile;

import java.io.File;
import java.util.concurrent.ExecutorService;

import org.eclipse.jdt.core.dom.CompilationUnit;

import com.google.common.base.Function;

/** {@link #compile(String)} returns a simple CU without resolveBindings support. */
public abstract class SimpleFileFixer extends FileFixer {

	public static interface Creator {
		Function<String, String> create();
	}

	protected SimpleFileFixer() {
		// reducing visibility
	}

	public static void main(String[] args, final Creator creator) throws InterruptedException {
		if (1 != args.length) {
			System.err.println("Usage: path");
			return;
		}

		loop(args[0], creator);
	}

	protected static void loop(final String path, final Creator creator)
			throws InterruptedException {
		final File f = new File(path);
		final ExecutorService es = service();

		for (final File file : fileOrFileList(f)) {
			es.submit(new Runnable() {
				@Override public void run() {
					proc(file);
					try {
						final String thispath = file.getAbsolutePath();
						final String read = consumeFile(thispath);

						final String result = creator.create().apply(read);

						if (!result.equals(read))
							writeFile(new File(thispath), result);

					} catch (Exception e) {
						err(file, e);
					} finally {
						 term(file);
					}
				}
			});
		}

		awaitTermination(es);
	}

	@Override protected CompilationUnit compile(String src) {
		return ASTWrapper.compile(src);
	}
}
