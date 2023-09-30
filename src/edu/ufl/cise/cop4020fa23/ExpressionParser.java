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

import static edu.ufl.cise.cop4020fa23.Kind.AND;
import static edu.ufl.cise.cop4020fa23.Kind.BANG;
import static edu.ufl.cise.cop4020fa23.Kind.BITAND;
import static edu.ufl.cise.cop4020fa23.Kind.BITOR;
import static edu.ufl.cise.cop4020fa23.Kind.COLON;
import static edu.ufl.cise.cop4020fa23.Kind.COMMA;
import static edu.ufl.cise.cop4020fa23.Kind.DIV;
import static edu.ufl.cise.cop4020fa23.Kind.EOF;
import static edu.ufl.cise.cop4020fa23.Kind.EQ;
import static edu.ufl.cise.cop4020fa23.Kind.EXP;
import static edu.ufl.cise.cop4020fa23.Kind.GE;
import static edu.ufl.cise.cop4020fa23.Kind.GT;
import static edu.ufl.cise.cop4020fa23.Kind.IDENT;
import static edu.ufl.cise.cop4020fa23.Kind.LE;
import static edu.ufl.cise.cop4020fa23.Kind.LPAREN;
import static edu.ufl.cise.cop4020fa23.Kind.LSQUARE;
import static edu.ufl.cise.cop4020fa23.Kind.LT;
import static edu.ufl.cise.cop4020fa23.Kind.MINUS;
import static edu.ufl.cise.cop4020fa23.Kind.MOD;
import static edu.ufl.cise.cop4020fa23.Kind.NUM_LIT;
import static edu.ufl.cise.cop4020fa23.Kind.OR;
import static edu.ufl.cise.cop4020fa23.Kind.PLUS;
import static edu.ufl.cise.cop4020fa23.Kind.QUESTION;
import static edu.ufl.cise.cop4020fa23.Kind.RARROW;
import static edu.ufl.cise.cop4020fa23.Kind.RES_blue;
import static edu.ufl.cise.cop4020fa23.Kind.RES_green;
import static edu.ufl.cise.cop4020fa23.Kind.RES_height;
import static edu.ufl.cise.cop4020fa23.Kind.RES_red;
import static edu.ufl.cise.cop4020fa23.Kind.RES_width;
import static edu.ufl.cise.cop4020fa23.Kind.RPAREN;
import static edu.ufl.cise.cop4020fa23.Kind.RSQUARE;
import static edu.ufl.cise.cop4020fa23.Kind.STRING_LIT;
import static edu.ufl.cise.cop4020fa23.Kind.TIMES;
import static edu.ufl.cise.cop4020fa23.Kind.CONST;

import java.util.Arrays;

