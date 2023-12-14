/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.expression.spel;

/**
 * Captures the possible configuration settings for a compiler that can be
 * used when evaluating expressions.
 *
 * @author Andy Clement
 * @since 4.1
 */
public enum SpelCompilerMode {

	/**
	 * 关闭编译器; 这是默认的。
	 */
	OFF,

	/**
	 * 在立即模式下，表达式会尽快编译 (通常在1个解释运行之后)。
	 * 如果编译的表达式失败，它将向调用者抛出异常。
	 */
	IMMEDIATE,

	/**
	 * 在混合模式下，表达式求值随时间在解释和编译之间静默切换。经过多次运行后，表达式将被编译。
	 * 如果以后失败 (可能是由于推断的类型信息更改)，则将在内部捕获，并且系统切换回解释模式。
	 * 随后可能会再次编译它。
	 */
	MIXED

}
