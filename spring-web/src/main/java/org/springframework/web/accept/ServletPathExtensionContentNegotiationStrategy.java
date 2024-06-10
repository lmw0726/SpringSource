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

package org.springframework.web.accept;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.ServletContext;
import java.util.Map;

/**
 * 扩展 {@code PathExtensionContentNegotiationStrategy}，还使用
 * {@link ServletContext#getMimeType(String)} 来解析文件扩展名。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 * @deprecated 从 5.2.4 开始。请参阅 {@link ContentNegotiationManagerFactoryBean} 中的类级别注释，了解路径扩展名配置选项的已弃用情况。
 */
@Deprecated
public class ServletPathExtensionContentNegotiationStrategy extends PathExtensionContentNegotiationStrategy {
	/**
	 * Servlet上下文
	 */
	private final ServletContext servletContext;


	/**
	 * 创建一个没有任何映射的实例。当通过
	 * {@link ServletContext#getMimeType(String)} 或
	 * 通过 {@link org.springframework.http.MediaTypeFactory} 解析扩展名时，
	 * 可以稍后添加映射。
	 */
	public ServletPathExtensionContentNegotiationStrategy(ServletContext context) {
		this(context, null);
	}

	/**
	 * 使用给定的扩展名到 MediaType 查找创建一个实例。
	 */
	public ServletPathExtensionContentNegotiationStrategy(
			ServletContext servletContext, @Nullable Map<String, MediaType> mediaTypes) {

		super(mediaTypes);
		Assert.notNull(servletContext, "ServletContext is required");
		this.servletContext = servletContext;
	}


	/**
	 * 通过 {@link ServletContext#getMimeType(String)} 解析文件扩展名，
	 * 并且也委托给基类进行可能的
	 * {@link org.springframework.http.MediaTypeFactory} 查找。
	 */
	@Override
	@Nullable
	protected MediaType handleNoMatch(NativeWebRequest webRequest, String extension)
			throws HttpMediaTypeNotAcceptableException {

		MediaType mediaType = null;

		// 获取指定文件扩展名对应的 MIME 类型
		String mimeType = this.servletContext.getMimeType("file." + extension);

		if (StringUtils.hasText(mimeType)) {
			// 如果 MIME 类型不为空，则解析为媒体类型
			mediaType = MediaType.parseMediaType(mimeType);
		}

		// 如果媒体类型仍然为空，或者是 application_octet_stream 类型
		if (mediaType == null || MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
			// 调用父类的 handleNoMatch 方法获取媒体类型
			MediaType superMediaType = super.handleNoMatch(webRequest, extension);

			// 如果父类返回了媒体类型，则使用父类返回的媒体类型
			if (superMediaType != null) {
				mediaType = superMediaType;
			}
		}

		// 返回解析或获取的媒体类型
		return mediaType;
	}

	/**
	 * 扩展基类 {@link PathExtensionContentNegotiationStrategy#getMediaTypeForResource}，
	 * 还能够通过 ServletContext 进行查找。
	 *
	 * @param resource 要查找的资源
	 * @return 扩展名的 MediaType，如果找不到则返回 {@code null}
	 * @since 4.3
	 */
	@Override
	public MediaType getMediaTypeForResource(Resource resource) {
		// 初始化媒体类型为null
		MediaType mediaType = null;
		// 获取资源文件名对应的MIME类型
		String mimeType = this.servletContext.getMimeType(resource.getFilename());
		// 如果MIME类型不为空，则解析为媒体类型
		if (StringUtils.hasText(mimeType)) {
			mediaType = MediaType.parseMediaType(mimeType);
		}
		// 如果媒体类型为null，或为 application_octet_stream
		if (mediaType == null || MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
			// 调用父类方法获取资源的媒体类型
			MediaType superMediaType = super.getMediaTypeForResource(resource);
			// 如果父类方法返回的媒体类型不为空，则使用父类方法返回的媒体类型
			if (superMediaType != null) {
				mediaType = superMediaType;
			}
		}
		// 返回媒体类型
		return mediaType;
	}

}
