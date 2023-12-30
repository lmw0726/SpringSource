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

package org.springframework.web.reactive.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * {@code AbstractPrefixVersionStrategy} 是一个抽象基类，用于插入前缀到 URL 路径中的 {@link VersionStrategy} 实现，
 * 例如 "version/static/myresource.js"。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractPrefixVersionStrategy implements VersionStrategy {

	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 版本前缀
	 */
	private final String prefix;

	/**
	 * 构造一个 AbstractPrefixVersionStrategy 实例。
	 *
	 * @param version 版本信息，不能为空
	 */
	protected AbstractPrefixVersionStrategy(String version) {
		Assert.hasText(version, "Version must not be empty");
		this.prefix = version;
	}

	/**
	 * 从请求路径中提取版本信息。
	 *
	 * @param requestPath 请求路径
	 * @return 如果请求路径以此前缀开头，则返回前缀；否则返回 null
	 */
	@Override
	public String extractVersion(String requestPath) {
		return (requestPath.startsWith(this.prefix) ? this.prefix : null);
	}

	/**
	 * 移除路径中的版本信息。
	 *
	 * @param requestPath 请求路径
	 * @param version     要移除的版本信息
	 * @return 移除版本信息后的路径
	 */
	@Override
	public String removeVersion(String requestPath, String version) {
		return requestPath.substring(this.prefix.length());
	}

	/**
	 * 向路径中添加版本信息。
	 *
	 * @param path    要添加版本信息的路径
	 * @param version 要添加的版本信息
	 * @return 添加了版本信息后的路径
	 */
	@Override
	public String addVersion(String path, String version) {
		if (path.startsWith(".")) {
			return path;
		} else if (this.prefix.endsWith("/") || path.startsWith("/")) {
			return this.prefix + path;
		} else {
			return this.prefix + '/' + path;
		}
	}

}
