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

package org.springframework.context.annotation;

import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.parsing.ProblemReporter;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.*;

/**
 * 表示用户定义的 {@link Configuration @Configuration} 类。
 * <p>包含一组 {@link Bean} 方法，包括该类祖先中定义的所有此类方法，以"扁平化"的方式呈现。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @see BeanMethod
 * @see ConfigurationClassParser
 * @since 3.0
 */
final class ConfigurationClass {

	private final AnnotationMetadata metadata;

	private final Resource resource;

	@Nullable
	private String beanName;

	private final Set<ConfigurationClass> importedBy = new LinkedHashSet<>(1);

	private final Set<BeanMethod> beanMethods = new LinkedHashSet<>();

	private final Map<String, Class<? extends BeanDefinitionReader>> importedResources =
			new LinkedHashMap<>();

	private final Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> importBeanDefinitionRegistrars =
			new LinkedHashMap<>();

	final Set<String> skippedBeanMethods = new HashSet<>();


	/**
	 * 使用给定的名称创建一个新的 {@link ConfigurationClass}。
	 *
	 * @param metadataReader 用于解析底层 {@link Class} 的读取器
	 * @param beanName       不能为 {@code null}
	 * @see ConfigurationClass#ConfigurationClass(Class, ConfigurationClass)
	 */
	ConfigurationClass(MetadataReader metadataReader, String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		this.metadata = metadataReader.getAnnotationMetadata();
		this.resource = metadataReader.getResource();
		this.beanName = beanName;
	}

	/**
	 * 创建一个新的 {@link ConfigurationClass}，表示通过 {@link Import} 注解导入的类，
	 * 或自动处理为嵌套配置类的类（如果 importedBy 不为 {@code null}）。
	 *
	 * @param metadataReader 用于解析底层 {@link Class} 的读取器
	 * @param importedBy     导入此类的配置类或 {@code null}
	 * @since 3.1.1
	 */
	ConfigurationClass(MetadataReader metadataReader, @Nullable ConfigurationClass importedBy) {
		this.metadata = metadataReader.getAnnotationMetadata();
		this.resource = metadataReader.getResource();
		this.importedBy.add(importedBy);
	}

	/**
	 * 使用给定的名称创建一个新的 {@link ConfigurationClass}。
	 *
	 * @param clazz    要表示的底层 {@link Class}
	 * @param beanName {@code @Configuration} 类 bean 的名称
	 * @see ConfigurationClass#ConfigurationClass(Class, ConfigurationClass)
	 */
	ConfigurationClass(Class<?> clazz, String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		this.metadata = AnnotationMetadata.introspect(clazz);
		this.resource = new DescriptiveResource(clazz.getName());
		this.beanName = beanName;
	}

	/**
	 * 创建一个新的 {@link ConfigurationClass}，表示通过 {@link Import} 注解导入的类，
	 * 或自动处理为嵌套配置类的类（如果 imported 为 {@code true}）。
	 *
	 * @param clazz      要表示的底层 {@link Class}
	 * @param importedBy 导入此类的配置类（或 {@code null}）
	 * @since 3.1.1
	 */
	ConfigurationClass(Class<?> clazz, @Nullable ConfigurationClass importedBy) {
		this.metadata = AnnotationMetadata.introspect(clazz);
		this.resource = new DescriptiveResource(clazz.getName());
		this.importedBy.add(importedBy);
	}

	/**
	 * 使用给定的名称创建一个新的 {@link ConfigurationClass}。
	 *
	 * @param metadata 要表示的底层类的元数据
	 * @param beanName {@code @Configuration} 类 bean 的名称
	 * @see ConfigurationClass#ConfigurationClass(Class, ConfigurationClass)
	 */
	ConfigurationClass(AnnotationMetadata metadata, String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		this.metadata = metadata;
		this.resource = new DescriptiveResource(metadata.getClassName());
		this.beanName = beanName;
	}


	AnnotationMetadata getMetadata() {
		return this.metadata;
	}

	Resource getResource() {
		return this.resource;
	}

	String getSimpleName() {
		return ClassUtils.getShortName(getMetadata().getClassName());
	}

	void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * 返回此配置类是否通过 @{@link Import} 注册，
	 * 或由于嵌套在另一个配置类中而自动注册。
	 *
	 * @see #getImportedBy()
	 * @since 3.1.1
	 */
	public boolean isImported() {
		return !this.importedBy.isEmpty();
	}

	/**
	 * 将给定配置类的导入声明合并到此配置类中。
	 *
	 * @since 4.0.5
	 */
	void mergeImportedBy(ConfigurationClass otherConfigClass) {
		this.importedBy.addAll(otherConfigClass.importedBy);
	}

	/**
	 * 返回导入此配置类的配置类，
	 * 如果此配置未被导入，则返回空 Set。
	 *
	 * @see #isImported()
	 * @since 4.0.5
	 */
	Set<ConfigurationClass> getImportedBy() {
		return this.importedBy;
	}

	void addBeanMethod(BeanMethod method) {
		this.beanMethods.add(method);
	}

	Set<BeanMethod> getBeanMethods() {
		return this.beanMethods;
	}

	void addImportedResource(String importedResource, Class<? extends BeanDefinitionReader> readerClass) {
		this.importedResources.put(importedResource, readerClass);
	}

	void addImportBeanDefinitionRegistrar(ImportBeanDefinitionRegistrar registrar, AnnotationMetadata importingClassMetadata) {
		this.importBeanDefinitionRegistrars.put(registrar, importingClassMetadata);
	}

	Map<ImportBeanDefinitionRegistrar, AnnotationMetadata> getImportBeanDefinitionRegistrars() {
		return this.importBeanDefinitionRegistrars;
	}

	Map<String, Class<? extends BeanDefinitionReader>> getImportedResources() {
		return this.importedResources;
	}

	void validate(ProblemReporter problemReporter) {
		// 配置类不能是 final 的（CGLIB 限制），除非声明了 proxyBeanMethods=false
		Map<String, Object> attributes = this.metadata.getAnnotationAttributes(Configuration.class.getName());
		if (attributes != null && (Boolean) attributes.get("proxyBeanMethods")) {
			if (this.metadata.isFinal()) {
				problemReporter.error(new FinalConfigurationProblem());
			}
			for (BeanMethod beanMethod : this.beanMethods) {
				beanMethod.validate(problemReporter);
			}
		}
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ConfigurationClass &&
				getMetadata().getClassName().equals(((ConfigurationClass) other).getMetadata().getClassName())));
	}

	@Override
	public int hashCode() {
		return getMetadata().getClassName().hashCode();
	}

	@Override
	public String toString() {
		return "ConfigurationClass: beanName '" + this.beanName + "', " + this.resource;
	}


	/**
	 * 配置类必须是非 final 的，以适应 CGLIB 子类化。
	 */
	private class FinalConfigurationProblem extends Problem {

		FinalConfigurationProblem() {
			super(String.format("@Configuration class '%s' may not be final. Remove the final modifier to continue.",
					getSimpleName()), new Location(getResource(), getMetadata()));
		}
	}

}
