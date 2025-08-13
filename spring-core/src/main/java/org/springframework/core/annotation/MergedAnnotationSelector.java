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

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;

/**
 * 策略接口，用于在两个 {@link MergedAnnotation} 实例之间进行选择。
 *
 * @author Phillip Webb
 * @since 5.2
 * @param <A> 注解类型
 * @see MergedAnnotationSelectors
 */
@FunctionalInterface
public interface MergedAnnotationSelector<A extends Annotation> {

	/**
	 * 确定现有注解是否已知为最佳候选者，以及是否可以跳过任何后续选择。
	 * @param annotation 要检查的注解
	 * @return 如果注解已知为最佳候选者，则返回 {@code true}
	 */
	default boolean isBestCandidate(MergedAnnotation<A> annotation) {
		return false;
	}

	/**
	 * 选择应该使用的注解。
	 * @param existing 早期结果返回的现有注解
	 * @param candidate 可能更合适的候选注解
	 * @return 从 {@code existing} 或 {@code candidate} 中最合适的注解
	 */
	MergedAnnotation<A> select(MergedAnnotation<A> existing, MergedAnnotation<A> candidate);

}
