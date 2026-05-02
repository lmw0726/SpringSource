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

package org.springframework.aop.support;

import java.lang.reflect.Method;

import org.springframework.aop.MethodMatcher;

/**
 * 动态方法匹配器的便捷抽象超类，
 * 这类匹配器关心运行时参数。
 *
 * @author Rod Johnson
 */
public abstract class DynamicMethodMatcher implements MethodMatcher {

	@Override
	public final boolean isRuntime() {
		return true;
	}

	/**
	 * 可以重写以添加动态匹配的前置条件。此实现始终返回 true。
	 */
	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return true;
	}

}
