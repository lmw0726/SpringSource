/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core;

/**
 * {@code Ordered}是一个接口，可以由应该<em>可排序</em>的对象实现，例如在{@code Collection}中。
 *
 * <p>实际的{@link #getOrder() order}可以解释为优先级排序，具有最低顺序值的第一个对象具有最高优先级。
 *
 * <p>请注意，该接口还有一个<em>优先级</em>标记：{@link PriorityOrdered}。请参阅{@code PriorityOrdered}的Javadoc，了解{@code PriorityOrdered}
 * 对象如何相对于<em>普通</em>{@link Ordered}对象排序的详细信息。
 *
 * <p>有关非有序对象的排序语义的详细信息，请参阅{@link OrderComparator}的Javadoc。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see PriorityOrdered
 * @see OrderComparator
 * @see org.springframework.core.annotation.Order
 * @see org.springframework.core.annotation.AnnotationAwareOrderComparator
 * @since 07.04.2003
 */
public interface Ordered {

	/**
	 * 用于最高优先级值的常量。
	 *
	 * @see java.lang.Integer#MIN_VALUE
	 */
	int HIGHEST_PRECEDENCE = Integer.MIN_VALUE;

	/**
	 * 用于最低优先级值的常量。
	 *
	 * @see java.lang.Integer#MAX_VALUE
	 */
	int LOWEST_PRECEDENCE = Integer.MAX_VALUE;


	/**
	 * 获取此对象的顺序值。
	 * <p>较高的值被解释为较低的优先级。因此，具有最低值的对象具有最高优先级（在某种程度上类似于Servlet {@code load-on-startup} 值）。
	 * <p>相同的顺序值将导致受影响对象的任意排序位置。
	 *
	 * @return 顺序值
	 * @see #HIGHEST_PRECEDENCE
	 * @see #LOWEST_PRECEDENCE
	 */
	int getOrder();

}
