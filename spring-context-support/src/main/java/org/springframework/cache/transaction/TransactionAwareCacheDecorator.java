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

package org.springframework.cache.transaction;

import java.util.concurrent.Callable;

import org.springframework.cache.Cache;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * 缓存装饰器，将其 {@link #put}、{@link #evict} 和 {@link #clear} 操作
 * 与 Spring 管理的事务同步（通过 Spring 的 {@link TransactionSynchronizationManager}），
 * 仅在成功事务的提交后阶段执行实际的缓存 put/evict/clear 操作。
 * 如果没有活跃的事务，{@link #put}、{@link #evict} 和 {@link #clear} 操作
 * 将像往常一样立即执行。
 *
 * <p><b>注意：</b>像 {@link #putIfAbsent} 和 {@link #evictIfPresent} 这样的
 * 立即执行操作不能延迟到正在运行的事务的提交后阶段。在事务环境中使用这些操作时请谨慎。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Stas Volsky
 * @since 3.2
 * @see TransactionAwareCacheManagerProxy
 */
public class TransactionAwareCacheDecorator implements Cache {

	private final Cache targetCache;


	/**
	 * 为给定的目标 Cache 创建一个新的 TransactionAwareCache。
	 * @param targetCache the target Cache to decorate
	 */
	public TransactionAwareCacheDecorator(Cache targetCache) {
		Assert.notNull(targetCache, "Target Cache must not be null");
		this.targetCache = targetCache;
	}


	/**
	 * 返回此 Cache 应委派的目标 Cache。
	 */
	public Cache getTargetCache() {
		return this.targetCache;
	}

	@Override
	public String getName() {
		return this.targetCache.getName();
	}

	@Override
	public Object getNativeCache() {
		return this.targetCache.getNativeCache();
	}

	@Override
	@Nullable
	public ValueWrapper get(Object key) {
		return this.targetCache.get(key);
	}

	@Override
	public <T> T get(Object key, @Nullable Class<T> type) {
		return this.targetCache.get(key, type);
	}

	@Override
	@Nullable
	public <T> T get(Object key, Callable<T> valueLoader) {
		return this.targetCache.get(key, valueLoader);
	}

	@Override
	public void put(final Object key, @Nullable final Object value) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					TransactionAwareCacheDecorator.this.targetCache.put(key, value);
				}
			});
		}
		else {
			this.targetCache.put(key, value);
		}
	}

	@Override
	@Nullable
	public ValueWrapper putIfAbsent(Object key, @Nullable Object value) {
		return this.targetCache.putIfAbsent(key, value);
	}

	@Override
	public void evict(final Object key) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					TransactionAwareCacheDecorator.this.targetCache.evict(key);
				}
			});
		}
		else {
			this.targetCache.evict(key);
		}
	}

	@Override
	public boolean evictIfPresent(Object key) {
		return this.targetCache.evictIfPresent(key);
	}

	@Override
	public void clear() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					targetCache.clear();
				}
			});
		}
		else {
			this.targetCache.clear();
		}
	}

	@Override
	public boolean invalidate() {
		return this.targetCache.invalidate();
	}

}
