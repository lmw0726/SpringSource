/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.aop.framework.autoproxy;

import org.springframework.aop.TargetSource;
import org.springframework.lang.Nullable;

/**
 * 实现可以创建特殊目标源，例如池化目标源，
 * 用于特定 bean。例如，它们的选择可能基于目标类上的属性，
 * 例如池化属性。
 *
 * <p>AbstractAutoProxyCreator 可以支持多个 TargetSourceCreator，
 * 它们将按顺序应用。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@FunctionalInterface
public interface TargetSourceCreator {

	/**
	 * 为给定 bean 创建特殊的目标源（如果有）。
	 * @param beanClass 要为其创建 TargetSource 的 bean 的类
	 * @param beanName bean 的名称
	 * @return 特殊的 TargetSource，如果此 TargetSourceCreator 对特定 bean 不感兴趣
	 * 则返回 {@code null}
	 */
	@Nullable
	TargetSource getTargetSource(Class<?> beanClass, String beanName);

}
