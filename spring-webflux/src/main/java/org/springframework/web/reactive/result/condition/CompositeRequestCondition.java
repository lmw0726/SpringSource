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

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * CompositeRequestCondition 类实现了 RequestCondition 接口的契约，通过委托给多个 RequestCondition 类型，
 * 并使用逻辑上的合取（' && '）来确保所有条件匹配给定的请求。
 * <p>
 * 当组合或比较 CompositeRequestCondition 实例时，预期它们：
 * (a) 包含相同数量的条件，
 * (b) 相同索引位置的条件是相同类型的。
 * 允许在构造函数中提供 null 条件或者不提供任何条件。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class CompositeRequestCondition extends AbstractRequestCondition<CompositeRequestCondition> {

	/**
	 * 请求条件持有者
	 */
	private final RequestConditionHolder[] requestConditions;


	/**
	 * 使用零个或多个 RequestCondition 类型的参数创建一个实例。
	 * 创建 CompositeRequestCondition 实例时，条件的数量应相同，以便进行比较和合并。
	 * 可以提供 null 作为条件。
	 */
	public CompositeRequestCondition(RequestCondition<?>... requestConditions) {
		// 调用私有方法 wrap() 将传入的条件数组包装成 RequestConditionHolder 数组，并赋值给成员变量 requestConditions
		this.requestConditions = wrap(requestConditions);
	}

	/**
	 * 私有构造函数，接收一个 RequestConditionHolder 数组作为参数，并将其赋值给类中的成员变量 requestConditions。
	 */
	private CompositeRequestCondition(RequestConditionHolder[] requestConditions) {
		this.requestConditions = requestConditions;
	}


	/**
	 * 将原始条件包装成 RequestConditionHolder 数组。
	 */
	private RequestConditionHolder[] wrap(RequestCondition<?>... rawConditions) {
		// 创建一个与原始条件长度相同的 RequestConditionHolder 数组
		RequestConditionHolder[] wrappedConditions = new RequestConditionHolder[rawConditions.length];
		// 遍历原始条件数组
		for (int i = 0; i < rawConditions.length; i++) {
			// 将每个原始条件包装成 RequestConditionHolder 对象，并添加到数组中
			wrappedConditions[i] = new RequestConditionHolder(rawConditions[i]);
		}
		// 返回包装后的 RequestConditionHolder 数组
		return wrappedConditions;
	}

	/**
	 * 判断此实例是否包含零个条件。
	 */
	@Override
	public boolean isEmpty() {
		return ObjectUtils.isEmpty(this.requestConditions);
	}

	/**
	 * 返回底层条件的列表，可能为空但永远不为 {@code null}。
	 */
	public List<RequestCondition<?>> getConditions() {
		return unwrap();
	}

	/**
	 * 获取 CompositeRequestCondition 实例中每个条件的列表。
	 */
	private List<RequestCondition<?>> unwrap() {
		// 创建一个新的空列表，用于存储提取出来的条件
		List<RequestCondition<?>> result = new ArrayList<>();

		// 遍历 CompositeRequestCondition 中的每个条件持有者
		for (RequestConditionHolder holder : this.requestConditions) {
			// 将每个条件持有者中的条件提取出来，并添加到结果列表中
			result.add(holder.getCondition());
		}

		// 返回包含提取条件的列表
		return result;
	}

	/**
	 * 返回一个集合对象，表示此对象的内容。
	 * 如果当前对象不为空，则返回条件列表；否则返回空集合。
	 */
	@Override
	protected Collection<?> getContent() {
		// 如果当前对象不为空，则返回条件列表
		// 否则返回一个空的集合
		return (!isEmpty() ? getConditions() : Collections.emptyList());
	}

	/**
	 * 返回一个字符串，用作条件组合的中缀连接符。
	 * 在这里，返回的字符串是 " && "，表示条件之间的逻辑 AND 连接。
	 */
	@Override
	protected String getToStringInfix() {
		return " && ";
	}

	/**
	 * 获取当前对象中 requestConditions 数组的长度。
	 */
	private int getLength() {
		// 返回 requestConditions 数组的长度
		return this.requestConditions.length;
	}

	/**
	 * 如果一个实例为空，则返回另一个实例。
	 * 如果两个实例都有条件，则将它们的条件组合起来，
	 * 在确保它们是相同类型和数量的条件后进行组合。
	 */
	@Override
	public CompositeRequestCondition combine(CompositeRequestCondition other) {
		// 检查两个实例是否都为空，如果是，则返回当前实例
		if (isEmpty() && other.isEmpty()) {
			return this;
		} else if (other.isEmpty()) {
			// 如果第二个实例为空，则返回当前实例
			return this;
		} else if (isEmpty()) {
			// 如果第一个实例为空，则返回第二个实例
			return other;
		} else {
			// 确保两个实例的条件数量相同
			assertNumberOfConditions(other);
			// 创建一个数组来存储组合后的条件
			RequestConditionHolder[] combinedConditions = new RequestConditionHolder[getLength()];
			// 循环遍历每个条件并将其组合
			for (int i = 0; i < getLength(); i++) {
				combinedConditions[i] = this.requestConditions[i].combine(other.requestConditions[i]);
			}
			// 返回一个新的 CompositeRequestCondition 实例，其中包含组合后的条件
			return new CompositeRequestCondition(combinedConditions);
		}
	}

	/**
	 * 断言条件的数量是否相同。
	 *
	 * @param other 另一个 CompositeRequestCondition 对象，用于比较条件数量。
	 */
	private void assertNumberOfConditions(CompositeRequestCondition other) {
		Assert.isTrue(getLength() == other.getLength(),
				"Cannot combine CompositeRequestConditions with a different number of conditions. " +
						ObjectUtils.nullSafeToString(this.requestConditions) + " and  " +
						ObjectUtils.nullSafeToString(other.requestConditions));
	}

	/**
	 * 委托给 <em>所有</em> 包含的条件来匹配请求并返回结果的“匹配”条件实例。
	 * <p>一个空的 {@code CompositeRequestCondition} 匹配所有请求。
	 */
	@Override
	public CompositeRequestCondition getMatchingCondition(ServerWebExchange exchange) {
		if (isEmpty()) {
			return this;
		}
		RequestConditionHolder[] matchingConditions = new RequestConditionHolder[getLength()];
		for (int i = 0; i < getLength(); i++) {
			// 获取每个条件的匹配情况
			matchingConditions[i] = this.requestConditions[i].getMatchingCondition(exchange);
			if (matchingConditions[i] == null) {
				return null;
			}
		}
		return new CompositeRequestCondition(matchingConditions);
	}

	/**
	 * 如果一个实例为空，则另一个“获胜”。如果两个实例都有条件，则按照它们提供的顺序进行比较。
	 */
	@Override
	public int compareTo(CompositeRequestCondition other, ServerWebExchange exchange) {
		if (isEmpty() && other.isEmpty()) {
			return 0;
		} else if (isEmpty()) {
			return 1;
		} else if (other.isEmpty()) {
			return -1;
		} else {
			assertNumberOfConditions(other);
			for (int i = 0; i < getLength(); i++) {
				// 比较条件，按照提供的顺序进行比较
				int result = this.requestConditions[i].compareTo(other.requestConditions[i], exchange);
				if (result != 0) {
					return result;
				}
			}
			return 0;
		}
	}

}
