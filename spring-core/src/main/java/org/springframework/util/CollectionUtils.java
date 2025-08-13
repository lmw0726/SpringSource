/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.util.*;

/**
 * 集合工具类，提供各种集合操作方法。
 * 主要用于框架内部使用。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Arjen Poutsma
 * @since 1.1.3
 */
public abstract class CollectionUtils {

	/**
	 * {@link HashMap}/{@link LinkedHashMap} 的默认负载因子。
	 *
	 * @see #newHashMap(int)
	 * @see #newLinkedHashMap(int)
	 */
	static final float DEFAULT_LOAD_FACTOR = 0.75f;


	/**
	 * 检查指定的集合是否为 null 或空。
	 *
	 * @param collection 要检查的集合
	 * @return 如果集合为 null 或空则返回 true，否则返回 false
	 */
	public static boolean isEmpty(@Nullable Collection<?> collection) {
		return (collection == null || collection.isEmpty());
	}

	/**
	 * 检查指定的 Map 是否为 null 或空。
	 *
	 * @param map 要检查的 Map
	 * @return 如果 Map 为 null 或空则返回 true，否则返回 false
	 */
	public static boolean isEmpty(@Nullable Map<?, ?> map) {
		return (map == null || map.isEmpty());
	}

	/**
	 * 创建一个新的 {@link HashMap} 实例，其初始容量可以容纳指定数量的元素，
	 * 而无需立即进行扩容/rehash操作。
	 * <p>这与常规的 {@link HashMap} 构造函数不同，后者接收的初始容量是相对于
	 * 负载因子的，但此方法与 JDK 的 {@link java.util.concurrent.ConcurrentHashMap#ConcurrentHashMap(int)}
	 * 实现保持一致。
	 *
	 * @param expectedSize 预期元素数量（将计算对应的容量以避免扩容/rehash操作）
	 * @see #newLinkedHashMap(int)
	 * @since 5.3
	 */
	public static <K, V> HashMap<K, V> newHashMap(int expectedSize) {
		return new HashMap<>((int) (expectedSize / DEFAULT_LOAD_FACTOR), DEFAULT_LOAD_FACTOR);
	}

