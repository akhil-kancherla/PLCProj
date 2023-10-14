package edu.ufl.cise.cop4020fa23;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.ufl.cise.cop4020fa23.ast.AST;
import edu.ufl.cise.cop4020fa23.ast.AssignmentStatement;
import edu.ufl.cise.cop4020fa23.ast.BinaryExpr;
import edu.ufl.cise.cop4020fa23.ast.Block;
import edu.ufl.cise.cop4020fa23.ast.Block.BlockElem;
import edu.ufl.cise.cop4020fa23.ast.BooleanLitExpr;
import edu.ufl.cise.cop4020fa23.ast.ChannelSelector;
import edu.ufl.cise.cop4020fa23.ast.ConditionalExpr;
import edu.ufl.cise.cop4020fa23.ast.ConstExpr;
import edu.ufl.cise.cop4020fa23.ast.Declaration;
import edu.ufl.cise.cop4020fa23.ast.Dimension;
import edu.ufl.cise.cop4020fa23.ast.DoStatement;
import edu.ufl.cise.cop4020fa23.ast.ExpandedPixelExpr;
import edu.ufl.cise.cop4020fa23.ast.Expr;
import edu.ufl.cise.cop4020fa23.ast.GuardedBlock;
import edu.ufl.cise.cop4020fa23.ast.IdentExpr;
import edu.ufl.cise.cop4020fa23.ast.IfStatement;
import edu.ufl.cise.cop4020fa23.ast.LValue;
import edu.ufl.cise.cop4020fa23.ast.NameDef;
import edu.ufl.cise.cop4020fa23.ast.NumLitExpr;
import edu.ufl.cise.cop4020fa23.ast.PixelSelector;
import edu.ufl.cise.cop4020fa23.ast.PostfixExpr;
import edu.ufl.cise.cop4020fa23.ast.Program;
import edu.ufl.cise.cop4020fa23.ast.ReturnStatement;
import edu.ufl.cise.cop4020fa23.ast.StatementBlock;
import edu.ufl.cise.cop4020fa23.ast.StringLitExpr;
import edu.ufl.cise.cop4020fa23.ast.UnaryExpr;
import edu.ufl.cise.cop4020fa23.ast.WriteStatement;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;

class ParserTest_starter {
	static final int TIMEOUT_MILLIS = 1000;

	AST getAST(String input) throws PLCCompilerException {
		return ComponentFactory.makeParser(input).parse();
	}

	NumLitExpr checkNumLitExpr(AST e, String value) {
		assertThat("", e, instanceOf(NumLitExpr.class));
		NumLitExpr ne = (NumLitExpr) e;
		assertEquals(value, ne.getText());
		return ne;
	}

	NumLitExpr checkNumLitExpr(AST e, int value) {
		assertThat("", e, instanceOf(NumLitExpr.class));
		NumLitExpr ne = (NumLitExpr) e;
		assertEquals(Integer.toString(value), ne.getText());
		return ne;
	}

	StringLitExpr checkStringLitExpr(AST e, String value) {
		assertThat("", e, instanceOf(StringLitExpr.class));
		StringLitExpr se = (StringLitExpr) e;
		String s = se.getText();
		assertEquals('"', s.charAt(0)); // check that first char is "
		assertEquals('"', s.charAt(s.length() - 1));
		assertEquals(value, s.substring(1, s.length() - 1));
		return se;
	}

	BooleanLitExpr checkBooleanLitExpr(AST e, boolean value) {
		assertThat("", e, instanceOf(BooleanLitExpr.class));
		BooleanLitExpr be = (BooleanLitExpr) e;
		assertEquals(Boolean.toString(value), be.getText());
		return be;
	}

	private UnaryExpr checkUnaryExpr(AST e, Kind op) {
		assertThat("", e, instanceOf(UnaryExpr.class));
		assertEquals(op, ((UnaryExpr) e).getOp());
		return (UnaryExpr) e;
	}

	private ConditionalExpr checkConditionalExpr(AST e) {
		assertThat("", e, instanceOf(ConditionalExpr.class));
		return (ConditionalExpr) e;
	}

	BinaryExpr checkBinaryExpr(AST e, Kind expectedOp) {
		assertThat("", e, instanceOf(BinaryExpr.class));
		BinaryExpr be = (BinaryExpr) e;
		assertEquals(expectedOp, be.getOp().kind());
		return be;
	}

	IdentExpr checkIdentExpr(AST e, String name) {
		assertThat("", e, instanceOf(IdentExpr.class));
		IdentExpr ident = (IdentExpr) e;
		assertEquals(name, ident.getName());
		return ident;
	}

	BooleanLitExpr checkBooleanLitExpr(AST e, String value) {
		assertThat("", e, instanceOf(BooleanLitExpr.class));
		BooleanLitExpr be = (BooleanLitExpr) e;
		assertEquals(value, be.getText());
		return be;
	}

	ConstExpr checkConstExpr(AST e, String name) {
		assertThat("", e, instanceOf(ConstExpr.class));
		ConstExpr ce = (ConstExpr) e;
		assertEquals(name, ce.getName());
		return ce;
	}

	PostfixExpr checkPostfixExpr(AST e, boolean hasPixelSelector, boolean hasChannelSelector) {
		assertThat("", e, instanceOf(PostfixExpr.class));
		PostfixExpr pfe = (PostfixExpr) e;
		AST channel = pfe.channel();
		assertEquals(hasChannelSelector, channel != null);
		AST pixel = pfe.pixel();
		assertEquals(hasPixelSelector, pixel != null);
		return pfe;
	}

	ChannelSelector checkChannelSelector(AST e, String expectedColor) {
		assertThat("", e, instanceOf(ChannelSelector.class));
		ChannelSelector chan = (ChannelSelector) e;
		assertEquals(expectedColor, getColorString(chan.color()));
		return chan;
	}

	ChannelSelector checkChannelSelector(AST e, Kind expectedColor) {
		assertThat("", e, instanceOf(ChannelSelector.class));
		ChannelSelector chan = (ChannelSelector) e;
		assertEquals(expectedColor, chan.color());
		return chan;
	}

	String getColorString(Kind kind) {
		return switch (kind) {
		case RES_red -> "red";
		case RES_blue -> "blue";
		case RES_green -> "green";
		default -> throw new IllegalArgumentException();
		};
	}

	LValue checkLValueName(AST lValue, String name) {
		assertThat("", lValue, instanceOf(LValue.class));
		LValue ident = (LValue) lValue;
		assertEquals(name, ident.getName());
		return ident;
	}

