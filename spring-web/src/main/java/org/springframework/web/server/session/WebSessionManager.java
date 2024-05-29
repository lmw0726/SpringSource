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

package org.springframework.web.server.session;

import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

/**
 * 用于访问 HTTP 请求的 {@link WebSession} 的主要类。
 *
 * @author Rossen Stoyanchev
 * @see WebSessionIdResolver
 * @see WebSessionStore
 * @since 5.0
 */
public interface WebSessionManager {

	/**
	 * 获取给定交换的 {@link WebSession}。始终保证返回一个实例，要么匹配客户端请求的会话 ID，
	 * 要么返回一个新会话，因为客户端未指定会话 ID，或者底层会话已过期。
	 *
	 * @param exchange 当前交换
	 * @return WebSession 的 Promise
	 */
	Mono<WebSession> getSession(ServerWebExchange exchange);

}
