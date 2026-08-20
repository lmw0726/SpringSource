/**
 * 用于声明式 JSR-107 缓存配置的支持包。当常规的 Spring 缓存配置检测到
 * JSR-107 API 和 Spring 的 JCache 实现时，会使用此包。
 *
 * <p>提供 {@code CachingConfigurer} 的扩展，用于暴露异常缓存解析器，
 * 参见 {@code JCacheConfigurer}。
 */
@NonNullApi
@NonNullFields
package org.springframework.cache.jcache.config;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
