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

package org.springframework.web.util;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.io.Serializable;

/**
 * Servlet HttpSessionListener，当 HttpSession 被创建时自动公开会话互斥锁。
 * 应在 {@code web.xml} 中注册为监听器。
 *
 * <p>会话互斥锁在整个会话的生命周期内保证是相同的对象，可在由 {@code SESSION_MUTEX_ATTRIBUTE} 常量定义的键下使用。
 * 它作为一个安全的引用，用于在当前会话上进行锁定。
 *
 * <p>在许多情况下，HttpSession 引用本身也是一个安全的互斥锁，因为对于相同的活动逻辑会话，它始终是相同的对象引用。
 * 但是，这不能在不同的servlet容器之间保证；唯一100%安全的方法是会话互斥锁。
 *
 * @author Juergen Hoeller
 * @see WebUtils#SESSION_MUTEX_ATTRIBUTE
 * @see WebUtils#getSessionMutex(javax.servlet.http.HttpSession)
 * @see org.springframework.web.servlet.mvc.AbstractController#setSynchronizeOnSession
 * @since 1.2.7
 */
public class HttpSessionMutexListener implements HttpSessionListener {

	@Override
	public void sessionCreated(HttpSessionEvent event) {
		event.getSession().setAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE, new Mutex());
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		event.getSession().removeAttribute(WebUtils.SESSION_MUTEX_ATTRIBUTE);
	}


	/**
	 * 要注册的互斥锁。只需要是一个普通的对象，用于同步。
	 * 应该是可序列化的，以允许 HttpSession 的持久化。
	 */
	@SuppressWarnings("serial")
	private static class Mutex implements Serializable {
	}

}
