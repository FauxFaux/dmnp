package com.goeswhere.dmnp.trace4jast;

import static com.goeswhere.dmnp.util.ASTContainers.duplicate;
import static com.goeswhere.dmnp.util.ASTContainers.isLoggerType;
import static com.goeswhere.dmnp.util.ASTWrapper.rewrite;
import static org.eclipse.jdt.core.dom.Modifier.isFinal;
import static org.eclipse.jdt.core.dom.Modifier.isPrivate;
import static org.eclipse.jdt.core.dom.Modifier.isStatic;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

import com.goeswhere.dmnp.util.ASTAllVisitor;
import com.goeswhere.dmnp.util.ASTContainers;
import com.goeswhere.dmnp.util.ASTWrapper;
import com.goeswhere.dmnp.util.Containers;
import com.goeswhere.dmnp.util.Mutable;
import com.goeswhere.dmnp.util.SimpleFileFixer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

// TODO constructors
// TODO long method / result variable assignment / ..?
public class Trace4jAst extends SimpleFileFixer {
	private static final class NoLineEndingDocument extends Document {
		private NoLineEndingDocument(String initialContent) {
			super(initialContent);
		}

		@Override public String getLineDelimiter(int line) throws BadLocationException {
			return " ";
		}
	}

	/** Generate a new name for the specified prefix.  Must return a different value each call. */
	public static interface NameGenerator extends Function<String, String> {
		@Override String apply(String prefix);
	}

	private static enum BuiltinNameGenerators implements NameGenerator {
		RANDOM {
			@Override public String apply(String prefix) {
				return prefix + Math.abs(TLS_RANDOM.get().nextLong());
			}
		},
		GLOBAL_SEQUENTIAL {
			@Override public String apply(String prefix) {
				return prefix + GLOBAL_SEQUENCE.incrementAndGet();
			}
		},
		TYPE_SEQUENCE {
			@Override public String apply(String prefix) {
				return prefix + KEYED_SEQUENCES.get(prefix).incrementAndGet();
			}
		};

		private static class ThreadLocalRandom extends ThreadLocal<Random> {
			@Override protected synchronized Random initialValue() {
				return new Random();
			}
		}

		private final static ThreadLocalRandom TLS_RANDOM = new ThreadLocalRandom();

		private final static AtomicLong GLOBAL_SEQUENCE = new AtomicLong();

		private final static Map<String, AtomicLong> KEYED_SEQUENCES = new MapMaker()
			.makeComputingMap(new Function<String, AtomicLong>() {
				@Override public AtomicLong apply(String from) {
					return new AtomicLong();
				}
			});
	}

	public static interface Rewriter {
		/** Rewrite the method to include calls to the specified logger.
		 * @param identifier The pretty name of the method.
		 * @param generator */
		void rewriteMethod(String loggerName, MethodDeclaration md,
				String identifier, NameGenerator generator);
	}

	public Trace4jAst(Rewriter rewriter, NameGenerator generator) {
		this.rewriter = rewriter;
		this.generator = generator;
	}

	public static void main(String[] args) throws InterruptedException {
		if (2 != args.length) {
			System.err.println("Usage: rewriter path");
			return;
		}

		final String rew = args[0];

		final BuiltInRewriters b;
		try {
			b = BuiltInRewriters.valueOf(rew);
		} catch (IllegalArgumentException e) {
			e.printStackTrace(System.err);
			System.err.println();
			System.err.println("Unrecognised rewriter '" + rew + "'.  " +
					"Supported: " + Arrays.toString(BuiltInRewriters.values()));
			return;
		}

		loop(args[1], new Creator() {
			@Override public Function<String, String> create() {
				return new Trace4jAst(b, BuiltinNameGenerators.GLOBAL_SEQUENTIAL);
			}
		});
	}

	private final Rewriter rewriter;
	private final NameGenerator generator;

	@Override public String apply(final String from) {
		final CompilationUnit cu = compile(from);
		cu.recordModifications();

		final SetMultimap<String, MethodDeclaration> meths = HashMultimap.create();
		final Set<String> names = Sets.newHashSet();

		cu.accept(new ASTAllVisitor() {
			@Override public void visitMethodDeclaration(MethodDeclaration md) {
				meths.put(md.getName().getIdentifier(), md);
			}

			@Override public void visitSimpleName(SimpleName sn) {
				names.add(sn.getIdentifier());
			}
		});

		cu.accept(new ASTAllVisitor() {
			@Override public void visitTypeDeclaration(TypeDeclaration td) {
				go(meths, names, td);
			}

			@Override public void visitEnumDeclaration(EnumDeclaration ed) {
				go(meths, names, ed);
			}
		});

		final NoLineEndingDocument d = new NoLineEndingDocument(from);
		for (Comment c : ASTContainers.comments(cu))
			if (c.isLineComment())
				try {
					d.replace(c.getStartPosition(), c.getLength(), Strings.repeat(" ", c.getLength()));
				} catch (BadLocationException e) {
					throw new RuntimeException(e);
				}

		return rewrite(d, cu);
	}

