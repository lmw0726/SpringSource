/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.scheduling.quartz;

import org.springframework.core.NestedRuntimeException;
import org.springframework.util.MethodInvoker;

/**
 * 包装从目标方法抛出的异常的非受检异常（unchecked exception）。
 * 该异常从通过反射调用任意目标方法的 Job 传播到 Quartz 调度器。
 *
 * @author Juergen Hoeller
 * @since 2.5.3
 * @see MethodInvokingJobDetailFactoryBean
 */
@SuppressWarnings("serial")
public class JobMethodInvocationFailedException extends NestedRuntimeException {

	/**
	 * JobMethodInvocationFailedException 的构造方法。
	 * @param methodInvoker 用于反射调用的 MethodInvoker
	 * @param cause 根本原因（即从目标方法抛出的异常）
	 */
	public JobMethodInvocationFailedException(MethodInvoker methodInvoker, Throwable cause) {
		super("Invocation of method '" + methodInvoker.getTargetMethod() +
				"' on target class [" + methodInvoker.getTargetClass() + "] failed", cause);
	}

}
