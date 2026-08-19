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

package org.springframework.beans.factory.aspectj;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.annotation.control.CodeGenerationHint;

/**
 * 抽象基类切面（aspect），无论对象是如何创建的，
 * 都可以对其执行依赖注入（Dependency Injection）。
 *
 * @author Ramnivas Laddad
 * @since 2.5.2
 */
public abstract aspect AbstractDependencyInjectionAspect {

	private pointcut preConstructionCondition() :
			leastSpecificSuperTypeConstruction() && preConstructionConfiguration();

	private pointcut postConstructionCondition() :
			mostSpecificSubTypeConstruction() && !preConstructionConfiguration();

	/**
	 * 选择被标记为需要依赖注入（DI）的最不具体的父类型
	 * （以便在使用构造前注入时，注入只发生一次）。
	 */
	public abstract pointcut leastSpecificSuperTypeConstruction();

	/**
	 * 选择实例初始化时最具体的初始化连接点（join point）
	 * （即最具体的类）。
	 */
	@CodeGenerationHint(ifNameSuffix="6f1")
	public pointcut mostSpecificSubTypeConstruction() :
			if (thisJoinPoint.getSignature().getDeclaringType() == thisJoinPoint.getThis().getClass());

	/**
	 * 是否选择在构造之前需要配置的 bean 中的连接点（join point）？
	 * 默认情况下，使用构造后注入（post-construction injection），
	 * 与 Configurable 注解中的默认值保持一致。
	 */
	public pointcut preConstructionConfiguration() : if (false);

	/**
	 * 选择需要注入依赖的对象的构造连接点（join point）。
	 */
	public abstract pointcut beanConstruction(Object bean);

	/**
	 * 选择需要注入依赖的对象的反序列化连接点（join point）。
	 */
	public abstract pointcut beanDeserialization(Object bean);

	/**
	 * 选择可配置 bean（configurable bean）中的连接点（join point）。
	 */
	public abstract pointcut inConfigurableBean();


	/**
	 * 构造前配置。
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	before(Object bean) :
		beanConstruction(bean) && preConstructionCondition() && inConfigurableBean()  {
		configureBean(bean);
	}

	/**
	 * 构造后配置。
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	after(Object bean) returning :
		beanConstruction(bean) && postConstructionCondition() && inConfigurableBean() {
		configureBean(bean);
	}

	/**
	 * 反序列化后配置。
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	after(Object bean) returning :
		beanDeserialization(bean) && inConfigurableBean() {
		configureBean(bean);
	}


	/**
	 * 配置给定的 bean。
	 */
	public abstract void configureBean(Object bean);

}
