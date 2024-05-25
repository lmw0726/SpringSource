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

package org.springframework.web;

import org.springframework.lang.Nullable;

import javax.servlet.ServletException;

/**
 * 当 HTTP 请求处理程序需要预先存在的会话时抛出的异常。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public class HttpSessionRequiredException extends ServletException {
	/**
	 * 预期会话属性的名称
	 */
	@Nullable
	private final String expectedAttribute;


	/**
	 * 创建一个新的 HttpSessionRequiredException。
	 * @param msg 详细消息
	 */
	public HttpSessionRequiredException(String msg) {
		super(msg);
		this.expectedAttribute = null;
	}

	/**
	 * 创建一个新的 HttpSessionRequiredException。
	 * @param msg                详细消息
	 * @param expectedAttribute 预期会话属性的名称
	 * @since 4.3
	 */
	public HttpSessionRequiredException(String msg, String expectedAttribute) {
		super(msg);
		this.expectedAttribute = expectedAttribute;
	}


	/**
	 * 返回预期会话属性的名称（如果有）。
	 * @since 4.3
	 */
	@Nullable
	public String getExpectedAttribute() {
		return this.expectedAttribute;
	}

}
