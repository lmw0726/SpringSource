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

package org.springframework.core.annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

/**
 * 提供一种快速访问 {@link Annotation} 属性方法的方式，
 * 具有一致的顺序以及一些有用的实用方法。
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 */
final class AttributeMethods {

	static final AttributeMethods NONE = new AttributeMethods(null, new Method[0]);


	private static final Map<Class<? extends Annotation>, AttributeMethods> cache =
			new ConcurrentReferenceHashMap<>();

	private static final Comparator<Method> methodComparator = (m1, m2) -> {
		if (m1 != null && m2 != null) {
			return m1.getName().compareTo(m2.getName());
		}
		return m1 != null ? -1 : 1;
	};


	@Nullable
	private final Class<? extends Annotation> annotationType;

	private final Method[] attributeMethods;

	private final boolean[] canThrowTypeNotPresentException;

	private final boolean hasDefaultValueMethod;

	private final boolean hasNestedAnnotation;


	private AttributeMethods(@Nullable Class<? extends Annotation> annotationType, Method[] attributeMethods) {
		this.annotationType = annotationType;
		this.attributeMethods = attributeMethods;
		this.canThrowTypeNotPresentException = new boolean[attributeMethods.length];
		boolean foundDefaultValueMethod = false;
		boolean foundNestedAnnotation = false;
		for (int i = 0; i < attributeMethods.length; i++) {
			Method method = this.attributeMethods[i];
			Class<?> type = method.getReturnType();
			if (!foundDefaultValueMethod && (method.getDefaultValue() != null)) {
				foundDefaultValueMethod = true;
			}
			if (!foundNestedAnnotation && (type.isAnnotation() || (type.isArray() && type.getComponentType().isAnnotation()))) {
				foundNestedAnnotation = true;
			}
			ReflectionUtils.makeAccessible(method);
			this.canThrowTypeNotPresentException[i] = (type == Class.class || type == Class[].class || type.isEnum());
		}
		this.hasDefaultValueMethod = foundDefaultValueMethod;
		this.hasNestedAnnotation = foundNestedAnnotation;
	}


	/**
	 * 确定此实例是否只包含一个名为 {@code value} 的属性。
	 * @return 如果只有一个 value 属性，则返回 {@code true}
	 */
	boolean hasOnlyValueAttribute() {
		return (this.attributeMethods.length == 1 &&
				MergedAnnotation.VALUE.equals(this.attributeMethods[0].getName()));
	}


