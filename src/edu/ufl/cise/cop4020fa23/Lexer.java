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

import static edu.ufl.cise.cop4020fa23.Kind.EOF;
import static edu.ufl.cise.cop4020fa23.Kind.PLUS;

import edu.ufl.cise.cop4020fa23.exceptions.LexicalException;


public class Lexer implements ILexer {

	String input;
	private int currentPosition = 0;
	private int currentLine = 1;
	private int currentColumn = 1;
	private char[] chars;
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
						int firstPositionOfToken = currentPosition;
						switch(currentChar){
							case ' ', '\t', '\r'->{currentPosition++;}
							case '+'->{
								new Token(Kind.PLUS,firstPositionOfToken,1, chars ,new SourceLocation(currentLine, currentColumn));
								currentPosition++;}
							case '='->{state = State.HAVE_EQ; currentPosition++;}
							case 0 ->{return new Token(Kind.EOF, 0, 0, chars, new SourceLocation(currentLine, currentColumn));}
					}
					break;
					case IN_IDENT:
					case HAVE_ZERO:
					case HAVE_DOT:
					case IN_FLOAT:
					case IN_NUM:
					case HAVE_EQ:
					case HAVE_MINUS:
					default: throw new IllegalStateException("Lexer bug");
				}
		}

		return new Token(Kind.EOF, 0, 0, new char[0], new SourceLocation(currentLine, currentColumn));

	}




}
