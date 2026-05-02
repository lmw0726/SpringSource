/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aop.support.annotation;

import java.lang.annotation.Annotation;

import org.springframework.aop.ClassFilter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 简单的 ClassFilter，用于查找指定注解是否存在于类上。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see AnnotationMatchingPointcut
 */
public class AnnotationClassFilter implements ClassFilter {

	private final Class<? extends Annotation> annotationType;

	private final boolean checkInherited;


	/**
	 * 为给定注解类型创建新的 AnnotationClassFilter。
	 * @param annotationType 要查找的注解类型
	 */
	public AnnotationClassFilter(Class<? extends Annotation> annotationType) {
		this(annotationType, false);
	}

	/**
	 * 为给定注解类型创建新的 AnnotationClassFilter。
	 * @param annotationType 要查找的注解类型
	 * @param checkInherited 是否还检查超类和接口，以及该注解类型的元注解
	 * （即是否使用 {@link AnnotatedElementUtils#hasAnnotation} 语义，
	 * 而不是标准 Java {@link Class#isAnnotationPresent}）
	 */
	public AnnotationClassFilter(Class<? extends Annotation> annotationType, boolean checkInherited) {
		Assert.notNull(annotationType, "Annotation type must not be null");
		this.annotationType = annotationType;
		this.checkInherited = checkInherited;
	}


	@Override
	public boolean matches(Class<?> clazz) {
		return (this.checkInherited ? AnnotatedElementUtils.hasAnnotation(clazz, this.annotationType) :
				clazz.isAnnotationPresent(this.annotationType));
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AnnotationClassFilter)) {
			return false;
		}
		AnnotationClassFilter otherCf = (AnnotationClassFilter) other;
		return (this.annotationType.equals(otherCf.annotationType) && this.checkInherited == otherCf.checkInherited);
	}

	@Override
	public int hashCode() {
		return this.annotationType.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.annotationType;
	}

}
