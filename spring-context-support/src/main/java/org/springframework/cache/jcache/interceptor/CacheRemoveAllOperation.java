/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheRemoveAll;

import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.util.ExceptionTypeFilter;

/**
 * {@link CacheRemoveAll} 操作的 {@link JCacheOperation} 实现。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see CacheRemoveAll
 */
class CacheRemoveAllOperation extends AbstractJCacheOperation<CacheRemoveAll> {

	private final ExceptionTypeFilter exceptionTypeFilter;


	public CacheRemoveAllOperation(CacheMethodDetails<CacheRemoveAll> methodDetails, CacheResolver cacheResolver) {
		super(methodDetails, cacheResolver);
		CacheRemoveAll ann = methodDetails.getCacheAnnotation();
		this.exceptionTypeFilter = createExceptionTypeFilter(ann.evictFor(), ann.noEvictFor());
	}


	@Override
	public ExceptionTypeFilter getExceptionTypeFilter() {
		return this.exceptionTypeFilter;
	}

	/**
	 * 指定是否在调用方法之前清除缓存。默认情况下，缓存在方法调用之后清除。
	 * @see javax.cache.annotation.CacheRemoveAll#afterInvocation()
	 */
	public boolean isEarlyRemove() {
		return !getCacheAnnotation().afterInvocation();
	}

}
