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

package org.springframework.cache.annotation;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.cache.interceptor.AbstractFallbackCacheOperationSource;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link org.springframework.cache.interceptor.CacheOperationSource
 * CacheOperationSource} 接口的实现，用于处理注解格式的缓存元数据。
 *
 * <p>该类读取 Spring 的 {@link Cacheable}、{@link CachePut} 和 {@link CacheEvict}
 * 注解，并将对应的缓存操作定义暴露给 Spring 的缓存基础设施。
 * 该类还可以作为自定义 {@code CacheOperationSource} 的基类。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
@SuppressWarnings("serial")
public class AnnotationCacheOperationSource extends AbstractFallbackCacheOperationSource implements Serializable {

	private final boolean publicMethodsOnly;

	private final Set<CacheAnnotationParser> annotationParsers;


	/**
	 * 创建一个默认的 AnnotationCacheOperationSource，支持带有
	 * {@code Cacheable} 和 {@code CacheEvict} 注解的公共方法。
	 */
	public AnnotationCacheOperationSource() {
		this(true);
	}

	/**
	 * 创建一个默认的 {@code AnnotationCacheOperationSource}，支持带有
	 * {@code Cacheable} 和 {@code CacheEvict} 注解的公共方法。
	 * @param publicMethodsOnly 是否仅支持带注解的公共方法（通常用于基于代理的 AOP），
	 * 还是也支持 protected/private 方法（通常用于 AspectJ 类织入）
	 */
	public AnnotationCacheOperationSource(boolean publicMethodsOnly) {
		this.publicMethodsOnly = publicMethodsOnly;
		this.annotationParsers = Collections.singleton(new SpringCacheAnnotationParser());
	}

	/**
	 * 创建一个自定义的 AnnotationCacheOperationSource。
	 * @param annotationParser 要使用的 CacheAnnotationParser
	 */
	public AnnotationCacheOperationSource(CacheAnnotationParser annotationParser) {
		this.publicMethodsOnly = true;
		Assert.notNull(annotationParser, "CacheAnnotationParser must not be null");
		this.annotationParsers = Collections.singleton(annotationParser);
	}

	/**
	 * 创建一个自定义的 AnnotationCacheOperationSource。
	 * @param annotationParsers 要使用的 CacheAnnotationParser
	 */
	public AnnotationCacheOperationSource(CacheAnnotationParser... annotationParsers) {
		this.publicMethodsOnly = true;
		Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
		this.annotationParsers = new LinkedHashSet<>(Arrays.asList(annotationParsers));
	}

	/**
	 * 创建一个自定义的 AnnotationCacheOperationSource。
	 * @param annotationParsers 要使用的 CacheAnnotationParser
	 */
	public AnnotationCacheOperationSource(Set<CacheAnnotationParser> annotationParsers) {
		this.publicMethodsOnly = true;
		Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
		this.annotationParsers = annotationParsers;
	}


	@Override
	public boolean isCandidateClass(Class<?> targetClass) {
		for (CacheAnnotationParser parser : this.annotationParsers) {
			if (parser.isCandidateClass(targetClass)) {
				return true;
			}
		}
		return false;
	}

	@Override
	@Nullable
	protected Collection<CacheOperation> findCacheOperations(Class<?> clazz) {
		return determineCacheOperations(parser -> parser.parseCacheAnnotations(clazz));
	}

	@Override
	@Nullable
	protected Collection<CacheOperation> findCacheOperations(Method method) {
		return determineCacheOperations(parser -> parser.parseCacheAnnotations(method));
	}

	/**
	 * 为给定的 {@link CacheOperationProvider} 确定缓存操作。
	 * <p>该实现委托给已配置的
	 * {@link CacheAnnotationParser CacheAnnotationParsers}
	 * 将已知注解解析为 Spring 的元数据属性类。
	 * <p>可以被重写以支持携带缓存元数据的自定义注解。
	 * @param provider 要使用的缓存操作提供者
	 * @return 配置好的缓存操作，如果没有找到则返回 {@code null}
	 */
	@Nullable
	protected Collection<CacheOperation> determineCacheOperations(CacheOperationProvider provider) {
		Collection<CacheOperation> ops = null;
		for (CacheAnnotationParser parser : this.annotationParsers) {
			Collection<CacheOperation> annOps = provider.getCacheOperations(parser);
			if (annOps != null) {
				if (ops == null) {
					ops = annOps;
				}
				else {
					Collection<CacheOperation> combined = new ArrayList<>(ops.size() + annOps.size());
					combined.addAll(ops);
					combined.addAll(annOps);
					ops = combined;
				}
			}
		}
		return ops;
	}

	/**
	 * 默认情况下，只有公共方法可以被设置为可缓存。
	 */
	@Override
	protected boolean allowPublicMethodsOnly() {
		return this.publicMethodsOnly;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AnnotationCacheOperationSource)) {
			return false;
		}
		AnnotationCacheOperationSource otherCos = (AnnotationCacheOperationSource) other;
		return (this.annotationParsers.equals(otherCos.annotationParsers) &&
				this.publicMethodsOnly == otherCos.publicMethodsOnly);
	}

	@Override
	public int hashCode() {
		return this.annotationParsers.hashCode();
	}


	/**
	 * 回调接口，根据给定的 {@link CacheAnnotationParser} 提供
	 * {@link CacheOperation} 实例。
	 */
	@FunctionalInterface
	protected interface CacheOperationProvider {

		/**
		 * 返回由指定解析器提供的 {@link CacheOperation} 实例。
		 * @param parser 要使用的解析器
		 * @return 缓存操作，如果没有找到则返回 {@code null}
		 */
		@Nullable
		Collection<CacheOperation> getCacheOperations(CacheAnnotationParser parser);
	}

}
