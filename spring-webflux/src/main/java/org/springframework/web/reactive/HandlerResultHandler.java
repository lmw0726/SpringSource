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

package org.springframework.web.reactive;

import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;

/**
 * 处理 {@link HandlerResult}，通常由 {@link HandlerAdapter} 返回。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface HandlerResultHandler {

	/**
	 * 检查该处理器是否支持给定的 {@link HandlerResult}。
	 *
	 * @param result 要检查的结果对象
	 * @return 是否可以使用给定结果的布尔值
	 */
	boolean supports(HandlerResult result);

	/**
	 * 处理给定的结果，修改响应头和/或向响应中写入数据。
	 *
	 * @param exchange 当前的服务器交换对象
	 * @param result 处理结果对象
	 * @return {@code Mono<Void>}，表示请求处理完成的情况
	 */
	Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result);

}

