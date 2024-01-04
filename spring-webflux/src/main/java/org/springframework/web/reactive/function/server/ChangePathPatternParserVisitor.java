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

package org.springframework.web.reactive.function.server;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

/**
 * {@link RouterFunctions.Visitor}的实现，在路径相关的请求谓词（例如{@link RequestPredicates.PathPatternPredicate}）上更改{@link PathPatternParser}。
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
	 * 构造函数，设置要使用的解析器。
	 *
	 * @param parser 要使用的解析器，不能为空
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
	public void resources(Function<ServerRequest, Mono<Resource>> lookupFunction) {
	}

	@Override
	public void attributes(Map<String, Object> attributes) {
	}

	@Override
	public void unknown(RouterFunction<?> routerFunction) {
	}

	/**
	 * 修改路由断言使用的解析器。
	 *
	 * @param predicate 要修改解析器的谓词
	 */
	private void changeParser(RequestPredicate predicate) {
		// 检查 predicate 是否是 Target 类的实例
		if (predicate instanceof Target) {
			Target target = (Target) predicate;

			// 更改解析器
			target.changeParser(this.parser);
		}

	}


	/**
	 * 由能够更改解析器的断言实现的接口。
	 */
	public interface Target {

		/**
		 * 更改解析器。
		 *
		 * @param parser 要使用的解析器
		 */
		void changeParser(PathPatternParser parser);
	}
}
