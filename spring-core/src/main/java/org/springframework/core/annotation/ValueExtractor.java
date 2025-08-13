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

package org.springframework.core.annotation;

import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 用于从给定源对象（通常是 {@link Annotation}、{@link Map} 或
 * {@link TypeMappedAnnotation}）中提取注解属性值的策略 API。
 *
 * @author Sam Brannen
 * @since 5.2.4
 */
@FunctionalInterface
interface ValueExtractor {

	/**
	 * 从提供的源 {@link Object} 中提取由提供的 {@link Method} 表示的注解属性。
	 */
	@Nullable
	Object extract(Method attribute, @Nullable Object object);

}
