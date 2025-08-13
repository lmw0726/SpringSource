/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.core.io.support;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

/**
 * 用于将位置模式（例如 Ant 风格的路径模式）解析为 {@link Resource} 对象的策略接口。
 *
 * <p>这是对 {@link org.springframework.core.io.ResourceLoader} 接口的扩展。
 * 传入的 {@code ResourceLoader}（例如通过
 * {@link org.springframework.context.ResourceLoaderAware} 在上下文中传入的
 * {@link org.springframework.context.ApplicationContext}）可以检查它是否也实现了此扩展接口。
 *
 * <p>{@link PathMatchingResourcePatternResolver} 是一个独立实现，
 * 可在 {@code ApplicationContext} 之外使用，也被
 * {@link ResourceArrayPropertyEditor} 用于填充 {@code Resource} 数组类型的 Bean 属性。
 *
 * <p>可以用于任何类型的位置模式 —— 例如 {@code "/WEB-INF/*-context.xml"}。
 * 但输入的模式必须符合策略实现的规则。此接口仅定义转换方法，而不限定具体的模式格式。
 *
 * <p>该接口还定义了 {@code "classpath*:"} 资源前缀，用于匹配类路径中所有符合条件的资源。
 * 注意资源位置也可以包含占位符 —— 例如 {@code "/beans-*.xml"}。
 * 类路径中的 JAR 文件或不同目录中可能包含多个同名文件。
 *
 * @author Juergen Hoeller
 * @see org.springframework.core.io.Resource
 * @see org.springframework.core.io.ResourceLoader
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.ResourceLoaderAware
 * @since 1.0.2
 */
public interface ResourcePatternResolver extends ResourceLoader {

	/**
	 * “classpath*:”这与ResourceLoader的classpath URL前缀不同，它检索给定名称的所有匹配资源(例如。“/beans.xml”)，例如在所有已部署JAR文件的根目录中。
	 */
	String CLASSPATH_ALL_URL_PREFIX = "classpath*:";

	/**
	 * 获取指定位置模式的多个资源
	 *
	 * @param locationPattern 位置模式
	 * @return 多个资源
	 * @throws IOException IO异常
	 */
	Resource[] getResources(String locationPattern) throws IOException;

}
