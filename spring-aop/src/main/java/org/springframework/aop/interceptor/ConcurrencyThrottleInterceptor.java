/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.aop.interceptor;

import java.io.Serializable;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrencyThrottleSupport;

/**
 * 对并发访问进行限流的拦截器，
 * 在达到指定并发限制时阻塞调用。
 *
 * <p>可应用于大量使用系统资源的本地服务方法，
 * 在这种场景下，对特定服务进行并发限流比限制整个线程池
 * （例如 Web 容器的线程池）更高效。
 *
 * <p>此拦截器的默认并发限制为 1。
 * 指定 "concurrencyLimit" bean 属性可更改此值。
 *
 * @author Juergen Hoeller
 * @since 11.02.2004
 * @see #setConcurrencyLimit
 */
@SuppressWarnings("serial")
public class ConcurrencyThrottleInterceptor extends ConcurrencyThrottleSupport
		implements MethodInterceptor, Serializable {

	public ConcurrencyThrottleInterceptor() {
		setConcurrencyLimit(1);
	}

	@Override
	@Nullable
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		beforeAccess();
		try {
			return methodInvocation.proceed();
		}
		finally {
			afterAccess();
		}
	}

}
