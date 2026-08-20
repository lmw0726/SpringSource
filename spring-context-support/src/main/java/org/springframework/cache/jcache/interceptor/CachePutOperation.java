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

import java.lang.reflect.Method;
import java.util.List;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CachePut;

import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.lang.Nullable;
import org.springframework.util.ExceptionTypeFilter;

/**
 * {@link CachePut} 操作的 {@link JCacheOperation} 实现。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see CachePut
 */
class CachePutOperation extends AbstractJCacheKeyOperation<CachePut> {

	private final ExceptionTypeFilter exceptionTypeFilter;

	private final CacheParameterDetail valueParameterDetail;


	public CachePutOperation(
			CacheMethodDetails<CachePut> methodDetails, CacheResolver cacheResolver, KeyGenerator keyGenerator) {

		super(methodDetails, cacheResolver, keyGenerator);

		CachePut ann = methodDetails.getCacheAnnotation();
		this.exceptionTypeFilter = createExceptionTypeFilter(ann.cacheFor(), ann.noCacheFor());

		CacheParameterDetail valueParameterDetail =
				initializeValueParameterDetail(methodDetails.getMethod(), this.allParameterDetails);
		if (valueParameterDetail == null) {
			throw new IllegalArgumentException("No parameter annotated with @CacheValue was found for " +
					methodDetails.getMethod());
		}
		this.valueParameterDetail = valueParameterDetail;
	}


	@Override
	public ExceptionTypeFilter getExceptionTypeFilter() {
		return this.exceptionTypeFilter;
	}

	/**
	 * 指定是否在调用方法之前更新缓存。默认情况下，缓存会在方法调用之后更新。
	 * @see javax.cache.annotation.CachePut#afterInvocation()
	 */
	public boolean isEarlyPut() {
		return !getCacheAnnotation().afterInvocation();
	}

	/**
	 * 返回持有要缓存值的参数对应的 {@link CacheInvocationParameter}。
	 * <p>方法参数必须与相关方法调用的签名相匹配。
	 * @param values 特定调用的参数值
	 * @return 值参数的 {@link CacheInvocationParameter} 实例
	 */
	public CacheInvocationParameter getValueParameter(Object... values) {
		int parameterPosition = this.valueParameterDetail.getParameterPosition();
		if (parameterPosition >= values.length) {
			throw new IllegalStateException("Values mismatch, value parameter at position " +
					parameterPosition + " cannot be matched against " + values.length + " value(s)");
		}
		return this.valueParameterDetail.toCacheInvocationParameter(values[parameterPosition]);
	}


	@Nullable
	private static CacheParameterDetail initializeValueParameterDetail(
			Method method, List<CacheParameterDetail> allParameters) {

		CacheParameterDetail result = null;
		for (CacheParameterDetail parameter : allParameters) {
			if (parameter.isValue()) {
				if (result == null) {
					result = parameter;
				}
				else {
					throw new IllegalArgumentException("More than one @CacheValue found on " + method + "");
				}
			}
		}
		return result;
	}

}
