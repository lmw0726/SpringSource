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

package org.springframework.web.multipart.commons;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 基于 Servlet 的 {@link MultipartResolver} 实现，使用 <a href="https://commons.apache.org/proper/commons-fileupload">Apache Commons FileUpload</a>
 * 1.2 或更高版本。此解析器变体委托给应用程序内部的本地 FileUpload 库，提供了最大的 Servlet 容器可移植性。
 *
 * <p>Commons FileUpload 传统上解析带有任何 "multipart/" 类型的 POST 请求。支持的 HTTP 方法可以通过 {@link #setSupportedMethods} 进行自定义。
 *
 * <p>提供 "maxUploadSize"、"maxInMemorySize" 和 "defaultEncoding" 设置作为 bean 属性（从 {@link CommonsFileUploadSupport} 继承）。
 * 有关默认值和接受的值的详细信息，请参阅相应的 ServletFileUpload / DiskFileItemFactory 属性 ("sizeMax"、"sizeThreshold"、"headerEncoding")。
 *
 * <p>将临时文件保存到 Servlet 容器的临时目录中。需要通过应用程序上下文或通过接受 ServletContext 的构造函数进行初始化（用于独立使用）。
 *
 * <p>注意：常见的替代方案是 {@link org.springframework.web.multipart.support.StandardServletMultipartResolver}，委托给 Servlet
 * 容器自己的多部分解析器，并在容器级别进行配置，可能存在容器特定的限制。
 *
 * @author Trevor D. Cook
 * @author Juergen Hoeller
 * @see #CommonsMultipartResolver(ServletContext)
 * @see #setResolveLazily
 * @see #setSupportedMethods
 * @see org.apache.commons.fileupload.servlet.ServletFileUpload
 * @see org.apache.commons.fileupload.disk.DiskFileItemFactory
 * @see org.springframework.web.multipart.support.StandardServletMultipartResolver
 * @since 29.09.2003
 */
public class CommonsMultipartResolver extends CommonsFileUploadSupport
		implements MultipartResolver, ServletContextAware {

	/**
	 * 是否延迟解析
	 */
	private boolean resolveLazily = false;

	/**
	 * 支持的方法名称
	 */
	@Nullable
	private Set<String> supportedMethods;


	/**
	 * 用作 bean 的构造函数。通过 ServletContext（通常由 WebApplicationContext 传递）确定 Servlet 容器的临时目录。
	 *
	 * @see #setServletContext
	 * @see org.springframework.web.context.ServletContextAware
	 * @see org.springframework.web.context.WebApplicationContext
	 */
	public CommonsMultipartResolver() {
		super();
	}

	/**
	 * 用于独立使用的构造函数。通过给定的 ServletContext 确定 Servlet 容器的临时目录。
	 *
	 * @param servletContext 要使用的 ServletContext
	 */
	public CommonsMultipartResolver(ServletContext servletContext) {
		this();
		setServletContext(servletContext);
	}


	/**
	 * 设置是否在文件或参数访问时延迟解析多部分请求。
	 * <p>默认值为 "false"，立即解析多部分元素，在 {@link #resolveMultipart} 调用时抛出相应的异常。
	 * 将其设置为 "true" 可以进行延迟解析，一旦应用程序尝试获取多部分文件或参数，就会抛出解析异常。
	 */
	public void setResolveLazily(boolean resolveLazily) {
		this.resolveLazily = resolveLazily;
	}

	/**
	 * 将支持的方法指定为 HTTP 方法名称数组。传统的 Commons FileUpload 默认值仅为 "POST"。
	 * <p>当配置为 Spring 属性值时，这可以是逗号分隔的字符串: 例如 "POST,PUT"。
	 *
	 * @since 5.3.9
	 */
	public void setSupportedMethods(String... supportedMethods) {
		this.supportedMethods = new HashSet<>(Arrays.asList(supportedMethods));
	}

	/**
	 * 初始化底层的 {@code org.apache.commons.fileupload.servlet.ServletFileUpload} 实例。
	 * 可以重写为使用自定义子类，例如用于测试目的。
	 *
	 * @param fileItemFactory 要使用的 Commons FileItemFactory
	 * @return 新的 ServletFileUpload 实例
	 */
	@Override
	protected FileUpload newFileUpload(FileItemFactory fileItemFactory) {
		return new ServletFileUpload(fileItemFactory);
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		if (!isUploadTempDirSpecified()) {
			// 如果未指定上传临时目录，则设置文件项工厂的存储库为Servlet上下文的临时目录
			getFileItemFactory().setRepository(WebUtils.getTempDir(servletContext));
		}
	}


	@Override
	public boolean isMultipart(HttpServletRequest request) {
		return (this.supportedMethods != null ?
				// 如果支持的请求方法不为空，则检查请求方法是否在支持的方法列表中，并且请求是否为多部分内容
				this.supportedMethods.contains(request.getMethod()) &&
						FileUploadBase.isMultipartContent(new ServletRequestContext(request)) :
				// 否则，使用ServletFileUpload.isMultipartContent检查请求是否为多部分内容
				ServletFileUpload.isMultipartContent(request));
	}

	@Override
	public MultipartHttpServletRequest resolveMultipart(final HttpServletRequest request) throws MultipartException {
		Assert.notNull(request, "Request must not be null");
		if (this.resolveLazily) {
			// 如果懒加载解析
			return new DefaultMultipartHttpServletRequest(request) {
				@Override
				protected void initializeMultipart() {
					// 解析请求
					MultipartParsingResult parsingResult = parseRequest(request);
					// 设置多部分文件
					setMultipartFiles(parsingResult.getMultipartFiles());
					// 设置多部分参数
					setMultipartParameters(parsingResult.getMultipartParameters());
					// 设置多部分参数的内容类型
					setMultipartParameterContentTypes(parsingResult.getMultipartParameterContentTypes());
				}
			};
		} else {
			// 如果不是懒加载解析
			// 解析请求
			MultipartParsingResult parsingResult = parseRequest(request);
			// 返回DefaultMultipartHttpServletRequest实例
			return new DefaultMultipartHttpServletRequest(request, parsingResult.getMultipartFiles(),
					parsingResult.getMultipartParameters(), parsingResult.getMultipartParameterContentTypes());
		}
	}

	/**
	 * 解析给定的 servlet 请求，解析其中的多部分元素。
	 *
	 * @param request 要解析的请求
	 * @return 解析结果
	 * @throws MultipartException 如果多部分解析失败。
	 */
	protected MultipartParsingResult parseRequest(HttpServletRequest request) throws MultipartException {
		// 确定请求的编码
		String encoding = determineEncoding(request);
		// 准备文件上传操作
		FileUpload fileUpload = prepareFileUpload(encoding);
		try {
			// 解析请求中的文件项
			List<FileItem> fileItems = ((ServletFileUpload) fileUpload).parseRequest(request);
			// 解析文件项并返回
			return parseFileItems(fileItems, encoding);
		} catch (FileUploadBase.SizeLimitExceededException ex) {
			// 文件大小超出限制异常
			throw new MaxUploadSizeExceededException(fileUpload.getSizeMax(), ex);
		} catch (FileUploadBase.FileSizeLimitExceededException ex) {
			// 单个文件大小超出限制异常
			throw new MaxUploadSizeExceededException(fileUpload.getFileSizeMax(), ex);
		} catch (FileUploadException ex) {
			// 解析多部分servlet请求失败异常
			throw new MultipartException("Failed to parse multipart servlet request", ex);
		}
	}

	/**
	 * 确定给定请求的编码。
	 * 可以在子类中重写。
	 * <p>默认实现检查请求编码，如果找不到，则使用此解析器指定的默认编码。
	 *
	 * @param request 当前 HTTP 请求
	 * @return 请求的编码（永不为 {@code null}）
	 * @see javax.servlet.ServletRequest#getCharacterEncoding
	 * @see #setDefaultEncoding
	 */
	protected String determineEncoding(HttpServletRequest request) {
		// 获取请求的字符编码
		String encoding = request.getCharacterEncoding();
		// 如果编码为空，则使用默认编码
		if (encoding == null) {
			encoding = getDefaultEncoding();
		}
		// 返回编码
		return encoding;
	}

	@Override
	public void cleanupMultipart(MultipartHttpServletRequest request) {
		if (!(request instanceof AbstractMultipartHttpServletRequest) ||
				((AbstractMultipartHttpServletRequest) request).isResolved()) {
			// 如果请求不是AbstractMultipartHttpServletRequest的实例，或者已经解析过
			try {
				// 尝试清理多部分文件项
				cleanupFileItems(request.getMultiFileMap());
			} catch (Throwable ex) {
				// 捕获并记录清理多部分文件项时的异常
				logger.warn("Failed to perform multipart cleanup for servlet request", ex);
			}
		}
	}

}
