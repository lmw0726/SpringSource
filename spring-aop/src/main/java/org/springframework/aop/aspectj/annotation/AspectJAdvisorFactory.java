/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import java.lang.reflect.Method;
import java.util.List;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.framework.AopConfigException;
import org.springframework.lang.Nullable;

/**
 * 可以从使用 AspectJ 注解语法注解的类创建 Spring AOP Advisor 的工厂接口。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see AspectMetadata
 * @see org.aspectj.lang.reflect.AjTypeSystem
 */
public interface AspectJAdvisorFactory {

	/**
	 * 确定给定类是否为 AspectJ 的 {@link org.aspectj.lang.reflect.AjTypeSystem} 报告的切面。
	 * <p>如果假设的切面无效（例如扩展了具体切面类），
	 * 则简单地返回 {@code false}。
	 * 对于 Spring AOP 无法处理的某些切面（例如具有不支持的实例化模型的切面），
	 * 将返回 true。如有必要，使用 {@link #validate} 方法处理这些情况。
	 * @param clazz 假设的注解样式 AspectJ 类
	 * @return AspectJ 是否将此类识别为切面类
	 */
	boolean isAspect(Class<?> clazz);

	/**
	 * 给定类是否为有效的 AspectJ 切面类？
	 * @param aspectClass 要验证的假设 AspectJ 注解样式类
	 * @throws AopConfigException 如果该类是无效的切面
	 *（这永远是不合法的）
	 * @throws NotAnAtAspectException 如果该类根本不是切面
	 *（这可能合法也可能不合法，具体取决于上下文）
	 */
	void validate(Class<?> aspectClass) throws AopConfigException;

	/**
	 * 为指定切面实例上的所有注解的 At-AspectJ 方法构建 Spring AOP Advisor。
	 * @param aspectInstanceFactory 切面实例工厂
	 *（不是切面实例本身，以避免急切实例化）
	 * @return 此类的 Advisor 列表
	 */
	List<Advisor> getAdvisors(MetadataAwareAspectInstanceFactory aspectInstanceFactory);

	/**
	 * 为给定的 AspectJ 通知方法构建 Spring AOP Advisor。
	 * @param candidateAdviceMethod 候选通知方法
	 * @param aspectInstanceFactory 切面实例工厂
	 * @param declarationOrder 切面中的声明顺序
	 * @param aspectName 切面的名称
	 * @return 如果该方法不是 AspectJ 通知方法，或者它是将用于其他通知
	 * 但本身不会创建 Spring 通知的切点，则返回 {@code null}
	 */
	@Nullable
	Advisor getAdvisor(Method candidateAdviceMethod, MetadataAwareAspectInstanceFactory aspectInstanceFactory,
			int declarationOrder, String aspectName);

	/**
	 * 为给定的 AspectJ 通知方法构建 Spring AOP Advice。
	 * @param candidateAdviceMethod 候选通知方法
	 * @param expressionPointcut AspectJ 表达式切点
	 * @param aspectInstanceFactory 切面实例工厂
	 * @param declarationOrder 切面中的声明顺序
	 * @param aspectName 切面的名称
	 * @return 如果该方法不是 AspectJ 通知方法，或者它是将用于其他通知
	 * 但本身不会创建 Spring 通知的切点，则返回 {@code null}
	 * @see org.springframework.aop.aspectj.AspectJAroundAdvice
	 * @see org.springframework.aop.aspectj.AspectJMethodBeforeAdvice
	 * @see org.springframework.aop.aspectj.AspectJAfterAdvice
	 * @see org.springframework.aop.aspectj.AspectJAfterReturningAdvice
	 * @see org.springframework.aop.aspectj.AspectJAfterThrowingAdvice
	 */
	@Nullable
	Advice getAdvice(Method candidateAdviceMethod, AspectJExpressionPointcut expressionPointcut,
			MetadataAwareAspectInstanceFactory aspectInstanceFactory, int declarationOrder, String aspectName);

}
