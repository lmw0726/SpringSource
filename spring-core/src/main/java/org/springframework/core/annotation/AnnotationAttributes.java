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
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link LinkedHashMap} 的子类，代表由 {@link AnnotationUtils}、
 * {@link AnnotatedElementUtils} 以及 Spring 基于反射和 ASM 的
 * {@link org.springframework.core.type.AnnotationMetadata} 实现所读取的注解属性
 * *键值对*。
 *
 * <p>提供“伪具体化”以避免调用代码中泛型 Map 的噪音，并提供便捷方法以类型安全的方式查找注解属性。
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 3.1.1
 * @see AnnotationUtils#getAnnotationAttributes
 * @see AnnotatedElementUtils
 */
@SuppressWarnings("serial")
public class AnnotationAttributes extends LinkedHashMap<String, Object> {

	private static final String UNKNOWN = "unknown";

	@Nullable
	private final Class<? extends Annotation> annotationType;

	final String displayName;

	boolean validated = false;


	/**
	 * 创建一个新的、空的 {@link AnnotationAttributes} 实例。
	 */
	public AnnotationAttributes() {
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * 创建一个新的、空的 {@link AnnotationAttributes} 实例，具有给定的初始容量以优化性能。
	 * @param initialCapacity 底层 map 的初始大小
	 */
	public AnnotationAttributes(int initialCapacity) {
		super(initialCapacity);
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * 创建一个新的 {@link AnnotationAttributes} 实例，包装提供的
	 * map 及其所有的 *键值对*。
	 * @param map 注解属性 *键值对* 的原始来源
	 * @see #fromMap(Map)
	 */
	public AnnotationAttributes(Map<String, Object> map) {
		super(map);
		this.annotationType = null;
		this.displayName = UNKNOWN;
	}

	/**
	 * 创建一个新的 {@link AnnotationAttributes} 实例，包装提供的
	 * map 及其所有的 *键值对*。
	 * @param other 注解属性 *键值对* 的原始来源
	 * @see #fromMap(Map)
	 */
	public AnnotationAttributes(AnnotationAttributes other) {
		super(other);
		this.annotationType = other.annotationType;
		this.displayName = other.displayName;
		this.validated = other.validated;
	}

	/**
	 * 为指定的 {@code annotationType} 创建一个新的、空的 {@link AnnotationAttributes} 实例。
	 * @param annotationType 此 {@code AnnotationAttributes} 实例所代表的注解类型；永不为 {@code null}
	 * @since 4.2
	 */
	public AnnotationAttributes(Class<? extends Annotation> annotationType) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = annotationType;
		this.displayName = annotationType.getName();
	}

	/**
	 * 为指定的 {@code annotationType} 创建一个可能已经验证过的新的、空的
	 * {@link AnnotationAttributes} 实例。
	 * @param annotationType 此 {@code AnnotationAttributes} 实例所代表的注解类型；永不为 {@code null}
	 * @param validated 属性是否被认为是已验证的
	 * @since 5.2
	 */
	AnnotationAttributes(Class<? extends Annotation> annotationType, boolean validated) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = annotationType;
		this.displayName = annotationType.getName();
		this.validated = validated;
	}

