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

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.hibernate5.SessionFactoryUtils;
import org.springframework.orm.hibernate5.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet过滤器，将一个Hibernate Session绑定到整个请求处理过程的线程上。
 * 用于"Open Session in View"模式，即使原始事务已经完成，也允许在Web视图中进行延迟加载。
 *
 * <p>此过滤器通过当前线程使Hibernate Sessions可用，将被事务管理器自动检测到。
 * 它适用于通过{@link org.springframework.orm.hibernate5.HibernateTransactionManager}的服务层事务，
 * 以及非事务执行（如果适当配置）。
 *
 * <p><b>注意</b>：此过滤器默认不会刷新Hibernate Session，将刷新模式设置为{@code FlushMode.MANUAL}。
 * 它假设与处理刷新的服务层事务组合使用：活动事务管理器将在读写事务期间暂时更改刷新模式为{@code FlushMode.AUTO}，
 * 在每个事务结束时将刷新模式重置为{@code FlushMode.MANUAL}。
 *
 * <p><b>警告：</b>将此过滤器应用于现有逻辑可能会引发以前未出现的问题，因为使用单个Hibernate Session来处理整个请求。
 * 特别是，持久对象与Hibernate Session的重新关联必须发生在请求处理的最开始，以避免与已加载的相同对象实例的冲突。
 *
 * <p>在Spring的根Web应用程序上下文中查找SessionFactory。
 * 在{@code web.xml}中支持一个名为"sessionFactoryBeanName"的过滤器初始化参数；
 * 默认的bean名称是"sessionFactory"。
 *
 * @author Juergen Hoeller
 * @see #lookupSessionFactory
 * @see OpenSessionInViewInterceptor
 * @see OpenSessionInterceptor
 * @see org.springframework.orm.hibernate5.HibernateTransactionManager
 * @see TransactionSynchronizationManager
 * @see SessionFactory#getCurrentSession()
 * @since 4.2
 */
public class OpenSessionInViewFilter extends OncePerRequestFilter {

	/**
	 * Session工厂的默认bean名称。
	 */
	public static final String DEFAULT_SESSION_FACTORY_BEAN_NAME = "sessionFactory";

	/**
	 * Hibernate Session工厂Bean名称
	 */
	private String sessionFactoryBeanName = DEFAULT_SESSION_FACTORY_BEAN_NAME;


	/**
	 * 设置要从Spring的根应用程序上下文中获取的SessionFactory的bean名称。默认值为"sessionFactory"。
	 *
	 * @见 #DEFAULT_SESSION_FACTORY_BEAN_NAME
	 */
	public void setSessionFactoryBeanName(String sessionFactoryBeanName) {
		this.sessionFactoryBeanName = sessionFactoryBeanName;
	}

	/**
	 * 返回要从Spring的根应用程序上下文中获取的SessionFactory的bean名称。
	 */
	protected String getSessionFactoryBeanName() {
		return this.sessionFactoryBeanName;
	}


	/**
	 * 返回"false"，以便过滤器可以重新将已打开的Hibernate Session绑定到每个异步调度线程上，
	 * 并将关闭它延迟到最后一个异步调度。
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * 返回"false"，以便过滤器可以为每个错误调度提供一个Hibernate Session。
	 */
	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// 查找 Session工厂
		SessionFactory sessionFactory = lookupSessionFactory(request);

		// 参与标志，默认为 false
		boolean participate = false;

		// 获取 Web异步管理器
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		// 获取已过滤属性名称
		String key = getAlreadyFilteredAttributeName();

