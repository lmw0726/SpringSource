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

package org.springframework.ejb.access;

import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBObject;
import javax.naming.NamingException;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.Nullable;
import org.springframework.remoting.RemoteLookupFailureException;

/**
 * 远程无状态会话 Bean（Stateless Session Bean）的基本调用器。
 * 设计用于 EJB 2.x，但也适用于 EJB 3 会话 Bean。
 *
 * <p>为每次调用"创建"一个新的 EJB 实例，或者为所有调用缓存会话 Bean 实例
 * （参见 {@link #setCacheSessionBean}）。
 * 有关如何指定目标 EJB 的 JNDI 位置的信息，
 * 请参见 {@link org.springframework.jndi.JndiObjectLocator}。
 *
 * <p>在 Bean 容器中，此类通常最好作为单例使用。但是，
 * 如果该 Bean 容器预实例化单例（如 XML ApplicationContext 变体），
 * 如果 Bean 容器在 EJB 容器加载目标 EJB 之前加载，则可能会出现问题。
 * 这是因为默认情况下 JNDI 查找将在本类的 init 方法中执行并缓存，
 * 但 EJB 尚未绑定到目标位置。最佳解决方案是将 "lookupHomeOnStartup"
 * 属性设置为 "false"，在这种情况下，将在首次访问 EJB 时获取 home。
 * （此标志默认为 true 仅为向后兼容）。
 *
 * <p>此调用器通常与 RMI 业务接口一起使用，该接口作为 EJB 组件接口的超接口。
 * 或者，此调用器也可以代理具有匹配的非 RMI 业务接口的远程 SLSB，
 * 即反映 EJB 业务方法但不声明 RemoteExceptions 的接口。
 * 在后一种情况下，EJB 桩抛出的 RemoteExceptions 将自动转换为
 * Spring 的非受检异常 RemoteAccessException。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 09.05.2003
 * @see org.springframework.remoting.RemoteAccessException
 * @see AbstractSlsbInvokerInterceptor#setLookupHomeOnStartup
 * @see AbstractSlsbInvokerInterceptor#setCacheHome
 * @see AbstractRemoteSlsbInvokerInterceptor#setRefreshHomeOnConnectFailure
 */
public class SimpleRemoteSlsbInvokerInterceptor extends AbstractRemoteSlsbInvokerInterceptor
		implements DisposableBean {

	private boolean cacheSessionBean = false;

	@Nullable
	private Object beanInstance;

	private final Object beanInstanceMonitor = new Object();


	/**
	 * 设置是否缓存实际的会话 Bean 对象。
	 * <p>默认关闭以符合标准 EJB 规范。对于已知允许缓存实际会话 Bean 对象的服务器，
	 * 打开此标志可优化会话 Bean 访问。
	 * @see #setCacheHome
	 */
	public void setCacheSessionBean(boolean cacheSessionBean) {
		this.cacheSessionBean = cacheSessionBean;
	}


	/**
	 * 此实现为每次调用"创建"一个新的 EJB 实例。
	 * 可为自定义调用策略重写此方法。
	 * <p>或者，重写 {@link #getSessionBeanInstance} 和
	 * {@link #releaseSessionBeanInstance} 以更改 EJB 实例创建，
	 * 例如持有单个共享的 EJB 组件实例。
	 */
	@Override
	@Nullable
	@SuppressWarnings("deprecation")
	protected Object doInvoke(MethodInvocation invocation) throws Throwable {
		Object ejb = null;
		try {
			ejb = getSessionBeanInstance();
			return org.springframework.remoting.rmi.RmiClientInterceptorUtils.invokeRemoteMethod(invocation, ejb);
		}
		catch (NamingException ex) {
			throw new RemoteLookupFailureException("Failed to locate remote EJB [" + getJndiName() + "]", ex);
		}
		catch (InvocationTargetException ex) {
			Throwable targetEx = ex.getTargetException();
			if (targetEx instanceof RemoteException) {
				RemoteException rex = (RemoteException) targetEx;
				throw org.springframework.remoting.rmi.RmiClientInterceptorUtils.convertRmiAccessException(
						invocation.getMethod(), rex, isConnectFailure(rex), getJndiName());
			}
			else if (targetEx instanceof CreateException) {
				throw org.springframework.remoting.rmi.RmiClientInterceptorUtils.convertRmiAccessException(
						invocation.getMethod(), targetEx, "Could not create remote EJB [" + getJndiName() + "]");
			}
			throw targetEx;
		}
		finally {
			if (ejb instanceof EJBObject) {
				releaseSessionBeanInstance((EJBObject) ejb);
			}
		}
	}

	/**
	 * 返回要委托调用的 EJB 组件实例。
	 * <p>默认实现委托给 {@link #newSessionBeanInstance}。
	 * @return EJB 组件实例
	 * @throws NamingException 如果由 JNDI 抛出
	 * @throws InvocationTargetException 如果由 create 方法抛出
	 * @see #newSessionBeanInstance
	 */
	protected Object getSessionBeanInstance() throws NamingException, InvocationTargetException {
		if (this.cacheSessionBean) {
			synchronized (this.beanInstanceMonitor) {
				if (this.beanInstance == null) {
					this.beanInstance = newSessionBeanInstance();
				}
				return this.beanInstance;
			}
		}
		else {
			return newSessionBeanInstance();
		}
	}

	/**
	 * 释放给定的 EJB 实例。
	 * <p>默认实现委托给 {@link #removeSessionBeanInstance}。
	 * @param ejb 要释放的 EJB 组件实例
	 * @see #removeSessionBeanInstance
	 */
	protected void releaseSessionBeanInstance(EJBObject ejb) {
		if (!this.cacheSessionBean) {
			removeSessionBeanInstance(ejb);
		}
	}

	/**
	 * 如有必要，重置缓存的会话 Bean 实例。
	 */
	@Override
	protected void refreshHome() throws NamingException {
		super.refreshHome();
		if (this.cacheSessionBean) {
			synchronized (this.beanInstanceMonitor) {
				this.beanInstance = null;
			}
		}
	}

	/**
	 * 如有必要，移除缓存的会话 Bean 实例。
	 */
	@Override
	public void destroy() {
		if (this.cacheSessionBean) {
			synchronized (this.beanInstanceMonitor) {
				if (this.beanInstance instanceof EJBObject) {
					removeSessionBeanInstance((EJBObject) this.beanInstance);
				}
			}
		}
	}

}
