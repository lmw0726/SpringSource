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

package org.springframework.web.servlet.function;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.server.PathContainer;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

/**
 * {@link RouterFunctions#resources(String, Resource)} 使用的查找函数。
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
class PathResourceLookupFunction implements Function<ServerRequest, Optional<Resource>> {
	/**
	 * 路径模式
	 */
	private final PathPattern pattern;

	/**
	 * 位置资源
	 */
	private final Resource location;


	public PathResourceLookupFunction(String pattern, Resource location) {
		Assert.hasLength(pattern, "'pattern' must not be empty");
		Assert.notNull(location, "'location' must not be null");
		this.pattern = PathPatternParser.defaultInstance.parse(pattern);
		this.location = location;
	}


	@Override
	public Optional<Resource> apply(ServerRequest request) {
		// 从请求中获取请求路径
		PathContainer pathContainer = request.requestPath().pathWithinApplication();
		// 如果请求路径与模式不匹配，则返回空Optional
		if (!this.pattern.matches(pathContainer)) {
			return Optional.empty();
		}

		// 从模式中提取请求路径
		pathContainer = this.pattern.extractPathWithinPattern(pathContainer);
		// 处理路径，例如解码URL编码
		String path = processPath(pathContainer.value());
		// 如果路径包含%字符，则解码路径中的百分号
		if (path.contains("%")) {
			path = StringUtils.uriDecode(path, StandardCharsets.UTF_8);
		}
		// 如果路径为空或者是无效路径，则返回空Optional
		if (!StringUtils.hasLength(path) || isInvalidPath(path)) {
			return Optional.empty();
		}

		try {
			// 根据路径创建资源
			Resource resource = this.location.createRelative(path);
			// 如果资源可读且在指定的位置下，则返回包含该资源的Optional
			if (resource.isReadable() && isResourceUnderLocation(resource)) {
				return Optional.of(resource);
			} else {
				return Optional.empty();
			}
		} catch (IOException ex) {
			// 如果发生IO异常，则抛出UncheckedIOException
			throw new UncheckedIOException(ex);
		}
	}

	private String processPath(String path) {
		// 初始化slash标志为false，表示路径是否以斜杠开头
		boolean slash = false;
		// 遍历路径中的字符
		for (int i = 0; i < path.length(); i++) {
			// 如果当前字符是斜杠，则将slash标志设置为true
			if (path.charAt(i) == '/') {
				slash = true;
			} else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
				// 如果当前字符不是空格且不是删除字符，则判断当前字符是否位于路径开头
				if (i == 0 || (i == 1 && slash)) {
					// 如果当前字符位于路径开头，则直接返回路径
					return path;
				}
				// 如果当前字符不是位于路径开头，则截取路径，保留当前字符及其后面的字符
				path = slash ? "/" + path.substring(i) : path.substring(i);
				return path;
			}
		}
		// 如果路径只包含空格或删除字符，则返回空字符串或者斜杠（取决于slash标志）
		return (slash ? "/" : "");
	}

	private boolean isInvalidPath(String path) {
		// 如果路径包含"WEB-INF"或"META-INF"，则认为路径无效，返回true
		if (path.contains("WEB-INF") || path.contains("META-INF")) {
			return true;
		}
		if (path.contains(":/")) {
			// 如果路径包含":/"，获取相对路径。
			String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
			if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
				// 则判断路径是否是URL或者以"url:"开头，若是则认为路径无效，返回true
				return true;
			}
		}
		// 如果路径包含".."，并且经过清理后的路径中包含"../"，则认为路径无效，返回true
		return path.contains("..") && StringUtils.cleanPath(path).contains("../");
	}

	private boolean isResourceUnderLocation(Resource resource) throws IOException {
		// 如果资源的类与位置的类不同，则认为资源不在位置下，返回false
		if (resource.getClass() != this.location.getClass()) {
			return false;
		}

		String resourcePath;
		String locationPath;

		// 根据资源的类型获取资源路径和位置路径
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

		// 如果资源路径等于位置路径，则认为资源在位置下，返回true
		if (locationPath.equals(resourcePath)) {
			return true;
		}
		// 如果位置路径不以"/"结尾或为空，则在位置路径末尾添加"/"
		locationPath = (locationPath.endsWith("/") || locationPath.isEmpty() ? locationPath : locationPath + "/");
		// 如果资源路径不是以位置路径开头，则认为资源不在位置下，返回false
		if (!resourcePath.startsWith(locationPath)) {
			return false;
		}
		// 如果资源路径中包含"%"，并且解码后的路径中包含"../"，则认为资源不在位置下，返回false
		return !resourcePath.contains("%") ||
				!StringUtils.uriDecode(resourcePath, StandardCharsets.UTF_8).contains("../");
	}


	@Override
	public String toString() {
		return this.pattern + " -> " + this.location;
	}

}