	NameDef checkNameDef(AST ast, String type, String name) {
		assertThat("", ast, instanceOf(NameDef.class));
		NameDef nameDef = (NameDef) ast;
		assertEquals(type, nameDef.getTypeToken().text());
		assertEquals(name, nameDef.getName());
		assertNull(nameDef.getDimension());
		return nameDef;
	}

	NameDef checkNameDefDim(AST ast, String type, String name) {
		assertThat("", ast, instanceOf(NameDef.class));
		NameDef nameDef = (NameDef) ast;
		assertEquals(type, nameDef.getTypeToken().text());
		assertEquals(name, nameDef.getName());
		assertNotNull(nameDef.getDimension());
		return nameDef;
	}

	Program checkProgram(AST ast, String type, String name) {
		assertThat("", ast, instanceOf(Program.class));
		Program program = (Program) ast;
		assertEquals(type, program.getTypeToken().text());
		assertEquals(name, program.getName());
		return program;
	}

	Declaration checkDec(AST ast) {
		assertThat("", ast, instanceOf(Declaration.class));
		Declaration dec0 = (Declaration) ast;
		return dec0;
	}

	@Test
	void test0a() throws PLCCompilerException {
		String input = """
				void prog0() <::>
				""";
		AST ast = getAST(input);
		Program program0 = checkProgram(ast, "void", "prog0");
		List<NameDef> params1 = program0.getParams();
		assertEquals(0, params1.size());
		Block programBlock2 = ((Program) ast).getBlock();
		List<BlockElem> blockElemList3 = programBlock2.getElems();
		assertEquals(0, blockElemList3.size());
	}

	@Test
	void test1a() throws PLCCompilerException {
		String input = """
				int prog()<:int a; string s; :>
				""";
		AST ast = getAST(input);
		Program program0 = checkProgram(ast, "int", "prog");
		List<NameDef> params1 = program0.getParams();
		assertEquals(0, params1.size());
		Block programBlock2 = ((Program) ast).getBlock();
		List<BlockElem> blockElemList3 = programBlock2.getElems();
		assertEquals(2, blockElemList3.size());
		BlockElem blockElem4 = ((List<BlockElem>) blockElemList3).get(0);
		checkDec(blockElem4);
		NameDef nameDef5 = ((Declaration) blockElem4).getNameDef();
		checkNameDef(nameDef5, "int", "a");
		BlockElem blockElem6 = ((List<BlockElem>) blockElemList3).get(1);
		checkDec(blockElem6);
		NameDef nameDef7 = ((Declaration) blockElem6).getNameDef();
		checkNameDef(nameDef7, "string", "s");
	}

	@Test
	void test2a() throws PLCCompilerException {
		String input = """
				void p0(int a, string s, boolean b, image i, pixel p)<::>
				""";
		AST ast = getAST(input);
		Program program0 = checkProgram(ast, "void", "p0");
		List<NameDef> params1 = program0.getParams();
		assertEquals(5, params1.size());
		NameDef paramNameDef2 = ((List<NameDef>) params1).get(0);
		checkNameDef(paramNameDef2, "int", "a");
		NameDef paramNameDef3 = ((List<NameDef>) params1).get(1);
		checkNameDef(paramNameDef3, "string", "s");
		NameDef paramNameDef4 = ((List<NameDef>) params1).get(2);
		checkNameDef(paramNameDef4, "boolean", "b");
		NameDef paramNameDef5 = ((List<NameDef>) params1).get(3);
		checkNameDef(paramNameDef5, "image", "i");
		NameDef paramNameDef6 = ((List<NameDef>) params1).get(4);
		checkNameDef(paramNameDef6, "pixel", "p");
		Block programBlock7 = ((Program) ast).getBlock();
		List<BlockElem> blockElemList8 = programBlock7.getElems();
		assertEquals(0, blockElemList8.size());
	}

	@Test
	void test3a() throws PLCCompilerException {
		String input = """
				boolean p0() <:
				int a;
				string s;
				boolean b;
				image i;
				pixel p;
				image[1028,256] d;
				:>
				""";
		AST ast = getAST(input);
		Program program0 = checkProgram(ast, "boolean", "p0");
		List<NameDef> params1 = program0.getParams();
		assertEquals(0, params1.size());
		Block programBlock2 = ((Program) ast).getBlock();
		List<BlockElem> blockElemList3 = programBlock2.getElems();
		assertEquals(6, blockElemList3.size());
		BlockElem blockElem4 = ((List<BlockElem>) blockElemList3).get(0);
		checkDec(blockElem4);
		NameDef nameDef5 = ((Declaration) blockElem4).getNameDef();
		checkNameDef(nameDef5, "int", "a");
		BlockElem blockElem6 = ((List<BlockElem>) blockElemList3).get(1);
		checkDec(blockElem6);
		NameDef nameDef7 = ((Declaration) blockElem6).getNameDef();
		checkNameDef(nameDef7, "string", "s");
		BlockElem blockElem8 = ((List<BlockElem>) blockElemList3).get(2);
		checkDec(blockElem8);
		NameDef nameDef9 = ((Declaration) blockElem8).getNameDef();
		checkNameDef(nameDef9, "boolean", "b");
		BlockElem blockElem10 = ((List<BlockElem>) blockElemList3).get(3);
		checkDec(blockElem10);
		NameDef nameDef11 = ((Declaration) blockElem10).getNameDef();
		checkNameDef(nameDef11, "image", "i");
		BlockElem blockElem12 = ((List<BlockElem>) blockElemList3).get(4);
		checkDec(blockElem12);
		NameDef nameDef13 = ((Declaration) blockElem12).getNameDef();
		checkNameDef(nameDef13, "pixel", "p");
		BlockElem blockElem14 = ((List<BlockElem>) blockElemList3).get(5);
		checkDec(blockElem14);
		NameDef nameDef15 = ((Declaration) blockElem14).getNameDef();
		checkNameDefDim(nameDef15, "image", "d");
		Dimension dimension16 = ((NameDef) nameDef15).getDimension();
		assertThat("", dimension16, instanceOf(Dimension.class));
		Expr width17 = ((Dimension) dimension16).getWidth();
		checkNumLitExpr(width17, 1028);
		Expr height18 = ((Dimension) dimension16).getHeight();
		checkNumLitExpr(height18, 256);
	}

