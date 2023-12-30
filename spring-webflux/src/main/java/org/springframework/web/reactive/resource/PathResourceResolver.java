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

package org.springframework.web.reactive.resource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 一个简单的{@code ResourceResolver}，尝试在给定的位置中查找与请求路径匹配的资源。
 *
 * <p>此解析器不委托给{@code ResourceResolverChain}，预期在解析器链的末尾进行配置。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class PathResourceResolver extends AbstractResourceResolver {

	/**
	 * 允许的资源位置列表
	 */
	@Nullable
	private Resource[] allowedLocations;


	/**
	 * 默认情况下，当找到资源时，将比较解析后的资源路径，以确保它在找到资源的输入位置下。
	 * 但有时可能不是这种情况，例如当{@link CssLinkResourceTransformer}解析其包含的链接的公共URL时，
	 * CSS文件是位置，正在解析的资源是css文件、图像、字体和其他位于相邻或父目录中的资源。
	 * <p>此属性允许配置完整的位置列表，以便如果资源不在与其相对找到位置下的位置，则也可以检查此列表。
	 * <p>默认情况下，{@link ResourceWebHandler}将此属性初始化为与其位置列表匹配。
	 *
	 * @param locations 允许的位置列表
	 */
	public void setAllowedLocations(@Nullable Resource... locations) {
		this.allowedLocations = locations;
	}

	/**
	 * 获取允许的资源位置列表。
	 *
	 * @return 允许的资源位置列表
	 */
	@Nullable
	public Resource[] getAllowedLocations() {
		return this.allowedLocations;
	}

	/**
	 * 解析资源路径。
	 *
	 * @param exchange     可能的服务器网络交换
	 * @param requestPath  请求的路径
	 * @param locations    资源的位置列表
	 * @param chain        资源解析链
	 * @return 包含解析的资源的{@link Mono}
	 */

	@Override
	protected Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
													 String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		return getResource(requestPath, locations);
	}

	/**
	 * 在给定位置下解析URL路径。
	 *
	 * @param path      要解析的路径
	 * @param locations 资源的位置列表
	 * @param chain     资源解析链
	 * @return 包含解析路径的{@link Mono}
	 */
	@Override
	protected Mono<String> resolveUrlPathInternal(String path, List<? extends Resource> locations,
												  ResourceResolverChain chain) {

		if (StringUtils.hasText(path)) {
			return getResource(path, locations).map(resource -> path);
		} else {
			return Mono.empty();
		}
	}

	/**
	 * 获取给定路径下的资源。
	 *
	 * @param resourcePath 资源路径
	 * @param locations    资源的位置列表
	 * @return 包含资源的{@link Mono}
	 */
	private Mono<Resource> getResource(String resourcePath, List<? extends Resource> locations) {
		return Flux.fromIterable(locations)
				.concatMap(location -> getResource(resourcePath, location))
				.next();
	}

	/**
	 * 查找给定位置下的资源。
	 * <p>默认实现检查是否存在相对于位置的给定路径的可读{@code Resource}。
	 *
	 * @param resourcePath 资源的路径
	 * @param location     要检查的位置
	 * @return 资源，如果找不到则为空{@link Mono}
	 */
	protected Mono<Resource> getResource(String resourcePath, Resource location) {
		try {
			// 如果位置不是UrlResource类型，解码资源路径
			if (!(location instanceof UrlResource)) {
				resourcePath = UriUtils.decode(resourcePath, StandardCharsets.UTF_8);
			}
			// 创建相对于位置的资源
			Resource resource = location.createRelative(resourcePath);
			if (resource.isReadable()) {
				// 检查资源是否在当前位置或允许的位置下
				if (checkResource(resource, location)) {
					// 返回可读的资源
					return Mono.just(resource);
				} else if (logger.isWarnEnabled()) {
					// 资源在当前位置或允许位置下都未找到的警告信息
					Resource[] allowed = getAllowedLocations();
					logger.warn(LogFormatUtils.formatValue(
							"Resource path \"" + resourcePath + "\" was successfully resolved " +
									"but resource \"" + resource.getURL() + "\" is neither under the " +
									"current location \"" + location.getURL() + "\" nor under any of the " +
									"allowed locations " + (allowed != null ? Arrays.asList(allowed) : "[]"), -1, true));
				}
			}
			// 未找到可读资源，返回空
			return Mono.empty();
		} catch (IOException ex) {
			// 处理IO异常
			if (logger.isDebugEnabled()) {
				String error = "Skip location [" + location + "] due to error";
				if (logger.isTraceEnabled()) {
					logger.trace(error, ex);
				} else {
					logger.debug(error + ": " + ex.getMessage());
				}
			}
			// 返回IO异常
			return Mono.error(ex);
		}
	}

	/**
	 * 对已解析的资源执行额外的检查，超出检查资源是否存在和可读的范围。
	 * 默认实现还验证资源是在找到资源的位置下，或者在{@link #setAllowedLocations allowed locations}之一下。
	 *
	 * @param resource 解析的资源
	 * @param location 找到资源的相对位置
	 * @return 如果资源位于有效位置，则为“true”，否则为“false”。
	 */
	protected boolean checkResource(Resource resource, Resource location) throws IOException {
		// 检查资源是否在指定位置下
		if (isResourceUnderLocation(resource, location)) {
			return true;
		}

		// 如果允许的位置不为空，则检查资源是否在其中任何一个位置下
		if (getAllowedLocations() != null) {
			for (Resource current : getAllowedLocations()) {
				if (isResourceUnderLocation(resource, current)) {
					return true;
				}
			}
		}

		// 资源不在任何允许的位置下
		return false;
	}

	/**
	 * 检查资源是否在指定位置下。
	 *
	 * @param resource 待检查的资源
	 * @param location 找到资源的相对位置
	 * @return 如果资源在有效位置下，则为“true”，否则为“false”。
	 * @throws IOException 当出现IO错误时
	 */
	private boolean isResourceUnderLocation(Resource resource, Resource location) throws IOException {
		// 检查资源和位置是否属于相同的类
		if (resource.getClass() != location.getClass()) {
			return false;
		}

		String resourcePath;
		String locationPath;

		// 根据资源类型获取相应的路径
		if (resource instanceof UrlResource) {
			resourcePath = resource.getURL().toExternalForm();
			locationPath = StringUtils.cleanPath(location.getURL().toString());
		} else if (resource instanceof ClassPathResource) {
			resourcePath = ((ClassPathResource) resource).getPath();
			locationPath = StringUtils.cleanPath(((ClassPathResource) location).getPath());
		} else {
			resourcePath = resource.getURL().getPath();
			locationPath = StringUtils.cleanPath(location.getURL().getPath());
		}

		// 检查路径是否相等
		if (locationPath.equals(resourcePath)) {
			return true;
		}

		// 确保locationPath以斜杠结尾，并且resourcePath以locationPath开头且不包含无效的编码路径
		locationPath = (locationPath.endsWith("/") || locationPath.isEmpty() ? locationPath : locationPath + "/");
		return (resourcePath.startsWith(locationPath) && !isInvalidEncodedPath(resourcePath));
	}


	/**
	 * 检查资源路径是否包含无效的编码。如果资源路径包含“%”字符，
	 * 则尝试使用URLDecoder（而不是UriUtils）解码UTF-8字符...
	 *
	 * @param resourcePath 资源路径
	 * @return 如果解码的路径包含“../”或“..\\”则为“true”，否则为“false”。
	 */
	private boolean isInvalidEncodedPath(String resourcePath) {
		if (resourcePath.contains("%")) {
			// 使用URLDecoder (vs UriUtils) 保留潜在解码的UTF-8字符...
			try {
				String decodedPath = URLDecoder.decode(resourcePath, "UTF-8");
				// 检查解码后的路径是否包含"../"或"..\\"
				if (decodedPath.contains("../") || decodedPath.contains("..\\")) {
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
		return false;
	}

}
