package com.goeswhere.dmnp.util;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberRef;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.MethodRef;
import org.eclipse.jdt.core.dom.MethodRefParameter;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.WildcardType;

@SuppressWarnings("unused")
public abstract class ASTAllVisitor extends ASTVisitor {
	@Override public boolean visit(AnnotationTypeDeclaration node) {
		visitAnnotationTypeDeclaration(node);
		return true;
	}

	@Override public boolean visit(AnnotationTypeMemberDeclaration node) {
		visitAnnotationTypeMemberDeclaration(node);
		return true;
	}

	@Override public boolean visit(AnonymousClassDeclaration node) {
		visitAnonymousClassDeclaration(node);
		return true;
	}

	@Override public boolean visit(ArrayAccess node) {
		visitArrayAccess(node);
		return true;
	}

	@Override public boolean visit(ArrayCreation node) {
		visitArrayCreation(node);
		return true;
	}

	@Override public boolean visit(ArrayInitializer node) {
		visitArrayInitializer(node);
		return true;
	}

	@Override public boolean visit(ArrayType node) {
		visitArrayType(node);
		return true;
	}

	@Override public boolean visit(AssertStatement node) {
		visitAssertStatement(node);
		return true;
	}

	@Override public boolean visit(Assignment node) {
		visitAssignment(node);
		return true;
	}

	@Override public boolean visit(Block node) {
		visitBlock(node);
		return true;
	}

	@Override public boolean visit(BlockComment node) {
		visitBlockComment(node);
		return true;
	}

	@Override public boolean visit(BooleanLiteral node) {
		visitBooleanLiteral(node);
		return true;
	}

	@Override public boolean visit(BreakStatement node) {
		visitBreakStatement(node);
		return true;
	}

	@Override public boolean visit(CastExpression node) {
		visitCastExpression(node);
		return true;
	}

	@Override public boolean visit(CatchClause node) {
		visitCatchClause(node);
		return true;
	}

	@Override public boolean visit(CharacterLiteral node) {
		visitCharacterLiteral(node);
		return true;
	}

	@Override public boolean visit(ClassInstanceCreation node) {
		visitClassInstanceCreation(node);
		return true;
	}

	@Override public boolean visit(CompilationUnit node) {
		visitCompilationUnit(node);
		return true;
	}

	@Override public boolean visit(ConditionalExpression node) {
		visitConditionalExpression(node);
		return true;
	}

	@Override public boolean visit(ConstructorInvocation node) {
		visitConstructorInvocation(node);
		return true;
	}

	@Override public boolean visit(ContinueStatement node) {
		visitContinueStatement(node);
		return true;
	}

	@Override public boolean visit(DoStatement node) {
		visitDoStatement(node);
		return true;
	}

	@Override public boolean visit(EmptyStatement node) {
		visitEmptyStatement(node);
		return true;
	}

	@Override public boolean visit(EnhancedForStatement node) {
		visitEnhancedForStatement(node);
		return true;
	}

	@Override public boolean visit(EnumConstantDeclaration node) {
		visitEnumConstantDeclaration(node);
		return true;
	}

	@Override public boolean visit(EnumDeclaration node) {
		visitEnumDeclaration(node);
		return true;
	}

	@Override public boolean visit(ExpressionStatement node) {
		visitExpressionStatement(node);
		return true;
	}

	@Override public boolean visit(FieldAccess node) {
		visitFieldAccess(node);
		return true;
	}

	@Override public boolean visit(FieldDeclaration node) {
		visitFieldDeclaration(node);
		return true;
	}

	@Override public boolean visit(ForStatement node) {
		visitForStatement(node);
		return true;
	}

	@Override public boolean visit(IfStatement node) {
		visitIfStatement(node);
		return true;
	}

	@Override public boolean visit(ImportDeclaration node) {
		visitImportDeclaration(node);
		return true;
	}

	@Override public boolean visit(InfixExpression node) {
		visitInfixExpression(node);
		return true;
	}

	@Override public boolean visit(Initializer node) {
		visitInitializer(node);
		return true;
	}

	@Override public boolean visit(InstanceofExpression node) {
		visitInstanceofExpression(node);
		return true;
	}

	@Override public boolean visit(Javadoc node) {
		visitJavadoc(node);
		return true;
	}

