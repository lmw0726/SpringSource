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

package org.springframework.aop.support;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;

/**
 * 当我们希望强制子类实现 {@link MethodMatcher} 接口，
 * 但子类又希望成为切点时使用的便捷超类。
 *
 * <p>可以设置 {@link #setClassFilter "classFilter"} 属性来自定义
 * {@link ClassFilter} 行为。默认值为 {@link ClassFilter#TRUE}。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class StaticMethodMatcherPointcut extends StaticMethodMatcher implements Pointcut {

	private ClassFilter classFilter = ClassFilter.TRUE;


	/**
	 * 设置此切点要使用的 {@link ClassFilter}。
	 * 默认值为 {@link ClassFilter#TRUE}。
	 */
	public void setClassFilter(ClassFilter classFilter) {
		this.classFilter = classFilter;
	}

	@Override
	public ClassFilter getClassFilter() {
		return this.classFilter;
	}


	@Override
	public final MethodMatcher getMethodMatcher() {
		return this;
	}

}
