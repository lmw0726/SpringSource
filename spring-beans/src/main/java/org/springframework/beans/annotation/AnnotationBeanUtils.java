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

package org.springframework.beans.annotation;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringValueResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 用于以 JavaBeans 风格处理注解的一般工具方法。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated 自 5.2 起，建议使用自定义注解属性处理替代
 */
@Deprecated
public abstract class AnnotationBeanUtils {

	/**
	 * 将指定 {@link Annotation} 的属性复制到目标 bean 实例中。
	 * {@code excludedProperties} 中定义的属性将不会被复制。
	 * @param ann 要复制的注解
	 * @param bean 目标 bean 实例
	 * @param excludedProperties 要排除的属性名称（可选）
	 * @see org.springframework.beans.BeanWrapper
	 */
	public static void copyPropertiesToBean(Annotation ann, Object bean, String... excludedProperties) {
		copyPropertiesToBean(ann, bean, null, excludedProperties);
	}

	/**
	 * 将指定 {@link Annotation} 的属性复制到目标 bean 实例中。
	 * {@code excludedProperties} 中定义的属性将不会被复制。
	 * <p>可以指定一个值解析器（valueResolver）来解析属性值中的占位符，例如。
	 * @param ann 要复制的注解
	 * @param bean 目标 bean 实例
	 * @param valueResolver 用于后处理 String 属性值的解析器（可以为 {@code null}）
	 * @param excludedProperties 要排除的属性名称（可选）
	 * @see org.springframework.beans.BeanWrapper
	 */
	public static void copyPropertiesToBean(Annotation ann, Object bean, @Nullable StringValueResolver valueResolver,
			String... excludedProperties) {

		Set<String> excluded = (excludedProperties.length == 0 ? Collections.emptySet() :
				new HashSet<>(Arrays.asList(excludedProperties)));
		Method[] annotationProperties = ann.annotationType().getDeclaredMethods();
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(bean);
		for (Method annotationProperty : annotationProperties) {
			String propertyName = annotationProperty.getName();
			if (!excluded.contains(propertyName) && bw.isWritableProperty(propertyName)) {
				Object value = ReflectionUtils.invokeMethod(annotationProperty, ann);
				if (valueResolver != null && value instanceof String) {
					value = valueResolver.resolveStringValue((String) value);
				}
				bw.setPropertyValue(propertyName, value);
			}
		}
	}

}
