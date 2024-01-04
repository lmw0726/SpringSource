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

package org.springframework.web.reactive.function.server;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.server.PathContainer;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * {@code RouterFunctions#resources(String, Resource)}使用的查找函数。
 * <p>
 * 此查找函数用于在给定模式匹配时查找资源。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class PathResourceLookupFunction implements Function<ServerRequest, Mono<Resource>> {
	/**
	 * 路径模式
	 */
	private final PathPattern pattern;

	/**
	 * 资源位置
	 */
	private final Resource location;

	/**
	 * 构造函数。
	 *
	 * @param pattern  用于匹配的模式
	 * @param location 资源的位置
	 */
	public PathResourceLookupFunction(String pattern, Resource location) {
		Assert.hasLength(pattern, "'pattern' must not be empty");
		Assert.notNull(location, "'location' must not be null");
		this.pattern = PathPatternParser.defaultInstance.parse(pattern);
		this.location = location;
	}


	/**
	 * 应用资源查找功能到给定的服务器请求。
	 *
	 * @param request 服务器请求对象
	 * @return 一个包含资源的 Mono，如果资源不存在或无法读取，则为空
	 * @throws UncheckedIOException 如果发生 I/O 错误时抛出
	 */
	@Override
	public Mono<Resource> apply(ServerRequest request) {
		// 获取请求的路径并将其转换为相对于应用程序的路径
		PathContainer pathContainer = request.requestPath().pathWithinApplication();

		// 检查路径是否与指定的模式匹配，若不匹配则返回空 Mono
		if (!this.pattern.matches(pathContainer)) {
			return Mono.empty();
		}

		// 从模式中提取路径，并对其进行处理
		pathContainer = this.pattern.extractPathWithinPattern(pathContainer);
		String path = processPath(pathContainer.value());

		// 如果路径中包含百分号，则使用 UTF-8 解码
		if (path.contains("%")) {
			path = StringUtils.uriDecode(path, StandardCharsets.UTF_8);
		}

		// 如果路径为空或者不合法，则返回空 Mono
		if (!StringUtils.hasLength(path) || isInvalidPath(path)) {
			return Mono.empty();
		}

		try {
			// 根据路径创建资源对象
			Resource resource = this.location.createRelative(path);

			// 检查资源是否可读且位于特定位置下，若满足条件则返回包含资源的 Mono；否则返回空 Mono
			if (resource.isReadable() && isResourceUnderLocation(resource)) {
				return Mono.just(resource);
			} else {
				return Mono.empty();
			}
		} catch (IOException ex) {
			// 如果发生 IO 异常，则抛出 UncheckedIOException
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * 处理给定路径的格式化。移除路径前面无效的字符并确保正确的格式。
	 *
	 * @param path 要处理的路径
	 * @return 格式化后的路径
	 */
	private String processPath(String path) {
		// 初始化标记变量 slash 为 false
		boolean slash = false;

		// 遍历路径中的字符
		for (int i = 0; i < path.length(); i++) {
			// 如果字符是斜杠，则将 slash 标记为 true
			if (path.charAt(i) == '/') {
				slash = true;
			} else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
				// 如果字符是可见字符并且不是 DEL 控制字符
				if (i == 0 || (i == 1 && slash)) {
					// 如果是第一个字符或者第二个字符是斜杠（即路径的第一个非斜杠字符），则直接返回原始路径
					return path;
				}
				// 根据 slash 变量的值对路径进行处理
				path = slash ? "/" + path.substring(i) : path.substring(i);
				return path;
			}
		}

		// 如果路径仅包含空白字符或者控制字符，则根据 slash 变量返回空路径或者根路径
		return (slash ? "/" : "");
	}

	/**
	 * 检查路径是否为无效路径。判断路径中是否包含常见的安全漏洞或不安全的内容。
	 *
	 * @param path 要检查的路径
	 * @return 如果路径无效则为true，否则为false
	 */
	private boolean isInvalidPath(String path) {
		// 检查路径是否包含 "WEB-INF" 或 "META-INF" 字符串，若包含则返回 true
		if (path.contains("WEB-INF") || path.contains("META-INF")) {
			return true;
		}

		// 检查路径是否包含 ":/" 字符串，以及相对路径是否是 URL 或以 "url:" 开头，若满足条件则返回 true
		if (path.contains(":/")) {
			String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
			if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
				return true;
			}
		}

		// 检查路径是否包含 ".." 并且经过清理后仍包含 "../" 字符串，若满足条件则返回 true
		if (path.contains("..") && StringUtils.cleanPath(path).contains("../")) {
			return true;
		}

		// 如果路径不包含敏感字符或特殊字符串，则返回 false
		return false;
	}

	/**
	 * 检查资源是否在给定的位置下。检查资源的路径是否在给定位置的路径下，用于安全验证。
	 *
	 * @param resource 要检查的资源
	 * @return 如果资源在给定位置下则为true，否则为false
	 * @throws IOException 当发生I/O错误时
	 */
	private boolean isResourceUnderLocation(Resource resource) throws IOException {
		// 检查资源和位置是否是同一类型，若不是则返回 false
		if (resource.getClass() != this.location.getClass()) {
			return false;
		}

		String resourcePath;
		String locationPath;

		// 根据资源类型获取资源路径和位置路径，并对其进行清理
		if (resource instanceof UrlResource) {
			resourcePath = resource.getURL().toExternalForm();
			locationPath = StringUtils.cleanPath(this.location.getURL().toString());
		} else if (resource instanceof ClassPathResource) {
			resourcePath = ((ClassPathResource) resource).getPath();
			locationPath = StringUtils.cleanPath(((ClassPathResource) this.location).getPath());
		} else {
			resourcePath = resource.getURL().getPath();
			locationPath = StringUtils.cleanPath(this.location.getURL().getPath());
		}

		// 如果资源路径和位置路径相等，则返回 true
		if (locationPath.equals(resourcePath)) {
			return true;
		}

		// 如果资源路径以位置路径开头，或者包含 "../" 并且包含百分号，则返回 true，否则返回 false
		locationPath = (locationPath.endsWith("/") || locationPath.isEmpty() ? locationPath : locationPath + "/");
		if (!resourcePath.startsWith(locationPath)) {
			return false;
		}
		if (resourcePath.contains("%") && StringUtils.uriDecode(resourcePath, StandardCharsets.UTF_8).contains("../")) {
			return false;
		}
		return true;
	}


	@Override
	public String toString() {
		return this.pattern + " -> " + this.location;
	}

}
