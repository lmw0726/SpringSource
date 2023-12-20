/*
 * Copyright 2002-2017 the original author or authors.
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
import org.springframework.web.server.ServerWebExchange;

import java.util.Collection;
import java.util.Collections;

/**
 * RequestConditionHolder 类是一个用于持有 RequestCondition 的包装器，
 * 当请求条件的类型在编译时不知道时很有用，比如自定义条件。
 * 由于这个类也是 RequestCondition 的实现，因此它有效地装饰了所持有的请求条件，
 * 并允许以类型和空值安全的方式将其与其他请求条件进行组合和比较。
 * <p>
 * 当两个 RequestConditionHolder 实例进行组合或比较时，预期它们持有的条件是相同类型的。
 * 如果它们的条件类型不同，会抛出 ClassCastException 异常。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class RequestConditionHolder extends AbstractRequestCondition<RequestConditionHolder> {

	/**
	 * 请求条件
	 */
	@Nullable
	private final RequestCondition<Object> condition;

	/**
	 * 创建一个新的持有者来包装给定的请求条件。
	 *
	 * @param requestCondition 要持有的条件（可以为 null）
	 */
	@SuppressWarnings("unchecked")
	public RequestConditionHolder(@Nullable RequestCondition<?> requestCondition) {
		this.condition = (RequestCondition<Object>) requestCondition;
	}


	/**
	 * 返回所持有的请求条件，如果没有则返回 null。
	 */
	@Nullable
	public RequestCondition<?> getCondition() {
		return this.condition;
	}

	/**
	 * 返回一个集合对象，表示此对象的内容。
	 * 如果条件存在，则返回包含条件的单元素集合；否则返回空集合。
	 */
	@Override
	protected Collection<?> getContent() {
		// 如果条件存在，则返回包含条件的单元素集合；否则返回空集合
		return (this.condition == null ? Collections.emptyList() : Collections.singleton(this.condition));
	}

	/**
	 * 返回一个字符串，用作条件组合的中缀连接符。
	 * 在这里，返回的字符串是空格 " "，表示条件之间的空格连接。
	 */
	@Override
	protected String getToStringInfix() {
		return " ";
	}

	/**
	 * 在确保条件类型相同的情况下，组合两个 RequestConditionHolder 实例持有的请求条件。
	 * 或者如果一个持有者为空，则返回另一个持有者。
	 */
	@Override
	public RequestConditionHolder combine(RequestConditionHolder other) {
		// 如果两个持有者的条件都为空，则返回当前持有者
		if (this.condition == null && other.condition == null) {
			return this;
		} else if (this.condition == null) {
			// 如果当前持有者的条件为空，则返回另一个持有者
			return other;
		} else if (other.condition == null) {
			// 如果另一个持有者的条件为空，则返回当前持有者
			return this;
		} else {
			// 确保条件类型相同
			assertEqualConditionTypes(this.condition, other.condition);
			// 组合两个条件
			RequestCondition<?> combined = (RequestCondition<?>) this.condition.combine(other.condition);
			// 创建一个新的 RequestConditionHolder 包装组合后的条件
			return new RequestConditionHolder(combined);
		}
	}

	/**
	 * 获取匹配的请求条件，并将其包装在一个新的 RequestConditionHolder 实例中。
	 * 如果当前持有者为空，则返回相同的持有者实例。
	 */
	@Override
	public RequestConditionHolder getMatchingCondition(ServerWebExchange exchange) {
		// 如果当前持有者的条件为空，则返回相同的持有者实例
		if (this.condition == null) {
			return this;
		}
		// 获取匹配的条件
		RequestCondition<?> match = (RequestCondition<?>) this.condition.getMatchingCondition(exchange);
		// 如果匹配的条件不为空，则创建一个新的 RequestConditionHolder 包装它
		return (match == null ? null : new RequestConditionHolder(match));
	}

	/**
	 * 在确保条件类型相同的情况下，比较两个 RequestConditionHolder 实例持有的请求条件。
	 * 或者如果一个持有者为空，则另一个持有者被认为更优先。
	 */
	@Override
	public int compareTo(RequestConditionHolder other, ServerWebExchange exchange) {
		// 如果两个持有者的条件都为空，则认为它们相等
		if (this.condition == null && other.condition == null) {
			return 0;
		} else if (this.condition == null) {
			// 如果当前持有者的条件为空，则认为其他持有者更优先
			return 1;
		} else if (other.condition == null) {
			// 如果其他持有者的条件为空，则认为当前持有者更优先
			return -1;
		} else {
			// 确保条件类型相同
			assertEqualConditionTypes(this.condition, other.condition);
			// 比较两个条件
			return this.condition.compareTo(other.condition, exchange);
		}
	}

	/**
	 * 确保持有的请求条件是相同类型的。
	 */
	private void assertEqualConditionTypes(RequestCondition<?> cond1, RequestCondition<?> cond2) {
		// 获取第一个条件的类
		Class<?> clazz = cond1.getClass();
		// 获取第二个条件的类
		Class<?> otherClazz = cond2.getClass();
		// 如果两个条件的类不相同，则抛出 ClassCastException 异常
		if (!clazz.equals(otherClazz)) {
			throw new ClassCastException("Incompatible request conditions: " + clazz + " vs " + otherClazz);
		}
	}

}
