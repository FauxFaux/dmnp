package com.goeswhere.dmnp.util;

import java.util.concurrent.locks.Lock;

import com.google.common.base.Function;

public interface FileFixerCreator {
	Function<String, String> create(String[] cp, String[] sourcePath, String unitName, Lock compileLock);
}