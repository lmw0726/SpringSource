/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel.standard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.expression.spel.InternalParseException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.SpelParseException;

/**
 * Lex some input data into a stream of tokens that can then be parsed.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 */
class Tokenizer {

	// 如果这被改变了，它必须保持排序...
	private static final String[] ALTERNATIVE_OPERATOR_NAMES =
			{"DIV", "EQ", "GE", "GT", "LE", "LT", "MOD", "NE", "NOT"};

	private static final byte[] FLAGS = new byte[256];

	private static final byte IS_DIGIT = 0x01;

	private static final byte IS_HEXDIGIT = 0x02;

	private static final byte IS_ALPHA = 0x04;

	static {
		for (int ch = '0'; ch <= '9'; ch++) {
			FLAGS[ch] |= IS_DIGIT | IS_HEXDIGIT;
		}
		for (int ch = 'A'; ch <= 'F'; ch++) {
			FLAGS[ch] |= IS_HEXDIGIT;
		}
		for (int ch = 'a'; ch <= 'f'; ch++) {
			FLAGS[ch] |= IS_HEXDIGIT;
		}
		for (int ch = 'A'; ch <= 'Z'; ch++) {
			FLAGS[ch] |= IS_ALPHA;
		}
		for (int ch = 'a'; ch <= 'z'; ch++) {
			FLAGS[ch] |= IS_ALPHA;
		}
	}


	private String expressionString;

	private char[] charsToProcess;

	private int pos;

	private int max;

	private List<Token> tokens = new ArrayList<>();


	public Tokenizer(String inputData) {
		this.expressionString = inputData;
		this.charsToProcess = (inputData + "\0").toCharArray();
		this.max = this.charsToProcess.length;
		this.pos = 0;
	}


