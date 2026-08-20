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

import javax.management.Descriptor;
import javax.management.JMException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.springframework.aop.support.AopUtils;
import org.springframework.jmx.support.JmxUtils;

/**
 * {@code MBeanInfoAssembler} 接口的抽象实现，封装了 {@code ModelMBeanInfo} 实例的创建，
 * 但将元数据的创建委托给子类。
 *
 * <p>该类提供了从受管 Bean 实例中提取 Class 的两种方式：
 * {@link #getTargetClass} 提取任何类型 AOP 代理背后的目标类，
 * {@link #getClassToExpose} 返回将被搜索注解并暴露给 JMX 运行时的类或接口。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 1.2
 */
public abstract class AbstractMBeanInfoAssembler implements MBeanInfoAssembler {

	/**
	 * 创建 {@code ModelMBeanInfoSupport} 类的实例（所有 JMX 实现都提供该类），
	 * 并通过调用子类来填充元数据。
	 * @param managedBean 将被暴露的 Bean（可能是 AOP 代理）
	 * @param beanKey 与受管 Bean 关联的键
	 * @return 填充好的 ModelMBeanInfo 实例
	 * @throws JMException 发生错误时抛出
	 * @see #getDescription(Object, String)
	 * @see #getAttributeInfo(Object, String)
	 * @see #getConstructorInfo(Object, String)
	 * @see #getOperationInfo(Object, String)
	 * @see #getNotificationInfo(Object, String)
	 * @see #populateMBeanDescriptor(javax.management.Descriptor, Object, String)
	 */
	@Override
	public ModelMBeanInfo getMBeanInfo(Object managedBean, String beanKey) throws JMException {
		checkManagedBean(managedBean);
		ModelMBeanInfo info = new ModelMBeanInfoSupport(
				getClassName(managedBean, beanKey), getDescription(managedBean, beanKey),
				getAttributeInfo(managedBean, beanKey), getConstructorInfo(managedBean, beanKey),
				getOperationInfo(managedBean, beanKey), getNotificationInfo(managedBean, beanKey));
		Descriptor desc = info.getMBeanDescriptor();
		populateMBeanDescriptor(desc, managedBean, beanKey);
		info.setMBeanDescriptor(desc);
		return info;
	}

	/**
	 * 检查给定的 Bean 实例，如果它不适合使用此汇编器暴露，则抛出 IllegalArgumentException。
	 * <p>默认实现为空，接受所有 Bean 实例。
	 * @param managedBean 将被暴露的 Bean（可能是 AOP 代理）
	 * @throws IllegalArgumentException Bean 不适合暴露
	 */
	protected void checkManagedBean(Object managedBean) throws IllegalArgumentException {
	}

	/**
	 * 返回给定 Bean 实例的实际 Bean 类。
	 * 这是暴露给描述式 JMX 属性的类。
	 * <p>默认实现返回 AOP 代理的目标类，否则返回普通 Bean 类。
	 * @param managedBean Bean 实例（可能是 AOP 代理）
	 * @return 要暴露的 Bean 类
	 * @see org.springframework.aop.support.AopUtils#getTargetClass(Object)
	 */
	protected Class<?> getTargetClass(Object managedBean) {
		return AopUtils.getTargetClass(managedBean);
	}

	/**
	 * 返回给定 Bean 要暴露的类或接口。
	 * 这是将被搜索属性和操作的类（例如，检查注解）。
	 * @param managedBean Bean 实例（可能是 AOP 代理）
	 * @return 要暴露的 Bean 类
	 * @see JmxUtils#getClassToExpose(Object)
	 */
	protected Class<?> getClassToExpose(Object managedBean) {
		return JmxUtils.getClassToExpose(managedBean);
	}

	/**
	 * 返回给定 Bean 类要暴露的类或接口。
	 * 这是将被搜索属性和操作的类。
	 * @param beanClass Bean 类（可能是 AOP 代理类）
	 * @return 要暴露的 Bean 类
	 * @see JmxUtils#getClassToExpose(Class)
	 */
	protected Class<?> getClassToExpose(Class<?> beanClass) {
		return JmxUtils.getClassToExpose(beanClass);
	}

