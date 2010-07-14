package com.goeswhere.dmnp.preplookup;

import static com.goeswhere.dmnp.util.ASTContainers.arguments;
import static com.goeswhere.dmnp.util.ASTContainers.duplicate;
import static com.goeswhere.dmnp.util.ASTContainers.expressions;
import static com.goeswhere.dmnp.util.ASTContainers.setLiteralValue;
import static com.goeswhere.dmnp.util.ASTWrapper.rewrite;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;

import com.goeswhere.dmnp.util.ASTAllVisitor;
import com.goeswhere.dmnp.util.ASTContainers;
import com.goeswhere.dmnp.util.ResolvingFileFixer;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class PrepLookup extends ResolvingFileFixer {
	private final String functionPrefix;
	private final int targetArg;

	protected PrepLookup(String[] classpath, String[] sourcepath,
			String unitName, Lock compilerLock, String function, int targetArg) {
		super(classpath, sourcepath,  unitName, compilerLock);
		this.functionPrefix = function;
		this.targetArg = targetArg;
	}


	public static void main(final String[] args) throws InterruptedException {
		main("functionPrefix argnum", 2, args, new Creator() {
			@Override public Function<String, String> create(String[] cp,
					String[] sourcePath, String unitName, Lock compileLock) {
				return new PrepLookup(cp, sourcePath, unitName, compileLock,
						args[0], Integer.parseInt(args[1]));
			}
		});

		System.out.println("edited=" + edited +
				", noninfix=" + noninfix +
				", literal=" + literal +
				", type=" + type +
				", badstrings=" + badstrings +
				", nothingtodo=" + nothingtodo);
	}

	private static final AtomicInteger
		edited = a(),
		noninfix = a(),
		literal = a(),
		badstrings = a(),
		type = a(),
		nothingtodo = a();

	private static AtomicInteger a() {
		return new AtomicInteger();
	}

	@Override public String apply(String from) {
		// optimisation
		if (!from.contains(functionPrefix))
			return from;

		final CompilationUnit cu = compile(from);
		cu.recordModifications();
		final AST ast = cu.getAST();
		cu.accept(new ASTAllVisitor() {

			@Override public void visitMethodInvocation(MethodInvocation mi) {
				if (!mi.getName().getIdentifier().startsWith(functionPrefix)
						|| targetArg + 1 != mi.arguments().size())
					return;

				final List<Expression> args = ASTContainers.arguments(mi);
				Expression ex = args.get(targetArg);

				if (null != ex.resolveConstantExpressionValue()) {
					literal.incrementAndGet();
					return;
				}

				while (ex instanceof ParenthesizedExpression)
					ex = ((ParenthesizedExpression) ex).getExpression();

				if (ex instanceof InfixExpression) {
					final InfixExpression ie = (InfixExpression) ex;
					if (!ie.getOperator().equals(Operator.PLUS)) {
						noninfix.incrementAndGet();
						System.err.println("Not plus? " + ie);
						return;
					}

					final List<Expression> exop = ASTContainers.extendedOperands(ie);
					final List<Expression> full = Lists.newArrayListWithCapacity(2 + exop.size());
					full.add(ie.getLeftOperand());
					full.add(ie.getRightOperand());
					full.addAll(exop);

					final int parts = full.size();

					final List<Expression> binds = Lists.newArrayListWithCapacity(parts);
					final List<Expression> newExpression = Lists.newArrayListWithCapacity(parts);

					for (int i = 0; i < parts; ++i) {
						Expression curr = full.get(i);

						// call to duplicate() misbehaves with StringLiterals
						if (curr instanceof StringLiteral) {
							final StringLiteral nsl = ast.newStringLiteral();
							nsl.setEscapedValue(((StringLiteral) curr).getEscapedValue());
							newExpression.add(nsl);
							continue;
						}

						final Object conexp = curr.resolveConstantExpressionValue();
						if (conexp instanceof String) {
							// may be modified by future iterations
							newExpression.add(duplicate(curr));
							continue;
						}

						Expression replacement = null;
						boolean requireQuotes = false;

						if (curr instanceof MethodInvocation) {
							final MethodInvocation ci = (MethodInvocation) curr;
							final String functionName = ci.getName().getIdentifier();
							final boolean df = "dateformat".equalsIgnoreCase(functionName);
							final boolean ssq = "safesql".equalsIgnoreCase(functionName);
							final List<Expression> arg = arguments(ci);

							if (1 == arg.size() && (df || ssq)) {
								replacement = duplicate(arg.get(0));
								requireQuotes = ssq;
							}
						}

						if (null != replacement)
							curr = replacement;
						else {
							final ITypeBinding tb = curr.resolveTypeBinding();
							if (unsafeTypeBinding(tb)) {
								if (!(curr instanceof SimpleName))
									System.err.println("Not safely mappable type '" + tb.getName() + "' for "
										+ curr + " in "+ mi);
								type.incrementAndGet();
								return;
							}
						}

						if (i == 0) {
							badstrings.incrementAndGet();
							return;
						}

						final Expression prev = newExpression.get(newExpression.size() - 1);

						if (i + 1 < parts
								&& stringLiteralEndingInOperatorQuote(prev)
								&& stringLiteralStartingWithQuote(full.get(i + 1))) {
							StringLiteral sl = (StringLiteral) prev;
							final String lit = sl.getLiteralValue();
							setLiteralValue(sl, lit.substring(0, lit.length() - 1) + "?");
							removeFirstCharacter((StringLiteral) full.get(i + 1));
						} else {
							if (requireQuotes) {
								badstrings.incrementAndGet();
								System.err.println("Expecting " + full.get(i) + " to be surrounded by ''s in " + mi);
								return;
							}

							final Expression prevAct = full.get(i - 1);
							final Object prevValue = prevAct.resolveConstantExpressionValue();

							if (!stringEndingInOperator(prevValue)) {
								badstrings.incrementAndGet();
								return;
							}

							if (prev instanceof StringLiteral) {
								final StringLiteral slprev = (StringLiteral) prev;
								setLiteralValue(slprev, slprev.getLiteralValue() + "?");
							} else {
								final StringLiteral nsl = ast.newStringLiteral();
								nsl.setLiteralValue("?");
								newExpression.add(nsl);
							}
						}
						binds.add(curr);
					}

					if (binds.isEmpty()) {
						nothingtodo.incrementAndGet();
						return;
					}

					dieee(ie);
					if (containsOnlyStringLiterals(newExpression)) {
						final Iterator<Expression> it = newExpression.iterator();
						while (it.hasNext())
							if (((StringLiteral) it.next()).getLiteralValue().isEmpty())
								it.remove();
					}

					if (1 == newExpression.size())
						args.set(targetArg, newExpression.get(0));
					else {
						final InfixExpression nie = ast.newInfixExpression();
						nie.setOperator(Operator.PLUS);
						nie.setLeftOperand(newExpression.get(0));
						nie.setRightOperand(newExpression.get(1));
						final List<Expression> nieo = ASTContainers.extendedOperands(nie);
						for (int i = 2; i < newExpression.size(); ++i)
							nieo.add(newExpression.get(i));

						args.set(targetArg, nie);
					}

					final ArrayCreation ac = ast.newArrayCreation();
					ac.setType(ast.newArrayType(ast.newSimpleType(ast.newSimpleName("Object"))));
					final ArrayInitializer ai = ast.newArrayInitializer();
					ac.setInitializer(ai);
					expressions(ai).addAll(binds);
					args.add(ac);
					edited.incrementAndGet();
				} else if (ex instanceof StringLiteral) {
					noninfix.incrementAndGet();
				} else if (ex instanceof SimpleName) {
					noninfix.incrementAndGet();
				} else if (ex instanceof MethodInvocation) {
					noninfix.incrementAndGet();
				} else {
					noninfix.incrementAndGet();
					System.err.println("What the: " + mi);
				}
			}
		});

		return rewrite(from, cu);
	}

	private void removeFirstCharacter(StringLiteral sl) {
		sl.setLiteralValue(sl.getLiteralValue().substring(1));
	}

	private static void dieee(final InfixExpression ie) {
		final AST ast = ie.getAST();
		final StringLiteral left = ast.newStringLiteral();
		left.setLiteralValue("PONIES");
		ie.setLeftOperand(left);
		ie.setRightOperand(ast.newStringLiteral());
		ie.extendedOperands().clear();
	}

	private static boolean stringEndingInOperator(Object o) {
		if (!(o instanceof String))
			return false;
		return ((String) o).matches(".*(?:=|<>|>|<)\\s*-?\\s*$");
	}

	private boolean stringLiteralEndingInOperatorQuote(Expression o) {
		if (!(o instanceof StringLiteral))
			return false;
		return ((StringLiteral) o).getLiteralValue().matches(".*(?:=|<>)\\s*'$");
	}

	private boolean stringLiteralStartingWithQuote(Expression o) {
		if (!(o instanceof StringLiteral))
			return false;
		return ((StringLiteral) o).getLiteralValue().startsWith("'");
	}

	private boolean containsOnlyStringLiterals(Iterable<? extends Expression> newExpression) {
		for (Expression q : newExpression)
			if (!(q instanceof StringLiteral))
				return false;
		return true;
	}


	private static boolean unsafeTypeBinding(final ITypeBinding ty) {
		return !ty.isPrimitive() && !ty.getName().equals("java.util.Date");
	}
}
