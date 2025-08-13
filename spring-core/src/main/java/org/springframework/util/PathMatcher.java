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

import java.util.Comparator;
import java.util.Map;

/**
 * 基于字符串的路径匹配策略接口。
 *
 * <p>被以下类使用：
 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}、
 * {@link org.springframework.web.servlet.handler.AbstractUrlHandlerMapping}、
 * 以及{@link org.springframework.web.servlet.mvc.WebContentInterceptor}。
 *
 * <p>默认实现是{@link AntPathMatcher}，支持Ant风格的模式语法。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see AntPathMatcher
 */
public interface PathMatcher {

	/**
	 * 判断给定的{@code path}是否表示可由本接口实现匹配的模式。
	 * <p>如果返回值为{@code false}，则不需要使用{@link #match}方法，
	 * 因为对静态路径字符串的直接相等比较会得出相同结果。
	 * @param path 要检查的路径
	 * @return 如果给定的{@code path}表示一个模式则返回{@code true}
	 */
	boolean isPattern(String path);

	/**
	 * 根据此PathMatcher的匹配策略，将给定{@code path}与给定{@code pattern}进行匹配。
	 * @param pattern 要匹配的模式
	 * @param path 要测试的路径
	 * @return 如果提供的{@code path}匹配则返回{@code true}，否则返回{@code false}
	 */
	boolean match(String pattern, String path);

	/**
	 * 根据此PathMatcher的匹配策略，将给定{@code path}与给定{@code pattern}的对应部分进行匹配。
	 * <p>确定模式至少匹配到给定基本路径的程度，假设完整路径也可能匹配。
	 * @param pattern 要匹配的模式
	 * @param path 要测试的路径
	 * @return 如果提供的{@code path}匹配则返回{@code true}，否则返回{@code false}
	 */
	boolean matchStart(String pattern, String path);

	/**
	 * 给定模式和完整路径，确定模式映射的部分。
	 * <p>此方法旨在找出路径中通过实际模式动态匹配的部分，
	 * 也就是说，它会从给定的完整路径中去掉静态定义的前导路径，
	 * 只返回路径中实际被模式匹配的部分。
	 * <p>例如：对于模式"myroot/*.html"和完整路径"myroot/myfile.html"，
	 * 此方法应返回"myfile.html"。具体的确定规则取决于此PathMatcher的匹配策略。
	 * <p>简单实现可能在遇到实际模式时原样返回给定完整路径，
	 * 而在模式不包含任何动态部分时返回空字符串（即{@code pattern}参数是
	 * 不符合{@link #isPattern 模式}条件的静态路径）。
	 * 复杂的实现会区分给定路径模式的静态部分和动态部分。
	 * @param pattern 路径模式
	 * @param path 要检查的完整路径
	 * @return 给定{@code path}中模式映射的部分（永不返回{@code null}）
	 */
	String extractPathWithinPattern(String pattern, String path);

	/**
	 * 根据给定的路径模式和完整路径，提取URI模板变量。URI模板
	 * 变量通过大括号('{'和'}')表示。
	 * <p>例如：对于模式"/hotels/{hotel}"和路径"/hotels/1"，该方法将
	 * 返回一个包含"hotel"→"1"的映射。
	 * @param pattern 路径模式，可能包含URI模板
	 * @param path 要从中提取模板变量的完整路径
	 * @return 一个映射，包含变量名作为键，变量值作为值
	 */
	Map<String, String> extractUriTemplateVariables(String pattern, String path);

	/**
	 * 给定一个完整路径，返回适合按该路径的明确性排序模式的{@link Comparator}。
	 * <p>使用的完整算法取决于底层实现，
	 * 但通常，返回的{@code Comparator}将
	 * {@linkplain java.util.List#sort(java.util.Comparator) 排序}
	 * 一个列表，使更具体的模式排在通用模式之前。
	 * @param path 用于比较的完整路径
	 * @return 能够按明确性排序模式的比较器
	 */
	Comparator<String> getPatternComparator(String path);

	/**
	 * 将两个模式组合成一个返回的新模式。
	 * <p>用于组合两个模式的完整算法取决于底层实现。
	 * @param pattern1 第一个模式
	 * @param pattern2 第二个模式
	 * @return 两个模式的组合
	 * @throws IllegalArgumentException 当两个模式无法组合时抛出
	 */
	String combine(String pattern1, String pattern2);

}
