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

import org.springframework.lang.Nullable;

/**
 * 基于请求的 {@link org.springframework.beans.factory.config.Scope} 实现。
 *
 * <p>依赖于一个线程绑定的 {@link RequestAttributes} 实例，可以通过 {@link RequestContextListener}、
 * {@link org.springframework.web.filter.RequestContextFilter} 或
 * {@link org.springframework.web.servlet.DispatcherServlet} 导出。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see RequestContextHolder#currentRequestAttributes()
 * @see RequestAttributes#SCOPE_REQUEST
 * @see RequestContextListener
 * @see org.springframework.web.filter.RequestContextFilter
 * @see org.springframework.web.servlet.DispatcherServlet
 * @since 2.0
 */
public class RequestScope extends AbstractRequestAttributesScope {

	@Override
	protected int getScope() {
		return RequestAttributes.SCOPE_REQUEST;
	}

	/**
	 * 请求没有会话 ID 的概念，因此此方法返回 {@code null}。
	 */
	@Override
	@Nullable
	public String getConversationId() {
		return null;
	}

}
