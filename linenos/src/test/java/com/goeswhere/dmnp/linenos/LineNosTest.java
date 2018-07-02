package com.goeswhere.dmnp.linenos;

import com.goeswhere.dmnp.util.ASMWrapper;
import com.goeswhere.dmnp.util.Mutable;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LineNosTest {

    @Test
    public void simple() throws Throwable {
        final Mutable<ClassNode> cn = Mutable.create();
        try {
            invoke(A.class, "run", cn);
            fail("Expected exception");
        } catch (NullPointerException npe) {
            LineNos.printStackTrace(npe);
            final int ln = npe.getStackTrace()[0].getLineNumber();
            assertEquals("invoke toUpperCase #4",
                    LineNos.prettify(LineNos.fromLine(cn.get(), ln), ln));
        }
    }

    private void invoke(final Class<A> clazz, final String method, Mutable<ClassNode> cn) throws IOException, Throwable {
        final byte[] a = LineNos.messWith(ASMWrapper.makeCn(new ClassReader(clazz.getName())));
        cn.set(ASMWrapper.makeCn(new ClassReader(a)));
        LineNos.contentResolver.put(clazz.getName(), cn.get());
        attempInstantiation(clazz, a, method);
    }

    private void attempInstantiation(final Class<A> type, final byte[] bytes, String method) throws Throwable {
        final Object ins = new ClassLoader(getClass().getClassLoader()) {
            Class<?> make() {
                return defineClass(type.getName(), bytes, 0, bytes.length);
            }
        }.make().newInstance();
        final Method meth = ins.getClass().getDeclaredMethod(method);
        meth.setAccessible(true);
        try {
            meth.invoke(ins);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
