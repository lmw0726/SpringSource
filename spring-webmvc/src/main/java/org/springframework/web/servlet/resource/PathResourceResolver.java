/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.servlet.resource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 一个简单的 {@code ResourceResolver}，尝试在给定的位置下查找与请求路径匹配的资源。
 *
 * <p>此解析器不委托给 {@code ResourceResolverChain}，预期在解析器链的末尾进行配置。
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 */
public class PathResourceResolver extends AbstractResourceResolver {

	/**
	 * 允许的位置数组
	 */
	@Nullable
	private Resource[] allowedLocations;

	/**
	 * 与位置关联的字符集
	 * 资源-字符集映射
	 */
	private final Map<Resource, Charset> locationCharsets = new HashMap<>(4);

	/**
	 * URL路径助手
	 */
	@Nullable
	private UrlPathHelper urlPathHelper;


	/**
	 * 默认情况下，当找到 Resource 时，将比较已解析资源的路径，以确保其位于找到的输入位置下。
	 * 但是有时这可能不是情况，例如当 {@link org.springframework.web.servlet.resource.CssLinkResourceTransformer}
	 * 解析其包含的链接的公共 URL 时，CSS 文件是位置，正在解析的资源是位于相邻或父目录中的 CSS 文件、图像、字体等。
	 * <p>此属性允许配置一个完整的位置列表，资源必须位于其中，以便如果资源不在相对于其找到的位置下，也可以检查此列表。
	 * <p>默认情况下，{@link ResourceHttpRequestHandler} 将此属性初始化为匹配其位置列表。
	 *
	 * @param locations 允许的位置列表
	 * @see ResourceHttpRequestHandler#initAllowedLocations()
	 * @since 4.1.2
	 */
	public void setAllowedLocations(@Nullable Resource... locations) {
		this.allowedLocations = locations;
	}

	@Nullable
	public Resource[] getAllowedLocations() {
		return this.allowedLocations;
	}

	/**
	 * 配置与位置关联的字符集。
	 * 如果在 {@link org.springframework.core.io.UrlResource URL 资源}
	 * 位置下找到静态资源，则使用字符集对相对路径进行编码
	 * <p><strong>注意：</strong>只有在还配置了 {@link #setUrlPathHelper urlPathHelper} 属性，
	 * 并且其 {@code urlDecode} 属性设置为 true 时，才会使用字符集。
	 *
	 * @since 4.3.13
	 */
	public void setLocationCharsets(Map<Resource, Charset> locationCharsets) {
		this.locationCharsets.clear();
		this.locationCharsets.putAll(locationCharsets);
	}

	/**
	 * 返回与静态资源位置关联的字符集。
	 *
	 * @since 4.3.13
	 */
	public Map<Resource, Charset> getLocationCharsets() {
		return Collections.unmodifiableMap(this.locationCharsets);
	}

	/**
	 * 提供对用于将请求映射到静态资源的 {@link UrlPathHelper} 的引用。
	 * 这有助于获取有关查找路径的信息，例如它是否已解码。
	 *
	 * @since 4.3.13
	 */
	public void setUrlPathHelper(@Nullable UrlPathHelper urlPathHelper) {
		this.urlPathHelper = urlPathHelper;
	}

	/**
	 * 已配置的 {@link UrlPathHelper}。
	 *
	 * @since 4.3.13
	 */
	@Nullable
	public UrlPathHelper getUrlPathHelper() {
		return this.urlPathHelper;
	}


