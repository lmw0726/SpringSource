/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.filter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * 一个用于实现 {@link RelativeRedirectFilter} 的响应包装器，
 * 也与 {@link ForwardedHeaderFilter} 共享。
 *
 * @author Rossen Stoyanchev
 * @since 4.3.10
 */
final class RelativeRedirectResponseWrapper extends HttpServletResponseWrapper {
	/**
	 * 重定向状态码
	 */
	private final HttpStatus redirectStatus;

	/**
	 * 构造函数，包含所需的信息。
	 *
	 * @param response        要包装的原始 HttpServletResponse
	 * @param redirectStatus  重定向状态码
	 */
	private RelativeRedirectResponseWrapper(HttpServletResponse response, HttpStatus redirectStatus) {
		super(response);
		Assert.notNull(redirectStatus, "'redirectStatus' is required");
		this.redirectStatus = redirectStatus;
	}


	@Override
	public void sendRedirect(String location) {
		setStatus(this.redirectStatus.value());
		setHeader(HttpHeaders.LOCATION, location);
	}

	/**
	 * 如果有必要，包装响应。
	 *
	 * @param response        要包装的原始 HttpServletResponse
	 * @param redirectStatus  重定向状态码
	 * @return 包装后的 HttpServletResponse，如果已经包装过则返回原始响应
	 */
	public static HttpServletResponse wrapIfNecessary(HttpServletResponse response, HttpStatus redirectStatus) {
		// 获取原生的 RelativeRedirectResponseWrapper 类型的响应对象
		RelativeRedirectResponseWrapper wrapper =
				WebUtils.getNativeResponse(response, RelativeRedirectResponseWrapper.class);

		// 如果原生的响应对象不为空，则返回原始的响应对象；
		// 否则，创建一个新的 RelativeRedirectResponseWrapper 对象并返回
		return (wrapper != null ? response : new RelativeRedirectResponseWrapper(response, redirectStatus));
	}

}
