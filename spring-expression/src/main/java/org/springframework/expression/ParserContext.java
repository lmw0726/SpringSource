/*
 * Copyright 2002-2017 the original author or authors.
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
 * Input provided to an expression parser that can influence an expression
 * parsing/compilation routine.
 *
 * @author Keith Donald
 * @author Andy Clement
 * @since 3.0
 */
public interface ParserContext {

	/**
	 * 被解析的表达式是否是模板。模板表达式由可以与可评估块混合的文字文本组成。一些例子:
	 * <pre class="code">
	 * 	   Some literal text
	 *     Hello #{name.firstName}!
	 *     #{3 + 4}
	 * </pre>
	 *
	 * @return 如果表达式是模板，则为true，否则为false
	 */
	boolean isTemplate();

	/**
	 * 对于模板表达式，返回标识字符串中表达式块开始的前缀。例如: "${"
	 *
	 * @return 标识表达式开始的前缀
	 */
	String getExpressionPrefix();

	/**
	 * 对于模板表达式，返回标识字符串中表达式块结尾的前缀。例如: "}"
	 *
	 * @return 标识表达式结尾的后缀
	 */
	String getExpressionSuffix();


	/**
	 * 启用模板表达式解析模式的默认ParserContext实现。表达式前缀为 “#{”，表达式后缀为 “}”。
	 *
	 * @see #isTemplate()
	 */
	ParserContext TEMPLATE_EXPRESSION = new ParserContext() {

		@Override
		public boolean isTemplate() {
			return true;
		}

		@Override
		public String getExpressionPrefix() {
			return "#{";
		}

		@Override
		public String getExpressionSuffix() {
			return "}";
		}
	};

}