	public List<Token> process() {
		while (this.pos < this.max) {
			char ch = this.charsToProcess[this.pos];
			if (isAlphabetic(ch)) {
				//如果是字母，添加到tokens中
				lexIdentifier();
			} else {
				switch (ch) {
					case '+':
						if (isTwoCharToken(TokenKind.INC)) {
							//如果是++符号，添加到tokens中
							pushPairToken(TokenKind.INC);
						} else {
							//否则按照+符号，添加到tokens中
							pushCharToken(TokenKind.PLUS);
						}
						break;
					case '_':
						// 启动标识符的另一种方法
						lexIdentifier();
						break;
					case '-':
						if (isTwoCharToken(TokenKind.DEC)) {
							//如果是--符号，添加到tokens中
							pushPairToken(TokenKind.DEC);
						} else {
							//否则作为-号，添加到tokens中
							pushCharToken(TokenKind.MINUS);
						}
						break;
					case ':':
						//添加一个冒号令牌到tokens中
						pushCharToken(TokenKind.COLON);
						break;
					case '.':
						//添加一个点号令牌到tokens中
						pushCharToken(TokenKind.DOT);
						break;
					case ',':
						//添加一个逗号令牌到tokens中
						pushCharToken(TokenKind.COMMA);
						break;
					case '*':
						//添加一个星号令牌到tokens中
						pushCharToken(TokenKind.STAR);
						break;
					case '/':
						//添加一个除号令牌到tokens中
						pushCharToken(TokenKind.DIV);
						break;
					case '%':
						//添加一个模号令牌到tokens中
						pushCharToken(TokenKind.MOD);
						break;
					case '(':
						//添加左括号令牌
						pushCharToken(TokenKind.LPAREN);
						break;
					case ')':
						//添加右括号令牌
						pushCharToken(TokenKind.RPAREN);
						break;
					case '[':
						//添加左中括号令牌
						pushCharToken(TokenKind.LSQUARE);
						break;
					case '#':
						//添加hash令牌
						pushCharToken(TokenKind.HASH);
						break;
					case ']':
						//添加右中括号令牌
						pushCharToken(TokenKind.RSQUARE);
						break;
					case '{':
						//添加左大括号令牌
						pushCharToken(TokenKind.LCURLY);
						break;
					case '}':
						//添加右大括号令牌
						pushCharToken(TokenKind.RCURLY);
						break;
					case '@':
						//添加一个bean引用令牌
						pushCharToken(TokenKind.BEAN_REF);
						break;
					case '^':
						if (isTwoCharToken(TokenKind.SELECT_FIRST)) {
							//如果是^[符号，添加到tokens中
							pushPairToken(TokenKind.SELECT_FIRST);
						} else {
							//作为乘法符号添加到tokens中
							pushCharToken(TokenKind.POWER);
						}
						break;
					case '!':
						if (isTwoCharToken(TokenKind.NE)) {
							//如果是!=符号，添加到tokens中
							pushPairToken(TokenKind.NE);
						} else if (isTwoCharToken(TokenKind.PROJECT)) {
							//如果是![符号，添加到tokens中
							pushPairToken(TokenKind.PROJECT);
						} else {
							//否则按照!符号，添加到tokens中
							pushCharToken(TokenKind.NOT);
						}
						break;
					case '=':
						if (isTwoCharToken(TokenKind.EQ)) {
							//如果是==符号，添加到tokens中
							pushPairToken(TokenKind.EQ);
						} else {
							//赋值符号，添加到tokens中
							pushCharToken(TokenKind.ASSIGN);
						}
						break;
					case '&':
						if (isTwoCharToken(TokenKind.SYMBOLIC_AND)) {
							//如果是&&符号，添加到tokens中
							pushPairToken(TokenKind.SYMBOLIC_AND);
						} else {
							//否则按照工厂bean引用，添加到tokens中
							pushCharToken(TokenKind.FACTORY_BEAN_REF);
						}
						break;
					case '|':
						if (!isTwoCharToken(TokenKind.SYMBOLIC_OR)) {
							//如果不是||符号，引发解析异常
							raiseParseException(this.pos, SpelMessage.MISSING_CHARACTER, "|");
						}
						//作为||符号(逻辑或)，添加到tokens中
						pushPairToken(TokenKind.SYMBOLIC_OR);
						break;
					case '?':
						if (isTwoCharToken(TokenKind.SELECT)) {
							//添加?[令牌，标识查询
							pushPairToken(TokenKind.SELECT);
						} else if (isTwoCharToken(TokenKind.ELVIS)) {
							//添加?:令牌
							pushPairToken(TokenKind.ELVIS);
						} else if (isTwoCharToken(TokenKind.SAFE_NAVI)) {
							//?.令牌
							pushPairToken(TokenKind.SAFE_NAVI);
						} else {
							//作为？符号添加到tokens中
							pushCharToken(TokenKind.QMARK);
						}
						break;
					case '$':
						if (isTwoCharToken(TokenKind.SELECT_LAST)) {
							//如果是$[符号，添加到tokens中
							pushPairToken(TokenKind.SELECT_LAST);
						} else {
							//作为标识符号添加到tokens中
							lexIdentifier();
						}
						break;
					case '>':
						if (isTwoCharToken(TokenKind.GE)) {
							//如果是>=符号，添加到tokens中
							pushPairToken(TokenKind.GE);
						} else {
							//否则按照>符号，添加到tokens中
							pushCharToken(TokenKind.GT);
						}
						break;
					case '<':
						if (isTwoCharToken(TokenKind.LE)) {
							//<=符号
							pushPairToken(TokenKind.LE);
						} else {
							//<符号
							pushCharToken(TokenKind.LT);
						}
						break;
					case '0':
					case '1':
					case '2':
					case '3':
					case '4':
					case '5':
					case '6':
					case '7':
					case '8':
					case '9':
						//添加数字令牌
						lexNumericLiteral(ch == '0');
						break;
					case ' ':
					case '\t':
					case '\r':
					case '\n':
						// 在白色空间上漂移
						this.pos++;
						break;
					case '\'':
						//作为字符串字面量，添加到tokens中
						lexQuotedStringLiteral();
						break;
					case '"':
						//双引号
						lexDoubleQuotedStringLiteral();
						break;
					case 0:
						// 在值的末端点击哨兵会把我们带到终点
						this.pos++;
						break;
					case '\\':
						//引发异常
						raiseParseException(this.pos, SpelMessage.UNEXPECTED_ESCAPE_CHAR);
						break;
					default:
						throw new IllegalStateException("Cannot handle (" + (int) ch + ") '" + ch + "'");
				}
			}
		}
		return this.tokens;
	}


