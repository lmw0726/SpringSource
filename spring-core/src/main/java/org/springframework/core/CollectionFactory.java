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

package org.springframework.core;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

import java.util.*;

/**
 * 针对常见的 Java 和 Spring 集合类型设计的集合工厂。
 *
 * <p>主要供框架内部使用。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Oliver Gierke
 * @author Sam Brannen
 * @since 1.1.1
 */
public final class CollectionFactory {

	private static final Set<Class<?>> approximableCollectionTypes = new HashSet<>();

	private static final Set<Class<?>> approximableMapTypes = new HashSet<>();


	static {
		// 标准集合接口
		approximableCollectionTypes.add(Collection.class);
		approximableCollectionTypes.add(List.class);
		approximableCollectionTypes.add(Set.class);
		approximableCollectionTypes.add(SortedSet.class);
		approximableCollectionTypes.add(NavigableSet.class);
		approximableMapTypes.add(Map.class);
		approximableMapTypes.add(SortedMap.class);
		approximableMapTypes.add(NavigableMap.class);

		// 常用具体集合类
		approximableCollectionTypes.add(ArrayList.class);
		approximableCollectionTypes.add(LinkedList.class);
		approximableCollectionTypes.add(HashSet.class);
		approximableCollectionTypes.add(LinkedHashSet.class);
		approximableCollectionTypes.add(TreeSet.class);
		approximableCollectionTypes.add(EnumSet.class);
		approximableMapTypes.add(HashMap.class);
		approximableMapTypes.add(LinkedHashMap.class);
		approximableMapTypes.add(TreeMap.class);
		approximableMapTypes.add(EnumMap.class);
	}


	private CollectionFactory() {
	}


	/**
	 * 判断给定的集合类型是否为 <em>可近似</em> 类型，
	 * 即是否为 {@link #createApproximateCollection} 可以近似创建的类型。
	 * @param collectionType 需要判断的集合类型
	 * @return 如果是 <em>可近似</em> 类型，则返回 {@code true}
	 */
	public static boolean isApproximableCollectionType(@Nullable Class<?> collectionType) {
		return (collectionType != null && approximableCollectionTypes.contains(collectionType));
	}

	/**
	 * 为给定的集合创建最接近的集合实例。
	 * <p><strong>警告</strong>：由于参数化类型 {@code E} 并未绑定到传入的
	 * {@code collection} 中元素的具体类型，如果传入的 {@code collection} 是一个 {@link EnumSet}，
	 * 则无法保证类型安全。在这种情况下，调用者需要确保传入的
	 * {@code collection} 的元素类型是与 {@code E} 匹配的枚举类型。
	 * 另外，调用者也可以将返回值视为一个原始集合或 {@link Object} 类型的集合。
	 * @param collection 原始集合对象，可能为 {@code null}
	 * @param capacity 初始容量
	 * @return 一个新的、空的集合实例
	 * @see #isApproximableCollectionType
	 * @see java.util.LinkedList
	 * @see java.util.ArrayList
	 * @see java.util.EnumSet
	 * @see java.util.TreeSet
	 * @see java.util.LinkedHashSet
	 */
	@SuppressWarnings({"rawtypes", "unchecked", "cast"})
	public static <E> Collection<E> createApproximateCollection(@Nullable Object collection, int capacity) {
		if (collection instanceof LinkedList) {
			return new LinkedList<>();
		}
		else if (collection instanceof List) {
			return new ArrayList<>(capacity);
		}
		else if (collection instanceof EnumSet) {
			// 在 Eclipse 4.4.1 中编译需要进行类型转换。
			Collection<E> enumSet = (Collection<E>) EnumSet.copyOf((EnumSet) collection);
			enumSet.clear();
			return enumSet;
		}
		else if (collection instanceof SortedSet) {
			return new TreeSet<>(((SortedSet<E>) collection).comparator());
		}
		else {
			return new LinkedHashSet<>(capacity);
		}
	}

	/**
	 * 为给定的集合类型创建最合适的集合实例。
	 * <p>该方法委托给 {@link #createCollection(Class, Class, int)}，传入
	 * {@code null} 作为元素类型。
	 * @param collectionType 目标集合的期望类型（不允许为 {@code null}）
	 * @param capacity 初始容量
	 * @return 一个新的集合实例
	 * @throws IllegalArgumentException 如果传入的 {@code collectionType} 为 {@code null}
	 * 或者类型是 {@link EnumSet}
	 */
	public static <E> Collection<E> createCollection(Class<?> collectionType, int capacity) {
		return createCollection(collectionType, null, capacity);
	}

