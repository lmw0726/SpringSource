/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

/**
 * 用于使用 JSR-107 缓存注解进行声明式缓存管理的 AOP Alliance MethodInterceptor。
 *
 * <p>继承自 {@link JCacheAspectSupport} 类，该类包含与 Spring 底层缓存 API 的集成。
 * JCacheInterceptor 仅调用相关的父类方法。
 *
 * <p>JCacheInterceptor 是线程安全的。
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.cache.interceptor.CacheInterceptor
 */
@SuppressWarnings("serial")
public class JCacheInterceptor extends JCacheAspectSupport implements MethodInterceptor, Serializable {

	/**
	 * 使用默认错误处理器构造一个新的 {@code JCacheInterceptor}。
	 */
	public JCacheInterceptor() {
	}

	/**
	 * 使用给定的错误处理器构造一个新的 {@code JCacheInterceptor}。
	 * @param errorHandler 用于提供错误处理器的 Supplier，如果 Supplier 无法解析，则应用默认错误处理器
	 * @since 5.1
	 */
	public JCacheInterceptor(@Nullable Supplier<CacheErrorHandler> errorHandler) {
		this.errorHandler = new SingletonSupplier<>(errorHandler, SimpleCacheErrorHandler::new);
	}


	@Override
	@Nullable
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();

		CacheOperationInvoker aopAllianceInvoker = () -> {
			try {
				return invocation.proceed();
			}
			catch (Throwable ex) {
				throw new CacheOperationInvoker.ThrowableWrapper(ex);
			}
		};

		Object target = invocation.getThis();
		Assert.state(target != null, "Target must not be null");
		try {
			return execute(aopAllianceInvoker, target, method, invocation.getArguments());
		}
		catch (CacheOperationInvoker.ThrowableWrapper th) {
			throw th.getOriginal();
		}
	}

}
