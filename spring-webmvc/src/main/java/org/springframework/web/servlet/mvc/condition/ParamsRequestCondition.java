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
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 逻辑“与”（' && '）的请求条件，用于匹配请求与在{@link RequestMapping#params()}中定义的参数表达式集合。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ParamsRequestCondition extends AbstractRequestCondition<ParamsRequestCondition> {
	/**
	 * 参数表达式集合
	 */
	private final Set<ParamExpression> expressions;


	/**
	 * 创建一个新实例，从给定的参数表达式中。
	 *
	 * @param params 在{@link RequestMapping#params()}中定义的语法表达式；如果为0，则条件将匹配每个请求。
	 */
	public ParamsRequestCondition(String... params) {
		this.expressions = parseExpressions(params);
	}

	private static Set<ParamExpression> parseExpressions(String... params) {
		// 如果参数数组为空，则返回空集合
		if (ObjectUtils.isEmpty(params)) {
			return Collections.emptySet();
		}

		// 初始化参数表达式集合
		Set<ParamExpression> expressions = new LinkedHashSet<>(params.length);

		// 遍历参数数组，将每个参数转换为参数表达式，并添加到集合中
		for (String param : params) {
			expressions.add(new ParamExpression(param));
		}

		// 返回参数表达式集合
		return expressions;
	}

	private ParamsRequestCondition(Set<ParamExpression> conditions) {
		this.expressions = conditions;
	}


	/**
	 * 返回包含的请求参数表达式。
	 */
	public Set<NameValueExpression<String>> getExpressions() {
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
	 * 返回一个新实例，其中包含来自“此”和“其他”实例的参数表达式的并集。
	 */
	@Override
	public ParamsRequestCondition combine(ParamsRequestCondition other) {
		// 如果当前对象和另一个对象都为空，则返回当前对象
		if (isEmpty() && other.isEmpty()) {
			return this;
		} else if (other.isEmpty()) {
			// 如果另一个对象为空，则返回当前对象
			return this;
		} else if (isEmpty()) {
			// 如果当前对象为空，则返回另一个对象
			return other;
		}

		// 初始化参数表达式集合
		Set<ParamExpression> set = new LinkedHashSet<>(this.expressions);

		// 将另一个对象的参数表达式集合添加到当前集合中
		set.addAll(other.expressions);

		// 返回新的参数请求条件对象
		return new ParamsRequestCondition(set);
	}

	/**
	 * 如果请求匹配所有参数表达式，则返回“此”实例；否则返回{@code null}。
	 */
	@Override
	@Nullable
	public ParamsRequestCondition getMatchingCondition(HttpServletRequest request) {
		// 遍历当前对象的参数表达式集合
		for (ParamExpression expression : this.expressions) {
			// 如果有任何一个参数表达式不匹配当前请求，则返回空
			if (!expression.match(request)) {
				return null;
			}
		}

		// 如果所有参数表达式都匹配当前请求，则返回当前对象
		return this;
	}

	/**
	 * 基于参数表达式与另一个条件进行比较。如果条件具有以下更多的表达式，则认为条件是更具体的匹配：
	 * <ol>
	 * <li>更多的表达式。
	 * <li>更多具体值的非否定表达式。
	 * </ol>
	 * 假定两个实例都是通过{@link #getMatchingCondition(HttpServletRequest)}获得的，
	 * 每个实例仅包含匹配的参数表达式，否则为空。
	 */
	@Override
	public int compareTo(ParamsRequestCondition other, HttpServletRequest request) {
		// 计算其他请求条件中参数表达式集合大小与当前请求条件中参数表达式集合大小的差值
		int result = other.expressions.size() - this.expressions.size();

		// 如果差值不为0，则返回差值作为比较结果
		if (result != 0) {
			return result;
		}

		// 如果差值为0，则比较其他请求条件中参数表达式集合与当前请求条件中参数表达式集合的匹配参数值数量
		// 并返回两者匹配参数值数量之差
		return (int) (getValueMatchCount(other.expressions) - getValueMatchCount(this.expressions));
	}

	private long getValueMatchCount(Set<ParamExpression> expressions) {
		// 初始化计数器
		long count = 0;

		// 遍历参数表达式集合
		for (ParamExpression e : expressions) {
			// 如果参数值不为空且不是否定的，则增加计数器
			if (e.getValue() != null && !e.isNegated()) {
				count++;
			}
		}

		// 返回计数结果
		return count;
	}


	/**
	 * 解析并匹配请求中的单个参数表达式。
	 */
	static class ParamExpression extends AbstractNameValueExpression<String> {

		/**
		 * 要匹配的名称集合
		 */
		private final Set<String> namesToMatch = new HashSet<>(WebUtils.SUBMIT_IMAGE_SUFFIXES.length + 1);

		/**
		 * 构造函数，解析表达式并初始化字段。
		 *
		 * @param expression 参数表达式字符串
		 */
		ParamExpression(String expression) {
			// 调用父类构造函数，并传入表达式
			super(expression);
			// 将名称添加到 要匹配的名称集合 中
			this.namesToMatch.add(getName());
			// 遍历 WebUtils.SUBMIT_IMAGE_SUFFIXES 中的每个后缀
			for (String suffix : WebUtils.SUBMIT_IMAGE_SUFFIXES) {
				// 将名称加上后缀添加到 要匹配的名称集合 中
				this.namesToMatch.add(getName() + suffix);
			}
		}

		@Override
		protected boolean isCaseSensitiveName() {
			return true;
		}

		@Override
		protected String parseValue(String valueExpression) {
			return valueExpression;
		}

		@Override
		protected boolean matchName(HttpServletRequest request) {
			// 遍历 要匹配的名称集合 列表中的每个名称
			for (String current : this.namesToMatch) {
				// 如果请求的参数映射中包含当前名称，则返回 true
				if (request.getParameterMap().get(current) != null) {
					return true;
				}
			}
			// 如果 要匹配的名称集合 列表中没有匹配的名称，则检查请求的参数映射中是否包含 名称
			return request.getParameterMap().containsKey(this.name);
		}

		@Override
		protected boolean matchValue(HttpServletRequest request) {
			return ObjectUtils.nullSafeEquals(this.value, request.getParameter(this.name));
		}
	}

}