	@Test
	void test4a() throws PLCCompilerException {
		String input = """
				string sss()<:
				write 3+5;
				write x;
				write Z;
				write [1,2,3];
				:>
				""";
		AST ast = getAST(input);
		Program program0 = checkProgram(ast, "string", "sss");
		List<NameDef> params1 = program0.getParams();
		assertEquals(0, params1.size());
		Block programBlock2 = ((Program) ast).getBlock();
		List<BlockElem> blockElemList3 = programBlock2.getElems();
		assertEquals(4, blockElemList3.size());
		BlockElem blockElem4 = ((List<BlockElem>) blockElemList3).get(0);
		assertThat("", blockElem4, instanceOf(WriteStatement.class));
		Expr writeStatementExpr5 = ((WriteStatement) blockElem4).getExpr();
		checkBinaryExpr(writeStatementExpr5, Kind.PLUS);
		Expr leftExpr6 = ((BinaryExpr) writeStatementExpr5).getLeftExpr();
		checkNumLitExpr(leftExpr6, 3);
		Expr rightExpr7 = ((BinaryExpr) writeStatementExpr5).getRightExpr();
		checkNumLitExpr(rightExpr7, 5);
		BlockElem blockElem8 = ((List<BlockElem>) blockElemList3).get(1);
		assertThat("", blockElem8, instanceOf(WriteStatement.class));
		Expr writeStatementExpr9 = ((WriteStatement) blockElem8).getExpr();
		checkIdentExpr(writeStatementExpr9, "x");
		BlockElem blockElem10 = ((List<BlockElem>) blockElemList3).get(2);
		assertThat("", blockElem10, instanceOf(WriteStatement.class));
		Expr writeStatementExpr11 = ((WriteStatement) blockElem10).getExpr();
		checkConstExpr(writeStatementExpr11, "Z");
		BlockElem blockElem12 = ((List<BlockElem>) blockElemList3).get(3);
		assertThat("", blockElem12, instanceOf(WriteStatement.class));
		Expr writeStatementExpr13 = ((WriteStatement) blockElem12).getExpr();
		Expr red14 = ((ExpandedPixelExpr) writeStatementExpr13).getRed();
		checkNumLitExpr(red14, 1);
		Expr green15 = ((ExpandedPixelExpr) writeStatementExpr13).getGreen();
		checkNumLitExpr(green15, 2);
		Expr blue16 = ((ExpandedPixelExpr) writeStatementExpr13).getBlue();
		checkNumLitExpr(blue16, 3);
	}

	@Test
	void test5a() throws PLCCompilerException {
		String input = """
				pixel ppp() <:
				a = 3;
				a[x,y] = 4;
				a[x,y]:red = 5;
				a:green = 5;
				:>
				""";
		AST ast = getAST(input);
		Program program0 = checkProgram(ast, "pixel", "ppp");
		List<NameDef> params1 = program0.getParams();
		assertEquals(0, params1.size());
		Block programBlock2 = ((Program) ast).getBlock();
		List<BlockElem> blockElemList3 = programBlock2.getElems();
		assertEquals(4, blockElemList3.size());
		BlockElem blockElem4 = ((List<BlockElem>) blockElemList3).get(0);
		assertThat("", blockElem4, instanceOf(AssignmentStatement.class));
		LValue LValue5 = ((AssignmentStatement) blockElem4).getlValue();
		assertThat("", LValue5, instanceOf(LValue.class));
		String name6 = ((LValue) LValue5).getName();
		assertEquals("a", name6);
		assertNull(LValue5.getPixelSelector());
		assertNull(((LValue) LValue5).getChannelSelector());
		Expr expr7 = ((AssignmentStatement) blockElem4).getE();
		checkNumLitExpr(expr7, 3);
		BlockElem blockElem8 = ((List<BlockElem>) blockElemList3).get(1);
		assertThat("", blockElem8, instanceOf(AssignmentStatement.class));
		LValue LValue9 = ((AssignmentStatement) blockElem8).getlValue();
		assertThat("", LValue9, instanceOf(LValue.class));
		String name10 = ((LValue) LValue9).getName();
		assertEquals("a", name10);
		PixelSelector pixel11 = ((LValue) LValue9).getPixelSelector();
		Expr x12 = ((PixelSelector) pixel11).xExpr();
		checkIdentExpr(x12, "x");
		Expr y13 = ((PixelSelector) pixel11).yExpr();
		checkIdentExpr(y13, "y");
		assertNull(((LValue) LValue9).getChannelSelector());
		Expr expr14 = ((AssignmentStatement) blockElem8).getE();
		checkNumLitExpr(expr14, 4);
		BlockElem blockElem15 = ((List<BlockElem>) blockElemList3).get(2);
		assertThat("", blockElem15, instanceOf(AssignmentStatement.class));
		LValue LValue16 = ((AssignmentStatement) blockElem15).getlValue();
		assertThat("", LValue16, instanceOf(LValue.class));
		String name17 = ((LValue) LValue16).getName();
		assertEquals("a", name17);
		PixelSelector pixel18 = ((LValue) LValue16).getPixelSelector();
		Expr x19 = ((PixelSelector) pixel18).xExpr();
		checkIdentExpr(x19, "x");
		Expr y20 = ((PixelSelector) pixel18).yExpr();
		checkIdentExpr(y20, "y");
		ChannelSelector channel21 = ((LValue) LValue16).getChannelSelector();
		checkChannelSelector(channel21, Kind.RES_red);
		Expr expr22 = ((AssignmentStatement) blockElem15).getE();
		checkNumLitExpr(expr22, 5);
		BlockElem blockElem23 = ((List<BlockElem>) blockElemList3).get(3);
		assertThat("", blockElem23, instanceOf(AssignmentStatement.class));
		LValue LValue24 = ((AssignmentStatement) blockElem23).getlValue();
		assertThat("", LValue24, instanceOf(LValue.class));
		String name25 = ((LValue) LValue24).getName();
		assertEquals("a", name25);
		assertNull(LValue24.getPixelSelector());
		ChannelSelector channel26 = ((LValue) LValue24).getChannelSelector();
		checkChannelSelector(channel26, Kind.RES_green);
		Expr expr27 = ((AssignmentStatement) blockElem23).getE();
		checkNumLitExpr(expr27, 5);
	}

