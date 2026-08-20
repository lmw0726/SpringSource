/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.jmx.export.naming;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * 允许基础设施组件向 {@code MBeanExporter} 提供其自身的
 * {@code ObjectName} 的接口。
 *
 * <p><b>注意：</b> 此接口主要用于内部使用。
 *
 * @author Rob Harrop
 * @since 1.2.2
 * @see org.springframework.jmx.export.MBeanExporter
 */
public interface SelfNaming {

	/**
	 * 返回实现对象的 {@code ObjectName}。
	 * @throws MalformedObjectNameException 如果 ObjectName 构造函数抛出此异常
	 * @see javax.management.ObjectName#ObjectName(String)
	 * @see javax.management.ObjectName#getInstance(String)
	 * @see org.springframework.jmx.support.ObjectNameManager#getInstance(String)
	 */
	ObjectName getObjectName() throws MalformedObjectNameException;

}
