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

package org.springframework.aop.framework;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.*;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 在给定 {@link Advised} 对象的情况下，
 * 为 Method 计算 advice 链的一种简单但明确的方式。
 * 始终重新构建每个 advice 链；子类可以提供缓存。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0.3
 */
@SuppressWarnings("serial")
public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {

	@Override
	public List<Object> getInterceptorsAndDynamicInterceptionAdvice(
			Advised config, Method method, @Nullable Class<?> targetClass) {

		// 这有点棘手... 我们必须先处理 introductions，
		// 但需要在最终列表中保留顺序。
		// 获取 Advisor 适配器注册器（负责把 Advice → MethodInterceptor）
		AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();
		// 获取当前代理对象中的所有 Advisor（切面 = Pointcut + Advice）
		Advisor[] advisors = config.getAdvisors();
		// 创建拦截器列表，用于存储最终结果
		List<Object> interceptorList = new ArrayList<>(advisors.length);
		// 确定实际类（优先 targetClass，否则用方法声明类）
		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
		// 是否存在 Introduction（引入增强）标记（延迟计算）
		Boolean hasIntroductions = null;
		// 遍历所有 Advisor（核心循环）
		for (Advisor advisor : advisors) {
			// ==================== 第一类：PointcutAdvisor ====================
			if (advisor instanceof PointcutAdvisor) {
				// 有条件地添加它。
				PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
				// 类级别匹配（ClassFilter）
				if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
					// 获取方法匹配器
					MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
					boolean match;
					// ==================== 支持 Introduction 感知 ====================
					if (mm instanceof IntroductionAwareMethodMatcher) {
						// 延迟计算是否有 Introduction Advisor
						if (hasIntroductions == null) {
							hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
						}
						// 方法匹配（带 introduction 信息）
						match = ((IntroductionAwareMethodMatcher) mm).matches(method, actualClass, hasIntroductions);
					}
					else {
						// 普通方法匹配（静态匹配）
						match = mm.matches(method, actualClass);
					}
					// ==================== 方法匹配成功 ====================
					if (match) {
						// 将 Advisor 转换为 MethodInterceptor（关键）
						MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
						// ==================== 动态匹配 ====================
						if (mm.isRuntime()) {
							// 在 getInterceptors() 方法中创建新的对象实例
							// 不是问题，因为我们通常会缓存已创建的链。
							// 需要运行时判断（例如参数判断）
							for (MethodInterceptor interceptor : interceptors) {
								// 包装成 动态匹配拦截器
								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
							}
						}
						else {
							// 静态匹配（直接加入）
							interceptorList.addAll(Arrays.asList(interceptors));
						}
					}
				}
			}
			// ==================== 第二类：IntroductionAdvisor ====================
			else if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				// 类匹配
				if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
					// 转换为拦截器
					Interceptor[] interceptors = registry.getInterceptors(advisor);
					interceptorList.addAll(Arrays.asList(interceptors));
				}
			}
			// ==================== 第三类：普通 Advisor ====================
			else {
				// 不需要匹配，直接转换
				Interceptor[] interceptors = registry.getInterceptors(advisor);
				interceptorList.addAll(Arrays.asList(interceptors));
			}
		}
		// 返回最终拦截器链
		return interceptorList;
	}

	/**
	 * 确定 Advisor 是否包含匹配的 introductions。
	 */
	private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (ia.getClassFilter().matches(actualClass)) {
					return true;
				}
			}
		}
		return false;
	}

}
