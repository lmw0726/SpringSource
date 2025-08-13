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

package org.springframework.core.env;

import org.springframework.lang.Nullable;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * 持有一个或多个 {@link PropertySource} 对象的容器。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see PropertySource
 */
public interface PropertySources extends Iterable<PropertySource<?>> {

	/**
	 * 返回一个包含属性源的顺序 {@link Stream}。
	 * @since 5.1
	 */
	default Stream<PropertySource<?>> stream() {
		return StreamSupport.stream(spliterator(), false);
	}

	/**
	 * 判断是否包含指定名称的属性源。
	 * @param name 要查找的属性源名称，参考 {@linkplain PropertySource#getName()}
	 * @return 是否包含
	 */
	boolean contains(String name);

	/**
	 * 返回指定名称的属性源，未找到时返回 {@code null}。
	 * @param name 要查找的属性源名称，参考 {@linkplain PropertySource#getName()}
	 * @return 对应的属性源或 {@code null}
	 */
	@Nullable
	PropertySource<?> get(String name);

}