	// 字符串字面量: '\''! (APOS|~'\'')* '\''!;
	private void lexQuotedStringLiteral() {
		int start = this.pos;
		boolean terminated = false;
		while (!terminated) {
			this.pos++;
			char ch = this.charsToProcess[this.pos];
			if (ch == '\'') {
				//如果当前字符是\字符
				// 如果后面的字符也是一个'
				if (this.charsToProcess[this.pos + 1] == '\'') {
					// 跳过它，继续
					this.pos++;
				} else {
					//否则，结束
					terminated = true;
				}
			}
			if (isExhausted()) {
				//达到最后一个字符，抛出异常
				raiseParseException(start, SpelMessage.NON_TERMINATING_QUOTED_STRING);
			}
		}
		this.pos++;
		//作为字面量令牌，添加到tokens中
		this.tokens.add(new Token(TokenKind.LITERAL_STRING, subarray(start, this.pos), start, this.pos));
	}

	// DQ_STRING_LITERAL: '"'! (~'"')* '"'!;
	private void lexDoubleQuotedStringLiteral() {
		int start = this.pos;
		boolean terminated = false;
		while (!terminated) {
			this.pos++;
			char ch = this.charsToProcess[this.pos];
			if (ch == '"') {
				// 如果后面的字符也是一个“
				if (this.charsToProcess[this.pos + 1] == '"') {
					// 跳过，继续
					this.pos++;
				} else {
					//结束
					terminated = true;
				}
			}
			if (isExhausted()) {
				//达到最后一个字符，抛出异常
				raiseParseException(start, SpelMessage.NON_TERMINATING_DOUBLE_QUOTED_STRING);
			}
		}
		this.pos++;
		//作为字面量令牌，添加到tokens中
		this.tokens.add(new Token(TokenKind.LITERAL_STRING, subarray(start, this.pos), start, this.pos));
	}

	// 真正的文字 :
	// ('.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?) |
	// ((DECIMAL_DIGIT)+ '.' (DECIMAL_DIGIT)+ (EXPONENT_PART)? (REAL_TYPE_SUFFIX)?) |
	// ((DECIMAL_DIGIT)+ (EXPONENT_PART) (REAL_TYPE_SUFFIX)?) |
	// ((DECIMAL_DIGIT)+ (REAL_TYPE_SUFFIX));
	// fragment INTEGER_TYPE_SUFFIX : ( 'L' | 'l' );
	// fragment HEX_DIGIT :
	// '0'|'1'|'2'|'3'|'4'|'5'|'6'|'7'|'8'|'9'|'A'|'B'|'C'|'D'|'E'|'F'|'a'|'b'|'c'|'d'|'e'|'f';
	//
	// fragment EXPONENT_PART : 'e' (SIGN)* (DECIMAL_DIGIT)+ | 'E' (SIGN)*
	// (DECIMAL_DIGIT)+ ;
	// fragment SIGN : '+' | '-' ;
	// fragment REAL_TYPE_SUFFIX : 'F' | 'f' | 'D' | 'd';
	// INTEGER_LITERAL
	// : (DECIMAL_DIGIT)+ (INTEGER_TYPE_SUFFIX)?;

