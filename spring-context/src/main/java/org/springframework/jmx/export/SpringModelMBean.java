/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.jmx.export;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.RequiredModelMBean;

/**
 * {@link RequiredModelMBean} 类的扩展，确保在任何调用发生之前，
 * 将 {@link Thread#getContextClassLoader() 线程上下文类加载器}切换为
 * 托管资源的 {@link ClassLoader}。
 *
 * @author Rob Harrop
 * @since 2.0
 * @see RequiredModelMBean
 */
public class SpringModelMBean extends RequiredModelMBean {

	/**
	 * 存储用于调用的 {@link ClassLoader}。默认为当前线程的 {@link ClassLoader}。
	 */
	private ClassLoader managedResourceClassLoader = Thread.currentThread().getContextClassLoader();


	/**
	 * 使用空的 {@link ModelMBeanInfo} 构造一个新的 SpringModelMBean 实例。
	 * @see javax.management.modelmbean.RequiredModelMBean#RequiredModelMBean()
	 */
	public SpringModelMBean() throws MBeanException, RuntimeOperationsException {
		super();
	}

	/**
	 * 使用给定的 {@link ModelMBeanInfo} 构造一个新的 SpringModelMBean 实例。
	 * @see javax.management.modelmbean.RequiredModelMBean#RequiredModelMBean(ModelMBeanInfo)
	 */
	public SpringModelMBean(ModelMBeanInfo mbi) throws MBeanException, RuntimeOperationsException {
		super(mbi);
	}


	/**
	 * 设置要暴露的托管资源并存储其 {@link ClassLoader}。
	 */
	@Override
	public void setManagedResource(Object managedResource, String managedResourceType)
			throws MBeanException, InstanceNotFoundException, InvalidTargetObjectTypeException {

		this.managedResourceClassLoader = managedResource.getClass().getClassLoader();
		super.setManagedResource(managedResource, managedResourceType);
	}


	/**
	 * 在允许调用发生之前，将 {@link Thread#getContextClassLoader() 上下文类加载器}切换为
	 * 托管资源的 {@link ClassLoader}。
	 * @see javax.management.modelmbean.ModelMBean#invoke
	 */
	@Override
	public Object invoke(String opName, Object[] opArgs, String[] sig)
			throws MBeanException, ReflectionException {

		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.managedResourceClassLoader);
			return super.invoke(opName, opArgs, sig);
		}
		finally {
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
	}

	/**
	 * 在允许调用发生之前，将 {@link Thread#getContextClassLoader() 上下文类加载器}切换为
	 * 托管资源的 {@link ClassLoader}。
	 * @see javax.management.modelmbean.ModelMBean#getAttribute
	 */
	@Override
	public Object getAttribute(String attrName)
			throws AttributeNotFoundException, MBeanException, ReflectionException {

		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.managedResourceClassLoader);
			return super.getAttribute(attrName);
		}
		finally {
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
	}

	/**
	 * 在允许调用发生之前，将 {@link Thread#getContextClassLoader() 上下文类加载器}切换为
	 * 托管资源的 {@link ClassLoader}。
	 * @see javax.management.modelmbean.ModelMBean#getAttributes
	 */
	@Override
	public AttributeList getAttributes(String[] attrNames) {
		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.managedResourceClassLoader);
			return super.getAttributes(attrNames);
		}
		finally {
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
	}

	/**
	 * 在允许调用发生之前，将 {@link Thread#getContextClassLoader() 上下文类加载器}切换为
	 * 托管资源的 {@link ClassLoader}。
	 * @see javax.management.modelmbean.ModelMBean#setAttribute
	 */
	@Override
	public void setAttribute(Attribute attribute)
			throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {

		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.managedResourceClassLoader);
			super.setAttribute(attribute);
		}
		finally {
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
	}

	/**
	 * 在允许调用发生之前，将 {@link Thread#getContextClassLoader() 上下文类加载器}切换为
	 * 托管资源的 {@link ClassLoader}。
	 * @see javax.management.modelmbean.ModelMBean#setAttributes
	 */
	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.managedResourceClassLoader);
			return super.setAttributes(attributes);
		}
		finally {
			Thread.currentThread().setContextClassLoader(currentClassLoader);
		}
	}

}
