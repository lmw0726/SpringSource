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

package org.springframework.cache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;

/**
 * 一个简单的 {@link CacheOperationSource} 实现，允许根据已注册的名称来匹配属性。
 *
 * @author Costin Leau
 * @since 3.1
 */
@SuppressWarnings("serial")
public class NameMatchCacheOperationSource implements CacheOperationSource, Serializable {

	/**
	 * 可供子类使用的日志记录器。
	 * <p>声明为静态以利于序列化。
	 */
	protected static final Log logger = LogFactory.getLog(NameMatchCacheOperationSource.class);


	/** 键是方法名；值是对应的 TransactionAttributes。 */
	private Map<String, Collection<CacheOperation>> nameMap = new LinkedHashMap<>();


	/**
	 * 设置一个名称/属性映射，包含方法名
	 * （例如 "myMethod"）和 CacheOperation 实例
	 * （或者将被转换为 CacheOperation 实例的字符串）。
	 * @see CacheOperation
	 */
	public void setNameMap(Map<String, Collection<CacheOperation>> nameMap) {
		nameMap.forEach(this::addCacheMethod);
	}

	/**
	 * 为可缓存方法添加一个属性。
	 * <p>方法名可以是精确匹配，也可以是 "xxx*"、"*xxx" 或 "*xxx*" 这样的模式，
	 * 用于匹配多个方法。
	 * @param methodName 方法的名称
	 * @param ops 与方法关联的操作
	 */
	public void addCacheMethod(String methodName, Collection<CacheOperation> ops) {
		if (logger.isDebugEnabled()) {
			logger.debug("Adding method [" + methodName + "] with cache operations [" + ops + "]");
		}
		this.nameMap.put(methodName, ops);
	}

	@Override
	@Nullable
	public Collection<CacheOperation> getCacheOperations(Method method, @Nullable Class<?> targetClass) {
		// 查找直接匹配的名称
		String methodName = method.getName();
		Collection<CacheOperation> ops = this.nameMap.get(methodName);

		if (ops == null) {
			// 查找最具体的名称匹配。
			String bestNameMatch = null;
			for (String mappedName : this.nameMap.keySet()) {
				if (isMatch(methodName, mappedName)
						&& (bestNameMatch == null || bestNameMatch.length() <= mappedName.length())) {
					ops = this.nameMap.get(mappedName);
					bestNameMatch = mappedName;
				}
			}
		}

		return ops;
	}

	/**
	 * 返回给定的方法名是否与映射的名称匹配。
	 * <p>默认实现会检查 "xxx*"、"*xxx" 和 "*xxx*" 的匹配，
	 * 以及直接相等。可以在子类中重写。
	 * @param methodName 类的方法名称
	 * @param mappedName 描述符中的名称
	 * @return 名称是否匹配
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isMatch(String methodName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, methodName);
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NameMatchCacheOperationSource)) {
			return false;
		}
		NameMatchCacheOperationSource otherTas = (NameMatchCacheOperationSource) other;
		return ObjectUtils.nullSafeEquals(this.nameMap, otherTas.nameMap);
	}

	@Override
	public int hashCode() {
		return NameMatchCacheOperationSource.class.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.nameMap;
	}
}
