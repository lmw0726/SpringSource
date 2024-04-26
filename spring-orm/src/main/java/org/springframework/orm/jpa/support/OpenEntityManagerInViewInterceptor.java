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

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.lang.Nullable;
import org.springframework.orm.jpa.EntityManagerFactoryAccessor;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.AsyncWebRequestInterceptor;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;

/**
 * 将 JPA EntityManager 绑定到线程以处理整个请求的 Spring web 请求拦截器。
 * 用于 "Open EntityManager in View" 模式，
 * 即使原始事务已经完成，也允许在 web 视图中进行延迟加载。
 *
 * <p>此拦截器通过当前线程使 JPA EntityManager 可用，事务管理器将自动检测到它。
 * 它适用于通过
 * {@link org.springframework.orm.jpa.JpaTransactionManager} 或 {@link org.springframework.transaction.jta.JtaTransactionManager}
 * 执行的服务层事务以及非事务性只读执行。
 *
 * <p>与 {@link OpenEntityManagerInViewFilter} 相比，此拦截器在 Spring 应用程序上下文中设置，
 * 因此可以利用 bean 自动装配。
 *
 * @author Juergen Hoeller
 * @see OpenEntityManagerInViewFilter
 * @see org.springframework.orm.jpa.JpaTransactionManager
 * @see org.springframework.orm.jpa.SharedEntityManagerCreator
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 * @since 2.0
 */
public class OpenEntityManagerInViewInterceptor extends EntityManagerFactoryAccessor implements AsyncWebRequestInterceptor {

	/**
	 * 附加到 EntityManagerFactory toString 表示形式的后缀，用于 "参与现有实体管理器处理" 请求属性。
	 *
	 * @see #getParticipateAttributeName
	 */
	public static final String PARTICIPATE_SUFFIX = ".PARTICIPATE";


	@Override
	public void preHandle(WebRequest request) throws DataAccessException {
		// 获取参与的属性名称
		String key = getParticipateAttributeName();
		// 获取 WebAsyncManager 实例
		WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
		// 如果异步管理器已经有并发结果，并且应用了实体管理器绑定拦截器，则直接返回，不再继续执行后续操作
		if (asyncManager.hasConcurrentResult() && applyEntityManagerBindingInterceptor(asyncManager, key)) {
			return;
		}

		// 获取 EntityManagerFactory
		EntityManagerFactory emf = obtainEntityManagerFactory();
		if (TransactionSynchronizationManager.hasResource(emf)) {
			// 如果当前事务已经绑定了 EntityManager
			// 不要修改 EntityManager：只需相应地标记请求。
			Integer count = (Integer) request.getAttribute(key, WebRequest.SCOPE_REQUEST);
			// 则将请求中对应属性的计数加一，用于跟踪 EntityManager 的使用次数
			int newCount = (count != null ? count + 1 : 1);
			request.setAttribute(getParticipateAttributeName(), newCount, WebRequest.SCOPE_REQUEST);
		} else {
			logger.debug("Opening JPA EntityManager in OpenEntityManagerInViewInterceptor");
			try {
				// 如果当前事务未绑定 EntityManager
				// 则创建一个新的 EntityManager
				EntityManager em = createEntityManager();
				// 创建一个实体管理持有者
				EntityManagerHolder emHolder = new EntityManagerHolder(em);
				// 并将其绑定到当前事务中
				TransactionSynchronizationManager.bindResource(emf, emHolder);

				// 创建一个异步请求拦截器，用于处理异步请求中的事务上下文
				AsyncRequestInterceptor interceptor = new AsyncRequestInterceptor(emf, emHolder);
				// 注册Callable拦截器
				asyncManager.registerCallableInterceptor(key, interceptor);
				// 注册延迟结果拦截器
				asyncManager.registerDeferredResultInterceptor(key, interceptor);
			} catch (PersistenceException ex) {
				throw new DataAccessResourceFailureException("Could not create JPA EntityManager", ex);
			}
		}
	}

	@Override
	public void postHandle(WebRequest request, @Nullable ModelMap model) {
	}

	@Override
	public void afterCompletion(WebRequest request, @Nullable Exception ex) throws DataAccessException {
		if (!decrementParticipateCount(request)) {
			// 如果无法成功减少请求中对应属性的计数，则说明当前请求中没有绑定的 实体管理器工厂
			// 解绑实体管理器
			EntityManagerHolder emHolder = (EntityManagerHolder)
					TransactionSynchronizationManager.unbindResource(obtainEntityManagerFactory());
			logger.debug("Closing JPA EntityManager in OpenEntityManagerInViewInterceptor");
			// 关闭实体管理器
			EntityManagerFactoryUtils.closeEntityManager(emHolder.getEntityManager());
		}
	}

	private boolean decrementParticipateCount(WebRequest request) {
		// 获取参与的属性名称
		String participateAttributeName = getParticipateAttributeName();
		// 从请求属性中获取计数
		Integer count = (Integer) request.getAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		// 如果计数为空，说明当前请求中没有绑定的 EntityManager
		if (count == null) {
			return false;
		}
		// 不要修改 Session：只需清除标记。
		if (count > 1) {
			// 如果计数大于 1，则将计数减 1；
			request.setAttribute(participateAttributeName, count - 1, WebRequest.SCOPE_REQUEST);
		} else {
			// 否则，从请求属性中移除该属性
			request.removeAttribute(participateAttributeName, WebRequest.SCOPE_REQUEST);
		}
		return true;
	}

	@Override
	public void afterConcurrentHandlingStarted(WebRequest request) {
		if (!decrementParticipateCount(request)) {
			// 如果无法减少参与计数，则解绑 实体管理器工厂
			TransactionSynchronizationManager.unbindResource(obtainEntityManagerFactory());
		}
	}

	/**
	 * 返回标识请求已经被过滤的请求属性名称。默认实现获取 EntityManagerFactory 实例的 toString 表示形式，
	 * 并附加 ".FILTERED"。
	 *
	 * @see #PARTICIPATE_SUFFIX
	 */
	protected String getParticipateAttributeName() {
		return obtainEntityManagerFactory().toString() + PARTICIPATE_SUFFIX;
	}


	private boolean applyEntityManagerBindingInterceptor(WebAsyncManager asyncManager, String key) {
		// 获取 Callable处理拦截器
		CallableProcessingInterceptor cpi = asyncManager.getCallableInterceptor(key);
		// 如果未找到 Callable处理拦截器，则返回 false
		if (cpi == null) {
			return false;
		}
		// 绑定 EntityManager
		((AsyncRequestInterceptor) cpi).bindEntityManager();
		return true;
	}

}