	private void lexNumericLiteral(boolean firstCharIsZero) {
		boolean isReal = false;
		int start = this.pos;
		char ch = this.charsToProcess[this.pos + 1];
		boolean isHex = ch == 'x' || ch == 'X';

		// 处理十六进制
		if (firstCharIsZero && isHex) {
			//第一个则字符串为0且第二个是x或者X
			this.pos = this.pos + 1;
			do {
				this.pos++;
			} while (isHexadecimalDigit(this.charsToProcess[this.pos]));

			if (isChar('L', 'l')) {
				//如果当前位置的字符是L或者l，添加十六进制的令牌到tokens中
				pushHexIntToken(subarray(start + 2, this.pos), true, start, this.pos);
				this.pos++;
			} else {
				//不是L和l,将令牌的isLong设置为false
				pushHexIntToken(subarray(start + 2, this.pos), false, start, this.pos);
			}
			return;
		}

		// 实数必须有前导数字

		// 消耗数字的第一部分
		do {
			this.pos++;
		} while (isDigit(this.charsToProcess[this.pos]));

		// 一个 '.' 表示此数字是实数
		ch = this.charsToProcess[this.pos];
		if (ch == '.') {
			//如果数字后的第一个字符为 .
			//表明此时是实数
			isReal = true;
			int dotpos = this.pos;
			// 继续消费数字
			do {
				this.pos++;
			} while (isDigit(this.charsToProcess[this.pos]));

			if (this.pos == dotpos + 1) {
				//这个数字大约是 “3.”。它确实是一个int，但可能是 '3.toString()'之类的东西的一部分。
				// 在这种情况下，将其处理为int，并将点保留为单独的令牌。
				this.pos = dotpos;
				pushIntToken(subarray(start, this.pos), false, start, this.pos);
				return;
			}
		}

		int endOfNumber = this.pos;

		// 现在可能有指数，也可能没有指数

		// 是不是long类型 ?
		if (isChar('L', 'l')) {
			if (isReal) {
				// 3.4L - 不允许，抛出异常
				raiseParseException(start, SpelMessage.REAL_CANNOT_BE_LONG);
			}
			pushIntToken(subarray(start, endOfNumber), true, start, endOfNumber);
			this.pos++;
		} else if (isExponentChar(this.charsToProcess[this.pos])) {
			//pos位置的字符是指数，即e或者E字符
			isReal = true;
			this.pos++;
			char possibleSign = this.charsToProcess[this.pos];
			if (isSign(possibleSign)) {
				//如果当前位置的字符是+或者-
				this.pos++;
			}

			// 指数数字
			do {
				this.pos++;
			} while (isDigit(this.charsToProcess[this.pos]));

			boolean isFloat = false;
			if (isFloatSuffix(this.charsToProcess[this.pos])) {
				//如果当前位置的字符是f或者F，则表明是浮点数
				isFloat = true;
				endOfNumber = ++this.pos;
			} else if (isDoubleSuffix(this.charsToProcess[this.pos])) {
				//如果当前位置的字符是d或者D，表明是浮点数
				endOfNumber = ++this.pos;
			}
			//解析成实数类型或者FLOAT实数的令牌，并存入tokens中
			pushRealToken(subarray(start, this.pos), isFloat, start, this.pos);
		} else {
			ch = this.charsToProcess[this.pos];
			boolean isFloat = false;
			if (isFloatSuffix(ch)) {
				//如果当前位置的字符是f或者F，则表明是浮点数
				isReal = true;
				isFloat = true;
				endOfNumber = ++this.pos;
			} else if (isDoubleSuffix(ch)) {
				//如果当前位置的字符是d或者D，表明是浮点数
				isReal = true;
				endOfNumber = ++this.pos;
			}
			if (isReal) {
				pushRealToken(subarray(start, endOfNumber), isFloat, start, endOfNumber);
			} else {
				pushIntToken(subarray(start, endOfNumber), false, start, endOfNumber);
			}
		}
	}

	private void lexIdentifier() {
		int start = this.pos;
		do {
			//如果是标识符，位置向后累加
			this.pos++;
		} while (isIdentifier(this.charsToProcess[this.pos]));
		//从开始位置到当前位置，复制出字符数组
		char[] subarray = subarray(start, this.pos);

		// 检查这是否是运算符的替代 (文本) 表示形式 (请参阅 alternativeOperatorNames)
		if ((this.pos - start) == 2 || (this.pos - start) == 3) {
			//如果当前位置距离开始位置2个字符或者3个字符，将上面的字符数组转为大写的字符串。
			String asString = new String(subarray).toUpperCase();
			// 二分查找逻辑符的位置
			int idx = Arrays.binarySearch(ALTERNATIVE_OPERATOR_NAMES, asString);
			if (idx >= 0) {
				//如果逻辑符号存在，将逻辑符号转为Token，并添加到tokens中
				pushOneCharOrTwoCharToken(TokenKind.valueOf(asString), start, subarray);
				return;
			}
		}
		//将其标识为一个标识符，并添加到tokens中
		this.tokens.add(new Token(TokenKind.IDENTIFIER, subarray, start, this.pos));
	}

