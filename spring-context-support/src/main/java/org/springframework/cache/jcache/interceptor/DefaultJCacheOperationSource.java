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

package org.springframework.cache.jcache.interceptor;

import java.util.Collection;
import java.util.function.Supplier;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.util.function.SupplierUtils;

/**
 * 默认的 {@link JCacheOperationSource} 实现，将默认操作委托给可配置的服务，
 * 当未配置时使用合理的默认值。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 */
public class DefaultJCacheOperationSource extends AnnotationJCacheOperationSource
		implements BeanFactoryAware, SmartInitializingSingleton {

	@Nullable
	private SingletonSupplier<CacheManager> cacheManager;

	@Nullable
	private SingletonSupplier<CacheResolver> cacheResolver;

	@Nullable
	private SingletonSupplier<CacheResolver> exceptionCacheResolver;

	private SingletonSupplier<KeyGenerator> keyGenerator;

	private final SingletonSupplier<KeyGenerator> adaptedKeyGenerator =
			SingletonSupplier.of(() -> new KeyGeneratorAdapter(this, getKeyGenerator()));

	@Nullable
	private BeanFactory beanFactory;


	/**
	 * 使用默认的密钥生成器构造一个新的 {@code DefaultJCacheOperationSource}。
	 * @see SimpleKeyGenerator
	 */
	public DefaultJCacheOperationSource() {
		this.keyGenerator = SingletonSupplier.of(SimpleKeyGenerator::new);
	}

	/**
	 * 使用给定的缓存管理器、缓存解析器和密钥生成器供应商构造一个新的 {@code DefaultJCacheOperationSource}，
	 * 如果供应商不可解析则应用相应的默认值。
	 * @since 5.1
	 */
	public DefaultJCacheOperationSource(
			@Nullable Supplier<CacheManager> cacheManager, @Nullable Supplier<CacheResolver> cacheResolver,
			@Nullable Supplier<CacheResolver> exceptionCacheResolver, @Nullable Supplier<KeyGenerator> keyGenerator) {

		this.cacheManager = SingletonSupplier.ofNullable(cacheManager);
		this.cacheResolver = SingletonSupplier.ofNullable(cacheResolver);
		this.exceptionCacheResolver = SingletonSupplier.ofNullable(exceptionCacheResolver);
		this.keyGenerator = new SingletonSupplier<>(keyGenerator, SimpleKeyGenerator::new);
	}


	/**
	 * 设置默认的 {@link CacheManager}，用于按名称查找缓存。
	 * 仅在未设置 {@linkplain CacheResolver 缓存解析器} 时是必需的。
	 */
	public void setCacheManager(@Nullable CacheManager cacheManager) {
		this.cacheManager = SingletonSupplier.ofNullable(cacheManager);
	}

	/**
	 * 返回指定的缓存管理器（如果有的话）。
	 */
	@Nullable
	public CacheManager getCacheManager() {
		return SupplierUtils.resolve(this.cacheManager);
	}

	/**
	 * 设置用于解析常规缓存的 {@link CacheResolver}。如果未设置，
	 * 将使用指定的缓存管理器的默认实现。
	 */
	public void setCacheResolver(@Nullable CacheResolver cacheResolver) {
		this.cacheResolver = SingletonSupplier.ofNullable(cacheResolver);
	}

	/**
	 * 返回指定的缓存解析器（如果有的话）。
	 */
	@Nullable
	public CacheResolver getCacheResolver() {
		return SupplierUtils.resolve(this.cacheResolver);
	}

	/**
	 * 设置用于解析异常缓存的 {@link CacheResolver}。如果未设置，
	 * 将使用指定的缓存管理器的默认实现。
	 */
	public void setExceptionCacheResolver(@Nullable CacheResolver exceptionCacheResolver) {
		this.exceptionCacheResolver = SingletonSupplier.ofNullable(exceptionCacheResolver);
	}

	/**
	 * 返回指定的异常缓存解析器（如果有的话）。
	 */
	@Nullable
	public CacheResolver getExceptionCacheResolver() {
		return SupplierUtils.resolve(this.exceptionCacheResolver);
	}

	/**
	 * 设置默认的 {@link KeyGenerator}。如果未设置，将使用遵循 JSR-107
	 * {@link javax.cache.annotation.CacheKey} 和
	 * {@link javax.cache.annotation.CacheValue} 的 {@link SimpleKeyGenerator}。
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = SingletonSupplier.of(keyGenerator);
	}

	/**
	 * 返回指定的密钥生成器。
	 */
	public KeyGenerator getKeyGenerator() {
		return this.keyGenerator.obtain();
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public void afterSingletonsInstantiated() {
		// 确保缓存解析器已初始化。异常缓存解析器仅在
		// 操作上设置了 exceptionCacheName 属性时才需要。
		Assert.notNull(getDefaultCacheResolver(), "Cache resolver should have been initialized");
	}


	@Override
	protected <T> T getBean(Class<T> type) {
		Assert.state(this.beanFactory != null, () -> "BeanFactory required for resolution of [" + type + "]");
		try {
			return this.beanFactory.getBean(type);
		}
		catch (NoUniqueBeanDefinitionException ex) {
			throw new IllegalStateException("No unique [" + type.getName() + "] bean found in application context - " +
					"mark one as primary, or declare a more specific implementation type for your cache", ex);
		}
		catch (NoSuchBeanDefinitionException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("No bean of type [" + type.getName() + "] found in application context", ex);
			}
			return BeanUtils.instantiateClass(type);
		}
	}

	protected CacheManager getDefaultCacheManager() {
		if (getCacheManager() == null) {
			Assert.state(this.beanFactory != null, "BeanFactory required for default CacheManager resolution");
			try {
				this.cacheManager = SingletonSupplier.of(this.beanFactory.getBean(CacheManager.class));
			}
			catch (NoUniqueBeanDefinitionException ex) {
				throw new IllegalStateException("No unique bean of type CacheManager found. "+
						"Mark one as primary or declare a specific CacheManager to use.");
			}
			catch (NoSuchBeanDefinitionException ex) {
				throw new IllegalStateException("No bean of type CacheManager found. Register a CacheManager "+
						"bean or remove the @EnableCaching annotation from your configuration.");
			}
		}
		return getCacheManager();
	}

	@Override
	protected CacheResolver getDefaultCacheResolver() {
		if (getCacheResolver() == null) {
			this.cacheResolver = SingletonSupplier.of(new SimpleCacheResolver(getDefaultCacheManager()));
		}
		return getCacheResolver();
	}

	@Override
	protected CacheResolver getDefaultExceptionCacheResolver() {
		if (getExceptionCacheResolver() == null) {
			this.exceptionCacheResolver = SingletonSupplier.of(new LazyCacheResolver());
		}
		return getExceptionCacheResolver();
	}

	@Override
	protected KeyGenerator getDefaultKeyGenerator() {
		return this.adaptedKeyGenerator.obtain();
	}


	/**
	 * 仅在需要处理异常时才解析默认的异常缓存解析器。
	 * <p>非 JSR-107 设置需要 {@link CacheManager} 或 {@link CacheResolver}。
	 * 如果只指定了后者，则无法从自定义 {@code CacheResolver} 实现中提取默认的异常
	 * {@code CacheResolver}，因此我们必须回退到 {@code CacheManager}。
	 * <p>这导致了一个奇怪的情况：一个完全有效的配置突然因为启用了 JCache 支持而失效。
	 * 为了避免这种情况，我们尽可能晚地解析默认的异常 {@code CacheResolver}，
	 * 以避免在其他情况下产生这种硬性要求。
	 */
	class LazyCacheResolver implements CacheResolver {

		private final SingletonSupplier<CacheResolver> cacheResolver =
				SingletonSupplier.of(() -> new SimpleExceptionCacheResolver(getDefaultCacheManager()));

		@Override
		public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
			return this.cacheResolver.obtain().resolveCaches(context);
		}
	}

}
