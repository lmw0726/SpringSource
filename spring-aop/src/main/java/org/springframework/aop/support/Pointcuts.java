/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.aop.MethodMatcher;
import org.springframework.aop.Pointcut;
import org.springframework.util.Assert;

/**
 * 用于匹配 getter 和 setter 的切点常量，
 * 以及用于操作和评估切点的静态方法。
 *
 * <p>这些方法在使用并集和交集方法组合切点时特别有用。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class Pointcuts {

	/** 匹配任何类中所有 Bean 属性 setter 的切点。 */
	public static final Pointcut SETTERS = SetterPointcut.INSTANCE;

	/** 匹配任何类中所有 Bean 属性 getter 的切点。 */
	public static final Pointcut GETTERS = GetterPointcut.INSTANCE;


	/**
	 * 匹配给定两个切点中<b>任一</b>切点（或两个切点）匹配的所有方法。
	 * @param pc1 第一个 Pointcut
	 * @param pc2 第二个 Pointcut
	 * @return 一个独立的 Pointcut，匹配给定任一 Pointcut 匹配的所有方法
	 */
	public static Pointcut union(Pointcut pc1, Pointcut pc2) {
		return new ComposablePointcut(pc1).union(pc2);
	}

	/**
	 * 匹配给定两个切点<b>都</b>匹配的所有方法。
	 * @param pc1 第一个 Pointcut
	 * @param pc2 第二个 Pointcut
	 * @return 一个独立的 Pointcut，匹配给定两个 Pointcut 都匹配的所有方法
	 */
	public static Pointcut intersection(Pointcut pc1, Pointcut pc2) {
		return new ComposablePointcut(pc1).intersection(pc2);
	}

	/**
	 * 执行开销最低的切点匹配检查。
	 * @param pointcut 要匹配的切点
	 * @param method 候选方法
	 * @param targetClass 目标类
	 * @param args 方法参数
	 * @return 是否存在运行时匹配
	 */
	public static boolean matches(Pointcut pointcut, Method method, Class<?> targetClass, Object... args) {
		Assert.notNull(pointcut, "Pointcut must not be null");
		if (pointcut == Pointcut.TRUE) {
			return true;
		}
		if (pointcut.getClassFilter().matches(targetClass)) {
			// 仅在通过第一道门槛后才检查。
			MethodMatcher mm = pointcut.getMethodMatcher();
			if (mm.matches(method, targetClass)) {
				// 我们可能需要额外的运行时（参数）检查。
				return (!mm.isRuntime() || mm.matches(method, targetClass, args));
			}
		}
		return false;
	}


	/**
	 * 匹配 Bean 属性 setter 的切点实现。
	 */
	@SuppressWarnings("serial")
	private static class SetterPointcut extends StaticMethodMatcherPointcut implements Serializable {

		public static final SetterPointcut INSTANCE = new SetterPointcut();

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			return (method.getName().startsWith("set") &&
					method.getParameterCount() == 1 &&
					method.getReturnType() == Void.TYPE);
		}

		private Object readResolve() {
			return INSTANCE;
		}

		@Override
		public String toString() {
			return "Pointcuts.SETTERS";
		}
	}


	/**
	 * 匹配 Bean 属性 getter 的切点实现。
	 */
	@SuppressWarnings("serial")
	private static class GetterPointcut extends StaticMethodMatcherPointcut implements Serializable {

		public static final GetterPointcut INSTANCE = new GetterPointcut();

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			return (method.getName().startsWith("get") &&
					method.getParameterCount() == 0);
		}

		private Object readResolve() {
			return INSTANCE;
		}

		@Override
		public String toString() {
			return "Pointcuts.GETTERS";
		}
	}

}
