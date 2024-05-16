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

package org.springframework.web.servlet.mvc.condition;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;

/**
 * 当请求条件的类型事先不确定时，用于持有{@link RequestCondition}的类。例如，自定义条件。
 * 由于这个类也是{@code RequestCondition}的实现，因此它实际上是装饰了持有的请求条件，
 * 并允许以类型安全和空安全的方式将其与其他请求条件进行组合和比较。
 *
 * <p>当两个{@code RequestConditionHolder}实例组合或相互比较时，预期它们持有的条件是相同类型的。
 * 如果它们不是，则会引发{@link ClassCastException}。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class RequestConditionHolder extends AbstractRequestCondition<RequestConditionHolder> {
	/**
	 * 请求条件
	 */
	@Nullable
	private final RequestCondition<Object> condition;


	/**
	 * 创建一个新的持有器来包装给定的请求条件。
	 *
	 * @param requestCondition 要持有的条件，可以为{@code null}
	 */
	@SuppressWarnings("unchecked")
	public RequestConditionHolder(@Nullable RequestCondition<?> requestCondition) {
		this.condition = (RequestCondition<Object>) requestCondition;
	}


	/**
	 * 返回持有的请求条件，如果没有持有，则返回{@code null}。
	 */
	@Nullable
	public RequestCondition<?> getCondition() {
		return this.condition;
	}

	@Override
	protected Collection<?> getContent() {
		return (this.condition != null ? Collections.singleton(this.condition) : Collections.emptyList());
	}

	@Override
	protected String getToStringInfix() {
		return " ";
	}

	/**
	 * 在确保条件具有相同类型之后，组合两个RequestConditionHolder实例持有的请求条件。
	 * 如果一个持有器为空，则返回另一个持有器。
	 */
	@Override
	public RequestConditionHolder combine(RequestConditionHolder other) {
		// 如果当前对象和另一个对象的条件都为空，则返回当前对象
		if (this.condition == null && other.condition == null) {
			return this;
		} else if (this.condition == null) {
			// 如果当前对象的条件为空，则返回另一个对象
			return other;
		} else if (other.condition == null) {
			// 如果另一个对象的条件为空，则返回当前对象
			return this;
		} else {
			// 如果两个对象的条件都不为空
			// 断言两个条件的类型相同
			assertEqualConditionTypes(this.condition, other.condition);
			// 组合两个条件
			RequestCondition<?> combined = (RequestCondition<?>) this.condition.combine(other.condition);
			// 返回组合后的新对象
			return new RequestConditionHolder(combined);
		}
	}

	/**
	 * 确保持有的请求条件具有相同的类型。
	 */
	private void assertEqualConditionTypes(RequestCondition<?> thisCondition, RequestCondition<?> otherCondition) {
		// 获取当前对象的条件类和另一个对象的条件类
		Class<?> clazz = thisCondition.getClass();
		Class<?> otherClazz = otherCondition.getClass();
		// 如果两个条件类不相等，则抛出类型转换异常
		if (!clazz.equals(otherClazz)) {
			throw new ClassCastException("Incompatible request conditions: " + clazz + " and " + otherClazz);
		}
	}

	/**
	 * 获取持有的请求条件的匹配条件，并将其包装在一个新的RequestConditionHolder实例中。
	 * 如果这是一个空的持有器，则返回相同的持有器实例。
	 */
	@Override
	@Nullable
	public RequestConditionHolder getMatchingCondition(HttpServletRequest request) {
		// 如果当前对象的条件为空，则返回当前对象
		if (this.condition == null) {
			return this;
		}
		// 获取匹配的条件
		RequestCondition<?> match = (RequestCondition<?>) this.condition.getMatchingCondition(request);
		// 如果匹配的条件不为空，则返回包含匹配条件的新对象，否则返回空
		return (match != null ? new RequestConditionHolder(match) : null);
	}

	/**
	 * 在确保条件具有相同类型之后，比较两个RequestConditionHolder实例持有的请求条件。
	 * 如果一个持有器为空，则另一个持有器优先。
	 */
	@Override
	public int compareTo(RequestConditionHolder other, HttpServletRequest request) {
		if (this.condition == null && other.condition == null) {
			return 0;
		} else if (this.condition == null) {
			return 1;
		} else if (other.condition == null) {
			return -1;
		} else {
			assertEqualConditionTypes(this.condition, other.condition);
			return this.condition.compareTo(other.condition, request);
		}
	}

}
