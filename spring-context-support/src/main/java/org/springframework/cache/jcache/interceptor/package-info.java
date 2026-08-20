/**
 * 基于AOP的解决方案，使用JSR-107注解实现声明式缓存划分。
 *
 * <p>强烈依赖于org.springframework.cache.interceptor中的基础设施，
 * 该基础设施处理Spring的缓存注解。
 *
 * <p>基于org.springframework.aop.framework中的AOP基础设施构建。
 * 任何POJO都可以通过Spring实现缓存增强。
 */
@NonNullApi
@NonNullFields
package org.springframework.cache.jcache.interceptor;

import org.springframework.lang.NonNullApi;
import org.springframework.lang.NonNullFields;