	@Override public boolean visit(LabeledStatement node) {
		visitLabeledStatement(node);
		return true;
	}

	@Override public boolean visit(LineComment node) {
		visitLineComment(node);
		return true;
	}

	@Override public boolean visit(MarkerAnnotation node) {
		visitMarkerAnnotation(node);
		return true;
	}

	@Override public boolean visit(MemberRef node) {
		visitMemberRef(node);
		return true;
	}

	@Override public boolean visit(MemberValuePair node) {
		visitMemberValuePair(node);
		return true;
	}

	@Override public boolean visit(MethodDeclaration node) {
		visitMethodDeclaration(node);
		return true;
	}

	@Override public boolean visit(MethodInvocation node) {
		visitMethodInvocation(node);
		return true;
	}

	@Override public boolean visit(MethodRef node) {
		visitMethodRef(node);
		return true;
	}

	@Override public boolean visit(MethodRefParameter node) {
		visitMethodRefParameter(node);
		return true;
	}

	@Override public boolean visit(Modifier node) {
		visitModifier(node);
		return true;
	}

	@Override public boolean visit(NormalAnnotation node) {
		visitNormalAnnotation(node);
		return true;
	}

	@Override public boolean visit(NullLiteral node) {
		visitNullLiteral(node);
		return true;
	}

	@Override public boolean visit(NumberLiteral node) {
		visitNumberLiteral(node);
		return true;
	}

	@Override public boolean visit(PackageDeclaration node) {
		visitPackageDeclaration(node);
		return true;
	}

	@Override public boolean visit(ParameterizedType node) {
		visitParameterizedType(node);
		return true;
	}

	@Override public boolean visit(ParenthesizedExpression node) {
		visitParenthesizedExpression(node);
		return true;
	}

	@Override public boolean visit(PostfixExpression node) {
		visitPostfixExpression(node);
		return true;
	}

	@Override public boolean visit(PrefixExpression node) {
		visitPrefixExpression(node);
		return true;
	}

	@Override public boolean visit(PrimitiveType node) {
		visitPrimitiveType(node);
		return true;
	}

	public boolean visit(QualifiedName node) {
		visitQualifiedName(node);
		return true;
	}

	@Override public boolean visit(QualifiedType node) {
		visitQualifiedType(node);
		return true;
	}

	@Override public boolean visit(ReturnStatement node) {
		visitReturnStatement(node);
		return true;
	}

	@Override public boolean visit(SimpleName node) {
		visitSimpleName(node);
		return true;
	}

	@Override public boolean visit(SimpleType node) {
		visitSimpleType(node);
		return true;
	}

	@Override public boolean visit(SingleMemberAnnotation node) {
		visitSingleMemberAnnotation(node);
		return true;
	}

	@Override public boolean visit(SingleVariableDeclaration node) {
		visitSingleVariableDeclaration(node);
		return true;
	}

	@Override public boolean visit(StringLiteral node) {
		visitStringLiteral(node);
		return true;
	}

	@Override public boolean visit(SuperConstructorInvocation node) {
		visitSuperConstructorInvocation(node);
		return true;
	}

	@Override public boolean visit(SuperFieldAccess node) {
		visitSuperFieldAccess(node);
		return true;
	}

	@Override public boolean visit(SuperMethodInvocation node) {
		visitSuperMethodInvocation(node);
		return true;
	}

	@Override public boolean visit(SwitchCase node) {
		visitSwitchCase(node);
		return true;
	}

	@Override public boolean visit(SwitchStatement node) {
		visitSwitchStatement(node);
		return true;
	}

	@Override public boolean visit(SynchronizedStatement node) {
		visitSynchronizedStatement(node);
		return true;
	}

	@Override public boolean visit(TagElement node) {
		visitTagElement(node);
		return true;
	}

	@Override public boolean visit(TextElement node) {
		visitTextElement(node);
		return true;
	}

	@Override public boolean visit(ThisExpression node) {
		visitThisExpression(node);
		return true;
	}

	@Override public boolean visit(ThrowStatement node) {
		visitThrowStatement(node);
		return true;
	}

	@Override public boolean visit(TryStatement node) {
		visitTryStatement(node);
		return true;
	}

	@Override public boolean visit(TypeDeclaration node) {
		visitTypeDeclaration(node);
		return true;
	}

