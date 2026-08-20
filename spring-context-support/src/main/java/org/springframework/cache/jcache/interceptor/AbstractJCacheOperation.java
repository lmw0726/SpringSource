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

package org.springframework.cache.jcache.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheValue;

import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.util.Assert;
import org.springframework.util.ExceptionTypeFilter;

/**
 * 一个基础的 {@link JCacheOperation} 实现。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @param <A> 注解类型
 */
abstract class AbstractJCacheOperation<A extends Annotation> implements JCacheOperation<A> {

	private final CacheMethodDetails<A> methodDetails;

	private final CacheResolver cacheResolver;

	protected final List<CacheParameterDetail> allParameterDetails;


	/**
	 * 构造一个新的 {@code AbstractJCacheOperation}。
	 * @param methodDetails 与被缓存方法相关的 {@link CacheMethodDetails}
	 * @param cacheResolver 用于解析常规缓存的缓存解析器
	 */
	protected AbstractJCacheOperation(CacheMethodDetails<A> methodDetails, CacheResolver cacheResolver) {
		Assert.notNull(methodDetails, "CacheMethodDetails must not be null");
		Assert.notNull(cacheResolver, "CacheResolver must not be null");
		this.methodDetails = methodDetails;
		this.cacheResolver = cacheResolver;
		this.allParameterDetails = initializeAllParameterDetails(methodDetails.getMethod());
	}

	private static List<CacheParameterDetail> initializeAllParameterDetails(Method method) {
		int parameterCount = method.getParameterCount();
		List<CacheParameterDetail> result = new ArrayList<>(parameterCount);
		for (int i = 0; i < parameterCount; i++) {
			CacheParameterDetail detail = new CacheParameterDetail(method, i);
			result.add(detail);
		}
		return result;
	}


	@Override
	public Method getMethod() {
		return this.methodDetails.getMethod();
	}

	@Override
	public Set<Annotation> getAnnotations() {
		return this.methodDetails.getAnnotations();
	}

	@Override
	public A getCacheAnnotation() {
		return this.methodDetails.getCacheAnnotation();
	}

	@Override
	public String getCacheName() {
		return this.methodDetails.getCacheName();
	}

	@Override
	public Set<String> getCacheNames() {
		return Collections.singleton(getCacheName());
	}

	@Override
	public CacheResolver getCacheResolver() {
		return this.cacheResolver;
	}

	@Override
	public CacheInvocationParameter[] getAllParameters(Object... values) {
		if (this.allParameterDetails.size() != values.length) {
			throw new IllegalStateException("Values mismatch, operation has " +
					this.allParameterDetails.size() + " parameter(s) but got " + values.length + " value(s)");
		}
		List<CacheInvocationParameter> result = new ArrayList<>();
		for (int i = 0; i < this.allParameterDetails.size(); i++) {
			result.add(this.allParameterDetails.get(i).toCacheInvocationParameter(values[i]));
		}
		return result.toArray(new CacheInvocationParameter[0]);
	}


	/**
	 * 返回用于过滤方法调用过程中抛出的异常的 {@link ExceptionTypeFilter}。
	 * @see #createExceptionTypeFilter
	 */
	public abstract ExceptionTypeFilter getExceptionTypeFilter();

	/**
	 * 子类用来创建特定 {@code ExceptionTypeFilter} 的便捷方法。
	 * @see #getExceptionTypeFilter()
	 */
	protected ExceptionTypeFilter createExceptionTypeFilter(
			Class<? extends Throwable>[] includes, Class<? extends Throwable>[] excludes) {

		return new ExceptionTypeFilter(Arrays.asList(includes), Arrays.asList(excludes), true);
	}


	@Override
	public String toString() {
		return getOperationDescription().append(']').toString();
	}

	/**
	 * 返回此缓存操作的标识性描述。
	 * <p>可供子类使用，以包含在其 {@code toString()} 结果中。
	 */
	protected StringBuilder getOperationDescription() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName());
		result.append('[');
		result.append(this.methodDetails);
		return result;
	}


	/**
	 * 单个缓存参数的详细信息。
	 */
	protected static class CacheParameterDetail {

		private final Class<?> rawType;

		private final Set<Annotation> annotations;

		private final int parameterPosition;

		private final boolean isKey;

		private final boolean isValue;

		public CacheParameterDetail(Method method, int parameterPosition) {
			this.rawType = method.getParameterTypes()[parameterPosition];
			this.annotations = new LinkedHashSet<>();
			boolean foundKeyAnnotation = false;
			boolean foundValueAnnotation = false;
			for (Annotation annotation : method.getParameterAnnotations()[parameterPosition]) {
				this.annotations.add(annotation);
				if (CacheKey.class.isAssignableFrom(annotation.annotationType())) {
					foundKeyAnnotation = true;
				}
				if (CacheValue.class.isAssignableFrom(annotation.annotationType())) {
					foundValueAnnotation = true;
				}
			}
			this.parameterPosition = parameterPosition;
			this.isKey = foundKeyAnnotation;
			this.isValue = foundValueAnnotation;
		}

		public int getParameterPosition() {
			return this.parameterPosition;
		}

		protected boolean isKey() {
			return this.isKey;
		}

		protected boolean isValue() {
			return this.isValue;
		}

		public CacheInvocationParameter toCacheInvocationParameter(Object value) {
			return new CacheInvocationParameterImpl(this, value);
		}
	}


	/**
	 * 单个缓存调用参数。
	 */
	protected static class CacheInvocationParameterImpl implements CacheInvocationParameter {

		private final CacheParameterDetail detail;

		private final Object value;

		public CacheInvocationParameterImpl(CacheParameterDetail detail, Object value) {
			this.detail = detail;
			this.value = value;
		}

		@Override
		public Class<?> getRawType() {
			return this.detail.rawType;
		}

		@Override
		public Object getValue() {
			return this.value;
		}

		@Override
		public Set<Annotation> getAnnotations() {
			return this.detail.annotations;
		}

		@Override
		public int getParameterPosition() {
			return this.detail.parameterPosition;
		}
	}

}
