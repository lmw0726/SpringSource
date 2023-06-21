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

package org.springframework.expression.spel;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.lang.Nullable;

/**
 * Base superclass for compiled expressions. Each generated compiled expression class
 * will extend this class and implement the {@link #getValue} method. It is not intended
 * to be subclassed by user code.
 *
 * @author Andy Clement
 * @since 4.1
 */
public abstract class CompiledExpression {

	/**
	 * 由SpelCompiler生成的CompiledExpression的子类将提供此方法的实现。
	 */
	public abstract Object getValue(@Nullable Object target, @Nullable EvaluationContext context)
			throws EvaluationException;

}
