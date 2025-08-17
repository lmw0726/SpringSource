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

package org.springframework.beans.factory.parsing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;

/**
 * 简单的{@link ProblemReporter}实现，在遇到错误时表现出快速失败
 * 行为。
 *
 * <p>遇到的第一个错误会导致抛出{@link BeanDefinitionParsingException}。
 *
 * <p>警告会写入此类的
 * {@link #setLogger(org.apache.commons.logging.Log) 日志}中。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 2.0
 */
public class FailFastProblemReporter implements ProblemReporter {

	private Log logger = LogFactory.getLog(getClass());


	/**
	 * 设置用于报告警告的{@link Log 日志记录器}。
	 * <p>如果设置为{@code null}，则将使用默认的{@link Log 日志记录器}，
	 * 其名称设置为实例类的名称。
	 * @param logger 用于报告警告的{@link Log 日志记录器}
	 */
	public void setLogger(@Nullable Log logger) {
		this.logger = (logger != null ? logger : LogFactory.getLog(getClass()));
	}


	/**
	 * 抛出详细描述已发生错误的{@link BeanDefinitionParsingException}。
	 * @param problem 错误的来源
	 */
	@Override
	public void fatal(Problem problem) {
		throw new BeanDefinitionParsingException(problem);
	}

	/**
	 * 抛出详细描述已发生错误的{@link BeanDefinitionParsingException}。
	 * @param problem 错误的来源
	 */
	@Override
	public void error(Problem problem) {
		throw new BeanDefinitionParsingException(problem);
	}

	/**
	 * 将提供的{@link Problem}以{@code WARN}级别写入{@link Log}。
	 * @param problem 警告的来源
	 */
	@Override
	public void warning(Problem problem) {
		logger.warn(problem, problem.getRootCause());
	}

}
