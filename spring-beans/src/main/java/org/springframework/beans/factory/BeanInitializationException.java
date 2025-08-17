/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.beans.factory;

import org.springframework.beans.FatalBeanException;

/**
 * 建议bean实现在其自身的工厂感知初始化代码失败时抛出的异常。
 * 由bean工厂方法本身抛出的BeansException应该简单地按原样传播。
 *
 * <p>注意{@code afterPropertiesSet()}或自定义的"init-method"
 * 可以抛出任何异常。
 *
 * @author Juergen Hoeller
 * @since 13.11.2003
 * @see BeanFactoryAware#setBeanFactory
 * @see InitializingBean#afterPropertiesSet
 */
@SuppressWarnings("serial")
public class BeanInitializationException extends FatalBeanException {

	/**
	 * 使用指定消息创建一个新的BeanInitializationException。
	 * @param msg 详细消息
	 */
	public BeanInitializationException(String msg) {
		super(msg);
	}

	/**
	 * 使用指定消息和根本原因创建一个新的BeanInitializationException。
	 * @param msg 详细消息
	 * @param cause 根本原因
	 */
	public BeanInitializationException(String msg, Throwable cause) {
		super(msg, cause);
	}

}
