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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.lang.Nullable;

/**
 * null Bean 实例的内部表示，例如从 {@link FactoryBean#getObject()} 或工厂方法返回的 {@code null} 值。
 *
 * <p>每一个这样的 null Bean 都由一个独立的 {@code NullBean} 实例表示，这些实例彼此不相等，
 * 从而在所有形式的 {@link org.springframework.beans.factory.BeanFactory#getBean} 方法返回时，
 * 能唯一区分各个 Bean。然而，每个此类实例在调用 {@code #equals(null)} 时会返回 {@code true}，
 * 并且在调用 {@code #toString()} 时返回 "null"。这使得外部可以通过这些方式对其进行判断
 *（由于该类本身不是公开的，因此无法直接引用）。
 *
 * @author Juergen Hoeller
 * @since 5.0
 */
final class NullBean {

	NullBean() {
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		return (this == obj || obj == null);
	}

	@Override
	public int hashCode() {
		return NullBean.class.hashCode();
	}

	@Override
	public String toString() {
		return "null";
	}

}
