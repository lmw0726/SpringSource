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

package org.springframework.aop.interceptor;

import com.jamonapi.MonKey;
import com.jamonapi.MonKeyImp;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import com.jamonapi.utils.Misc;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;

/**
 * 使用 <b>JAMon</b> 库对被拦截方法执行性能测量并输出统计数据的
 * 性能监控拦截器。此外，它跟踪/计算被拦截方法抛出的异常。
 * 堆栈跟踪可以在 JAMon Web 应用程序中查看。
 *
 * <p>此代码灵感来自 Thierry Templier 的博客。
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Steve Souza
 * @since 1.1.3
 * @see com.jamonapi.MonitorFactory
 * @see PerformanceMonitorInterceptor
 */
@SuppressWarnings("serial")
public class JamonPerformanceMonitorInterceptor extends AbstractMonitoringInterceptor {

	private boolean trackAllInvocations = false;


	/**
	 * 使用静态记录器创建新的 JamonPerformanceMonitorInterceptor。
	 */
	public JamonPerformanceMonitorInterceptor() {
	}

	/**
	 * 根据给定标志使用动态或静态记录器创建新的 JamonPerformanceMonitorInterceptor。
	 * @param useDynamicLogger 是否使用动态记录器或静态记录器
	 * @see #setUseDynamicLogger
	 */
	public JamonPerformanceMonitorInterceptor(boolean useDynamicLogger) {
		setUseDynamicLogger(useDynamicLogger);
	}

	/**
	 * 根据给定标志使用动态或静态记录器创建新的 JamonPerformanceMonitorInterceptor。
	 * @param useDynamicLogger 是否使用动态记录器或静态记录器
	 * @param trackAllInvocations 是否跟踪通过此拦截器的所有调用，
	 * 还是仅跟踪启用了跟踪日志记录的调用
	 * @see #setUseDynamicLogger
	 */
	public JamonPerformanceMonitorInterceptor(boolean useDynamicLogger, boolean trackAllInvocations) {
		setUseDynamicLogger(useDynamicLogger);
		setTrackAllInvocations(trackAllInvocations);
	}


	/**
	 * 设置是否跟踪通过此拦截器的所有调用，
	 * 还是仅跟踪启用了跟踪日志记录的调用。
	 * <p>默认为 "false"：仅监控启用了跟踪日志记录的调用。
	 * 指定 "true" 以让 JAMon 跟踪所有调用，
	 * 即使在禁用跟踪日志记录时也收集统计数据。
	 */
	public void setTrackAllInvocations(boolean trackAllInvocations) {
		this.trackAllInvocations = trackAllInvocations;
	}


	/**
	 * 如果已设置 "trackAllInvocations" 标志，则始终应用拦截器；
	 * 否则仅在启用日志时生效。
	 * @see #setTrackAllInvocations
	 * @see #isLogEnabled
	 */
	@Override
	protected boolean isInterceptorEnabled(MethodInvocation invocation, Log logger) {
		return (this.trackAllInvocations || isLogEnabled(logger));
	}

	/**
	 * 使用 JAMon Monitor 包装调用并将当前性能统计数据写入日志（如果启用）。
	 * @see com.jamonapi.MonitorFactory#start
	 * @see com.jamonapi.Monitor#stop
	 */
	@Override
	protected Object invokeUnderTrace(MethodInvocation invocation, Log logger) throws Throwable {
		String name = createInvocationTraceName(invocation);
		MonKey key = new MonKeyImp(name, name, "ms.");

		Monitor monitor = MonitorFactory.start(key);
		try {
			return invocation.proceed();
		}
		catch (Throwable ex) {
			trackException(key, ex);
			throw ex;
		}
		finally {
			monitor.stop();
			if (!this.trackAllInvocations || isLogEnabled(logger)) {
				writeToLog(logger, "JAMon performance statistics for method [" + name + "]:\n" + monitor);
			}
		}
	}

	/**
	 * 计算抛出的异常并将堆栈跟踪放在键的详细信息部分。
	 * 这将允许在 JAMon Web 应用程序中查看堆栈跟踪。
	 */
	protected void trackException(MonKey key, Throwable ex) {
		String stackTrace = "stackTrace=" + Misc.getExceptionTrace(ex);
		key.setDetails(stackTrace);

		// 特定异常计数器。例如：java.lang.RuntimeException
		MonitorFactory.add(new MonKeyImp(ex.getClass().getName(), stackTrace, "Exception"), 1);

		// 通用异常计数器，是所有抛出异常的总计
		MonitorFactory.add(new MonKeyImp(MonitorFactory.EXCEPTIONS_LABEL, stackTrace, "Exception"), 1);
	}

}
