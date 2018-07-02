package com.goeswhere.dmnp.listcalls;

import com.goeswhere.dmnp.util.ASTAllVisitor;
import com.goeswhere.dmnp.util.ASTWrapper;
import com.goeswhere.dmnp.util.ResolvingFileFixer;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.Set;
import java.util.concurrent.locks.Lock;

public class ListCalls extends ResolvingFileFixer {

    private final Set<String> results;

    protected ListCalls(String[] classpath, String[] sourcepath,
                        String unitName, Lock compilerLock, Set<String> results) {
        super(classpath, sourcepath, unitName, compilerLock);
        this.results = results;
    }

    public static void main(final String[] args) throws InterruptedException {
        final Set<String> results = Sets.newSetFromMap(Maps.<String, Boolean>newConcurrentMap());

        main(args, new Creator() {
            @Override
            public Function<String, String> create(String[] cp,
                                                   String[] sourcePath, String unitName, Lock compileLock) {
                return new ListCalls(cp, sourcePath, unitName, compileLock, results);
            }
        });

        for (String s : results)
            System.out.println(s);
    }

    @Override
    public String apply(String from) {
        compile(from).accept(new ASTAllVisitor() {
            @Override
            public void visitMethodInvocation(MethodInvocation mi) {
                results.add(classAndSignature(mi.resolveMethodBinding()));
            }

            @Override
            public void visitClassInstanceCreation(ClassInstanceCreation cic) {
                results.add(classAndSignature(cic.resolveConstructorBinding()));
            }
        });
        return from;
    }

    private static String classAndSignature(IMethodBinding b) {
        return b.getDeclaringClass().getQualifiedName() + ":" + ASTWrapper.signature(b);
    }
}
