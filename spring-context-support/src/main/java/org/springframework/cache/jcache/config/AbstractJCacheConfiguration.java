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

package org.springframework.cache.jcache.config;

import java.util.function.Supplier;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cache.annotation.AbstractCachingConfiguration;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.jcache.interceptor.DefaultJCacheOperationSource;
import org.springframework.cache.jcache.interceptor.JCacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.lang.Nullable;

/**
 * 抽象的 JSR-107 特定 {@code @Configuration} 类，提供启用 JSR-107 注解驱动的缓存管理能力的通用结构。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see JCacheConfigurer
 */
@Configuration(proxyBeanMethods = false)
public abstract class AbstractJCacheConfiguration extends AbstractCachingConfiguration {

	@Nullable
	protected Supplier<CacheResolver> exceptionCacheResolver;


	@Override
	protected void useCachingConfigurer(CachingConfigurerSupplier cachingConfigurerSupplier) {
		super.useCachingConfigurer(cachingConfigurerSupplier);
		this.exceptionCacheResolver = cachingConfigurerSupplier.adapt(config -> {
			if (config instanceof JCacheConfigurer) {
				return ((JCacheConfigurer) config).exceptionCacheResolver();
			}
			return null;
		});
	}

	@Bean(name = "jCacheOperationSource")
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public JCacheOperationSource cacheOperationSource() {
		return new DefaultJCacheOperationSource(
				this.cacheManager, this.cacheResolver, this.exceptionCacheResolver, this.keyGenerator);
	}

}
