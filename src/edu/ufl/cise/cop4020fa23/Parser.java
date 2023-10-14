/*Copyright 2023 by Beverly A Sanders
 *
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the
 * University of Florida during the fall semester 2023 as part of the course project.
 *
 * No other use is authorized.
 *
 * This code may not be posted on a public web site either during or after the course.
 */
package edu.ufl.cise.cop4020fa23;

import edu.ufl.cise.cop4020fa23.ast.*;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;

import static edu.ufl.cise.cop4020fa23.Kind.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//		Program::= Type IDENT ( ParamList ) Block
//		Block ::= <: (Declaration ; | Statement ;)* :>
//		ParamList ::= ε | NameDef ( , NameDef ) *
//		NameDef ::= Type IDENT | Type Dimension IDENT
//		Type ::= image | pixel | int | string | void | boolean
//		Declaration::= NameDef | NameDef = Expr
//		Expr::= ConditionalExpr | LogicalOrExpr
//		ConditionalExpr ::= ? Expr -> Expr , Expr
//		LogicalOrExpr ::= LogicalAndExpr ( ( | | || ) LogicalAndExpr)*
//		LogicalAndExpr ::= ComparisonExpr ( ( & | && ) ComparisonExpr)*
//		ComparisonExpr ::= PowExpr ( (< | > | == | <= | >=) PowExpr)*
//		PowExpr ::= AdditiveExpr ** PowExpr | AdditiveExpr
//		AdditiveExpr ::= MultiplicativeExpr ( ( + | - ) MultiplicativeExpr )*
//		MultiplicativeExpr ::= UnaryExpr (( * | / | % ) UnaryExpr)*
//		UnaryExpr ::= ( ! | - | width | height) UnaryExpr | PostfixExpr
//		PostfixExpr::= PrimaryExpr (PixelSelector | ε ) (ChannelSelector | ε )
//		PrimaryExpr ::=STRING_LIT | NUM_LIT | IDENT | ( Expr ) | CONST | BOOLEAN_LIT |
//		ExpandedPixelExpr
//		ChannelSelector ::= : red | : green | : blue
//		PixelSelector ::= [ Expr , Expr ]
//		ExpandedPixelExpr ::= [ Expr , Expr , Expr ]
//		Dimension ::= [ Expr , Expr ]
//		LValue ::= IDENT (PixelSelectorIn | ε ) (ChannelSelector | ε )
//		Statement::=
//		LValue = Expr |
//		write Expr |
//		do GuardedBlock [] GuardedBlock* od |
//		if GuardedBlock [] GuardedBlock* if |
//		^ Expr |
//		BlockStatement |
//		GuardedBlock := Expr -> Block
//		BlockStatement ::= Block

public class Parser implements IParser {

	final ILexer tokens;
	private IToken currentToken;

	public Parser(ILexer lexer) throws LexicalException {
		this.tokens = lexer;
		currentToken = lexer.next();
	}

	@Override
	public AST parse() throws PLCCompilerException {
		AST ast = program();
		if (currentToken.kind() != Kind.EOF) {
			throw new SyntaxException("Expected end of input but found " + currentToken.kind());
		}
		return ast;
	}

	private Program program() throws PLCCompilerException {
		IToken firstToken = currentToken;
		IToken typeToken = currentToken;
		matchType();
		IToken identToken = currentToken;
		match(Kind.IDENT);
		match(Kind.LPAREN);
		List<NameDef> params = paramList();
		match(Kind.RPAREN);
		Block block = block();
		return new Program(firstToken, typeToken, identToken, params, block);
	}

	private List<NameDef> paramList() throws PLCCompilerException {
		List<NameDef> params = new ArrayList<>();
		while (currentToken.kind() != Kind.RPAREN) {
			params.add(nameDef());
			if (currentToken.kind() == Kind.COMMA) {
				match(Kind.COMMA);
			}
		}
		return params;
	}

	private NameDef nameDef() throws PLCCompilerException {
		IToken firstToken = currentToken;
		IToken typeToken = currentToken;
		matchType();
		Dimension dimension = null;
		if (currentToken.kind() == Kind.LSQUARE) {
			dimension = dimension();
		}
		IToken identToken = currentToken;
		match(Kind.IDENT);
		return new NameDef(firstToken, typeToken, dimension, identToken);
	}

