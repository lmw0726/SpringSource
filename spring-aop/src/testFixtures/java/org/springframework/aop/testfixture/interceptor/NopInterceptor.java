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

package org.springframework.aop.testfixture.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * 可以在链中引入一个微不足道的拦截器来显示它
 *
 * @author Rod Johnson
 */
public class NopInterceptor implements MethodInterceptor {

	private int count;


	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		increment();
		return invocation.proceed();
	}

	protected void increment() {
		this.count++;
	}

	public int getCount() {
		return this.count;
	}


	@Override
	public boolean equals(Object other) {
		if (!(other instanceof NopInterceptor)) {
			return false;
		}
		if (this == other) {
			return true;
		}
		return this.count == ((NopInterceptor) other).count;
	}

	@Override
	public int hashCode() {
		return NopInterceptor.class.hashCode();
	}

}
