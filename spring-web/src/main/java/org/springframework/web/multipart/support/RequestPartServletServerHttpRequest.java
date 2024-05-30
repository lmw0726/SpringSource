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

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * {@link ServerHttpRequest} 实现，用于访问多部分请求的一个部分。
 * 如果使用 {@link MultipartResolver} 配置，则通过 {@link MultipartFile} 访问该部分。
 * 或者如果使用 Servlet 3.0 多部分处理，则通过 {@code ServletRequest.getPart} 访问该部分。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class RequestPartServletServerHttpRequest extends ServletServerHttpRequest {
	/**
	 * 多部分请求
	 */
	private final MultipartHttpServletRequest multipartRequest;

	/**
	 * 请求部分名称
	 */
	private final String requestPartName;

	/**
	 * 多部分请求头
	 */
	private final HttpHeaders multipartHeaders;


	/**
	 * 创建一个新的 {@code RequestPartServletServerHttpRequest} 实例。
	 *
	 * @param request         当前的 servlet 请求
	 * @param requestPartName 要适应 {@link ServerHttpRequest} 合同的部分的名称
	 * @throws MissingServletRequestPartException 如果找不到请求部分
	 * @throws MultipartException                 如果无法初始化 MultipartHttpServletRequest
	 */
	public RequestPartServletServerHttpRequest(HttpServletRequest request, String requestPartName)
			throws MissingServletRequestPartException {

		// 调用父类构造函数，传入HttpServletRequest对象
		super(request);

		// 将HttpServletRequest对象转换为MultipartHttpServletRequest对象
		this.multipartRequest = MultipartResolutionDelegate.asMultipartHttpServletRequest(request);
		// 设置请求部分的名称
		this.requestPartName = requestPartName;

		// 获取请求部分的头部信息
		HttpHeaders multipartHeaders = this.multipartRequest.getMultipartHeaders(requestPartName);
		// 如果头部信息为空，则抛出MissingServletRequestPartException异常
		if (multipartHeaders == null) {
			throw new MissingServletRequestPartException(requestPartName);
		}
		// 设置请求部分的头部信息
		this.multipartHeaders = multipartHeaders;
	}


	@Override
	public HttpHeaders getHeaders() {
		return this.multipartHeaders;
	}

	@Override
	public InputStream getBody() throws IOException {
		// 更喜欢 Servlet 部分解析，以覆盖文件和参数流
		boolean servletParts = (this.multipartRequest instanceof StandardMultipartHttpServletRequest);
		if (servletParts) {
			// 如果是Servlet部分，检索Servlet部分
			Part part = retrieveServletPart();
			if (part != null) {
				// 如果检索到了部分，则返回该部分的输入流
				return part.getInputStream();
			}
		}

		// Spring 风格的区分 MultipartFile 和 String 参数
		MultipartFile file = this.multipartRequest.getFile(this.requestPartName);
		if (file != null) {
			// 如果是多部分文件，则返回多部分文件的输入流
			return file.getInputStream();
		}
		// 获取参数值
		String paramValue = this.multipartRequest.getParameter(this.requestPartName);
		if (paramValue != null) {
			// 如果参数值存在，则返回该参数值对应的字节数组输入流
			return new ByteArrayInputStream(paramValue.getBytes(determineCharset()));
		}

		// 如果未指示，则回退到 Servlet 部分解析
		if (!servletParts) {
			// 如果不是Servlet部分，检索Servlet部分
			Part part = retrieveServletPart();
			if (part != null) {
				// 如果检索到了部分，则返回该部分的输入流
				return part.getInputStream();
			}
		}
		// 否则抛出异常
		throw new IllegalStateException("No body available for request part '" + this.requestPartName + "'");
	}

	@Nullable
	private Part retrieveServletPart() {
		try {
			return this.multipartRequest.getPart(this.requestPartName);
		} catch (Exception ex) {
			throw new MultipartException("Failed to retrieve request part '" + this.requestPartName + "'", ex);
		}
	}

	private Charset determineCharset() {
		// 获取请求头部的Content-Type
		MediaType contentType = getHeaders().getContentType();
		// 如果Content-Type不为空
		if (contentType != null) {
			// 获取Content-Type的字符集
			Charset charset = contentType.getCharset();
			// 如果字符集不为空，则返回该字符集
			if (charset != null) {
				return charset;
			}
		}
		// 获取请求的字符编码
		String encoding = this.multipartRequest.getCharacterEncoding();
		// 如果字符编码不为空，则返回对应的字符集；否则返回默认的表单字符集
		return (encoding != null ? Charset.forName(encoding) : FORM_CHARSET);
	}

}
