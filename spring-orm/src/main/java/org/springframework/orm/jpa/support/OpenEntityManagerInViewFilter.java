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

package org.springframework.orm.jpa.support;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet过滤器，将JPA EntityManager绑定到整个请求的处理线程中。用于"Open EntityManager in View"模式，
 * 即使原始事务已经完成，也允许在Web视图中进行延迟加载。
 *
 * <p>此过滤器通过当前线程使JPA EntityManagers可用，事务管理器将自动检测到它。适用于通过{@link org.springframework.orm.jpa.JpaTransactionManager}
 * 或 {@link org.springframework.transaction.jta.JtaTransactionManager} 进行的服务层事务以及非事务性只读执行。
 *
 * <p>在Spring的根Web应用程序上下文中查找EntityManagerFactory。在{@code web.xml} 中的 "entityManagerFactoryBeanName" 过滤器初始化参数中支持；
 * 默认的bean名称为 "entityManagerFactory"。作为替代方案，"persistenceUnitName" 初始化参数允许通过逻辑单元名称检索（在 {@code persistence.xml} 中指定）。
 *
 * @author Juergen Hoeller
 * @see OpenEntityManagerInViewInterceptor
 * @see org.springframework.orm.jpa.JpaTransactionManager
 * @see org.springframework.orm.jpa.SharedEntityManagerCreator
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 * @since 2.0
 */
public class OpenEntityManagerInViewFilter extends OncePerRequestFilter {

	/**
	 * 默认的EntityManagerFactory bean名称："entityManagerFactory"。
	 * 仅在没有指定 "persistenceUnitName" 参数时适用。
	 *
	 * @see #setEntityManagerFactoryBeanName
	 * @see #setPersistenceUnitName
	 */
	public static final String DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME = "entityManagerFactory";


	/**
	 * 实体管理器工厂 Bean 的名称。
	 */
	@Nullable
	private String entityManagerFactoryBeanName;

	/**
	 * 持久化单元的名称。
	 */
	@Nullable
	private String persistenceUnitName;

	/**
	 * 实体管理器工厂。
	 */
	@Nullable
	private volatile EntityManagerFactory entityManagerFactory;


	/**
	 * 设置要从Spring的根应用程序上下文中获取的EntityManagerFactory的bean名称。
	 * <p>默认为 "entityManagerFactory"。注意，只有在没有指定 "persistenceUnitName" 参数时，默认值才适用。
	 *
	 * @see #setPersistenceUnitName
	 * @see #DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME
	 */
	public void setEntityManagerFactoryBeanName(@Nullable String entityManagerFactoryBeanName) {
		this.entityManagerFactoryBeanName = entityManagerFactoryBeanName;
	}

	/**
	 * 返回要从Spring的根应用程序上下文中获取的EntityManagerFactory的bean名称。
	 */
	@Nullable
	protected String getEntityManagerFactoryBeanName() {
		return this.entityManagerFactoryBeanName;
	}

	/**
	 * 设置要访问EntityManagerFactory的持久单元的名称。
	 * <p>这是一种替代方法，用于通过bean名称解析EntityManagerFactory，而不是通过其持久性单元名称解析它。
	 * 如果没有指定bean名称和持久单元名称，我们将检查是否存在用于默认bean名称 "entityManagerFactory" 的bean；
	 * 如果没有，将通过查找类型为 EntityManagerFactory 的单个唯一bean来检索默认EntityManagerFactory。
	 *
	 * @see #setEntityManagerFactoryBeanName
	 * @see #DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME
	 */
	public void setPersistenceUnitName(@Nullable String persistenceUnitName) {
		this.persistenceUnitName = persistenceUnitName;
	}

	/**
	 * 返回要访问EntityManagerFactory的持久单元的名称，如果有的话。
	 */
	@Nullable
	protected String getPersistenceUnitName() {
		return this.persistenceUnitName;
	}


	/**
	 * 返回 "false"，以便过滤器可以重新绑定已打开的 {@code EntityManager} 到每个异步调度的线程，并延迟关闭它直到最后一个异步调度。
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * 返回 "false"，以便过滤器可以为每个错误调度提供 {@code EntityManager}。
	 */
	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// 通过 请求 获取 实体管理器工厂 对象
		EntityManagerFactory emf = lookupEntityManagerFactory(request);

		// 定义一个布尔变量，用于标识当前是否已经参与了事务
		boolean participate = false;

		// 获取 Web异步管理器 对象
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		// 获取已过滤属性的名称
		String key = getAlreadyFilteredAttributeName();

