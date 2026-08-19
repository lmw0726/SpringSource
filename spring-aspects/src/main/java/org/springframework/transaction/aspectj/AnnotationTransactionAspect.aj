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

import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;

/**
 * 使用 Spring 的 {@link org.springframework.transaction.annotation.Transactional}
 * 注解的具体 AspectJ 事务切面（aspect）。
 *
 * <p>使用此切面时，你<i>必须</i>在实现类（和/或该类中的方法）上添加注解，
 * <i>而不是</i>在该类所实现的接口（如果有的话）上添加注解。
 * AspectJ 遵循 Java 的规则：接口上的注解是<i>不会</i>被继承的。
 *
 * <p>类上的 @Transactional 注解规定了该类中任何 <b>public</b> 操作的执行的
 * 默认事务语义。
 *
 * <p>类中方法上的 @Transactional 注解会覆盖类注解（如果存在）所给定的默认
 * 事务语义。任何方法都可以添加注解（无论其可见性如何）。直接为非 public
 * 方法添加注解，是让此类操作的执行获得事务划分（transaction demarcation）的
 * 唯一方式。
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Adrian Colyer
 * @since 2.0
 * @see org.springframework.transaction.annotation.Transactional
 */
public aspect AnnotationTransactionAspect extends AbstractTransactionAspect {

	public AnnotationTransactionAspect() {
		super(new AnnotationTransactionAttributeSource(false));
	}

	/**
	 * 匹配带有 Transactional 注解的类型（或带有 Transactional 注解的类型的任何
	 * 子类型）中任何 public 方法的执行。
	 */
	private pointcut executionOfAnyPublicMethodInAtTransactionalType() :
		execution(public * ((@Transactional *)+).*(..)) && within(@Transactional *);

	/**
	 * 匹配带有 Transactional 注解的任何方法的执行。
	 */
	private pointcut executionOfTransactionalMethod() :
		execution(@Transactional * *(..));

	/**
	 * 来自父切面的 pointcut（切点）定义——匹配到的连接点（join point）
	 * 将应用 Spring 的事务管理。
	 */
	protected pointcut transactionalMethodExecution(Object txObject) :
		(executionOfAnyPublicMethodInAtTransactionalType() || executionOfTransactionalMethod() ) && this(txObject);

}
