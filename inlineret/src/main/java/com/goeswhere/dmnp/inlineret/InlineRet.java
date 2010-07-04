package com.goeswhere.dmnp.inlineret;

import static com.goeswhere.dmnp.util.ASTWrapper.doesNothingUseful;
import static com.goeswhere.dmnp.util.ASTWrapper.removeFragment;
import static com.goeswhere.dmnp.util.ASTWrapper.removeFromParent;
import static com.goeswhere.dmnp.util.ASTWrapper.returnsVoid;
import static com.goeswhere.dmnp.util.ASTWrapper.rewrite;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.goeswhere.dmnp.util.ASTContainers;
import com.goeswhere.dmnp.util.Containers;
import com.goeswhere.dmnp.util.ResolvingFileFixer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Maps;

class InlineRet extends ResolvingFileFixer {
	@VisibleForTesting InlineRet(String[] classpath, String[] sourcepath, String unitName, Lock l) {
		super(classpath, sourcepath, unitName, l);
	}

	public static void main(String[] args) throws InterruptedException {
		main(args, new Creator() {
			@Override public Function<String, String> create(String[] cp,
					String[] sourcePath, String unitName, Lock l) {
				return new InlineRet(cp, sourcePath, unitName, l);
			}
		});
	}

	@Override public String apply(final String src) {
		final CompilationUnit cu = compile(src);
		cu.recordModifications();

		cu.accept(new ASTVisitor() {
			@Override public boolean visit(MethodDeclaration md) {
				final Block bd = md.getBody();

				if (returnsVoid(md) || null == bd)
					return true;
				final Map<IBinding, Integer> refCounts = Maps.newHashMap();
				final Map<IBinding, VariableDeclarationFragment> decls = Maps.newHashMap();

				bd.accept(new ASTVisitor() {
					@Override public boolean visit(VariableDeclarationFragment node) {
						final IVariableBinding bin = node.resolveBinding();
						refCounts.put(bin, 0);
						decls.put(bin, node);
						return true;
					}

					@Override public boolean visit(SimpleName node) {
						final IBinding bin = node.resolveBinding();
						final Integer i = refCounts.get(bin);
						if (null != i)
							refCounts.put(bin, i + 1);
						return true;
					}
				});

				bd.accept(new ASTVisitor() {

					@Override public boolean visit(ReturnStatement retur) {

						final Expression rex = retur.getExpression(); // null check above
						if (!(rex instanceof Name))
							return true;

						final IBinding bin = ((Name) rex).resolveBinding();
						final Integer refs = refCounts.get(bin);
						if (null == refs)
							return true;

						final ASTNode prev;
						try {
							prev = prev(retur);
						} catch (FirstElementOfBlock e) {
							return true;
						}

						if (!(prev instanceof ExpressionStatement))
							return true;

						final ExpressionStatement expstat = (ExpressionStatement) prev;

						final Expression expr = expstat.getExpression();
						if (!(expr instanceof Assignment))
							return true;

						final Assignment assign = (Assignment) expr;

						if (!Assignment.Operator.ASSIGN.equals(assign.getOperator()))
							return true;

						final Expression lhs = assign.getLeftHandSide();
						if (!(lhs instanceof Name && ((Name) lhs).resolveBinding().equals(bin)))
							return true;

						final Expression rhs = assign.getRightHandSide();
						assign.setRightHandSide(cu.getAST().newNullLiteral());
						retur.setExpression(rhs);
						refCounts.put(bin, refs - 2); // a = 5; return a; == 2

						removeFromParent(expstat);
						return true;
					}

				});

				for (Entry<IBinding, Integer> a : refCounts.entrySet())
					if (1 == a.getValue()) {
						final VariableDeclarationFragment decl = decls.get(a.getKey());
						if (null == decl.getInitializer() || doesNothingUseful(decl.getInitializer()))
							removeFragment(decl);
					}

				return true;
			}
		});

		return rewrite(src, cu);
	}

	private static class FirstElementOfBlock extends IllegalArgumentException {
		FirstElementOfBlock(String s) {
			super(s);
		}
	}

	private static ASTNode prev(Statement retur) {
		final ASTNode par = retur.getParent();
		if (!(par instanceof Block))
			throw new IllegalArgumentException("Parent isn't a block: " + Containers.classAndToString(par));
		final List<Statement> stats = ASTContainers.statements((Block) par);
		final int ind = stats.indexOf(retur);
		if (0 == ind)
			throw new FirstElementOfBlock("Statement is first element of block");

		if (-1 == ind)
			throw new AssertionError();

		return stats.get(ind - 1);
	}
}
