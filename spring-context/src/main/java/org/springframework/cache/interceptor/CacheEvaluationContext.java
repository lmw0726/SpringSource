/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.cache.interceptor;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;

/**
 * 缓存专用的求值上下文，以懒加载的方式将方法参数添加为 SpEL 变量。
 * 这种懒加载特性避免了为发现参数而解析类字节码的不必要开销。
 *
 * <p>同时还定义了一组"不可用变量"（即被访问时应当立即抛出异常的变量）。
 * 这可用于验证条件表达式在尚未具备全部潜在变量时依然不会匹配。
 *
 * <p>为限制对象的创建，这里使用了一个不太优雅的构造函数
 * （而不是专门用于延迟执行的类似 'closure' 的类）。
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 3.1
 */
class CacheEvaluationContext extends MethodBasedEvaluationContext {

	private final Set<String> unavailableVariables = new HashSet<>(1);


	CacheEvaluationContext(Object rootObject, Method method, Object[] arguments,
			ParameterNameDiscoverer parameterNameDiscoverer) {

		super(rootObject, method, arguments, parameterNameDiscoverer);
	}


	/**
	 * 将指定变量名标记为该上下文中不可用的变量。
	 * 任何试图访问该变量的表达式都应导致抛出异常。
	 * <p>这允许对可能引用某个变量的表达式进行校验，即使该变量尚未可用。
	 * 因此，任何试图使用该变量的表达式都应求值失败。
	 */
	public void addUnavailableVariable(String name) {
		this.unavailableVariables.add(name);
	}


	/**
	 * 仅在需要时才加载参数信息。
	 */
	@Override
	@Nullable
	public Object lookupVariable(String name) {
		if (this.unavailableVariables.contains(name)) {
			throw new VariableNotAvailableException(name);
		}
		return super.lookupVariable(name);
	}

}
