/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.util.pattern;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.RouteMatcher;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 {@link PathPatternParser} 构建的 {@code RouteMatcher}，使用 {@link PathContainer} 和 {@link PathPattern}
 * 作为路由和模式的解析表示。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class PathPatternRouteMatcher implements RouteMatcher {

	/**
	 * 用于解析路径模式的解析器。
	 */
	private final PathPatternParser parser;

	/**
	 * 缓存路径模式的映射。键为路径模式字符串，值为对应的 PathPattern 实例。
	 */
	private final Map<String, PathPattern> pathPatternCache = new ConcurrentHashMap<>();


	/**
	 * 默认构造函数，使用自定义的 {@link PathPatternParser} 用于 {@link org.springframework.http.server.PathContainer.Options#MESSAGE_ROUTE}
	 * 并且不匹配尾部分隔符。
	 */
	public PathPatternRouteMatcher() {
		// 创建一个 PathPatternParser 对象
		this.parser = new PathPatternParser();
		// 设置路径选项为 消息路由 模式
		this.parser.setPathOptions(PathContainer.Options.MESSAGE_ROUTE);
		// 设置是否匹配可选的尾部分隔符为 false
		this.parser.setMatchOptionalTrailingSeparator(false);
	}

	/**
	 * 使用给定的 {@link PathPatternParser} 构造函数。
	 */
	public PathPatternRouteMatcher(PathPatternParser parser) {
		Assert.notNull(parser, "PathPatternParser must not be null");
		this.parser = parser;
	}


	@Override
	public Route parseRoute(String routeValue) {
		return new PathContainerRoute(PathContainer.parsePath(routeValue, this.parser.getPathOptions()));
	}

	@Override
	public boolean isPattern(String route) {
		return getPathPattern(route).hasPatternSyntax();
	}

	@Override
	public String combine(String pattern1, String pattern2) {
		return getPathPattern(pattern1).combine(getPathPattern(pattern2)).getPatternString();
	}

	@Override
	public boolean match(String pattern, Route route) {
		return getPathPattern(pattern).matches(getPathContainer(route));
	}

	@Override
	@Nullable
	public Map<String, String> matchAndExtract(String pattern, Route route) {
		// 获取路径模式的 PathPattern 对象，然后对路由的路径进行匹配和提取
		PathPattern.PathMatchInfo info = getPathPattern(pattern).matchAndExtract(getPathContainer(route));
		// 如果匹配信息不为空，则返回匹配的URI变量；否则返回 null
		return info != null ? info.getUriVariables() : null;
	}

	@Override
	public Comparator<String> getPatternComparator(Route route) {
		return Comparator.comparing(this::getPathPattern);
	}

	private PathPattern getPathPattern(String pattern) {
		return this.pathPatternCache.computeIfAbsent(pattern, this.parser::parse);
	}

	private PathContainer getPathContainer(Route route) {
		Assert.isInstanceOf(PathContainerRoute.class, route);
		return ((PathContainerRoute) route).pathContainer;
	}


	private static class PathContainerRoute implements Route {
		/**
		 * 路径容器
		 */
		private final PathContainer pathContainer;


		PathContainerRoute(PathContainer pathContainer) {
			this.pathContainer = pathContainer;
		}


		@Override
		public String value() {
			return this.pathContainer.value();
		}


		@Override
		public String toString() {
			return value();
		}
	}

}
