/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jmx.export.assembler;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@code AbstractReflectiveMBeanInfoAssembler} 的子类，允许将方法名
 * 显式排除为 MBean 操作和属性。
 *
 * <p>未在管理接口中显式排除的任何方法都将暴露给 JMX。
 * JavaBean 的 getter 和 setter 将自动暴露为 JMX 属性。
 *
 * <p>您可以通过 {@code ignoredMethods} 属性提供一个方法名数组。
 * 如果您有多个 Bean，并且希望每个 Bean 使用不同的方法名集合，
 * 则可以使用 {@code ignoredMethodMappings} 属性将 Bean 键
 * （即传递给 {@code MBeanExporter} 的名称）映射到方法名列表。
 *
 * <p>如果同时指定了 {@code ignoredMethodMappings} 和
 * {@code ignoredMethods} 的值，Spring 将首先尝试在映射中查找方法名。
 * 如果未找到该 Bean 的方法名，则使用 {@code ignoredMethods} 定义的方法名。
 *
 * @author Rob Harrop
 * @author Seth Ladd
 * @since 1.2.5
 * @see #setIgnoredMethods
 * @see #setIgnoredMethodMappings
 * @see InterfaceBasedMBeanInfoAssembler
 * @see SimpleReflectiveMBeanInfoAssembler
 * @see MethodNameBasedMBeanInfoAssembler
 * @see org.springframework.jmx.export.MBeanExporter
 */
public class MethodExclusionMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler {

	@Nullable
	private Set<String> ignoredMethods;

	@Nullable
	private Map<String, Set<String>> ignoredMethodMappings;


	/**
	 * 设置在创建管理信息时需要<b>忽略</b>的方法名数组。
	 * <p>如果在 {@code ignoredMethodMappings} 属性中未找到对应 Bean 的条目，
	 * 则将使用这些方法名。
	 * @see #setIgnoredMethodMappings(java.util.Properties)
	 */
	public void setIgnoredMethods(String... ignoredMethodNames) {
		this.ignoredMethods = new HashSet<>(Arrays.asList(ignoredMethodNames));
	}

	/**
	 * 设置 Bean 键到逗号分隔的方法名列表的映射。
	 * <p>这些方法名在创建管理接口时将被<b>忽略</b>。
	 * <p>属性键必须与 Bean 键匹配，属性值必须与方法名列表匹配。
	 * 在搜索要忽略的 Bean 方法名时，Spring 将首先检查这些映射。
	 */
	public void setIgnoredMethodMappings(Properties mappings) {
		this.ignoredMethodMappings = new HashMap<>();
		for (Enumeration<?> en = mappings.keys(); en.hasMoreElements();) {
			String beanKey = (String) en.nextElement();
			String[] methodNames = StringUtils.commaDelimitedListToStringArray(mappings.getProperty(beanKey));
			this.ignoredMethodMappings.put(beanKey, new HashSet<>(Arrays.asList(methodNames)));
		}
	}


	@Override
	protected boolean includeReadAttribute(Method method, String beanKey) {
		return isNotIgnored(method, beanKey);
	}

	@Override
	protected boolean includeWriteAttribute(Method method, String beanKey) {
		return isNotIgnored(method, beanKey);
	}

	@Override
	protected boolean includeOperation(Method method, String beanKey) {
		return isNotIgnored(method, beanKey);
	}

	/**
	 * 判断给定方法是否应该被包含，即未被配置为忽略。
	 * @param method 操作方法
	 * @param beanKey 与 {@code MBeanExporter} 中 Bean 映射关联的
	 * MBean 的键
	 */
	protected boolean isNotIgnored(Method method, String beanKey) {
		if (this.ignoredMethodMappings != null) {
			Set<String> methodNames = this.ignoredMethodMappings.get(beanKey);
			if (methodNames != null) {
				return !methodNames.contains(method.getName());
			}
		}
		if (this.ignoredMethods != null) {
			return !this.ignoredMethods.contains(method.getName());
		}
		return true;
	}

}