	/**
	 * 为给定的集合类型创建最合适的集合实例。
	 * <p><strong>警告</strong>：由于参数化类型 {@code E} 并未绑定到传入的
	 * {@code elementType}，如果期望的 {@code collectionType} 是 {@link EnumSet}，
	 * 则无法保证类型安全。在这种情况下，调用者需要确保传入的
	 * {@code elementType} 是与 {@code E} 匹配的枚举类型。
	 * 另外，调用者也可以将返回值视为一个原始集合或 {@link Object} 类型的集合。
	 * @param collectionType 目标集合的期望类型（不允许为 {@code null}）
	 * @param elementType 集合元素类型，如果未知则为 {@code null}
	 * （仅在创建 {@link EnumSet} 时相关）
	 * @param capacity 初始容量
	 * @return 一个新的集合实例
	 * @throws IllegalArgumentException 如果传入的 {@code collectionType} 为 {@code null}；
	 * 或者当期望类型是 {@link EnumSet} 但传入的 {@code elementType} 不是 {@link Enum} 子类型时
	 * @since 4.1.3
	 * @see java.util.LinkedHashSet
	 * @see java.util.ArrayList
	 * @see java.util.TreeSet
	 * @see java.util.EnumSet
	 */
	@SuppressWarnings({"unchecked", "cast"})
	public static <E> Collection<E> createCollection(Class<?> collectionType, @Nullable Class<?> elementType, int capacity) {
		Assert.notNull(collectionType, "Collection type must not be null");
		if (collectionType.isInterface()) {
			if (Set.class == collectionType || Collection.class == collectionType) {
				return new LinkedHashSet<>(capacity);
			}
			else if (List.class == collectionType) {
				return new ArrayList<>(capacity);
			}
			else if (SortedSet.class == collectionType || NavigableSet.class == collectionType) {
				return new TreeSet<>();
			}
			else {
				throw new IllegalArgumentException("Unsupported Collection interface: " + collectionType.getName());
			}
		}
		else if (EnumSet.class.isAssignableFrom(collectionType)) {
			Assert.notNull(elementType, "Cannot create EnumSet for unknown element type");
			// 在 Eclipse 4.4.1 中编译需要进行类型转换。
			return (Collection<E>) EnumSet.noneOf(asEnumType(elementType));
		}
		else {
			if (!Collection.class.isAssignableFrom(collectionType)) {
				throw new IllegalArgumentException("Unsupported Collection type: " + collectionType.getName());
			}
			try {
				return (Collection<E>) ReflectionUtils.accessibleConstructor(collectionType).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
					"Could not instantiate Collection type: " + collectionType.getName(), ex);
			}
		}
	}

	/**
	 * 判断给定的 Map 类型是否为 <em>可近似</em> 类型，
	 * 即是否为 {@link #createApproximateMap} 可以近似创建的类型。
	 * @param mapType 需要判断的 Map 类型
	 * @return 如果是 <em>可近似</em> 类型，则返回 {@code true}
	 */
	public static boolean isApproximableMapType(@Nullable Class<?> mapType) {
		return (mapType != null && approximableMapTypes.contains(mapType));
	}

	/**
	 * 为给定的 map 创建最接近的映射。
	 * <p><strong>警告</strong>：由于泛型参数类型 {@code K} 并未绑定到传入的 {@code map} 中所包含的键类型，
	 * 如果传入的 {@code map} 是一个 {@link EnumMap}，则无法保证类型安全。
	 * 在这种情况下，调用者需要确保传入 {@code map} 的键类型是与 {@code K} 类型匹配的枚举类型。
	 * 作为替代，调用者可以将返回值视为原始的 map，或以 {@link Object} 作为键的 map。
	 * @param map 原始的 map 对象，可能为 {@code null}
	 * @param capacity 初始容量
	 * @return 一个新的、空的 map 实例
	 * @see #isApproximableMapType
	 * @see java.util.EnumMap
	 * @see java.util.TreeMap
	 * @see java.util.LinkedHashMap
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <K, V> Map<K, V> createApproximateMap(@Nullable Object map, int capacity) {
		if (map instanceof EnumMap) {
			EnumMap enumMap = new EnumMap((EnumMap) map);
			enumMap.clear();
			return enumMap;
		}
		else if (map instanceof SortedMap) {
			return new TreeMap<>(((SortedMap<K, V>) map).comparator());
		}
		else {
			return new LinkedHashMap<>(capacity);
		}
	}

	/**
	 * 为给定的 map 类型创建最合适的 map。
	 * <p>委托给 {@link #createMap(Class, Class, int)}，并传入 {@code null} 的键类型。
	 * @param mapType 目标 map 的期望类型
	 * @param capacity 初始容量
	 * @return 一个新的 map 实例
	 * @throws IllegalArgumentException 如果传入的 {@code mapType} 为 {@code null} 或类型为 {@link EnumMap}
	 */
	public static <K, V> Map<K, V> createMap(Class<?> mapType, int capacity) {
		return createMap(mapType, null, capacity);
	}

	/**
	 * 为给定的 map 类型创建最合适的 map。
	 * <p><strong>警告</strong>：由于泛型参数类型 {@code K} 并未绑定到传入的 {@code keyType}，
	 * 如果期望的 {@code mapType} 是 {@link EnumMap}，则无法保证类型安全。
	 * 在这种情况下，调用者需要确保 {@code keyType} 是与 {@code K} 匹配的枚举类型。
	 * 作为替代，调用者可以将返回值视为原始的 map，或以 {@link Object} 作为键的 map。
	 * 同样地，如果期望的 {@code mapType} 是 {@link MultiValueMap}，也无法强制类型安全。
	 * @param mapType 目标 map 的期望类型（不能为 {@code null}）
	 * @param keyType map 的键类型，若未知可为 {@code null}（仅对 {@link EnumMap} 创建相关）
	 * @param capacity 初始容量
	 * @return 一个新的 map 实例
	 * @throws IllegalArgumentException 如果传入的 {@code mapType} 为 {@code null}；
	 * 或者期望的 {@code mapType} 是 {@link EnumMap} 且传入的 {@code keyType} 不是 {@link Enum} 的子类型
	 * @since 4.1.3
	 * @see java.util.LinkedHashMap
	 * @see java.util.TreeMap
	 * @see org.springframework.util.LinkedMultiValueMap
	 * @see java.util.EnumMap
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <K, V> Map<K, V> createMap(Class<?> mapType, @Nullable Class<?> keyType, int capacity) {
		Assert.notNull(mapType, "Map type must not be null");
		if (mapType.isInterface()) {
			if (Map.class == mapType) {
				return new LinkedHashMap<>(capacity);
			}
			else if (SortedMap.class == mapType || NavigableMap.class == mapType) {
				return new TreeMap<>();
			}
			else if (MultiValueMap.class == mapType) {
				return new LinkedMultiValueMap();
			}
			else {
				throw new IllegalArgumentException("Unsupported Map interface: " + mapType.getName());
			}
		}
		else if (EnumMap.class == mapType) {
			Assert.notNull(keyType, "Cannot create EnumMap for unknown key type");
			return new EnumMap(asEnumType(keyType));
		}
		else {
			if (!Map.class.isAssignableFrom(mapType)) {
				throw new IllegalArgumentException("Unsupported Map type: " + mapType.getName());
			}
			try {
				return (Map<K, V>) ReflectionUtils.accessibleConstructor(mapType).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Could not instantiate Map type: " + mapType.getName(), ex);
			}
		}
	}

	/**
	 * 创建一个 {@link java.util.Properties} 的变体，该变体在 {@link Properties#getProperty} 中
	 * 会自动将非字符串值转换为字符串表示形式。
	 * <p>此外，返回的 {@code Properties} 实例会根据键按字母数字顺序排序属性。
	 * @return 一个新的 {@code Properties} 实例
	 * @since 4.3.4
	 * @see #createSortedProperties(boolean)
	 * @see #createSortedProperties(Properties, boolean)
	 */
	@SuppressWarnings("serial")
	public static Properties createStringAdaptingProperties() {
		return new SortedProperties(false) {
			@Override
			@Nullable
			public String getProperty(String key) {
				Object value = get(key);
				return (value != null ? value.toString() : null);
			}
		};
	}

	/**
	 * 创建一个 {@link java.util.Properties} 的变体，该变体根据键按字母数字顺序排序属性。
	 * <p>当将 {@link Properties} 实例存储到属性文件中时，这很有用，
	 * 因为它允许以可重复且属性顺序一致的方式生成此类文件。
	 * 生成的属性文件中的注释也可以选择性地省略。
	 * @param omitComments 如果为 {@code true}，则在将属性存储到文件时省略注释
	 * @return 一个新的 {@code Properties} 实例
	 * @since 5.2
	 * @see #createStringAdaptingProperties()
	 * @see #createSortedProperties(Properties, boolean)
	 */
	public static Properties createSortedProperties(boolean omitComments) {
		return new SortedProperties(omitComments);
	}

	/**
	 * 创建一个 {@link java.util.Properties} 的变体，该变体根据键按字母数字顺序排序属性。
	 * <p>当将 {@code Properties} 实例存储到属性文件中时，这很有用，
	 * 因为它允许以可重复且属性顺序一致的方式生成此类文件。
	 * 生成的属性文件中的注释也可以选择性地省略。
	 * <p>返回的 {@code Properties} 实例会用提供的 {@code properties} 对象中的属性填充，
	 * 但不会复制提供的 {@code properties} 对象中的默认属性。
	 * @param properties 用于复制初始属性的 {@code Properties} 对象
	 * @param omitComments 如果为 {@code true}，则在将属性存储到文件时省略注释
	 * @return 一个新的 {@code Properties} 实例
	 * @since 5.2
	 * @see #createStringAdaptingProperties()
	 * @see #createSortedProperties(boolean)
	 */
	public static Properties createSortedProperties(Properties properties, boolean omitComments) {
		return new SortedProperties(properties, omitComments);
	}

	/**
	 * 将给定的类型转换为 {@link Enum} 的子类型。
	 * @param enumType 枚举类型，绝不为 {@code null}
	 * @return 给定类型作为 {@link Enum} 的子类型
	 * @throws IllegalArgumentException 如果给定类型不是 {@link Enum} 的子类型
	 */
	@SuppressWarnings("rawtypes")
	private static Class<? extends Enum> asEnumType(Class<?> enumType) {
		Assert.notNull(enumType, "Enum type must not be null");
		if (!Enum.class.isAssignableFrom(enumType)) {
			throw new IllegalArgumentException("Supplied type is not an enum: " + enumType.getName());
		}
		return enumType.asSubclass(Enum.class);
	}

}