	@Test
	void test6a() throws PLCCompilerException {
		String input = """
				image sss()<:
				do 1 -> <: write 2; :>
				 []  a -> <: b = d; :>
				od;
				:>
				""";
		AST ast = getAST(input);
		Program program0 = checkProgram(ast, "image", "sss");
		List<NameDef> params1 = program0.getParams();
		assertEquals(0, params1.size());
		Block programBlock2 = ((Program) ast).getBlock();
		List<BlockElem> blockElemList3 = programBlock2.getElems();
		assertEquals(1, blockElemList3.size());
		BlockElem blockElem4 = ((List<BlockElem>) blockElemList3).get(0);
		assertThat("", blockElem4, instanceOf(DoStatement.class));
		List<GuardedBlock> guardedBlocks5 = ((DoStatement) blockElem4).getGuardedBlocks();
		assertEquals(2, guardedBlocks5.size());
		GuardedBlock guardedBlock6 = ((List<GuardedBlock>) guardedBlocks5).get(0);
		assertThat("", guardedBlock6, instanceOf(GuardedBlock.class));
		Expr guard7 = ((GuardedBlock) guardedBlock6).getGuard();
		checkNumLitExpr(guard7, 1);
		Block block8 = ((GuardedBlock) guardedBlock6).getBlock();
		List<BlockElem> blockElemList9 = block8.getElems();
		assertEquals(1, blockElemList9.size());
		BlockElem blockElem10 = ((List<BlockElem>) blockElemList9).get(0);
		assertThat("", blockElem10, instanceOf(WriteStatement.class));
		Expr writeStatementExpr11 = ((WriteStatement) blockElem10).getExpr();
		checkNumLitExpr(writeStatementExpr11, 2);
		GuardedBlock guardedBlock12 = ((List<GuardedBlock>) guardedBlocks5).get(1);
		assertThat("", guardedBlock12, instanceOf(GuardedBlock.class));
		Expr guard13 = ((GuardedBlock) guardedBlock12).getGuard();
		checkIdentExpr(guard13, "a");
		Block block14 = ((GuardedBlock) guardedBlock12).getBlock();
		List<BlockElem> blockElemList15 = block14.getElems();
		assertEquals(1, blockElemList15.size());
		BlockElem blockElem16 = ((List<BlockElem>) blockElemList15).get(0);
		assertThat("", blockElem16, instanceOf(AssignmentStatement.class));
		LValue LValue17 = ((AssignmentStatement) blockElem16).getlValue();
		assertThat("", LValue17, instanceOf(LValue.class));
		String name18 = ((LValue) LValue17).getName();
		assertEquals("b", name18);
		assertNull(LValue17.getPixelSelector());
		assertNull(((LValue) LValue17).getChannelSelector());
		Expr expr19 = ((AssignmentStatement) blockElem16).getE();
		checkIdentExpr(expr19, "d");
	}

	@Test
	void test7a() throws PLCCompilerException {
		String input = """
				image sss()<:
				if 1 -> <: write 2; :>
				[]   a -> <: b = d; :>
				fi;
				:>
				""";
		AST ast = getAST(input);
		Program program0 = checkProgram(ast, "image", "sss");
		List<NameDef> params1 = program0.getParams();
		assertEquals(0, params1.size());
		Block programBlock2 = ((Program) ast).getBlock();
		List<BlockElem> blockElemList3 = programBlock2.getElems();
		assertEquals(1, blockElemList3.size());
		BlockElem blockElem4 = ((List<BlockElem>) blockElemList3).get(0);
		assertThat("", blockElem4, instanceOf(IfStatement.class));
		List<GuardedBlock> guardedBlocks5 = ((IfStatement) blockElem4).getGuardedBlocks();
		assertEquals(2, guardedBlocks5.size());
		GuardedBlock guardedBlock6 = ((List<GuardedBlock>) guardedBlocks5).get(0);
		assertThat("", guardedBlock6, instanceOf(GuardedBlock.class));
		Expr guard7 = ((GuardedBlock) guardedBlock6).getGuard();
		checkNumLitExpr(guard7, 1);
		Block block8 = ((GuardedBlock) guardedBlock6).getBlock();
		List<BlockElem> blockElemList9 = block8.getElems();
		assertEquals(1, blockElemList9.size());
		BlockElem blockElem10 = ((List<BlockElem>) blockElemList9).get(0);
		assertThat("", blockElem10, instanceOf(WriteStatement.class));
		Expr writeStatementExpr11 = ((WriteStatement) blockElem10).getExpr();
		checkNumLitExpr(writeStatementExpr11, 2);
		GuardedBlock guardedBlock12 = ((List<GuardedBlock>) guardedBlocks5).get(1);
		assertThat("", guardedBlock12, instanceOf(GuardedBlock.class));
		Expr guard13 = ((GuardedBlock) guardedBlock12).getGuard();
		checkIdentExpr(guard13, "a");
		Block block14 = ((GuardedBlock) guardedBlock12).getBlock();
		List<BlockElem> blockElemList15 = block14.getElems();
		assertEquals(1, blockElemList15.size());
		BlockElem blockElem16 = ((List<BlockElem>) blockElemList15).get(0);
		assertThat("", blockElem16, instanceOf(AssignmentStatement.class));
		LValue LValue17 = ((AssignmentStatement) blockElem16).getlValue();
		assertThat("", LValue17, instanceOf(LValue.class));
		String name18 = ((LValue) LValue17).getName();
		assertEquals("b", name18);
		assertNull(LValue17.getPixelSelector());
		assertNull(((LValue) LValue17).getChannelSelector());
		Expr expr19 = ((AssignmentStatement) blockElem16).getE();
		checkIdentExpr(expr19, "d");
	}

	@Test
	void test8a() throws PLCCompilerException {
		String input = """
				void p() <:
				   ^3;
				   :>
				""";
		AST ast = getAST(input);
		Program program0 = checkProgram(ast, "void", "p");
		List<NameDef> params1 = program0.getParams();
		assertEquals(0, params1.size());
		Block programBlock2 = ((Program) ast).getBlock();
		List<BlockElem> blockElemList3 = programBlock2.getElems();
		assertEquals(1, blockElemList3.size());
		BlockElem blockElem4 = ((List<BlockElem>) blockElemList3).get(0);
		assertThat("", blockElem4, instanceOf(ReturnStatement.class));
		Expr returnValueExpr5 = ((ReturnStatement) blockElem4).getE();
		checkNumLitExpr(returnValueExpr5, 3);
	}

	@Test
	void test9a() throws PLCCompilerException {
		String input = """
				void p() <:
				   <::>;
				   :>
				""";
		AST ast = getAST(input);
		Program program0 = checkProgram(ast, "void", "p");
		List<NameDef> params1 = program0.getParams();
		assertEquals(0, params1.size());
		Block programBlock2 = ((Program) ast).getBlock();
		List<BlockElem> blockElemList3 = programBlock2.getElems();
		assertEquals(1, blockElemList3.size());
		BlockElem blockElem4 = ((List<BlockElem>) blockElemList3).get(0);
		assertThat("", blockElem4, instanceOf(StatementBlock.class));
		Block block5 = ((StatementBlock) blockElem4).getBlock();
		List<BlockElem> blockElemList6 = block5.getElems();
		assertEquals(0, blockElemList6.size());
	}

