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

package org.springframework.ejb.access;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.naming.Context;
import javax.naming.NamingException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.jndi.JndiObjectLocator;
import org.springframework.lang.Nullable;

/**
 * 调用本地或远程无状态会话 Bean 的 AOP 拦截器基类。
 * 专为 EJB 2.x 设计，但同样适用于 EJB 3 会话 Bean。
 *
 * <p>此类拦截器必须是通知链中的最后一个拦截器。
 * 在这种情况下，不存在直接的目标对象：调用以特殊方式处理，
 * 在通过 EJB Home 获取的 EJB 实例上执行。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class AbstractSlsbInvokerInterceptor extends JndiObjectLocator
		implements MethodInterceptor {

	private boolean lookupHomeOnStartup = true;

	private boolean cacheHome = true;

	private boolean exposeAccessContext = false;

	/**
	 * EJB 的 Home 对象，可能会被缓存。
	 * 类型必须为 Object，因为它可能是 EJBHome 或 EJBLocalHome。
	 */
	@Nullable
	private Object cachedHome;

	/**
	 * EJB Home 所需的无参 create() 方法，可能会被缓存。
	 */
	@Nullable
	private Method createMethod;

	private final Object homeMonitor = new Object();


	/**
	 * 设置是否在启动时查找 EJB Home 对象。
	 * 默认值为 "true"。
	 * <p>可以关闭此选项以允许 EJB 服务器延迟启动。
	 * 在这种情况下，EJB Home 对象将在首次访问时获取。
	 * @see #setCacheHome
	 */
	public void setLookupHomeOnStartup(boolean lookupHomeOnStartup) {
		this.lookupHomeOnStartup = lookupHomeOnStartup;
	}

	/**
	 * 设置是否在定位 EJB Home 对象后对其进行缓存。
	 * 默认值为 "true"。
	 * <p>可以关闭此选项以允许 EJB 服务器热重启。
	 * 在这种情况下，每次调用都将获取 EJB Home 对象。
	 * @see #setLookupHomeOnStartup
	 */
	public void setCacheHome(boolean cacheHome) {
		this.cacheHome = cacheHome;
	}

	/**
	 * 设置是否为所有对目标 EJB 的访问公开 JNDI 环境上下文，
	 * 即为公开对象引用上的所有方法调用公开。
	 * <p>默认值为 "false"，即仅为对象查找公开 JNDI 上下文。
	 * 将此标志切换为 "true" 以在每次 EJB 调用时公开 JNDI 环境
	 * （包括授权上下文），如 WebLogic 对具有授权要求的 EJB 所需。
	 */
	public void setExposeAccessContext(boolean exposeAccessContext) {
		this.exposeAccessContext = exposeAccessContext;
	}


	/**
	 * 在启动时获取 EJB Home（如果必要）。
	 * @see #setLookupHomeOnStartup
	 * @see #refreshHome
	 */
	@Override
	public void afterPropertiesSet() throws NamingException {
		super.afterPropertiesSet();
		if (this.lookupHomeOnStartup) {
			// 查找 EJB Home 和 create 方法
			refreshHome();
		}
	}

	/**
	 * 刷新缓存的 Home 对象（如果适用）。
	 * 同时缓存 Home 对象上的 create 方法。
	 * @throws NamingException 如果 JNDI 查找抛出异常
	 * @see #lookup
	 * @see #getCreateMethod
	 */
	protected void refreshHome() throws NamingException {
		synchronized (this.homeMonitor) {
			Object home = lookup();
			if (this.cacheHome) {
				this.cachedHome = home;
				this.createMethod = getCreateMethod(home);
			}
		}
	}

	/**
	 * 确定给定 EJB Home 对象的 create 方法。
	 * @param home EJB Home 对象
	 * @return create 方法
	 * @throws EjbAccessException 如果无法获取该方法
	 */
	@Nullable
	protected Method getCreateMethod(Object home) throws EjbAccessException {
		try {
			// 缓存必须在 Home 接口上声明的 EJB create() 方法。
			return home.getClass().getMethod("create");
		}
		catch (NoSuchMethodException ex) {
			throw new EjbAccessException("EJB home [" + home + "] has no no-arg create() method");
		}
	}

	/**
	 * 返回要使用的 EJB Home 对象。每次调用时都会被调用。
	 * <p>默认实现返回初始化时创建的 Home（如果有）；
	 * 否则，它会调用 lookup 为每次调用获取新的代理。
	 * <p>可以在子类中重写此方法，例如在指定时间内缓存 Home 对象
	 * 然后重新创建，或者测试 Home 对象是否仍然存活。
	 * @return 用于调用的 EJB Home 对象
	 * @throws NamingException 如果代理创建失败
	 * @see #lookup
	 * @see #getCreateMethod
	 */
	protected Object getHome() throws NamingException {
		if (!this.cacheHome || (this.lookupHomeOnStartup && !isHomeRefreshable())) {
			return (this.cachedHome != null ? this.cachedHome : lookup());
		}
		else {
			synchronized (this.homeMonitor) {
				if (this.cachedHome == null) {
					this.cachedHome = lookup();
					this.createMethod = getCreateMethod(this.cachedHome);
				}
				return this.cachedHome;
			}
		}
	}

	/**
	 * 返回缓存的 EJB Home 对象是否可能支持按需刷新。
	 * 默认值为 "false"。
	 */
	protected boolean isHomeRefreshable() {
		return false;
	}


	/**
	 * 在必要时准备线程上下文，并委托给
	 * {@link #invokeInContext}。
	 */
	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Context ctx = (this.exposeAccessContext ? getJndiTemplate().getContext() : null);
		try {
			return invokeInContext(invocation);
		}
		finally {
			getJndiTemplate().releaseContext(ctx);
		}
	}

	/**
	 * 在当前 EJB Home 上执行给定的调用，
	 * 在相应准备的线程上下文中进行。
	 * 由子类实现的模板方法。
	 * @param invocation AOP 方法调用
	 * @return 调用结果（如果有）
	 * @throws Throwable 如果调用失败
	 */
	@Nullable
	protected abstract Object invokeInContext(MethodInvocation invocation) throws Throwable;


	/**
	 * 在缓存的 EJB Home 对象上调用 {@code create()} 方法。
	 * @return 新的 EJBObject 或 EJBLocalObject
	 * @throws NamingException 如果 JNDI 抛出异常
	 * @throws InvocationTargetException 如果 create 方法抛出异常
	 */
	protected Object create() throws NamingException, InvocationTargetException {
		try {
			Object home = getHome();
			Method createMethodToUse = this.createMethod;
			if (createMethodToUse == null) {
				createMethodToUse = getCreateMethod(home);
			}
			if (createMethodToUse == null) {
				return home;
			}
			// 在 EJB Home 对象上调用 create() 方法。
			return createMethodToUse.invoke(home, (Object[]) null);
		}
		catch (IllegalAccessException ex) {
			throw new EjbAccessException("Could not access EJB home create() method", ex);
		}
	}

}
