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

package org.springframework.web.reactive.result.condition;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 逻辑 AND（' && '）请求条件，根据在 {@link RequestMapping#headers()} 中定义的一组标头表达式匹配请求。
 *
 * <p>构造函数中传递的带有标头名称 'Accept' 或 'Content-Type' 的表达式将被忽略。
 * 请参阅 {@link ConsumesRequestCondition} 和 {@link ProducesRequestCondition}。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class HeadersRequestCondition extends AbstractRequestCondition<HeadersRequestCondition> {

	/**
	 * 预先匹配的 HeadersRequestCondition 实例。
	 */
	private static final HeadersRequestCondition PRE_FLIGHT_MATCH = new HeadersRequestCondition();

	/**
	 * 包含的 HeaderExpression 集合。
	 */
	private final Set<HeaderExpression> expressions;


	/**
	 * 从给定的标头表达式创建一个新实例。
	 * 忽略具有 'Accept' 或 'Content-Type' 标头名称的表达式。请参阅 {@link ConsumesRequestCondition}
	 * 和 {@link ProducesRequestCondition}。
	 *
	 * @param headers 媒体类型表达式，语法定义在 {@link RequestMapping#headers()}；
	 *                如果为 0，则该条件将匹配每个请求。
	 */
	public HeadersRequestCondition(String... headers) {
		this.expressions = parseExpressions(headers);
	}

	/**
	 * 解析标头表达式的静态方法。
	 *
	 * @param headers 要解析的标头表达式。
	 * @return 解析后的 HeaderExpression 集合。
	 */
	private static Set<HeaderExpression> parseExpressions(String... headers) {
		// 创建一个空的结果集合
		Set<HeaderExpression> result = null;

		// 如果头部不为空
		if (!ObjectUtils.isEmpty(headers)) {
			// 对于每个头部字符串
			for (String header : headers) {
				// 创建一个 HeaderExpression 对象
				HeaderExpression expr = new HeaderExpression(header);

				// 如果头部名称为 "Accept" 或者 "Content-Type"
				if ("Accept".equalsIgnoreCase(expr.name) || "Content-Type".equalsIgnoreCase(expr.name)) {
					// 继续下一次循环，跳过当前头部
					continue;
				}

				// 如果结果集合为空
				if (result == null) {
					// 创建一个 LinkedHashSet，指定初始容量为 headers.length
					result = new LinkedHashSet<>(headers.length);
				}

				// 将表达式添加到结果集合中
				result.add(expr);
			}
		}

		// 如果结果集合仍为空
		if (result == null) {
			// 返回一个空的不可变集合
			return Collections.emptySet();
		}

		// 返回结果集合
		return result;

	}

	private HeadersRequestCondition(Set<HeaderExpression> conditions) {
		this.expressions = conditions;
	}


	/**
	 * 返回包含的请求标头表达式。
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
	 * 返回一个新实例，其中包含“this”和“other”实例中的头部表达式的并集。
	 */
	@Override
	public HeadersRequestCondition combine(HeadersRequestCondition other) {
		// 如果“this”和“other”都为空
		if (isEmpty() && other.isEmpty()) {
			// 返回当前实例
			return this;
		}
		// 如果“other”为空
		else if (other.isEmpty()) {
			// 返回当前实例
			return this;
		}
		// 如果“this”为空
		else if (isEmpty()) {
			// 返回“other”实例
			return other;
		}

		// 创建一个新的 LinkedHashSet，包含“this”实例中的表达式
		Set<HeaderExpression> set = new LinkedHashSet<>(this.expressions);
		// 将“other”实例中的表达式添加到新集合中
		set.addAll(other.expressions);

		// 返回一个新的 HeadersRequestCondition 实例，包含合并后的表达式集合
		return new HeadersRequestCondition(set);
	}

	/**
	 * 如果请求匹配所有表达式，则返回“this”实例；否则返回{@code null}。
	 */
	@Override
	@Nullable
	public HeadersRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		// 如果是预检请求
		if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
			// 返回预检请求的匹配条件
			return PRE_FLIGHT_MATCH;
		}

		// 对于每个表达式
		for (HeaderExpression expression : this.expressions) {
			// 如果表达式不匹配请求
			if (!expression.match(exchange)) {
				// 返回空，请求不匹配
				return null;
			}
		}

		// 返回当前实例，表示请求匹配所有表达式
		return this;
	}


	/**
	 * 基于头部表达式与另一个条件进行比较。如果一个条件：
	 * <ol>
	 * <li>具有更多的表达式。
	 * <li>具有更多具体值的非否定表达式。
	 * </ol>
	 * 则认为该条件是更具体的匹配。
	 * <p>假定两个实例都是通过{@link #getMatchingCondition(ServerWebExchange)}获得的，
	 * 每个实例仅包含匹配的头部表达式或为空。
	 */
	@Override
	public int compareTo(HeadersRequestCondition other, ServerWebExchange exchange) {
		// 计算另一个实例表达式数量与当前实例表达式数量的差值
		int result = other.expressions.size() - this.expressions.size();

		// 如果差值不为0，返回差值
		if (result != 0) {
			return result;
		}

		// 返回具有非否定具体值的表达式数量的差值
		return (int) (getValueMatchCount(other.expressions) - getValueMatchCount(this.expressions));
	}


	/**
	 * 计算具有匹配值的表达式数量。
	 *
	 * @param expressions 要计算的表达式集合
	 * @return 具有匹配值的表达式数量
	 */
	private long getValueMatchCount(Set<HeaderExpression> expressions) {
		// 初始化匹配值的表达式计数
		long count = 0;

		// 对于每个表达式
		for (HeaderExpression e : expressions) {
			// 如果表达式具有值且不是否定的
			if (e.getValue() != null && !e.isNegated()) {
				// 增加匹配值的表达式计数
				count++;
			}
		}

		// 返回具有匹配值的表达式数量
		return count;
	}


	/**
	 * 解析并将单个标头表达式与请求匹配。
	 */
	static class HeaderExpression extends AbstractNameValueExpression<String> {

		/**
		 * 构造函数，使用给定的表达式。
		 *
		 * @param expression 给定的表达式。
		 */
		public HeaderExpression(String expression) {
			super(expression);
		}

		/**
		 * 判断名称是否区分大小写。
		 *
		 * @return 如果不区分大小写则返回 false。
		 */
		@Override
		protected boolean isCaseSensitiveName() {
			return false;
		}

		/**
		 * 解析值表达式。
		 *
		 * @param valueExpression 值表达式。
		 * @return 解析后的字符串值。
		 */
		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		/**
		 * 判断给定的 ServerWebExchange 是否匹配名称。
		 *
		 * @param exchange 要匹配的 ServerWebExchange 对象。
		 * @return 如果名称匹配则返回 true。
		 */
		@Override
		protected boolean matchName(ServerWebExchange exchange) {
			return (exchange.getRequest().getHeaders().get(this.name) != null);
		}

		/**
		 * 判断给定的 ServerWebExchange 是否匹配值。
		 *
		 * @param exchange 要匹配的 ServerWebExchange 对象。
		 * @return 如果值匹配则返回 true。
		 */
		@Override
		protected boolean matchValue(ServerWebExchange exchange) {
			return (this.value != null && this.value.equals(exchange.getRequest().getHeaders().getFirst(this.name)));
		}
	}

}
