/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.multipart;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;

/**
 * 在 servlet 请求中提供了处理多部分内容的附加方法，允许访问上传的文件。
 * 实现还需要覆盖标准的 {@link javax.servlet.ServletRequest} 方法以进行参数访问，使多部分参数可用。
 *
 * <p>具体的实现是 {@link org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest}。
 * 作为中间步骤，可以继承 {@link org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest}。
 *
 * @author Juergen Hoeller
 * @author Trevor D. Cook
 * @see MultipartResolver
 * @see MultipartFile
 * @see javax.servlet.http.HttpServletRequest#getParameter
 * @see javax.servlet.http.HttpServletRequest#getParameterNames
 * @see javax.servlet.http.HttpServletRequest#getParameterMap
 * @see org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest
 * @see org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest
 * @since 29.09.2003
 */
public interface MultipartHttpServletRequest extends HttpServletRequest, MultipartRequest {

	/**
	 * 以便利的 HttpMethod 实例返回此请求的方法。
	 */
	@Nullable
	HttpMethod getRequestMethod();

	/**
	 * 以便利的 HttpHeaders 实例返回此请求的头信息。
	 */
	HttpHeaders getRequestHeaders();

	/**
	 * 返回多部分请求的指定部分的头信息。
	 * <p>如果底层实现支持访问部分头信息，则返回所有头信息。
	 * 否则，例如对于文件上传，如果可用，返回的头信息可能会公开“Content-Type”。
	 */
	@Nullable
	HttpHeaders getMultipartHeaders(String paramOrFileName);

}
