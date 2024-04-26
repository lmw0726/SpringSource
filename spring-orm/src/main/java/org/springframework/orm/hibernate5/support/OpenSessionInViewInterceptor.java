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

package org.springframework.orm.hibernate5.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.lang.Nullable;
import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.orm.hibernate5.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;
import org.springframework.web.context.request.AsyncWebRequestInterceptor;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;

/**
 * Spring web 请求拦截器，将 Hibernate {@code Session} 绑定到整个请求的线程上。
 *
 * <p>此类是 "Open Session in View" 模式的具体表达，这是一种模式，允许在 web 视图中进行关联的延迟加载，
 * 尽管原始事务已经完成。
 *
 * <p>此拦截器通过当前线程使 Hibernate Session 可用，事务管理器将自动检测到它。它适用于通过
 * {@link org.springframework.orm.hibernate5.HibernateTransactionManager} 执行的服务层事务，
 * 以及非事务性执行（如果适当配置）。
 *
 * <p>与 {@link OpenSessionInViewFilter} 相比，此拦截器是在 Spring 应用程序上下文中配置的，
 * 因此可以利用 bean 自动装配。
 *
 * <p><b>警告:</b> 将此拦截器应用于现有逻辑可能会导致以前未出现的问题，因为在整个请求的处理中使用单个
 * Hibernate {@code Session}。特别是，必须在请求处理的开始时重新关联持久对象与 Hibernate {@code Session}，
 * 以避免与已加载的相同对象实例的冲突。
 *
 * @author Juergen Hoeller
 * @see OpenSessionInViewFilter
 * @see OpenSessionInterceptor
 * @see org.springframework.orm.hibernate5.HibernateTransactionManager
 * @see TransactionSynchronizationManager
 * @see SessionFactory#getCurrentSession()
 * @since 4.2
 */
public class OpenSessionInViewInterceptor implements AsyncWebRequestInterceptor {

	/**
	 * {@code SessionFactory} {@code toString()} 表示形式的后缀，用于 "参与现有会话处理" 请求属性。
	 *
	 * @see #getParticipateAttributeName
	 */
	public static final String PARTICIPATE_SUFFIX = ".PARTICIPATE";

	/**
	 * 日志记录器
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Hibernate 会话工厂
	 */
	@Nullable
	private SessionFactory sessionFactory;


