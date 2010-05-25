package com.goeswhere.dmnp.util;

import com.google.common.base.Function;

public interface FileFixerCreator {
	Function<String, String> create(String[] cp, String[] sourcePath, String unitName);
}