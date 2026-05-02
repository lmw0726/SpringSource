/*
 * Copyright 2002-2007 the original author or authors.
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

import org.aopalliance.intercept.MethodInterceptor;

/**
 * AOP Alliance MethodInterceptor 的子接口，允许拦截器实现附加接口，
 * 并通过使用该拦截器的代理暴露这些接口。这是一个称为
 * <b>引介</b>（introduction）的基础 AOP 概念。
 *
 * <p>引介通常是 <b>mixin</b>，支持构建复合对象，
 * 这些对象可以实现 Java 中多重继承的许多目标。
 *
 * @author Rod Johnson
 * @see DynamicIntroductionAdvice
 */
public interface IntroductionInterceptor extends MethodInterceptor, DynamicIntroductionAdvice {

}