import edu.ufl.cise.cop4020fa23.ast.AST;
import edu.ufl.cise.cop4020fa23.ast.BinaryExpr;
import edu.ufl.cise.cop4020fa23.ast.BooleanLitExpr;
import edu.ufl.cise.cop4020fa23.ast.ChannelSelector;
import edu.ufl.cise.cop4020fa23.ast.ConditionalExpr;
import edu.ufl.cise.cop4020fa23.ast.ConstExpr;
import edu.ufl.cise.cop4020fa23.ast.ExpandedPixelExpr;
import edu.ufl.cise.cop4020fa23.ast.Expr;
import edu.ufl.cise.cop4020fa23.ast.IdentExpr;
import edu.ufl.cise.cop4020fa23.ast.NumLitExpr;
import edu.ufl.cise.cop4020fa23.ast.PixelSelector;
import edu.ufl.cise.cop4020fa23.ast.PostfixExpr;
import edu.ufl.cise.cop4020fa23.ast.StringLitExpr;
import edu.ufl.cise.cop4020fa23.ast.UnaryExpr;
import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;
import edu.ufl.cise.cop4020fa23.exceptions.PLCCompilerException;
import edu.ufl.cise.cop4020fa23.exceptions.SyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
/**
 Expr::=  ConditionalExpr | LogicalOrExpr
 ConditionalExpr ::=  ?  Expr  :  Expr  :  Expr
 LogicalOrExpr ::= LogicalAndExpr (    (   |   |   ||   ) LogicalAndExpr)*
 LogicalAndExpr ::=  ComparisonExpr ( (   &   |  &&   )  ComparisonExpr)*
 ComparisonExpr ::= PowExpr ( (< | > | == | <= | >=) PowExpr)*
 PowExpr ::= AdditiveExpr ** PowExpr |   AdditiveExpr
 AdditiveExpr ::= MultiplicativeExpr ( ( + | -  ) MultiplicativeExpr )*
 MultiplicativeExpr ::= UnaryExpr (( * |  /  |  % ) UnaryExpr)*
 UnaryExpr ::=  ( ! | - | length | width) UnaryExpr  |  UnaryExprPostfix
 UnaryExprPostfix::= PrimaryExpr (PixelSelector | ε ) (ChannelSelector | ε )
 PrimaryExpr ::=STRING_LIT | NUM_LIT |  IDENT | ( Expr ) | Z
 ExpandedPixel
 ChannelSelector ::= : red | : green | : blue
 PixelSelector  ::= [ Expr , Expr ]
 ExpandedPixel ::= [ Expr , Expr , Expr ]
 Dimension  ::=  [ Expr , Expr ]

 */

public class ExpressionParser implements IParser {
	private String input;
	final ILexer tokens;
	private IToken currentToken;


	/**
	 * @param lexer
	 * @throws LexicalException
	 */
	public ExpressionParser(ILexer lexer) throws LexicalException {
		super();
		this.tokens = lexer;
		currentToken = lexer.next();
	}


	@Override
	public AST parse() throws PLCCompilerException {
		Expr e = expr();
		if (currentToken.kind() != EOF) {
			throw new SyntaxException("Expected end of input but found " + currentToken.kind());
		}
		return e;
	}


	private Expr expr() throws PLCCompilerException {
		return conditionalExpr();
	}

	private Expr conditionalExpr() throws PLCCompilerException {
		if (currentToken.kind() == QUESTION) {
			IToken firstToken = currentToken; // Store the current token for later use
			match(QUESTION);
			Expr guardExpr = expr();
			match(RARROW);
			Expr trueExpr = expr();
			match(COMMA);
			Expr falseExpr = expr();
			return new ConditionalExpr(firstToken, guardExpr, trueExpr, falseExpr);
		}
		return logicalOrExpr();
	}

	private Expr logicalOrExpr() throws PLCCompilerException {
		Expr e = logicalAndExpr();
		while (currentToken.kind() == OR || currentToken.kind() == BITOR) {
			IToken opToken = currentToken; // Store the current token before matching
			match(currentToken.kind()); // Match the current operator
			Expr e2 = logicalAndExpr();
			e = new BinaryExpr(opToken, e, opToken, e2);
		}
		return e;
	}

	private Expr logicalAndExpr() throws PLCCompilerException {
		Expr e = comparisonExpr();
		while (currentToken.kind() == AND || currentToken.kind() == BITAND) {
			IToken opToken = currentToken; // Store the current token before matching
			match(opToken.kind());
			Expr e2 = comparisonExpr();
			e = new BinaryExpr(opToken, e, opToken, e2);
		}
		return e;
	}

	private Expr comparisonExpr() throws PLCCompilerException {
		Expr e = powExpr();
		while (Arrays.asList(LT, GT, LE, GE, EQ).contains(currentToken.kind())) {
			IToken opToken = currentToken; // Store the current token before matching
			match(opToken.kind());
			Expr e2 = powExpr();
			e = new BinaryExpr(opToken, e, opToken, e2);
		}
		return e;
	}

