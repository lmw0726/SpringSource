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

package org.springframework.cache;

import java.util.concurrent.Callable;

import org.springframework.lang.Nullable;

/**
 * 定义通用缓存操作的接口。
 *
 * <b>注意：</b>由于缓存的使用具有通用性，建议实现类允许存储 <tt>null</tt> 值
 * （例如用于缓存返回 {@code null} 的方法）。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public interface Cache {

	/**
	 * 返回缓存名称。
	 */
	String getName();

	/**
	 * 返回底层的原生缓存提供者。
	 */
	Object getNativeCache();

	/**
	 * 返回此缓存映射到指定键的值。
	 * <p>如果缓存中不包含该键的任何映射，则返回 {@code null}；
	 * 否则，缓存值（其本身可能为 {@code null}）将封装在 {@link ValueWrapper} 中返回。
	 * @param key 要返回其关联值的键
	 * @return 此缓存映射到指定键的值，封装在 {@link ValueWrapper} 中，
	 * 该包装器也可能保存缓存中的 {@code null} 值。直接返回 {@code null}
	 * 表示缓存中不包含该键的任何映射。
	 * @see #get(Object, Class)
	 * @see #get(Object, Callable)
	 */
	@Nullable
	ValueWrapper get(Object key);

	/**
	 * 返回此缓存映射到指定键的值，并泛型指定返回值将强制转换成的类型。
	 * <p>注意：此 {@code get} 变体无法区分缓存中的 {@code null} 值与完全找不到缓存条目
	 * 这两种情况。如需区分，请改用标准的 {@link #get(Object)} 变体。
	 * @param key 要返回其关联值的键
	 * @param type 返回值的必需类型（可为 {@code null} 以跳过类型检查；
	 * 如果缓存中找到的是 {@code null} 值，则指定的类型无关紧要）
	 * @return 此缓存映射到指定键的值（其本身可能为 {@code null}），
	 * 如果缓存中不包含该键的任何映射，同样返回 {@code null}
	 * @throws IllegalStateException 如果找到了缓存条目，但该条目与指定类型不匹配
	 * @since 4.0
	 * @see #get(Object)
	 */
	@Nullable
	<T> T get(Object key, @Nullable Class<T> type);

	/**
	 * 返回此缓存映射到指定键的值，如有必要则从 {@code valueLoader} 获取该值。
	 * 此方法为传统的“如果已缓存则返回；否则创建、缓存并返回”模式提供了简单的替代方案。
	 * <p>如果可能，实现应确保加载操作是同步的，以便在并发访问同一键时，
	 * 指定的 {@code valueLoader} 只会被调用一次。
	 * <p>如果 {@code valueLoader} 抛出异常，该异常将被包装在
	 * {@link ValueRetrievalException} 中
	 * @param key 要返回其关联值的键
	 * @return 此缓存映射到指定键的值
	 * @throws ValueRetrievalException 如果 {@code valueLoader} 抛出异常
	 * @since 4.3
	 * @see #get(Object)
	 */
	@Nullable
	<T> T get(Object key, Callable<T> valueLoader);

	/**
	 * 将指定值与此缓存中的指定键关联。
	 * <p>如果缓存此前已包含该键的映射，则旧值将被指定的值替换。
	 * <p>实际的注册操作可能以异步或延迟的方式进行，后续的查询可能暂时还看不到该条目。
	 * 例如，事务性缓存装饰器就属于这种情况。
	 * 如需保证立即注册，请使用 {@link #putIfAbsent}。
	 * @param key 要与指定值关联的键
	 * @param value 要与指定键关联的值
	 * @see #putIfAbsent(Object, Object)
	 */
	void put(Object key, @Nullable Object value);

	/**
	 * 如果指定键尚未设置值，则以原子方式将指定值与此缓存中的指定键关联。
	 * <p>这等价于：
	 * <pre><code>
	 * ValueWrapper existingValue = cache.get(key);
	 * if (existingValue == null) {
	 *     cache.put(key, value);
	 * }
	 * return existingValue;
	 * </code></pre>
	 * 区别在于该操作是以原子方式执行的。虽然所有开箱即用的
	 * {@link CacheManager} 实现都能以原子方式执行 put 操作，
	 * 但该操作也可以非原子地分两步实现，例如先检查键是否存在，随后再执行 put。
	 * 有关更多细节，请查阅你所使用的原生缓存实现的文档。
	 * <p>默认实现按照上面的代码片段，委托给 {@link #get(Object)} 和
	 * {@link #put(Object, Object)}。
	 * @param key 要与指定值关联的键
	 * @param value 要与指定键关联的值
	 * @return 此缓存映射到指定键的值（其本身可能为 {@code null}），
	 * 如果调用前缓存中不包含该键的任何映射，同样返回 {@code null}。
	 * 因此返回 {@code null} 表示给定的 {@code value} 已与该键关联。
	 * @since 4.1
	 * @see #put(Object, Object)
	 */
	@Nullable
	default ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		ValueWrapper existingValue = get(key);
		if (existingValue == null) {
			put(key, value);
		}
		return existingValue;
	}

	/**
	 * 如果此缓存中存在该键的映射，则将其驱逐（移除）。
	 * <p>实际的驱逐操作可能以异步或延迟的方式进行，后续的查询可能仍然能看到该条目。
	 * 例如，事务性缓存装饰器就属于这种情况。
	 * 如需保证立即移除，请使用 {@link #evictIfPresent}。
	 * @param key 要从缓存中移除其映射的键
	 * @see #evictIfPresent(Object)
	 */
	void evict(Object key);

	/**
	 * 如果此缓存中存在该键的映射，则将其驱逐（移除），并期望该键在后续查询中立即可见地消失。
	 * <p>默认实现委托给 {@link #evict(Object)}，在无法确定键先前是否存在时返回 {@code false}。
	 * 鼓励缓存提供者，尤其是缓存装饰器，尽可能执行立即驱逐（例如在事务内缓存操作普遍延迟的情况下），
	 * 并可靠地判断给定键先前是否存在。
	 * @param key 要从缓存中移除其映射的键
	 * @return 如果此缓存此前已知包含该键的映射则返回 {@code true}，
	 * 否则返回 {@code false}（或无法确定键先前是否存在时）
	 * @since 5.2
	 * @see #evict(Object)
	 */
	default boolean evictIfPresent(Object key) {
		evict(key);
		return false;
	}

	/**
	 * 通过移除所有映射来清空缓存。
	 * <p>实际的清空操作可能以异步或延迟的方式进行，后续的查询可能仍然能看到这些条目。
	 * 例如，事务性缓存装饰器就属于这种情况。
	 * 如需保证立即移除条目，请使用 {@link #invalidate()}。
	 * @see #invalidate()
	 */
	void clear();

	/**
	 * 通过移除所有映射来使缓存失效，并期望所有条目在后续查询中立即可见地消失。
	 * @return 如果此缓存此前已知包含映射则返回 {@code true}，
	 * 否则返回 {@code false}（或无法确定条目先前是否存在时）
	 * @since 5.2
	 * @see #clear()
	 */
	default boolean invalidate() {
		clear();
		return false;
	}


	/**
	 * 表示缓存值的（包装）对象。
	 */
	@FunctionalInterface
	interface ValueWrapper {

		/**
		 * 返回缓存中的实际值。
		 */
		@Nullable
		Object get();
	}


	/**
	 * 当值加载器回调因异常而失败时，从 {@link #get(Object, Callable)} 抛出的包装异常。
	 * @since 4.3
	 */
	@SuppressWarnings("serial")
	class ValueRetrievalException extends RuntimeException {

		@Nullable
		private final Object key;

		public ValueRetrievalException(@Nullable Object key, Callable<?> loader, Throwable ex) {
			super(String.format("Value for key '%s' could not be loaded using '%s'", key, loader), ex);
			this.key = key;
		}

		@Nullable
		public Object getKey() {
			return this.key;
		}
	}

}
