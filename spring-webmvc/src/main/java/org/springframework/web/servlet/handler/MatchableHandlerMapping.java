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

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.http.HttpServletRequest;

/**
 * {@link HandlerMapping} 可以实现的附加接口，以公开与其内部请求匹配配置和实现对齐的请求匹配 API。
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 * @see HandlerMappingIntrospector
 */
public interface MatchableHandlerMapping extends HandlerMapping {

	/**
	 * 返回此 {@code HandlerMapping} 的解析器，如果已配置，则使用预解析的模式。
	 * @since 5.3
	 */
	@Nullable
	default PathPatternParser getPatternParser() {
		return null;
	}

	/**
	 * 确定请求是否与给定模式匹配。当 {@link #getPatternParser()} 返回 {@code null} 时使用此方法，
	 * 这意味着 {@code HandlerMapping} 正在使用字符串模式匹配。
	 * @param request 当前请求
	 * @param pattern 要匹配的模式
	 * @return 请求匹配的结果，如果没有则返回 {@code null}
	 */
	@Nullable
	RequestMatchResult match(HttpServletRequest request, String pattern);

}
