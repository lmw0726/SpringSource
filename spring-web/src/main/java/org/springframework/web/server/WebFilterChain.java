/*
 * Copyright 2002-2015 the original author or authors.
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
 * 允许 {@link WebFilter} 委托给链中的下一个过滤器的契约。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebFilterChain {

	/**
	 * 委托给链中的下一个 {@code WebFilter}。
	 *
	 * @param exchange 当前的服务器交换
	 * @return {@code Mono<Void>} 以指示请求处理何时完成
	 */
	Mono<Void> filter(ServerWebExchange exchange);

}
