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
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 通过委托给多个{@code RequestCondition}类型并使用逻辑与运算符（{@code '&&'}）来实现{@link RequestCondition}约定，
 * 以确保所有条件都匹配给定的请求。
 *
 * <p>当{@code CompositeRequestCondition}实例被组合或比较时，预期它们（a）包含相同数量的条件，
 * 并且（b）各个索引处的条件是相同类型的。可以在构造函数中提供{@code null}条件或不提供条件。
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class CompositeRequestCondition extends AbstractRequestCondition<CompositeRequestCondition> {

	/**
	 * 请求条件持有者数组
	 */
	private final RequestConditionHolder[] requestConditions;


	/**
	 * 创建一个具有0个或多个{@code RequestCondition}类型的实例。
	 * 重要的是要创建具有相同数量条件的{@code CompositeRequestCondition}实例，以便它们可以进行比较和组合。
	 * 可以提供{@code null}条件。
	 */
	public CompositeRequestCondition(RequestCondition<?>... requestConditions) {
		this.requestConditions = wrap(requestConditions);
	}

	private CompositeRequestCondition(RequestConditionHolder[] requestConditions) {
		this.requestConditions = requestConditions;
	}


	private RequestConditionHolder[] wrap(RequestCondition<?>... rawConditions) {
		// 创建一个包含原始条件的数组
		RequestConditionHolder[] wrappedConditions = new RequestConditionHolder[rawConditions.length];
		// 遍历原始条件数组
		for (int i = 0; i < rawConditions.length; i++) {
			// 使用原始条件创建一个新的 RequestConditionHolder 对象，并将其放入数组中
			wrappedConditions[i] = new RequestConditionHolder(rawConditions[i]);
		}
		// 返回包含所有原始条件的 RequestConditionHolder 数组
		return wrappedConditions;
	}

	/**
	 * 是否包含0个条件。
	 */
	@Override
	public boolean isEmpty() {
		return ObjectUtils.isEmpty(this.requestConditions);
	}

	/**
	 * 返回底层条件（可能为空，但永远不会是{@code null}）。
	 */
	public List<RequestCondition<?>> getConditions() {
		return unwrap();
	}

	private List<RequestCondition<?>> unwrap() {
		// 创建一个列表用于存储条件
		List<RequestCondition<?>> result = new ArrayList<>();
		// 遍历请求条件持有者列表
		for (RequestConditionHolder holder : this.requestConditions) {
			// 将每个持有者中的条件添加到结果列表中
			result.add(holder.getCondition());
		}
		// 返回结果列表
		return result;
	}

	@Override
	protected Collection<?> getContent() {
		return (!isEmpty() ? getConditions() : Collections.emptyList());
	}

	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	private int getLength() {
		return this.requestConditions.length;
	}

	/**
	 * 如果一个实例为空，则返回另一个实例。
	 * 如果两个实例都有条件，则在确保它们具有相同类型和数量的条件之后，组合各个条件。
	 */
	@Override
	public CompositeRequestCondition combine(CompositeRequestCondition other) {
		// 如果当前对象和另一个对象的条件都为空，则返回当前对象
		if (isEmpty() && other.isEmpty()) {
			return this;
		} else if (other.isEmpty()) {
			// 如果另一个对象的条件为空，则返回当前对象
			return this;
		} else if (isEmpty()) {
			// 如果当前对象的条件为空，则返回另一个对象
			return other;
		} else {
			// 如果两个对象的条件都不为空
			// 断言另一个对象的条件数量与当前对象相同
			assertNumberOfConditions(other);
			// 创建一个数组来组合条件
			RequestConditionHolder[] combinedConditions = new RequestConditionHolder[getLength()];
			// 遍历条件持有者数组，将每个条件持有者的条件与另一个对象对应位置的条件进行组合
			for (int i = 0; i < getLength(); i++) {
				combinedConditions[i] = this.requestConditions[i].combine(other.requestConditions[i]);
			}
			// 返回一个包含组合条件的新的 CompositeRequestCondition 对象
			return new CompositeRequestCondition(combinedConditions);
		}
	}

	private void assertNumberOfConditions(CompositeRequestCondition other) {
		Assert.isTrue(getLength() == other.getLength(),
				"Cannot combine CompositeRequestConditions with a different number of conditions. " +
						ObjectUtils.nullSafeToString(this.requestConditions) + " and  " +
						ObjectUtils.nullSafeToString(other.requestConditions));
	}

	/**
	 * 委托给<em>所有</em>包含的条件来匹配请求，并返回结果的“匹配”条件实例。
	 * <p>一个空的{@code CompositeRequestCondition}匹配所有请求。
	 */
	@Override
	@Nullable
	public CompositeRequestCondition getMatchingCondition(HttpServletRequest request) {
		// 如果当前对象的条件为空，则返回当前对象
		if (isEmpty()) {
			return this;
		}
		// 创建一个数组来存储匹配的条件
		RequestConditionHolder[] matchingConditions = new RequestConditionHolder[getLength()];
		// 遍历条件持有者数组
		for (int i = 0; i < getLength(); i++) {
			// 获取每个条件持有者与请求匹配的条件
			matchingConditions[i] = this.requestConditions[i].getMatchingCondition(request);
			// 如果某个条件为空，则返回空
			if (matchingConditions[i] == null) {
				return null;
			}
		}
		// 返回一个新的 CompositeRequestCondition 对象，其中包含匹配的条件
		return new CompositeRequestCondition(matchingConditions);
	}

	/**
	 * 如果一个实例为空，则另一个“获胜”。
	 * 如果两个实例都有条件，则按照它们提供的顺序进行比较。
	 */
	@Override
	public int compareTo(CompositeRequestCondition other, HttpServletRequest request) {
		if (isEmpty() && other.isEmpty()) {
			return 0;
		} else if (isEmpty()) {
			return 1;
		} else if (other.isEmpty()) {
			return -1;
		} else {
			assertNumberOfConditions(other);
			for (int i = 0; i < getLength(); i++) {
				int result = this.requestConditions[i].compareTo(other.requestConditions[i], request);
				if (result != 0) {
					return result;
				}
			}
			return 0;
		}
	}

}
