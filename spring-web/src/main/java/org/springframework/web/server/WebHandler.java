/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.server;

import reactor.core.publisher.Mono;

import org.springframework.web.server.adapter.HttpWebHandlerAdapter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * 用于处理Web请求的合同。
 *
 * <p>使用 {@link HttpWebHandlerAdapter} 来适应 {@code WebHandler} 到
 * {@link org.springframework.http.server.reactive.HttpHandler HttpHandler}。
 * {@link WebHttpHandlerBuilder} 提供了一种方便的方式来实现适应，
 * 同时也可以选择配置一个或多个过滤器和/或异常处理器。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebHandler {

	/**
	 * 处理Web服务器交换。
	 *
	 * @param exchange 当前服务器交换
	 * @return {@code Mono<Void>} 以指示请求处理何时完成
	 */
	Mono<Void> handle(ServerWebExchange exchange);

}
