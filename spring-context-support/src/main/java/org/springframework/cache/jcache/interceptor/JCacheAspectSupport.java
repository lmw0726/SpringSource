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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.interceptor.AbstractCacheInvoker;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * JSR-107 缓存切面的基类，例如 {@link JCacheInterceptor}
 * 或 AspectJ 切面。
 *
 * <p>使用 Spring 缓存抽象进行缓存相关操作。处理标准 JSR-107 缓存注解时，
 * 不需要 JSR-107 的 {@link javax.cache.Cache} 或 {@link javax.cache.CacheManager}。
 *
 * <p>{@link JCacheOperationSource} 用于确定缓存操作
 *
 * <p>如果其 {@code JCacheOperationSource} 是可序列化的，则缓存切面也是可序列化的。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see org.springframework.cache.interceptor.CacheAspectSupport
 * @see KeyGeneratorAdapter
 * @see CacheResolverAdapter
 */
public class JCacheAspectSupport extends AbstractCacheInvoker implements InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private JCacheOperationSource cacheOperationSource;

	@Nullable
	private CacheResultInterceptor cacheResultInterceptor;

	@Nullable
	private CachePutInterceptor cachePutInterceptor;

	@Nullable
	private CacheRemoveEntryInterceptor cacheRemoveEntryInterceptor;

	@Nullable
	private CacheRemoveAllInterceptor cacheRemoveAllInterceptor;

	private boolean initialized = false;


	/**
	 * 设置此缓存切面的 CacheOperationSource。
	 */
	public void setCacheOperationSource(JCacheOperationSource cacheOperationSource) {
		Assert.notNull(cacheOperationSource, "JCacheOperationSource must not be null");
		this.cacheOperationSource = cacheOperationSource;
	}

	/**
	 * 返回此缓存切面的 CacheOperationSource。
	 */
	public JCacheOperationSource getCacheOperationSource() {
		Assert.state(this.cacheOperationSource != null, "The 'cacheOperationSource' property is required: " +
				"If there are no cacheable methods, then don't use a cache aspect.");
		return this.cacheOperationSource;
	}

	@Override
	public void afterPropertiesSet() {
		getCacheOperationSource();

		this.cacheResultInterceptor = new CacheResultInterceptor(getErrorHandler());
		this.cachePutInterceptor = new CachePutInterceptor(getErrorHandler());
		this.cacheRemoveEntryInterceptor = new CacheRemoveEntryInterceptor(getErrorHandler());
		this.cacheRemoveAllInterceptor = new CacheRemoveAllInterceptor(getErrorHandler());

		this.initialized = true;
	}


	@Nullable
	protected Object execute(CacheOperationInvoker invoker, Object target, Method method, Object[] args) {
		// 检查切面是否已启用，以处理 AspectJ 被自动引入的情况
		if (this.initialized) {
			Class<?> targetClass = AopProxyUtils.ultimateTargetClass(target);
			JCacheOperation<?> operation = getCacheOperationSource().getCacheOperation(method, targetClass);
			if (operation != null) {
				CacheOperationInvocationContext<?> context =
						createCacheOperationInvocationContext(target, args, operation);
				return execute(context, invoker);
			}
		}

		return invoker.invoke();
	}

	@SuppressWarnings("unchecked")
	private CacheOperationInvocationContext<?> createCacheOperationInvocationContext(
			Object target, Object[] args, JCacheOperation<?> operation) {

		return new DefaultCacheInvocationContext<>(
				(JCacheOperation<Annotation>) operation, target, args);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	private Object execute(CacheOperationInvocationContext<?> context, CacheOperationInvoker invoker) {
		CacheOperationInvoker adapter = new CacheOperationInvokerAdapter(invoker);
		BasicOperation operation = context.getOperation();

		if (operation instanceof CacheResultOperation) {
			Assert.state(this.cacheResultInterceptor != null, "No CacheResultInterceptor");
			return this.cacheResultInterceptor.invoke(
					(CacheOperationInvocationContext<CacheResultOperation>) context, adapter);
		}
		else if (operation instanceof CachePutOperation) {
			Assert.state(this.cachePutInterceptor != null, "No CachePutInterceptor");
			return this.cachePutInterceptor.invoke(
					(CacheOperationInvocationContext<CachePutOperation>) context, adapter);
		}
		else if (operation instanceof CacheRemoveOperation) {
			Assert.state(this.cacheRemoveEntryInterceptor != null, "No CacheRemoveEntryInterceptor");
			return this.cacheRemoveEntryInterceptor.invoke(
					(CacheOperationInvocationContext<CacheRemoveOperation>) context, adapter);
		}
		else if (operation instanceof CacheRemoveAllOperation) {
			Assert.state(this.cacheRemoveAllInterceptor != null, "No CacheRemoveAllInterceptor");
			return this.cacheRemoveAllInterceptor.invoke(
					(CacheOperationInvocationContext<CacheRemoveAllOperation>) context, adapter);
		}
		else {
			throw new IllegalArgumentException("Cannot handle " + operation);
		}
	}

	/**
	 * 执行底层操作（通常在缓存未命中时），并返回调用结果。
	 * 如果发生异常，将被包装在 {@code ThrowableWrapper} 中：
	 * 异常可以被处理或修改，但<em>必须</em>同样包装在 {@code ThrowableWrapper} 中。
	 * @param invoker 处理被缓存操作的调用器
	 * @return 调用结果
	 * @see CacheOperationInvoker#invoke()
	 */
	@Nullable
	protected Object invokeOperation(CacheOperationInvoker invoker) {
		return invoker.invoke();
	}


	private class CacheOperationInvokerAdapter implements CacheOperationInvoker {

		private final CacheOperationInvoker delegate;

		public CacheOperationInvokerAdapter(CacheOperationInvoker delegate) {
			this.delegate = delegate;
		}

		@Override
		public Object invoke() throws ThrowableWrapper {
			return invokeOperation(this.delegate);
		}
	}

}
