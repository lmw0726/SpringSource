/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.aop;

import org.aopalliance.aop.Advice;

/**
 * AOP Alliance Advice 的子接口，允许 Advice 实现附加接口，
 * 并通过使用该拦截器的代理暴露这些接口。这是一个称为
 * <b>引介</b>（introduction）的基础 AOP 概念。
 *
 * <p>引介通常是 <b>mixin</b>，支持构建复合对象，
 * 这些对象可以实现 Java 中多重继承的许多目标。
 *
 * <p>与 {@link IntroductionInfo} 相比，此接口允许通知实现一组
 * 不一定预先已知的接口。因此，可以使用 {@link IntroductionAdvisor}
 * 指定将在被通知对象中暴露哪些接口。
 *
 * @author Rod Johnson
 * @since 1.1.1
 * @see IntroductionInfo
 * @see IntroductionAdvisor
 */
public interface DynamicIntroductionAdvice extends Advice {

	/**
	 * 此引介通知是否实现给定接口？
	 * @param intf 要检查的接口
	 * @return 该通知是否实现指定接口
	 */
	boolean implementsInterface(Class<?> intf);

}
