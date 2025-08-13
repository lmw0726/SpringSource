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

package org.springframework.core.log;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于 Spring 日志规范的通用 {@link Log} 委托工厂。
 *
 * <p>主要供框架内部与 Apache Commons Logging 配合使用，
 * 通常通过 {@code spring-jcl} 桥接实现，但也兼容其他 Commons Logging 桥接实现。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.1
 * @see org.apache.commons.logging.LogFactory
 */
public final class LogDelegateFactory {

	private LogDelegateFactory() {
	}


	/**
	 * 创建一个复合日志器，根据优先级依次委托到主日志器、次日志器及更多的备用日志器。
	 * <p>此方法可用于底层包向上层包的日志回退，这些包在包结构上不共享合适的父包，
	 * 但逻辑上应该合并日志（例如 web 包与底层 http、codec 包）。通过此复合日志器，
	 * 主日志器先尝试输出，若未启用则回退至备用日志器。
	 * @param primaryLogger 首选日志器
	 * @param secondaryLogger 次选日志器
	 * @param tertiaryLoggers 其他备用日志器（可选）
	 * @return 复合日志器，负责相关类别的日志输出
	 */
	public static Log getCompositeLog(Log primaryLogger, Log secondaryLogger, Log... tertiaryLoggers) {
		List<Log> loggers = new ArrayList<>(2 + tertiaryLoggers.length);
		loggers.add(primaryLogger);
		loggers.add(secondaryLogger);
		Collections.addAll(loggers, tertiaryLoggers);
		return new CompositeLog(loggers);
	}

	/**
	 * 创建一个“隐藏”日志器，其类别名前缀为 "_"，
	 * 使其不会与同包的其他日志类别同时启用。
	 * 适合用于过于详细或可选、不必一直显示的专用输出。
	 * @param clazz 需要创建日志器的类
	 * @return 类别名为 "_" + 类的全限定名的日志器
	 */
	public static Log getHiddenLog(Class<?> clazz) {
		return getHiddenLog(clazz.getName());
	}

	/**
	 * 创建一个“隐藏”日志器，其类别名前缀为 "_"，
	 * 使其不会与同包的其他日志类别同时启用。
	 * 适合用于过于详细或可选、不必一直显示的专用输出。
	 * @param category 日志类别名
	 * @return 类别名为 "_" + category 的日志器
	 * @since 5.3.5
	 */
	public static Log getHiddenLog(String category) {
		return LogFactory.getLog("_" + category);
	}

}
