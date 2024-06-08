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

import org.springframework.core.NamedInheritableThreadLocal;
import org.springframework.core.NamedThreadLocal;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import javax.faces.context.FacesContext;

/**
 * 持有类，以线程绑定的 {@link RequestAttributes} 对象的形式公开 web 请求。
 * 如果将 {@code inheritable} 标志设置为 {@code true}，则此请求将被当前线程生成的任何子线程继承。
 *
 * <p>使用 {@link RequestContextListener} 或 {@link org.springframework.web.filter.RequestContextFilter}
 * 来公开当前 web 请求。请注意，{@link org.springframework.web.servlet.DispatcherServlet} 默认已经公开了当前请求。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @see RequestContextListener
 * @see org.springframework.web.filter.RequestContextFilter
 * @see org.springframework.web.servlet.DispatcherServlet
 * @since 2.0
 */
public abstract class RequestContextHolder {
	/**
	 * 是否存在Faces上下文
	 */
	private static final boolean jsfPresent =
			ClassUtils.isPresent("javax.faces.context.FacesContext", RequestContextHolder.class.getClassLoader());
	/**
	 * 请求属性持有者
	 */
	private static final ThreadLocal<RequestAttributes> requestAttributesHolder =
			new NamedThreadLocal<>("Request attributes");

	/**
	 * 可继承请求属性持有者
	 */
	private static final ThreadLocal<RequestAttributes> inheritableRequestAttributesHolder =
			new NamedInheritableThreadLocal<>("Request context");


	/**
	 * 重置当前线程的 RequestAttributes。
	 */
	public static void resetRequestAttributes() {
		requestAttributesHolder.remove();
		inheritableRequestAttributesHolder.remove();
	}

	/**
	 * 将给定的 RequestAttributes 绑定到当前线程，<i>不</i>将其公开为可继承的。
	 *
	 * @param attributes 要公开的 RequestAttributes
	 * @see #setRequestAttributes(RequestAttributes, boolean)
	 */
	public static void setRequestAttributes(@Nullable RequestAttributes attributes) {
		setRequestAttributes(attributes, false);
	}

	/**
	 * 将给定的 RequestAttributes 绑定到当前线程。
	 *
	 * @param attributes  要公开的 RequestAttributes，如果为 {@code null}，则重置线程绑定的上下文
	 * @param inheritable 是否将 RequestAttributes 作为可继承的公开给子线程（使用 {@link InheritableThreadLocal}）
	 */
	public static void setRequestAttributes(@Nullable RequestAttributes attributes, boolean inheritable) {
		// 如果 Servlet 请求属性对象为空
		if (attributes == null) {
			// 重置请求属性
			resetRequestAttributes();
		} else {
			// 如果属性继承为 true
			if (inheritable) {
				// 将 Servlet 请求属性对象设置到可继承请求属性持有者中
				inheritableRequestAttributesHolder.set(attributes);
				// 移除请求属性持有者
				requestAttributesHolder.remove();
			} else {
				// 将 Servlet 请求属性对象设置到请求属性持有者中
				requestAttributesHolder.set(attributes);
				// 移除可继承请求属性持有者
				inheritableRequestAttributesHolder.remove();
			}
		}
	}

	/**
	 * 返回当前线程绑定的 RequestAttributes。
	 *
	 * @return 当前线程绑定的 RequestAttributes，如果没有绑定，则返回 {@code null}
	 */
	@Nullable
	public static RequestAttributes getRequestAttributes() {
		// 从请求属性持有者获取请求属性
		RequestAttributes attributes = requestAttributesHolder.get();
		if (attributes == null) {
			// 如果请求属性为空，则尝试从可继承请求属性持有者获取
			attributes = inheritableRequestAttributesHolder.get();
		}
		// 返回请求属性
		return attributes;
	}

	/**
	 * 返回当前线程绑定的 RequestAttributes。
	 * <p>如果之前绑定了 RequestAttributes 实例，则暴露它。如果有的话，将回退到当前的 JSF FacesContext。
	 *
	 * @return 当前线程绑定的 RequestAttributes
	 * @throws IllegalStateException 如果当前线程没有绑定 RequestAttributes 对象
	 * @see #setRequestAttributes
	 * @see ServletRequestAttributes
	 * @see FacesRequestAttributes
	 * @see javax.faces.context.FacesContext#getCurrentInstance()
	 */
	public static RequestAttributes currentRequestAttributes() throws IllegalStateException {
		// 获取请求属性
		RequestAttributes attributes = getRequestAttributes();
		// 如果请求属性为空
		if (attributes == null) {
			// 如果 JSF 存在
			if (jsfPresent) {
				// 获取 JSF 请求属性
				attributes = FacesRequestAttributesFactory.getFacesRequestAttributes();
			}
			// 如果请求属性仍为空
			if (attributes == null) {
				throw new IllegalStateException("No thread-bound request found: " +
						"Are you referring to request attributes outside of an actual web request, " +
						"or processing a request outside of the originally receiving thread? " +
						"If you are actually operating within a web request and still receive this message, " +
						"your code is probably running outside of DispatcherServlet: " +
						"In this case, use RequestContextListener or RequestContextFilter to expose the current request.");
			}
		}
		// 返回请求属性
		return attributes;
	}


	/**
	 * 内部类，避免对 JSF 的硬编码依赖。
	 */
	private static class FacesRequestAttributesFactory {

		@Nullable
		public static RequestAttributes getFacesRequestAttributes() {
			// 获取当前的 Faces上下文
			FacesContext facesContext = FacesContext.getCurrentInstance();
			// 如果 Faces上下文 不为空，则创建并返回 Faces请求属性 对象；
			// 否则返回 null
			return (facesContext != null ? new FacesRequestAttributes(facesContext) : null);
		}
	}

}
