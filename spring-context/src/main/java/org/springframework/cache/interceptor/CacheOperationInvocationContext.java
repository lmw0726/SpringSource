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

import java.lang.reflect.Method;

/**
 * 缓存操作调用上下文的表示。
 *
 * <p>缓存操作是静态的，与特定的调用无关；
 * 本接口将操作与特定的调用关联起来。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @param <O> 操作类型
 */
public interface CacheOperationInvocationContext<O extends BasicOperation> {

	/**
	 * 返回缓存操作。
	 */
	O getOperation();

	/**
	 * 返回调用方法时所针对的目标实例。
	 */
	Object getTarget();

	/**
	 * 返回被调用的方法。
	 */
	Method getMethod();

	/**
	 * 返回用于调用方法的参数列表。
	 */
	Object[] getArgs();

}
