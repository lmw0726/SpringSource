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

import java.lang.reflect.Constructor;

import javax.annotation.Nonnull;

/**
 * 对构造方法调用的描述，在构造方法调用时
 * 提供给拦截器。
 *
 * <p>构造方法调用是一个连接点，可以被构造方法拦截器拦截。
 *
 * @author Rod Johnson
 * @see ConstructorInterceptor
 */
public interface ConstructorInvocation extends Invocation {

	/**
	 * 获取正在调用的构造方法。
	 * <p>此方法是 {@link Joinpoint#getStaticPart()} 方法
	 * 的友好实现（结果相同）。
	 * @return 正在调用的构造方法
	 */
	@Nonnull
	Constructor<?> getConstructor();

}
