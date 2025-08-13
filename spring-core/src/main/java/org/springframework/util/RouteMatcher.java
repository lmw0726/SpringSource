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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.util.Comparator;
import java.util.Map;

/**
 * 路由匹配模式的契约接口。
 *
 * <p>相当于 {@link PathMatcher}，但为了效率原因，支持使用解析后的路由和模式表示，
 * 适用于来自传入消息的路由持续匹配大量消息处理器模式的场景。
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 * @see PathMatcher
 */
public interface RouteMatcher {

	/**
	 * 返回给定路由的解析表示。
	 * @param routeValue 要解析的路由
	 * @return 路由的解析表示
	 */
	Route parseRoute(String routeValue);

	/**
	 * 判断给定的 {@code route} 是否包含需要使用 {@link #match(String, Route)} 方法的模式语法，
	 * 或者它是一个可直接比较的普通字符串。
	 * @param route 要检查的路由
	 * @return 如果给定的 {@code route} 表示一个模式，则返回 {@code true}
	 */
	boolean isPattern(String route);

	/**
	 * 将两个模式合并成一个单一模式。
	 * @param pattern1 第一个模式
	 * @param pattern2 第二个模式
	 * @return 两个模式的合并结果
	 * @throws IllegalArgumentException 当两个模式无法合并时抛出
	 */
	String combine(String pattern1, String pattern2);

	/**
	 * 将给定的路由与指定的模式进行匹配。
	 * @param pattern 要尝试匹配的模式
	 * @param route 要测试的路由
	 * @return 如果匹配成功返回 {@code true}，否则返回 {@code false}
	 */
	boolean match(String pattern, Route route);

	/**
	 * 将模式与路由匹配并提取模板变量。
	 * @param pattern 可能包含模板变量的模式
	 * @param route 用于提取模板变量的路由
	 * @return 包含模板变量及其对应值的映射
	 */
	@Nullable
	Map<String, String> matchAndExtract(String pattern, Route route);

	/**
	 * 根据给定路由，返回一个 {@link Comparator}，用于对模式进行显式性排序，
	 * 使得更具体的模式排在更通用的模式之前。
	 * @param route 用于比较的完整路径
	 * @return 能够按显式性排序模式的比较器
	 */
	Comparator<String> getPatternComparator(Route route);


	/**
	 * 路由的解析表示。
	 */
	interface Route {

		/**
		 * 原始路由值。
		 */
		String value();
	}

}
