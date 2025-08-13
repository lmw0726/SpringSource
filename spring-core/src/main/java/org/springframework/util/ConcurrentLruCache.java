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

package org.springframework.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * 简单的 LRU（最近最少使用）缓存，受指定缓存限制约束。
 *
 * <p>此实现由 {@code ConcurrentHashMap} 支持用于存储缓存值，
 * 由 {@code ConcurrentLinkedDeque} 用于排序键并在缓存达到满容量时选择最近最少使用的键。
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 5.3
 * @param <K> 用于缓存检索的键类型
 * @param <V> 缓存值的类型
 * @see #get
 */
public class ConcurrentLruCache<K, V> {

	private final int sizeLimit;

	private final Function<K, V> generator;

	private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();

	private final ConcurrentLinkedDeque<K> queue = new ConcurrentLinkedDeque<>();

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private volatile int size;


	/**
	 * 使用给定的限制和生成器函数创建新的缓存实例。
	 * @param sizeLimit 缓存中的最大条目数
	 * （0 表示不缓存，总是生成新值）
	 * @param generator 为给定键生成新值的函数
	 */
	public ConcurrentLruCache(int sizeLimit, Function<K, V> generator) {
		Assert.isTrue(sizeLimit >= 0, "Cache size limit must not be negative");
		Assert.notNull(generator, "Generator function must not be null");
		this.sizeLimit = sizeLimit;
		this.generator = generator;
	}


	/**
	 * 从缓存中检索条目，可能触发值的生成。
	 * @param key 要检索条目的键
	 * @return 缓存的或新生成的值
	 */
	public V get(K key) {
		if (this.sizeLimit == 0) {
			return this.generator.apply(key);
		}

		V cached = this.cache.get(key);
		if (cached != null) {
			if (this.size < this.sizeLimit) {
				return cached;
			}
			this.lock.readLock().lock();
			try {
				if (this.queue.removeLastOccurrence(key)) {
					this.queue.offer(key);
				}
				return cached;
			}
			finally {
				this.lock.readLock().unlock();
			}
		}

		this.lock.writeLock().lock();
		try {
			// 在同一键的并发读取情况下重试
			cached = this.cache.get(key);
			if (cached != null) {
				if (this.queue.removeLastOccurrence(key)) {
					this.queue.offer(key);
				}
				return cached;
			}
			// 首先生成值，以防止大小不一致
			V value = this.generator.apply(key);
			if (this.size == this.sizeLimit) {
				K leastUsed = this.queue.poll();
				if (leastUsed != null) {
					this.cache.remove(leastUsed);
				}
			}
			this.queue.offer(key);
			this.cache.put(key, value);
			this.size = this.cache.size();
			return value;
		}
		finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * 确定给定键是否存在于此缓存中。
	 * @param key 要检查的键
	 * @return 如果键存在则返回 {@code true}，
	 * 如果没有匹配的键则返回 {@code false}
	 */
	public boolean contains(K key) {
		return this.cache.containsKey(key);
	}

	/**
	 * 立即删除给定键及其关联的值。
	 * @param key 要驱逐条目的键
	 * @return 如果键之前存在则返回 {@code true}，
	 * 如果没有匹配的键则返回 {@code false}
	 */
	public boolean remove(K key) {
		this.lock.writeLock().lock();
		try {
			boolean wasPresent = (this.cache.remove(key) != null);
			this.queue.remove(key);
			this.size = this.cache.size();
			return wasPresent;
		}
		finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * 立即从此缓存中删除所有条目。
	 */
	public void clear() {
		this.lock.writeLock().lock();
		try {
			this.cache.clear();
			this.queue.clear();
			this.size = 0;
		}
		finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * 返回缓存的当前大小。
	 * @see #sizeLimit()
	 */
	public int size() {
		return this.size;
	}

	/**
	 * 返回缓存中的最大条目数
	 * （0 表示不缓存，总是生成新值）。
	 * @see #size()
	 */
	public int sizeLimit() {
		return this.sizeLimit;
	}

}
