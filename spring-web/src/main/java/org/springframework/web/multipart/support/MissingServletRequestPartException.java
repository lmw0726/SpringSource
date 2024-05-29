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

package org.springframework.web.multipart.support;

import org.springframework.web.multipart.MultipartResolver;

import javax.servlet.ServletException;

/**
 * 当无法找到以其名称标识的“multipart/form-data”请求的部分时引发。
 *
 * <p>这可能是因为请求不是 multipart/form-data 请求，因为请求中不存在该部分，
 * 或者因为 Web 应用程序未正确配置以处理多部分请求，例如没有 {@link MultipartResolver}。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
@SuppressWarnings("serial")
public class MissingServletRequestPartException extends ServletException {
	/**
	 * 请求部分名称
	 */
	private final String requestPartName;


	/**
	 * MissingServletRequestPartException 的构造方法。
	 *
	 * @param requestPartName multipart 请求中缺少的部分的名称
	 */
	public MissingServletRequestPartException(String requestPartName) {
		super("Required request part '" + requestPartName + "' is not present");
		this.requestPartName = requestPartName;
	}


	/**
	 * 返回多部分请求中有问题的部分的名称。
	 */
	public String getRequestPartName() {
		return this.requestPartName;
	}

}