	/**
	 * 获取 MBean 资源的类名。
	 * <p>默认实现基于类名为 MBean 返回一个简单的描述。
	 * @param managedBean Bean 实例（可能是 AOP 代理）
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中关联的键
	 * @return MBean 描述
	 * @throws JMException 发生错误时抛出
	 */
	protected String getClassName(Object managedBean, String beanKey) throws JMException {
		return getTargetClass(managedBean).getName();
	}

	/**
	 * 获取 MBean 资源的描述。
	 * <p>默认实现基于类名为 MBean 返回一个简单的描述。
	 * @param managedBean Bean 实例（可能是 AOP 代理）
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中关联的键
	 * @throws JMException 发生错误时抛出
	 */
	protected String getDescription(Object managedBean, String beanKey) throws JMException {
		String targetClassName = getTargetClass(managedBean).getName();
		if (AopUtils.isAopProxy(managedBean)) {
			return "Proxy for " + targetClassName;
		}
		return targetClassName;
	}

	/**
	 * 在 {@code ModelMBeanInfo} 实例构造完成后、传递给 {@code MBeanExporter} 之前调用。
	 * <p>子类可以实现此方法来向 MBean 元数据添加额外的描述符。默认实现为空。
	 * @param descriptor MBean 资源的 {@code Descriptor}
	 * @param managedBean Bean 实例（可能是 AOP 代理）
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中关联的键
	 * @throws JMException 发生错误时抛出
	 */
	protected void populateMBeanDescriptor(Descriptor descriptor, Object managedBean, String beanKey)
			throws JMException {
	}

	/**
	 * 获取 MBean 资源的构造函数元数据。子类应实现此方法，
	 * 返回应在受管资源的管理接口中暴露的所有构造函数的适当元数据。
	 * <p>默认实现返回空的 {@code ModelMBeanConstructorInfo} 数组。
	 * @param managedBean Bean 实例（可能是 AOP 代理）
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中关联的键
	 * @return 构造函数元数据
	 * @throws JMException 发生错误时抛出
	 */
	protected ModelMBeanConstructorInfo[] getConstructorInfo(Object managedBean, String beanKey)
			throws JMException {
		return new ModelMBeanConstructorInfo[0];
	}

	/**
	 * 获取 MBean 资源的通告元数据。子类应实现此方法，
	 * 返回应在受管资源的管理接口中暴露的所有通告的适当元数据。
	 * <p>默认实现返回空的 {@code ModelMBeanNotificationInfo} 数组。
	 * @param managedBean Bean 实例（可能是 AOP 代理）
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中关联的键
	 * @return 通告元数据
	 * @throws JMException 发生错误时抛出
	 */
	protected ModelMBeanNotificationInfo[] getNotificationInfo(Object managedBean, String beanKey)
			throws JMException {
		return new ModelMBeanNotificationInfo[0];
	}


	/**
	 * 获取 MBean 资源的属性元数据。子类应实现此方法，
	 * 返回应在受管资源的管理接口中暴露的所有属性的适当元数据。
	 * @param managedBean Bean 实例（可能是 AOP 代理）
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中关联的键
	 * @return 属性元数据
	 * @throws JMException 发生错误时抛出
	 */
	protected abstract ModelMBeanAttributeInfo[] getAttributeInfo(Object managedBean, String beanKey)
			throws JMException;

	/**
	 * 获取 MBean 资源的操作元数据。子类应实现此方法，
	 * 返回应在受管资源的管理接口中暴露的所有操作的适当元数据。
	 * @param managedBean Bean 实例（可能是 AOP 代理）
	 * @param beanKey MBean 在 {@code MBeanExporter} 的 beans 映射中关联的键
	 * @return 操作元数据
	 * @throws JMException 发生错误时抛出
	 */
	protected abstract ModelMBeanOperationInfo[] getOperationInfo(Object managedBean, String beanKey)
			throws JMException;

}