	private Dimension dimension() throws PLCCompilerException {
		IToken firstToken = currentToken;
		match(Kind.LSQUARE);
		Expr width = expr();
		match(Kind.COMMA);
		Expr height = expr();
		match(Kind.RSQUARE);
		return new Dimension(firstToken, width, height);
	}


	private Block block() throws PLCCompilerException {
		if(currentToken.kind() == Kind.RARROW) {
			throw new SyntaxException("RARROW found but not handled. Update the logic.");
		}

		if(currentToken.kind() != Kind.BLOCK_OPEN) {
			throw new SyntaxException("Expected BLOCK_OPEN but found " + currentToken.kind());
		}

		match(Kind.BLOCK_OPEN);
		List<Block.BlockElem> elems = new ArrayList<>();
		while (currentToken.kind() != Kind.BLOCK_CLOSE) {
			if (isType(currentToken.kind())) {
				elems.add(declaration());
				if (currentToken.kind() == Kind.SEMI) {
					match(Kind.SEMI);
				}
			} else {
				elems.add(statement());
				if (currentToken.kind() == Kind.SEMI) {
					match(Kind.SEMI);
				}
			}
		}
		match(Kind.BLOCK_CLOSE);
		return new Block(currentToken, elems);
	}



	private boolean isType(Kind kind) {
		return kind == Kind.RES_image || kind == Kind.RES_pixel || kind == Kind.RES_int ||
				kind == Kind.RES_string || kind == Kind.RES_void || kind == Kind.RES_boolean;
	}

	private Declaration declaration() throws PLCCompilerException {
		IToken startToken = currentToken;
		NameDef nameDef = nameDef();
		Expr initializer = null;
		if (currentToken.kind() == Kind.ASSIGN) {
			match(Kind.ASSIGN);
			initializer = expr();
		}
		match(Kind.SEMI);
		return new Declaration(startToken, nameDef, initializer);
	}


	private Statement statement() throws PLCCompilerException {
		switch (currentToken.kind()) {
			case RES_write:
				match(Kind.RES_write);
				Expr e = expr();
				match(Kind.SEMI);
				return new WriteStatement(currentToken, e);

			case IDENT:
				LValue lValue = lValue();
				if (currentToken.kind() == Kind.ASSIGN) {
					match(Kind.ASSIGN);
					Expr value = expr();
					match(Kind.SEMI);
					return new AssignmentStatement(currentToken, lValue, value);
				} else {
					throw new SyntaxException("Expected ASSIGN but found " + currentToken.kind());
				}

			case RETURN:
				match(Kind.RETURN);
				Expr returnValue = expr();
				match(Kind.SEMI);
				return new ReturnStatement(currentToken, returnValue);

			case RES_if:
				match(Kind.RES_if);
				List<GuardedBlock> guardedBlocks = new ArrayList<>();
				guardedBlocks.add(guardedBlock());

				while (currentToken.kind() == Kind.BOX) {
					match(Kind.BOX);
					guardedBlocks.add(guardedBlock());
				}
				match(Kind.RES_fi);
				return new IfStatement(currentToken, guardedBlocks);

			case RES_do:
				match(Kind.RES_do);
				List<GuardedBlock> doGuardedBlocks = new ArrayList<>();
				doGuardedBlocks.add(guardedBlock());

				while (currentToken.kind() == Kind.BOX) {
					match(Kind.BOX);
					doGuardedBlocks.add(guardedBlock());
				}
				match(Kind.RES_od);
				return new DoStatement(currentToken, doGuardedBlocks);

			case BLOCK_OPEN:
				Statement blockStmt = blockStatement();
				match(Kind.SEMI);
				return blockStmt;

			default:
				throw new SyntaxException("Unexpected token in statement: " + currentToken.kind());
		}
	}





	private StatementBlock blockStatement() throws PLCCompilerException {
		Block blockContent = block();
		return new StatementBlock(currentToken, blockContent);
	}

	private LValue lValue() throws PLCCompilerException {
		if (currentToken.kind() == Kind.IDENT) {
			IToken nameToken = currentToken;
			match(Kind.IDENT);

			PixelSelector pixelSelector = null;
			if (currentToken.kind() == Kind.LSQUARE) {
				pixelSelector = pixelSelector();
			}

			ChannelSelector channelSelector = null;
			if (currentToken.kind() == Kind.COLON) {
				channelSelector = channelSelector();
			}

			return new LValue(currentToken, nameToken, pixelSelector, channelSelector);
		} else {
			throw new SyntaxException("Expected IDENT but found: " + currentToken.kind());
		}
	}

