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

package org.springframework.transaction.aspectj;

import javax.transaction.Transactional;

import org.aspectj.lang.annotation.RequiredTypes;

import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;

/**
 * 使用 JTA 1.2 的 {@link javax.transaction.Transactional} 注解的具体 AspectJ 事务切面（aspect）。
 *
 * <p>使用此切面时，<i>必须</i>在实现类（和/或该类中的方法）上标注注解，
 * <i>而不要</i>在类所实现的接口（如果有的话）上标注。AspectJ 遵循 Java 的规则：
 * 接口上的注解<i>不会</i>被继承。
 *
 * <p>类上的 @Transactional 注解指定该类中任何 <b>public</b> 操作执行的默认事务语义。
 *
 * <p>类中方法上的 @Transactional 注解会覆盖类注解（如果存在）给定的默认事务语义。
 * 任何方法都可以标注（无论可见性如何）。直接标注非 public 方法是为此类操作的执行
 * 获得事务划分的唯一方式。
 *
 * @author Stephane Nicoll
 * @since 4.2
 * @see javax.transaction.Transactional
 * @see AnnotationTransactionAspect
 */
@RequiredTypes("javax.transaction.Transactional")
public aspect JtaAnnotationTransactionAspect extends AbstractTransactionAspect {

	public JtaAnnotationTransactionAspect() {
		super(new AnnotationTransactionAttributeSource(false));
	}

	/**
	 * 匹配带有 Transactional 注解的类型（或带有 Transactional 注解的类型的任何子类型）中
	 * 任何 public 方法的执行。
	 */
	private pointcut executionOfAnyPublicMethodInAtTransactionalType() :
		execution(public * ((@Transactional *)+).*(..)) && within(@Transactional *);

	/**
	 * 匹配带有 Transactional 注解的任何方法的执行。
	 */
	private pointcut executionOfTransactionalMethod() :
		execution(@Transactional * *(..));

	/**
	 * 父切面（aspect）中 pointcut（切点）的定义 - 匹配到的连接点
	 * 将应用 Spring 事务管理。
	 */
	protected pointcut transactionalMethodExecution(Object txObject) :
		(executionOfAnyPublicMethodInAtTransactionalType() || executionOfTransactionalMethod() ) && this(txObject);

}
