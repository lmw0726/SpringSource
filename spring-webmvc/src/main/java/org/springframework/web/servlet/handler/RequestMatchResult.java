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

package org.springframework.web.servlet.handler;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.PathMatcher;
import org.springframework.web.util.pattern.PathPattern;

import java.util.Map;

/**
 * 包含通过{@link MatchableHandlerMapping}进行请求模式匹配的结果的容器，以及进一步从模式中提取URI模板变量的方法。
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 */
public class RequestMatchResult {

	/**
	 * 匹配的{@code PathPattern}。
	 */
	@Nullable
	private final PathPattern pathPattern;

	/**
	 * 查找路径的容器。
	 */
	@Nullable
	private final PathContainer lookupPathContainer;

	/**
	 * 匹配的模式。
	 */
	@Nullable
	private final String pattern;

	/**
	 * 查找路径。
	 */
	@Nullable
	private final String lookupPath;

	/**
	 * 用于匹配的PathMatcher实例。
	 */
	@Nullable
	private final PathMatcher pathMatcher;


	/**
	 * 使用匹配的{@code PathPattern}创建一个实例。
	 *
	 * @param pathPattern 匹配的模式
	 * @param lookupPath  映射路径
	 * @since 5.3
	 */
	public RequestMatchResult(PathPattern pathPattern, PathContainer lookupPath) {
		Assert.notNull(pathPattern, "PathPattern is required");
		Assert.notNull(pathPattern, "PathContainer is required");

		this.pattern = null;
		this.lookupPath = null;
		this.pathMatcher = null;

		this.pathPattern = pathPattern;
		this.lookupPathContainer = lookupPath;

	}

	/**
	 * 使用匹配的String模式创建一个实例。
	 *
	 * @param pattern     匹配的模式，可能附带'/'后缀
	 * @param lookupPath  映射路径
	 * @param pathMatcher 用于匹配的PathMatcher实例
	 */
	public RequestMatchResult(String pattern, String lookupPath, PathMatcher pathMatcher) {
		Assert.hasText(pattern, "'matchingPattern' is required");
		Assert.hasText(lookupPath, "'lookupPath' is required");
		Assert.notNull(pathMatcher, "PathMatcher is required");

		this.pattern = pattern;
		this.lookupPath = lookupPath;
		this.pathMatcher = pathMatcher;

		this.pathPattern = null;
		this.lookupPathContainer = null;
	}

	/**
	 * 从匹配模式中提取URI模板变量，如{@link PathMatcher#extractUriTemplateVariables}中所定义的。
	 *
	 * @return 包含URI模板变量的映射
	 */
	@SuppressWarnings("ConstantConditions")
	public Map<String, String> extractUriTemplateVariables() {
		// 如果路径模式不为空，则使用路径模式匹配并提取URI变量
		// 否则，使用路径匹配器提取URI模板变量
		return (this.pathPattern != null ?
				this.pathPattern.matchAndExtract(this.lookupPathContainer).getUriVariables() :
				this.pathMatcher.extractUriTemplateVariables(this.pattern, this.lookupPath));
	}
}
