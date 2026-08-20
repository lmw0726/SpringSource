/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.scheduling.support;

import java.util.concurrent.Future;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;
import org.springframework.util.ReflectionUtils;

/**
 * 用于装饰任务以进行错误处理的实用方法。
 *
 * <p><b>注意：</b>本类仅供 Spring 调度器实现内部使用。将其声明为 public 是为了其他包中的实现类
 * 能够访问它，<i>不</i>适用于通用使用场景。
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class TaskUtils {

	/**
	 * 一种 ErrorHandler 策略，仅记录异常日志而不进行进一步处理。
	 * 它会抑制错误，以确保任务的后续执行不会被阻止。
	 */
	public static final ErrorHandler LOG_AND_SUPPRESS_ERROR_HANDLER = new LoggingErrorHandler();

	/**
	 * 一种 ErrorHandler 策略，以 error 级别记录日志后重新抛出异常。
	 * 注意：这通常会阻止调度任务的后续执行。
	 */
	public static final ErrorHandler LOG_AND_PROPAGATE_ERROR_HANDLER = new PropagatingErrorHandler();


	/**
	 * 为任务添加错误处理装饰。如果提供的 {@link ErrorHandler}
	 * 不为 {@code null}，则使用该处理器；否则，重复执行的任务默认抑制错误，
	 * 而一次性任务默认传播错误（因为可通过返回的 {@link Future} 获取这些异常）。
	 * 两种情况都会记录错误日志。
	 */
	public static DelegatingErrorHandlingRunnable decorateTaskWithErrorHandler(
			Runnable task, @Nullable ErrorHandler errorHandler, boolean isRepeatingTask) {

		if (task instanceof DelegatingErrorHandlingRunnable) {
			return (DelegatingErrorHandlingRunnable) task;
		}
		ErrorHandler eh = (errorHandler != null ? errorHandler : getDefaultErrorHandler(isRepeatingTask));
		return new DelegatingErrorHandlingRunnable(task, eh);
	}

	/**
	 * 根据指示任务是否为重复任务的布尔值，返回默认的 {@link ErrorHandler} 实现。
	 * 重复任务会抑制错误，一次性任务会传播错误。两种情况都会记录错误日志。
	 */
	public static ErrorHandler getDefaultErrorHandler(boolean isRepeatingTask) {
		return (isRepeatingTask ? LOG_AND_SUPPRESS_ERROR_HANDLER : LOG_AND_PROPAGATE_ERROR_HANDLER);
	}


	/**
	 * 一个 {@link ErrorHandler} 实现，以 error 级别记录 Throwable。
	 * 它不执行任何额外的错误处理。当预期行为是抑制错误时，此类非常有用。
	 */
	private static class LoggingErrorHandler implements ErrorHandler {

		private final Log logger = LogFactory.getLog(LoggingErrorHandler.class);

		@Override
		public void handleError(Throwable t) {
			logger.error("Unexpected error occurred in scheduled task", t);
		}
	}


	/**
	 * 一个 {@link ErrorHandler} 实现，以 error 级别记录 Throwable 后将其传播。
	 */
	private static class PropagatingErrorHandler extends LoggingErrorHandler {

		@Override
		public void handleError(Throwable t) {
			super.handleError(t);
			ReflectionUtils.rethrowRuntimeException(t);
		}
	}

}
