/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.web.server.WebSession;

/**
 * 用于 {@link WebSession} 持久化的策略。
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 5.0
 */
public interface WebSessionStore {

	/**
	 * 创建一个新的 WebSession。
	 * <p>请注意，这只是创建一个新实例。
	 * WebSession 可以通过 {@link WebSession#start()} 显式启动，或通过添加属性（然后通过
	 * {@link WebSession#save()}）隐式启动。
	 *
	 * @return 创建的会话实例
	 */
	Mono<WebSession> createWebSession();

	/**
	 * 返回给定 ID 的 WebSession。
	 * <p><strong>注意：</strong>此方法应执行过期检查，如果已过期，则删除会话并返回空。
	 * 此方法还应更新检索到的会话的 lastAccessTime。
	 *
	 * @param sessionId 要加载的会话
	 * @return 会话，或空的 {@code Mono}。
	 */
	Mono<WebSession> retrieveSession(String sessionId);

	/**
	 * 删除指定 ID 的 WebSession。
	 *
	 * @param sessionId 要删除的会话的 ID
	 * @return 完成通知（成功或错误）
	 */
	Mono<Void> removeSession(String sessionId);

	/**
	 * 将最后访问时间更新为“现在”。
	 *
	 * @param webSession 要更新的会话
	 * @return 更新了最后访问时间的会话
	 */
	Mono<WebSession> updateLastAccessTime(WebSession webSession);

}
