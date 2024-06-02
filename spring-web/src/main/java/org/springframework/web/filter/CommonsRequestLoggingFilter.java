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

package org.springframework.web.filter;

import javax.servlet.http.HttpServletRequest;

/**
 * 简单的请求日志过滤器，将请求URI（以及可选的查询字符串）写入Commons Log。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see #setIncludeQueryString
 * @see #setBeforeMessagePrefix
 * @see #setBeforeMessageSuffix
 * @see #setAfterMessagePrefix
 * @see #setAfterMessageSuffix
 * @see org.apache.commons.logging.Log#debug(Object)
 * @since 1.2.5
 */
public class CommonsRequestLoggingFilter extends AbstractRequestLoggingFilter {

	@Override
	protected boolean shouldLog(HttpServletRequest request) {
		return logger.isDebugEnabled();
	}

	/**
	 * 在处理请求之前写入日志消息。
	 */
	@Override
	protected void beforeRequest(HttpServletRequest request, String message) {
		logger.debug(message);
	}

	/**
	 * 在处理请求之后写入日志消息。
	 */
	@Override
	protected void afterRequest(HttpServletRequest request, String message) {
		logger.debug(message);
	}

}
