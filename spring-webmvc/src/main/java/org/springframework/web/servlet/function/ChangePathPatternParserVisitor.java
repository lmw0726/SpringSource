/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.servlet.function;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * {@link RouterFunctions.Visitor}的实现，用于更改与路径相关的请求谓词
 * (即{@code RequestPredicates.PathPatternPredicate})上的{@link PathPatternParser}。
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
class ChangePathPatternParserVisitor implements RouterFunctions.Visitor {
	/**
	 * 路径模式解析器
	 */
	private final PathPatternParser parser;

	/**
	 * 构造函数，初始化{@code PathPatternParser}。
	 *
	 * @param parser 必须不为空的解析器
	 */
	public ChangePathPatternParserVisitor(PathPatternParser parser) {
		Assert.notNull(parser, "Parser must not be null");
		this.parser = parser;
	}

	@Override
	public void startNested(RequestPredicate predicate) {
		changeParser(predicate);
	}

	@Override
	public void endNested(RequestPredicate predicate) {
	}

	@Override
	public void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction) {
		changeParser(predicate);
	}

	@Override
	public void resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
	}

	@Override
	public void attributes(Map<String, Object> attributes) {
	}

	@Override
	public void unknown(RouterFunction<?> routerFunction) {
	}

	/**
	 * 更改给定请求谓词的解析器。
	 *
	 * @param predicate 请求谓词
	 */
	private void changeParser(RequestPredicate predicate) {
		// 如果谓词对象是Target的实例
		if (predicate instanceof Target) {
			// 将谓词对象转换为Target类型
			Target target = (Target) predicate;
			// 调用Target对象的changeParser方法，传入当前的解析器
			target.changeParser(this.parser);
		}
	}


	/**
	 * 实现该接口的谓词可以更改解析器。
	 */
	public interface Target {
		/**
		 * 更改解析器的方法。
		 *
		 * @param parser 要设置的新解析器
		 */
		void changeParser(PathPatternParser parser);
	}
}
