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

package org.springframework.validation.annotation;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;

import java.lang.annotation.Annotation;

/**
 * 用于处理验证注解的实用工具类。
 * 主要用于框架内部使用。
 *
 * @author Christoph Dreis
 * @since 5.3.7
 */
public abstract class ValidationAnnotationUtils {

	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

	/**
	 * 根据给定的注解确定任何验证提示。
	 * <p>此实现检查{@code @javax.validation.Valid}、Spring的{@link org.springframework.validation.annotation.Validated}，
	 * 以及名称以"Valid"开头的自定义注解。
	 *
	 * @param ann 注解（可能是验证注解）
	 * @return 要应用的验证提示（可能是空数组），
	 * 如果此注解不触发任何验证，则返回{@code null}
	 */
	@Nullable
	public static Object[] determineValidationHints(Annotation ann) {
		// 获取注解的类型和名称
		Class<? extends Annotation> annotationType = ann.annotationType();
		String annotationName = annotationType.getName();

		// 检查是否为 javax.validation.Valid 注解，如果是则返回空数组
		if ("javax.validation.Valid".equals(annotationName)) {
			return EMPTY_OBJECT_ARRAY;
		}

		// 使用AnnotationUtils获取 @Validated 注解，若存在，则获取其中的值并转换成验证提示
		Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
		if (validatedAnn != null) {
			Object hints = validatedAnn.value();
			return convertValidationHints(hints);
		}

		// 如果注解的简单类名以 "Valid" 开头，获取该注解的值并转换成验证提示
		if (annotationType.getSimpleName().startsWith("Valid")) {
			Object hints = AnnotationUtils.getValue(ann);
			return convertValidationHints(hints);
		}

		// 如果以上条件均不满足，则返回null
		return null;

	}

	/**
	 * 将验证提示转换为对象数组的私有静态方法。
	 *
	 * @param hints 待转换的验证提示对象
	 * @return 转换后的对象数组（可能为空数组）
	 */
	private static Object[] convertValidationHints(@Nullable Object hints) {
		// 如果验证提示为null，则返回空数组
		if (hints == null) {
			return EMPTY_OBJECT_ARRAY;
		}

		// 如果验证提示是对象数组，则直接转换并返回
		return (hints instanceof Object[] ? (Object[]) hints : new Object[]{hints});
	}

}
