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

/**
 * 处理 Web 服务器交换处理期间的异常的契约。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebExceptionHandler {

	/**
	 * 处理给定的异常。通过返回值的完成信号表示错误处理已完成，而错误信号表示异常仍未处理。
	 *
	 * @param exchange 当前的交换
	 * @param ex       要处理的异常
	 * @return {@code Mono<Void>} 以指示异常处理何时完成
	 */
	Mono<Void> handle(ServerWebExchange exchange, Throwable ex);

}
