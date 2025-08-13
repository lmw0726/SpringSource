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
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link InvocationHandler}，用于 Spring 已**合成**（即包装在动态代理中）的 {@link Annotation}，
 * 具有附加功能，例如属性别名处理。
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 5.2
 * @param <A> 注解类型
 * @see Annotation
 * @see AnnotationUtils#synthesizeAnnotation(Annotation, AnnotatedElement)
 */
final class SynthesizedMergedAnnotationInvocationHandler<A extends Annotation> implements InvocationHandler {

	private final MergedAnnotation<?> annotation;

	private final Class<A> type;

	private final AttributeMethods attributes;

	private final Map<String, Object> valueCache = new ConcurrentHashMap<>(8);

	@Nullable
	private volatile Integer hashCode;

	@Nullable
	private volatile String string;


	private SynthesizedMergedAnnotationInvocationHandler(MergedAnnotation<A> annotation, Class<A> type) {
		Assert.notNull(annotation, "MergedAnnotation must not be null");
		Assert.notNull(type, "Type must not be null");
		Assert.isTrue(type.isAnnotation(), "Type must be an annotation");
		this.annotation = annotation;
		this.type = type;
		this.attributes = AttributeMethods.forAnnotationType(type);
	}


	@Override
	public Object invoke(Object proxy, Method method, Object[] args) {
		if (ReflectionUtils.isEqualsMethod(method)) {
			return annotationEquals(args[0]);
		}
		if (ReflectionUtils.isHashCodeMethod(method)) {
			return annotationHashCode();
		}
		if (ReflectionUtils.isToStringMethod(method)) {
			return annotationToString();
		}
		if (isAnnotationTypeMethod(method)) {
			return this.type;
		}
		if (this.attributes.indexOf(method.getName()) != -1) {
			return getAttributeValue(method);
		}
		throw new AnnotationConfigurationException(String.format(
				"Method [%s] is unsupported for synthesized annotation type [%s]", method, this.type));
	}

	private boolean isAnnotationTypeMethod(Method method) {
		return (method.getName().equals("annotationType") && method.getParameterCount() == 0);
	}

