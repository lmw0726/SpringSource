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

package org.springframework.aop.aspectj;

import org.aopalliance.aop.Advice;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.support.ClassFilters;
import org.springframework.aop.support.DelegatePerTargetObjectIntroductionInterceptor;
import org.springframework.aop.support.DelegatingIntroductionInterceptor;

/**
 * 委托给给定对象的引介 advisor。
 * 实现 DeclareParents 注解的 AspectJ 注解风格行为。
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @since 2.0
 */
public class DeclareParentsAdvisor implements IntroductionAdvisor {

	private final Advice advice;

	private final Class<?> introducedInterface;

	private final ClassFilter typePatternClassFilter;


	/**
	 * 为此 DeclareParents 字段创建新的 advisor。
	 * @param interfaceType 定义引介的静态字段
	 * @param typePattern 引介受限于的类型模式
	 * @param defaultImpl 默认实现类
	 */
	public DeclareParentsAdvisor(Class<?> interfaceType, String typePattern, Class<?> defaultImpl) {
		this(interfaceType, typePattern,
				new DelegatePerTargetObjectIntroductionInterceptor(defaultImpl, interfaceType));
	}

	/**
	 * 为此 DeclareParents 字段创建新的 advisor。
	 * @param interfaceType 定义引介的静态字段
	 * @param typePattern 引介受限于的类型模式
	 * @param delegateRef 委托实现对象
	 */
	public DeclareParentsAdvisor(Class<?> interfaceType, String typePattern, Object delegateRef) {
		this(interfaceType, typePattern, new DelegatingIntroductionInterceptor(delegateRef));
	}

	/**
	 * 私有构造函数，用于在基于实现的委托和基于引用的委托之间共享公共代码
	 * （由于使用了 final 字段，不能使用 init() 之类的方法共享公共代码）。
	 * @param interfaceType 定义引介的静态字段
	 * @param typePattern 引介受限于的类型模式
	 * @param interceptor 作为 {@link IntroductionInterceptor} 的委托通知
	 */
	private DeclareParentsAdvisor(Class<?> interfaceType, String typePattern, IntroductionInterceptor interceptor) {
		this.advice = interceptor;
		this.introducedInterface = interfaceType;

		// 排除已实现的方法。
		ClassFilter typePatternFilter = new TypePatternClassFilter(typePattern);
		ClassFilter exclusion = (clazz -> !this.introducedInterface.isAssignableFrom(clazz));
		this.typePatternClassFilter = ClassFilters.intersection(typePatternFilter, exclusion);
	}


	@Override
	public ClassFilter getClassFilter() {
		return this.typePatternClassFilter;
	}

	@Override
	public void validateInterfaces() throws IllegalArgumentException {
		// 不执行任何操作
	}

	@Override
	public boolean isPerInstance() {
		return true;
	}

	@Override
	public Advice getAdvice() {
		return this.advice;
	}

	@Override
	public Class<?>[] getInterfaces() {
		return new Class<?>[] {this.introducedInterface};
	}

}
