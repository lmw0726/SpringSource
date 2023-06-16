/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.expression.common;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.ParserContext;
import org.springframework.lang.Nullable;

/**
 * An expression parser that understands templates. It can be subclassed by expression
 * parsers that do not offer first class support for templating.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Andy Clement
 * @since 3.0
 */
public abstract class TemplateAwareExpressionParser implements ExpressionParser {

	@Override
	public Expression parseExpression(String expressionString) throws ParseException {
		return parseExpression(expressionString, null);
	}

	@Override
	public Expression parseExpression(String expressionString, @Nullable ParserContext context) throws ParseException {
		if (context != null && context.isTemplate()) {
			//如果解析上下文存在，且解析上下文是一个模板，解析模板
			return parseTemplate(expressionString, context);
		} else {
			//否则解析表达式
			return doParseExpression(expressionString, context);
		}
	}


	private Expression parseTemplate(String expressionString, ParserContext context) throws ParseException {
		if (expressionString.isEmpty()) {
			//如果表达式字符串为空，抛出异常
			return new LiteralExpression("");
		}
		//解析表达式
		Expression[] expressions = parseExpressions(expressionString, context);
		if (expressions.length == 1) {
			//如果只有一个表达式，直接返回
			return expressions[0];
		} else {
			//否则抛出异常
			return new CompositeStringExpression(expressionString, expressions);
		}
	}

	/**
	 * 使用配置的解析器解析给定表达式字符串的助手。表达式字符串可以包含所有包含在 “${...}” 标记中的任意数量的表达式。
	 * <p>
	 * 例如："foo${expr0}bar${expr1}". 静态文本片段也将作为仅返回该静态文本片段的表达式返回。
	 * 他的结果：对所有返回的表达式进行求值并将结果串联会产生完整的求值字符串。
	 * <p>
	 * 展开仅对找到的最外面的定界符进行，因此字符串 “hello ${foo${abc}}” 将分解为 “hello” 和 “foo${abc}”。
	 * 这意味着支持使用 ${..} 作为其功能一部分的表达式语言，没有任何问题。
	 * 解析能知道嵌套的表达式结构。
	 * 它假定括号 '('，方括号 '[' 和大括号 '}' 必须在表达式中成对，除非它们在字符串文字中，并且字符串文字以单引号开头和终止。
	 *
	 * @param expressionString 表达式字符串
	 * @return 解析后的表达式
	 * @throws ParseException 当无法解析表达式时
	 */
	private Expression[] parseExpressions(String expressionString, ParserContext context) throws ParseException {
		List<Expression> expressions = new ArrayList<>();
		//获取表达式前缀
		String prefix = context.getExpressionPrefix();
		//获取表达式后缀
		String suffix = context.getExpressionSuffix();
		int startIdx = 0;

		while (startIdx < expressionString.length()) {
			//获取表达式前缀的索引位置
			int prefixIndex = expressionString.indexOf(prefix, startIdx);
			if (prefixIndex >= startIdx) {
				//如果该前缀索引大于等于开始位置
				// 发现了一个内在的表达 -- 这是一个复合的
				if (prefixIndex > startIdx) {
					//如果前缀索引大于开始位置，构建一个字面表达式添加到表达式列表中
					expressions.add(new LiteralExpression(expressionString.substring(startIdx, prefixIndex)));
				}
				//前缀后的字符串开始位置
				int afterPrefixIndex = prefixIndex + prefix.length();
				//跳过更正结尾后缀，获取后缀位置
				int suffixIndex = skipToCorrectEndSuffix(suffix, expressionString, afterPrefixIndex);
				if (suffixIndex == -1) {
					//如果后缀位置不存在，抛出异常
					throw new ParseException(expressionString, prefixIndex,
							"No ending suffix '" + suffix + "' for expression starting at character " +
									prefixIndex + ": " + expressionString.substring(prefixIndex));
				}
				if (suffixIndex == afterPrefixIndex) {
					//如果后缀位置刚好在前缀位置的后一位，抛出异常
					throw new ParseException(expressionString, prefixIndex,
							"No expression defined within delimiter '" + prefix + suffix +
									"' at character " + prefixIndex);
				}
				//获取前缀和后缀之间的字符串作为表达式
				String expr = expressionString.substring(prefixIndex + prefix.length(), suffixIndex);
				//去除空格
				expr = expr.trim();
				if (expr.isEmpty()) {
					//如果表达式为空，抛出异常
					throw new ParseException(expressionString, prefixIndex,
							"No expression defined within delimiter '" + prefix + suffix +
									"' at character " + prefixIndex);
				}
				//解析表达式，并添加到表达式列表中
				expressions.add(doParseExpression(expr, context));
				//跳过表达式后缀，继续解析下一个表达式
				startIdx = suffixIndex + suffix.length();
			} else {
				//在字符串中找不到更多 ${表达式}，将剩下的添加为静态文本
				expressions.add(new LiteralExpression(expressionString.substring(startIdx)));
				startIdx = expressionString.length();
			}
		}

		return expressions.toArray(new Expression[0]);
	}

