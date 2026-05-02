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

package org.aopalliance.intercept;

import javax.annotation.Nonnull;

/**
 * 拦截新对象的构造。
 *
 * <p>用户应实现 {@link #construct(ConstructorInvocation)} 方法来修改原始行为。
 * 例如，以下类实现了一个单例拦截器
 * （只允许被拦截类有一个唯一实例）：
 *
 * <pre class=code>
 * class DebuggingInterceptor implements ConstructorInterceptor {
 *   Object instance=null;
 *
 *   Object construct(ConstructorInvocation i) throws Throwable {
 *     if(instance==null) {
 *       return instance=i.proceed();
 *     } else {
 *       throw new Exception("singleton does not allow multiple instance");
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Rod Johnson
 */
public interface ConstructorInterceptor extends Interceptor  {

	/**
	 * 实现此方法，以便在新对象构造之前和之后执行额外处理。
	 * 规范的实现当然通常会调用 {@link Joinpoint#proceed()}。
	 * @param invocation 构造连接点
	 * @return 新创建的对象，它也是调用 {@link Joinpoint#proceed()} 的结果；
	 * 可能会被拦截器替换
	 * @throws Throwable 如果拦截器或目标对象抛出异常
	 */
	@Nonnull
	Object construct(ConstructorInvocation invocation) throws Throwable;

}
