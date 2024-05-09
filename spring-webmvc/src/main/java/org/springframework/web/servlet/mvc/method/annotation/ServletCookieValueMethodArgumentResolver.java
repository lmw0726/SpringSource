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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.AbstractCookieValueMethodArgumentResolver;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * 从 {@link HttpServletRequest} 中解析 cookie 值的 {@link org.springframework.web.method.annotation.AbstractCookieValueMethodArgumentResolver}。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletCookieValueMethodArgumentResolver extends AbstractCookieValueMethodArgumentResolver {

	/**
	 * URL路径助手
	 */
	private UrlPathHelper urlPathHelper = UrlPathHelper.defaultInstance;


	public ServletCookieValueMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
		super(beanFactory);
	}


	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}


	@Override
	@Nullable
	protected Object resolveName(String cookieName, MethodParameter parameter,
								 NativeWebRequest webRequest) throws Exception {

		// 获取原生的HttpServletRequest对象
		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		Assert.state(servletRequest != null, "No HttpServletRequest");

		// 从请求中获取指定名称的Cookie对象
		Cookie cookieValue = WebUtils.getCookie(servletRequest, cookieName);
		// 如果参数的嵌套类型是Cookie，则直接返回Cookie对象
		if (Cookie.class.isAssignableFrom(parameter.getNestedParameterType())) {
			return cookieValue;
		} else if (cookieValue != null) {
			// 如果Cookie值不为空，则解码并返回其值
			return this.urlPathHelper.decodeRequestString(servletRequest, cookieValue.getValue());
		} else {
			// 否则，返回null
			return null;
		}
	}

}