		// 如果当前线程中已经绑定了 实体管理器工厂
		if (TransactionSynchronizationManager.hasResource(emf)) {
			// 设置 participate 为 true，表示不修改 EntityManager，只设置参与标志位
			participate = true;
		} else {
			// 如果是第一个请求，或者未应用 实体管理器 绑定拦截器
			boolean isFirstRequest = !isAsyncDispatch(request);
			if (isFirstRequest || !applyEntityManagerBindingInterceptor(asyncManager, key)) {
				logger.debug("Opening JPA EntityManager in OpenEntityManagerInViewFilter");
				try {
					// 则创建一个新的 实体管理器
					EntityManager em = createEntityManager(emf);
					EntityManagerHolder emHolder = new EntityManagerHolder(em);
					// 绑定 实体管理器持有者 和 实体管理器
					TransactionSynchronizationManager.bindResource(emf, emHolder);

					// 构建异步请求拦截器
					AsyncRequestInterceptor interceptor = new AsyncRequestInterceptor(emf, emHolder);
					// 注册回调拦截器
					asyncManager.registerCallableInterceptor(key, interceptor);
					// 注册延迟结果拦截器
					asyncManager.registerDeferredResultInterceptor(key, interceptor);
				} catch (PersistenceException ex) {
					throw new DataAccessResourceFailureException("Could not create JPA EntityManager", ex);
				}
			}
		}

		try {
			// 执行过滤器链的下一个过滤器
			filterChain.doFilter(request, response);
		} finally {
			// 如果当前没有参与事务，则释放 EntityManager
			if (!participate) {
				EntityManagerHolder emHolder = (EntityManagerHolder) TransactionSynchronizationManager.unbindResource(emf);
				if (!isAsyncStarted(request)) {
					// 如果异步请求还没有开始
					logger.debug("Closing JPA EntityManager in OpenEntityManagerInViewFilter");
					// 关闭实体管理器
					EntityManagerFactoryUtils.closeEntityManager(emHolder.getEntityManager());
				}
			}
		}
	}

	/**
	 * 查找此过滤器应使用的 EntityManagerFactory，以当前 HTTP 请求作为参数。
	 * <p>默认实现委托给 {@code lookupEntityManagerFactory}，不带参数，一旦获取到 EntityManagerFactory 引用，就会缓存它。
	 *
	 * @return 要使用的 EntityManagerFactory
	 * @see #lookupEntityManagerFactory()
	 */
	protected EntityManagerFactory lookupEntityManagerFactory(HttpServletRequest request) {
		// 获取已经存在的 实体管理器工厂 对象
		EntityManagerFactory emf = this.entityManagerFactory;

		// 如果 实体管理器工厂 为 null
		if (emf == null) {
			// 重新获取 实体管理器工厂
			emf = lookupEntityManagerFactory();
			// 并将其赋值给 实体管理器工厂 变量
			this.entityManagerFactory = emf;
		}

		// 返回 实体管理器工厂 对象
		return emf;
	}

	/**
	 * 查找此过滤器应使用的 EntityManagerFactory。
	 * <p>默认实现在 Spring 的根 Web 应用程序上下文中查找指定名称的 bean。
	 *
	 * @return 要使用的 EntityManagerFactory
	 * @see #getEntityManagerFactoryBeanName
	 */
	protected EntityManagerFactory lookupEntityManagerFactory() {
		// 获取 Web应用上下文 对象
		WebApplicationContext wac = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());

		// 获取 实体管理器工厂 Bean 的名称
		String emfBeanName = getEntityManagerFactoryBeanName();

		// 获取持久化单元的名称
		String puName = getPersistenceUnitName();

		if (StringUtils.hasLength(emfBeanName)) {
			// 如果 实体管理器工厂 Bean 的名称不为空，则返回对应的 实体管理器工厂 的实例
			return wac.getBean(emfBeanName, EntityManagerFactory.class);
		} else if (!StringUtils.hasLength(puName) && wac.containsBean(DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME)) {
			// 如果持久化单元的名称为空，并且容器中包含默认的实体管理器工厂 Bean，则返回默认的 实体管理器工厂 对象
			return wac.getBean(DEFAULT_ENTITY_MANAGER_FACTORY_BEAN_NAME, EntityManagerFactory.class);
		} else {
			// 否则，使用持久化单元的名称在容器中查找 实体管理器工厂 对象
			// 包括通过类型查找单个 实体管理器工厂 Bean 的后备搜索。
			return EntityManagerFactoryUtils.findEntityManagerFactory(wac, puName);
		}
	}

	/**
	 * 创建要绑定到请求的 JPA EntityManager。
	 * <p>可在子类中重写。
	 *
	 * @param emf 要使用的 EntityManagerFactory
	 * @see javax.persistence.EntityManagerFactory#createEntityManager()
	 */
	protected EntityManager createEntityManager(EntityManagerFactory emf) {
		return emf.createEntityManager();
	}

	private boolean applyEntityManagerBindingInterceptor(WebAsyncManager asyncManager, String key) {
		// 从异步管理器获取可调用处理拦截器
		CallableProcessingInterceptor cpi = asyncManager.getCallableInterceptor(key);

		// 如果拦截器为 null，则返回 false
		if (cpi == null) {
			return false;
		}

		// 将拦截器转换为 异步请求拦截器 类型，并绑定 实体管理器
		((AsyncRequestInterceptor) cpi).bindEntityManager();

		// 返回 true 表示成功绑定 实体管理器
		return true;
	}

}
