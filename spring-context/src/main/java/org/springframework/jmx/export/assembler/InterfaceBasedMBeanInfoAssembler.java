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

package org.springframework.jmx.export.assembler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@code AbstractReflectiveMBeanInfoAssembler} 的子类，允许使用任意接口
 * 来定义 bean 的管理接口。这些接口中定义的任何方法或属性都将被暴露为
 * MBean 操作和属性。
 *
 * <p>默认情况下，此类根据 bean 类实现的接口来决定是否包含每个操作或属性。
 * 但是，你可以通过 {@code managedInterfaces} 属性提供一个接口数组来替代。
 * 如果你有多个 bean 并且希望每个 bean 使用不同的接口集，那么可以使用
 * {@code interfaceMappings} 属性将 bean 键（即传递给 {@code MBeanExporter}
 * 的名称）映射到接口名称列表。
 *
 * <p>如果你同时指定了 {@code interfaceMappings} 和 {@code managedInterfaces}
 * 的值，Spring 将首先尝试在映射中查找接口。如果找不到该 bean 对应的接口，
 * 则将使用 {@code managedInterfaces} 定义的接口。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 * @see #setManagedInterfaces
 * @see #setInterfaceMappings
 * @see MethodNameBasedMBeanInfoAssembler
 * @see SimpleReflectiveMBeanInfoAssembler
 * @see org.springframework.jmx.export.MBeanExporter
 */
