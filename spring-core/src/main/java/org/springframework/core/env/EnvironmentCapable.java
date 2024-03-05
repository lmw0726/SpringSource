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

package org.springframework.core.env;

/**
 * 接口指示一个包含并公开 {@link Environment} 引用的组件。
 *
 * <p>所有的 Spring 应用程序上下文都是 EnvironmentCapable，并且该接口主要用于在框架方法中执行 {@code instanceof}
 * 检查，这些方法接受可能实际上是 ApplicationContext 实例的 BeanFactory 实例，以便在环境可用时与环境进行交互。
 *
 * <p>如前所述，{@link org.springframework.context.ApplicationContext ApplicationContext} 扩展了 EnvironmentCapable，
 * 因此公开了一个 {@link #getEnvironment()} 方法；但是，{@link org.springframework.context.ConfigurableApplicationContext ConfigurableApplicationContext}
 * 重新定义了 {@link org.springframework.context.ConfigurableApplicationContext#getEnvironment getEnvironment()}，
 * 并缩小了签名以返回一个 {@link ConfigurableEnvironment}。效果是，直到从 ConfigurableApplicationContext 访问它时，
 * Environment 对象都是 '只读' 的，此时它也可以被配置。
 *
 * @author Chris Beams
 * @since 3.1
 * @see Environment
 * @see ConfigurableEnvironment
 * @see org.springframework.context.ConfigurableApplicationContext#getEnvironment()
 */
public interface EnvironmentCapable {

	/**返回与此组件关联的 {@link Environment}。
	 */
	Environment getEnvironment();

}
