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

import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.lang.Nullable;

/**
 * 供标注了 @{@link org.springframework.context.annotation.Configuration
 * Configuration} 注解且带有 @{@link EnableCaching} 注解的类实现的接口，这些类希望或需要
 * 显式指定如何解析缓存，以及如何为基于注解的缓存管理生成键。
 *
 * <p>一般示例和上下文请参阅 @{@link EnableCaching}；详细说明请参阅
 * {@link #cacheManager()}、{@link #cacheResolver()} 和 {@link #keyGenerator()}。
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1
 * @see EnableCaching
 */
public interface CachingConfigurer {

	/**
	 * 返回用于基于注解的缓存管理的缓存管理器（CacheManager）Bean。
	 * 默认的 {@link CacheResolver} 会在幕后使用该缓存管理器进行初始化。
	 * 若需要对缓存解析进行更细粒度的管理，可考虑直接设置
	 * {@link CacheResolver}。
	 * <p>实现类必须显式声明
	 * {@link org.springframework.context.annotation.Bean @Bean}，例如
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     &#064;Bean // 重要！
	 *     &#064;Override
	 *     public CacheManager cacheManager() {
	 *         // 配置并返回 CacheManager 实例
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * 更完整的示例请参阅 @{@link EnableCaching}。
	 */
	@Nullable
	default CacheManager cacheManager() {
		return null;
	}

	/**
	 * 返回用于基于注解的缓存管理中解析常规缓存的 {@link CacheResolver} Bean。
	 * 这是指定要使用的 {@link CacheManager} 的一种替代且更强大的方式。
	 * <p>如果同时设置了 {@link #cacheManager()} 和 {@code #cacheResolver()}，
	 * 则缓存管理器（cache manager）将被忽略。
	 * <p>实现类必须显式声明
	 * {@link org.springframework.context.annotation.Bean @Bean}，例如
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     &#064;Bean // 重要！
	 *     &#064;Override
	 *     public CacheResolver cacheResolver() {
	 *         // 配置并返回 CacheResolver 实例
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * 更完整的示例请参阅 {@link EnableCaching}。
	 */
	@Nullable
	default CacheResolver cacheResolver() {
		return null;
	}

	/**
	 * 返回用于基于注解的缓存管理的键生成器（KeyGenerator）Bean。
	 * 实现类必须显式声明
	 * {@link org.springframework.context.annotation.Bean @Bean}，例如
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     &#064;Bean // 重要！
	 *     &#064;Override
	 *     public KeyGenerator keyGenerator() {
	 *         // 配置并返回 KeyGenerator 实例
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * 更完整的示例请参阅 @{@link EnableCaching}。
	 */
	@Nullable
	default KeyGenerator keyGenerator() {
		return null;
	}

	/**
	 * 返回用于处理缓存相关错误的 {@link CacheErrorHandler}。
	 * <p>默认情况下，使用 {@link org.springframework.cache.interceptor.SimpleCacheErrorHandler}，
	 * 它会直接将异常抛回给客户端。
	 * <p>实现类必须显式声明
	 * {@link org.springframework.context.annotation.Bean @Bean}，例如
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig extends CachingConfigurerSupport {
	 *     &#064;Bean // 重要！
	 *     &#064;Override
	 *     public CacheErrorHandler errorHandler() {
	 *         // 配置并返回 CacheErrorHandler 实例
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * 更完整的示例请参阅 @{@link EnableCaching}。
	 */
	@Nullable
	default CacheErrorHandler errorHandler() {
		return null;
	}

}
