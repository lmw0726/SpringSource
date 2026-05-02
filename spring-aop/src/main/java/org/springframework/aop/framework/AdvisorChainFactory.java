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

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.lang.Nullable;

/**
 * advisor 链的工厂接口。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface AdvisorChainFactory {

	/**
	 * 根据给定的 advisor 链配置，确定
	 * {@link org.aopalliance.intercept.MethodInterceptor} 对象列表。
	 * @param config Advised 对象形式的 AOP 配置
	 * @param method 被代理的方法
	 * @param targetClass 目标类（可以为 {@code null}，表示没有目标对象的代理，
	 * 此时方法的声明类是次优选择）
	 * @return MethodInterceptor 的 List（也可能包含 InterceptorAndDynamicMethodMatcher）
	 */
	List<Object> getInterceptorsAndDynamicInterceptionAdvice(Advised config, Method method, @Nullable Class<?> targetClass);

}
