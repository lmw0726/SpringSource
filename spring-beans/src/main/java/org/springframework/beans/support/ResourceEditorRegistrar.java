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

package org.springframework.beans.support;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.PropertyEditorRegistrySupport;
import org.springframework.beans.propertyeditors.*;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.*;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.xml.sax.InputSource;

import java.beans.PropertyEditor;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

/**
 * PropertyEditorRegistrar 的实现，用于向给定的 PropertyEditorRegistry（通常是用于在 ApplicationContext 中创建 bean 的 BeanWrapper）
 * 注册资源编辑器。被 AbstractApplicationContext 使用。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 2.0
 */
public class ResourceEditorRegistrar implements PropertyEditorRegistrar {
	/**
	 * 属性解析器
	 */
	private final PropertyResolver propertyResolver;
	/**
	 * 资源加载器
	 */
	private final ResourceLoader resourceLoader;


	/**
	 * 为给定的 ResourceLoader 和 PropertyResolver 创建一个新的 ResourceEditorRegistrar。
	 *
	 * @param resourceLoader   ResourceLoader（或 ResourcePatternResolver），用于为其创建编辑器（通常是 ApplicationContext）
	 * @param propertyResolver PropertyResolver（通常是 Environment）
	 * @see org.springframework.core.env.Environment
	 * @see org.springframework.core.io.support.ResourcePatternResolver
	 * @see org.springframework.context.ApplicationContext
	 */
	public ResourceEditorRegistrar(ResourceLoader resourceLoader, PropertyResolver propertyResolver) {
		this.resourceLoader = resourceLoader;
		this.propertyResolver = propertyResolver;
	}


	/**
	 * 使用以下资源编辑器填充给定的注册表 registry：
	 * ResourceEditor、InputStreamEditor、InputSourceEditor、FileEditor、URLEditor、URIEditor、ClassEditor、ClassArrayEditor。
	 * <p>如果此注册器已配置了 ResourcePatternResolver，则还将注册 ResourceArrayPropertyEditor。
	 *
	 * @see org.springframework.core.io.ResourceEditor
	 * @see org.springframework.beans.propertyeditors.InputStreamEditor
	 * @see org.springframework.beans.propertyeditors.InputSourceEditor
	 * @see org.springframework.beans.propertyeditors.FileEditor
	 * @see org.springframework.beans.propertyeditors.URLEditor
	 * @see org.springframework.beans.propertyeditors.URIEditor
	 * @see org.springframework.beans.propertyeditors.ClassEditor
	 * @see org.springframework.beans.propertyeditors.ClassArrayEditor
	 * @see org.springframework.core.io.support.ResourceArrayPropertyEditor
	 */
	@Override
	public void registerCustomEditors(PropertyEditorRegistry registry) {
		// 创建 ResourceEditor 实例，并使用 resourceLoader 和 propertyResolver 进行初始化
		ResourceEditor baseEditor = new ResourceEditor(this.resourceLoader, this.propertyResolver);
		// 注册 Resource 类型的编辑器
		doRegisterEditor(registry, Resource.class, baseEditor);
		// 注册 ContextResource 类型的编辑器
		doRegisterEditor(registry, ContextResource.class, baseEditor);
		// 注册 WritableResource 类型的编辑器
		doRegisterEditor(registry, WritableResource.class, baseEditor);
		// 注册 InputStream 类型的编辑器
		doRegisterEditor(registry, InputStream.class, new InputStreamEditor(baseEditor));
		// 注册 InputSource 类型的编辑器
		doRegisterEditor(registry, InputSource.class, new InputSourceEditor(baseEditor));
		// 注册 File 类型的编辑器
		doRegisterEditor(registry, File.class, new FileEditor(baseEditor));
		// 注册 Path 类型的编辑器
		doRegisterEditor(registry, Path.class, new PathEditor(baseEditor));
		// 注册 Reader 类型的编辑器
		doRegisterEditor(registry, Reader.class, new ReaderEditor(baseEditor));
		// 注册 URL 类型的编辑器
		doRegisterEditor(registry, URL.class, new URLEditor(baseEditor));

		// 获取资源加载器的类加载器
		ClassLoader classLoader = this.resourceLoader.getClassLoader();
		// 注册 URI 类型的编辑器
		doRegisterEditor(registry, URI.class, new URIEditor(classLoader));
		// 注册 Class 类型的编辑器
		doRegisterEditor(registry, Class.class, new ClassEditor(classLoader));
		// 注册 Class[] 类型的编辑器
		doRegisterEditor(registry, Class[].class, new ClassArrayEditor(classLoader));

		// 如果资源加载器是 ResourcePatternResolver 类型
		if (this.resourceLoader instanceof ResourcePatternResolver) {
			// 注册 Resource[] 类型的编辑器
			doRegisterEditor(registry, Resource[].class,
					new ResourceArrayPropertyEditor((ResourcePatternResolver) this.resourceLoader, this.propertyResolver));
		}
	}

	/**
	 * 如果可能，覆盖默认编辑器（因为这实际上是我们想要在这里做的事情）；否则注册为自定义编辑器。
	 *
	 * @param registry     属性编辑器注册表
	 * @param requiredType 所需类型
	 * @param editor       属性编辑器
	 */
	private void doRegisterEditor(PropertyEditorRegistry registry, Class<?> requiredType, PropertyEditor editor) {
		// 如果 registry 是 PropertyEditorRegistrySupport 的实例
		if (registry instanceof PropertyEditorRegistrySupport) {
			// 使用 overrideDefaultEditor 方法覆盖默认的编辑器
			((PropertyEditorRegistrySupport) registry).overrideDefaultEditor(requiredType, editor);
		} else {
			// 否则，使用 registerCustomEditor 方法注册自定义编辑器
			registry.registerCustomEditor(requiredType, editor);
		}
	}

}
