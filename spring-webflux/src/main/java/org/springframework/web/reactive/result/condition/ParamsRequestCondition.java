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

import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ServerWebExchange;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 一个逻辑与（{@code ' && '）的请求条件，用于将请求与在{@link RequestMapping#params()}中定义语法的参数表达式集合进行匹配。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class ParamsRequestCondition extends AbstractRequestCondition<ParamsRequestCondition> {
	/**
	 * 参数表达式集合
	 */
	private final Set<ParamExpression> expressions;


	/**
	 * 使用给定的参数表达式创建一个新实例。
	 *
	 * @param params 符合{@link RequestMapping#params()}语法的表达式；
	 *               如果为 0，该条件将匹配所有请求。
	 */
	public ParamsRequestCondition(String... params) {
		// 使用给定的参数表达式数组解析并初始化表达式集合
		this.expressions = parseExpressions(params);
	}


	/**
	 * 解析参数表达式数组，并返回 ParamExpression 集合。
	 *
	 * @param params 要解析的参数表达式数组
	 * @return 解析后的 ParamExpression 集合
	 */
	private static Set<ParamExpression> parseExpressions(String... params) {
		// 如果参数数组为空
		if (ObjectUtils.isEmpty(params)) {
			// 返回一个空的不可变集合
			return Collections.emptySet();
		}

		// 创建一个新的 LinkedHashSet，指定初始容量为 params.length
		Set<ParamExpression> result = new LinkedHashSet<>(params.length);

		// 对于每个参数字符串
		for (String param : params) {
			// 将解析后的 ParamExpression 添加到结果集合中
			result.add(new ParamExpression(param));
		}

		// 返回 ParamExpression 集合
		return result;
	}

	private ParamsRequestCondition(Set<ParamExpression> conditions) {
		this.expressions = conditions;
	}

	/**
	 * 返回包含的请求参数表达式集合。
	 */
	public Set<NameValueExpression<String>> getExpressions() {
		// 返回一个新的 LinkedHashSet，包含当前实例的表达式集合
		return new LinkedHashSet<>(this.expressions);
	}

	@Override
	protected Collection<ParamExpression> getContent() {
		return this.expressions;
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	/**
	 * 返回一个新实例，其中包含“this”和“other”实例中的参数表达式的并集。
	 */
	@Override
	public ParamsRequestCondition combine(ParamsRequestCondition other) {
		// 如果“this”和“other”都为空
		if (isEmpty() && other.isEmpty()) {
			// 返回当前实例
			return this;
		} else if (other.isEmpty()) {
			// 返回当前实例
			return this;
		} else if (isEmpty()) {
			// 返回“other”实例
			return other;
		}

		// 创建一个新的 LinkedHashSet，包含“this”实例中的表达式
		Set<ParamExpression> set = new LinkedHashSet<>(this.expressions);
		// 将“other”实例中的表达式添加到新集合中
		set.addAll(other.expressions);

		// 返回一个新的 ParamsRequestCondition 实例，包含合并后的表达式集合
		return new ParamsRequestCondition(set);
	}

	/**
	 * 如果请求匹配所有参数表达式，则返回“this”实例；否则返回{@code null}。
	 */
	@Override
	public ParamsRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		// 对于每个参数表达式
		for (ParamExpression expression : this.expressions) {
			// 如果表达式不匹配请求
			if (!expression.match(exchange)) {
				// 返回空，请求不匹配
				return null;
			}
		}

		// 返回当前实例，表示请求匹配所有参数表达式
		return this;
	}

	/**
	 * 基于参数表达式与另一个条件进行比较。如果一个条件：
	 * <ol>
	 * <li>具有更多的表达式。
	 * <li>具有更多具体值的非否定表达式。
	 * </ol>
	 * 则认为该条件是更具体的匹配。
	 * <p>假定两个实例都是通过{@link #getMatchingCondition(ServerWebExchange)}获得的，
	 * 每个实例仅包含匹配的参数表达式或为空。
	 */
	@Override
	public int compareTo(ParamsRequestCondition other, ServerWebExchange exchange) {
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
	 * 计算具有匹配值的 ParamExpression 数量。
	 *
	 * @param expressions 要计算的 ParamExpression 集合
	 * @return 具有匹配值的 ParamExpression 数量
	 */
	private long getValueMatchCount(Set<ParamExpression> expressions) {
		// 初始化匹配值的 ParamExpression 计数
		long count = 0;

		// 对于每个 ParamExpression
		for (ParamExpression e : expressions) {
			// 如果 ParamExpression 具有值且不是否定的
			if (e.getValue() != null && !e.isNegated()) {
				// 增加匹配值的 ParamExpression 计数
				count++;
			}
		}

		// 返回具有匹配值的 ParamExpression 数量
		return count;
	}


	/**
	 * 解析并匹配单个参数表达式到请求。
	 */
	static class ParamExpression extends AbstractNameValueExpression<String> {

		/**
		 * 构造函数，使用给定的表达式创建 ParamExpression 对象。
		 *
		 * @param expression 参数表达式字符串
		 */
		ParamExpression(String expression) {
			super(expression);
		}

		/**
		 * 指定参数名是否区分大小写。
		 *
		 * @return 总是返回true，表示参数名区分大小写
		 */
		@Override
		protected boolean isCaseSensitiveName() {
			return true;
		}

		/**
		 * 解析参数值表达式。
		 *
		 * @param valueExpression 参数值表达式字符串
		 * @return 解析后的参数值
		 */
		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		/**
		 * 检查请求中是否存在与参数名匹配的参数。
		 *
		 * @param exchange 包含请求信息的 ServerWebExchange 对象
		 * @return 如果存在匹配的参数名，返回true；否则返回false
		 */
		@Override
		protected boolean matchName(ServerWebExchange exchange) {
			return exchange.getRequest().getQueryParams().containsKey(this.name);
		}

		/**
		 * 检查请求中与参数名匹配的参数值是否与表达式中指定的值相匹配。
		 *
		 * @param exchange 包含请求信息的 ServerWebExchange 对象
		 * @return 如果匹配，返回true；否则返回false
		 */
		@Override
		protected boolean matchValue(ServerWebExchange exchange) {
			return (this.value != null &&
					this.value.equals(exchange.getRequest().getQueryParams().getFirst(this.name)));
		}
	}

}
