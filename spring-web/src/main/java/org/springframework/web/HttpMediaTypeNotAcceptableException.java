/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.http.MediaType;

import java.util.List;

/**
 * 在请求处理程序无法生成客户端可接受的响应时抛出的异常。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
@SuppressWarnings("serial")
public class HttpMediaTypeNotAcceptableException extends HttpMediaTypeException {

	/**
	 * 创建一个新的 HttpMediaTypeNotAcceptableException。
	 *
	 * @param message 异常消息
	 */
	public HttpMediaTypeNotAcceptableException(String message) {
		super(message);
	}

	/**
	 * 创建一个新的 HttpMediaTypeNotSupportedException。
	 *
	 * @param supportedMediaTypes 支持的媒体类型列表
	 */
	public HttpMediaTypeNotAcceptableException(List<MediaType> supportedMediaTypes) {
		super("Could not find acceptable representation", supportedMediaTypes);
	}

}
