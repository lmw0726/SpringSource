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

package org.springframework.web.testfixture.servlet;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.*;

/**
 * {@link org.springframework.web.multipart.MultipartHttpServletRequest} 接口的模拟实现。
 *
 * <p>从 Spring 5.0 开始，这组模拟是基于 Servlet 4.0 的基线设计的。
 *
 * <p>用于测试访问多部分上传的应用程序控制器。
 * {@link MockMultipartFile} 可用于使用文件填充这些模拟请求。
 *
 * @author Juergen Hoeller
 * @author Eric Crampton
 * @author Arjen Poutsma
 * @see MockMultipartFile
 * @since 2.0
 */
public class MockMultipartHttpServletRequest extends MockHttpServletRequest implements MultipartHttpServletRequest {
	/**
	 * 文件名称 —— 多部分文件映射
	 */
	private final MultiValueMap<String, MultipartFile> multipartFiles = new LinkedMultiValueMap<>();


	/**
	 * 使用默认的 {@link MockServletContext} 创建一个新的 {@code MockMultipartHttpServletRequest}。
	 *
	 * @see #MockMultipartHttpServletRequest(ServletContext)
	 */
	public MockMultipartHttpServletRequest() {
		this(null);
	}

	/**
	 * 使用提供的 {@link ServletContext} 创建一个新的 {@code MockMultipartHttpServletRequest}。
	 *
	 * @param servletContext 请求运行的 ServletContext（可以为 {@code null} 以使用默认的 {@link MockServletContext}）
	 */
	public MockMultipartHttpServletRequest(@Nullable ServletContext servletContext) {
		super(servletContext);
		setMethod("POST");
		setContentType("multipart/form-data");
	}


	/**
	 * 向此请求添加文件。从 multipart 表单中获取参数名称。
	 *
	 * @param file 要添加的多部分文件
	 */
	public void addFile(MultipartFile file) {
		Assert.notNull(file, "MultipartFile must not be null");
		this.multipartFiles.add(file.getName(), file);
	}

	@Override
	public Iterator<String> getFileNames() {
		return this.multipartFiles.keySet().iterator();
	}

	@Override
	public MultipartFile getFile(String name) {
		return this.multipartFiles.getFirst(name);
	}

	@Override
	public List<MultipartFile> getFiles(String name) {
		List<MultipartFile> multipartFiles = this.multipartFiles.get(name);
		if (multipartFiles != null) {
			// 如果多部分文件列表不为空，则返回该列表
			return multipartFiles;
		} else {
			// 否则，返回一个空的列表
			return Collections.emptyList();
		}
	}

	@Override
	public Map<String, MultipartFile> getFileMap() {
		return this.multipartFiles.toSingleValueMap();
	}

	@Override
	public MultiValueMap<String, MultipartFile> getMultiFileMap() {
		return new LinkedMultiValueMap<>(this.multipartFiles);
	}

	@Override
	public String getMultipartContentType(String paramOrFileName) {
		MultipartFile file = getFile(paramOrFileName);
		if (file != null) {
			// 如果文件不为空，则返回文件的内容类型
			return file.getContentType();
		}
		try {
			// 尝试获取指定参数名的Part对象
			Part part = getPart(paramOrFileName);
			if (part != null) {
				// 如果Part对象不为空，则返回Part对象的内容类型
				return part.getContentType();
			}
		} catch (ServletException | IOException ex) {
			// 捕获异常并抛出IllegalStateException异常
			throw new IllegalStateException(ex);
		}
		// 如果无法获取到文件或Part对象，则返回null
		return null;
	}

	@Override
	public HttpMethod getRequestMethod() {
		return HttpMethod.resolve(getMethod());
	}

	@Override
	public HttpHeaders getRequestHeaders() {
		// 创建一个HttpHeaders对象来存储所有的头部信息
		HttpHeaders headers = new HttpHeaders();
		// 获取所有头部名的枚举
		Enumeration<String> headerNames = getHeaderNames();
		// 遍历枚举中的每个头部名
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			// 将头部名及其对应的值列表添加到HttpHeaders对象中
			headers.put(headerName, Collections.list(getHeaders(headerName)));
		}
		// 返回存储头部信息的HttpHeaders对象
		return headers;
	}

	@Override
	public HttpHeaders getMultipartHeaders(String paramOrFileName) {
		MultipartFile file = getFile(paramOrFileName);
		if (file != null) {
			// 如果文件不为空，则构建包含Content-Type头部的HttpHeaders对象
			HttpHeaders headers = new HttpHeaders();
			if (file.getContentType() != null) {
				headers.add(HttpHeaders.CONTENT_TYPE, file.getContentType());
			}
			return headers;
		}
		try {
			// 尝试获取指定参数名的Part对象
			Part part = getPart(paramOrFileName);
			if (part != null) {
				// 如果Part对象不为空，则构建包含所有头部信息的HttpHeaders对象
				HttpHeaders headers = new HttpHeaders();
				for (String headerName : part.getHeaderNames()) {
					headers.put(headerName, new ArrayList<>(part.getHeaders(headerName)));
				}
				return headers;
			}
		} catch (Throwable ex) {
			// 捕获异常并抛出MultipartException异常
			throw new MultipartException("Could not access multipart servlet request", ex);
		}
		// 如果无法获取到文件或Part对象，则返回null
		return null;
	}

}
