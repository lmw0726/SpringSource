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

package org.springframework.web.servlet.mvc.condition;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 逻辑与（{@code ' && ' }）请求条件，将请求与使用{@link RequestMapping#headers()}中定义的语法的一组标头表达式进行匹配。
 *
 * <p>通过头部名称为'Accept'或'Content-Type'的表达式传递给构造函数的表达式将被忽略。
 * 有关这些表达式，请参见{@link ConsumesRequestCondition}和{@link ProducesRequestCondition}。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class HeadersRequestCondition extends AbstractRequestCondition<HeadersRequestCondition> {

	/**
	 * 预检请求头条件匹配
	 */
	private static final HeadersRequestCondition PRE_FLIGHT_MATCH = new HeadersRequestCondition();

	/**
	 * 请求头表达式集合
	 */
	private final Set<HeaderExpression> expressions;


	/**
	 * 从给定的头部表达式创建一个新实例。带有头部名称为'Accept'或'Content-Type'的表达式将被忽略。
	 * 有关这些表达式，请参见{@link ConsumesRequestCondition}和{@link ProducesRequestCondition}。
	 *
	 * @param headers 使用{@link RequestMapping#headers()}中定义的语法的媒体类型表达式；
	 *                如果为0，条件将匹配所有请求
	 */
	public HeadersRequestCondition(String... headers) {
		this.expressions = parseExpressions(headers);
	}

	private static Set<HeaderExpression> parseExpressions(String... headers) {
		// 创建一个用于存储头部表达式的集合
		Set<HeaderExpression> result = null;
		// 如果头部数组不为空
		if (!ObjectUtils.isEmpty(headers)) {
			// 遍历头部数组
			for (String header : headers) {
				// 解析头部表达式
				HeaderExpression expr = new HeaderExpression(header);
				// 如果头部名称是 "Accept" 或 "Content-Type"，则跳过
				if ("Accept".equalsIgnoreCase(expr.name) || "Content-Type".equalsIgnoreCase(expr.name)) {
					continue;
				}
				// 如果结果集合不为空，则将当前表达式添加到集合中；
				// 如果结果集合为空，则创建一个新的集合
				result = (result != null ? result : new LinkedHashSet<>(headers.length));
				// 将当前表达式添加到其中
				result.add(expr);
			}
		}
		// 如果结果集合不为空，则返回结果集合；如果结果集合为空，则返回空集合
		return (result != null ? result : Collections.emptySet());
	}

	private HeadersRequestCondition(Set<HeaderExpression> conditions) {
		this.expressions = conditions;
	}


	/**
	 * 返回包含的请求头表达式。
	 */
	public Set<NameValueExpression<String>> getExpressions() {
		return new LinkedHashSet<>(this.expressions);
	}

	@Override
	protected Collection<HeaderExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	/**
	 * 返回包含“this”和“other”实例中的头部表达式的并集的新实例。
	 */
	@Override
	public HeadersRequestCondition combine(HeadersRequestCondition other) {
		// 如果当前对象和另一个对象的条件均为空，则返回当前对象
		if (isEmpty() && other.isEmpty()) {
			return this;
		} else if (other.isEmpty()) {
			// 如果另一个对象的条件为空，则返回当前对象
			return this;
		} else if (isEmpty()) {
			// 如果当前对象的条件为空，则返回另一个对象
			return other;
		}
		// 创建一个新的集合，将当前对象和另一个对象的条件都添加到该集合中
		Set<HeaderExpression> set = new LinkedHashSet<>(this.expressions);
		set.addAll(other.expressions);
		// 返回一个新的 HeadersRequestCondition 对象，其中包含了合并后的条件集合
		return new HeadersRequestCondition(set);
	}

	/**
	 * 如果请求匹配所有表达式，则返回“this”实例；否则返回{@code null}。
	 */
	@Override
	@Nullable
	public HeadersRequestCondition getMatchingCondition(HttpServletRequest request) {
		// 如果是预检请求，则返回预检请求匹配
		if (CorsUtils.isPreFlightRequest(request)) {
			return PRE_FLIGHT_MATCH;
		}
		// 遍历当前对象的条件集合
		for (HeaderExpression expression : this.expressions) {
			// 如果当前请求与某个条件不匹配，则返回空
			if (!expression.match(request)) {
				return null;
			}
		}
		// 如果所有条件都匹配，则返回当前对象
		return this;
	}

	/**
	 * 根据头部表达式比较另一个条件。如果条件具有以下条件，则认为条件匹配更具体：
	 * <ol>
	 * <li>较多的表达式数量。
	 * <li>具有具体值的非否定表达式数量较多。
	 * </ol>
	 * <p>假定两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}获得的，
	 * 每个实例只包含匹配的头部表达式或为空。
	 */
	@Override
	public int compareTo(HeadersRequestCondition other, HttpServletRequest request) {
		// 比较另一个对象和当前对象的条件集合大小差异
		int result = other.expressions.size() - this.expressions.size();
		// 如果大小不同，则直接返回差异值
		if (result != 0) {
			return result;
		}
		// 否则，比较另一个对象和当前对象的条件集合中值的匹配数量
		return (int) (getValueMatchCount(other.expressions) - getValueMatchCount(this.expressions));
	}

	private long getValueMatchCount(Set<HeaderExpression> expressions) {
		// 初始化计数器
		long count = 0;
		// 遍历当前对象的条件集合
		for (HeaderExpression e : expressions) {
			// 如果条件的值不为空且未被否定，则计数器加一
			if (e.getValue() != null && !e.isNegated()) {
				count++;
			}
		}
		// 返回计数器值
		return count;
	}


	/**
	 * 解析并将单个头表达式与请求匹配。
	 */
	static class HeaderExpression extends AbstractNameValueExpression<String> {

		HeaderExpression(String expression) {
			super(expression);
		}

		@Override
		protected boolean isCaseSensitiveName() {
			return false;
		}

		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		@Override
		protected boolean matchName(HttpServletRequest request) {
			return (request.getHeader(this.name) != null);
		}

		@Override
		protected boolean matchValue(HttpServletRequest request) {
			return ObjectUtils.nullSafeEquals(this.value, request.getHeader(this.name));
		}
	}

}