	/**
	 * 确定给定注解中的值是否可以安全访问，而不会导致
	 * 任何 {@link TypeNotPresentException TypeNotPresentExceptions}。
	 * @param annotation 要检查的注解
	 * @return 如果所有值都存在，则返回 {@code true}
	 * @see #validate(Annotation)
	 */
	boolean isValid(Annotation annotation) {
		assertAnnotation(annotation);
		for (int i = 0; i < size(); i++) {
			if (canThrowTypeNotPresentException(i)) {
				try {
					get(i).invoke(annotation);
				}
				catch (Throwable ex) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 检查给定注解中的值是否可以安全访问，而不会导致
	 * 任何 {@link TypeNotPresentException TypeNotPresentExceptions}。特别是，
	 * 此方法旨在解决 Google App Engine 中 {@code Class} 值的此类异常延迟发生的情况
	 * （而不是更典型的早期 {@code Class.getAnnotations()} 失败）。
	 * @param annotation 要验证的注解
	 * @throws IllegalStateException 如果声明的 {@code Class} 属性无法读取
	 * @see #isValid(Annotation)
	 */
	void validate(Annotation annotation) {
		assertAnnotation(annotation);
		for (int i = 0; i < size(); i++) {
			if (canThrowTypeNotPresentException(i)) {
				try {
					get(i).invoke(annotation);
				}
				catch (Throwable ex) {
					throw new IllegalStateException("Could not obtain annotation attribute value for " +
							get(i).getName() + " declared on " + annotation.annotationType(), ex);
				}
			}
		}
	}

	private void assertAnnotation(Annotation annotation) {
		Assert.notNull(annotation, "Annotation must not be null");
		if (this.annotationType != null) {
			Assert.isInstanceOf(this.annotationType, annotation);
		}
	}

	/**
	 * 获取具有指定名称的属性，如果不存在匹配的属性，则返回 {@code null}。
	 * @param name 要查找的属性名称
	 * @return 属性方法或 {@code null}
	 */
	@Nullable
	Method get(String name) {
		int index = indexOf(name);
		return index != -1 ? this.attributeMethods[index] : null;
	}

	/**
	 * 获取指定索引处的属性。
	 * @param index 要返回的属性的索引
	 * @return 属性方法
	 * @throws IndexOutOfBoundsException 如果索引超出范围
	 * (<tt>index &lt; 0 || index &gt;= size()</tt>)
	 */
	Method get(int index) {
		return this.attributeMethods[index];
	}

	/**
	 * 确定指定索引处的属性在访问时是否可能抛出 {@link TypeNotPresentException}。
	 * @param index 要检查的属性的索引
	 * @return 如果属性可能抛出 {@link TypeNotPresentException}，则返回 {@code true}
	 */
	boolean canThrowTypeNotPresentException(int index) {
		return this.canThrowTypeNotPresentException[index];
	}

	/**
	 * 获取具有指定名称的属性的索引，如果没有具有该名称的属性，则返回 {@code -1}。
	 * @param name 要查找的名称
	 * @return 属性的索引，如果未找到则返回 {@code -1}
	 */
	int indexOf(String name) {
		for (int i = 0; i < this.attributeMethods.length; i++) {
			if (this.attributeMethods[i].getName().equals(name)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 获取指定属性的索引，如果此集合中没有该属性，则返回 {@code -1}。
	 * @param attribute 要查找的属性
	 * @return 属性的索引，如果未找到则返回 {@code -1}
	 */
	int indexOf(Method attribute) {
		for (int i = 0; i < this.attributeMethods.length; i++) {
			if (this.attributeMethods[i].equals(attribute)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 获取此集合中的属性数量。
	 * @return 属性数量
	 */
	int size() {
		return this.attributeMethods.length;
	}

	/**
	 * 确定至少一个属性方法是否具有默认值。
	 * @return 如果至少有一个属性方法具有默认值，则返回 {@code true}
	 */
	boolean hasDefaultValueMethod() {
		return this.hasDefaultValueMethod;
	}

	/**
	 * 确定至少一个属性方法是否是嵌套注解。
	 * @return 如果至少有一个属性方法具有嵌套注解类型，则返回 {@code true}
	 */
	boolean hasNestedAnnotation() {
		return this.hasNestedAnnotation;
	}


	/**
	 * 获取给定注解类型的属性方法。
	 * @param annotationType 注解类型
	 * @return 注解类型的属性方法
	 */
	static AttributeMethods forAnnotationType(@Nullable Class<? extends Annotation> annotationType) {
		if (annotationType == null) {
			return NONE;
		}
		return cache.computeIfAbsent(annotationType, AttributeMethods::compute);
	}

	private static AttributeMethods compute(Class<? extends Annotation> annotationType) {
		Method[] methods = annotationType.getDeclaredMethods();
		int size = methods.length;
		for (int i = 0; i < methods.length; i++) {
			if (!isAttributeMethod(methods[i])) {
				methods[i] = null;
				size--;
			}
		}
		if (size == 0) {
			return NONE;
		}
		Arrays.sort(methods, methodComparator);
		Method[] attributeMethods = Arrays.copyOf(methods, size);
		return new AttributeMethods(annotationType, attributeMethods);
	}

	private static boolean isAttributeMethod(Method method) {
		return (method.getParameterCount() == 0 && method.getReturnType() != void.class);
	}

	/**
	 * 为给定的属性方法创建适合在异常消息和日志中使用的描述。
	 * @param attribute 要描述的属性
	 * @return 属性的描述
	 */
	static String describe(@Nullable Method attribute) {
		if (attribute == null) {
			return "(none)";
		}
		return describe(attribute.getDeclaringClass(), attribute.getName());
	}

	/**
	 * 为给定的属性方法创建适合在异常消息和日志中使用的描述。
	 * @param annotationType 注解类型
	 * @param attributeName 属性名称
	 * @return 属性的描述
	 */
	static String describe(@Nullable Class<?> annotationType, @Nullable String attributeName) {
		if (attributeName == null) {
			return "(none)";
		}
		String in = (annotationType != null ? " in annotation [" + annotationType.getName() + "]" : "");
		return "attribute '" + attributeName + "'" + in;
	}

}
