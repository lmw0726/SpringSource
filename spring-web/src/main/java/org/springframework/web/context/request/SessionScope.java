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

package org.springframework.web.context.request;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.lang.Nullable;

/**
 * 基于会话的 {@link org.springframework.beans.factory.config.Scope} 实现。
 *
 * <p>依赖于一个线程绑定的 {@link RequestAttributes} 实例，可以通过 {@link RequestContextListener}、
 * {@link org.springframework.web.filter.RequestContextFilter} 或
 * {@link org.springframework.web.servlet.DispatcherServlet} 导出。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see RequestContextHolder#currentRequestAttributes()
 * @see RequestAttributes#SCOPE_SESSION
 * @see RequestContextListener
 * @see org.springframework.web.filter.RequestContextFilter
 * @see org.springframework.web.servlet.DispatcherServlet
 * @since 2.0
 */
public class SessionScope extends AbstractRequestAttributesScope {

	@Override
	protected int getScope() {
		return RequestAttributes.SCOPE_SESSION;
	}

	@Override
	public String getConversationId() {
		return RequestContextHolder.currentRequestAttributes().getSessionId();
	}

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		// 获取当前请求的会话互斥对象
		Object mutex = RequestContextHolder.currentRequestAttributes().getSessionMutex();
		// 使用会话互斥对象进行同步
		synchronized (mutex) {
			// 调用父类的 get 方法获取属性值，并使用提供的对象工厂创建属性值
			return super.get(name, objectFactory);
		}
	}

	@Override
	@Nullable
	public Object remove(String name) {
		// 获取当前请求的会话互斥对象
		Object mutex = RequestContextHolder.currentRequestAttributes().getSessionMutex();
		// 使用互斥对象进行同步
		synchronized (mutex) {
			// 调用父类的remove方法
			return super.remove(name);
		}
	}

}
