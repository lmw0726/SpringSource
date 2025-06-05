/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;

import java.util.concurrent.Executor;

/**
 * {@link ApplicationEventMulticaster} 接口的简单实现。
 *
 * <p>将所有事件广播给所有注册的监听器，由监听器自行忽略不感兴趣的事件。
 * 监听器通常会对传入的事件对象执行相应的 {@code instanceof} 检查。
 *
 * <p>默认情况下，所有监听器都在调用线程中执行。
 * 这虽然存在单个监听器阻塞整个应用的风险，但开销最小。
 * 也可以指定其他任务执行器，让监听器在不同线程中执行，例如线程池中的线程。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @see #setTaskExecutor
 */
public class SimpleApplicationEventMulticaster extends AbstractApplicationEventMulticaster {

	@Nullable
	private Executor taskExecutor;

	@Nullable
	private ErrorHandler errorHandler;

	@Nullable
	private volatile Log lazyLogger;


	/**
	 * 创建一个新的 SimpleApplicationEventMulticaster 实例。
	 */
	public SimpleApplicationEventMulticaster() {
	}

	/**
	 * 为给定的 BeanFactory 创建一个新的 SimpleApplicationEventMulticaster 实例。
	 */
	public SimpleApplicationEventMulticaster(BeanFactory beanFactory) {
		setBeanFactory(beanFactory);
	}


	/**
	 * 设置一个自定义执行器（通常是 {@link org.springframework.core.task.TaskExecutor}），
	 * 用于调用每个监听器。
	 * <p>默认相当于 {@link org.springframework.core.task.SyncTaskExecutor}，
	 * 在调用线程中同步执行所有监听器。
	 * <p>可以考虑指定一个异步任务执行器，以避免阻塞调用者直到所有监听器执行完毕。
	 * 但请注意，除非任务执行器明确支持，否则异步执行不会继承调用线程的上下文（类加载器、事务关联等）。
	 * @see org.springframework.core.task.SyncTaskExecutor
	 * @see org.springframework.core.task.SimpleAsyncTaskExecutor
	 */
	public void setTaskExecutor(@Nullable Executor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * 返回当前用于该广播器的任务执行器。
	 */
	@Nullable
	protected Executor getTaskExecutor() {
		return this.taskExecutor;
	}

	/**
	 * 设置当监听器抛出异常时调用的 {@link ErrorHandler}。
	 * <p>默认情况下不设置错误处理器，监听器异常会停止当前事件的广播并向事件发布者传播。
	 * 如果指定了 {@linkplain #setTaskExecutor 任务执行器}，每个监听器的异常会传播到执行器，
	 * 但不一定会停止其他监听器的执行。
	 * <p>建议设置一个 {@link ErrorHandler} 实现来捕获并记录异常（例如
	 * {@link org.springframework.scheduling.support.TaskUtils#LOG_AND_SUPPRESS_ERROR_HANDLER}），
	 * 或者一个在记录异常的同时仍将异常传播的实现
	 * （例如 {@link org.springframework.scheduling.support.TaskUtils#LOG_AND_PROPAGATE_ERROR_HANDLER}）。
	 * @since 4.1
	 */
	public void setErrorHandler(@Nullable ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * 返回当前多播器使用的错误处理器。
	 * @since 4.1
	 */
	@Nullable
	protected ErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	@Override
	public void multicastEvent(ApplicationEvent event) {
		multicastEvent(event, resolveDefaultEventType(event));
	}

	@Override
	public void multicastEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {
		ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));
		Executor executor = getTaskExecutor();
		for (ApplicationListener<?> listener : getApplicationListeners(event, type)) {
			if (executor != null) {
				executor.execute(() -> invokeListener(listener, event));
			}
			else {
				invokeListener(listener, event);
			}
		}
	}

	private ResolvableType resolveDefaultEventType(ApplicationEvent event) {
		return ResolvableType.forInstance(event);
	}

	/**
	 * 使用给定的事件调用指定的监听器。
	 * @param listener 要调用的 ApplicationListener
	 * @param event 当前需要传播的事件
	 * @since 4.1
	 */
	protected void invokeListener(ApplicationListener<?> listener, ApplicationEvent event) {
		ErrorHandler errorHandler = getErrorHandler();
		if (errorHandler != null) {
			try {
				doInvokeListener(listener, event);
			}
			catch (Throwable err) {
				errorHandler.handleError(err);
			}
		}
		else {
			doInvokeListener(listener, event);
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void doInvokeListener(ApplicationListener listener, ApplicationEvent event) {
		try {
			listener.onApplicationEvent(event);
		}
		catch (ClassCastException ex) {
			String msg = ex.getMessage();
			if (msg == null || matchesClassCastMessage(msg, event.getClass()) ||
					(event instanceof PayloadApplicationEvent &&
							matchesClassCastMessage(msg, ((PayloadApplicationEvent) event).getPayload().getClass()))) {
				// 可能是使用 lambda 定义的监听器，无法解析泛型事件类型
				// -> 抑制异常。
				Log loggerToUse = this.lazyLogger;
				if (loggerToUse == null) {
					loggerToUse = LogFactory.getLog(getClass());
					this.lazyLogger = loggerToUse;
				}
				if (loggerToUse.isTraceEnabled()) {
					loggerToUse.trace("Non-matching event type for listener: " + listener, ex);
				}
			}
			else {
				throw ex;
			}
		}
	}

	private boolean matchesClassCastMessage(String classCastMessage, Class<?> eventClass) {
		// 在 Java 8 中，异常信息以类名开头："java.lang.String cannot be cast..."
		if (classCastMessage.startsWith(eventClass.getName())) {
			return true;
		}
		// 在 Java 11 中，异常信息以 "class ..." 开头，即 Class.toString() 的结果
		if (classCastMessage.startsWith(eventClass.toString())) {
			return true;
		}
		// 在 Java 9 中，异常信息中包含模块名，如："java.base/java.lang.String cannot be cast..."
		int moduleSeparatorIndex = classCastMessage.indexOf('/');
		if (moduleSeparatorIndex != -1 && classCastMessage.startsWith(eventClass.getName(), moduleSeparatorIndex + 1)) {
			return true;
		}
		// 假设是无关的类转换异常
		return false;
	}

}
