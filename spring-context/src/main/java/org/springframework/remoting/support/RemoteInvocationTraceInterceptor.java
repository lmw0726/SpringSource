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

package org.springframework.remoting.support;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 用于追踪远程调用的 AOP Alliance MethodInterceptor。
 * 由 RemoteExporter 及其子类自动应用。
 *
 * <p>以 DEBUG 级别记录传入的远程调用以及远程调用处理完成的日志。
 * 如果远程调用的处理结果导致受检异常，该异常将以 INFO 级别记录；
 * 如果导致非受检异常（或错误），该异常将以 WARN 级别记录。
 *
 * <p>异常的日志记录对于在服务器端保存堆栈跟踪信息特别有用，
 * 而不是仅仅将异常传播给客户端（客户端可能记录也可能不记录）。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see RemoteExporter#setRegisterTraceInterceptor
 * @see RemoteExporter#getProxyForService
 */
public class RemoteInvocationTraceInterceptor implements MethodInterceptor {

	protected static final Log logger = LogFactory.getLog(RemoteInvocationTraceInterceptor.class);

	private final String exporterNameClause;


	/**
	 * 创建一个新的 RemoteInvocationTraceInterceptor。
	 */
	public RemoteInvocationTraceInterceptor() {
		this.exporterNameClause = "";
	}

	/**
	 * 创建一个新的 RemoteInvocationTraceInterceptor。
	 * @param exporterName 远程导出器的名称（用作日志消息中的上下文信息）
	 */
	public RemoteInvocationTraceInterceptor(String exporterName) {
		this.exporterNameClause = exporterName + " ";
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		if (logger.isDebugEnabled()) {
			logger.debug("Incoming " + this.exporterNameClause + "remote call: " +
					ClassUtils.getQualifiedMethodName(method));
		}
		try {
			Object retVal = invocation.proceed();
			if (logger.isDebugEnabled()) {
				logger.debug("Finished processing of " + this.exporterNameClause + "remote call: " +
						ClassUtils.getQualifiedMethodName(method));
			}
			return retVal;
		}
		catch (Throwable ex) {
			if (ex instanceof RuntimeException || ex instanceof Error) {
				if (logger.isWarnEnabled()) {
					logger.warn("Processing of " + this.exporterNameClause + "remote call resulted in fatal exception: " +
							ClassUtils.getQualifiedMethodName(method), ex);
				}
			}
			else {
				if (logger.isInfoEnabled()) {
					logger.info("Processing of " + this.exporterNameClause + "remote call resulted in exception: " +
							ClassUtils.getQualifiedMethodName(method), ex);
				}
			}
			throw ex;
		}
	}

}
