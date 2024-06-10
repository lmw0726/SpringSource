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
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;

/**
 * 一个 {@code ContentNegotiationStrategy}，将请求路径中的文件扩展名解析为用于查找媒体类型的键。
 *
 * <p>如果文件扩展名在构造函数中提供的显式注册中找不到，则使用 {@link MediaTypeFactory} 作为后备机制。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 * @deprecated 从 5.2.4 开始。
 * 请参阅 {@link ContentNegotiationManagerFactoryBean} 中的类级别注释，
 * 了解路径扩展名配置选项的已弃用情况。
 */
@Deprecated
public class PathExtensionContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {
	/**
	 * URL路径助手
	 */
	private UrlPathHelper urlPathHelper = new UrlPathHelper();


	/**
	 * 创建一个没有任何映射的实例。如果通过 Java 激活框架解析任何扩展名，则稍后可以添加映射。
	 */
	public PathExtensionContentNegotiationStrategy() {
		this(null);
	}

	/**
	 * 使用给定的文件扩展名和媒体类型映射创建一个实例。
	 */
	public PathExtensionContentNegotiationStrategy(@Nullable Map<String, MediaType> mediaTypes) {
		// 调用父类的构造函数，使用给定的媒体类型列表初始化
		super(mediaTypes);

		// 设置是否仅使用已注册的扩展名
		setUseRegisteredExtensionsOnly(false);

		// 设置是否忽略未知的扩展名
		setIgnoreUnknownExtensions(true);

		// 禁用 URL 解码
		this.urlPathHelper.setUrlDecode(false);
	}


	/**
	 * 配置一个 {@code UrlPathHelper} 用于 {@link #getMediaTypeKey} 中使用，
	 * 以便为目标请求 URL 路径派生查找路径。
	 *
	 * @since 4.2.8
	 */
	public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 指示是否使用 Java 激活框架作为从文件扩展名到媒体类型的后备选项映射。
	 *
	 * @deprecated 从 5.0 开始，使用 {@link #setUseRegisteredExtensionsOnly(boolean)}。
	 */
	@Deprecated
	public void setUseJaf(boolean useJaf) {
		setUseRegisteredExtensionsOnly(!useJaf);
	}

	@Override
	@Nullable
	protected String getMediaTypeKey(NativeWebRequest webRequest) {
		// 从 Web请求 中获取原生 HttpServlet请求 对象
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		// 如果获取的 HttpServlet请求 为空，则返回null
		if (request == null) {
			return null;
		}
		// 忽略 LOOKUP_PATH 属性，使用自定义的 Url路径助手，并关闭解码
		String path = this.urlPathHelper.getLookupPathForRequest(request);
		// 提取路径中的文件扩展名
		String extension = UriUtils.extractFileExtension(path);
		// 如果提取到的扩展名不为空，则转换为小写并返回；否则返回null
		return (StringUtils.hasText(extension) ? extension.toLowerCase(Locale.ENGLISH) : null);
	}

	/**
	 * 一个公共方法，将路径扩展名策略的知识暴露给这个方法来为给定的 {@link Resource} 解析文件扩展名到 {@link MediaType}。
	 * 该方法首先查找任何显式注册的文件扩展名，然后如果可用，则使用 {@link MediaTypeFactory} 进行后备。
	 *
	 * @param resource 要查找的资源
	 * @return 扩展名的 MediaType，如果找不到则返回 {@code null}
	 * @since 4.3
	 */
	@Nullable
	public MediaType getMediaTypeForResource(Resource resource) {
		Assert.notNull(resource, "Resource must not be null");
		MediaType mediaType = null;
		// 获取资源文件名
		String filename = resource.getFilename();
		// 获取文件名的扩展名
		String extension = StringUtils.getFilenameExtension(filename);
		if (extension != null) {
			// 如果扩展名不为空，则查找对应的媒体类型
			mediaType = lookupMediaType(extension);
		}
		if (mediaType == null) {
			// 如果媒体类型仍为null，则使用 媒体类型工厂 获取媒体类型
			mediaType = MediaTypeFactory.getMediaType(filename).orElse(null);
		}
		// 返回媒体类型
		return mediaType;
	}

}
