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

package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

/**
 * 用于加载资源（例如类路径或文件系统资源）的策略接口。
 * {@link org.springframework.context.ApplicationContext} 需要提供此功能以及扩展的
 * {@link org.springframework.core.io.support.ResourcePatternResolver} 支持。
 *
 * <p>{@link DefaultResourceLoader} 是一个独立的实现，可以在 ApplicationContext 外部使用，
 * 也被 {@link ResourceEditor} 使用。
 *
 * <p>当在 ApplicationContext 中运行时，可以使用特定上下文的资源加载策略，从字符串中填充类型为
 * {@code Resource} 和 {@code Resource[]} 的 bean 属性。
 *
 * @author Juergen Hoeller
 * @see Resource
 * @see org.springframework.core.io.support.ResourcePatternResolver
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 * @since 10.03.2004
 */
public interface ResourceLoader {

	/**
	 * 用于从类路径加载的伪URL前缀:" classpath:"。
	 */
	String CLASSPATH_URL_PREFIX = ResourceUtils.CLASSPATH_URL_PREFIX;


	/**
	 * 根据位置获取资源，可以重复获取资源
	 * 支持完全限定的URL，如"file:C:/test.dat".
	 * 支持classpath开头的伪路径，如 "classpath:test.dat".
	 * 支持相对的文件路径，如"WEB-INF/test.dat".
	 *
	 * @param location 资源的位置
	 * @return 资源
	 */
	Resource getResource(String location);

	/**
	 * 获取类加载器
	 * 需要直接访问ClassLoader的客户端可以使用ResourceLoader以统一的方式进行访问，
	 * 而不是依赖于线程上下文ClassLoader。
	 *
	 * @return 类加载器(只有在系统ClassLoader不可访问时才为空)
	 */
	@Nullable
	ClassLoader getClassLoader();

}