	@Override public boolean visit(TypeDeclarationStatement node) {
		visitTypeDeclarationStatement(node);
		return true;
	}

	@Override public boolean visit(TypeLiteral node) {
		visitTypeLiteral(node);
		return true;
	}

	@Override public boolean visit(TypeParameter node) {
		visitTypeParameter(node);
		return true;
	}

	@Override public boolean visit(VariableDeclarationExpression node) {
		visitVariableDeclarationExpression(node);
		return true;
	}

	@Override public boolean visit(VariableDeclarationFragment node) {
		visitVariableDeclarationFragment(node);
		return true;
	}

	@Override public boolean visit(VariableDeclarationStatement node) {
		visitVariableDeclarationStatement(node);
		return true;
	}

	@Override public boolean visit(WhileStatement node) {
		visitWhileStatement(node);
		return true;
	}

	@Override public boolean visit(WildcardType node) {
		visitWildcardType(node);
		return true;
	}

	public void visitAnnotationTypeDeclaration(AnnotationTypeDeclaration atd) {
		// nothing at all
	}

	public void visitAnnotationTypeMemberDeclaration(AnnotationTypeMemberDeclaration atmd) {
		// nothing at all
	}

	public void visitAnonymousClassDeclaration(AnonymousClassDeclaration acd) {
		// nothing at all
	}

	public void visitArrayAccess(ArrayAccess aa) {
		// nothing at all
	}

	public void visitArrayCreation(ArrayCreation ac) {
		// nothing at all
	}

	public void visitArrayInitializer(ArrayInitializer ai) {
		// nothing at all
	}

	public void visitArrayType(ArrayType at) {
		// nothing at all
	}

	public void visitAssertStatement(AssertStatement as) {
		// nothing at all
	}

	public void visitAssignment(Assignment a) {
		// nothing at all
	}

	public void visitBlock(Block b) {
		// nothing at all
	}

	public void visitBlockComment(BlockComment bc) {
		// nothing at all
	}

	public void visitBooleanLiteral(BooleanLiteral bl) {
		// nothing at all
	}

	public void visitBreakStatement(BreakStatement bs) {
		// nothing at all
	}

	public void visitCastExpression(CastExpression ce) {
		// nothing at all
	}

	public void visitCatchClause(CatchClause cc) {
		// nothing at all
	}

	public void visitCharacterLiteral(CharacterLiteral cl) {
		// nothing at all
	}

	public void visitClassInstanceCreation(ClassInstanceCreation cic) {
		// nothing at all
	}

	public void visitCompilationUnit(CompilationUnit cu) {
		// nothing at all
	}

	public void visitConditionalExpression(ConditionalExpression ce) {
		// nothing at all
	}

	public void visitConstructorInvocation(ConstructorInvocation ci) {
		// nothing at all
	}

	public void visitContinueStatement(ContinueStatement cs) {
		// nothing at all
	}

	public void visitDoStatement(DoStatement ds) {
		// nothing at all
	}

	public void visitEmptyStatement(EmptyStatement es) {
		// nothing at all
	}

	public void visitEnhancedForStatement(EnhancedForStatement ef) {
		// nothing at all
	}

	public void visitEnumConstantDeclaration(EnumConstantDeclaration ec) {
		// nothing at all
	}

	public void visitEnumDeclaration(EnumDeclaration ed) {
		// nothing at all
	}

	public void visitExpressionStatement(ExpressionStatement es) {
		// nothing at all
	}

	public void visitFieldAccess(FieldAccess fa) {
		// nothing at all
	}

	public void visitFieldDeclaration(FieldDeclaration fd) {
		// nothing at all
	}

	public void visitForStatement(ForStatement fs) {
		// nothing at all
	}

	public void visitIfStatement(IfStatement is) {
		// nothing at all
	}

	public void visitImportDeclaration(ImportDeclaration id) {
		// nothing at all
	}

	public void visitInfixExpression(InfixExpression ie) {
		// nothing at all
	}

	public void visitInitializer(Initializer in) {
		// nothing at all
	}

	public void visitInstanceofExpression(InstanceofExpression ie) {
		// nothing at all
	}

	public void visitJavadoc(Javadoc jd) {
		// nothing at all
	}

