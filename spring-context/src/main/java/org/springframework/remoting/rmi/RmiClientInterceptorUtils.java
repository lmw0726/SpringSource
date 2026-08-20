/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.remoting.rmi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.StubNotFoundException;
import java.rmi.UnknownHostException;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;
import org.springframework.remoting.RemoteProxyFailureException;
import org.springframework.util.ReflectionUtils;

/**
 * 用于在 RMI 客户端中执行调用的提取方法。
 * 可以处理基于 RMI 存根（stub）的 RMI 和非 RMI 服务接口。
 *
 * <p>注意：这是一个 SPI 类，不打算供应用程序使用。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @deprecated 从 5.3 版本开始（逐步淘汰基于序列化的远程调用）
 */
@Deprecated
public abstract class RmiClientInterceptorUtils {

	private static final Log logger = LogFactory.getLog(RmiClientInterceptorUtils.class);


	/**
	 * 在给定的 RMI 存根上执行原始方法调用，
	 * 直接传递反射异常。
	 * @param invocation AOP 方法调用
	 * @param stub RMI 存根
	 * @return 调用结果（如果有）
	 * @throws InvocationTargetException 如果由反射抛出
	 */
	@Nullable
	public static Object invokeRemoteMethod(MethodInvocation invocation, Object stub)
			throws InvocationTargetException {

		Method method = invocation.getMethod();
		try {
			if (method.getDeclaringClass().isInstance(stub)) {
				// 直接实现
				return method.invoke(stub, invocation.getArguments());
			}
			else {
				// 非直接实现
				Method stubMethod = stub.getClass().getMethod(method.getName(), method.getParameterTypes());
				return stubMethod.invoke(stub, invocation.getArguments());
			}
		}
		catch (InvocationTargetException ex) {
			throw ex;
		}
		catch (NoSuchMethodException ex) {
			throw new RemoteProxyFailureException("No matching RMI stub method found for: " + method, ex);
		}
		catch (Throwable ex) {
			throw new RemoteProxyFailureException("Invocation of RMI stub method failed: " + method, ex);
		}
	}

	/**
	 * 将远程访问期间发生的任意异常包装为
	 * RemoteException 或 Spring 的 RemoteAccessException（如果方法签名
	 * 不支持 RemoteException）。
	 * <p>仅对远程访问异常调用此方法，不要对目标服务本身抛出的异常调用！
	 * @param method 被调用的方法
	 * @param ex 发生的异常，用作 RemoteAccessException 或
	 * RemoteException 的原因
	 * @param message RemoteAccessException 或
	 * RemoteException 的消息
	 * @return 要抛出给调用者的异常
	 */
	public static Exception convertRmiAccessException(Method method, Throwable ex, String message) {
		if (logger.isDebugEnabled()) {
			logger.debug(message, ex);
		}
		if (ReflectionUtils.declaresException(method, RemoteException.class)) {
			return new RemoteException(message, ex);
		}
		else {
			return new RemoteAccessException(message, ex);
		}
	}

	/**
	 * 如果方法签名不支持 RemoteException，则将远程访问期间发生的
	 * RemoteException 转换为 Spring 的 RemoteAccessException。
	 * 否则，返回原始的 RemoteException。
	 * @param method 被调用的方法
	 * @param ex 发生的 RemoteException
	 * @param serviceName 服务名称（用于调试）
	 * @return 要抛出给调用者的异常
	 */
	public static Exception convertRmiAccessException(Method method, RemoteException ex, String serviceName) {
		return convertRmiAccessException(method, ex, isConnectFailure(ex), serviceName);
	}

	/**
	 * 如果方法签名不支持 RemoteException，则将远程访问期间发生的
	 * RemoteException 转换为 Spring 的 RemoteAccessException。
	 * 否则，返回原始的 RemoteException。
	 * @param method 被调用的方法
	 * @param ex 发生的 RemoteException
	 * @param isConnectFailure 给定的异常是否应被视为连接失败
	 * @param serviceName 服务名称（用于调试）
	 * @return 要抛出给调用者的异常
	 */
	public static Exception convertRmiAccessException(
			Method method, RemoteException ex, boolean isConnectFailure, String serviceName) {

		if (logger.isDebugEnabled()) {
			logger.debug("Remote service [" + serviceName + "] threw exception", ex);
		}
		if (ReflectionUtils.declaresException(method, ex.getClass())) {
			return ex;
		}
		else {
			if (isConnectFailure) {
				return new RemoteConnectFailureException("Could not connect to remote service [" + serviceName + "]", ex);
			}
			else {
				return new RemoteAccessException("Could not access remote service [" + serviceName + "]", ex);
			}
		}
	}

	/**
	 * 判断给定的 RMI 异常是否表示连接失败。
	 * <p>将 RMI 的 ConnectException、ConnectIOException、
	 * UnknownHostException、NoSuchObjectException 和
	 * StubNotFoundException 视为连接失败。
	 * @param ex 要检查的 RMI 异常
	 * @return 该异常是否应被视为连接失败
	 * @see java.rmi.ConnectException
	 * @see java.rmi.ConnectIOException
	 * @see java.rmi.UnknownHostException
	 * @see java.rmi.NoSuchObjectException
	 * @see java.rmi.StubNotFoundException
	 */
	public static boolean isConnectFailure(RemoteException ex) {
		return (ex instanceof ConnectException || ex instanceof ConnectIOException ||
				ex instanceof UnknownHostException || ex instanceof NoSuchObjectException ||
				ex instanceof StubNotFoundException || ex.getCause() instanceof SocketException);
	}

}
