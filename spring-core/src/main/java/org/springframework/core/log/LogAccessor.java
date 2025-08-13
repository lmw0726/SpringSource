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

package org.springframework.core.log;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.function.Supplier;

/**
 * 一个便捷的 Commons Logging 访问器，不仅提供基于 {@code CharSequence} 的日志方法，
 * 还提供基于 {@code Supplier} 的变体，方便在 Java 8 lambda 表达式中使用。
 *
 * @author Juergen Hoeller
 * @since 5.2
 */
public class LogAccessor {

	private final Log log;


	/**
	 * 为给定的 Commons Log 创建一个新的访问器。
	 * @see LogFactory#getLog(Class)
	 * @see LogFactory#getLog(String)
	 */
	public LogAccessor(Log log) {
		this.log = log;
	}

	/**
	 * 为指定的 Commons Log 分类创建一个新的访问器。
	 * @see LogFactory#getLog(Class)
	 */
	public LogAccessor(Class<?> logCategory) {
		this.log = LogFactory.getLog(logCategory);
	}

	/**
	 * 为指定的 Commons Log 分类创建一个新的访问器。
	 * @see LogFactory#getLog(String)
	 */
	public LogAccessor(String logCategory) {
		this.log = LogFactory.getLog(logCategory);
	}


	/**
	 * 返回目标 Commons Log。
	 */
	public final Log getLog() {
		return this.log;
	}

	// 日志级别检查

	/**
	 * 当前是否启用致命错误日志？
	 */
	public boolean isFatalEnabled() {
		return this.log.isFatalEnabled();
	}

	/**
	 * 当前是否启用错误日志？
	 */
	public boolean isErrorEnabled() {
		return this.log.isErrorEnabled();
	}

	/**
	 * 当前是否启用警告日志？
	 */
	public boolean isWarnEnabled() {
		return this.log.isWarnEnabled();
	}

	/**
	 * 当前是否启用信息日志？
	 */
	public boolean isInfoEnabled() {
		return this.log.isInfoEnabled();
	}

	/**
	 * 当前是否启用调试日志？
	 */
	public boolean isDebugEnabled() {
		return this.log.isDebugEnabled();
	}

	/**
	 * 当前是否启用跟踪日志？
	 */
	public boolean isTraceEnabled() {
		return this.log.isTraceEnabled();
	}

	// 纯日志方法

	/**
	 * 记录一条致命错误级别的消息。
	 * @param message 要记录的消息
	 */
	public void fatal(CharSequence message) {
		this.log.fatal(message);
	}

	/**
	 * 记录一条致命错误级别的异常及消息。
	 * @param cause 要记录的异常
	 * @param message 要记录的消息
	 */
	public void fatal(Throwable cause, CharSequence message) {
		this.log.fatal(message, cause);
	}

	/**
	 * 记录一条错误级别的消息。
	 * @param message 要记录的消息
	 */
	public void error(CharSequence message) {
		this.log.error(message);
	}

	/**
	 * 记录一条错误级别的异常及消息。
	 * @param cause 要记录的异常
	 * @param message 要记录的消息
	 */
	public void error(Throwable cause, CharSequence message) {
		this.log.error(message, cause);
	}

	/**
	 * 记录一条警告级别的消息。
	 * @param message 要记录的消息
	 */
	public void warn(CharSequence message) {
		this.log.warn(message);
	}

	/**
	 * 记录一条警告级别的异常及消息。
	 * @param cause 要记录的异常
	 * @param message 要记录的消息
	 */
	public void warn(Throwable cause, CharSequence message) {
		this.log.warn(message, cause);
	}

	/**
	 * 记录一条信息级别的消息。
	 * @param message 要记录的消息
	 */
	public void info(CharSequence message) {
		this.log.info(message);
	}

	/**
	 * 记录一条信息级别的异常及消息。
	 * @param cause 要记录的异常
	 * @param message 要记录的消息
	 */
	public void info(Throwable cause, CharSequence message) {
		this.log.info(message, cause);
	}

	/**
	 * 记录一条调试级别的消息。
	 * @param message 要记录的消息
	 */
	public void debug(CharSequence message) {
		this.log.debug(message);
	}

	/**
	 * 记录一条调试级别的异常及消息。
	 * @param cause 要记录的异常
	 * @param message 要记录的消息
	 */
	public void debug(Throwable cause, CharSequence message) {
		this.log.debug(message, cause);
	}

	/**
	 * 记录一条跟踪级别的消息。
	 * @param message 要记录的消息
	 */
	public void trace(CharSequence message) {
		this.log.trace(message);
	}

