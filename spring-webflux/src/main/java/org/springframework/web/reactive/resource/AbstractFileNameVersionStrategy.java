/*
 * Copyright 2002-2016 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于文件名后缀的 {@link VersionStrategy} 实现的抽象基类，例如 "static/myresource-version.js"。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @see VersionStrategy
 * @since 5.0
 */
public abstract class AbstractFileNameVersionStrategy implements VersionStrategy {

	protected final Log logger = LogFactory.getLog(getClass());

	private static final Pattern pattern = Pattern.compile("-(\\S*)\\.");

	/**
	 * 从请求路径中提取资源版本的 {@link VersionStrategy} 实现。
	 *
	 * @param requestPath 要检查的请求路径
	 * @return 版本字符串，如果未找到则为 {@code null}
	 */
	@Override
	public String extractVersion(String requestPath) {
		// 创建一个匹配器，用于从请求路径中提取版本信息
		Matcher matcher = pattern.matcher(requestPath);

		// 查找匹配的版本信息
		if (matcher.find()) {
			// 获取匹配的部分
			String match = matcher.group(1);
			// 如果匹配部分包含 "-"，则返回最后一个 "-" 后面的内容作为版本号，否则返回匹配部分作为版本号
			return (match.contains("-") ? match.substring(match.lastIndexOf('-') + 1) : match);
		} else {
			// 未找到版本号，返回 null
			return null;
		}
	}

	@Override
	public String removeVersion(String requestPath, String version) {
		return StringUtils.delete(requestPath, "-" + version);
	}

	/**
	 * 将版本添加到给定的请求路径中的 {@link VersionStrategy} 实现。
	 *
	 * @param requestPath 请求路径
	 * @param version     版本
	 * @return 带有版本字符串更新的请求路径
	 */
	@Override
	public String addVersion(String requestPath, String version) {
		// 获取不带文件扩展名的基本文件名
		String baseFilename = StringUtils.stripFilenameExtension(requestPath);
		// 获取文件扩展名
		String extension = StringUtils.getFilenameExtension(requestPath);
		// 将版本添加到基本文件名后面，然后添加文件扩展名，返回更新后的请求路径
		return (baseFilename + '-' + version + '.' + extension);
	}

}
