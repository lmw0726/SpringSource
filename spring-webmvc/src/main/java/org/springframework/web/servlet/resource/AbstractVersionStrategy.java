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

package org.springframework.web.servlet.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link VersionStrategy}实现的抽象基类。
 *
 * <p>支持以下版本格式：
 * <ul>
 * <li>请求路径中的前缀，如"version/static/myresource.js"
 * <li>请求路径中的文件名后缀，如"static/myresource-version.js"
 * </ul>
 *
 * <p>注意：此基类不提供生成版本字符串的支持。
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public abstract class AbstractVersionStrategy implements VersionStrategy {

	/**
	 * 日志记录器
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 版本路径策略
	 */
	private final VersionPathStrategy pathStrategy;


	protected AbstractVersionStrategy(VersionPathStrategy pathStrategy) {
		Assert.notNull(pathStrategy, "VersionPathStrategy is required");
		this.pathStrategy = pathStrategy;
	}


	public VersionPathStrategy getVersionPathStrategy() {
		return this.pathStrategy;
	}


	@Override
	@Nullable
	public String extractVersion(String requestPath) {
		return this.pathStrategy.extractVersion(requestPath);
	}

	@Override
	public String removeVersion(String requestPath, String version) {
		return this.pathStrategy.removeVersion(requestPath, version);
	}

	@Override
	public String addVersion(String requestPath, String version) {
		return this.pathStrategy.addVersion(requestPath, version);
	}


	/**
	 * 基于前缀的 {@code VersionPathStrategy}，例如 {@code "{version}/path/foo.js"}。
	 */
	protected static class PrefixVersionPathStrategy implements VersionPathStrategy {
		/**
		 * 前缀
		 */
		private final String prefix;

		public PrefixVersionPathStrategy(String version) {
			Assert.hasText(version, "Version must not be empty");
			this.prefix = version;
		}

		@Override
		@Nullable
		public String extractVersion(String requestPath) {
			return (requestPath.startsWith(this.prefix) ? this.prefix : null);
		}

		@Override
		public String removeVersion(String requestPath, String version) {
			return requestPath.substring(this.prefix.length());
		}

		@Override
		public String addVersion(String path, String version) {
			if (path.startsWith(".")) {
				// 如果路径以 . 开头，直接返回当前路径
				return path;
			} else {
				// 如果前缀以斜杠结尾，或者路径以斜杠开头，则直接连接前缀和路径
				// 否则，在前缀和路径之间添加斜杠后再连接
				return (this.prefix.endsWith("/") || path.startsWith("/") ?
						this.prefix + path : this.prefix + '/' + path);
			}
		}
	}


	/**
	 * 基于文件名的 {@code VersionPathStrategy}，例如 {@code "path/foo-{version}.css"}。
	 */
	protected static class FileNameVersionPathStrategy implements VersionPathStrategy {

		/**
		 * 匹配 -加字母和.的字符串
		 */
		private static final Pattern pattern = Pattern.compile("-(\\S*)\\.");

		@Override
		@Nullable
		public String extractVersion(String requestPath) {
			Matcher matcher = pattern.matcher(requestPath);
			if (matcher.find()) {
				// 如果匹配到了字符串
				String match = matcher.group(1);
				// 如果匹配到的字符串含有-，则截取- 后的字符串
				return (match.contains("-") ? match.substring(match.lastIndexOf('-') + 1) : match);
			} else {
				return null;
			}
		}

		@Override
		public String removeVersion(String requestPath, String version) {
			return StringUtils.delete(requestPath, "-" + version);
		}

		@Override
		public String addVersion(String requestPath, String version) {
			// 获取不带扩展名的文件名
			String baseFilename = StringUtils.stripFilenameExtension(requestPath);
			// 获取文件扩展名
			String extension = StringUtils.getFilenameExtension(requestPath);
			// 将文件名、版本号和扩展名拼接成新的文件名
			return (baseFilename + '-' + version + '.' + extension);
		}
	}

}
