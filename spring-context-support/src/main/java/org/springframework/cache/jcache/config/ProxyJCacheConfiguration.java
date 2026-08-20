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

package org.springframework.cache.jcache.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cache.config.CacheManagementConfigUtils;
import org.springframework.cache.jcache.interceptor.BeanFactoryJCacheOperationSourceAdvisor;
import org.springframework.cache.jcache.interceptor.JCacheInterceptor;
import org.springframework.cache.jcache.interceptor.JCacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * 注册启用基于代理的注解驱动 JSR-107 缓存管理所必需的 Spring 基础设施 Bean 的 {@code @Configuration} 类。
 *
 * <p>可以与 Spring 的缓存支持安全地一起使用。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.cache.annotation.EnableCaching
 * @see org.springframework.cache.annotation.CachingConfigurationSelector
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProxyJCacheConfiguration extends AbstractJCacheConfiguration {

	@Bean(name = CacheManagementConfigUtils.JCACHE_ADVISOR_BEAN_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public BeanFactoryJCacheOperationSourceAdvisor cacheAdvisor(
			JCacheOperationSource jCacheOperationSource, JCacheInterceptor jCacheInterceptor) {

		BeanFactoryJCacheOperationSourceAdvisor advisor = new BeanFactoryJCacheOperationSourceAdvisor();
		advisor.setCacheOperationSource(jCacheOperationSource);
		advisor.setAdvice(jCacheInterceptor);
		if (this.enableCaching != null) {
			advisor.setOrder(this.enableCaching.<Integer>getNumber("order"));
		}
		return advisor;
	}

	@Bean(name = "jCacheInterceptor")
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public JCacheInterceptor cacheInterceptor(JCacheOperationSource jCacheOperationSource) {
		JCacheInterceptor interceptor = new JCacheInterceptor(this.errorHandler);
		interceptor.setCacheOperationSource(jCacheOperationSource);
		return interceptor;
	}

}
