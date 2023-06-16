/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.expression;

/**
 * Parses expression strings into compiled expressions that can be evaluated.
 * Supports parsing templates as well as standard expression strings.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @since 3.0
 */
public interface ExpressionParser {

	/**
	 * 解析表达式字符串并返回可用于重复求值的表达式对象。
	 * <p>一些例子:
	 * <pre class="code">
	 *     3 + 4
	 *     name.firstName
	 * </pre>
	 *
	 * @param expressionString 要解析的原始表达式字符串
	 * @return 解析表达式的评估器
	 * @throws ParseException 解析过程中出现异常
	 */
	Expression parseExpression(String expressionString) throws ParseException;

	/**
	 * 解析表达式字符串并返回可用于重复求值的表达式对象。
	 * <p>一些例子:
	 * <pre class="code">
	 *     3 + 4
	 *     name.firstName
	 * </pre>
	 *
	 * @param expressionString 要解析的原始表达式字符串
	 * @param context          影响此表达式解析例程的上下文 (可选)
	 * @return 解析表达式的评估器
	 * @throws ParseException 解析过程中出现异常
	 */
	Expression parseExpression(String expressionString, ParserContext context) throws ParseException;

}
