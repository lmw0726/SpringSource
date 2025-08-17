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

package org.springframework.beans.factory.support;

import java.lang.reflect.Method;

/**
 * 由能够重新实现 IoC 容器管理对象中任意方法的类实现的接口：
 * 即依赖注入的 <b>方法注入（Method Injection）</b> 形式。
 *
 * <p>这些方法可以（但不必）是抽象的；如果是抽象方法，
 * 容器将创建一个具体子类来实例化该对象。
 *
 * @author Rod Johnson
 * @since 1.1
 */
public interface MethodReplacer {

	/**
	 * 重新实现给定的方法。
	 * @param obj 我们正在重新实现该方法的实例
	 * @param method 重新实现的方法
	 * @param args 方法的参数
	 * @return 方法的返回值
	 */
	Object reimplement(Object obj, Method method, Object[] args) throws Throwable;

}
