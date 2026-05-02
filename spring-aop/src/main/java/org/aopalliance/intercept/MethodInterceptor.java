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

package org.aopalliance.intercept;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 拦截接口上发往目标对象的调用。这些拦截器会嵌套在目标对象“之上”。
 *
 * <p>用户应实现 {@link #invoke(MethodInvocation)} 方法来修改原始行为。
 * 例如，以下类实现了一个跟踪拦截器（跟踪被拦截方法上的所有调用）：
 *
 * <pre class=code>
 * class TracingInterceptor implements MethodInterceptor {
 *   Object invoke(MethodInvocation i) throws Throwable {
 *     System.out.println("method "+i.getMethod()+" is called on "+
 *                        i.getThis()+" with args "+i.getArguments());
 *     Object ret=i.proceed();
 *     System.out.println("method "+i.getMethod()+" returns "+ret);
 *     return ret;
 *   }
 * }
 * </pre>
 *
 * @author Rod Johnson
 */
@FunctionalInterface
public interface MethodInterceptor extends Interceptor {

	/**
	 * 实现此方法，以便在调用之前和之后执行额外处理。
	 * 规范的实现当然通常会调用 {@link Joinpoint#proceed()}。
	 * @param invocation 方法调用连接点
	 * @return 调用 {@link Joinpoint#proceed()} 的结果；
	 * 可能会被拦截器拦截
	 * @throws Throwable 如果拦截器或目标对象抛出异常
	 */
	@Nullable
	Object invoke(@Nonnull MethodInvocation invocation) throws Throwable;

}