	@Test
	void test10a() throws PLCCompilerException {
		String input = """
				void p() <:
				int r;
				a=Z;
				boolean b;
				<: a[x,y]:red = b; :>;
				c=2;
				:>
				""";
		AST ast = getAST(input);
		Program program0 = checkProgram(ast, "void", "p");
		List<NameDef> params1 = program0.getParams();
		assertEquals(0, params1.size());
		Block programBlock2 = ((Program) ast).getBlock();
		List<BlockElem> blockElemList3 = programBlock2.getElems();
		assertEquals(5, blockElemList3.size());
		BlockElem blockElem4 = ((List<BlockElem>) blockElemList3).get(0);
		checkDec(blockElem4);
		NameDef nameDef5 = ((Declaration) blockElem4).getNameDef();
		checkNameDef(nameDef5, "int", "r");
		BlockElem blockElem6 = ((List<BlockElem>) blockElemList3).get(1);
		assertThat("", blockElem6, instanceOf(AssignmentStatement.class));
		LValue LValue7 = ((AssignmentStatement) blockElem6).getlValue();
		assertThat("", LValue7, instanceOf(LValue.class));
		String name8 = ((LValue) LValue7).getName();
		assertEquals("a", name8);
		assertNull(LValue7.getPixelSelector());
		assertNull(((LValue) LValue7).getChannelSelector());
		Expr expr9 = ((AssignmentStatement) blockElem6).getE();
		checkConstExpr(expr9, "Z");
		BlockElem blockElem10 = ((List<BlockElem>) blockElemList3).get(2);
		checkDec(blockElem10);
		NameDef nameDef11 = ((Declaration) blockElem10).getNameDef();
		checkNameDef(nameDef11, "boolean", "b");
		BlockElem blockElem12 = ((List<BlockElem>) blockElemList3).get(3);
		assertThat("", blockElem12, instanceOf(StatementBlock.class));
		Block block13 = ((StatementBlock) blockElem12).getBlock();
		List<BlockElem> blockElemList14 = block13.getElems();
		assertEquals(1, blockElemList14.size());
		BlockElem blockElem15 = ((List<BlockElem>) blockElemList14).get(0);
		assertThat("", blockElem15, instanceOf(AssignmentStatement.class));
		LValue LValue16 = ((AssignmentStatement) blockElem15).getlValue();
		assertThat("", LValue16, instanceOf(LValue.class));
		String name17 = ((LValue) LValue16).getName();
		assertEquals("a", name17);
		PixelSelector pixel18 = ((LValue) LValue16).getPixelSelector();
		Expr x19 = ((PixelSelector) pixel18).xExpr();
		checkIdentExpr(x19, "x");
		Expr y20 = ((PixelSelector) pixel18).yExpr();
		checkIdentExpr(y20, "y");
		ChannelSelector channel21 = ((LValue) LValue16).getChannelSelector();
		checkChannelSelector(channel21, Kind.RES_red);
		Expr expr22 = ((AssignmentStatement) blockElem15).getE();
		checkIdentExpr(expr22, "b");
		BlockElem blockElem23 = ((List<BlockElem>) blockElemList3).get(4);
		assertThat("", blockElem23, instanceOf(AssignmentStatement.class));
		LValue LValue24 = ((AssignmentStatement) blockElem23).getlValue();
		assertThat("", LValue24, instanceOf(LValue.class));
		String name25 = ((LValue) LValue24).getName();
		assertEquals("c", name25);
		assertNull(LValue24.getPixelSelector());
		assertNull(((LValue) LValue24).getChannelSelector());
		Expr expr26 = ((AssignmentStatement) blockElem23).getE();
		checkNumLitExpr(expr26, 2);
	}

	@Test
	void test11a() throws PLCCompilerException {
		String input = """
				int f()
				<:
				int a = TRUE;
				string b = 3;
				pixel p = "hello";
				:>
				""";
		AST ast = getAST(input);
		Program program0 = checkProgram(ast, "int", "f");
		List<NameDef> params1 = program0.getParams();
		assertEquals(0, params1.size());
		Block programBlock2 = ((Program) ast).getBlock();
		List<BlockElem> blockElemList3 = programBlock2.getElems();
		assertEquals(3, blockElemList3.size());
		BlockElem blockElem4 = ((List<BlockElem>) blockElemList3).get(0);
		checkDec(blockElem4);
		NameDef nameDef5 = ((Declaration) blockElem4).getNameDef();
		checkNameDef(nameDef5, "int", "a");
		Expr expr6 = ((Declaration) blockElem4).getInitializer();
		checkBooleanLitExpr(expr6, "TRUE");
		BlockElem blockElem7 = ((List<BlockElem>) blockElemList3).get(1);
		checkDec(blockElem7);
		NameDef nameDef8 = ((Declaration) blockElem7).getNameDef();
		checkNameDef(nameDef8, "string", "b");
		Expr expr9 = ((Declaration) blockElem7).getInitializer();
		checkNumLitExpr(expr9, 3);
		BlockElem blockElem10 = ((List<BlockElem>) blockElemList3).get(2);
		checkDec(blockElem10);
		NameDef nameDef11 = ((Declaration) blockElem10).getNameDef();
		checkNameDef(nameDef11, "pixel", "p");
		Expr expr12 = ((Declaration) blockElem10).getInitializer();
		checkStringLitExpr(expr12, "hello");
	}

	@Test
	void test12a() throws PLCCompilerException {
		String input = """
				int s()<:
				xx = 22
				:>
				""";
		assertThrows(SyntaxException.class, () -> {
			@SuppressWarnings("unused")
			AST ast = getAST(input);
		});
	}

	@Test
	void test13a() throws PLCCompilerException {
		String input = """
				boolean prog()<:
				x = @;
				:>
				""";
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			AST ast = getAST(input);
		});
	}

	@Test
	void test14a() throws PLCCompilerException {
		String input = """
				pixel ppp() <:
				a = 3;
				a[x,y] = 4;
				a[x,y]:red = 5;
				a:green = 5;
				:>
				trailing_stuff
				""";
		assertThrows(SyntaxException.class, () -> {
			@SuppressWarnings("unused")
			AST ast = getAST(input);
		});
	}

	@Test
	void test0() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
""
""";
			AST ast = getAST(input);
			checkStringLitExpr(ast, "");
		});
	}
	@Test
	void test1() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
56
""";
			AST ast = getAST(input);
			checkNumLitExpr(ast, 56);
		});
	}
	@Test
	void test2() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
abcdefg
""";
			AST ast = getAST(input);
			checkIdentExpr(ast, "abcdefg");
		});
	}
	@Test
	void test3() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
"hello there"
""";
			AST ast = getAST(input);
			checkStringLitExpr(ast, "hello there");
		});
	}
	@Test
	void test4() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
