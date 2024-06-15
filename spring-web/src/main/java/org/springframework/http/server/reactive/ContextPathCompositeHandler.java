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

package org.springframework.http.server.reactive;

import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@code HttpHandler}，根据简单的基于前缀的映射将请求委派给多个 {@code HttpHandler} 中的一个。
 *
 * <p>这是一种粗粒度的机制，用于将请求委派给多个应用程序之一，每个应用程序由一个 {@code HttpHandler} 表示，
 * 应用程序的 "上下文路径"（基于前缀的映射）通过 {@link ServerHttpRequest#getPath()} 暴露出来。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ContextPathCompositeHandler implements HttpHandler {
	/**
	 * 路径——Http处理器映射
	 */
	private final Map<String, HttpHandler> handlerMap;


	public ContextPathCompositeHandler(Map<String, ? extends HttpHandler> handlerMap) {
		Assert.notEmpty(handlerMap, "Handler map must not be empty");
		this.handlerMap = initHandlers(handlerMap);
	}

	private static Map<String, HttpHandler> initHandlers(Map<String, ? extends HttpHandler> map) {
		map.keySet().forEach(ContextPathCompositeHandler::assertValidContextPath);
		return new LinkedHashMap<>(map);
	}

	private static void assertValidContextPath(String contextPath) {
		// 断言上下文路径不能为空
		Assert.hasText(contextPath, "Context path must not be empty");
		if (contextPath.equals("/")) {
			// 如果上下文路径为 ‘/’，直接返回
			return;
		}
		// 断言上下文路径以 ‘/’ 开头
		Assert.isTrue(contextPath.startsWith("/"), "Context path must begin with '/'");
		// 断言上下文路径以 ‘/’ 结尾
		Assert.isTrue(!contextPath.endsWith("/"), "Context path must not end with '/'");
	}


	@Override
	public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
		// 从请求中获取应用程序内部的路径值
		String path = request.getPath().pathWithinApplication().value();

		// 在处理映射中查找第一个与路径匹配的条目，并处理请求
		return this.handlerMap.entrySet().stream()
				// 过滤路径以映射条目的键开头的条目
				.filter(entry -> path.startsWith(entry.getKey()))
				// 找到第一个匹配的映射条目
				.findFirst()
				.map(entry -> {
					// 构建新的请求对象，设置上下文路径为当前请求路径的上下文路径加上匹配映射条目的键
					String contextPath = request.getPath().contextPath().value() + entry.getKey();
					ServerHttpRequest newRequest = request.mutate().contextPath(contextPath).build();
					// 调用匹配映射条目的处理器处理新的请求，并返回处理结果
					return entry.getValue().handle(newRequest, response);
				})
				.orElseGet(() -> {
					// 如果没有找到匹配的映射条目，设置响应状态为 404 Not Found
					response.setStatusCode(HttpStatus.NOT_FOUND);
					// 完成响应
					return response.setComplete();
				});
	}

}
