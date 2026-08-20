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

package org.springframework.context.expression;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * 基于方法的 {@link org.springframework.expression.EvaluationContext}，
 * 为基于方法的调用提供显式支持。
 *
 * <p>使用以下别名暴露实际的方法参数：
 * <ol>
 * <li>pX，其中 X 是参数的索引（p0 表示第一个参数）</li>
 * <li>aX，其中 X 是参数的索引（a1 表示第二个参数）</li>
 * <li>由可配置的 {@link ParameterNameDiscoverer} 发现的参数名称</li>
 * </ol>
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.2
 */
public class MethodBasedEvaluationContext extends StandardEvaluationContext {

	private final Method method;

	private final Object[] arguments;

	private final ParameterNameDiscoverer parameterNameDiscoverer;

	private boolean argumentsLoaded = false;


	public MethodBasedEvaluationContext(Object rootObject, Method method, Object[] arguments,
			ParameterNameDiscoverer parameterNameDiscoverer) {

		super(rootObject);
		this.method = method;
		this.arguments = arguments;
		this.parameterNameDiscoverer = parameterNameDiscoverer;
	}


	@Override
	@Nullable
	public Object lookupVariable(String name) {
		Object variable = super.lookupVariable(name);
		if (variable != null) {
			return variable;
		}
		if (!this.argumentsLoaded) {
			lazyLoadArguments();
			this.argumentsLoaded = true;
			variable = super.lookupVariable(name);
		}
		return variable;
	}

	/**
	 * 仅在需要时加载参数信息。
	 */
	protected void lazyLoadArguments() {
		// 如果没有参数需要加载，则直接返回
		if (ObjectUtils.isEmpty(this.arguments)) {
			return;
		}

		// 暴露索引变量以及参数名称（如果可发现）
		String[] paramNames = this.parameterNameDiscoverer.getParameterNames(this.method);
		int paramCount = (paramNames != null ? paramNames.length : this.method.getParameterCount());
		int argsCount = this.arguments.length;

		for (int i = 0; i < paramCount; i++) {
			Object value = null;
			if (argsCount > paramCount && i == paramCount - 1) {
				// 将剩余参数作为可变参数数组暴露给最后一个参数
				value = Arrays.copyOfRange(this.arguments, i, argsCount);
			}
			else if (argsCount > i) {
				// 找到实际参数 - 否则保持为 null
				value = this.arguments[i];
			}
			setVariable("a" + i, value);
			setVariable("p" + i, value);
			if (paramNames != null && paramNames[i] != null) {
				setVariable(paramNames[i], value);
			}
		}
	}

}