	private void go(final SetMultimap<String,MethodDeclaration> meths,
			final Set<String> names, final AbstractTypeDeclaration atd) {

		if (atd instanceof TypeDeclaration) {
			final TypeDeclaration td = (TypeDeclaration) atd;
			if (td.isInterface()
					|| (!Modifier.isStatic(td.getModifiers()) && td.isMemberTypeDeclaration())
					|| td.isLocalTypeDeclaration())
				return;
		}

		final String loggerName = ensureLogger(names, atd);
		names.add(loggerName);

		atd.accept(new ASTAllVisitor() {

			@Override public void visitMethodDeclaration(MethodDeclaration md) {
				final Block oldbody = md.getBody();
				if (md.getParent() != atd || null == oldbody)
					return;

				final List<Statement> contents = ASTContainers.statements(oldbody);
				if (contents.isEmpty())
					return;

				if (md.isConstructor()) {
					if (atd instanceof EnumDeclaration)
						// you can't refer to constants inside enum "constructors";
						// logging them makes no sense anyway.
						return;

					final Statement first = contents.get(0);
					if (first instanceof SuperConstructorInvocation
							|| first instanceof ConstructorInvocation)
						// Skip constructor with alternative constructor call in as it can't be wrapped.
						return;
				}

				if (trivial(contents)) {
					contents.add(0, log(md.getAST(), loggerName, identify(meths, md), "called"));
					return;
				}

				rewriter.rewriteMethod(loggerName, md, identify(meths, md), generator);
			}

		});
	}

	private static boolean trivial(List<Statement> cont) {
		if (cont.isEmpty())
			return true;
		if (1 != cont.size())
			return false;

		final Statement s = cont.get(0);
		if (s instanceof ReturnStatement)
			return ASTWrapper.doesNothingUseful(((ReturnStatement) s).getExpression());
		if (s instanceof ExpressionStatement) {
			final Expression ex = ((ExpressionStatement) s).getExpression();
			if (ASTWrapper.doesNothingUseful(ex))
				return true;

			if (ex instanceof Assignment) {
				final Assignment a = (Assignment) ex;
				return a.getOperator().equals(Assignment.Operator.ASSIGN) &&
					safeAssignmentTarget(a.getLeftHandSide()) &&
					ASTWrapper.doesNothingUseful(a.getRightHandSide());
			}
		}

		return false;
	}

	private static boolean safeAssignmentTarget(Expression e) {
		return e instanceof SimpleName
			|| (e instanceof FieldAccess && ((FieldAccess) e).getExpression() instanceof ThisExpression);
	}

	static interface HasExpression {
		Expression getExpression();
		void setExpression(Expression ex);
		Statement get();
		Type makeTemporaryType();
	}