	/**
	 * 创建一个新的 {@link LinkedHashMap} 实例，其初始容量可以容纳指定数量的元素，
	 * 而无需立即进行扩容/rehash操作。
	 * <p>这与常规的 {@link LinkedHashMap} 构造函数不同，后者接收的初始容量是相对于
	 * 负载因子的，但此方法与 Spring 5.3 版本的 {@link LinkedCaseInsensitiveMap} 和
	 * {@link LinkedMultiValueMap} 构造函数语义保持一致。
	 *
	 * @param expectedSize 预期元素数量（将计算对应的容量以避免扩容/rehash操作）
	 * @see #newHashMap(int)
	 * @since 5.3
	 */
	public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int expectedSize) {
		return new LinkedHashMap<>((int) (expectedSize / DEFAULT_LOAD_FACTOR), DEFAULT_LOAD_FACTOR);
	}

	/**
	 * 将给定的数组转换为 List。原始类型数组将被转换为相应包装类型的 List。
	 * <p><b>注意：</b>通常建议使用标准的 {@link Arrays#asList} 方法。
	 * 此 {@code arrayToList} 方法主要用于处理运行时可能是 {@code Object[]}
	 * 或原始类型数组的输入对象。
	 * <p>{@code null} 源值将被转换为空 List。
	 *
	 * @param source 可能是原始类型的数组
	 * @return 转换后的 List 结果
	 * @see ObjectUtils#toObjectArray(Object)
	 * @see Arrays#asList(Object[])
	 */
	public static List<?> arrayToList(@Nullable Object source) {
		return Arrays.asList(ObjectUtils.toObjectArray(source));
	}

	/**
	 * 将给定数组合并到指定的 Collection 中。
	 *
	 * @param array 要合并的数组（可能为 {@code null}）
	 * @param collection 要合并数组的目标 Collection
	 */
	@SuppressWarnings("unchecked")
	public static <E> void mergeArrayIntoCollection(@Nullable Object array, Collection<E> collection) {
		Object[] arr = ObjectUtils.toObjectArray(array);
		for (Object elem : arr) {
			collection.add((E) elem);
		}
	}

	/**
	 * 将给定的属性实例合并到给定的映射中，复制所有属性 (键值对)。
	 * <p> 使用 {@code Properties.propertyNames()} 甚至捕获链接到原始属性实例的默认属性。
	 *
	 * @param props 要合并的属性实例 (可能为 {@code null})
	 * @param map   将属性合并到的目标Map
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> void mergePropertiesIntoMap(@Nullable Properties props, Map<K, V> map) {
		if (props == null) {
			return;
		}
		for (Enumeration<?> en = props.propertyNames(); en.hasMoreElements(); ) {
			String key = (String) en.nextElement();
			Object value = props.get(key);
			if (value == null) {
				//允许默认回退或可能被覆盖的访问器...
				value = props.getProperty(key);
			}
			map.put((K) key, (V) value);
		}
	}


	/**
	 * 检查给定的迭代器是否包含指定的元素。
	 *
	 * @param iterator 要检查的迭代器
	 * @param element  要查找的元素
	 * @return 如果找到则返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean contains(@Nullable Iterator<?> iterator, Object element) {
		if (iterator != null) {
			while (iterator.hasNext()) {
				Object candidate = iterator.next();
				if (ObjectUtils.nullSafeEquals(candidate, element)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 检查给定的枚举是否包含指定的元素。
	 *
	 * @param enumeration 要检查的枚举
	 * @param element     要查找的元素
	 * @return 如果找到则返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean contains(@Nullable Enumeration<?> enumeration, Object element) {
		if (enumeration != null) {
			while (enumeration.hasMoreElements()) {
				Object candidate = enumeration.nextElement();
				if (ObjectUtils.nullSafeEquals(candidate, element)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 检查给定的集合是否包含指定的元素实例。
	 * <p>强制要求必须是完全相同的实例，而不仅仅是相等的元素。
	 *
	 * @param collection 要检查的集合
	 * @param element    要查找的元素
	 * @return 如果找到则返回 {@code true}，否则返回 {@code false}
	 */
	public static boolean containsInstance(@Nullable Collection<?> collection, Object element) {
		if (collection != null) {
			for (Object candidate : collection) {
				if (candidate == element) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 如果 '{@code candidates}' 中的任意元素包含在 '{@code source}' 中，则返回 {@code true}；
	 * 否则返回 {@code false}。
	 *
	 * @param source     源集合
	 * @param candidates 要搜索的候选元素集合
	 * @return 是否找到任意候选元素
	 */
	public static boolean containsAny(Collection<?> source, Collection<?> candidates) {
		return findFirstMatch(source, candidates) != null;
	}

	/**
	 * 返回 '{@code candidates}' 中包含在 '{@code source}' 中的第一个元素。
	 * 如果 '{@code candidates}' 中没有元素存在于 '{@code source}' 中，则返回 {@code null}。
	 * 迭代顺序取决于具体的 {@link Collection} 实现。
	 *
	 * @param source     源集合
	 * @param candidates 要搜索的候选元素集合
	 * @return 第一个匹配的元素，如果找不到则返回 {@code null}
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <E> E findFirstMatch(Collection<?> source, Collection<E> candidates) {
		if (isEmpty(source) || isEmpty(candidates)) {
			return null;
		}
		for (Object candidate : candidates) {
			if (source.contains(candidate)) {
				return (E) candidate;
			}
		}
		return null;
	}

	/**
	 * 在给定集合中查找指定类型的单个值。
	 *
	 * @param collection 要搜索的集合
	 * @param type       要查找的类型
	 * @return 如果找到明确匹配的给定类型的值则返回该值，
	 *         如果没有找到或找到多个这样的值则返回 {@code null}
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T> T findValueOfType(Collection<?> collection, @Nullable Class<T> type) {
		if (isEmpty(collection)) {
			return null;
		}
		T value = null;
		for (Object element : collection) {
			if (type == null || type.isInstance(element)) {
				if (value != null) {
					// 找到多个值...没有明确的单个值
					return null;
				}
				value = (T) element;
			}
		}
		return value;
	}

	/**
	 * 在给定的集合中查找符合指定类型之一的单个值：
	 * 首先在集合中查找第一个类型的值，如果没有找到则继续查找第二个类型的值，依此类推。
	 *
	 * @param collection 要搜索的集合
	 * @param types      要查找的类型数组，按优先级排序
	 * @return 如果找到明确匹配的给定类型之一的值则返回该值，
	 *         如果没有找到或找到多个这样的值则返回 {@code null}
	 */
	@Nullable
	public static Object findValueOfType(Collection<?> collection, Class<?>[] types) {
		if (isEmpty(collection) || ObjectUtils.isEmpty(types)) {
			return null;
		}
		for (Class<?> type : types) {
			Object value = findValueOfType(collection, type);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	/**
	 * 判断给定的集合是否只包含唯一的一个对象。
	 *
	 * @param collection 要检查的集合
	 * @return 如果集合包含单个引用或多个对同一实例的引用则返回 {@code true}，
	 *         否则返回 {@code false}
	 */
	public static boolean hasUniqueObject(Collection<?> collection) {
		if (isEmpty(collection)) {
			return false;
		}
		boolean hasCandidate = false;
		Object candidate = null;
		for (Object elem : collection) {
			if (!hasCandidate) {
				hasCandidate = true;
				candidate = elem;
			} else if (candidate != elem) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 查找给定集合中元素的共同类型（如果存在）。
	 *
	 * @param collection 要检查的集合
	 * @return 共同的元素类型，如果没有找到明确的共同类型（或集合为空）则返回 {@code null}
	 */
	@Nullable
	public static Class<?> findCommonElementType(Collection<?> collection) {
		if (isEmpty(collection)) {
			return null;
		}
		Class<?> candidate = null;
		for (Object val : collection) {
			if (val != null) {
				if (candidate == null) {
					candidate = val.getClass();
				} else if (candidate != val.getClass()) {
					return null;
				}
			}
		}
		return candidate;
	}

	/**
	 * 检索给定集合的第一个元素，使用 {@link SortedSet#first()}
	 * 或者使用迭代器。
	 * @param set 要检查的集合（可能为 {@code null} 或空）
	 * @return 第一个元素，如果没有则返回 {@code null}
	 * @see SortedSet
	 * @see LinkedHashMap#keySet()
	 * @see java.util.LinkedHashSet
	 * @since 5.2.3
	 */
	@Nullable
	public static <T> T firstElement(@Nullable Set<T> set) {
		if (isEmpty(set)) {
			return null;
		}
		if (set instanceof SortedSet) {
			return ((SortedSet<T>) set).first();
		}

		Iterator<T> it = set.iterator();
		T first = null;
		if (it.hasNext()) {
			first = it.next();
		}
		return first;
	}

	/**
	 * 检索给定列表的第一个元素，访问零索引。
	 * @param list 要检查的列表（可能为 {@code null} 或空）
	 * @return 第一个元素，如果没有则返回 {@code null}
	 * @since 5.2.3
	 */
	@Nullable
	public static <T> T firstElement(@Nullable List<T> list) {
		if (isEmpty(list)) {
			return null;
		}
		return list.get(0);
	}

	/**
	 * 检索给定集合的最后一个元素，使用 {@link SortedSet#last()}
	 * 或者遍历所有元素（假设是链式集合）。
	 * @param set 要检查的集合（可能为 {@code null} 或空）
	 * @return 最后一个元素，如果没有则返回 {@code null}
	 * @see SortedSet
	 * @see LinkedHashMap#keySet()
	 * @see java.util.LinkedHashSet
	 * @since 5.0.3
	 */
	@Nullable
	public static <T> T lastElement(@Nullable Set<T> set) {
		if (isEmpty(set)) {
			return null;
		}
		if (set instanceof SortedSet) {
			return ((SortedSet<T>) set).last();
		}

		// 需要完整遍历...
		Iterator<T> it = set.iterator();
		T last = null;
		while (it.hasNext()) {
			last = it.next();
		}
		return last;
	}

	/**
	 * 检索给定列表的最后一个元素，访问最高索引。
	 * @param list 要检查的列表（可能为 {@code null} 或空）
	 * @return 最后一个元素，如果没有则返回 {@code null}
	 * @since 5.0.3
	 */
	@Nullable
	public static <T> T lastElement(@Nullable List<T> list) {
		if (isEmpty(list)) {
			return null;
		}
		return list.get(list.size() - 1);
	}

	/**
	 * 将给定枚举中的元素编组到给定类型的数组中。
	 * 枚举元素必须可分配给给定数组的类型。返回的数组将与给定数组不同的实例。
	 */
	public static <A, E extends A> A[] toArray(Enumeration<E> enumeration, A[] array) {
		ArrayList<A> elements = new ArrayList<>();
		while (enumeration.hasMoreElements()) {
			elements.add(enumeration.nextElement());
		}
		return elements.toArray(array);
	}

	/**
	 * 将 {@link Enumeration} 适配为 {@link Iterator}。
	 * @param enumeration 原始的 {@code Enumeration}
	 * @return 适配后的 {@code Iterator}
	 */
	public static <E> Iterator<E> toIterator(@Nullable Enumeration<E> enumeration) {
		return (enumeration != null ? new EnumerationIterator<>(enumeration) : Collections.emptyIterator());
	}

	/**
	 * 将 {@code Map<K, List<V>>} 适配为 {@code MultiValueMap<K, V>}。
	 * @param targetMap 原始映射
	 * @return 适配后的多值映射（包装原始映射）
	 * @since 3.1
	 */
	public static <K, V> MultiValueMap<K, V> toMultiValueMap(Map<K, List<V>> targetMap) {
		return new MultiValueMapAdapter<>(targetMap);
	}

	/**
	 * 返回指定多值映射的不可修改视图。
	 * @param targetMap 要返回不可修改视图的映射
	 * @return 指定多值映射的不可修改视图
	 * @since 3.1
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> MultiValueMap<K, V> unmodifiableMultiValueMap(
			MultiValueMap<? extends K, ? extends V> targetMap) {

		Assert.notNull(targetMap, "'targetMap' must not be null");
		Map<K, List<V>> result = newLinkedHashMap(targetMap.size());
		targetMap.forEach((key, value) -> {
			List<? extends V> values = Collections.unmodifiableList(value);
			result.put(key, (List<V>) values);
		});
		Map<K, List<V>> unmodifiableMap = Collections.unmodifiableMap(result);
		return toMultiValueMap(unmodifiableMap);
	}


	/**
	 * 包装枚举的迭代器。
	 */
	private static class EnumerationIterator<E> implements Iterator<E> {

		private final Enumeration<E> enumeration;

		public EnumerationIterator(Enumeration<E> enumeration) {
			this.enumeration = enumeration;
		}

		@Override
		public boolean hasNext() {
			return this.enumeration.hasMoreElements();
		}

		@Override
		public E next() {
			return this.enumeration.nextElement();
		}

		@Override
		public void remove() throws UnsupportedOperationException {
			throw new UnsupportedOperationException("Not supported");
		}
	}


}
