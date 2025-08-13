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

package org.springframework.core;

import org.springframework.lang.Nullable;

/**
 * 任何对象都可以实现此接口以提供其实际的 {@link ResolvableType}。
 *
 * <p>当判断实例是否匹配泛型签名时，这类信息非常有用，
 * 因为 Java 在运行时不保留泛型签名信息。
 *
 * <p>此接口的使用者在复杂继承层级中应谨慎，
 * 尤其是在子类中泛型签名发生变化时。
 * 始终可以返回 {@code null}，以回退到默认行为。
 *
 * @author Stephane Nicoll
 * @since 4.2
 */
public interface ResolvableTypeProvider {

	/**
	 * 返回描述该实例的 {@link ResolvableType}，
	 * 如果应使用某种默认处理，则返回 {@code null}。
	 */
	@Nullable
	ResolvableType getResolvableType();

}