	@VisibleForTesting static enum BuiltInRewriters implements Rewriter {
		/** <code>T foo() { a(); return b(); }</code> to
		 *  <code>T foo() { try { enter(); a(); return b(); } finally { leave(); } }</code>*/
		FINALLY_REWRITER {
			@Override public void rewriteMethod(String loggerName, MethodDeclaration md,
					String identifier, NameGenerator ng) {
				final AST ast = md.getAST();
				final Block oldbody = md.getBody();
				final List<Statement> contents = ASTContainers.statements(oldbody);

				contents.add(0, enter(ast, loggerName, identifier));
				final Block newbody = ast.newBlock();
				md.setBody(newbody);

				final TryStatement trs = ast.newTryStatement();
				trs.setBody(oldbody);
				final Block finblock = ast.newBlock();
				ASTContainers.statements(finblock).add(leave(ast, loggerName, identifier, "leaving"));
				trs.setFinally(finblock);
				ASTContainers.statements(newbody).add(trs);
			}
		},
		/** <code>T foo() { a(); return b(); }</code> to
		 *  <code>T foo() { enter(); a(); { final T t = b(); exit(); return t; } }</code>*/
		ENTER_AND_EXTRACT_RETURN {
			@Override public void rewriteMethod(final String loggerName, final MethodDeclaration md,
					final String identifier, NameGenerator ng) {
				final AST ast = md.getAST();
				final Block oldbody = md.getBody();
				final List<Statement> contents = ASTContainers.statements(oldbody);

				contents.add(0, enter(ast, loggerName, identifier));

				final List<HasExpression> toProcess = Lists.newArrayListWithExpectedSize(1);
				md.accept(new ASTAllVisitor() {
					@Override public void visitReturnStatement(final ReturnStatement rs) {
						ASTNode p = rs.getParent();
						while (!(p instanceof MethodDeclaration)) {
							p = p.getParent();
						}
						final Type ct = ((MethodDeclaration) p).getReturnType2();

						toProcess.add(new HasExpression() {
							@Override public void setExpression(Expression ex) {
								rs.setExpression(ex);
							}

							@Override public Expression getExpression() {
								return rs.getExpression();
							}

							@Override public Statement get() {
								return rs;
							}

							@Override public Type makeTemporaryType() {
								return duplicate(ct);
							}
						});
					}

					@Override public void visitThrowStatement(final ThrowStatement ts) {
						final Expression ex = ts.getExpression();
						if (!ASTWrapper.doesNothingUseful(ex) && !(ex instanceof ClassInstanceCreation)) {
							System.err.println("can't cope with " + ts + " in " + ASTWrapper.signature(md));
							return;
						}

						toProcess.add(new HasExpression() {
							@Override public void setExpression(Expression e) {
								ts.setExpression(e);
							}

							@Override public Expression getExpression() {
								return ts.getExpression();
							}

							@Override public Statement get() {
								return ts;
							}

							// assumption: this won't be called if ex is a SimpleType etc.,
							// as there will be no local variable.
							@Override public Type makeTemporaryType() {
								return duplicate(((ClassInstanceCreation) ex).getType());
							}
						});
					}
				});

				if (toProcess.isEmpty()) {
					final Statement lastStatement = contents.get(contents.size() - 1);
					if (!(lastStatement instanceof WhileStatement
							&& ((WhileStatement) lastStatement).getExpression() instanceof BooleanLiteral))
						contents.add(leave(ast, loggerName, identifier, "leaving"));
				}

				int item = 0;
				for (HasExpression ex : toProcess) {
					final Statement rs = ex.get();
					final Block newblock = blockate(rs);

					final List<Statement> newcontents = ASTContainers.statements(newblock);
					final Expression retexp = ex.getExpression();
					if (null != retexp && !ASTWrapper.doesNothingUseful(retexp)) {
						final VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
						final String tempvar = ng.apply("rv");
						vdf.setName(ast.newSimpleName(tempvar));
						ex.setExpression(ast.newSimpleName(tempvar));
						vdf.setInitializer(retexp);
						final VariableDeclarationStatement vds = ast.newVariableDeclarationStatement(vdf);
						ASTContainers.modifiers(vds).add(ast.newModifier(ModifierKeyword.FINAL_KEYWORD));
						vds.setType(ex.makeTemporaryType());
						newcontents.add(vds);
					}

					newcontents.add(leave(ast, loggerName, identifier, "leaving at " + ++item));
					newcontents.add(rs);
				}
			}

		},
	}

	/** Replace a statement with an empty block.  The statement is no-longer part of it's AST.
	 * Works when the parent is an if, switch or {}. */
	private static Block blockate(final Statement rs) {
		final AST ast = rs.getAST();
		final ASTNode parent = rs.getParent();
		if (parent instanceof IfStatement) {
			final IfStatement ifs = (IfStatement) parent;
			final Block p = ast.newBlock();
			final Statement tn = ifs.getThenStatement();
			final Statement el = ifs.getElseStatement();
			if (tn == rs && el != rs) {
				ifs.setThenStatement(p);
			} else if (tn != rs && el == rs) {
				ifs.setElseStatement(p);
			} else
				throw new IllegalArgumentException("inconsistent if statement..? " + Containers.classAndToString(parent));
			return p;
		}

		final List<Statement> parcont;
		if (parent instanceof Block)
			parcont = ASTContainers.statements((Block) parent);
		else
			parcont = ASTContainers.statements((SwitchStatement) parent);

		final int loc = parcont.indexOf(rs);
		parcont.remove(loc);
		final Block newblock = ast.newBlock();
		parcont.add(loc, newblock);
		return newblock;
	}

