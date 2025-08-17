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

import org.springframework.beans.FatalBeanException;

/**
 * 当FactoryBean的{@code getObject()}方法中bean尚未完全初始化时抛出的异常，
 * 例如因为它涉及循环引用。
 *
 * <p>注意：FactoryBean的循环引用不能像普通bean那样通过预先缓存单例实例来解决。
 * 原因是<i>每个</i>FactoryBean都需要在返回创建的bean之前完全初始化，
 * 而只有<i>特定的</i>普通bean需要初始化 - 也就是说，如果协作bean在初始化时
 * 实际调用它们而不是仅仅存储引用。
 *
 * @author Juergen Hoeller
 * @since 30.10.2003
 * @see FactoryBean#getObject()
 */
@SuppressWarnings("serial")
public class FactoryBeanNotInitializedException extends FatalBeanException {

	/**
	 * 使用默认消息创建一个新的FactoryBeanNotInitializedException。
	 */
	public FactoryBeanNotInitializedException() {
		super("FactoryBean is not fully initialized yet");
	}

	/**
	 * 使用给定消息创建一个新的FactoryBeanNotInitializedException。
	 * @param msg 详细消息
	 */
	public FactoryBeanNotInitializedException(String msg) {
		super(msg);
	}

}
