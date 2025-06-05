/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.context;

/**
 * 回调接口，在 Spring {@link ConfigurableApplicationContext} 被
 * {@linkplain ConfigurableApplicationContext#refresh() 刷新} 之前进行初始化。
 *
 * <p>通常用于需要以编程方式初始化应用上下文的 Web 应用程序中。
 * 例如，可用于注册属性源或激活 {@linkplain ConfigurableApplicationContext#getEnvironment()
 * 上下文环境}中的 profile。参考 {@code ContextLoader} 和 {@code FrameworkServlet}
 * 对于声明 "contextInitializerClasses" 的 context-param 和 init-param 的支持。
 *
 * <p>建议 {@code ApplicationContextInitializer} 的处理器检测是否实现了 Spring 的
 * {@link org.springframework.core.Ordered Ordered} 接口或是否存在
 * {@link org.springframework.core.annotation.Order @Order} 注解，并在调用之前按顺序对其实例排序。
 *
 * @author Chris Beams
 * @since 3.1
 * @param <C> 应用上下文类型
 * @see org.springframework.web.context.ContextLoader#customizeContext
 * @see org.springframework.web.context.ContextLoader#CONTEXT_INITIALIZER_CLASSES_PARAM
 * @see org.springframework.web.servlet.FrameworkServlet#setContextInitializerClasses
 * @see org.springframework.web.servlet.FrameworkServlet#applyInitializers
 */
@FunctionalInterface
public interface ApplicationContextInitializer<C extends ConfigurableApplicationContext> {

	/**
	 * 初始化给定的应用上下文。
	 * @param applicationContext 要配置的应用上下文
	 */
	void initialize(C applicationContext);

}
