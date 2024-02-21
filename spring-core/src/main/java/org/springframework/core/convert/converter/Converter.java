/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core.convert.converter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 转换器将类型为 {@code S} 的源对象转换为类型为 {@code T} 的目标对象。
 *
 * <p>该接口的实现是线程安全的，可以共享。
 *
 * <p>实现还可以实现 {@link ConditionalConverter}。
 *
 * @param <S> 源类型
 * @param <T> 目标类型
 * @author Keith Donald
 * @author Josh Cummings
 * @since 3.0
 */
@FunctionalInterface
public interface Converter<S, T> {

	/**
	 * 将类型为 {@code S} 的源对象转换为类型为 {@code T} 的目标对象。
	 *
	 * @param source 要转换的源对象，必须是 {@code S} 的实例（永远不会是 {@code null}）
	 * @return 转换后的对象，必须是 {@code T} 的实例（可能是 {@code null}）
	 * @throws IllegalArgumentException 如果源对象无法转换为所需的目标类型
	 */
	@Nullable
	T convert(S source);

	/**
	 * 构造一个组合的 {@link Converter}，该组合首先将此 {@link Converter} 应用于其输入，然后将 {@code after}
	 * {@link Converter} 应用于结果。
	 *
	 * @param after 应用于此 {@link Converter} 后的 {@link Converter}
	 * @param <U>   {@code after} {@link Converter} 和组合的 {@link Converter} 的输出类型
	 * @return 首先应用此 {@link Converter}，然后应用 {@code after} {@link Converter} 的组合 {@link Converter}
	 * @since 5.3
	 */
	default <U> Converter<S, U> andThen(Converter<? super T, ? extends U> after) {
		Assert.notNull(after, "After Converter must not be null");
		return (S s) -> {
			T initialResult = convert(s);
			return (initialResult != null ? after.convert(initialResult) : null);
		};
	}

}
