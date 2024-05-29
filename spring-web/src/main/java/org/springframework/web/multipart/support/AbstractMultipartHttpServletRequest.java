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

package org.springframework.web.multipart.support;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.*;

/**
 * MultipartHttpServletRequest 接口的抽象基础实现。提供预生成 MultipartFile 实例的管理。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 06.10.2003
 */
public abstract class AbstractMultipartHttpServletRequest extends HttpServletRequestWrapper
		implements MultipartHttpServletRequest {
	/**
	 * 参数名称 —— 多部分文件映射
	 */
	@Nullable
	private MultiValueMap<String, MultipartFile> multipartFiles;


	/**
	 * 使用给定的 HttpServletRequest 包装一个 MultipartHttpServletRequest。
	 *
	 * @param request 要包装的请求
	 */
	protected AbstractMultipartHttpServletRequest(HttpServletRequest request) {
		super(request);
	}


	@Override
	public HttpServletRequest getRequest() {
		return (HttpServletRequest) super.getRequest();
	}

	@Override
	public HttpMethod getRequestMethod() {
		return HttpMethod.resolve(getRequest().getMethod());
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
	public Iterator<String> getFileNames() {
		return getMultipartFiles().keySet().iterator();
	}

	@Override
	public MultipartFile getFile(String name) {
		return getMultipartFiles().getFirst(name);
	}

	@Override
	public List<MultipartFile> getFiles(String name) {
		// 根据名称获取多部分文件列表
		List<MultipartFile> multipartFiles = getMultipartFiles().get(name);
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
		return getMultipartFiles().toSingleValueMap();
	}

	@Override
	public MultiValueMap<String, MultipartFile> getMultiFileMap() {
		return getMultipartFiles();
	}

	/**
	 * 确定底层多部分请求是否已解析。
	 *
	 * @return {@code true} 表示已急切地初始化或在首次访问参数或多部分文件之前已中止的惰性解析请求时，{@code false}
	 * @see #getMultipartFiles()
	 * @since 4.3.15
	 */
	public boolean isResolved() {
		return (this.multipartFiles != null);
	}


	/**
	 * 设置参数名称作为键和 MultipartFile 对象列表作为值的映射。由子类在初始化时调用。
	 */
	protected final void setMultipartFiles(MultiValueMap<String, MultipartFile> multipartFiles) {
		this.multipartFiles =
				new LinkedMultiValueMap<>(Collections.unmodifiableMap(multipartFiles));
	}

	/**
	 * 获取 MultipartFile 映射以进行检索，如有必要则延迟初始化。
	 *
	 * @see #initializeMultipart()
	 */
	protected MultiValueMap<String, MultipartFile> getMultipartFiles() {
		if (this.multipartFiles == null) {
			// 如果多部分文件列表为空，则初始化多部分内容
			initializeMultipart();
		}
		// 返回多部分文件列表
		return this.multipartFiles;
	}

	/**
	 * 如果可能，延迟初始化多部分请求。仅在尚未急切初始化时调用。
	 */
	protected void initializeMultipart() {
		throw new IllegalStateException("Multipart request not initialized");
	}

}
