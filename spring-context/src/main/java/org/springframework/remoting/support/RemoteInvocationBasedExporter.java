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

package org.springframework.remoting.support;

import java.lang.reflect.InvocationTargetException;

/**
 * 基于 {@link RemoteInvocation} 对象反序列化的远程服务导出器的抽象基类。
 *
 * <p>提供 "remoteInvocationExecutor" 属性，默认使用 {@link DefaultRemoteInvocationExecutor} 作为策略。
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see RemoteInvocationExecutor
 * @see DefaultRemoteInvocationExecutor
 */
public abstract class RemoteInvocationBasedExporter extends RemoteExporter {

	private RemoteInvocationExecutor remoteInvocationExecutor = new DefaultRemoteInvocationExecutor();


	/**
	 * 设置用于此导出器的 RemoteInvocationExecutor。
	 * 默认是 DefaultRemoteInvocationExecutor。
	 * <p>自定义的调用执行器可以从调用中提取更多上下文信息，例如用户凭证。
	 */
	public void setRemoteInvocationExecutor(RemoteInvocationExecutor remoteInvocationExecutor) {
		this.remoteInvocationExecutor = remoteInvocationExecutor;
	}

	/**
	 * 返回此导出器使用的 RemoteInvocationExecutor。
	 */
	public RemoteInvocationExecutor getRemoteInvocationExecutor() {
		return this.remoteInvocationExecutor;
	}


	/**
	 * 将给定的远程调用应用到给定的目标对象。
	 * 默认实现委托给 RemoteInvocationExecutor。
	 * <p>可以在子类中重写以实现自定义调用行为，
	 * 可能用于应用来自自定义 RemoteInvocation 子类的额外调用参数。请注意，
	 * 更推荐使用自定义的 RemoteInvocationExecutor，这是一种可重用的策略。
	 * @param invocation 远程调用
	 * @param targetObject 要应用调用的目标对象
	 * @return 调用结果
	 * @throws NoSuchMethodException 如果无法解析方法名
	 * @throws IllegalAccessException 如果无法访问该方法
	 * @throws InvocationTargetException 如果方法调用导致异常
	 * @see RemoteInvocationExecutor#invoke
	 */
	protected Object invoke(RemoteInvocation invocation, Object targetObject)
			throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

		if (logger.isTraceEnabled()) {
			logger.trace("Executing " + invocation);
		}
		try {
			return getRemoteInvocationExecutor().invoke(invocation, targetObject);
		}
		catch (NoSuchMethodException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not find target method for " + invocation, ex);
			}
			throw ex;
		}
		catch (IllegalAccessException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not access target method for " + invocation, ex);
			}
			throw ex;
		}
		catch (InvocationTargetException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Target method failed for " + invocation, ex.getTargetException());
			}
			throw ex;
		}
	}

	/**
	 * 将给定的远程调用应用到给定的目标对象，并将调用结果包装在可序列化的 RemoteInvocationResult 对象中。
	 * 默认实现创建一个普通的 RemoteInvocationResult。
	 * <p>可以在子类中重写以实现自定义调用行为，
	 * 例如返回额外的上下文信息。请注意，这不在 RemoteInvocationExecutor 策略的覆盖范围内！
	 * @param invocation 远程调用
	 * @param targetObject 要应用调用的目标对象
	 * @return 调用结果
	 * @see #invoke
	 */
	protected RemoteInvocationResult invokeAndCreateResult(RemoteInvocation invocation, Object targetObject) {
		try {
			Object value = invoke(invocation, targetObject);
			return new RemoteInvocationResult(value);
		}
		catch (Throwable ex) {
			return new RemoteInvocationResult(ex);
		}
	}

}
