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

package org.springframework.cache.annotation;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * 抽象基类 {@code @Configuration}，为启用 Spring 的注解驱动缓存管理能力提供通用结构。
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 3.1
 * @see EnableCaching
 */
@Configuration(proxyBeanMethods = false)
public abstract class AbstractCachingConfiguration implements ImportAware {

	@Nullable
	protected AnnotationAttributes enableCaching;

	@Nullable
	protected Supplier<CacheManager> cacheManager;

	@Nullable
	protected Supplier<CacheResolver> cacheResolver;

	@Nullable
	protected Supplier<KeyGenerator> keyGenerator;

	@Nullable
	protected Supplier<CacheErrorHandler> errorHandler;


	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		this.enableCaching = AnnotationAttributes.fromMap(
				importMetadata.getAnnotationAttributes(EnableCaching.class.getName()));
		if (this.enableCaching == null) {
			throw new IllegalArgumentException(
					"@EnableCaching is not present on importing class " + importMetadata.getClassName());
		}
	}

	@Autowired
	void setConfigurers(ObjectProvider<CachingConfigurer> configurers) {
		Supplier<CachingConfigurer> configurer = () -> {
			List<CachingConfigurer> candidates = configurers.stream().collect(Collectors.toList());
			if (CollectionUtils.isEmpty(candidates)) {
				return null;
			}
			if (candidates.size() > 1) {
				throw new IllegalStateException(candidates.size() + " implementations of " +
						"CachingConfigurer were found when only 1 was expected. " +
						"Refactor the configuration such that CachingConfigurer is " +
						"implemented only once or not at all.");
			}
			return candidates.get(0);
		};
		useCachingConfigurer(new CachingConfigurerSupplier(configurer));
	}

	/**
	 * 从指定的 {@link CachingConfigurer} 中提取配置。
	 */
	protected void useCachingConfigurer(CachingConfigurerSupplier cachingConfigurerSupplier) {
		this.cacheManager = cachingConfigurerSupplier.adapt(CachingConfigurer::cacheManager);
		this.cacheResolver = cachingConfigurerSupplier.adapt(CachingConfigurer::cacheResolver);
		this.keyGenerator = cachingConfigurerSupplier.adapt(CachingConfigurer::keyGenerator);
		this.errorHandler = cachingConfigurerSupplier.adapt(CachingConfigurer::errorHandler);
	}


	protected static class CachingConfigurerSupplier {

		private final Supplier<CachingConfigurer> supplier;

		public CachingConfigurerSupplier(Supplier<CachingConfigurer> supplier) {
			this.supplier = SingletonSupplier.of(supplier);
		}

		/**
		 * 将 {@link CachingConfigurer} 供应商适配为由指定映射函数提供的另一个供应商。
		 * 如果底层的 {@link CachingConfigurer} 为 {@code null}，则返回 {@code null}，
		 * 并且不会调用映射函数。
		 * @param provider 用于适配供应商的提供者
		 * @param <T> 供应商的类型
		 * @return 由指定函数映射得到的另一个供应商
		 */
		@Nullable
		public <T> Supplier<T> adapt(Function<CachingConfigurer, T> provider) {
			return () -> {
				CachingConfigurer cachingConfigurer = this.supplier.get();
				return (cachingConfigurer != null ? provider.apply(cachingConfigurer) : null);
			};
		}

	}

}