TRUE
""";
			AST ast = getAST(input);
			assertThat("", ast, instanceOf(BooleanLitExpr.class));
		});
	}
	@Test
	void test5() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
FALSE
""";
			AST ast = getAST(input);
			assertThat("", ast, instanceOf(BooleanLitExpr.class));
		});
	}
	@Test
	void test6() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
? a -> b , c
""";
			AST ast = getAST(input);
			checkConditionalExpr(ast);
			Expr v0 = ((ConditionalExpr) ast).getGuardExpr();
			checkIdentExpr(v0, "a");
			Expr v1 = ((ConditionalExpr) ast).getTrueExpr();
			checkIdentExpr(v1, "b");
			Expr v2 = ((ConditionalExpr) ast).getFalseExpr();
			checkIdentExpr(v2, "c");
		});
	}
	@Test
	void test7() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
a | b
""";
			AST ast = getAST(input);
			checkBinaryExpr(ast, Kind.BITOR);
			Expr v0 = ((BinaryExpr) ast).getLeftExpr();
			checkIdentExpr(v0, "a");
			Expr v1 = ((BinaryExpr) ast).getRightExpr();
			checkIdentExpr(v1, "b");
		});
	}
	@Test
	void test8() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
a & b
""";
			AST ast = getAST(input);
			checkBinaryExpr(ast, Kind.BITAND);
			Expr v0 = ((BinaryExpr) ast).getLeftExpr();
			checkIdentExpr(v0, "a");
			Expr v1 = ((BinaryExpr) ast).getRightExpr();
			checkIdentExpr(v1, "b");
		});
	}
	@Test
	void test9() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
a == b
""";
			AST ast = getAST(input);
			checkBinaryExpr(ast, Kind.EQ);
			Expr v0 = ((BinaryExpr) ast).getLeftExpr();
			checkIdentExpr(v0, "a");
			Expr v1 = ((BinaryExpr) ast).getRightExpr();
			checkIdentExpr(v1, "b");
		});
	}
	@Test
	void test10() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
a ** b
""";
			AST ast = getAST(input);
			checkBinaryExpr(ast, Kind.EXP);
			Expr v0 = ((BinaryExpr) ast).getLeftExpr();
			checkIdentExpr(v0, "a");
			Expr v1 = ((BinaryExpr) ast).getRightExpr();
			checkIdentExpr(v1, "b");
		});
	}
	@Test
	void test11() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
a + b
""";
			AST ast = getAST(input);
			checkBinaryExpr(ast, Kind.PLUS);
			Expr v0 = ((BinaryExpr) ast).getLeftExpr();
			checkIdentExpr(v0, "a");
			Expr v1 = ((BinaryExpr) ast).getRightExpr();
			checkIdentExpr(v1, "b");
		});
	}
	@Test
	void test12() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
a * b
""";
			AST ast = getAST(input);
			checkBinaryExpr(ast, Kind.TIMES);
			Expr v0 = ((BinaryExpr) ast).getLeftExpr();
			checkIdentExpr(v0, "a");
			Expr v1 = ((BinaryExpr) ast).getRightExpr();
			checkIdentExpr(v1, "b");
		});
	}
	@Test
	void test13() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
[a, b, c]
""";
			AST ast = getAST(input);
			Expr v0 = ((ExpandedPixelExpr) ast).getRed();
			checkIdentExpr(v0, "a");
			Expr v1 = ((ExpandedPixelExpr) ast).getGreen();
			checkIdentExpr(v1, "b");
			Expr v2 = ((ExpandedPixelExpr) ast).getBlue();
			checkIdentExpr(v2, "c");
		});
	}
	@Test
	void test14() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
(a+b)[2+2, 3+3]:green
""";
			AST ast = getAST(input);
			checkPostfixExpr(ast, true, true);
			Expr v0 = ((PostfixExpr) ast).primary();
			checkBinaryExpr(v0, Kind.PLUS);
			Expr v1 = ((BinaryExpr) v0).getLeftExpr();
			checkIdentExpr(v1, "a");
			Expr v2 = ((BinaryExpr) v0).getRightExpr();
			checkIdentExpr(v2, "b");
			PixelSelector v3 = ((PostfixExpr) ast).pixel();
			Expr v4 = ((PixelSelector) v3).xExpr();
			checkBinaryExpr(v4, Kind.PLUS);
			Expr v5 = ((BinaryExpr) v4).getLeftExpr();
			checkNumLitExpr(v5, 2);
			Expr v6 = ((BinaryExpr) v4).getRightExpr();
			checkNumLitExpr(v6, 2);
			Expr v7 = ((PixelSelector) v3).yExpr();
			checkBinaryExpr(v7, Kind.PLUS);
			Expr v8 = ((BinaryExpr) v7).getLeftExpr();
			checkNumLitExpr(v8, 3);
			Expr v9 = ((BinaryExpr) v7).getRightExpr();
			checkNumLitExpr(v9, 3);
			ChannelSelector v10 = ((PostfixExpr) ast).channel();
			checkChannelSelector(v10, Kind.RES_green);
		});
	}
	@Test
	void test15() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
a:green
""";
			AST ast = getAST(input);
			checkPostfixExpr(ast, false, true);
			Expr v0 = ((PostfixExpr) ast).primary();
			checkIdentExpr(v0, "a");
			ChannelSelector v1 = ((PostfixExpr) ast).channel();
			checkChannelSelector(v1, Kind.RES_green);
		});
	}
	@Test
	void test16() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
(? 2+2 -> TRUE , "hello")[a,b]
""";
			AST ast = getAST(input);
			checkPostfixExpr(ast, true, false);
			Expr v0 = ((PostfixExpr) ast).primary();
			checkConditionalExpr(v0);
			Expr v1 = ((ConditionalExpr) v0).getGuardExpr();
			checkBinaryExpr(v1, Kind.PLUS);
			Expr v2 = ((BinaryExpr) v1).getLeftExpr();
			checkNumLitExpr(v2, 2);
			Expr v3 = ((BinaryExpr) v1).getRightExpr();
			checkNumLitExpr(v3, 2);
			Expr v4 = ((ConditionalExpr) v0).getTrueExpr();
			assertThat("", v4, instanceOf(BooleanLitExpr.class));
			Expr v5 = ((ConditionalExpr) v0).getFalseExpr();
			checkStringLitExpr(v5, "hello");
			PixelSelector v6 = ((PostfixExpr) ast).pixel();
			Expr v7 = ((PixelSelector) v6).xExpr();
			checkIdentExpr(v7, "a");
			Expr v8 = ((PixelSelector) v6).yExpr();
			checkIdentExpr(v8, "b");
		});
	}
	@Test
	void test17() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
(? a -> b , c) + (? d -> e , f)
""";
			AST ast = getAST(input);
			checkBinaryExpr(ast, Kind.PLUS);
			Expr v0 = ((BinaryExpr) ast).getLeftExpr();
			checkConditionalExpr(v0);
			Expr v1 = ((ConditionalExpr) v0).getGuardExpr();
			checkIdentExpr(v1, "a");
			Expr v2 = ((ConditionalExpr) v0).getTrueExpr();
			checkIdentExpr(v2, "b");
			Expr v3 = ((ConditionalExpr) v0).getFalseExpr();
			checkIdentExpr(v3, "c");
			Expr v4 = ((BinaryExpr) ast).getRightExpr();
			checkConditionalExpr(v4);
			Expr v5 = ((ConditionalExpr) v4).getGuardExpr();
			checkIdentExpr(v5, "d");
			Expr v6 = ((ConditionalExpr) v4).getTrueExpr();
			checkIdentExpr(v6, "e");
			Expr v7 = ((ConditionalExpr) v4).getFalseExpr();
			checkIdentExpr(v7, "f");
		});
	}
	@Test
	void test18() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
