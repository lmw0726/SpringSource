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

/**
 * 可用于处理注解的回调接口。
 *
 * @author Phillip Webb
 * @since 5.2
 * @param <C> 上下文类型
 * @param <R> 结果类型
 * @see AnnotationsScanner
 * @see TypeMappedAnnotations
 */
@FunctionalInterface
interface AnnotationsProcessor<C, R> {

	/**
	 * 在即将处理聚合时调用。此方法可以返回一个非 {@code null} 结果以短路任何进一步的处理。
	 * @param context 与处理器相关的上下文信息
	 * @param aggregateIndex 即将处理的聚合索引
	 * @return 如果不需要进一步处理，则为非 {@code null} 结果
	 */
	@Nullable
	default R doWithAggregate(C context, int aggregateIndex) {
		return null;
	}

	/**
	 * 在可以处理注解数组时调用。此方法可以返回一个非 {@code null} 结果以短路任何进一步的处理。
	 * @param context 与处理器相关的上下文信息
	 * @param aggregateIndex 提供的注解的聚合索引
	 * @param source 注解的原始源，如果已知
	 * @param annotations 要处理的注解（此数组可能包含 {@code null} 元素）
	 * @return 如果不需要进一步处理，则为非 {@code null} 结果
	 */
	@Nullable
	R doWithAnnotations(C context, int aggregateIndex, @Nullable Object source, Annotation[] annotations);

	/**
	 * 获取要返回的最终结果。默认情况下，此方法返回最后一个处理结果。
	 * @param result 最后一个提前退出结果，如果无则为 {@code null}
	 * @return 将返回给调用者的最终结果
	 */
	@Nullable
	default R finish(@Nullable R result) {
		return result;
	}

}
