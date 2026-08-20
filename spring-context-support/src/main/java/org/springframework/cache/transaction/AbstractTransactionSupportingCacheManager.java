/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;

/**
 * 支持 Spring 事务感知的 CacheManager 实现的基类。
 * 通常需要通过 {@link #setTransactionAware} bean 属性显式开启事务感知。
 *
 * @author Juergen Hoeller
 * @since 3.2
 * @see #setTransactionAware
 * @see TransactionAwareCacheDecorator
 * @see TransactionAwareCacheManagerProxy
 */
public abstract class AbstractTransactionSupportingCacheManager extends AbstractCacheManager {

	private boolean transactionAware = false;


	/**
	 * 设置此 CacheManager 是否应暴露事务感知的 Cache 对象。
	 * <p>默认值为 "false"。将其设置为 "true" 可将缓存的 put/evict
	 * 操作与正在进行的 Spring 事务同步，仅在成功事务的 after-commit 阶段执行实际的缓存 put/evict 操作。
	 */
	public void setTransactionAware(boolean transactionAware) {
		this.transactionAware = transactionAware;
	}

	/**
	 * 返回此 CacheManager 是否已配置为事务感知模式。
	 */
	public boolean isTransactionAware() {
		return this.transactionAware;
	}


	@Override
	protected Cache decorateCache(Cache cache) {
		return (isTransactionAware() ? new TransactionAwareCacheDecorator(cache) : cache);
	}

}