	private void pushIntToken(char[] data, boolean isLong, int start, int end) {
		if (isLong) {
			this.tokens.add(new Token(TokenKind.LITERAL_LONG, data, start, end));
		} else {
			this.tokens.add(new Token(TokenKind.LITERAL_INT, data, start, end));
		}
	}

	private void pushHexIntToken(char[] data, boolean isLong, int start, int end) {
		if (data.length == 0) {
			//如果字符数组不存在，抛出异常
			if (isLong) {
				raiseParseException(start, SpelMessage.NOT_A_LONG, this.expressionString.substring(start, end + 1));
			} else {
				raiseParseException(start, SpelMessage.NOT_AN_INTEGER, this.expressionString.substring(start, end));
			}
		}
		//添加成不同参数类型的Token到tokens中
		if (isLong) {
			this.tokens.add(new Token(TokenKind.LITERAL_HEXLONG, data, start, end));
		} else {
			this.tokens.add(new Token(TokenKind.LITERAL_HEXINT, data, start, end));
		}
	}

	private void pushRealToken(char[] data, boolean isFloat, int start, int end) {
		if (isFloat) {
			this.tokens.add(new Token(TokenKind.LITERAL_REAL_FLOAT, data, start, end));
		} else {
			this.tokens.add(new Token(TokenKind.LITERAL_REAL, data, start, end));
		}
	}

	private char[] subarray(int start, int end) {
		return Arrays.copyOfRange(this.charsToProcess, start, end);
	}

	/**
	 * 检查这是否可能是两个字符的令牌。
	 */
	private boolean isTwoCharToken(TokenKind kind) {
		return (kind.tokenChars.length == 2 &&
				this.charsToProcess[this.pos] == kind.tokenChars[0] &&
				this.charsToProcess[this.pos + 1] == kind.tokenChars[1]);
	}

	/**
	 * 推一个只有一个字符长度的标记。
	 */
	private void pushCharToken(TokenKind kind) {
		this.tokens.add(new Token(kind, this.pos, this.pos + 1));
		this.pos++;
	}

	/**
	 * 按两个字符长度的标记。
	 */
	private void pushPairToken(TokenKind kind) {
		this.tokens.add(new Token(kind, this.pos, this.pos + 2));
		this.pos += 2;
	}

	private void pushOneCharOrTwoCharToken(TokenKind kind, int pos, char[] data) {
		this.tokens.add(new Token(kind, data, pos, pos + kind.getLength()));
	}

	// ID: ('a'..'z'|'A'..'Z'|'_'|'$') ('a'..'z'|'A'..'Z'|'_'|'$'|'0'..'9'|DOT_ESCAPED)*;
	private boolean isIdentifier(char ch) {
		return isAlphabetic(ch) || isDigit(ch) || ch == '_' || ch == '$';
	}

	private boolean isChar(char a, char b) {
		char ch = this.charsToProcess[this.pos];
		return ch == a || ch == b;
	}

	private boolean isExponentChar(char ch) {
		return ch == 'e' || ch == 'E';
	}

	private boolean isFloatSuffix(char ch) {
		return ch == 'f' || ch == 'F';
	}

	private boolean isDoubleSuffix(char ch) {
		return ch == 'd' || ch == 'D';
	}

	private boolean isSign(char ch) {
		return ch == '+' || ch == '-';
	}

	private boolean isDigit(char ch) {
		if (ch > 255) {
			return false;
		}
		return (FLAGS[ch] & IS_DIGIT) != 0;
	}

	private boolean isAlphabetic(char ch) {
		if (ch > 255) {
			return false;
		}
		return (FLAGS[ch] & IS_ALPHA) != 0;
	}

	private boolean isHexadecimalDigit(char ch) {
		if (ch > 255) {
			return false;
		}
		return (FLAGS[ch] & IS_HEXDIGIT) != 0;
	}

	private boolean isExhausted() {
		return (this.pos == this.max - 1);
	}

	private void raiseParseException(int start, SpelMessage msg, Object... inserts) {
		throw new InternalParseException(new SpelParseException(this.expressionString, start, msg, inserts));
	}

}
