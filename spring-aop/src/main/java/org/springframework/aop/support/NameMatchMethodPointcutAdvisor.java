/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.aop.support;

import org.aopalliance.aop.Advice;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;

/**
 * 持有 Advice 的方法名匹配方法切点便捷类，
 * 从而使其成为 Advisor。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @see NameMatchMethodPointcut
 */
@SuppressWarnings("serial")
public class NameMatchMethodPointcutAdvisor extends AbstractGenericPointcutAdvisor {

	private final NameMatchMethodPointcut pointcut = new NameMatchMethodPointcut();


	public NameMatchMethodPointcutAdvisor() {
	}

	public NameMatchMethodPointcutAdvisor(Advice advice) {
		setAdvice(advice);
	}


	/**
	 * 设置此切点要使用的 {@link ClassFilter}。
	 * 默认值为 {@link ClassFilter#TRUE}。
	 * @see NameMatchMethodPointcut#setClassFilter
	 */
	public void setClassFilter(ClassFilter classFilter) {
		this.pointcut.setClassFilter(classFilter);
	}

	/**
	 * 当只有一个方法名需要匹配时使用的便捷方法。
	 * 使用此方法或 {@code setMappedNames}，不要同时使用两者。
	 * @see #setMappedNames
	 * @see NameMatchMethodPointcut#setMappedName
	 */
	public void setMappedName(String mappedName) {
		this.pointcut.setMappedName(mappedName);
	}

	/**
	 * 设置定义要匹配方法的方法名。
	 * 匹配结果将是所有这些名称的并集；如果任一名称匹配，
	 * 则切点匹配。
	 * @see NameMatchMethodPointcut#setMappedNames
	 */
	public void setMappedNames(String... mappedNames) {
		this.pointcut.setMappedNames(mappedNames);
	}

	/**
	 * 在已命名的方法之外，再添加一个符合条件的方法名。
	 * 与 set 方法一样，此方法用于配置代理时，
	 * 即在代理被使用之前调用。
	 * @param name 将要匹配的附加方法名称
	 * @return 此切点，以便在一行中多次添加
	 * @see NameMatchMethodPointcut#addMethodName
	 */
	public NameMatchMethodPointcut addMethodName(String name) {
		return this.pointcut.addMethodName(name);
	}


	@Override
	public Pointcut getPointcut() {
		return this.pointcut;
	}

}
