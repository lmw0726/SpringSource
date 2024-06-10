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

package org.springframework.web.bind.support;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.WebRequest;

/**
 * {@link SessionAttributeStore} 接口的默认实现，
 * 将属性存储在 WebRequest 会话中（即 HttpSession）。
 *
 * @author Juergen Hoeller
 * @see #setAttributeNamePrefix
 * @see org.springframework.web.context.request.WebRequest#setAttribute
 * @see org.springframework.web.context.request.WebRequest#getAttribute
 * @see org.springframework.web.context.request.WebRequest#removeAttribute
 * @since 2.5
 */
public class DefaultSessionAttributeStore implements SessionAttributeStore {
	/**
	 * 属性名称前缀
	 */
	private String attributeNamePrefix = "";


	/**
	 * 指定用于后端会话中的属性名称的前缀。
	 * <p>默认是不用前缀，将会话属性以模型中的同名存储。
	 */
	public void setAttributeNamePrefix(@Nullable String attributeNamePrefix) {
		this.attributeNamePrefix = (attributeNamePrefix != null ? attributeNamePrefix : "");
	}


	@Override
	public void storeAttribute(WebRequest request, String attributeName, Object attributeValue) {
		Assert.notNull(request, "WebRequest must not be null");
		Assert.notNull(attributeName, "Attribute name must not be null");
		Assert.notNull(attributeValue, "Attribute value must not be null");
		// 获取存储在会话中的属性名称
		String storeAttributeName = getAttributeNameInSession(request, attributeName);

		// 将属性值设置到请求中，并指定其作用域为会话范围
		request.setAttribute(storeAttributeName, attributeValue, WebRequest.SCOPE_SESSION);
	}

	@Override
	@Nullable
	public Object retrieveAttribute(WebRequest request, String attributeName) {
		Assert.notNull(request, "WebRequest must not be null");
		Assert.notNull(attributeName, "Attribute name must not be null");
		// 获取存储在会话中的属性名称
		String storeAttributeName = getAttributeNameInSession(request, attributeName);

		// 从请求中获取会话范围内的属性值并返回
		return request.getAttribute(storeAttributeName, WebRequest.SCOPE_SESSION);
	}

	@Override
	public void cleanupAttribute(WebRequest request, String attributeName) {
		Assert.notNull(request, "WebRequest must not be null");
		Assert.notNull(attributeName, "Attribute name must not be null");
		// 获取存储在会话中的属性名称
		String storeAttributeName = getAttributeNameInSession(request, attributeName);

		// 从请求中移除会话范围内的属性
		request.removeAttribute(storeAttributeName, WebRequest.SCOPE_SESSION);
	}


	/**
	 * 计算后端会话中的属性名称。
	 * <p>默认实现简单地在配置的 {@link #setAttributeNamePrefix "attributeNamePrefix"} 前加上前缀（如果有的话）。
	 *
	 * @param request       当前请求
	 * @param attributeName 属性名称
	 * @return 后端会话中的属性名称
	 */
	protected String getAttributeNameInSession(WebRequest request, String attributeName) {
		return this.attributeNamePrefix + attributeName;
	}

}
