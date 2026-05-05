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

package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Advisor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

import java.io.Serializable;

/**
 * 将当前 {@link org.aopalliance.intercept.MethodInvocation}
 * 暴露为线程本地对象的拦截器。我们偶尔需要这样做；例如，
 * 当切点（例如 AspectJ 表达式切点）需要知道完整调用上下文时。
 *
 * <p>除非确实必要，否则不要使用此拦截器。目标对象通常不应了解 Spring AOP，
 * 因为这会产生对 Spring API 的依赖。目标对象应尽可能是普通 POJO。
 *
 * <p>如果使用，此拦截器通常会是拦截器链中的第一个。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public final class ExposeInvocationInterceptor implements MethodInterceptor, PriorityOrdered, Serializable {

	/** 此类的单例实例。 */
	public static final ExposeInvocationInterceptor INSTANCE = new ExposeInvocationInterceptor();

	/**
	 * 此类的单例 advisor。在使用 Spring AOP 时，优先使用它而不是 INSTANCE，
	 * 因为它避免了创建新的 Advisor 来包装该实例的需要。
	 */
	public static final Advisor ADVISOR = new DefaultPointcutAdvisor(INSTANCE) {
		@Override
		public String toString() {
			return ExposeInvocationInterceptor.class.getName() +".ADVISOR";
		}
	};

	private static final ThreadLocal<MethodInvocation> invocation =
			new NamedThreadLocal<>("Current AOP method invocation");


	/**
	 * 返回与当前调用关联的 AOP Alliance MethodInvocation 对象。
	 * @return 与当前调用关联的调用对象
	 * @throws IllegalStateException 如果当前没有正在进行的 AOP 调用，
	 * 或 ExposeInvocationInterceptor 未添加到此拦截器链中
	 */
	public static MethodInvocation currentInvocation() throws IllegalStateException {
		MethodInvocation mi = invocation.get();
		if (mi == null) {
			throw new IllegalStateException(
					"No MethodInvocation found: Check that an AOP invocation is in progress and that the " +
					"ExposeInvocationInterceptor is upfront in the interceptor chain. Specifically, note that " +
					"advices with order HIGHEST_PRECEDENCE will execute before ExposeInvocationInterceptor! " +
					"In addition, ExposeInvocationInterceptor and ExposeInvocationInterceptor.currentInvocation() " +
					"must be invoked from the same thread.");
		}
		return mi;
	}


	/**
	 * 确保只能创建规范实例。
	 */
	private ExposeInvocationInterceptor() {
	}

	@Override
	@Nullable
	public Object invoke(MethodInvocation mi) throws Throwable {
		// 获取当前线程之前保存的 MethodInvocation
		MethodInvocation oldInvocation = invocation.get();
		// 将当前的 MethodInvocation 放入 ThreadLocal 中
		invocation.set(mi);
		try {
			// 处理目标方法，并返回值
			return mi.proceed();
		}
		finally {
			// 恢复之前的 MethodInvocation
			invocation.set(oldInvocation);
		}
	}

	@Override
	public int getOrder() {
		return PriorityOrdered.HIGHEST_PRECEDENCE + 1;
	}

	/**
	 * 支持序列化所需。在反序列化时替换为规范实例，
	 * 保护单例模式。
	 * <p>这是重写 {@code equals} 方法的替代方式。
	 */
	private Object readResolve() {
		return INSTANCE;
	}

}
