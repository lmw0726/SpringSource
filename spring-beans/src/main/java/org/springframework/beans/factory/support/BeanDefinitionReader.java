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

package org.springframework.beans.factory.support;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

/**
 * 为 bean 定义读取器定义的简单接口，指定了使用 {@link Resource} 和 {@code String} 类型路径参数的加载方法。
 *
 * <p>具体的 bean 定义读取器当然可以添加额外的加载和注册方法，以支持其特定的 bean 定义格式。
 *
 * <p>注意：bean 定义读取器并非必须实现此接口。它仅作为希望遵循标准命名规范的
 * bean 定义读取器的一种建议。
 *
 * @author Juergen Hoeller
 * @see org.springframework.core.io.Resource
 * @since 1.1
 */
public interface BeanDefinitionReader {

	/**
	 * 返回用于注册 bean 定义的 bean 工厂。
	 * <p>该工厂通过 {@link BeanDefinitionRegistry} 接口暴露，封装了与 bean 定义处理相关的方法。
	 */
	BeanDefinitionRegistry getRegistry();

	/**
	 * 返回用于资源路径的 {@link ResourceLoader}。
	 * <p>可以检查是否实现了 {@code ResourcePatternResolver} 接口，并相应地进行类型转换，以加载给定模式匹配的多个资源。
	 * <p>返回 {@code null} 表示该 bean 定义读取器不支持绝对路径资源加载。
	 * <p>这主要用于在 bean 定义资源内部导入其他资源，例如 XML bean 定义中的 "import" 标签。
	 * 然而，建议相对于当前定义资源进行此类导入；只有显式的完整资源路径才会触发基于绝对路径的资源加载。
	 * <p>此外还提供了 {@code loadBeanDefinitions(String)} 方法，用于从资源路径（或路径模式）加载 bean 定义，
	 * 这是一种便利方式，可避免显式处理 {@code ResourceLoader}。
	 *
	 * @see #loadBeanDefinitions(String)
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 */
	@Nullable
	ResourceLoader getResourceLoader();

	/**
	 * 返回用于加载 bean 类的类加载器。
	 * <p>返回 {@code null} 表示不应急于加载 bean 类，而应仅使用类名注册 bean 定义，
	 * 对应的类将在后续解析（或永不解析）。
	 */
	@Nullable
	ClassLoader getBeanClassLoader();

	/**
	 * 返回用于匿名 bean（未显式指定 bean 名称）的 {@link BeanNameGenerator}。
	 */
	BeanNameGenerator getBeanNameGenerator();


	/**
	 * 从指定的资源加载bean定义。
	 *
	 * @param resource 资源描述符
	 * @return 找到的bean定义的数量
	 * @throws BeanDefinitionStoreException 在加载或解析错误的情况下
	 */
	int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException;

	/**
	 * 从指定的资源中加载 bean 定义。
	 *
	 * @param resources 资源描述符
	 * @return 找到的 bean 定义数量
	 * @throws BeanDefinitionStoreException 如果发生加载或解析错误
	 */
	int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException;

	/**
	 * 从指定的资源路径加载 bean 定义。
	 * <p>该路径也可以是一个路径模式，前提是此 bean 定义读取器的 {@link ResourceLoader}
	 * 是一个 {@code ResourcePatternResolver}。
	 *
	 * @param location 资源路径，将使用此 bean 定义读取器的 {@code ResourceLoader}
	 *                 （或 {@code ResourcePatternResolver}）进行加载
	 * @return 找到的 bean 定义数量
	 * @throws BeanDefinitionStoreException 如果发生加载或解析错误
	 * @see #getResourceLoader()
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource)
	 * @see #loadBeanDefinitions(org.springframework.core.io.Resource[])
	 */
	int loadBeanDefinitions(String location) throws BeanDefinitionStoreException;

	/**
	 * 从指定的多个资源路径加载 bean 定义。
	 *
	 * @param locations 资源路径列表，将使用此 bean 定义读取器的 {@code ResourceLoader}
	 *                  （或 {@code ResourcePatternResolver}）进行加载
	 * @return 找到的 bean 定义数量
	 * @throws BeanDefinitionStoreException 如果发生加载或解析错误
	 */
	int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException;

}
