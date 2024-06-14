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

package org.springframework.http;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.log.LogDelegateFactory;

/**
 * 保存名为 "org.springframework.web.HttpLogging" 的共享日志器，用于在
 * "org.springframework.http" 未启用但 "org.springframework.web" 启用时进行HTTP相关日志记录。
 *
 * <p>这意味着 "org.springframework.web" 启用所有Web日志记录，包括来自
 * 诸如 "org.springframework.http" 和 "spring-core" 中被
 * {@link org.springframework.http.codec.EncoderHttpMessageWriter EncoderHttpMessageWriter}
 * 或 {@link org.springframework.http.codec.DecoderHttpMessageReader DecoderHttpMessageReader}
 * 包装的编解码器模块等较低级别包的日志记录。
 *
 * <p>要查看来自主要类日志记录器的日志，只需启用 "org.springframework.http" 和 "org.springframework.codec" 的日志记录。
 *
 * @author Rossen Stoyanchev
 * @see LogDelegateFactory
 * @since 5.1
 */
public abstract class HttpLogging {

	/**
	 * 回退日志记录器
	 */
	private static final Log fallbackLogger =
			LogFactory.getLog("org.springframework.web." + HttpLogging.class.getSimpleName());


	/**
	 * 为给定类创建一个主日志记录器，并将其包装成一个复合日志记录器，
	 * 该复合日志记录器委托给它或后备日志记录器 "org.springframework.web.HttpLogging"，如果主日志记录器未启用。
	 *
	 * @param primaryLoggerClass 主日志记录器的类
	 * @return 生成的复合日志记录器
	 */
	public static Log forLogName(Class<?> primaryLoggerClass) {
		// 获取主要的日志记录器
		Log primaryLogger = LogFactory.getLog(primaryLoggerClass);
		return forLog(primaryLogger);
	}

	/**
	 * 将给定的主日志记录器包装成一个复合日志记录器，该复合日志记录器委托给它或
	 * 后备日志记录器 "org.springframework.web.HttpLogging"，如果主日志记录器未启用。
	 *
	 * @param primaryLogger 主日志记录器
	 * @return 生成的复合日志记录器
	 */
	public static Log forLog(Log primaryLogger) {
		return LogDelegateFactory.getCompositeLog(primaryLogger, fallbackLogger);
	}

}
