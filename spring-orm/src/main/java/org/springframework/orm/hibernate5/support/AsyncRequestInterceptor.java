/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.orm.hibernate5.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.orm.hibernate5.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.DeferredResultProcessingInterceptor;

import java.util.concurrent.Callable;

/**
 * 用于在 OpenSessionInViewFilter 和 OpenSessionInViewInterceptor 中处理异步 Web 请求的拦截器。
 * 确保以下内容：
 * 1) 当“可调用处理”开始时，会话被绑定/解除绑定
 * 2) 如果异步请求超时或发生错误，会话将关闭
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
class AsyncRequestInterceptor implements CallableProcessingInterceptor, DeferredResultProcessingInterceptor {

	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(AsyncRequestInterceptor.class);

	/**
	 * 用于创建 Hibernate 会话的工厂
	 */
	private final SessionFactory sessionFactory;

	/**
	 * 持有当前会话的对象
	 */
	private final SessionHolder sessionHolder;

	/**
	 * 表示是否有超时操作正在进行的标志。
	 */
	private volatile boolean timeoutInProgress;

	/**
	 * 表示是否有错误操作正在进行的标志。
	 */
	private volatile boolean errorInProgress;


	/**
	 * 构造函数，接受 SessionFactory 和 SessionHolder 作为参数。
	 *
	 * @param sessionFactory 用于创建 Hibernate 会话的工厂
	 * @param sessionHolder  持有当前会话的对象
	 */
	public AsyncRequestInterceptor(SessionFactory sessionFactory, SessionHolder sessionHolder) {
		this.sessionFactory = sessionFactory;
		this.sessionHolder = sessionHolder;
	}


	@Override
	public <T> void preProcess(NativeWebRequest request, Callable<T> task) {
		bindSession();
	}

	/**
	 * 绑定当前会话到线程上下文。
	 */
	public void bindSession() {
		// 没有超时操作正在处理
		this.timeoutInProgress = false;
		// 没有错误操作正在处理
		this.errorInProgress = false;
		// 将 会话持有者 绑定到当前 会话工厂 上的资源
		TransactionSynchronizationManager.bindResource(this.sessionFactory, this.sessionHolder);
	}

	@Override
	public <T> void postProcess(NativeWebRequest request, Callable<T> task, Object concurrentResult) {
		TransactionSynchronizationManager.unbindResource(this.sessionFactory);
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
		closeSession();
	}

	/**
	 * 关闭当前会话，如果发生了超时或错误。
	 */
	private void closeSession() {
		// 如果当前有超时或错误正在处理中
		if (this.timeoutInProgress || this.errorInProgress) {
			// 记录调试信息，说明在异步请求超时或错误后关闭 Hibernate Session
			logger.debug("Closing Hibernate Session after async request timeout/error");
			// 关闭当前 会话持有者 中的 Hibernate 会话
			SessionFactoryUtils.closeSession(this.sessionHolder.getSession());
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
		closeSession();
	}

}
