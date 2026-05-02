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

package org.aopalliance.intercept;

import org.aopalliance.aop.Advice;

/**
 * 此接口表示一个通用的拦截器。
 *
 * <p>通用拦截器可以拦截基础程序中发生的运行时事件。
 * 这些事件通过连接点（joinpoints）具体化（具象化）。
 * 运行时连接点可以是方法调用、字段访问、异常等等。
 *
 * <p>此接口不直接使用。请使用子接口来拦截特定的事件。
 * 例如，以下类实现了一些特定的拦截器，以实现一个调试器：
 *
 * <pre class=code>
 * class DebuggingInterceptor implements MethodInterceptor,
 *     ConstructorInterceptor {
 *
 *   Object invoke(MethodInvocation i) throws Throwable {
 *     debug(i.getMethod(), i.getThis(), i.getArgs());
 *     return i.proceed();
 *   }
 *
 *   Object construct(ConstructorInvocation i) throws Throwable {
 *     debug(i.getConstructor(), i.getThis(), i.getArgs());
 *     return i.proceed();
 *   }
 *
 *   void debug(AccessibleObject ao, Object this, Object value) {
 *     ...
 *   }
 * }
 * </pre>
 *
 * @author Rod Johnson
 * @see Joinpoint
 */
public interface Interceptor extends Advice {

}
