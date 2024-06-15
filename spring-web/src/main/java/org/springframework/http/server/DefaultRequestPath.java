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

package org.springframework.http.server;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * {@link RequestPath}的默认实现
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultRequestPath implements RequestPath {
	/**
	 * 储存全路径的路径容器
	 */
	private final PathContainer fullPath;

	/**
	 * 储存上下文路径的路径容器
	 */
	private final PathContainer contextPath;

	/**
	 * 储存应用内路径的路径容器
	 */
	private final PathContainer pathWithinApplication;


	DefaultRequestPath(String rawPath, @Nullable String contextPath) {
		// 解析原始路径并存储到 fullPath 变量中
		this.fullPath = PathContainer.parsePath(rawPath);

		// 初始化上下文路径，并存储到 contextPath 变量中
		this.contextPath = initContextPath(this.fullPath, contextPath);

		// 提取应用程序内部路径，并存储到 pathWithinApplication 变量中
		this.pathWithinApplication = extractPathWithinApplication(this.fullPath, this.contextPath);
	}

	private DefaultRequestPath(RequestPath requestPath, String contextPath) {
		this.fullPath = requestPath;
		// 初始化上下文路径，并存储到 contextPath 变量中
		this.contextPath = initContextPath(this.fullPath, contextPath);

		// 提取应用程序内部路径，并存储到 pathWithinApplication 变量中
		this.pathWithinApplication = extractPathWithinApplication(this.fullPath, this.contextPath);
	}

	private static PathContainer initContextPath(PathContainer path, @Nullable String contextPath) {
		// 如果上下文路径为空或者只包含一个斜杠，则返回一个空的路径容器
		if (!StringUtils.hasText(contextPath) || StringUtils.matchesCharacter(contextPath, '/')) {
			return PathContainer.parsePath("");
		}

		// 验证上下文路径是否有效，可能会根据业务逻辑实现
		validateContextPath(path.value(), contextPath);

		// 初始化变量
		int length = contextPath.length();
		int counter = 0;

		// 遍历路径元素
		for (int i = 0; i < path.elements().size(); i++) {
			PathContainer.Element element = path.elements().get(i);
			counter += element.value().length();
			// 如果累计长度等于上下文路径的长度，则返回路径的子路径
			if (length == counter) {
				return path.subPath(0, i + 1);
			}
		}

		// 如果未能成功初始化上下文路径，抛出异常
		throw new IllegalStateException("Failed to initialize contextPath '" + contextPath + "'" +
				" for requestPath '" + path.value() + "'");
	}

	private static void validateContextPath(String fullPath, String contextPath) {
		// 获取上下文路径的长度
		int length = contextPath.length();

		if (contextPath.charAt(0) != '/' || contextPath.charAt(length - 1) == '/') {
			// 如果上下文路径不是以 '/' 开始或者以 '/' 结束，则抛出异常
			throw new IllegalArgumentException("Invalid contextPath: '" + contextPath + "': " +
					"must start with '/' and not end with '/'");
		}

		if (!fullPath.startsWith(contextPath)) {
			// 全路径不以上下文路径开头，抛出异常
			throw new IllegalArgumentException("Invalid contextPath '" + contextPath + "': " +
					"must match the start of requestPath: '" + fullPath + "'");
		}

		// 检查完整路径长度大于上下文路径长度时，检查完整路径第一个字符是否为 '/'
		if (fullPath.length() > length && fullPath.charAt(length) != '/') {
			// 如果全路径长度大于上下文长度，
			// 并且全路径中，与上下文路径最后一个位置同一位置的字符，不是‘/’，
			// 抛出异常
			throw new IllegalArgumentException("Invalid contextPath '" + contextPath + "': " +
					"must match to full path segments for requestPath: '" + fullPath + "'");
		}
	}

	private static PathContainer extractPathWithinApplication(PathContainer fullPath, PathContainer contextPath) {
		return fullPath.subPath(contextPath.elements().size());
	}


	// 路径容器 方法

	@Override
	public String value() {
		return this.fullPath.value();
	}

	@Override
	public List<Element> elements() {
		return this.fullPath.elements();
	}


	// 请求路径 方法

	@Override
	public PathContainer contextPath() {
		return this.contextPath;
	}

	@Override
	public PathContainer pathWithinApplication() {
		return this.pathWithinApplication;
	}

	@Override
	public RequestPath modifyContextPath(String contextPath) {
		return new DefaultRequestPath(this, contextPath);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		DefaultRequestPath otherPath = (DefaultRequestPath) other;
		return (this.fullPath.equals(otherPath.fullPath) &&
				this.contextPath.equals(otherPath.contextPath) &&
				this.pathWithinApplication.equals(otherPath.pathWithinApplication));
	}

	@Override
	public int hashCode() {
		int result = this.fullPath.hashCode();
		result = 31 * result + this.contextPath.hashCode();
		result = 31 * result + this.pathWithinApplication.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return this.fullPath.toString();
	}

}
