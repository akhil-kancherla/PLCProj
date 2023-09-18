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

import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;

import java.util.HashMap;
import java.util.Map;

import static edu.ufl.cise.cop4020fa23.Kind.*;


public class Lexer implements ILexer {

	String input;
	private int currentPosition = 0;
	private int currentLine = 1;
	private int currentColumn = 0;
	private char[] chars;
	private int startPos;
	private enum State {
		START, IN_IDENT, HAVE_STRING_LIT
		, IN_NUM
	}
	private State state = State.START;
	private static final Map<String, Kind> keywords = new HashMap<>();
	static {
		keywords.put("image",    RES_image);
		keywords.put("pixel",  RES_pixel);
		keywords.put("int",   RES_int);
		keywords.put("string",  RES_string);
		keywords.put("void",    RES_void);
		keywords.put("boolean",    RES_boolean);
		keywords.put("write",     RES_write);
		keywords.put("height",    RES_height);
		keywords.put("width",     RES_width);
		keywords.put("if",  RES_if);
		keywords.put("fi", RES_fi);
		keywords.put("do",  RES_do);
		keywords.put("od",   RES_od);
		keywords.put("red",   RES_red);
		keywords.put("green",    RES_green);
		keywords.put("blue",  RES_blue);
		keywords.put("Z", CONST);
		keywords.put("BLACK", CONST);
		keywords.put("BLUE", CONST);
		keywords.put("CYAN", CONST);
		keywords.put("DARK_GRAY", CONST);
		keywords.put("GRAY", CONST);
		keywords.put("GREEN", CONST);
		keywords.put("LIGHT_GRAY", CONST);
		keywords.put("MAGENTA", CONST);
		keywords.put("ORANGE", CONST);
		keywords.put("PINK", CONST);
		keywords.put("RED", CONST);
		keywords.put("WHITE", CONST);
		keywords.put("YELLOW", CONST);
		keywords.put("TRUE", BOOLEAN_LIT);
		keywords.put("FALSE", BOOLEAN_LIT);
	}





	public Lexer(String input) {
		this.input = input;
		this.chars = (input + '\0').toCharArray();
	}

