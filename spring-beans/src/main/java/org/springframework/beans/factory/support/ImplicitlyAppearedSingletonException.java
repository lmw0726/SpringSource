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

package org.springframework.beans.factory.support;

/**
 * 从 {@link ConstructorResolver} 内部抛出的异常，直接传递给发起的 {@link DefaultSingletonBeanRegistry}，
 * 无需包装成 {@code BeanCreationException}。
 *
 * <p>该异常用于标识在工厂 Bean 创建过程中，其目标单例 Bean 实例意外提前出现的情况，
 * 通常发生在循环依赖解析期间，当目标单例已隐式创建但尚未完成注册时。
 *
 * @author Juergen Hoeller
 * @since 5.0
 */
@SuppressWarnings("serial")
class ImplicitlyAppearedSingletonException extends IllegalStateException {

	public ImplicitlyAppearedSingletonException() {
		super("About-to-be-created singleton instance implicitly appeared through the " +
				"creation of the factory bean that its bean definition points to");
	}

}
