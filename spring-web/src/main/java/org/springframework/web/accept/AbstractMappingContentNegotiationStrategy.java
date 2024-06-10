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

package org.springframework.web.accept;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code ContentNegotiationStrategy} 实现的基类，包含解析请求到媒体类型的步骤。
 *
 * <p>首先必须从请求中提取一个键（例如 "json"、"pdf"），然后通过基类 {@link MappingMediaTypeFileExtensionResolver} 将该键解析为媒体类型。
 *
 * <p>方法 {@link #handleNoMatch} 允许子类插入额外的查找媒体类型的方式（例如通过 Java 激活框架，或 {@link javax.servlet.ServletContext#getMimeType}）。
 * 通过基类解析的媒体类型然后添加到基类 {@link MappingMediaTypeFileExtensionResolver} 中，即缓存以供新的查找。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public abstract class AbstractMappingContentNegotiationStrategy extends MappingMediaTypeFileExtensionResolver
		implements ContentNegotiationStrategy {
	/**
	 * 日志记录器
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 是否仅使用已注册的映射来查找文件扩展名
	 */
	private boolean useRegisteredExtensionsOnly = false;

	/**
	 * 是否忽略具有未知文件扩展名的请求
	 */
	private boolean ignoreUnknownExtensions = false;


	/**
	 * 使用给定的文件扩展名和媒体类型映射创建一个实例。
	 */
	public AbstractMappingContentNegotiationStrategy(@Nullable Map<String, MediaType> mediaTypes) {
		super(mediaTypes);
	}


	/**
	 * 是否仅使用已注册的映射来查找文件扩展名，还是也使用动态解析（例如通过 {@link MediaTypeFactory}）。
	 * <p>默认情况下，此项设置为 {@code false}。
	 */
	public void setUseRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
		this.useRegisteredExtensionsOnly = useRegisteredExtensionsOnly;
	}

	public boolean isUseRegisteredExtensionsOnly() {
		return this.useRegisteredExtensionsOnly;
	}

	/**
	 * 是否忽略具有未知文件扩展名的请求。将此设置为 {@code false} 会导致 {@code HttpMediaTypeNotAcceptableException}。
	 * <p>默认情况下，此项设置为 {@literal false}，但在 {@link PathExtensionContentNegotiationStrategy} 中被覆盖为 {@literal true}。
	 */
	public void setIgnoreUnknownExtensions(boolean ignoreUnknownExtensions) {
		this.ignoreUnknownExtensions = ignoreUnknownExtensions;
	}

	public boolean isIgnoreUnknownExtensions() {
		return this.ignoreUnknownExtensions;
	}


	@Override
	public List<MediaType> resolveMediaTypes(NativeWebRequest webRequest)
			throws HttpMediaTypeNotAcceptableException {

		return resolveMediaTypeKey(webRequest, getMediaTypeKey(webRequest));
	}

	/**
	 * {@link #resolveMediaTypes(NativeWebRequest)} 的替代方法，接受已提取的键。
	 *
	 * @since 3.2.16
	 */
	public List<MediaType> resolveMediaTypeKey(NativeWebRequest webRequest, @Nullable String key)
			throws HttpMediaTypeNotAcceptableException {

		// 如果键不为空
		if (StringUtils.hasText(key)) {
			// 查找键对应的媒体类型
			MediaType mediaType = lookupMediaType(key);
			// 如果找到了匹配的媒体类型
			if (mediaType != null) {
				// 处理匹配，并返回媒体类型的单例列表
				handleMatch(key, mediaType);
				return Collections.singletonList(mediaType);
			}
			// 否则，处理未匹配情况，并返回媒体类型
			mediaType = handleNoMatch(webRequest, key);
			if (mediaType != null) {
				// 添加映射，并返回媒体类型的单例列表
				addMapping(key, mediaType);
				return Collections.singletonList(mediaType);
			}
		}
		// 返回 媒体类型全部列表
		return MEDIA_TYPE_ALL_LIST;
	}


	/**
	 * 从请求中提取用于查找媒体类型的键。
	 *
	 * @return 查找键，如果没有则返回 {@code null}
	 */
	@Nullable
	protected abstract String getMediaTypeKey(NativeWebRequest request);

	/**
	 * 在通过 {@link #lookupMediaType} 成功解析键时提供处理方法。
	 */
	protected void handleMatch(String key, MediaType mediaType) {
	}

	/**
	 * 在未通过 {@link #lookupMediaType} 解析键时提供处理方法。
	 * 子类可以采取进一步的步骤来确定媒体类型。
	 * 如果此方法返回 MediaType，则将其添加到基类中的缓存中。
	 */
	@Nullable
	protected MediaType handleNoMatch(NativeWebRequest request, String key)
			throws HttpMediaTypeNotAcceptableException {

		// 如果不仅使用已注册的扩展名
		if (!isUseRegisteredExtensionsOnly()) {
			// 获取指定文件扩展名对应的媒体类型
			Optional<MediaType> mediaType = MediaTypeFactory.getMediaType("file." + key);

			// 如果获取到了媒体类型，则返回该媒体类型
			if (mediaType.isPresent()) {
				return mediaType.get();
			}
		}

		// 如果忽略未知的扩展名
		if (isIgnoreUnknownExtensions()) {
			// 返回空
			return null;
		}

		// 抛出 HttpMediaTypeNotAcceptableException 异常，表示不可接受的媒体类型
		throw new HttpMediaTypeNotAcceptableException(getAllMediaTypes());
	}

}
