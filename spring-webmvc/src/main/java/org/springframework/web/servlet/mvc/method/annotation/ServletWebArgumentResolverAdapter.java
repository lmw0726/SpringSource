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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.util.Assert;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.AbstractWebArgumentResolverAdapter;

/**
 * 从 {@link ServletRequestAttributes} 创建 {@link NativeWebRequest} 的 Servlet 特定适配器。
 *
 * <p><strong>注意：</strong>此类提供了向后兼容性。
 * 但是建议将 {@code WebArgumentResolver} 重写为 {@code HandlerMethodArgumentResolver}。
 * 有关详细信息，请参阅 {@link org.springframework.web.method.annotation.AbstractWebArgumentResolverAdapter} 的 javadoc。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletWebArgumentResolverAdapter extends AbstractWebArgumentResolverAdapter {

	public ServletWebArgumentResolverAdapter(WebArgumentResolver adaptee) {
		super(adaptee);
	}

	@Override
	protected NativeWebRequest getWebRequest() {
		// 获取当前请求的 RequestAttributes
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		// 断言获取的 RequestAttributes 是 ServletRequestAttributes 类型，如果不是则抛出异常
		Assert.state(requestAttributes instanceof ServletRequestAttributes, "No ServletRequestAttributes");
		// 将获取的 RequestAttributes 转换为 ServletRequestAttributes 类型
		ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
		// 使用 ServletRequestAttributes 中的 HttpServletRequest 创建 ServletWebRequest
		return new ServletWebRequest(servletRequestAttributes.getRequest());
	}
}