	/**
	 * 如果可以在提供的表达式字符串中的提供位置找到指定的后缀，则返回true。
	 *
	 * @param expressionString 可能包含后缀的表达式字符串
	 * @param pos              检查后缀的开始位置
	 * @param suffix           后缀字符串
	 */
	private boolean isSuffixHere(String expressionString, int pos, String suffix) {
		int suffixPosition = 0;
		for (int i = 0; i < suffix.length() && pos < expressionString.length(); i++) {
			if (expressionString.charAt(pos++) != suffix.charAt(suffixPosition++)) {
				//如果在表达式字符串中pos位置的字符和后缀在suffixPosition位置的字符不同，则返回false
				return false;
			}
		}
		if (suffixPosition != suffix.length()) {
			//在完全找到后缀之前，表达式字符串用完了
			return false;
		}
		return true;
	}

	/**
	 * 应对嵌套，例如 “${...${...}}”，其中第一个 ${ 的正确结尾是最后的 }。
	 *
	 * @param suffix           后缀
	 * @param expressionString 表达式字符串
	 * @param afterPrefixIndex 最近找到的前缀位置，正在寻找匹配的结尾后缀
	 * @return 正确匹配的下一个后缀的位置或-1 (如果找不到)
	 */
	private int skipToCorrectEndSuffix(String suffix, String expressionString, int afterPrefixIndex)
			throws ParseException {

		//考虑表达式文本-依靠规则: 括号必须成对: () [] {}
		// 字符串文字是 “...” 或 “...”，它们可能包含不匹配的括号
		int pos = afterPrefixIndex;
		int maxlen = expressionString.length();
		int nextSuffix = expressionString.indexOf(suffix, afterPrefixIndex);
		if (nextSuffix == -1) {
			return -1; // the suffix is missing
		}
		//构建一个括号栈
		Deque<Bracket> stack = new ArrayDeque<>();
		while (pos < maxlen) {
			if (isSuffixHere(expressionString, pos, suffix) && stack.isEmpty()) {
				//如果后缀在表达式中pos位置，且括号栈为空，则直接退出循环
				break;
			}
			//获取pos位置的字符
			char ch = expressionString.charAt(pos);
			switch (ch) {
				case '{':
				case '[':
				case '(':
					//如果是三类开始括号符号，记录位置和字符，推入栈中
					stack.push(new Bracket(ch, pos));
					break;
				case '}':
				case ']':
				case ')':
					//如果是三类结束括号字符
					if (stack.isEmpty()) {
						//如果栈为空，抛出异常
						throw new ParseException(expressionString, pos, "Found closing '" + ch +
								"' at position " + pos + " without an opening '" +
								Bracket.theOpenBracketFor(ch) + "'");
					}
					//将栈顶的括号弹出
					Bracket p = stack.pop();
					if (!p.compatibleWithCloseBracket(ch)) {
						throw new ParseException(expressionString, pos, "Found closing '" + ch +
								"' at position " + pos + " but most recent opening is '" + p.bracket +
								"' at position " + p.pos);
					}
					break;
				case '\'':
				case '"':
					// 跳到字面的结尾
					int endLiteral = expressionString.indexOf(ch, pos + 1);
					if (endLiteral == -1) {
						throw new ParseException(expressionString, pos,
								"Found non terminating string literal starting at position " + pos);
					}
					pos = endLiteral;
					break;
			}
			pos++;
		}
		if (!stack.isEmpty()) {
			//如果栈不为空，抛出异常
			Bracket p = stack.pop();
			throw new ParseException(expressionString, p.pos, "Missing closing '" +
					Bracket.theCloseBracketFor(p.bracket) + "' for '" + p.bracket + "' at position " + p.pos);
		}
		if (!isSuffixHere(expressionString, pos, suffix)) {
			//如果后缀不在表达式中pos位置，返回-1
			return -1;
		}
		//否则就返回该位置
		return pos;
	}


	/**
	 * 实际解析表达式字符串并返回一个表达式对象。
	 *
	 * @param expressionString 要解析的原始表达式字符串
	 * @param context          影响此表达式解析例程的上下文 (可选)
	 * @return 解析表达式的评估器
	 * @throws ParseException 解析过程中出现异常
	 */
	protected abstract Expression doParseExpression(String expressionString, @Nullable ParserContext context)
			throws ParseException;


	/**
	 * This captures a type of bracket and the position in which it occurs in the
	 * expression. The positional information is used if an error has to be reported
	 * because the related end bracket cannot be found. Bracket is used to describe:
	 * square brackets [] round brackets () and curly brackets {}
	 */
	private static class Bracket {

		char bracket;

		int pos;

		Bracket(char bracket, int pos) {
			this.bracket = bracket;
			this.pos = pos;
		}

		boolean compatibleWithCloseBracket(char closeBracket) {
			if (this.bracket == '{') {
				return closeBracket == '}';
			} else if (this.bracket == '[') {
				return closeBracket == ']';
			}
			return closeBracket == ')';
		}

		static char theOpenBracketFor(char closeBracket) {
			if (closeBracket == '}') {
				return '{';
			} else if (closeBracket == ']') {
				return '[';
			}
			return '(';
		}

		static char theCloseBracketFor(char openBracket) {
			if (openBracket == '{') {
				return '}';
			} else if (openBracket == '[') {
				return ']';
			}
			return ')';
		}
	}

}