public class InterfaceBasedMBeanInfoAssembler extends AbstractConfigurableMBeanInfoAssembler
		implements BeanClassLoaderAware, InitializingBean {


	@Nullable
	private Class<?>[] managedInterfaces;

	/** bean 键到类数组的映射。 */
	@Nullable
	private Properties interfaceMappings;

	@Nullable
	private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

	/** bean 键到类数组的映射。 */
	@Nullable
	private Map<String, Class<?>[]> resolvedInterfaceMappings;


	/**
	 * 设置用于创建管理信息的接口数组。
	 * 如果在 {@code interfaceMappings} 属性中没有找到对应的 bean 条目，
	 * 则将使用这些接口。
	 * @param managedInterfaces 表示要使用的接口的类数组。
	 * 每个条目<strong>必须</strong>是一个接口。
	 * @see #setInterfaceMappings
	 */
	public void setManagedInterfaces(@Nullable Class<?>... managedInterfaces) {
		if (managedInterfaces != null) {
			for (Class<?> ifc : managedInterfaces) {
				if (!ifc.isInterface()) {
					throw new IllegalArgumentException(
							"Management interface [" + ifc.getName() + "] is not an interface");
				}
			}
		}
		this.managedInterfaces = managedInterfaces;
	}

	/**
	 * 设置 bean 键到逗号分隔的接口名称列表的映射。
	 * <p>属性键应与 bean 键匹配，属性值应与接口名称列表匹配。
	 * 当为 bean 搜索接口时，Spring 将首先检查这些映射。
	 * @param mappings bean 键到接口名称的映射
	 */
	public void setInterfaceMappings(@Nullable Properties mappings) {
		this.interfaceMappings = mappings;
	}

	@Override
	public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.interfaceMappings != null) {
			this.resolvedInterfaceMappings = resolveInterfaceMappings(this.interfaceMappings);
		}
	}

	/**
	 * 解析给定的接口映射，将类名转换为 Class 对象。
	 * @param mappings 指定的接口映射
	 * @return 解析后的接口映射（值为 Class 对象）
	 */
	private Map<String, Class<?>[]> resolveInterfaceMappings(Properties mappings) {
		Map<String, Class<?>[]> resolvedMappings = CollectionUtils.newHashMap(mappings.size());
		for (Enumeration<?> en = mappings.propertyNames(); en.hasMoreElements();) {
			String beanKey = (String) en.nextElement();
			String[] classNames = StringUtils.commaDelimitedListToStringArray(mappings.getProperty(beanKey));
			Class<?>[] classes = resolveClassNames(classNames, beanKey);
			resolvedMappings.put(beanKey, classes);
		}
		return resolvedMappings;
	}

	/**
	 * 将给定的类名解析为 Class 对象。
	 * @param classNames 要解析的类名
	 * @param beanKey 类名关联的 bean 键
	 * @return 解析后的 Class 对象
	 */
	private Class<?>[] resolveClassNames(String[] classNames, String beanKey) {
		Class<?>[] classes = new Class<?>[classNames.length];
		for (int x = 0; x < classes.length; x++) {
			Class<?> cls = ClassUtils.resolveClassName(classNames[x].trim(), this.beanClassLoader);
			if (!cls.isInterface()) {
				throw new IllegalArgumentException(
						"Class [" + classNames[x] + "] mapped to bean key [" + beanKey + "] is no interface");
			}
			classes[x] = cls;
		}
		return classes;
	}


	/**
	 * 检查 {@code Method} 是否在配置的某个接口中声明，
	 * 并且是公共方法。
	 * @param method 访问器 {@code Method}。
	 * @param beanKey 与 {@code beans} {@code Map} 中
	 * MBean 关联的键。
	 * @return 如果 {@code Method} 在配置的某个接口中声明，
	 * 则返回 {@code true}，否则返回 {@code false}。
	 */
	@Override
	protected boolean includeReadAttribute(Method method, String beanKey) {
		return isPublicInInterface(method, beanKey);
	}

	/**
	 * 检查 {@code Method} 是否在配置的某个接口中声明，
	 * 并且是公共方法。
	 * @param method 修改器 {@code Method}。
	 * @param beanKey 与 {@code beans} {@code Map} 中
	 * MBean 关联的键。
	 * @return 如果 {@code Method} 在配置的某个接口中声明，
	 * 则返回 {@code true}，否则返回 {@code false}。
	 */
	@Override
	protected boolean includeWriteAttribute(Method method, String beanKey) {
		return isPublicInInterface(method, beanKey);
	}

	/**
	 * 检查 {@code Method} 是否在配置的某个接口中声明，
	 * 并且是公共方法。
	 * @param method 操作 {@code Method}。
	 * @param beanKey 与 {@code beans} {@code Map} 中
	 * MBean 关联的键。
	 * @return 如果 {@code Method} 在配置的某个接口中声明，
	 * 则返回 {@code true}，否则返回 {@code false}。
	 */
	@Override
	protected boolean includeOperation(Method method, String beanKey) {
		return isPublicInInterface(method, beanKey);
	}

	/**
	 * 检查 {@code Method} 是否既是公共方法，又在
	 * 配置的某个接口中声明。
	 * @param method 要检查的 {@code Method}。
	 * @param beanKey 与 beans 映射中 MBean 关联的键
	 * @return 如果 {@code Method} 在配置的某个接口中声明
	 * 并且是公共方法，则返回 {@code true}，否则返回 {@code false}。
	 */
	private boolean isPublicInInterface(Method method, String beanKey) {
		return Modifier.isPublic(method.getModifiers()) && isDeclaredInInterface(method, beanKey);
	}

	/**
	 * 检查给定的方法是否在给定 bean 的
	 * 管理接口中声明。
	 */
	private boolean isDeclaredInInterface(Method method, String beanKey) {
		Class<?>[] ifaces = null;

		if (this.resolvedInterfaceMappings != null) {
			ifaces = this.resolvedInterfaceMappings.get(beanKey);
		}

		if (ifaces == null) {
			ifaces = this.managedInterfaces;
			if (ifaces == null) {
				ifaces = ClassUtils.getAllInterfacesForClass(method.getDeclaringClass());
			}
		}

		for (Class<?> ifc : ifaces) {
			for (Method ifcMethod : ifc.getMethods()) {
				if (ifcMethod.getName().equals(method.getName()) &&
						ifcMethod.getParameterCount() == method.getParameterCount() &&
						Arrays.equals(ifcMethod.getParameterTypes(), method.getParameterTypes())) {
					return true;
				}
			}
		}

		return false;
	}

}
