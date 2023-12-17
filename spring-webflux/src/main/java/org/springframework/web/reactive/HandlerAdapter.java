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

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * 将{@link DispatcherHandler}与调用处理程序的细节解耦，使支持任何处理程序类型成为可能的契约。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface HandlerAdapter {

	/**
	 * 此{@code HandlerAdapter}是否支持给定的{@code handler}。
	 *
	 * @param handler 要检查的处理程序对象
	 * @return 处理程序是否受支持
	 */
	boolean supports(Object handler);

	/**
	 * 使用给定处理程序处理请求。
	 * <p>鼓励实现以顺序处理由处理程序调用引起的异常，并在必要时返回表示错误响应的备用结果。
	 * <p>此外，由于异步{@code HandlerResult}可能在结果处理期间稍后产生错误，
	 * 因此还鼓励实现在{@code HandlerResult}上{@link HandlerResult#setExceptionHandler(Function)设置异常处理程序}，
	 * 以便在结果处理后稍后也可以应用它。
	 *
	 * @param exchange 当前服务器交换信息
	 * @param handler  已经通过{@link #supports(Object)}事先检查过的选择的处理程序
	 * @return {@link Mono}，发出单个{@code HandlerResult}或如果已完全处理请求并且不需要进一步处理则发出空值。
	 */
	Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler);

}
