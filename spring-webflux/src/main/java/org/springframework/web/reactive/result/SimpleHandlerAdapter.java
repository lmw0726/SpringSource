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

package org.springframework.web.reactive.result;

import reactor.core.publisher.Mono;

import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;

/**
 * {@link HandlerAdapter}的实现，允许使用普通的 {@link WebHandler} 协议
 * 与通用的 {@link DispatcherHandler} 进行交互。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public class SimpleHandlerAdapter implements HandlerAdapter {

	/**
	 * 判断给定的处理器是否被支持。
	 *
	 * @param handler 要检查的处理器对象
	 * @return 如果支持该处理器，则为 true；否则为 false
	 */
	@Override
	public boolean supports(Object handler) {
		return WebHandler.class.isAssignableFrom(handler.getClass());
	}

	/**
	 * 处理请求并返回处理结果的 Mono。
	 *
	 * @param exchange 用于表示 HTTP 请求和响应的 ServerWebExchange 对象
	 * @param handler 处理请求的对象
	 * @return 包含处理结果的 Mono 对象
	 */
	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
		WebHandler webHandler = (WebHandler) handler;
		Mono<Void> mono = webHandler.handle(exchange);
		return mono.then(Mono.empty());
	}

}