	private Expr powExpr() throws PLCCompilerException {
		Expr e = additiveExpr();
		if (currentToken.kind() == EXP) {
			IToken opToken = currentToken; // Store the current token before matching
			match(EXP);
			Expr e2 = powExpr();
			e = new BinaryExpr(opToken, e, opToken, e2);
		}
		return e;
	}

	private Expr additiveExpr() throws PLCCompilerException {
		Expr e = multiplicativeExpr();
		while (currentToken.kind() == PLUS || currentToken.kind() == MINUS) {
			IToken opToken = currentToken; // Store the current token before matching
			match(opToken.kind());
			Expr e2 = multiplicativeExpr();
			e = new BinaryExpr(opToken, e, opToken, e2);
		}
		return e;
	}

	private Expr multiplicativeExpr() throws PLCCompilerException {
		Expr e = unaryExpr();
		while (Arrays.asList(TIMES, DIV, MOD).contains(currentToken.kind())) {
			IToken opToken = currentToken; // Store the current token before matching
			match(opToken.kind());
			Expr e2 = unaryExpr();
			e = new BinaryExpr(opToken, e, opToken, e2);
		}
		return e;
	}

	private Expr unaryExpr() throws PLCCompilerException {
		if (Arrays.asList(BANG, MINUS).contains(currentToken.kind())) {
			IToken opToken = currentToken; // Store the current token before matching
			match(opToken.kind());
			Expr e = unaryExpr();
			return new UnaryExpr(opToken, opToken, e);
		} else if (Arrays.asList(RES_height, RES_width).contains(currentToken.kind())) {
			IToken opToken = currentToken; // Store the current token before matching
			match(opToken.kind());
			Expr e = unaryExpr();
			return new UnaryExpr(opToken, opToken, e);
		}
		return unaryExprPostfix();
	}



	private Expr unaryExprPostfix() throws PLCCompilerException {
		Expr e = primaryExpr();
		PixelSelector pixelSelector = null;
		ChannelSelector channelSelector = null;

		if (currentToken.kind() == LSQUARE) {
			IToken firstTokenForPixelSelector = currentToken; // Store the current token before matching
			match(LSQUARE);
			Expr x = expr();
			match(COMMA);
			Expr y = expr();
			match(RSQUARE);
			pixelSelector = new PixelSelector(firstTokenForPixelSelector, x, y);
		}

		if (currentToken.kind() == COLON) {
			IToken firstTokenForChannelSelector = currentToken; // Store the current token before matching
			match(COLON);
			IToken colorToken = currentToken; // Store the current token for color
			match(colorToken.kind());
			channelSelector = new ChannelSelector(firstTokenForChannelSelector, colorToken);
		}

		if (pixelSelector != null || channelSelector != null) {
			e = new PostfixExpr(e.firstToken(), e, pixelSelector, channelSelector);
		}

		return e;
	}

	private Expr primaryExpr() throws PLCCompilerException {
		Expr e = null;
		if (currentToken == null || currentToken.kind() == EOF) {
			throw new SyntaxException("Unexpected end of input");
		}
		switch (currentToken.kind()) {
			case IDENT -> {
				e = new IdentExpr(currentToken);
				match(IDENT);
			}
			case NUM_LIT -> {
				e = new NumLitExpr(currentToken);
				match(NUM_LIT);
			}
			case STRING_LIT -> {
				e = new StringLitExpr(currentToken);
				match(STRING_LIT);
			}
			case LPAREN -> {
				match(LPAREN);
				e = expr();
				match(RPAREN);
			}
			case CONST -> { // Assuming Z and MAGENTA are constants
				e = new ConstExpr(currentToken);
				match(CONST);
			}
			default -> throw new SyntaxException("Unexpected token: " + currentToken);
		}
		return e;
	}

	private void match(Kind kind) throws PLCCompilerException {
		if (currentToken.kind() == kind) {
			consume();
		} else {
			throw new SyntaxException("Expected " + kind + " but found " + currentToken.kind());
		}
	}

	private void consume() throws PLCCompilerException {
		currentToken = tokens.next();
	}

}