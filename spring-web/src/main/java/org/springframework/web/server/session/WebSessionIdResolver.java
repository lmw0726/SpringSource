/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.web.server.ServerWebExchange;

import java.util.List;

/**
 * 会话 ID 解析策略的契约。允许通过请求解析会话 ID，并通过响应发送会话 ID 或使会话过期。
 *
 * @author Rossen Stoyanchev
 * @see CookieWebSessionIdResolver
 * @since 5.0
 */
public interface WebSessionIdResolver {

	/**
	 * 解析与请求关联的会话 ID。
	 *
	 * @param exchange 当前的交换对象
	 * @return 会话 ID 列表，如果没有则返回空列表
	 */
	List<String> resolveSessionIds(ServerWebExchange exchange);

	/**
	 * 将给定的会话 ID 发送给客户端。
	 *
	 * @param exchange  当前的交换对象
	 * @param sessionId 会话 ID
	 */
	void setSessionId(ServerWebExchange exchange, String sessionId);

	/**
	 * 指示客户端结束当前会话。
	 *
	 * @param exchange 当前的交换对象
	 */
	void expireSession(ServerWebExchange exchange);
}
