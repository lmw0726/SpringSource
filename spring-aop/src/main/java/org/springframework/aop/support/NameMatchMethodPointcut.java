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

package org.springframework.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.PatternMatchUtils;

/**
 * 用于简单方法名匹配的切点 Bean，可作为正则表达式模式的替代方案。
 *
 * <p>不处理重载方法：具有给定名称的所有方法都将符合条件。
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Rob Harrop
 * @since 11.02.2004
 * @see #isMatch
 */
@SuppressWarnings("serial")
public class NameMatchMethodPointcut extends StaticMethodMatcherPointcut implements Serializable {

	private List<String> mappedNames = new ArrayList<>();


	/**
	 * 当只有一个方法名需要匹配时使用的便捷方法。
	 * 使用此方法或 {@code setMappedNames}，不要同时使用两者。
	 * @see #setMappedNames
	 */
	public void setMappedName(String mappedName) {
		setMappedNames(mappedName);
	}

	/**
	 * 设置定义要匹配方法的方法名。
	 * 匹配结果将是所有这些名称的并集；如果任一名称匹配，
	 * 则切点匹配。
	 */
	public void setMappedNames(String... mappedNames) {
		this.mappedNames = new ArrayList<>(Arrays.asList(mappedNames));
	}

	/**
	 * 在已命名的方法之外，再添加一个符合条件的方法名。
	 * 与 set 方法一样，此方法用于配置代理时，
	 * 即在代理被使用之前调用。
	 * <p><b>注意：</b>代理投入使用后，此方法不起作用，
	 * 因为通知链将被缓存。
	 * @param name 将要匹配的附加方法名称
	 * @return 此切点，以便在一行中多次添加
	 */
	public NameMatchMethodPointcut addMethodName(String name) {
		this.mappedNames.add(name);
		return this;
	}


	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		for (String mappedName : this.mappedNames) {
			if (mappedName.equals(method.getName()) || isMatch(method.getName(), mappedName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 返回给定方法名是否匹配映射名称。
	 * <p>默认实现检查 "xxx*"、"*xxx" 和 "*xxx*" 匹配，
	 * 以及直接相等。可在子类中重写。
	 * @param methodName 类中的方法名
	 * @param mappedName 描述符中的名称
	 * @return 名称是否匹配
	 * @see org.springframework.util.PatternMatchUtils#simpleMatch(String, String)
	 */
	protected boolean isMatch(String methodName, String mappedName) {
		return PatternMatchUtils.simpleMatch(mappedName, methodName);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof NameMatchMethodPointcut &&
				this.mappedNames.equals(((NameMatchMethodPointcut) other).mappedNames)));
	}

	@Override
	public int hashCode() {
		return this.mappedNames.hashCode();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + this.mappedNames;
	}

}
