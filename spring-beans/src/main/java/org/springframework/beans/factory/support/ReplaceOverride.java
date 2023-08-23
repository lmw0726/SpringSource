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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Extension of {@link MethodOverride} that represents an arbitrary
 * override of a method by the IoC container.
 *
 * <p>Any non-final method can be overridden, irrespective of its
 * parameters and return types.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 1.1
 */
public class ReplaceOverride extends MethodOverride {

	/**
	 * 替换方法的bean名称
	 */
	private final String methodReplacerBeanName;

	/**
	 * 参数的类型标识列表
	 */
	private final List<String> typeIdentifiers = new ArrayList<>();


	/**
	 * 构造一个新的ReplaceOverride。
	 *
	 * @param methodName             要覆盖的方法的名称
	 * @param methodReplacerBeanName {@link MethodReplacer} 的bean名称
	 */
	public ReplaceOverride(String methodName, String methodReplacerBeanName) {
		super(methodName);
		Assert.notNull(methodReplacerBeanName, "Method replacer bean name must not be null");
		this.methodReplacerBeanName = methodReplacerBeanName;
	}


	/**
	 * 返回实现MethodReplacer的bean的名称。
	 */
	public String getMethodReplacerBeanName() {
		return this.methodReplacerBeanName;
	}

	/**
	 * 添加类字符串的片段，例如 “Exception” 或 “java.lang.Exc”，以标识参数类型。
	 *
	 * @param identifier 完全限定类名的子字符串
	 */
	public void addTypeIdentifier(String identifier) {
		this.typeIdentifiers.add(identifier);
	}


	@Override
	public boolean matches(Method method) {
		if (!method.getName().equals(getMethodName())) {
			//如果方法名称和重载方法名称不同，返回false
			return false;
		}
		if (!isOverloaded()) {
			//如果不是重载方法，返回true
			// 不是重载函数: 不用担心arg类型匹配...
			return true;
		}
		// 如果我们到了这里，我们需要坚持精确的参数匹配...
		if (this.typeIdentifiers.size() != method.getParameterCount()) {
			//类型标识符列表的大小和方法参数个数不匹配，返回false
			return false;
		}
		//获取方法参数的类型
		Class<?>[] parameterTypes = method.getParameterTypes();
		for (int i = 0; i < this.typeIdentifiers.size(); i++) {
			String identifier = this.typeIdentifiers.get(i);
			if (!parameterTypes[i].getName().contains(identifier)) {
				//如果参数类型名称和同一位置的类型标识符不匹配，返回false。
				return false;
			}
		}
		return true;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (!(other instanceof ReplaceOverride) || !super.equals(other)) {
			return false;
		}
		ReplaceOverride that = (ReplaceOverride) other;
		return (ObjectUtils.nullSafeEquals(this.methodReplacerBeanName, that.methodReplacerBeanName) &&
				ObjectUtils.nullSafeEquals(this.typeIdentifiers, that.typeIdentifiers));
	}

	@Override
	public int hashCode() {
		int hashCode = super.hashCode();
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.methodReplacerBeanName);
		hashCode = 29 * hashCode + ObjectUtils.nullSafeHashCode(this.typeIdentifiers);
		return hashCode;
	}

	@Override
	public String toString() {
		return "Replace override for method '" + getMethodName() + "'";
	}

}
