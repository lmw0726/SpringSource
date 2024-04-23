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

package org.springframework.web.servlet.handler;

import org.springframework.http.server.PathContainer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 使用 {@link PathPatternParser} 配置的 {@link MatchableHandlerMapping} 进行包装，
 * 以便延迟解析模式并缓存它们以供重新使用。
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
class PathPatternMatchableHandlerMapping implements MatchableHandlerMapping {

	/**
	 * 最大的模式数量
	 */
	private static final int MAX_PATTERNS = 1024;

	/**
	 * 可匹配的处理映射
	 */
	private final MatchableHandlerMapping delegate;
	/**
	 * 路径模式解析器
	 */
	private final PathPatternParser parser;

	/**
	 * 路径模式缓存
	 */
	private final Map<String, PathPattern> pathPatternCache = new ConcurrentHashMap<>();


	public PathPatternMatchableHandlerMapping(MatchableHandlerMapping delegate) {
		Assert.notNull(delegate, "Delegate MatchableHandlerMapping is required.");
		Assert.notNull(delegate.getPatternParser(), "PatternParser is required.");
		this.delegate = delegate;
		this.parser = delegate.getPatternParser();
	}

	@Nullable
	@Override
	public RequestMatchResult match(HttpServletRequest request, String pattern) {
		// 根据模式从路径模式缓存中获取路径模式，如果不存在则进行解析并添加到缓存中
		PathPattern pathPattern = this.pathPatternCache.computeIfAbsent(pattern, value -> {
			// 检查模式缓存的大小是否超出了最大限制
			Assert.isTrue(this.pathPatternCache.size() < MAX_PATTERNS, "Max size for pattern cache exceeded.");
			// 解析路径模式
			return this.parser.parse(pattern);
		});
		// 获取请求的解析后路径
		PathContainer path = ServletRequestPathUtils.getParsedRequestPath(request).pathWithinApplication();
		// 如果路径模式匹配请求路径，则返回请求匹配结果对象，否则返回 null
		return (pathPattern.matches(path) ? new RequestMatchResult(pathPattern, path) : null);
	}

	@Nullable
	@Override
	public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		return this.delegate.getHandler(request);
	}

}
