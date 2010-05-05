package com.goeswhere.dmnp.conclass;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.goeswhere.dmnp.util.FailedException;
import com.goeswhere.dmnp.util.FileUtils;


public class ConClassTest {
	private static final String CONST_CONTENTS = "class Const {" +
					" public static final int A = 65;" +
					" public static final int B = 66;" +
					" public static final int C = 67;" +
					"}";

	@Test public void typical() throws IOException, FailedException, InterruptedException {
		final File root = FileUtils.createTempDir();
		try {
			FileUtils.writeFile(new File(root, "Const.java"), CONST_CONTENTS);
			FileUtils.writeFile(new File(root, "Left.java"), "class Left {" +
					" void foo() { System.out.println(Const.A); }" +
					"}");
			FileUtils.writeFile(new File(root, "Right.java"), "class Right {" +
					" void bar() { System.out.println(\"Const.B: \" + Const.C); }" +
					"}");

			FileUtils.writeFile(new File(root, "Wrong.java"), "class Right {" +
					" void bar() { System.out.println(\"No reference to B at all\"); }" +
					"}");

			assertEquals("class Const {" +
					" public static final int A = 65;" +
					" public static final int C = 67;" +
					"}",
					ConClass.go("Const", root.getAbsolutePath()));

		} finally {
			FileUtils.recursivelyDelete(root);
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void noFiles() throws IOException, FailedException, InterruptedException {
		final File root = FileUtils.createTempDir();
		try {
			ConClass.go("Const", root.getAbsolutePath());
		} finally {
			FileUtils.recursivelyDelete(root);
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void tooManyFiles() throws IOException, FailedException, InterruptedException {
		final File root = FileUtils.createTempDir();
		try {
			FileUtils.writeFile(new File(root, "Left.java"), CONST_CONTENTS);
			FileUtils.writeFile(new File(root, "Right.java"), CONST_CONTENTS);
			ConClass.go("Const", root.getAbsolutePath());
		} finally {
			FileUtils.recursivelyDelete(root);
		}
	}
}
