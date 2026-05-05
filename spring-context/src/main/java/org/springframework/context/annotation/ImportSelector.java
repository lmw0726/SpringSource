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

package org.springframework.context.annotation;

import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.Nullable;

import java.util.function.Predicate;

/**
 * 要由实现类实现的接口，用于根据给定的选择条件（通常是一个或多个注解属性），
 * 决定应导入哪些 @{@link Configuration} 类。
 *
 * <p>{@link ImportSelector} 可以实现以下任意一个
 * {@link org.springframework.beans.factory.Aware Aware} 接口，
 * 对应的方法会在调用 {@link #selectImports} 之前被执行：
 * <ul>
 * <li>{@link org.springframework.context.EnvironmentAware EnvironmentAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactoryAware BeanFactoryAware}</li>
 * <li>{@link org.springframework.beans.factory.BeanClassLoaderAware BeanClassLoaderAware}</li>
 * <li>{@link org.springframework.context.ResourceLoaderAware ResourceLoaderAware}</li>
 * </ul>
 *
 * <p>或者，该类也可以提供一个构造函数，参数类型可以是以下任意一个或多个：
 * <ul>
 * <li>{@link org.springframework.core.env.Environment Environment}</li>
 * <li>{@link org.springframework.beans.factory.BeanFactory BeanFactory}</li>
 * <li>{@link java.lang.ClassLoader ClassLoader}</li>
 * <li>{@link org.springframework.core.io.ResourceLoader ResourceLoader}</li>
 * </ul>
 *
 * <p>{@code ImportSelector} 实现类通常和普通的 {@code @Import} 注解一样被处理。
 * 不过，也可以推迟导入类的选择，直到所有 {@code @Configuration} 类都处理完成
 * （详见 {@link DeferredImportSelector}）。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see DeferredImportSelector
 * @see Import
 * @see ImportBeanDefinitionRegistrar
 * @see Configuration
 */
public interface ImportSelector {

	/**
	 * 根据导入的 @{@link Configuration} 类的 {@link AnnotationMetadata}，选择并返回
	 * 应该导入的类的名称。
	 * @return 类名数组，如果没有则返回空数组
	 */
	String[] selectImports(AnnotationMetadata importingClassMetadata);

	/**
	 * 返回一个用于排除导入候选类的断言（Predicate），会递归应用到通过该选择器找到的所有类上。
	 * <p>如果该断言返回 {@code true}，则对应的类不会被视为导入的配置类，Spring 将跳过该类的
	 * 类文件加载和元数据解析。
	 * @return 一个用于排除传递性导入配置类的完全限定类名的断言，
	 * 如果不需要过滤则返回 {@code null}
	 * @since 5.2.4
	 */
	@Nullable
	default Predicate<String> getExclusionFilter() {
		return null;
	}
}
