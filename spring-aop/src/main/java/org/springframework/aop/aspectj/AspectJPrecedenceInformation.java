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

package org.springframework.aop.aspectj;

import org.springframework.core.Ordered;

/**
 * 由能够提供按 AspectJ 优先级规则对通知/Advisor 进行排序所需信息的类型实现的接口。
 *
 * @author Adrian Colyer
 * @since 2.0
 * @see org.springframework.aop.aspectj.autoproxy.AspectJPrecedenceComparator
 */
public interface AspectJPrecedenceInformation extends Ordered {

	// 实现说明：
	// 我们需要此接口提供的间接级别，否则
	// AspectJPrecedenceComparator 必须在所有情况下向 Advisor 询问其 Advice
	// 才能对 Advisor 进行排序。这会对 InstantiationModelAwarePointcutAdvisor 造成问题，
	// 因为它需要延迟为具有非单例实例化模型的切面创建其 Advice。

	/**
	 * 返回声明该通知的切面（bean）的名称。
	 */
	String getAspectName();

	/**
	 * 返回通知成员在切面中的声明顺序。
	 */
	int getDeclarationOrder();

	/**
	 * 返回这是否是 before 通知。
	 */
	boolean isBeforeAdvice();

	/**
	 * 返回这是否是 after 通知。
	 */
	boolean isAfterAdvice();

}