	public void visitLabeledStatement(LabeledStatement ls) {
		// nothing at all
	}

	public void visitLineComment(LineComment lc) {
		// nothing at all
	}

	public void visitMarkerAnnotation(MarkerAnnotation ma) {
		// nothing at all
	}

	public void visitMemberRef(MemberRef mr) {
		// nothing at all
	}

	public void visitMemberValuePair(MemberValuePair mv) {
		// nothing at all
	}

	public void visitMethodDeclaration(MethodDeclaration md) {
		// nothing at all
	}

	public void visitMethodInvocation(MethodInvocation mi) {
		// nothing at all
	}

	public void visitMethodRef(MethodRef mr) {
		// nothing at all
	}

	public void visitMethodRefParameter(MethodRefParameter mrp) {
		// nothing at all
	}

	public void visitModifier(Modifier mod) {
		// nothing at all
	}

	public void visitNormalAnnotation(NormalAnnotation na) {
		// nothing at all
	}

	public void visitNullLiteral(NullLiteral nl) {
		// nothing at all
	}

	public void visitNumberLiteral(NumberLiteral nl) {
		// nothing at all
	}

	public void visitPackageDeclaration(PackageDeclaration pd) {
		// nothing at all
	}

	public void visitParameterizedType(ParameterizedType pt) {
		// nothing at all
	}

	public void visitParenthesizedExpression(ParenthesizedExpression pe) {
		// nothing at all
	}

	public void visitPostfixExpression(PostfixExpression pe) {
		// nothing at all
	}

	public void visitPrefixExpression(PrefixExpression pe) {
		// nothing at all
	}

	public void visitPrimitiveType(PrimitiveType pt) {
		// nothing at all
	}

	public void visitQualifiedName(QualifiedName qn) {
		// nothing at all
	}

	public void visitQualifiedType(QualifiedType qt) {
		// nothing at all
	}

	public void visitReturnStatement(ReturnStatement rs) {
		// nothing at all
	}

	public void visitSimpleName(SimpleName st) {
		// nothing at all
	}

	public void visitSimpleType(SimpleType st) {
		// nothing at all
	}

	public void visitSingleMemberAnnotation(SingleMemberAnnotation sm) {
		// nothing at all
	}

	public void visitSingleVariableDeclaration(SingleVariableDeclaration sv) {
		// nothing at all
	}

	public void visitStringLiteral(StringLiteral sl) {
		// nothing at all
	}

	private void visitSuperConstructorInvocation(SuperConstructorInvocation sci) {
		// nothing at all
	}

	public void visitSuperFieldAccess(SuperFieldAccess sf) {
		// nothing at all
	}

	public void visitSuperMethodInvocation(SuperMethodInvocation sm) {
		// nothing at all
	}

	public void visitSwitchCase(SwitchCase sc) {
		// nothing at all
	}

	public void visitSwitchStatement(SwitchStatement ss) {
		// nothing at all
	}

	public void visitSynchronizedStatement(SynchronizedStatement ss) {
		// nothing at all
	}

	public void visitTagElement(TagElement te) {
		// nothing at all
	}

	public void visitTextElement(TextElement te) {
		// nothing at all
	}

	public void visitThisExpression(ThisExpression te) {
		// nothing at all
	}

	public void visitThrowStatement(ThrowStatement ts) {
		// nothing at all
	}

	public void visitTryStatement(TryStatement ts) {
		// nothing at all
	}

	public void visitTypeDeclaration(TypeDeclaration td) {
		// nothing at all
	}

	public void visitTypeDeclarationStatement(TypeDeclarationStatement tds) {
		// nothing at all
	}

	public void visitTypeLiteral(TypeLiteral tl) {
		// nothing at all
	}

	public void visitTypeParameter(TypeParameter tp) {
		// nothing at all
	}

	public void visitVariableDeclarationExpression(VariableDeclarationExpression vde) {
		// nothing at all
	}

	public void visitVariableDeclarationFragment(VariableDeclarationFragment vdf) {
		// nothing at all
	}

	public void visitVariableDeclarationStatement(VariableDeclarationStatement vds) {
		// nothing at all
	}

	public void visitWhileStatement(WhileStatement ws) {
		// nothing at all
	}

	public void visitWildcardType(WildcardType wt) {
		// nothing at all
	}
}