5[2+2,"weird expression"]
""";
			AST ast = getAST(input);
			checkPostfixExpr(ast, true, false);
			Expr v0 = ((PostfixExpr) ast).primary();
			checkNumLitExpr(v0, 5);
			PixelSelector v1 = ((PostfixExpr) ast).pixel();
			Expr v2 = ((PixelSelector) v1).xExpr();
			checkBinaryExpr(v2, Kind.PLUS);
			Expr v3 = ((BinaryExpr) v2).getLeftExpr();
			checkNumLitExpr(v3, 2);
			Expr v4 = ((BinaryExpr) v2).getRightExpr();
			checkNumLitExpr(v4, 2);
			Expr v5 = ((PixelSelector) v1).yExpr();
			checkStringLitExpr(v5, "weird expression");
		});
	}
	@Test
	void test19() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
[2, 3, 4]["hello" + 2,6]:green
""";
			AST ast = getAST(input);
			checkPostfixExpr(ast, true, true);
			Expr v0 = ((PostfixExpr) ast).primary();
			Expr v1 = ((ExpandedPixelExpr) v0).getRed();
			checkNumLitExpr(v1, 2);
			Expr v2 = ((ExpandedPixelExpr) v0).getGreen();
			checkNumLitExpr(v2, 3);
			Expr v3 = ((ExpandedPixelExpr) v0).getBlue();
			checkNumLitExpr(v3, 4);
			PixelSelector v4 = ((PostfixExpr) ast).pixel();
			Expr v5 = ((PixelSelector) v4).xExpr();
			checkBinaryExpr(v5, Kind.PLUS);
			Expr v6 = ((BinaryExpr) v5).getLeftExpr();
			checkStringLitExpr(v6, "hello");
			Expr v7 = ((BinaryExpr) v5).getRightExpr();
			checkNumLitExpr(v7, 2);
			Expr v8 = ((PixelSelector) v4).yExpr();
			checkNumLitExpr(v8, 6);
			ChannelSelector v9 = ((PostfixExpr) ast).channel();
			checkChannelSelector(v9, Kind.RES_green);
		});
	}
	@Test
	void test20() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
DARK_GRAY:green
""";
			AST ast = getAST(input);
			checkPostfixExpr(ast, false, true);
			Expr v0 = ((PostfixExpr) ast).primary();
			assertThat("", v0, instanceOf(ConstExpr.class));
			ChannelSelector v1 = ((PostfixExpr) ast).channel();
			checkChannelSelector(v1, Kind.RES_green);
		});
	}
	@Test
	void test21() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
-2
""";
			AST ast = getAST(input);
			checkUnaryExpr(ast, Kind.MINUS);
			Expr v0 = ((UnaryExpr) ast).getExpr();
			checkNumLitExpr(v0, 2);
		});
	}
	@Test
	void test22() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
!-2
""";
			AST ast = getAST(input);
			checkUnaryExpr(ast, Kind.BANG);
			Expr v0 = ((UnaryExpr) ast).getExpr();
			checkUnaryExpr(v0, Kind.MINUS);
			Expr v1 = ((UnaryExpr) v0).getExpr();
			checkNumLitExpr(v1, 2);
		});
	}
	@Test
	void test23() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
!(a+b)[r,2]:green
""";
			AST ast = getAST(input);
			checkUnaryExpr(ast, Kind.BANG);
			Expr v0 = ((UnaryExpr) ast).getExpr();
			checkPostfixExpr(v0, true, true);
			Expr v1 = ((PostfixExpr) v0).primary();
			checkBinaryExpr(v1, Kind.PLUS);
			Expr v2 = ((BinaryExpr) v1).getLeftExpr();
			checkIdentExpr(v2, "a");
			Expr v3 = ((BinaryExpr) v1).getRightExpr();
			checkIdentExpr(v3, "b");
			PixelSelector v4 = ((PostfixExpr) v0).pixel();
			Expr v5 = ((PixelSelector) v4).xExpr();
			checkIdentExpr(v5, "r");
			Expr v6 = ((PixelSelector) v4).yExpr();
			checkNumLitExpr(v6, 2);
			ChannelSelector v7 = ((PostfixExpr) v0).channel();
			checkChannelSelector(v7, Kind.RES_green);
		});
	}
	@Test
	void test24() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
2+2*4-5
""";
			AST ast = getAST(input);
			checkBinaryExpr(ast, Kind.MINUS);
			Expr v0 = ((BinaryExpr) ast).getLeftExpr();
			checkBinaryExpr(v0, Kind.PLUS);
			Expr v1 = ((BinaryExpr) v0).getLeftExpr();
			checkNumLitExpr(v1, 2);
			Expr v2 = ((BinaryExpr) v0).getRightExpr();
			checkBinaryExpr(v2, Kind.TIMES);
			Expr v3 = ((BinaryExpr) v2).getLeftExpr();
			checkNumLitExpr(v3, 2);
			Expr v4 = ((BinaryExpr) v2).getRightExpr();
			checkNumLitExpr(v4, 4);
			Expr v5 = ((BinaryExpr) ast).getRightExpr();
			checkNumLitExpr(v5, 5);
		});
	}
	@Test
	void test25() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
2**2**3+4
""";
			AST ast = getAST(input);
			checkBinaryExpr(ast, Kind.EXP);
			Expr v0 = ((BinaryExpr) ast).getLeftExpr();
			checkNumLitExpr(v0, 2);
			Expr v1 = ((BinaryExpr) ast).getRightExpr();
			checkBinaryExpr(v1, Kind.EXP);
			Expr v2 = ((BinaryExpr) v1).getLeftExpr();
			checkNumLitExpr(v2, 2);
			Expr v3 = ((BinaryExpr) v1).getRightExpr();
			checkBinaryExpr(v3, Kind.PLUS);
			Expr v4 = ((BinaryExpr) v3).getLeftExpr();
			checkNumLitExpr(v4, 3);
			Expr v5 = ((BinaryExpr) v3).getRightExpr();
			checkNumLitExpr(v5, 4);
		});
	}
	@Test
	void test26() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
