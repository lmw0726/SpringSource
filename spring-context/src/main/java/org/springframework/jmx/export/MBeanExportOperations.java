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

package org.springframework.jmx.export;

import javax.management.ObjectName;

/**
 * 定义一组 MBean 导出操作的接口，这些操作旨在供应用程序开发人员在应用程序运行时访问。
 *
 * <p>应使用此接口通过 Spring 的管理接口生成功能将应用程序资源导出到 JMX，并可选择性地使用其 {@link ObjectName} 生成功能。
 *
 * @author Rob Harrop
 * @since 2.0
 * @see MBeanExporter
 */
public interface MBeanExportOperations {


	/**
	 * 将提供的资源注册到 JMX。如果资源还不是有效的 MBean，Spring 将为其生成管理接口。生成的接口将取决于实现及其配置。此调用还会为托管资源生成 {@link ObjectName} 并将其返回给调用者。
	 * @param managedResource 要通过 JMX 暴露的资源
	 * @return 资源暴露所使用的 {@link ObjectName}
	 * @throws MBeanExportException 如果 Spring 无法生成 {@link ObjectName} 或注册 MBean
	 */
	ObjectName registerManagedResource(Object managedResource) throws MBeanExportException;

	/**
	 * 将提供的资源注册到 JMX。如果资源还不是有效的 MBean，Spring 将为其生成管理接口。生成的接口将取决于实现及其配置。
	 * @param managedResource 要通过 JMX 暴露的资源
	 * @param objectName 资源暴露所使用的 {@link ObjectName}
	 * @throws MBeanExportException 如果 Spring 无法注册 MBean
	 */
	void registerManagedResource(Object managedResource, ObjectName objectName) throws MBeanExportException;

	/**
	 * 从底层 MBeanServer 注册表中移除指定的 MBean。
	 * @param objectName 要移除的资源的 {@link ObjectName}
	 */
	void unregisterManagedResource(ObjectName objectName);

}
