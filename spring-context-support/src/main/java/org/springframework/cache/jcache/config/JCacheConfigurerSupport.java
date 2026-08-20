/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.cache.jcache.config;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.lang.Nullable;

/**
 * {@link CachingConfigurerSupport} 的扩展，同时实现了 {@link JCacheConfigurer}。
 *
 * <p>使用 JSR-107 注解的用户可以继承此类，而不是直接实现 {@link JCacheConfigurer} 接口。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see JCacheConfigurer
 * @see CachingConfigurerSupport
 */
public class JCacheConfigurerSupport extends CachingConfigurerSupport implements JCacheConfigurer {

	@Override
	@Nullable
	public CacheResolver exceptionCacheResolver() {
		return null;
	}

}