	/**
	 * 有关所需算法的定义，请参见 {@link Annotation#equals(Object)}。
	 * @param other 要比较的其他对象
	 */
	private boolean annotationEquals(Object other) {
		if (this == other) {
			return true;
		}
		if (!this.type.isInstance(other)) {
			return false;
		}
		for (int i = 0; i < this.attributes.size(); i++) {
			Method attribute = this.attributes.get(i);
			Object thisValue = getAttributeValue(attribute);
			Object otherValue = ReflectionUtils.invokeMethod(attribute, other);
			if (!ObjectUtils.nullSafeEquals(thisValue, otherValue)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 有关所需算法的定义，请参见 {@link Annotation#hashCode()}。
	 */
	private int annotationHashCode() {
		Integer hashCode = this.hashCode;
		if (hashCode == null) {
			hashCode = computeHashCode();
			this.hashCode = hashCode;
		}
		return hashCode;
	}

	private Integer computeHashCode() {
		int hashCode = 0;
		for (int i = 0; i < this.attributes.size(); i++) {
			Method attribute = this.attributes.get(i);
			Object value = getAttributeValue(attribute);
			hashCode += (127 * attribute.getName().hashCode()) ^ getValueHashCode(value);
		}
		return hashCode;
	}

	private int getValueHashCode(Object value) {
		// 使用 Arrays.hashCode(...)，因为 Spring 的 ObjectUtils 不符合 Annotation#hashCode() 中指定的要求。
		if (value instanceof boolean[]) {
			return Arrays.hashCode((boolean[]) value);
		}
		if (value instanceof byte[]) {
			return Arrays.hashCode((byte[]) value);
		}
		if (value instanceof char[]) {
			return Arrays.hashCode((char[]) value);
		}
		if (value instanceof double[]) {
			return Arrays.hashCode((double[]) value);
		}
		if (value instanceof float[]) {
			return Arrays.hashCode((float[]) value);
		}
		if (value instanceof int[]) {
			return Arrays.hashCode((int[]) value);
		}
		if (value instanceof long[]) {
			return Arrays.hashCode((long[]) value);
		}
		if (value instanceof short[]) {
			return Arrays.hashCode((short[]) value);
		}
		if (value instanceof Object[]) {
			return Arrays.hashCode((Object[]) value);
		}
		return value.hashCode();
	}

	private String annotationToString() {
		String string = this.string;
		if (string == null) {
			StringBuilder builder = new StringBuilder("@").append(getName(this.type)).append('(');
			for (int i = 0; i < this.attributes.size(); i++) {
				Method attribute = this.attributes.get(i);
				if (i > 0) {
					builder.append(", ");
				}
				builder.append(attribute.getName());
				builder.append('=');
				builder.append(toString(getAttributeValue(attribute)));
			}
			builder.append(')');
			string = builder.toString();
			this.string = string;
		}
		return string;
	}

	/**
	 * 此方法目前未解决以下问题，我们可能会在稍后解决。
	 *
	 * <ul>
	 * <li>字符或字符串字面量中的非 ASCII、不可见和不可打印字符未转义。</li>
	 * <li>浮点和双精度值的格式未考虑值是否不是数字 (NaN) 或无穷大。</li>
	 * </ul>
	 * @param value 要格式化的属性值
	 * @return 格式化的字符串表示形式
	 */
	private String toString(Object value) {
		if (value instanceof String) {
			return '"' + value.toString() + '"';
		}
		if (value instanceof Character) {
			return '\'' + value.toString() + '\'';
		}
		if (value instanceof Byte) {
			return String.format("(byte) 0x%02X", value);
		}
		if (value instanceof Long) {
			return Long.toString(((Long) value)) + 'L';
		}
		if (value instanceof Float) {
			return Float.toString(((Float) value)) + 'f';
		}
		if (value instanceof Double) {
			return Double.toString(((Double) value)) + 'd';
		}
		if (value instanceof Enum) {
			return ((Enum<?>) value).name();
		}
		if (value instanceof Class) {
			return getName((Class<?>) value) + ".class";
		}
		if (value.getClass().isArray()) {
			StringBuilder builder = new StringBuilder("{");
			for (int i = 0; i < Array.getLength(value); i++) {
				if (i > 0) {
					builder.append(", ");
				}
				builder.append(toString(Array.get(value, i)));
			}
			builder.append('}');
			return builder.toString();
		}
		return String.valueOf(value);
	}

	private Object getAttributeValue(Method method) {
		Object value = this.valueCache.computeIfAbsent(method.getName(), attributeName -> {
			Class<?> type = ClassUtils.resolvePrimitiveIfNecessary(method.getReturnType());
			return this.annotation.getValue(attributeName, type).orElseThrow(
					() -> new NoSuchElementException("No value found for attribute named '" + attributeName +
							"' in merged annotation " + this.annotation.getType().getName()));
		});

		// 克隆非空数组，以便用户不能更改我们缓存中值的内容。
		if (value.getClass().isArray() && Array.getLength(value) > 0) {
			value = cloneArray(value);
		}

		return value;
	}

	/**
	 * 克隆提供的数组，确保保留原始组件类型。
	 * @param array 要克隆的数组
	 */
	private Object cloneArray(Object array) {
		if (array instanceof boolean[]) {
			return ((boolean[]) array).clone();
		}
		if (array instanceof byte[]) {
			return ((byte[]) array).clone();
		}
		if (array instanceof char[]) {
			return ((char[]) array).clone();
		}
		if (array instanceof double[]) {
			return ((double[]) array).clone();
		}
		if (array instanceof float[]) {
			return ((float[]) array).clone();
		}
		if (array instanceof int[]) {
			return ((int[]) array).clone();
		}
		if (array instanceof long[]) {
			return ((long[]) array).clone();
		}
		if (array instanceof short[]) {
			return ((short[]) array).clone();
		}

		// else
		return ((Object[]) array).clone();
	}

	@SuppressWarnings("unchecked")
	static <A extends Annotation> A createProxy(MergedAnnotation<A> annotation, Class<A> type) {
		ClassLoader classLoader = type.getClassLoader();
		InvocationHandler handler = new SynthesizedMergedAnnotationInvocationHandler<>(annotation, type);
		Class<?>[] interfaces = isVisible(classLoader, SynthesizedAnnotation.class) ?
				new Class<?>[] {type, SynthesizedAnnotation.class} : new Class<?>[] {type};
		return (A) Proxy.newProxyInstance(classLoader, interfaces, handler);
	}

	private static String getName(Class<?> clazz) {
		String canonicalName = clazz.getCanonicalName();
		return (canonicalName != null ? canonicalName : clazz.getName());
	}


	private static boolean isVisible(ClassLoader classLoader, Class<?> interfaceClass) {
		if (classLoader == interfaceClass.getClassLoader()) {
			return true;
		}
		try {
			return Class.forName(interfaceClass.getName(), false, classLoader) == interfaceClass;
		}
		catch (ClassNotFoundException ex) {
			return false;
		}
	}

}
