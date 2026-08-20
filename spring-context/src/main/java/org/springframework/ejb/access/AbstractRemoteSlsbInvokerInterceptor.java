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

package org.springframework.ejb.access;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.naming.NamingException;

import org.aopalliance.intercept.MethodInvocation;

import org.springframework.lang.Nullable;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteLookupFailureException;

/**
 * 代理远程无状态会话 Bean（Stateless Session Bean）的拦截器基类。
 * 专为 EJB 2.x 设计，但也适用于 EJB 3 会话 Bean。
 *
 * <p>此类拦截器必须是通知链中的最后一个拦截器。
 * 在这种情况下，不存在目标对象。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class AbstractRemoteSlsbInvokerInterceptor extends AbstractSlsbInvokerInterceptor {

	private boolean refreshHomeOnConnectFailure = false;

	private volatile boolean homeAsComponent;



	/**
	 * 设置连接失败时是否刷新 EJB 主对象（home）。
	 * 默认值为 "false"。
	 * <p>可以开启此选项以允许 EJB 服务器热重启。
	 * 如果缓存的 EJB 主对象抛出指示远程连接失败的 RMI 异常，
	 * 则会获取新的主对象并重试调用。
	 * @see java.rmi.ConnectException
	 * @see java.rmi.ConnectIOException
	 * @see java.rmi.NoSuchObjectException
	 */
	public void setRefreshHomeOnConnectFailure(boolean refreshHomeOnConnectFailure) {
		this.refreshHomeOnConnectFailure = refreshHomeOnConnectFailure;
	}

	@Override
	protected boolean isHomeRefreshable() {
		return this.refreshHomeOnConnectFailure;
	}


	/**
	 * 检查是否为 EJB3 风格的主对象，该对象可直接作为 EJB 组件使用。
	 */
	@Override
	protected Method getCreateMethod(Object home) throws EjbAccessException {
		if (this.homeAsComponent) {
			return null;
		}
		if (!(home instanceof EJBHome)) {
			// 一个 EJB3 会话 Bean...
			this.homeAsComponent = true;
			return null;
		}
		return super.getCreateMethod(home);
	}


	/**
	 * 获取 EJB 主对象并委托给 {@code doInvoke}。
	 * <p>如果配置了连接失败时刷新，则在遇到相应的 RMI 异常时
	 * 会调用 {@link #refreshAndRetry}。
	 * @see #getHome
	 * @see #doInvoke
	 * @see #refreshAndRetry
	 */
	@Override
	@Nullable
	public Object invokeInContext(MethodInvocation invocation) throws Throwable {
		try {
			return doInvoke(invocation);
		}
		catch (RemoteConnectFailureException ex) {
			return handleRemoteConnectFailure(invocation, ex);
		}
		catch (RemoteException ex) {
			if (isConnectFailure(ex)) {
				return handleRemoteConnectFailure(invocation, ex);
			}
			else {
				throw ex;
			}
		}
	}

	/**
	 * 判断给定的 RMI 异常是否表示连接失败。
	 * <p>默认实现委托给 RmiClientInterceptorUtils。
	 * @param ex the RMI exception to check
	 * @return whether the exception should be treated as connect failure
	 * @see org.springframework.remoting.rmi.RmiClientInterceptorUtils#isConnectFailure
	 */
	@SuppressWarnings("deprecation")
	protected boolean isConnectFailure(RemoteException ex) {
		return org.springframework.remoting.rmi.RmiClientInterceptorUtils.isConnectFailure(ex);
	}

	@Nullable
	private Object handleRemoteConnectFailure(MethodInvocation invocation, Exception ex) throws Throwable {
		if (this.refreshHomeOnConnectFailure) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not connect to remote EJB [" + getJndiName() + "] - retrying", ex);
			}
			else if (logger.isWarnEnabled()) {
				logger.warn("Could not connect to remote EJB [" + getJndiName() + "] - retrying");
			}
			return refreshAndRetry(invocation);
		}
		else {
			throw ex;
		}
	}

	/**
	 * 刷新 EJB 主对象并重试给定的调用。
	 * 在连接失败时由 invoke 方法调用。
	 * @param invocation the AOP method invocation
	 * @return the invocation result, if any
	 * @throws Throwable in case of invocation failure
	 * @see #invoke
	 */
	@Nullable
	protected Object refreshAndRetry(MethodInvocation invocation) throws Throwable {
		try {
			refreshHome();
		}
		catch (NamingException ex) {
			throw new RemoteLookupFailureException("Failed to locate remote EJB [" + getJndiName() + "]", ex);
		}
		return doInvoke(invocation);
	}


	/**
	 * 在当前 EJB 主对象上执行给定的调用。
	 * 需要由子类实现的模板方法。
	 * @param invocation the AOP method invocation
	 * @return the invocation result, if any
	 * @throws Throwable in case of invocation failure
	 * @see #getHome
	 * @see #newSessionBeanInstance
	 */
	@Nullable
	protected abstract Object doInvoke(MethodInvocation invocation) throws Throwable;


	/**
	 * 返回一个新的无状态会话 Bean 实例。
	 * 由具体的远程 SLSB 调用器子类调用。
	 * <p>可以重写此方法以更改算法。
	 * @throws NamingException if thrown by JNDI
	 * @throws InvocationTargetException if thrown by the create method
	 * @see #create
	 */
	protected Object newSessionBeanInstance() throws NamingException, InvocationTargetException {
		if (logger.isDebugEnabled()) {
			logger.debug("Trying to create reference to remote EJB");
		}
		Object ejbInstance = create();
		if (logger.isDebugEnabled()) {
			logger.debug("Obtained reference to remote EJB: " + ejbInstance);
		}
		return ejbInstance;
	}

	/**
	 * 移除给定的 EJB 实例。
	 * 由具体的远程 SLSB 调用器子类调用。
	 * @param ejb the EJB instance to remove
	 * @see javax.ejb.EJBObject#remove
	 */
	protected void removeSessionBeanInstance(@Nullable EJBObject ejb) {
		if (ejb != null && !this.homeAsComponent) {
			try {
				ejb.remove();
			}
			catch (Throwable ex) {
				logger.warn("Could not invoke 'remove' on remote EJB proxy", ex);
			}
		}
	}

}
