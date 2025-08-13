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

package org.springframework.core;

import org.springframework.lang.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 用于发现方法和构造函数参数名的接口。
 *
 * <p>参数名发现并不总是可能，但有多种策略可尝试，
 * 例如查找编译时可能生成的调试信息，以及查找
 * 附带 AspectJ 注解方法的 argname 注解值。
 *
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0
 */
public interface ParameterNameDiscoverer {

	/**
	 * 返回方法的参数名，如果无法确定则返回 {@code null}。
	 * <p>数组中的单个条目可能为 {@code null}，如果只对某些参数可用参数名而对其他不可用。
	 * 但建议尽可能使用占位参数名。
	 * @param method 需要查找参数名的方法
	 * @return 参数名数组（如果能解析到），否则 {@code null}
	 */
	@Nullable
	String[] getParameterNames(Method method);

	/**
	 * 返回构造函数的参数名，如果无法确定则返回 {@code null}。
	 * <p>数组中的单个条目可能为 {@code null}，如果只对某些参数可用参数名而对其他不可用。
	 * 但建议尽可能使用占位参数名。
	 * @param ctor 需要查找参数名的构造函数
	 * @return 参数名数组（如果能解析到），否则 {@code null}
	 */
	@Nullable
	String[] getParameterNames(Constructor<?> ctor);

}