	/**
	 * 为指定的 {@code annotationType} 创建一个新的、空的 {@link AnnotationAttributes} 实例。
	 * @param annotationType 此 {@code AnnotationAttributes} 实例所代表的注解类型名称；永不为 {@code null}
	 * @param classLoader 尝试加载注解类型的 ClassLoader，
	 * 或为 {@code null} 以仅存储注解类型名称
	 * @since 4.3.2
	 */
	public AnnotationAttributes(String annotationType, @Nullable ClassLoader classLoader) {
		Assert.notNull(annotationType, "'annotationType' must not be null");
		this.annotationType = getAnnotationType(annotationType, classLoader);
		this.displayName = annotationType;
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private static Class<? extends Annotation> getAnnotationType(String annotationType, @Nullable ClassLoader classLoader) {
		if (classLoader != null) {
			try {
				return (Class<? extends Annotation>) classLoader.loadClass(annotationType);
			}
			catch (ClassNotFoundException ex) {
				// 注解类无法解析
			}
		}
		return null;
	}


	/**
	 * 获取此 {@code AnnotationAttributes} 所代表的注解类型。
	 * @return 注解类型，如果未知则返回 {@code null}
	 * @since 4.2
	 */
	@Nullable
	public Class<? extends Annotation> annotationType() {
		return this.annotationType;
	}

	/**
	 * 以字符串形式获取在指定 {@code attributeName} 下存储的值。
	 * @param attributeName 要获取的属性名称；
	 * 永不为 {@code null} 或空
	 * @return 值
	 * @throws IllegalArgumentException 如果属性不存在或
	 * 如果其类型不是预期类型
	 */
	public String getString(String attributeName) {
		return getRequiredAttribute(attributeName, String.class);
	}

	/**
	 * 获取指定 {@code attributeName} 下存储的值，并将其作为字符串数组返回。
	 * <p>如果存储的值是单个字符串，会在返回前将其包装成单元素数组。
	 * @param attributeName 要获取的属性名；不能为空或 null
	 * @return 字符串数组
	 * @throws IllegalArgumentException 如果属性不存在，或不是期望的类型
	 */
	public String[] getStringArray(String attributeName) {
		return getRequiredAttribute(attributeName, String[].class);
	}

	/**
	 * 获取指定 {@code attributeName} 下存储的值，并将其作为布尔值返回。
	 * @param attributeName 要获取的属性名；不能为空或 null
	 * @return 布尔值
	 * @throws IllegalArgumentException 如果属性不存在，或不是期望的类型
	 */
	public boolean getBoolean(String attributeName) {
		return getRequiredAttribute(attributeName, Boolean.class);
	}

	/**
	 * 获取指定 {@code attributeName} 下存储的值，并将其作为数字类型返回。
	 * @param attributeName 要获取的属性名；不能为空或 null
	 * @return 数字值
	 * @throws IllegalArgumentException 如果属性不存在，或不是期望的类型
	 */
	@SuppressWarnings("unchecked")
	public <N extends Number> N getNumber(String attributeName) {
		return (N) getRequiredAttribute(attributeName, Number.class);
	}

	/**
	 * 获取指定 {@code attributeName} 下存储的值，并将其作为枚举类型返回。
	 * @param attributeName 要获取的属性名；不能为空或 null
	 * @return 枚举值
	 * @throws IllegalArgumentException 如果属性不存在，或不是期望的类型
	 */
	@SuppressWarnings("unchecked")
	public <E extends Enum<?>> E getEnum(String attributeName) {
		return (E) getRequiredAttribute(attributeName, Enum.class);
	}

	/**
	 * 获取指定 {@code attributeName} 对应的值，并将其作为 Class 类型返回。
	 * @param attributeName 要获取的属性名；不能为空或 null
	 * @return 属性值（Class 类型）
	 * @throws IllegalArgumentException 如果属性不存在，或不是期望的类型
	 */
	@SuppressWarnings("unchecked")
	public <T> Class<? extends T> getClass(String attributeName) {
		return getRequiredAttribute(attributeName, Class.class);
	}

	/**
	 * 获取指定 {@code attributeName} 对应的值，并将其作为 Class 数组返回。
	 * <p>如果存储的值是单个 Class，会被包装成单元素数组后返回。
	 * @param attributeName 要获取的属性名；不能为空或 null
	 * @return 属性值（Class[] 类型）
	 * @throws IllegalArgumentException 如果属性不存在，或不是期望的类型
	 */
	public Class<?>[] getClassArray(String attributeName) {
		return getRequiredAttribute(attributeName, Class[].class);
	}

	/**
	 * 获取指定 {@code attributeName} 对应的 {@link AnnotationAttributes}。
	 * <p>注意：如果你需要的是实际的注解实例，请使用 {@link #getAnnotation(String, Class)}。
	 * @param attributeName 要获取的属性名；不能为空或 null
	 * @return 对应的 {@code AnnotationAttributes}
	 * @throws IllegalArgumentException 如果属性不存在，或不是期望的类型
	 */
	public AnnotationAttributes getAnnotation(String attributeName) {
		return getRequiredAttribute(attributeName, AnnotationAttributes.class);
	}

	/**
	 * 获取指定 {@code attributeName} 对应的注解实例。
	 * @param attributeName 要获取的属性名；不能为空或 null
	 * @param annotationType 期望的注解类型；不能为空
	 * @return 注解实例
	 * @throws IllegalArgumentException 如果属性不存在，或不是期望的类型
	 * @since 4.2
	 */
	public <A extends Annotation> A getAnnotation(String attributeName, Class<A> annotationType) {
		return getRequiredAttribute(attributeName, annotationType);
	}

	/**
	 * 获取指定 {@code attributeName} 对应的 {@link AnnotationAttributes} 数组。
	 * <p>如果存储的值是单个 {@code AnnotationAttributes} 实例，会被包装成单元素数组后返回。
	 * <p>注意：如果你需要的是实际的注解数组，请使用 {@link #getAnnotationArray(String, Class)}。
	 * @param attributeName 要获取的属性名；不能为空或 null
	 * @return {@code AnnotationAttributes[]} 数组
	 * @throws IllegalArgumentException 如果属性不存在，或不是期望的类型
	 */
	public AnnotationAttributes[] getAnnotationArray(String attributeName) {
		return getRequiredAttribute(attributeName, AnnotationAttributes[].class);
	}

	/**
	 * 获取存储在指定 {@code attributeName} 下、类型为 {@code annotationType} 的数组。
	 * <p>如果在指定 {@code attributeName} 下存储的值是一个 {@code Annotation}，
	 * 则会在返回前将其包装成单元素数组。
	 * @param attributeName 要获取的属性名；不能为空或 {@code null}
	 * @param annotationType 期望的注解类型；不能为空
	 * @return 注解数组
	 * @throws IllegalArgumentException 如果属性不存在，或不是期望的类型
	 * @since 4.2
	 */
	@SuppressWarnings("unchecked")
	public <A extends Annotation> A[] getAnnotationArray(String attributeName, Class<A> annotationType) {
		Object array = Array.newInstance(annotationType, 0);
		return (A[]) getRequiredAttribute(attributeName, array.getClass());
	}

	/**
	 * 获取存储在指定 {@code attributeName} 下的值，并确保该值是 {@code expectedType} 类型。
	 * <p>如果 {@code expectedType} 是数组类型，而指定 {@code attributeName} 下的值
	 * 是该数组类型的单个元素，则会在返回前将该元素包装成一个相应类型的单元素数组。
	 * @param attributeName 要获取的属性名；不能为空或 {@code null}
	 * @param expectedType 期望的类型；不能为空
	 * @return 属性值
	 * @throws IllegalArgumentException 如果属性不存在，或不是期望的类型
	 */
	@SuppressWarnings("unchecked")
	private <T> T getRequiredAttribute(String attributeName, Class<T> expectedType) {
		Assert.hasText(attributeName, "'attributeName' must not be null or empty");
		Object value = get(attributeName);
		assertAttributePresence(attributeName, value);
		assertNotException(attributeName, value);
		if (!expectedType.isInstance(value) && expectedType.isArray() &&
				expectedType.getComponentType().isInstance(value)) {
			Object array = Array.newInstance(expectedType.getComponentType(), 1);
			Array.set(array, 0, value);
			value = array;
		}
		assertAttributeType(attributeName, value, expectedType);
		return (T) value;
	}

	private void assertAttributePresence(String attributeName, Object attributeValue) {
		Assert.notNull(attributeValue, () -> String.format(
				"Attribute '%s' not found in attributes for annotation [%s]",
				attributeName, this.displayName));
	}

	private void assertNotException(String attributeName, Object attributeValue) {
		if (attributeValue instanceof Throwable) {
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' for annotation [%s] was not resolvable due to exception [%s]",
					attributeName, this.displayName, attributeValue), (Throwable) attributeValue);
		}
	}

	private void assertAttributeType(String attributeName, Object attributeValue, Class<?> expectedType) {
		if (!expectedType.isInstance(attributeValue)) {
			throw new IllegalArgumentException(String.format(
					"Attribute '%s' is of type %s, but %s was expected in attributes for annotation [%s]",
					attributeName, attributeValue.getClass().getSimpleName(), expectedType.getSimpleName(),
					this.displayName));
		}
	}

	@Override
	public String toString() {
		Iterator<Map.Entry<String, Object>> entries = entrySet().iterator();
		StringBuilder sb = new StringBuilder("{");
		while (entries.hasNext()) {
			Map.Entry<String, Object> entry = entries.next();
			sb.append(entry.getKey());
			sb.append('=');
			sb.append(valueToString(entry.getValue()));
			if (entries.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append('}');
		return sb.toString();
	}

	private String valueToString(Object value) {
		if (value == this) {
			return "(this Map)";
		}
		if (value instanceof Object[]) {
			return "[" + StringUtils.arrayToDelimitedString((Object[]) value, ", ") + "]";
		}
		return String.valueOf(value);
	}


	/**
	 * 根据给定的 Map 返回一个 {@link AnnotationAttributes} 实例。
	 * <p>如果该 Map 已经是 {@code AnnotationAttributes} 实例，
	 * 则会直接进行类型转换并立即返回，而不会创建新实例。
	 * 否则将通过调用 {@link #AnnotationAttributes(Map)} 构造方法，
	 * 使用提供的 Map 创建一个新的实例。
	 * @param map 注解属性键值对的原始来源
	 */
	@Nullable
	public static AnnotationAttributes fromMap(@Nullable Map<String, Object> map) {
		if (map == null) {
			return null;
		}
		if (map instanceof AnnotationAttributes) {
			return (AnnotationAttributes) map;
		}
		return new AnnotationAttributes(map);
	}

}