	@Override
	public IToken next() throws LexicalException {
		if (currentPosition >= input.length()) {
			return new Token(Kind.EOF, 0, 0, new char[0], new SourceLocation(currentLine, currentColumn));
		}

		while(currentPosition < input.length()){
			char currentChar = chars[currentPosition];
				switch(state){

					case START:
						startPos = currentPosition;
						switch(currentChar){
							case ' ', '\t', '\r', '\n' ->{
								currentPosition++;
								currentColumn++;
								if (currentChar == '\n'){
									currentLine++;
									currentColumn = 0;
								}
							}
							case ','->{currentPosition++;currentColumn++;
								return new Token(Kind.COMMA,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case ';'->{currentPosition++;currentColumn++;
								return new Token(Kind.SEMI,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '?'->{currentPosition++;currentColumn++;
								return new Token(Kind.QUESTION,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '('->{currentPosition++;currentColumn++;
								return new Token(Kind.LPAREN,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case ')'->{currentPosition++;currentColumn++;
								return new Token(Kind.RPAREN,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '!'->{currentPosition++;currentColumn++;
								return new Token(Kind.BANG,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '+'->{currentPosition++;currentColumn++;
								return new Token(Kind.PLUS,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
								}

							case '-'->{
								if((chars[currentPosition+1] == '>')) {
									currentPosition += 2;
									currentColumn+=2;
									return new Token(RARROW, startPos, 2, chars, new SourceLocation(currentLine, currentColumn-1));
								}
								currentPosition++;
								currentColumn++;
								return new Token(MINUS,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '/'->{currentPosition++;currentColumn++;
								return new Token(Kind.DIV,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '%'->{currentPosition++;currentColumn++;
								return new Token(Kind.MOD,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '^'->{currentPosition++;currentColumn++;
								return new Token(Kind.RETURN,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '&'->{
								if((chars[currentPosition+1] == '&')){
									currentPosition+=2;
									currentColumn+=2;
									return new Token(AND,startPos,2, chars, new SourceLocation(currentLine, currentColumn-1));
								}
								currentPosition++;
								currentColumn++;
								return new Token(BITAND,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '*'->{
								if((chars[currentPosition+1] == '*')){
									currentPosition+=2;
									currentColumn+=2;
									return new Token(Kind.EXP,startPos,2, chars, new SourceLocation(currentLine, currentColumn-1));
								}
								currentPosition++;
								currentColumn++;
								return new Token(Kind.TIMES,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}

							case '|'-> {
								if((chars[currentPosition+1] == '|')){
									currentPosition+=2;
									currentColumn+=2;
									return new Token(OR,startPos,2, chars, new SourceLocation(currentLine, currentColumn-1));
								}
								currentPosition++;
								currentColumn++;
								return new Token(Kind.BITOR,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}

							case '<'-> {
								if((chars[currentPosition+1] == '=')){
									currentPosition+=2;
									currentColumn+=2;
									return new Token(LE,startPos,2, chars, new SourceLocation(currentLine, currentColumn-1));
								} else if ((chars[currentPosition+1] == ':')){
									currentPosition+=2;
									currentColumn+=2;
									return new Token(BLOCK_OPEN,startPos,2, chars, new SourceLocation(currentLine, currentColumn-1));
								}
								currentPosition++;
								currentColumn++;
								return new Token(Kind.LT,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}

							case '>'->{
								if((chars[currentPosition+1] == '=')) {
									currentPosition += 2;
									currentColumn+=2;
									return new Token(GE, startPos, 2, chars, new SourceLocation(currentLine, currentColumn-1));
								}
								currentPosition++;
								currentColumn++;
								return new Token(Kind.GT,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}

							case ':'->{
								if((chars[currentPosition+1] == '>')) {
									currentPosition += 2;
									currentColumn+=2;
									return new Token(BLOCK_CLOSE, startPos, 2, chars, new SourceLocation(currentLine, currentColumn-1));
								}
								currentPosition++;
								currentColumn++;
								return new Token(Kind.COLON,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}

							case '['-> {
								if((chars[currentPosition+1] == ']')){
									currentPosition+=2;
									currentColumn+=2;
									return new Token(BOX,startPos,2, chars, new SourceLocation(currentLine, currentColumn-1));
								}
								currentPosition++;
								currentColumn++;
								return new Token(LSQUARE,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}

							case ']'-> {
								currentPosition++;
								currentColumn++;
								return new Token(RSQUARE,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}

							case '#'-> {
								if((chars[currentPosition+1] == '#')){
									//currentPosition+=2;
									while (chars[currentPosition] != '\n' && chars[currentPosition] != '\r' && chars[currentPosition] != '\t'){
										currentPosition++;
										currentColumn++;
									}
									//currentLine++;
									state = State.START;
								} else {
									throw new LexicalException("Singular # is not accepted");
								}
							}

							case '='->{
								if((chars[currentPosition+1] == '=')) {
									currentPosition += 2;
									currentColumn+=2;
									return new Token(EQ, startPos, 2, chars, new SourceLocation(currentLine, currentColumn-1));
								}
								currentPosition++;
								currentColumn++;
								return new Token(ASSIGN,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}

							default ->{
								if(Character.isLetter(currentChar) || currentChar == '_'){
									state = State.IN_IDENT;
								} else if (Character.isDigit(currentChar)){
									if (currentChar == '0'){
										state = State.START;
										currentPosition++;
										currentColumn++;
										return new Token(NUM_LIT, startPos, 1, chars, new SourceLocation(currentLine, currentColumn));
									}
									state = State.IN_NUM;
								} else if (currentChar == '\"'){
									state = State.HAVE_STRING_LIT;
								} else {
									throw new LexicalException("Unrecognized token");
								}
							}


					}
					break;
					case IN_IDENT:
						currentPosition++;
						currentColumn++;
						int identLength = 0;
						while (Character.isLetter(chars[currentPosition])|| Character.isDigit(chars[currentPosition])|| chars[currentPosition] == '_'){
							currentPosition++;
							currentColumn++;
							identLength++;
						}
						if (keywords.containsKey(input.substring(startPos, (currentPosition)))){
							state = State.START;
							Kind val = keywords.get(input.substring(startPos, (currentPosition)));
							return new Token(val, startPos, (currentPosition)-(startPos), chars, new SourceLocation(currentLine, currentColumn-identLength));
						}
						state = State.START;
						return new Token(IDENT, startPos, currentPosition-startPos, chars, new SourceLocation(currentLine, currentColumn-identLength));

					case HAVE_STRING_LIT:
						currentPosition++; // Move past the opening double quotation mark
						int stringLength = 0;
						while (currentPosition < input.length()) {
							if (chars[currentPosition] == '"') {
								currentPosition++; // Move past the closing double quotation mark
								state = State.START;
								return new Token(STRING_LIT, startPos, (currentPosition) - (startPos), chars, new SourceLocation(currentLine, currentColumn-stringLength+1));
							} else if (chars[currentPosition] == '\n') {
								// Handle newline character within an unterminated string
								throw new LexicalException("Unterminated string literal");
							} else if (chars[currentPosition] == '\\') {
								// Handle escape sequences, such as \" or \\
								currentPosition++;
								currentColumn++;
								char nextChar = chars[currentPosition];
								if (nextChar == '\\' || nextChar == '"') {
									currentPosition++;
									currentColumn++;
									stringLength += 2;

								} else {
									throw new LexicalException("Invalid escape sequence in string literal");
								}
							} else {
								currentPosition++;
								currentColumn++;
								stringLength++;
							}
						}

						// If the loop finishes without encountering the closing double quotation mark
						throw new LexicalException("Unterminated string literal");
					case IN_NUM:
						currentPosition++;
						currentColumn++;
						while (Character.isDigit(chars[currentPosition])){
							currentPosition++;
							currentColumn++;
						}
						state = State.START;
						String numLiteral = input.substring(startPos,currentPosition);
						int length = numLiteral.length();

						try {
							int numValue = Integer.parseInt(numLiteral);
							// Check if the parsed integer is within the valid range
							if (numValue < Integer.MAX_VALUE) {
								state = State.START;
								return new Token(Kind.NUM_LIT, startPos, currentPosition - startPos, chars, new SourceLocation(currentLine, currentColumn-length+1));
							} else {
								// The parsed integer is out of range
								throw new LexicalException("Numeric literal is out of range");
							}
						} catch (NumberFormatException e) {
							// Parsing failed, rethrow as LexicalException
							throw new LexicalException("Invalid numeric literal");
						}
					default: throw new IllegalStateException("Lexer bug");
				}
		}


		return new Token(Kind.EOF, 0, 0, new char[0], new SourceLocation(currentLine, currentColumn));

	}

}
