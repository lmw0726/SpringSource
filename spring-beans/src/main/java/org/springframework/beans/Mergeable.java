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

package org.springframework.beans;

import org.springframework.lang.Nullable;

/**
 * Interface representing an object whose value set can be merged with
 * that of a parent object.
 *
 * @author Rob Harrop
 * @see org.springframework.beans.factory.support.ManagedSet
 * @see org.springframework.beans.factory.support.ManagedList
 * @see org.springframework.beans.factory.support.ManagedMap
 * @see org.springframework.beans.factory.support.ManagedProperties
 * @since 2.0
 */
public interface Mergeable {

	/**
	 * 是否为此特定实例启用了合并？
	 */
	boolean isMergeEnabled();

	/**
	 * 将设置的当前值与提供的对象的值合并。
	 * <p> 提供的对象被视为父对象，被取值集中的值必须覆盖提供的对象的值。
	 *
	 * @param parent 要与之合并的对象
	 * @return 合并操作的结果
	 * @throws IllegalArgumentException 如果提供的父项为 {@code null}
	 * @throws IllegalStateException    如果未为此实例启用合并 (即 {@code mergeEnabled} 等于 {@code false})。
	 */
	Object merge(@Nullable Object parent);

}
