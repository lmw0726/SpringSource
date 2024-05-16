/*
 * Copyright 2002-2018 the original author or authors.
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

import javax.servlet.http.HttpServletRequest;

/**
 * 支持 "name=value" 风格表达式的抽象类，如：
 * {@link org.springframework.web.bind.annotation.RequestMapping#params()} 和
 * {@link org.springframework.web.bind.annotation.RequestMapping#headers()} 中描述的那样。
 *
 * @param <T> 值的类型
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 3.1
 */
abstract class AbstractNameValueExpression<T> implements NameValueExpression<T> {
	/**
	 * 名称
	 */
	protected final String name;

	/**
	 * 值
	 */
	@Nullable
	protected final T value;

	/**
	 * 是否取反
	 */
	protected final boolean isNegated;

	/**
	 * 构造函数，解析表达式并初始化字段。
	 *
	 * @param expression 表达式字符串
	 */
	AbstractNameValueExpression(String expression) {
		// 找到表达式中等号的位置
		int separator = expression.indexOf('=');
		// 如果表达式中没有等号
		if (separator == -1) {
			// 判断是否以 "!" 开头，并设置 是否取反
			this.isNegated = expression.startsWith("!");
			// 根据是否以 "!" 开头，设置 名称
			this.name = (this.isNegated ? expression.substring(1) : expression);
			// 没有等号时，值为 null
			this.value = null;
		} else {
			// 判断等号前一个字符是否为 "!" 并设置 是否取反
			this.isNegated = (separator > 0) && (expression.charAt(separator - 1) == '!');
			// 根据是否以 "!" 结束等号前的部分，设置 名称
			this.name = (this.isNegated ? expression.substring(0, separator - 1) : expression.substring(0, separator));
			// 解析等号后面的部分，并设置 值
			this.value = parseValue(expression.substring(separator + 1));
		}
	}


	@Override
	public String getName() {
		return this.name;
	}

	@Override
	@Nullable
	public T getValue() {
		return this.value;
	}

	@Override
	public boolean isNegated() {
		return this.isNegated;
	}

	/**
	 * 检查表达式是否与请求匹配。
	 *
	 * @param request HttpServletRequest 请求对象
	 * @return 如果匹配返回 true；否则返回 false
	 */
	public final boolean match(HttpServletRequest request) {
		// 定义是否匹配的布尔变量
		boolean isMatch;
		// 如果有 值，则调用 matchValue 方法进行匹配
		if (this.value != null) {
			isMatch = matchValue(request);
		} else {
			// 否则，调用 matchName 方法进行匹配
			isMatch = matchName(request);
		}
		// 返回是否匹配的结果，考虑 取反 标志
		return this.isNegated != isMatch;
	}

	protected abstract boolean isCaseSensitiveName();

	protected abstract T parseValue(String valueExpression);

	protected abstract boolean matchName(HttpServletRequest request);

	protected abstract boolean matchValue(HttpServletRequest request);


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		AbstractNameValueExpression<?> that = (AbstractNameValueExpression<?>) other;
		return ((isCaseSensitiveName() ? this.name.equals(that.name) : this.name.equalsIgnoreCase(that.name)) &&
				ObjectUtils.nullSafeEquals(this.value, that.value) && this.isNegated == that.isNegated);
	}

	@Override
	public int hashCode() {
		int result = (isCaseSensitiveName() ? this.name.hashCode() : this.name.toLowerCase().hashCode());
		result = 31 * result + (this.value != null ? this.value.hashCode() : 0);
		result = 31 * result + (this.isNegated ? 1 : 0);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		if (this.value != null) {
			builder.append(this.name);
			if (this.isNegated) {
				builder.append('!');
			}
			builder.append('=');
			builder.append(this.value);
		} else {
			if (this.isNegated) {
				builder.append('!');
			}
			builder.append(this.name);
		}
		return builder.toString();
	}

}
