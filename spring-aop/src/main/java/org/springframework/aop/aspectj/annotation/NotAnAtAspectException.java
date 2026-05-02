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

package org.springframework.aop.aspectj.annotation;

import org.springframework.aop.framework.AopConfigException;

/**
 * 当尝试在不是 AspectJ 注解样式切面的类上执行
 * 通知器生成操作时抛出的 AopConfigException 扩展。
 *
 * @author Rod Johnson
 * @since 2.0
 */
@SuppressWarnings("serial")
public class NotAnAtAspectException extends AopConfigException {

	private final Class<?> nonAspectClass;


	/**
	 * 为给定类创建新的 NotAnAtAspectException。
	 * @param nonAspectClass 有问题的类
	 */
	public NotAnAtAspectException(Class<?> nonAspectClass) {
		super(nonAspectClass.getName() + " is not an @AspectJ aspect");
		this.nonAspectClass = nonAspectClass;
	}

	/**
	 * 返回有问题的类。
	 */
	public Class<?> getNonAspectClass() {
		return this.nonAspectClass;
	}

}