		// 如果当前线程已经绑定了 Session工厂，设置参与标志为 true
		if (TransactionSynchronizationManager.hasResource(sessionFactory)) {
			// 不修改 Session，只设置参与标志
			participate = true;
		} else {
			// 检查是否是第一个请求或者在异步调度期间未应用会话绑定拦截器
			boolean isFirstRequest = !isAsyncDispatch(request);
			if (isFirstRequest || !applySessionBindingInterceptor(asyncManager, key)) {
				logger.debug("Opening Hibernate Session in OpenSessionInViewFilter");
				// 打开 Hibernate Session
				Session session = openSession(sessionFactory);
				// 将 Session 绑定到当前线程
				SessionHolder sessionHolder = new SessionHolder(session);
				// 绑定Session工厂和Session持有者
				TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder);
				// 注册异步请求拦截器
				AsyncRequestInterceptor interceptor = new AsyncRequestInterceptor(sessionFactory, sessionHolder);
				// 注册回调拦截器
				asyncManager.registerCallableInterceptor(key, interceptor);
				// 注册延迟结果拦截器
				asyncManager.registerDeferredResultInterceptor(key, interceptor);
			}
		}

		try {
			// 调用过滤器链的下一个过滤器
			filterChain.doFilter(request, response);
		} finally {
			// 如果没有参与标志，则关闭 Session
			if (!participate) {
				// 解绑Session工厂
				SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.unbindResource(sessionFactory);
				// 如果请求不是异步启动的，则关闭 Session
				if (!isAsyncStarted(request)) {
					logger.debug("Closing Hibernate Session in OpenSessionInViewFilter");
					// 关闭Session
					SessionFactoryUtils.closeSession(sessionHolder.getSession());
				}
			}
		}
	}

	/**
	 * 查找此过滤器应该使用的SessionFactory，以当前HTTP请求作为参数。
	 * <p>默认实现委托给不带参数的{@link #lookupSessionFactory()}变体。
	 *
	 * @param request 当前请求
	 * @return 要使用的SessionFactory
	 */
	protected SessionFactory lookupSessionFactory(HttpServletRequest request) {
		return lookupSessionFactory();
	}

	/**
	 * 查找此过滤器应该使用的SessionFactory。
	 * <p>默认实现在Spring的根应用程序上下文中查找具有指定名称的bean。
	 *
	 * @return 要使用的SessionFactory
	 * @见 #getSessionFactoryBeanName
	 */
	protected SessionFactory lookupSessionFactory() {
		// 如果调试日志级别已启用，则记录使用的 SessionFactory 的信息
		if (logger.isDebugEnabled()) {
			logger.debug("Using SessionFactory '" + getSessionFactoryBeanName() + "' for OpenSessionInViewFilter");
		}

		// 从 Servlet 上下文获取 Web 应用程序上下文
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());

		// 从 Web 应用程序上下文中获取指定名称的 Session工厂 bean，并返回
		return wac.getBean(getSessionFactoryBeanName(), SessionFactory.class);
	}

	/**
	 * 为此过滤器使用的SessionFactory打开一个Session。
	 * <p>默认实现委托给{@link SessionFactory#openSession}方法，并将{@link Session}的刷新模式设置为"MANUAL"。
	 *
	 * @param sessionFactory 此过滤器使用的SessionFactory
	 * @return 要使用的Session
	 * @throws DataAccessResourceFailureException 如果无法创建Session
	 * @see FlushMode#MANUAL
	 */
	protected Session openSession(SessionFactory sessionFactory) throws DataAccessResourceFailureException {
		try {
			// 打开Session会话
			Session session = sessionFactory.openSession();
			// 设置刷新模式
			session.setHibernateFlushMode(FlushMode.MANUAL);
			return session;
		} catch (HibernateException ex) {
			throw new DataAccessResourceFailureException("Could not open Hibernate Session", ex);
		}
	}

	private boolean applySessionBindingInterceptor(WebAsyncManager asyncManager, String key) {
		// 从异步管理器中获取给定键的可调用处理拦截器
		CallableProcessingInterceptor cpi = asyncManager.getCallableInterceptor(key);

		// 如果拦截器为空，则返回 false
		if (cpi == null) {
			return false;
		}

		// 将拦截器强制转换为异步请求拦截器，并调用其绑定会话的方法
		((AsyncRequestInterceptor) cpi).bindSession();

		// 返回 true，表示成功绑定会话
		return true;
	}

}