	/**
	 * 记录一条跟踪级别的异常及消息。
	 * @param cause 要记录的异常
	 * @param message 要记录的消息
	 */
	public void trace(Throwable cause, CharSequence message) {
		this.log.trace(message, cause);
	}

	// 基于 Supplier 的日志方法

	/**
	 * 记录一条致命错误级别的消息。
	 * @param messageSupplier 懒加载消息供应器
	 */
	public void fatal(Supplier<? extends CharSequence> messageSupplier) {
		if (this.log.isFatalEnabled()) {
			this.log.fatal(LogMessage.of(messageSupplier));
		}
	}

	/**
	 * 记录一条致命错误级别的异常及消息。
	 * @param cause 要记录的异常
	 * @param messageSupplier 懒加载消息供应器
	 */
	public void fatal(Throwable cause, Supplier<? extends CharSequence> messageSupplier) {
		if (this.log.isFatalEnabled()) {
			this.log.fatal(LogMessage.of(messageSupplier), cause);
		}
	}

	/**
	 * 记录一条错误级别的消息。
	 * @param messageSupplier 懒加载消息供应器
	 */
	public void error(Supplier<? extends CharSequence> messageSupplier) {
		if (this.log.isErrorEnabled()) {
			this.log.error(LogMessage.of(messageSupplier));
		}
	}

	/**
	 * 记录一条错误级别的异常及消息。
	 * @param cause 要记录的异常
	 * @param messageSupplier 懒加载消息供应器
	 */
	public void error(Throwable cause, Supplier<? extends CharSequence> messageSupplier) {
		if (this.log.isErrorEnabled()) {
			this.log.error(LogMessage.of(messageSupplier), cause);
		}
	}

	/**
	 * 记录一条警告级别的消息。
	 * @param messageSupplier 懒加载消息供应器
	 */
	public void warn(Supplier<? extends CharSequence> messageSupplier) {
		if (this.log.isWarnEnabled()) {
			this.log.warn(LogMessage.of(messageSupplier));
		}
	}

	/**
	 * 记录一条警告级别的异常及消息。
	 * @param cause 要记录的异常
	 * @param messageSupplier 懒加载消息供应器
	 */
	public void warn(Throwable cause, Supplier<? extends CharSequence> messageSupplier) {
		if (this.log.isWarnEnabled()) {
			this.log.warn(LogMessage.of(messageSupplier), cause);
		}
	}

	/**
	 * 记录一条信息级别的消息。
	 * @param messageSupplier 懒加载消息供应器
	 */
	public void info(Supplier<? extends CharSequence> messageSupplier) {
		if (this.log.isInfoEnabled()) {
			this.log.info(LogMessage.of(messageSupplier));
		}
	}

	/**
	 * 记录一条信息级别的异常及消息。
	 * @param cause 要记录的异常
	 * @param messageSupplier 懒加载消息供应器
	 */
	public void info(Throwable cause, Supplier<? extends CharSequence> messageSupplier) {
		if (this.log.isInfoEnabled()) {
			this.log.info(LogMessage.of(messageSupplier), cause);
		}
	}

	/**
	 * 记录一条调试级别的消息。
	 * @param messageSupplier 懒加载消息供应器
	 */
	public void debug(Supplier<? extends CharSequence> messageSupplier) {
		if (this.log.isDebugEnabled()) {
			this.log.debug(LogMessage.of(messageSupplier));
		}
	}

	/**
	 * 记录一条调试级别的异常及消息。
	 * @param cause 要记录的异常
	 * @param messageSupplier 懒加载消息供应器
	 */
	public void debug(Throwable cause, Supplier<? extends CharSequence> messageSupplier) {
		if (this.log.isDebugEnabled()) {
			this.log.debug(LogMessage.of(messageSupplier), cause);
		}
	}

	/**
	 * 记录一条跟踪级别的消息。
	 * @param messageSupplier 懒加载消息供应器
	 */
	public void trace(Supplier<? extends CharSequence> messageSupplier) {
		if (this.log.isTraceEnabled()) {
			this.log.trace(LogMessage.of(messageSupplier));
		}
	}

	/**
	 * 记录一条跟踪级别的异常及消息。
	 * @param cause 要记录的异常
	 * @param messageSupplier 懒加载消息供应器
	 */
	public void trace(Throwable cause, Supplier<? extends CharSequence> messageSupplier) {
		if (this.log.isTraceEnabled()) {
			this.log.trace(LogMessage.of(messageSupplier), cause);
		}
	}

}
