package com.goeswhere.dmnp.schema;

import com.goeswhere.dmnp.util.ASTWrapper;
import com.goeswhere.dmnp.util.FileUtils;
import org.eclipse.jdt.core.dom.*;

import java.io.IOException;

/**
 * Process a Schema.java to a list of env variables.
 * <p>
 * input: e.g.
 * class Foo {
 * void dbupdateversion4_0() { if (a <= 1) {} if (a <= 2) {} }
 * void dbupdateversion4_1() { if (a <= 1) {} if (a <= 2) {} if (a <= 3) {} }
 * }
 * will print out:
 * dbupdateversion4_0=2
 * dbupdateversion4_1=3
 */
public class SchemaReader {

    static interface Outputter {
        void output(String s);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: Schema.java");
            System.exit(-1);
            throw new RuntimeException();
        }

        go(FileUtils.consumeFile(args[0]), s -> System.out.println(s));
    }

    static void go(final String fileContents, final Outputter out) {
        // Parse the file:
        final CompilationUnit cu = ASTWrapper.compile(fileContents);

        // Search the file for..
        cu.accept(new ASTVisitor() {

            // ..all methods..
            @Override
            public boolean visit(MethodDeclaration m) {
                final String identifier = m.getName().getIdentifier();

                // where the identifier is "interesting", and process them:
                if (interesting(identifier))
                    out.output(identifier.toLowerCase() + "=" + processBlock(identifier, 0, m.getBody()));
                return super.visit(m);
            }

            /** Scan through a block ({}) and process the statements inside it. */
            @SuppressWarnings("unchecked")
            private int processBlock(final String identifier, int prev,
                                     final Block body) {
                for (Statement s : (Iterable<Statement>) body.statements())
                    prev = processStatement(identifier, prev, s);
                return prev;
            }

            /** Process a statement. */
            private int processStatement(final String identifier, int prev,
                                         Statement s) {

                // if it's an if block...
                if (s instanceof IfStatement) {
                    final Expression expression = ((IfStatement) s).getExpression();

                    // which is <=..
                    if (expression instanceof InfixExpression) {
                        final InfixExpression ie = (InfixExpression) expression;
                        if (ie.getOperator().equals(InfixExpression.Operator.LESS_EQUALS)
                                && ie.getRightOperand() instanceof NumberLiteral)

                            // take the right-hand of the <= as the current if statement..
                            prev = Integer.parseInt(((NumberLiteral) ie.getRightOperand()).getToken());
                    }
                } else if (s instanceof TryStatement) {
                    // it's a try {} block, process it's inside bit instead..
                    prev = processBlock(identifier, prev, ((TryStatement) s).getBody());
                }
                return prev;
            }
        });
    }

    /**
     * A method is interesting if it's one of the schema update methods.
     */
    static boolean interesting(String identifier) {
        return "checkSystemParameters".equalsIgnoreCase(identifier)
                || "DBUpdate4".equalsIgnoreCase(identifier)
                || identifier.toLowerCase().matches("checksystemparametersversion3_.")
                || identifier.toLowerCase().matches("dbupdateversion4_.")
                || identifier.toLowerCase().matches("dbupdateversion3_.");
    }
}
