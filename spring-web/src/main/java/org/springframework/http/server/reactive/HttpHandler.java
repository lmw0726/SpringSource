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

import reactor.core.publisher.Mono;

/**
 * 用作不同运行时环境中响应式HTTP请求处理的最低级契约。
 *
 * <p>应用程序中更高级别但仍通用的构建块，例如 {@code WebFilter}、{@code WebSession}、
 * {@code ServerWebExchange} 等，位于 {@code org.springframework.web.server} 包中。
 *
 * <p>应用程序级别的编程模型，例如注解控制器和函数式处理程序，位于 {@code spring-webflux} 模块中。
 *
 * <p>通常，一个 {@link HttpHandler} 表示一个完整的应用程序，通过 {@link org.springframework.web.server.adapter.WebHttpHandlerBuilder}
 * 桥接更高级别的编程模型。多个应用程序可以通过 {@link ContextPathCompositeHandler} 在唯一的上下文路径中进行插入。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @see ContextPathCompositeHandler
 * @since 5.0
 */
public interface HttpHandler {

	/**
	 * 处理给定的请求并写入响应。
	 *
	 * @param request  当前请求
	 * @param response 当前响应
	 * @return 表示请求处理完成的 Mono
	 */
	Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response);

}