(-"hello")
""";
			AST ast = getAST(input);
			checkUnaryExpr(ast, Kind.MINUS);
			Expr v0 = ((UnaryExpr) ast).getExpr();
			checkStringLitExpr(v0, "hello");
		});
	}
	@Test
	void test27() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
2+2>=5-2+b/"hello"&&b+(? a -> b , c) % 2 | b
""";
			AST ast = getAST(input);
			checkBinaryExpr(ast, Kind.BITOR);
			Expr v0 = ((BinaryExpr) ast).getLeftExpr();
			checkBinaryExpr(v0, Kind.AND);
			Expr v1 = ((BinaryExpr) v0).getLeftExpr();
			checkBinaryExpr(v1, Kind.GE);
			Expr v2 = ((BinaryExpr) v1).getLeftExpr();
			checkBinaryExpr(v2, Kind.PLUS);
			Expr v3 = ((BinaryExpr) v2).getLeftExpr();
			checkNumLitExpr(v3, 2);
			Expr v4 = ((BinaryExpr) v2).getRightExpr();
			checkNumLitExpr(v4, 2);
			Expr v5 = ((BinaryExpr) v1).getRightExpr();
			checkBinaryExpr(v5, Kind.PLUS);
			Expr v6 = ((BinaryExpr) v5).getLeftExpr();
			checkBinaryExpr(v6, Kind.MINUS);
			Expr v7 = ((BinaryExpr) v6).getLeftExpr();
			checkNumLitExpr(v7, 5);
			Expr v8 = ((BinaryExpr) v6).getRightExpr();
			checkNumLitExpr(v8, 2);
			Expr v9 = ((BinaryExpr) v5).getRightExpr();
			checkBinaryExpr(v9, Kind.DIV);
			Expr v10 = ((BinaryExpr) v9).getLeftExpr();
			checkIdentExpr(v10, "b");
			Expr v11 = ((BinaryExpr) v9).getRightExpr();
			checkStringLitExpr(v11, "hello");
			Expr v12 = ((BinaryExpr) v0).getRightExpr();
			checkBinaryExpr(v12, Kind.PLUS);
			Expr v13 = ((BinaryExpr) v12).getLeftExpr();
			checkIdentExpr(v13, "b");
			Expr v14 = ((BinaryExpr) v12).getRightExpr();
			checkBinaryExpr(v14, Kind.MOD);
			Expr v15 = ((BinaryExpr) v14).getLeftExpr();
			checkConditionalExpr(v15);
			Expr v16 = ((ConditionalExpr) v15).getGuardExpr();
			checkIdentExpr(v16, "a");
			Expr v17 = ((ConditionalExpr) v15).getTrueExpr();
			checkIdentExpr(v17, "b");
			Expr v18 = ((ConditionalExpr) v15).getFalseExpr();
			checkIdentExpr(v18, "c");
			Expr v19 = ((BinaryExpr) v14).getRightExpr();
			checkNumLitExpr(v19, 2);
			Expr v20 = ((BinaryExpr) ast).getRightExpr();
			checkIdentExpr(v20, "b");
		});
	}
	@Test
	void test28() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
?a -> ? b -> ? c -> 2, 3, 4, 5, 6, 7
""";
			AST ast = getAST(input);
			checkConditionalExpr(ast);
			Expr v0 = ((ConditionalExpr) ast).getGuardExpr();
			checkIdentExpr(v0, "a");
			Expr v1 = ((ConditionalExpr) ast).getTrueExpr();
			checkConditionalExpr(v1);
			Expr v2 = ((ConditionalExpr) v1).getGuardExpr();
			checkIdentExpr(v2, "b");
			Expr v3 = ((ConditionalExpr) v1).getTrueExpr();
			checkConditionalExpr(v3);
			Expr v4 = ((ConditionalExpr) v3).getGuardExpr();
			checkIdentExpr(v4, "c");
			Expr v5 = ((ConditionalExpr) v3).getTrueExpr();
			checkNumLitExpr(v5, 2);
			Expr v6 = ((ConditionalExpr) v3).getFalseExpr();
			checkNumLitExpr(v6, 3);
			Expr v7 = ((ConditionalExpr) v1).getFalseExpr();
			checkNumLitExpr(v7, 4);
			Expr v8 = ((ConditionalExpr) ast).getFalseExpr();
			checkNumLitExpr(v8, 5);
		});
	}
	@Test
	void test29() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
-----(2+2)
""";
			AST ast = getAST(input);
			checkUnaryExpr(ast, Kind.MINUS);
			Expr v0 = ((UnaryExpr) ast).getExpr();
			checkUnaryExpr(v0, Kind.MINUS);
			Expr v1 = ((UnaryExpr) v0).getExpr();
			checkUnaryExpr(v1, Kind.MINUS);
			Expr v2 = ((UnaryExpr) v1).getExpr();
			checkUnaryExpr(v2, Kind.MINUS);
			Expr v3 = ((UnaryExpr) v2).getExpr();
			checkUnaryExpr(v3, Kind.MINUS);
			Expr v4 = ((UnaryExpr) v3).getExpr();
			checkBinaryExpr(v4, Kind.PLUS);
			Expr v5 = ((BinaryExpr) v4).getLeftExpr();
			checkNumLitExpr(v5, 2);
			Expr v6 = ((BinaryExpr) v4).getRightExpr();
			checkNumLitExpr(v6, 2);
		});
	}
	@Test
	void test30() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
width a
""";
			AST ast = getAST(input);
			checkUnaryExpr(ast, Kind.RES_width);
			Expr v0 = ((UnaryExpr) ast).getExpr();
			checkIdentExpr(v0, "a");
		});
	}
	@Test
	void test31() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
""";
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}
	@Test
	void test32() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
a + + + 3
""";
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}
	@Test
	void test33() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
? -> ,
""";
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}
	@Test
	void test34() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
a :
""";
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}
	@Test
	void test35() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
a[2+2,
""";
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}
	@Test
	void test36() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
(a,
""";
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}
	@Test
	void test37() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
[a,a,]
""";
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}
	@Test
	void test38() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
5 /
""";
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}
	@Test
	void test39() throws PLCCompilerException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
!
""";
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

}
