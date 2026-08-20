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

import javax.cache.annotation.CacheResult;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ExceptionTypeFilter;
import org.springframework.util.SerializationUtils;

/**
 * 拦截使用 {@link CacheResult} 注解的方法。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@SuppressWarnings("serial")
class CacheResultInterceptor extends AbstractKeyCacheInterceptor<CacheResultOperation, CacheResult> {

	public CacheResultInterceptor(CacheErrorHandler errorHandler) {
		super(errorHandler);
	}


	@Override
	@Nullable
	protected Object invoke(
			CacheOperationInvocationContext<CacheResultOperation> context, CacheOperationInvoker invoker) {

		CacheResultOperation operation = context.getOperation();
		Object cacheKey = generateKey(context);

		Cache cache = resolveCache(context);
		Cache exceptionCache = resolveExceptionCache(context);

		if (!operation.isAlwaysInvoked()) {
			Cache.ValueWrapper cachedValue = doGet(cache, cacheKey);
			if (cachedValue != null) {
				return cachedValue.get();
			}
			checkForCachedException(exceptionCache, cacheKey);
		}

		try {
			Object invocationResult = invoker.invoke();
			doPut(cache, cacheKey, invocationResult);
			return invocationResult;
		}
		catch (CacheOperationInvoker.ThrowableWrapper ex) {
			Throwable original = ex.getOriginal();
			cacheException(exceptionCache, operation.getExceptionTypeFilter(), cacheKey, original);
			throw ex;
		}
	}

	/**
	 * 检查是否有缓存的异常。如果找到异常，则直接抛出。
	 */
	protected void checkForCachedException(@Nullable Cache exceptionCache, Object cacheKey) {
		if (exceptionCache == null) {
			return;
		}
		Cache.ValueWrapper result = doGet(exceptionCache, cacheKey);
		if (result != null) {
			Throwable ex = (Throwable) result.get();
			Assert.state(ex != null, "No exception in cache");
			throw rewriteCallStack(ex, getClass().getName(), "invoke");
		}
	}

	protected void cacheException(@Nullable Cache exceptionCache, ExceptionTypeFilter filter, Object cacheKey, Throwable ex) {
		if (exceptionCache == null) {
			return;
		}
		if (filter.match(ex.getClass())) {
			doPut(exceptionCache, cacheKey, ex);
		}
	}

	@Nullable
	private Cache resolveExceptionCache(CacheOperationInvocationContext<CacheResultOperation> context) {
		CacheResolver exceptionCacheResolver = context.getOperation().getExceptionCacheResolver();
		if (exceptionCacheResolver != null) {
			return extractFrom(context.getOperation().getExceptionCacheResolver().resolveCaches(context));
		}
		return null;
	}


	/**
	 * 重写指定 {@code exception} 的调用栈，使其与当前调用栈匹配，直到（包括）指定的方法调用。
	 * <p>克隆指定的异常。如果异常不可序列化（{@code serializable}），
	 * 则返回原始异常。如果找不到公共祖先，则返回原始异常。
	 * <p>用于确保缓存的异常具有有效的调用上下文。
	 * @param exception 要与当前调用栈合并的异常
	 * @param className 公共祖先的类名
	 * @param methodName 公共祖先的方法名
	 * @return 一个克隆的异常，其调用栈由当前调用栈（直到并包括由 {@code className} 和
	 * {@code methodName} 参数指定的公共祖先）以及指定 {@code exception} 在公共祖先之后的
	 * 堆栈跟踪元素组成。
	 */
	private static CacheOperationInvoker.ThrowableWrapper rewriteCallStack(
			Throwable exception, String className, String methodName) {

		Throwable clone = cloneException(exception);
		if (clone == null) {
			return new CacheOperationInvoker.ThrowableWrapper(exception);
		}

		StackTraceElement[] callStack = new Exception().getStackTrace();
		StackTraceElement[] cachedCallStack = exception.getStackTrace();

		int index = findCommonAncestorIndex(callStack, className, methodName);
		int cachedIndex = findCommonAncestorIndex(cachedCallStack, className, methodName);
		if (index == -1 || cachedIndex == -1) {
			return new CacheOperationInvoker.ThrowableWrapper(exception); // 无法找到公共祖先
		}
		StackTraceElement[] result = new StackTraceElement[cachedIndex + callStack.length - index];
		System.arraycopy(cachedCallStack, 0, result, 0, cachedIndex);
		System.arraycopy(callStack, index, result, cachedIndex, callStack.length - index);

		clone.setStackTrace(result);
		return new CacheOperationInvoker.ThrowableWrapper(clone);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private static <T extends Throwable> T cloneException(T exception) {
		try {
			return (T) SerializationUtils.deserialize(SerializationUtils.serialize(exception));
		}
		catch (Exception ex) {
			return null;  // 异常参数无法被克隆
		}
	}

	private static int findCommonAncestorIndex(StackTraceElement[] callStack, String className, String methodName) {
		for (int i = 0; i < callStack.length; i++) {
			StackTraceElement element = callStack[i];
			if (className.equals(element.getClassName()) && methodName.equals(element.getMethodName())) {
				return i;
			}
		}
		return -1;
	}

}