	@Override
	protected Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath,
											   List<? extends Resource> locations, ResourceResolverChain chain) {

		return getResource(requestPath, request, locations);
	}

	@Override
	protected String resolveUrlPathInternal(String resourcePath, List<? extends Resource> locations,
											ResourceResolverChain chain) {

		// 如果资源路径不为空，并且能够获取该路径对应的资源，则直接返回资源路径
		if (StringUtils.hasText(resourcePath) &&
				getResource(resourcePath, null, locations) != null) {
			return resourcePath;
		}

		// 否则返回null
		return null;
	}

	@Nullable
	private Resource getResource(String resourcePath, @Nullable HttpServletRequest request,
								 List<? extends Resource> locations) {

		// 遍历所有的资源位置
		for (Resource location : locations) {
			try {
				// 对资源路径进行必要的编码或解码处理
				String pathToUse = encodeOrDecodeIfNecessary(resourcePath, request, location);
				// 获取资源
				Resource resource = getResource(pathToUse, location);
				// 如果成功获取到资源，则直接返回
				if (resource != null) {
					return resource;
				}
			} catch (IOException ex) {
				// 如果在处理过程中发生IOException异常，则记录日志，并继续处理下一个位置的资源
				if (logger.isDebugEnabled()) {
					String error = "Skip location [" + location + "] due to error";
					if (logger.isTraceEnabled()) {
						logger.trace(error, ex);
					} else {
						logger.debug(error + ": " + ex.getMessage());
					}
				}
			}
		}

		// 如果所有位置的资源都无法获取到，则返回null
		return null;
	}

	/**
	 * 查找给定位置下的资源。
	 * <p>默认实现检查是否存在给定位置相对路径的可读 {@code Resource}。
	 *
	 * @param resourcePath 资源路径
	 * @param location     要检查的位置
	 * @return 资源，如果找不到则为 {@code null}
	 */
	@Nullable
	protected Resource getResource(String resourcePath, Resource location) throws IOException {
		// 创建相对路径的资源
		Resource resource = location.createRelative(resourcePath);
		// 如果资源可读
		if (resource.isReadable()) {
			// 检查资源是否符合要求
			if (checkResource(resource, location)) {
				return resource;
			} else if (logger.isWarnEnabled()) {
				// 如果资源不符合要求，则记录警告日志
				Resource[] allowed = getAllowedLocations();
				logger.warn(LogFormatUtils.formatValue(
						"Resource path \"" + resourcePath + "\" was successfully resolved " +
								"but resource \"" + resource.getURL() + "\" is neither under " +
								"the current location \"" + location.getURL() + "\" nor under any of " +
								"the allowed locations " + (allowed != null ? Arrays.asList(allowed) : "[]"), -1, true));
			}
		}

		// 如果资源不可读或不符合要求，则返回null
		return null;
	}

	/**
	 * 在检查资源是否存在且可读之外，对已解析的资源执行其他检查。
	 * 默认实现还验证资源是位于找到的位置的相对位置，还是位于 {@link #setAllowedLocations 允许的位置} 之一下。
	 *
	 * @param resource 要检查的资源
	 * @param location 资源被找到的位置相对位置
	 * @return 如果资源在有效位置，则为 "true"，否则为 "false"
	 * @since 4.1.2
	 */
	protected boolean checkResource(Resource resource, Resource location) throws IOException {
		// 如果资源位于当前位置下，则返回true
		if (isResourceUnderLocation(resource, location)) {
			return true;
		}

		// 获取允许的位置数组
		Resource[] allowedLocations = getAllowedLocations();
		if (allowedLocations != null) {
			// 如果存在允许的位置数组，则遍历允许的位置数组
			for (Resource current : allowedLocations) {
				// 如果资源位于当前遍历的允许位置下，则返回true
				if (isResourceUnderLocation(resource, current)) {
					return true;
				}
			}
		}

		// 否则返回false
		return false;
	}

	private boolean isResourceUnderLocation(Resource resource, Resource location) throws IOException {
		// 如果资源和位置的类型不同，则返回false
		if (resource.getClass() != location.getClass()) {
			return false;
		}

		String resourcePath;
		String locationPath;

		// 根据资源的类型获取资源路径和位置路径
		if (resource instanceof UrlResource) {
			resourcePath = resource.getURL().toExternalForm();
			locationPath = StringUtils.cleanPath(location.getURL().toString());
		} else if (resource instanceof ClassPathResource) {
			resourcePath = ((ClassPathResource) resource).getPath();
			locationPath = StringUtils.cleanPath(((ClassPathResource) location).getPath());
		} else if (resource instanceof ServletContextResource) {
			resourcePath = ((ServletContextResource) resource).getPath();
			locationPath = StringUtils.cleanPath(((ServletContextResource) location).getPath());
		} else {
			resourcePath = resource.getURL().getPath();
			locationPath = StringUtils.cleanPath(location.getURL().getPath());
		}

		// 如果资源路径和位置路径相等，则返回true
		if (locationPath.equals(resourcePath)) {
			return true;
		}

		// 如果位置路径是以斜杠结尾或为空，则保持不变，否则添加斜杠结尾
		locationPath = (locationPath.endsWith("/") || locationPath.isEmpty() ? locationPath : locationPath + "/");
		// 如果资源路径以位置路径开头，且不是无效编码路径，则返回true
		return (resourcePath.startsWith(locationPath) && !isInvalidEncodedPath(resourcePath));
	}

	private String encodeOrDecodeIfNecessary(String path, @Nullable HttpServletRequest request, Resource location) {
		// 如果存在Http请求
		if (request != null) {
			// 检查是否使用了路径模式
			boolean usesPathPattern = (
					ServletRequestPathUtils.hasCachedPath(request) &&
							ServletRequestPathUtils.getCachedPath(request) instanceof PathContainer);

			// 如果应该解码相对路径
			if (shouldDecodeRelativePath(location, usesPathPattern)) {
				// 以 UTF-8 编码格式，对路径进行解码
				return UriUtils.decode(path, StandardCharsets.UTF_8);
			} else if (shouldEncodeRelativePath(location, usesPathPattern)) {
				// 如果应该编码相对路径
				// 获取位置的字符集，如果未指定，则使用UTF-8
				Charset charset = this.locationCharsets.getOrDefault(location, StandardCharsets.UTF_8);
				StringBuilder sb = new StringBuilder();
				// 根据/分割路径，获取每一个字符串
				StringTokenizer tokenizer = new StringTokenizer(path, "/");
				// 逐个对路径中的每个部分进行编码，并拼接到StringBuilder中
				while (tokenizer.hasMoreTokens()) {
					// 对路径以特定字符集进行解码
					String value = UriUtils.encode(tokenizer.nextToken(), charset);
					sb.append(value);
					sb.append('/');
				}
				// 如果路径不以斜杠结尾，则删除StringBuilder末尾的斜杠
				if (!path.endsWith("/")) {
					sb.setLength(sb.length() - 1);
				}
				return sb.toString();
			}
		}

		// 否则直接返回原始路径
		return path;
	}

	/**
	 * 当 {@code HandlerMapping} 设置为不解码 URL 路径时，需要对非-{@code UrlResource} 位置的路径进行解码。
	 */
	private boolean shouldDecodeRelativePath(Resource location, boolean usesPathPattern) {
		if (location instanceof UrlResource) {
			// 如果 位置资源是UrlResource类型，返回false
			return false;
		}
		if (this.urlPathHelper != null && !this.urlPathHelper.isUrlDecode()) {
			// 存在URL路径助手且 不需要进行URL解码，则返回true
			return true;
		} else if (usesPathPattern) {
			// 使用了路径模式，则返回true
			return true;
		}
		return false;
	}

	/**
	 * 当 {@code HandlerMapping} 设置为解码 URL 路径时，需要对 {@code UrlResource} 位置的路径进行编码。
	 */
	private boolean shouldEncodeRelativePath(Resource location, boolean usesPathPattern) {
		if (location instanceof UrlResource) {
			// 如果位置资源是UrlResource
			if (usesPathPattern) {
				// 如果 使用路径模式 ，返回false
				return false;
			}
			if (this.urlPathHelper != null && this.urlPathHelper.isUrlDecode()) {
				// 如果URL路径助手存在，且 需要进行URL解码，则返回true
				return true;
			}
		}
		// 否则返回false
		return false;
	}

	private boolean isInvalidEncodedPath(String resourcePath) {
		if (resourcePath.contains("%")) {
			// 如果资源路径包含百分号，则尝试使用URLDecoder（而不是UriUtils）以保留可能已解码的UTF-8字符...
			try {
				// 解码资源路径
				String decodedPath = URLDecoder.decode(resourcePath, "UTF-8");
				// 检查解码后的路径中是否包含"../"或"..\\"
				if (decodedPath.contains("../") || decodedPath.contains("..\\")) {
					// 如果包含则记录警告信息并返回true
					logger.warn(LogFormatUtils.formatValue(
							"Resolved resource path contains encoded \"../\" or \"..\\\": " + resourcePath, -1, true));
					return true;
				}
			} catch (IllegalArgumentException ex) {
				// 可能无法解码...
			} catch (UnsupportedEncodingException ex) {
				// 不应该发生...
			}
		}
		// 否则返回false
		return false;
	}

}
