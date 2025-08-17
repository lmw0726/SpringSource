/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.lang.Nullable;

/**
 * 通过将值作为表达式进行求值来解析值的策略接口（如适用）。
 *
 * <p>普通的 {@link org.springframework.beans.factory.BeanFactory} 并不包含该策略的默认实现。
 * 但是，{@link org.springframework.context.ApplicationContext} 实现会开箱即用地提供表达式支持。
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public interface BeanExpressionResolver {

	/**
	 * 将给定值计算为表达式 (如果适用); 否则按 “是” 返回值。
	 * @param value 要作为表达式求值的值
	 * @param beanExpressionContext 评估表达式时要使用的bean表达式上下文
	 * @return 解析值 (可能是给定值)
	 * @throws BeansException 如果评估失败
	 */
	@Nullable
	Object evaluate(@Nullable String value, BeanExpressionContext beanExpressionContext) throws BeansException;

}
