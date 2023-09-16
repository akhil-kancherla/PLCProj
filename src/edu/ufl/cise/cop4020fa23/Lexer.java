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

import static edu.ufl.cise.cop4020fa23.Kind.*;


public class Lexer implements ILexer {

	String input;
	private int currentPosition = 0;
	private int currentLine = 1;
	private int currentColumn = 1;
	private char[] chars;
	private int startPos;
	private enum State {
		START, IN_IDENT, HAVE_ZERO, HAVE_DOT,
		IN_FLOAT, IN_NUM, HAVE_EQ, HAVE_MINUS
	}
	private State state = State.START;




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
							case ' ', '\t', '\r', '\n' ->{currentPosition++;}
							case ','->{currentPosition++;
								return new Token(Kind.COMMA,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case ';'->{currentPosition++;
								return new Token(Kind.SEMI,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '?'->{currentPosition++;
								return new Token(Kind.QUESTION,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '('->{currentPosition++;
								return new Token(Kind.LPAREN,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case ')'->{currentPosition++;
								return new Token(Kind.RPAREN,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '!'->{currentPosition++;
								return new Token(Kind.BANG,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '+'->{currentPosition++;
								return new Token(Kind.PLUS,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
								}
							case '-'->{currentPosition++;
								return new Token(Kind.MINUS,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '/'->{currentPosition++;
								return new Token(Kind.DIV,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '%'->{currentPosition++;
								return new Token(Kind.MOD,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}
							case '^'->{currentPosition++;
								return new Token(Kind.RETURN,startPos,1, chars, new SourceLocation(currentLine, currentColumn));
							}

							case '='->{state = State.HAVE_EQ;}
//
							//case 0 ->{return new Token(Kind.EOF, 0, 0, new char[0], new SourceLocation(currentLine, currentColumn));}

							default ->{
								//return new Token(Kind.EOF, 0, 0, chars, new SourceLocation(currentLine, currentColumn));
								if(Character.isLetter(currentChar) || currentChar == '_'){
									state = State.IN_IDENT;
								} else if (Character.isDigit(currentChar)){
									if (currentChar == '0'){
										state = State.START;
										currentPosition++;
										return new Token(NUM_LIT, startPos, 1, chars, new SourceLocation(currentLine, currentColumn));
									}
									state = State.IN_NUM;
								}
							}


					}
					break;
					case IN_IDENT:
						currentPosition++;
						while (Character.isLetter(chars[currentPosition])|| chars[currentPosition] == '_'){
							currentPosition++;
						}
						state = State.START;
						return new Token(IDENT, startPos, currentPosition-startPos, chars, new SourceLocation(currentLine, currentColumn));

					case HAVE_ZERO:
					case HAVE_DOT:
					case IN_FLOAT:
					case IN_NUM:
						currentPosition++;
						while (Character.isDigit(chars[currentPosition])){
							currentPosition++;
						}
						state = State.START;
						return new Token(NUM_LIT, startPos, currentPosition-startPos, chars, new SourceLocation(currentLine, currentColumn));

					case HAVE_EQ:
						switch (currentChar) {
							case '=' -> {
								if (chars[currentPosition+1] =='='){
									currentPosition += 2;
									state = State.START;
									return new Token(Kind.EQ, currentPosition, 2, chars, new SourceLocation(currentLine, currentColumn));

								} else {
									currentPosition += 1;
									state = State.START;
									return new Token(Kind.ASSIGN, currentPosition, 1, chars, new SourceLocation(currentLine, currentColumn));
								}
							}

							default -> {
								return new Token(Kind.ASSIGN, currentPosition, 1, chars, new SourceLocation(currentLine, currentColumn));
							}
						}
					case HAVE_MINUS:
					default: throw new IllegalStateException("Lexer bug");
				}
		}


		return new Token(Kind.EOF, 0, 0, new char[0], new SourceLocation(currentLine, currentColumn));

	}

}
