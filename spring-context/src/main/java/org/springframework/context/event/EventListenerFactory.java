/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.context.event;

import java.lang.reflect.Method;

import org.springframework.context.ApplicationListener;

/**
 * 用于为标注了 {@link EventListener} 的方法创建 {@link ApplicationListener} 的策略接口。
 *
 * @author Stephane Nicoll
 * @since 4.2
 */
public interface EventListenerFactory {

	/**
	 * 指定此工厂是否支持指定的 {@link Method}。
	 * @param method 一个标注了 {@link EventListener} 的方法
	 * @return 如果此工厂支持指定的方法则返回 {@code true}
	 */
	boolean supportsMethod(Method method);

	/**
	 * 为指定的方法创建一个 {@link ApplicationListener}。
	 * @param beanName bean 的名称
	 * @param type 实例的目标类型
	 * @param method 标注了 {@link EventListener} 的方法
	 * @return 一个适用于调用指定方法的应用监听器
	 */
	ApplicationListener<?> createApplicationListener(String beanName, Class<?> type, Method method);

}
