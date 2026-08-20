/*
 * Copyright 2002-2015 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @CacheConfig} 提供了一种在类级别共享通用缓存相关设置的机制。
 *
 * <p>当该注解标注在某个类上时，它会为定义在该类中的任何缓存操作提供一组默认设置。
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheConfig {

	/**
	 * 为被注解类中定义的缓存操作所考虑的默认缓存名称。
	 * <p>如果操作级别未设置缓存名称，则使用这些名称而不是默认值。
	 * <p>可用于确定目标缓存（或缓存集合），与限定符值或特定 bean 定义的 bean 名称相匹配。
	 */
	String[] cacheNames() default {};

	/**
	 * 用于该类的默认 {@link org.springframework.cache.interceptor.KeyGenerator} 的 bean 名称。
	 * <p>如果操作级别未设置，则使用该 key generator 而不是默认值。
	 * <p>key generator 与自定义 key 的使用互斥。当为操作定义了自定义 key 时，
	 * 该 key generator 的值将被忽略。
	 */
	String keyGenerator() default "";

	/**
	 * 自定义 {@link org.springframework.cache.CacheManager} 的 bean 名称，当尚未设置
	 * {@link org.springframework.cache.interceptor.CacheResolver} 时，
	 * 用于创建默认的 {@link org.springframework.cache.interceptor.CacheResolver}。
	 * <p>如果操作级别未设置 resolver 和 cache manager，并且未通过 {@link #cacheResolver}
	 * 设置 cache resolver，则使用该 cache manager 而不是默认值。
	 * @see org.springframework.cache.interceptor.SimpleCacheResolver
	 */
	String cacheManager() default "";

	/**
	 * 要使用的自定义 {@link org.springframework.cache.interceptor.CacheResolver} 的 bean 名称。
	 * <p>如果操作级别未设置 resolver 和 cache manager，则使用该 cache resolver 而不是默认值。
	 */
	String cacheResolver() default "";

}
