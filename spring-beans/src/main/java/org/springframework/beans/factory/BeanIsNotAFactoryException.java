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

package org.springframework.beans.factory;

/**
 * 当bean不是工厂，但用户试图获取给定bean名称的工厂时抛出的异常。
 * bean是否为工厂由其是否实现FactoryBean接口决定。
 *
 * @author Rod Johnson
 * @since 10.03.2003
 * @see org.springframework.beans.factory.FactoryBean
 */
@SuppressWarnings("serial")
public class BeanIsNotAFactoryException extends BeanNotOfRequiredTypeException {

	/**
	 * 创建一个新的BeanIsNotAFactoryException。
	 * @param name 请求的bean名称
	 * @param actualType 返回的实际类型，与期望类型不匹配
	 */
	public BeanIsNotAFactoryException(String name, Class<?> actualType) {
		super(name, FactoryBean.class, actualType);
	}

}
