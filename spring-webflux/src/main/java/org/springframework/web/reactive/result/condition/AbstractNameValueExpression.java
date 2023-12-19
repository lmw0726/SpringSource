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

package org.springframework.web.reactive.result.condition;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * 支持“name=value”样式表达式，如：
 * {@link org.springframework.web.bind.annotation.RequestMapping#params()} 和
 * {@link org.springframework.web.bind.annotation.RequestMapping#headers()} 中所述。
 *
 * @param <T> the value type
 * @author Rossen Stoyanchev
 * @since 5.0
 */
abstract class AbstractNameValueExpression<T> implements NameValueExpression<T> {
	/**
	 * 表达式中的名称部分，如“name=value”中的“name”。
	 */
	protected final String name;

	/**
	 * 表达式中的值部分，如“name=value”中的“value”。
	 */
	@Nullable
	protected final T value;

	/**
	 * 表达式是否被否定，即是否存在“!”操作符。
	 */
	protected final boolean isNegated;


	/**
	 * 根据传入的表达式字符串初始化表达式对象。
	 * 如果表达式中不包含“=”号，则表达式只有名称部分。
	 * 如果表达式包含“=”号，则表达式包含名称和值两部分。
	 *
	 * @param expression 表达式字符串，例如“name=value”
	 */
	AbstractNameValueExpression(String expression) {
		// 寻找“=”号的位置
		int separator = expression.indexOf('=');

		if (separator == -1) {
			// 如果没有“=”号，表示表达式中只有名称部分
			this.isNegated = expression.startsWith("!");
			this.name = (this.isNegated ? expression.substring(1) : expression);
			// 没有值
			this.value = null;
		} else {
			// 如果有“=”号，表示表达式中包含名称和值两部分
			this.isNegated = (separator > 0) && (expression.charAt(separator - 1) == '!');
			this.name = (this.isNegated ? expression.substring(0, separator - 1) : expression.substring(0, separator));
			// 解析值部分
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

	public final boolean match(ServerWebExchange exchange) {
		boolean isMatch;
		if (this.value != null) {
			isMatch = matchValue(exchange);
		} else {
			isMatch = matchName(exchange);
		}
		return this.isNegated != isMatch;
	}


	/**
	 * 判断是否区分大小写的名称。
	 * @return 如果区分大小写则返回 true，否则返回 false。
	 */
	protected abstract boolean isCaseSensitiveName();

	/**
	 * 解析给定值表达式的抽象方法。
	 * @param valueExpression 要解析的值表达式。
	 * @return 解析后的值对象。
	 */
	protected abstract T parseValue(String valueExpression);

	/**
	 * 判断给定的 ServerWebExchange 是否匹配名称的抽象方法。
	 * @param exchange 要匹配的 ServerWebExchange 对象。
	 * @return 如果匹配名称则返回 true，否则返回 false。
	 */
	protected abstract boolean matchName(ServerWebExchange exchange);

	/**
	 * 判断给定的 ServerWebExchange 是否匹配值的抽象方法。
	 * @param exchange 要匹配的 ServerWebExchange 对象。
	 * @return 如果匹配值则返回 true，否则返回 false。
	 */
	protected abstract boolean matchValue(ServerWebExchange exchange);


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
		int result = (isCaseSensitiveName() ? this.name : this.name.toLowerCase()).hashCode();
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.value);
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
