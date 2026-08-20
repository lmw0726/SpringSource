/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.beans.factory.Aware;
import org.springframework.core.io.ResourceLoader;

/**
 * 希望获知其运行所在 {@link ResourceLoader}（通常是 ApplicationContext）的任何对象都应实现此接口。
 * 这是通过 {@link org.springframework.context.ApplicationContextAware} 接口获取完整
 * {@link ApplicationContext} 依赖的一种替代方案。
 *
 * <p>请注意，{@link org.springframework.core.io.Resource} 依赖也可以通过 {@code Resource}
 * 或 {@code Resource[]} 类型的 bean 属性来暴露，由 bean 工厂通过字符串自动类型转换来填充。
 * 这样就无需为了访问特定的文件资源而实现任何回调接口。
 *
 * <p>当应用程序对象需要访问名称经过计算的各种文件资源时，通常需要一个 {@link ResourceLoader}。
 * 一个好的策略是让该对象使用 {@link org.springframework.core.io.DefaultResourceLoader}，
 * 但仍实现 {@code ResourceLoaderAware}，以便在 {@code ApplicationContext} 中运行时可以进行覆盖。
 * 请参阅 {@link org.springframework.context.support.ReloadableResourceBundleMessageSource} 中的示例。
 *
 * <p>传入的 {@code ResourceLoader} 还可以检查 {@link org.springframework.core.io.support.ResourcePatternResolver}
 * 接口并进行相应转换，以便将资源模式解析为 {@code Resource} 对象数组。这在 ApplicationContext 中运行时总是有效的
 *（因为上下文接口扩展了 ResourcePatternResolver 接口）。使用
 * {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver} 作为默认实现；
 * 另请参阅 {@code ResourcePatternUtils.getResourcePatternResolver} 方法。
 *
 * <p>作为 {@code ResourcePatternResolver} 依赖的替代方案，可以考虑暴露 {@code Resource[]}
 * 数组类型的 bean 属性，由 bean 工厂在绑定时通过模式字符串自动类型转换来填充。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 10.03.2004
 * @see ApplicationContextAware
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ResourceLoader
 * @see org.springframework.core.io.support.ResourcePatternResolver
 */
public interface ResourceLoaderAware extends Aware {

	/**
	 * 设置此对象运行所在的 ResourceLoader。
	 * <p>这可能是一个 ResourcePatternResolver，可以通过 {@code instanceof ResourcePatternResolver}
	 * 进行检查。另请参阅 {@code ResourcePatternUtils.getResourcePatternResolver} 方法。
	 * <p>在普通 bean 属性填充之后、初始化回调之前调用，例如 InitializingBean 的
	 * {@code afterPropertiesSet} 或自定义的 init-method。在 ApplicationContextAware 的
	 * {@code setApplicationContext} 之前调用。
	 * @param resourceLoader 供此对象使用的 ResourceLoader 对象
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 * @see org.springframework.core.io.support.ResourcePatternUtils#getResourcePatternResolver
	 */
	void setResourceLoader(ResourceLoader resourceLoader);

}