	private static ExpressionStatement enter(final AST ast, String loggerName, String identifier) {
		return log(ast, loggerName, identifier, "entering");
	}

	private static ExpressionStatement leave(final AST ast, String loggerName, String identifier, String msg) {
		return log(ast, loggerName, identifier, msg);
	}

	private String ensureLogger(Set<String> names, final AbstractTypeDeclaration atd) {
		final String atdname = atd.getName().getIdentifier();
		final Mutable<String> name = Mutable.of(null);
		atd.accept(new ASTVisitor() {
			@Override public boolean visit(FieldDeclaration node) {
				if (atd != node.getParent())
					return true; // nested class etc

				final int mod = node.getModifiers();
				if (!isPrivate(mod) || !isStatic(mod) || !isFinal(mod) || !isLoggerType(node.getType()))
					return true;
				final VariableDeclarationFragment vdf = Iterables.getOnlyElement(ASTContainers.fragments(node));
				final Expression init = vdf.getInitializer();
				if (!(init instanceof MethodInvocation))
					return true;

				final MethodInvocation mi = (MethodInvocation) init;
				if (!"getLogger".equals(mi.getName().getIdentifier()))
					return true;

				final Expression el = Iterables.getOnlyElement(ASTContainers.arguments(mi));
				if (!(el instanceof TypeLiteral))
					return true;

				final TypeLiteral tl = (TypeLiteral) el;
				if (!atdname.equals(tl.getType().toString()))
					return true;

				name.set(vdf.getName().getIdentifier());

				return false;
			}
		});

		if (null != name.get())
			return name.get();
		else
			return addLogger(atd, names);
	}

	private String addLogger(final AbstractTypeDeclaration atd, final Set<String> names) {
		final String pickedName;
		final AST ast = atd.getAST();
		final VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
		pickedName = generateName(names, "logger");
		vdf.setName(ast.newSimpleName(pickedName));
		final TypeLiteral tl = ast.newTypeLiteral();
		tl.setType(ast.newSimpleType(ast.newName(atd.getName().getIdentifier())));
		final MethodInvocation mi = ast.newMethodInvocation();
		mi.setExpression(makeName(ast));
		mi.setName(ast.newSimpleName("getLogger"));
		ASTContainers.arguments(mi).add(tl);
		vdf.setInitializer(mi);
		final FieldDeclaration fd = ast.newFieldDeclaration(vdf);

		fd.setType(makeLogger(ast));
		ASTContainers.modifiers(fd).addAll(ImmutableList.of(
				ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD),
				ast.newModifier(ModifierKeyword.STATIC_KEYWORD),
				ast.newModifier(ModifierKeyword.FINAL_KEYWORD)));

		ASTContainers.bodyDeclarations(atd).add(fd);
		return pickedName;
	}

	private String generateName(final Set<String> names, String prefix) {
		String n;
		do {
			n = generator.apply(prefix);
		} while (names.contains(n));
		return n;
	}

	private static SimpleType makeLogger(final AST ast) {
		return ast.newSimpleType(makeName(ast));
	}

	private static Name makeName(final AST ast) {
		return qualifiedName(ast, "org", "apache", "log4j", "Logger");
	}

	private static Name qualifiedName(AST ast, String... ar) {
		return ast.newName(ar);
	}

	private static ExpressionStatement log(AST ast, final String loggerName, final String identifier, final String msg) {
		final MethodInvocation mi = ast.newMethodInvocation();
		mi.setExpression(ast.newSimpleName(loggerName));
		mi.setName(ast.newSimpleName("trace"));
		final StringLiteral sl = ast.newStringLiteral();
		sl.setLiteralValue(identifier + ": " + msg);
		ASTContainers.arguments(mi).add(sl);
		return ast.newExpressionStatement(mi);
	}

	private static String identify(SetMultimap<String, MethodDeclaration> meths, MethodDeclaration md) {
		final String name = md.getName().getIdentifier();
		final Set<MethodDeclaration> decls = meths.get(name);
		if (1 == decls.size())
			return name;

		if (paramsUnique(decls))
			return name + "#" + md.parameters().size();

		return ASTWrapper.signature(md);
	}

	private static boolean paramsUnique(final Set<MethodDeclaration> decls) {
		final Set<Integer> paramCount = Sets.newHashSet();
		for (MethodDeclaration m : decls)
			if (!paramCount.add(m.parameters().size()))
				return false;
		return true;
	}
}