	/**
	 * 设置应该用于创建 Hibernate Session 的 Hibernate SessionFactory。
	 */
	public void setSessionFactory(@Nullable SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * 返回应该用于创建 Hibernate Session 的 Hibernate SessionFactory。
	 */
	@Nullable
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	private SessionFactory obtainSessionFactory() {
		SessionFactory sf = getSessionFactory();
		Assert.state(sf != null, "No SessionFactory set");
		return sf;
	}


	/**
	 * 打开一个新的 Hibernate {@code Session} 并通过 {@link TransactionSynchronizationManager} 将其绑定到线程。
	 */
	@Override
	public void preHandle(WebRequest request) throws DataAccessException {
		// 获取参与属性的名称
		String key = getParticipateAttributeName();
		// 获取异步管理器
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		// 如果异步管理器具有并发结果并且应用了会话绑定拦截器，则返回
		if (asyncManager.hasConcurrentResult() && applySessionBindingInterceptor(asyncManager, key)) {
			return;
		}

		// 如果事务同步管理器具有SessionFactory资源
		if (TransactionSynchronizationManager.hasResource(obtainSessionFactory())) {
			// 不要修改Session：只需适当地标记请求
			Integer count = (Integer) request.getAttribute(key, WebRequest.SCOPE_REQUEST);
			int newCount = (count != null ? count + 1 : 1);
			request.setAttribute(getParticipateAttributeName(), newCount, WebRequest.SCOPE_REQUEST);
		} else {
			// 如果没有Hibernate Session
			logger.debug("Opening Hibernate Session in OpenSessionInViewInterceptor");
			// 打开会话
			Session session = openSession();
			// 创建Session持有者
			SessionHolder sessionHolder = new SessionHolder(session);
			// 将会话持有者和SessionFactory绑定到事务同步管理器的资源中
			TransactionSynchronizationManager.bindResource(obtainSessionFactory(), sessionHolder);

			// 创建AsyncRequestInterceptor
			AsyncRequestInterceptor asyncRequestInterceptor =
					new AsyncRequestInterceptor(obtainSessionFactory(), sessionHolder);
			// 注册为Callable拦截器
			asyncManager.registerCallableInterceptor(key, asyncRequestInterceptor);
			// 注册延迟结果拦截器
			asyncManager.registerDeferredResultInterceptor(key, asyncRequestInterceptor);
		}
	}

	@Override
	public void postHandle(WebRequest request, @Nullable ModelMap model) {
	}

	/**
	 * 解绑线程上的 Hibernate {@code Session} 并关闭它）。
	 *
	 * @see TransactionSynchronizationManager
	 */
	@Override
	public void afterCompletion(WebRequest request, @Nullable Exception ex) throws DataAccessException {
		// 如果减少参与计数失败
		if (!decrementParticipateCount(request)) {
			// 解绑SessionFactory资源，获取SessionHolder
			SessionHolder sessionHolder =
					(SessionHolder) TransactionSynchronizationManager.unbindResource(obtainSessionFactory());
			logger.debug("Closing Hibernate Session in OpenSessionInViewInterceptor");
			// 关闭Hibernate会话
			SessionFactoryUtils.closeSession(sessionHolder.getSession());
		}
	}

	private boolean decrementParticipateCount(WebRequest request) {
		// 获取参与计数属性名
		String participateAttributeName = getParticipateAttributeName();
		// 从请求属性中获取参与计数
		Integer count = (Integer) request.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		// 如果计数为空，返回false
		if (count == null) {
			return false;
		}
		// 不要修改 Session：只需清除标记。
		if (count > 1) {
			// 如果计数大于1，递减计数并更新请求属性
			request.setAttribute(participateAttributeName, count - 1, WebRequest.SCOPE_REQUEST);
		} else {
			// 如果计数为1，直接移除请求属性
			request.removeAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		}
		return true;
	}

	@Override
	public void afterConcurrentHandlingStarted(WebRequest request) {
		if (!decrementParticipateCount(request)) {
			TransactionSynchronizationManager.unbindResource(obtainSessionFactory());
		}
	}

	/**
	 * 为此拦截器使用的 SessionFactory 打开一个 Session。
	 * <p>默认实现委托给 {@link SessionFactory#openSession} 方法，并将 {@link Session} 的刷新模式设置为 "MANUAL"。
	 *
	 * @return 要使用的 Session
	 * @throws DataAccessResourceFailureException 如果无法创建 Session
	 * @see FlushMode#MANUAL
	 */
	protected Session openSession() throws DataAccessResourceFailureException {
		try {
			// 打开会话
			Session session = obtainSessionFactory().openSession();
			// 设置刷新模式
			session.setHibernateFlushMode(FlushMode.MANUAL);
			return session;
		} catch (HibernateException ex) {
			throw new DataAccessResourceFailureException("Could not open Hibernate Session", ex);
		}
	}

	/**
	 * 返回标识请求已经被拦截的请求属性的名称。
	 * <p>默认实现获取 {@code SessionFactory} 实例的 {@code toString()} 表示形式，并附加 {@link #PARTICIPATE_SUFFIX}。
	 */
	protected String getParticipateAttributeName() {
		return obtainSessionFactory().toString() + PARTICIPATE_SUFFIX;
	}

	private boolean applySessionBindingInterceptor(WebAsyncManager asyncManager, String key) {
		// 获取异步管理器中的Callable拦截器
		CallableProcessingInterceptor cpi = asyncManager.getCallableInterceptor(key);
		// 如果拦截器为空，返回false
		if (cpi == null) {
			return false;
		}
		// 将拦截器转换为AsyncRequestInterceptor，并绑定会话
		((AsyncRequestInterceptor) cpi).bindSession();
		return true;
	}

}
