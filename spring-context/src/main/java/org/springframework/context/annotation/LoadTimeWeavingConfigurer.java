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

package org.springframework.context.annotation;

import org.springframework.instrument.classloading.LoadTimeWeaver;

/**
 * 需要由使用 {@link EnableLoadTimeWeaving @EnableLoadTimeWeaving} 注解的
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * 类实现的接口，用于自定义要使用的 {@link LoadTimeWeaver} 实例。
 *
 * <p>使用示例和关于不使用此接口时如何选择默认 {@code LoadTimeWeaver} 的信息，
 * 请参阅 {@link org.springframework.scheduling.annotation.EnableAsync @EnableAsync}。
 *
 * @author Chris Beams
 * @since 3.1
 * @see LoadTimeWeavingConfiguration
 * @see EnableLoadTimeWeaving
 */
public interface LoadTimeWeavingConfigurer {

	/**
	 * 创建、配置并返回要使用的 {@code LoadTimeWeaver} 实例。注意，
	 * 不需要用 {@code @Bean} 注解此方法，因为返回的对象将被
	 * {@link LoadTimeWeavingConfiguration#loadTimeWeaver()} 自动注册为 Bean。
	 */
	LoadTimeWeaver getLoadTimeWeaver();

}
