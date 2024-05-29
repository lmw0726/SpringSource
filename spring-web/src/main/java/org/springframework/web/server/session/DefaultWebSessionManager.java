/*
 * Copyright 2002-2020 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * {@link WebSessionManager} 的默认实现，委托给 {@link WebSessionIdResolver} 进行会话 ID 解析，
 * 并委托给 {@link WebSessionStore} 进行存储。
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 5.0
 */
public class DefaultWebSessionManager implements WebSessionManager {
	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(DefaultWebSessionManager.class);

	/**
	 * 会话编号解析器
	 */
	private WebSessionIdResolver sessionIdResolver = new CookieWebSessionIdResolver();

	/**
	 * 会话保存起器
	 */
	private WebSessionStore sessionStore = new InMemoryWebSessionStore();


	/**
	 * 配置 ID 解析策略。
	 * <p>默认为 {@link CookieWebSessionIdResolver} 的实例。
	 *
	 * @param sessionIdResolver 要使用的解析器
	 */
	public void setSessionIdResolver(WebSessionIdResolver sessionIdResolver) {
		Assert.notNull(sessionIdResolver, "WebSessionIdResolver is required");
		this.sessionIdResolver = sessionIdResolver;
	}

	/**
	 * 返回配置的 {@link WebSessionIdResolver}。
	 */
	public WebSessionIdResolver getSessionIdResolver() {
		return this.sessionIdResolver;
	}

	/**
	 * 配置持久化策略。
	 * <p>默认为 {@link InMemoryWebSessionStore} 的实例。
	 *
	 * @param sessionStore 要使用的持久化策略
	 */
	public void setSessionStore(WebSessionStore sessionStore) {
		Assert.notNull(sessionStore, "WebSessionStore is required");
		this.sessionStore = sessionStore;
	}

	/**
	 * 返回配置的 {@link WebSessionStore}。
	 */
	public WebSessionStore getSessionStore() {
		return this.sessionStore;
	}


	@Override
	public Mono<WebSession> getSession(ServerWebExchange exchange) {
		// 使用 Mono.defer() 创建一个延迟执行的 Mono
		return Mono.defer(() ->
				// 检索会话，如果会话不存在则创建一个新的 Web 会话
				retrieveSession(exchange)
						.switchIfEmpty(createWebSession())
						// 在下一个信号触发时执行操作，将保存会话的逻辑绑定到响应的 beforeCommit 钩子上
						.doOnNext(session -> exchange.getResponse().beforeCommit(() -> save(exchange, session))));
	}

	private Mono<WebSession> createWebSession() {
		// 创建一个新的 Web 会话
		Mono<WebSession> session = this.sessionStore.createWebSession();
		if (logger.isDebugEnabled()) {
			// 如果日志级别为调试模式，将日志记录“Created new WebSession.”
			session = session.doOnNext(s -> logger.debug("Created new WebSession."));
		}
		return session;
	}

	private Mono<WebSession> retrieveSession(ServerWebExchange exchange) {
		// 从会话 ID 解析器获取会话 ID 列表，然后创建 Flux 流
		Flux<WebSession> sessionFlux = Flux.fromIterable(getSessionIdResolver().resolveSessionIds(exchange))
				// 使用会话存储检索每个会话
				.concatMap(this.sessionStore::retrieveSession);
		// 只取第一个会话
		return sessionFlux.next();
	}

	private Mono<Void> save(ServerWebExchange exchange, WebSession session) {
		// 从会话 ID 解析器获取会话 ID 列表
		List<String> ids = getSessionIdResolver().resolveSessionIds(exchange);

		// 检查会话状态是否已启动或已过期
		if (!session.isStarted() || session.isExpired()) {
			// 如果会话未启动或已过期，则检查是否有会话 ID
			if (!ids.isEmpty()) {
				// 如果会话已过期或在处理请求时已失效，则记录日志并使会话过期
				if (logger.isDebugEnabled()) {
					logger.debug("WebSession expired or has been invalidated");
				}
				this.sessionIdResolver.expireSession(exchange);
			}
			// 返回空 Mono
			return Mono.empty();
		}

		// 检查是否有会话 ID 或会话 ID 是否与第一个 ID 匹配
		if (ids.isEmpty() || !session.getId().equals(ids.get(0))) {
			// 如果没有会话 ID 或会话 ID 不匹配第一个 ID，则设置会话 ID
			this.sessionIdResolver.setSessionId(exchange, session.getId());
		}

		// 保存会话并返回结果
		return session.save();
	}

}
