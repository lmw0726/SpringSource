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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheMethodDetails;

import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;

/**
 * 操作键的基类 {@link JCacheOperation}。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @param <A> 注解类型
 */
abstract class AbstractJCacheKeyOperation<A extends Annotation> extends AbstractJCacheOperation<A> {

	private final KeyGenerator keyGenerator;

	private final List<CacheParameterDetail> keyParameterDetails;


	/**
	 * 创建新实例。
	 * @param methodDetails 与缓存方法相关的 {@link CacheMethodDetails}
	 * @param cacheResolver 用于解析常规缓存的缓存解析器
	 * @param keyGenerator 用于计算缓存键的键生成器
	 */
	protected AbstractJCacheKeyOperation(CacheMethodDetails<A> methodDetails,
			CacheResolver cacheResolver, KeyGenerator keyGenerator) {

		super(methodDetails, cacheResolver);
		this.keyGenerator = keyGenerator;
		this.keyParameterDetails = initializeKeyParameterDetails(this.allParameterDetails);
	}


	/**
	 * 返回用于计算缓存键的 {@link KeyGenerator}。
	 */
	public KeyGenerator getKeyGenerator() {
		return this.keyGenerator;
	}

	/**
	 * 返回用于计算键的参数的 {@link CacheInvocationParameter}。
	 * <p>根据规范，如果某些方法参数使用了 {@link javax.cache.annotation.CacheKey} 注解，
	 * 则只有这些参数应该是键的一部分。如果没有参数使用注解，则除了使用了
	 * {@link javax.cache.annotation.CacheValue} 注解的参数外，所有参数都应该是键的一部分。
	 * <p>方法参数必须与相关方法调用的签名匹配
	 * @param values 特定调用的参数值
	 * @return 用于计算键的参数的 {@link CacheInvocationParameter} 实例
	 */
	public CacheInvocationParameter[] getKeyParameters(Object... values) {
		List<CacheInvocationParameter> result = new ArrayList<>();
		for (CacheParameterDetail keyParameterDetail : this.keyParameterDetails) {
			int parameterPosition = keyParameterDetail.getParameterPosition();
			if (parameterPosition >= values.length) {
				throw new IllegalStateException("Values mismatch, key parameter at position "
						+ parameterPosition + " cannot be matched against " + values.length + " value(s)");
			}
			result.add(keyParameterDetail.toCacheInvocationParameter(values[parameterPosition]));
		}
		return result.toArray(new CacheInvocationParameter[0]);
	}


	private static List<CacheParameterDetail> initializeKeyParameterDetails(List<CacheParameterDetail> allParameters) {
		List<CacheParameterDetail> all = new ArrayList<>();
		List<CacheParameterDetail> annotated = new ArrayList<>();
		for (CacheParameterDetail allParameter : allParameters) {
			if (!allParameter.isValue()) {
				all.add(allParameter);
			}
			if (allParameter.isKey()) {
				annotated.add(allParameter);
			}
		}
		return (annotated.isEmpty() ? all : annotated);
	}

}
