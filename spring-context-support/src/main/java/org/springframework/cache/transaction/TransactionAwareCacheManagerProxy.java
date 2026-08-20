/*
 * Copyright 2002-2017 the original author or authors.
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

import java.util.Collection;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 目标 {@link CacheManager} 的代理，暴露事务感知的 {@link Cache} 对象，
 * 其 {@link Cache#put} 操作与 Spring 管理的事务同步
 * （通过 Spring 的 {@link org.springframework.transaction.support.TransactionSynchronizationManager}），
 * 仅在成功事务的 after-commit 阶段执行实际的缓存 put 操作。
 * 如果没有活动事务，{@link Cache#put} 操作将按常规立即执行。
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @see #setTargetCacheManager
 * @see TransactionAwareCacheDecorator
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 */
public class TransactionAwareCacheManagerProxy implements CacheManager, InitializingBean {

	@Nullable
	private CacheManager targetCacheManager;


	/**
	 * 创建一个新的 TransactionAwareCacheManagerProxy，通过 {@link #setTargetCacheManager} bean 属性设置目标 CacheManager。
	 */
	public TransactionAwareCacheManagerProxy() {
	}

	/**
	 * 为给定的目标 CacheManager 创建一个新的 TransactionAwareCacheManagerProxy。
	 * @param targetCacheManager 要代理的目标 CacheManager
	 */
	public TransactionAwareCacheManagerProxy(CacheManager targetCacheManager) {
		Assert.notNull(targetCacheManager, "Target CacheManager must not be null");
		this.targetCacheManager = targetCacheManager;
	}


	/**
	 * 设置要代理的目标 CacheManager。
	 */
	public void setTargetCacheManager(CacheManager targetCacheManager) {
		this.targetCacheManager = targetCacheManager;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.targetCacheManager == null) {
			throw new IllegalArgumentException("Property 'targetCacheManager' is required");
		}
	}


	@Override
	@Nullable
	public Cache getCache(String name) {
		Assert.state(this.targetCacheManager != null, "No target CacheManager set");
		Cache targetCache = this.targetCacheManager.getCache(name);
		return (targetCache != null ? new TransactionAwareCacheDecorator(targetCache) : null);
	}

	@Override
	public Collection<String> getCacheNames() {
		Assert.state(this.targetCacheManager != null, "No target CacheManager set");
		return this.targetCacheManager.getCacheNames();
	}

}
