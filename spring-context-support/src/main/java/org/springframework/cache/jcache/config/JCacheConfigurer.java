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

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.lang.Nullable;

/**
 * JSR-107 实现的 {@link CachingConfigurer} 扩展。
 *
 * <p>由使用 {@link org.springframework.cache.annotation.EnableCaching} 注解的类实现，
 * 这些类希望或需要显式指定注解驱动缓存管理中异常缓存的解析方式。
 *
 * <p>有关通用示例和上下文，请参阅 {@link org.springframework.cache.annotation.EnableCaching}；
 * 有关详细说明，请参阅 {@link #exceptionCacheResolver()}。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see CachingConfigurer
 * @see org.springframework.cache.annotation.EnableCaching
 */
public interface JCacheConfigurer extends CachingConfigurer {

	/**
	 * 返回用于解析注解驱动缓存管理中异常缓存的 {@link CacheResolver} bean。
	 * 实现类必须显式声明 {@link org.springframework.context.annotation.Bean @Bean}，例如：
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig extends JCacheConfigurerSupport {
	 *     &#064;Bean // 重要！
	 *     &#064;Override
	 *     public CacheResolver exceptionCacheResolver() {
	 *         // 配置并返回 CacheResolver 实例
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * 有关更完整的示例，请参阅 {@link org.springframework.cache.annotation.EnableCaching}。
	 */
	@Nullable
	default CacheResolver exceptionCacheResolver() {
		return null;
	}

}
