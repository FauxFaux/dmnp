package com.goeswhere.dmnp.util;

import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class DumbParsing {
    @Nullable
    static String packageOf(File f) throws IOException {
        final BufferedReader br = new BufferedReader(new FileReader(f));
        try {
            return packageOf(br);
        } finally {
            br.close();
        }
    }

    /**
     * @param br is not closed
     * @return null if none
     */
    @Nullable
    static String packageOf(final BufferedReader br) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final StringBuilder u = new StringBuilder(4);

        int lh;
        boolean blockComment = false;
        boolean lineComment = false;
        boolean inBackslash = false;
        int inU = 0;
        int curr = 0;
        while (-1 != (lh = br.read())) {
            try {
                if (0 != inU) {
                    --inU;
                    u.appendCodePoint(curr);
                    if (0 == inU)
                        curr = Integer.parseInt(u.toString(), 16);
                    else
                        continue;
                }


                if (!inBackslash && '\\' == curr)
                    inBackslash = true;
                else {
                    if (inBackslash) {
                        inBackslash = false;
                        if ('u' == curr) {
                            inU = 4;
                            u.setLength(0);
                            continue;
                        }
                    }
                }

                if (!lineComment && '/' == curr && '*' == lh)
                    blockComment = true;

                if (blockComment) {
                    if ('*' == curr && '/' == lh)
                        blockComment = false;
                    continue;
                }

                if ('/' == curr && '/' == lh)
                    lineComment = true;

                if ('\n' == curr)
                    lineComment = false;

                if (lineComment)
                    continue;

                if (curr == ';') {
                    final String string = sb.toString().replaceAll("\\s+", " ").trim();
                    if (string.startsWith("package "))
                        return string.substring("package ".length());
                    return null;
                }

                if (!blockComment && !lineComment &&
                        ('.' == curr || Character.isJavaIdentifierPart(curr)) &&
                        0 != curr)
                    sb.appendCodePoint(curr);
                else if (Character.isWhitespace(curr))
                    sb.append(' ');
            } finally {
                curr = lh;
            }
        }

        return null;
    }
}
