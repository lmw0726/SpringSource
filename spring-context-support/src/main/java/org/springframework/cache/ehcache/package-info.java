/**
 * 针对开源缓存
 * <a href="https://www.ehcache.org/">EhCache 2.x</a> 的支撑类，
 * 允许在 Spring 上下文中将 EhCache CacheManager 和 Caches
 * 配置为 Bean。
 *
 * <p>注意：EhCache 3.x 位于不同的包命名空间中，
 * 不包含在此处的传统支撑类中。
 * 建议通过 JCache（JSR-107）使用它，Spring 在
 * {@code org.springframework.cache.jcache} 中提供了相应支持。
 */
@NonNullApi
@NonNullFields
package org.springframework.cache.ehcache;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
