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

package org.springframework.orm.jpa.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;

import javax.persistence.EntityManagerFactory;
import java.util.concurrent.Callable;

/**
 * 一个用于 OpenSessionInViewFilter 和 OpenSessionInViewInterceptor 中处理异步 Web 请求的拦截器。
 * <p>
 * 确保以下几点：
 * 1) 在“可调用处理”开始时，会话被绑定/解除绑定
 * 2) 如果异步请求超时或发生错误，会话将关闭
 *
 * @author Rossen Stoyanchev
 * @since 3.2.5
 */
class AsyncRequestInterceptor implements CallableProcessingInterceptor, DeferredResultProcessingInterceptor {

	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(AsyncRequestInterceptor.class);

	/**
	 * 用于管理 JPA 实体管理器。
	 */
	private final EntityManagerFactory emFactory;

	/**
	 * 用于持有当前线程的 JPA 实体管理器。
	 */
	private final EntityManagerHolder emHolder;

	/**
	 * 表示是否有超时操作正在进行的标志。
	 */
	private volatile boolean timeoutInProgress;

	/**
	 * 表示是否有错误操作正在进行的标志。
	 */
	private volatile boolean errorInProgress;

	/**
	 * 构造函数，接受 EntityManagerFactory 和 EntityManagerHolder 作为参数。
	 *
	 * @param emFactory 用于创建 JPA 实体管理器的工厂
	 * @param emHolder  持有当前实体管理器的对象
	 */
	public AsyncRequestInterceptor(EntityManagerFactory emFactory, EntityManagerHolder emHolder) {
		this.emFactory = emFactory;
		this.emHolder = emHolder;
	}


	@Override
	public <T> void preProcess(NativeWebRequest request, Callable<T> task) {
		bindEntityManager();
	}

	/**
	 * 绑定当前实体管理器到线程上下文。
	 */
	public void bindEntityManager() {
		// 没有超时操作正在处理
		this.timeoutInProgress = false;
		// 没有错误操作正在处理
		this.errorInProgress = false;
		// 将 JPA 实体管理器持有者 绑定到当前 JPA 实体管理器工厂 上的资源
		TransactionSynchronizationManager.bindResource(this.emFactory, this.emHolder);
	}

	@Override
	public <T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) {
		TransactionSynchronizationManager.unbindResource(this.emFactory);
	}

	@Override
	public <T> Object handleTimeout(NativeWebRequest request, Callable<T> task) {
		this.timeoutInProgress = true;
		// 给其他拦截器一个处理超时的机会
		return RESULT_NONE;
	}

	@Override
	public <T> Object handleError(NativeWebRequest request, Callable<T> task, Throwable t) {
		this.errorInProgress = true;
		// 给其他拦截器一个处理错误的机会
		return RESULT_NONE;
	}

	@Override
	public <T> void afterCompletion(NativeWebRequest request, Callable<T> task) throws Exception {
		closeEntityManager();
	}

	/**
	 * 关闭当前实体管理器，如果发生了超时或错误。
	 */
	private void closeEntityManager() {
		// 如果超时或错误标志为 true
		if (this.timeoutInProgress || this.errorInProgress) {
			// 记录调试信息，表示在异步请求超时或错误后关闭 JPA EntityManager
			logger.debug("Closing JPA EntityManager after async request timeout/error");
			// 关闭 JPA实体管理器
			EntityManagerFactoryUtils.closeEntityManager(this.emHolder.getEntityManager());
		}
	}

	// 实现 DeferredResultProcessingInterceptor 接口的方法

	@Override
	public <T> boolean handleTimeout(NativeWebRequest request, DeferredResult<T> deferredResult) {
		this.timeoutInProgress = true;
		// 给其他拦截器一个处理超时的机会
		return true;
	}

	@Override
	public <T> boolean handleError(NativeWebRequest request, DeferredResult<T> deferredResult, Throwable t) {
		this.errorInProgress = true;
		// 给其他拦截器一个处理错误的机会
		return true;
	}

	@Override
	public <T> void afterCompletion(NativeWebRequest request, DeferredResult<T> deferredResult) {
		closeEntityManager();
	}

}
