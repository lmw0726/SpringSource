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

package org.springframework.cache.interceptor;

import org.springframework.aop.Pointcut;
import org.springframework.aop.framework.AbstractSingletonProxyFactoryBean;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cache.CacheManager;

/**
 * 用于简化声明式缓存处理的代理 factory bean。
 * 这是标准 AOP {@link org.springframework.aop.framework.ProxyFactoryBean}
 * 加单独 {@link CacheInterceptor} 定义的便捷替代方案。
 *
 * <p>此类旨在促进声明式缓存边界划分：即使用缓存代理包装 singleton
 * 目标对象，代理目标实现的所有接口。主要用于第三方框架集成。
 * <strong>用户应优先使用 {@code cache:} XML 命名空间和
 * {@link org.springframework.cache.annotation.Cacheable @Cacheable} 注解。</strong>
 * 有关更多信息，请参阅 Spring 参考文档中的
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#cache-annotations">基于声明式注解的缓存</a>
 * 章节。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 * @see org.springframework.aop.framework.ProxyFactoryBean
 * @see CacheInterceptor
 */
@SuppressWarnings("serial")
public class CacheProxyFactoryBean extends AbstractSingletonProxyFactoryBean
		implements BeanFactoryAware, SmartInitializingSingleton {

	private final CacheInterceptor cacheInterceptor = new CacheInterceptor();

	private Pointcut pointcut = Pointcut.TRUE;


	/**
	 * 设置一个或多个用于查找缓存操作的源。
	 * @see CacheInterceptor#setCacheOperationSources
	 */
	public void setCacheOperationSources(CacheOperationSource... cacheOperationSources) {
		this.cacheInterceptor.setCacheOperationSources(cacheOperationSources);
	}

	/**
	 * 设置此缓存切面在没有为操作设置特定键生成器时应委托的
	 * 默认 {@link KeyGenerator}。
	 * <p>默认值为 {@link SimpleKeyGenerator}。
	 * @since 5.0.3
	 * @see CacheInterceptor#setKeyGenerator
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.cacheInterceptor.setKeyGenerator(keyGenerator);
	}

	/**
	 * 设置此缓存切面在没有为操作设置特定缓存解析器时应委托的
	 * 默认 {@link CacheResolver}。
	 * <p>默认解析器会根据缓存名称和默认缓存管理器解析缓存。
	 * @since 5.0.3
	 * @see CacheInterceptor#setCacheResolver
	 */
	public void setCacheResolver(CacheResolver cacheResolver) {
		this.cacheInterceptor.setCacheResolver(cacheResolver);
	}

	/**
	 * 设置用于创建默认 {@link CacheResolver} 的 {@link CacheManager}。
	 * 替换当前的 {@link CacheResolver}（如果有）。
	 * @since 5.0.3
	 * @see CacheInterceptor#setCacheManager
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheInterceptor.setCacheManager(cacheManager);
	}

	/**
	 * 设置 pointcut，即根据传入的方法和属性触发
	 * {@link CacheInterceptor} 条件调用的 bean。
	 * <p>注意：附加拦截器始终会被调用。
	 * @see #setPreInterceptors
	 * @see #setPostInterceptors
	 */
	public void setPointcut(Pointcut pointcut) {
		this.pointcut = pointcut;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.cacheInterceptor.setBeanFactory(beanFactory);
	}

	@Override
	public void afterSingletonsInstantiated() {
		this.cacheInterceptor.afterSingletonsInstantiated();
	}


	@Override
	protected Object createMainInterceptor() {
		this.cacheInterceptor.afterPropertiesSet();
		return new DefaultPointcutAdvisor(this.pointcut, this.cacheInterceptor);
	}

}