	private PixelSelector pixelSelector() throws PLCCompilerException {
		match(Kind.LSQUARE);
		Expr xCoord = expr();
		match(Kind.COMMA);
		Expr yCoord = expr();
		if (currentToken.kind() == Kind.COMMA) {
			throw new SyntaxException("Unexpected COMMA after second expression in pixel selector.");
		}
		match(Kind.RSQUARE);
		return new PixelSelector(currentToken, xCoord, yCoord);
	}


	private ChannelSelector channelSelector() throws PLCCompilerException {
		IToken channelToken = currentToken;
		match(Kind.COLON);
		IToken colorToken = currentToken;
		switch (colorToken.kind()) {
			case RES_red:
				match(Kind.RES_red);
				break;
			case RES_green:
				match(Kind.RES_green);
				break;
			case RES_blue:
				match(Kind.RES_blue);
				break;
			default:
				throw new SyntaxException("Expected a color channel but found: " + colorToken.kind());
		}
		return new ChannelSelector(channelToken, colorToken);
	}

	private GuardedBlock guardedBlock() throws PLCCompilerException {
		Expr condition = expr();

		match(Kind.RARROW);

		Block block = block();

		return new GuardedBlock(currentToken, condition, block);
	}


	private Expr expr() throws PLCCompilerException {
		Expr result = additiveExpr();
		while (true) {
			IToken opToken = currentToken;
			switch (opToken.kind()) {
				case PLUS:
					match(PLUS);
					IToken leftToken = result.firstToken;
					result = new BinaryExpr(leftToken, result, opToken, additiveExpr());
					break;
				case MINUS:
					match(MINUS);
					leftToken = result.firstToken;
					result = new BinaryExpr(leftToken, result, opToken, additiveExpr());
					break;
				default:
					return result;
			}
		}
	}


	private Expr additiveExpr() throws PLCCompilerException {
		Expr result = term();
		while (true) {
			IToken opToken = currentToken;
			switch (opToken.kind()) {
				case TIMES:
					match(TIMES);
					IToken leftToken = result.firstToken;
					result = new BinaryExpr(leftToken, result, opToken, term());
					break;
				case DIV:
					match(DIV);
					leftToken = result.firstToken;
					result = new BinaryExpr(leftToken, result, opToken, term());
					break;

				default:
					return result;
			}
		}
	}


	private Expr term() throws PLCCompilerException {
		switch (currentToken.kind()) {
			case NUM_LIT:
				IToken numToken = currentToken;
				match(Kind.NUM_LIT);
				return new NumLitExpr(numToken);

			case CONST:
				IToken constToken = currentToken;
				match(Kind.CONST);
				return new ConstExpr(constToken);

			case IDENT:
				IToken identToken = currentToken;
				match(Kind.IDENT);
				return new IdentExpr(identToken);

			case LSQUARE:
				IToken firstToken = currentToken;
				match(Kind.LSQUARE);
				Expr red = expr();
				match(Kind.COMMA);
				Expr green = expr();
				match(Kind.COMMA);
				Expr blue = expr();
				match(Kind.RSQUARE);
				return new ExpandedPixelExpr(firstToken, red, green, blue);

			case STRING_LIT:
				IToken stringToken = currentToken;
				match(Kind.STRING_LIT);
				return new StringLitExpr(stringToken);

			case BOOLEAN_LIT:
				IToken boolToken = currentToken;
				match(Kind.BOOLEAN_LIT);
				return new BooleanLitExpr(boolToken);

			default:
				throw new SyntaxException("Unexpected token in expression: " + currentToken.kind());
		}
	}



	private void match(Kind expected) throws PLCCompilerException {
		if (currentToken.kind() == expected) {
			currentToken = tokens.next();
		} else {
			throw new SyntaxException("Expected " + expected + " but found " + currentToken.kind());
		}
	}

	private void matchType() throws PLCCompilerException {
		if (isType(currentToken.kind())) {
			currentToken = tokens.next();
		} else {
			throw new SyntaxException("Expected a type but found " + currentToken.kind());
		}
	}
}
