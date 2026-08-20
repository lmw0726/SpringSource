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
 * {@code AbstractReflectiveMBeanInfoAssembler} 的子类，允许指定要公开为 MBean 操作和属性的方法名。
 * JavaBean 的 getter 和 setter 方法将自动公开为 JMX 属性。
 *
 * <p>您可以通过 {@code managedMethods} 属性提供方法名数组。如果您有多个 Bean，并且希望每个 Bean 使用不同的
 * 方法名集合，则可以使用 {@code methodMappings} 属性将 Bean 键（即传递给 {@code MBeanExporter} 的名称）
 * 映射到方法名列表。
 *
 * <p>如果同时指定了 {@code methodMappings} 和 {@code managedMethods} 的值，Spring 将首先尝试在
 * 映射中查找方法名。如果未找到该 Bean 的方法名，则将使用 {@code managedMethods} 定义的方法名。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setManagedMethods
 * @see #setMethodMappings
 * @see InterfaceBasedMBeanInfoAssembler
 * @see SimpleReflectiveMBeanInfoAssembler
 * @see MethodExclusionMBeanInfoAssembler
 * @see org.springframework.jmx.export.MBeanExporter
 */
public class MethodNameBasedMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler {

	/**
	 * 存储用于创建管理接口的方法名集合。
	 */
	@Nullable
	private Set<String> managedMethods;

	/**
	 * 存储 Bean 键到方法名数组的映射关系。
	 */
	@Nullable
	private Map<String, Set<String>> methodMappings;


	/**
	 * 设置用于创建管理信息的方法名数组。
	 * 如果在 {@code methodMappings} 属性中未找到对应 Bean 的条目，
	 * 则将使用这些方法名。
	 * @param methodNames 表示要使用的方法的方法名数组
	 * @see #setMethodMappings
	 */
	public void setManagedMethods(String... methodNames) {
		this.managedMethods = new HashSet<>(Arrays.asList(methodNames));
	}

	/**
	 * 设置 Bean 键到以逗号分隔的方法名列表的映射关系。
	 * 属性键应与 Bean 键匹配，属性值应与方法名列表匹配。在查找 Bean 的方法名时，
	 * Spring 将首先检查这些映射。
	 * @param mappings Bean 键到方法名的映射关系
	 */
	public void setMethodMappings(Properties mappings) {
		this.methodMappings = new HashMap<>();
		for (Enumeration<?> en = mappings.keys(); en.hasMoreElements();) {
			String beanKey = (String) en.nextElement();
			String[] methodNames = StringUtils.commaDelimitedListToStringArray(mappings.getProperty(beanKey));
			this.methodMappings.put(beanKey, new HashSet<>(Arrays.asList(methodNames)));
		}
	}


	@Override
	protected boolean includeReadAttribute(Method method, String beanKey) {
		return isMatch(method, beanKey);
	}

	@Override
	protected boolean includeWriteAttribute(Method method, String beanKey) {
		return isMatch(method, beanKey);
	}

	@Override
	protected boolean includeOperation(Method method, String beanKey) {
		return isMatch(method, beanKey);
	}

	protected boolean isMatch(Method method, String beanKey) {
		if (this.methodMappings != null) {
			Set<String> methodNames = this.methodMappings.get(beanKey);
			if (methodNames != null) {
				return methodNames.contains(method.getName());
			}
		}
		return (this.managedMethods != null && this.managedMethods.contains(method.getName()));
	}

}
