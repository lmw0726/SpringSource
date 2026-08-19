/*
 * Copyright 2002-2020 the original author or authors.
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

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

/**
 * 供 AspectJ 事务切面（aspect）使用的抽象超切面（superaspect）。具体的
 * 子切面将使用诸如 Java 5 注解之类的策略来实现 {@code transactionalMethodExecution()}
 * 切点（pointcut）。
 *
 * <p>适用于在 Spring IoC 容器内部或外部使用。请适当设置 "transactionManager"
 * 属性，从而可以使用 Spring 所支持的任何事务（transaction）实现。
 *
 * <p><b>注意：</b> 如果某个方法实现了一个本身带有事务注解的接口，则相关的
 * Spring 事务属性将<i>不会</i>被解析。在对接口进行代理（而不是对类进行代理）
 * 时，此行为将不同于 Spring AOP 的行为。我们建议将事务注解添加到类上，而
 * 不是添加到业务接口上，因为它们是实现细节，而不是契约规范的校验。
 *
 * @author Rod Johnson
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @since 2.0
 */
public abstract aspect AbstractTransactionAspect extends TransactionAspectSupport implements DisposableBean {

	/**
	 * 使用给定的事务元数据检索策略来构造该切面。
	 * @param tas TransactionAttributeSource 实现，用于为每个连接点（joinpoint）
	 * 检索 Spring 事务元数据。如果打算通过 Setter 注入进行配置，请在子类中传入
	 * {@code null}。
	 */
	protected AbstractTransactionAspect(TransactionAttributeSource tas) {
		setTransactionAttributeSource(tas);
	}

	@Override
	public void destroy() {
		// 切面本质上是一个单例（singleton）-> 在销毁时进行清理
		clearTransactionManagerCache();
	}

	@SuppressAjWarnings("adviceDidNotMatch")
	Object around(final Object txObject): transactionalMethodExecution(txObject) {
		MethodSignature methodSignature = (MethodSignature) thisJoinPoint.getSignature();
		// 适配 TransactionAspectSupport 的 invokeWithinTransaction...
		try {
			return invokeWithinTransaction(methodSignature.getMethod(), txObject.getClass(), new InvocationCallback() {
				public Object proceedWithInvocation() throws Throwable {
					return proceed(txObject);
				}
			});
		}
		catch (RuntimeException | Error ex) {
			throw ex;
		}
		catch (Throwable thr) {
			Rethrower.rethrow(thr);
			throw new IllegalStateException("Should never get here", thr);
		}
	}

	/**
	 * 具体的子切面必须实现此切点（pointcut），以识别
	 * 事务方法。对于每个选中的连接点（joinpoint），将使用
	 * Spring 的 TransactionAttributeSource 接口检索 TransactionMetadata。
	 */
	protected abstract pointcut transactionalMethodExecution(Object txObject);


	/**
	 * 丑陋但安全的变通方案（workaround）：我们需要能够传播受检异常（checked exception），
	 * 尽管 AspectJ 的 around 通知（advice）仅支持特别声明的异常。
	 */
	private static class Rethrower {

		public static void rethrow(final Throwable exception) {
			class CheckedExceptionRethrower<T extends Throwable> {
				@SuppressWarnings("unchecked")
				private void rethrow(Throwable exception) throws T {
					throw (T) exception;
				}
			}
			new CheckedExceptionRethrower<RuntimeException>().rethrow(exception);
		}
	}

}
