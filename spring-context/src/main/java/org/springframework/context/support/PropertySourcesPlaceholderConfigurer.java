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

package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringValueResolver;

import java.io.IOException;
import java.util.Properties;

/**
 * {@code PlaceholderConfigurerSupport} 的特化版本，用于解析 bean 定义属性值和 {@code @Value} 注解中的 ${...} 占位符，
 * 根据当前 Spring {@link Environment} 及其一组 {@link PropertySources} 进行解析。
 *
 * <p>这个类被设计为 {@code PropertyPlaceholderConfigurer} 的通用替代品。它默认用于支持 {@code property-placeholder} 元素，
 * 以便与 spring-context-3.1 或更高版本的 XSD 配合使用；而 spring-context 版本 &lt;= 3.0 默认使用 {@code PropertyPlaceholderConfigurer}，
 * 以确保向后兼容性。有关完整详细信息，请参阅 spring-context XSD 文档。
 *
 * <p>任何本地属性（例如，通过 {@link #setProperties}、{@link #setLocations} 等添加的属性）都将作为 {@code PropertySource} 添加。
 * 本地属性的搜索优先级基于 {@link #setLocalOverride localOverride} 属性的值，默认为 {@code false}，表示本地属性将在所有环境属性源之后搜索。
 *
 * <p>有关操作环境属性源的详细信息，请参阅 {@link org.springframework.core.env.ConfigurableEnvironment} 和相关的 javadocs。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see org.springframework.core.env.ConfigurableEnvironment
 * @see org.springframework.beans.factory.config.PlaceholderConfigurerSupport
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 * @since 3.1
 */
public class PropertySourcesPlaceholderConfigurer extends PlaceholderConfigurerSupport implements EnvironmentAware {

	/**
	 * 提供给此配置器的一组 {@linkplain #mergeProperties() 合并的属性} 的 {@link PropertySource} 的名称是 {@value}。
	 */
	public static final String LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME = "localProperties";

	/**
	 * 包装 {@linkplain #setEnvironment 提供给此配置器的环境} 的 {@link PropertySource} 的名称是 {@value}。
	 */
	public static final String ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME = "environmentProperties";


	/**
	 * 可变的属性源
	 */
	@Nullable
	private MutablePropertySources propertySources;

	/**
	 * 已经应用的属性源
	 */
	@Nullable
	private PropertySources appliedPropertySources;

	/**
	 * 当前的环境
	 */
	@Nullable
	private Environment environment;


	/**
	 * 自定义此配置器使用的属性源集合。
	 * <p>设置此属性表示应忽略环境属性源和本地属性。
	 *
	 * @see #postProcessBeanFactory
	 */
	public void setPropertySources(PropertySources propertySources) {
		this.propertySources = new MutablePropertySources(propertySources);
	}

	/**
	 * 当替换 ${...} 占位符时，将搜索给定 {@link Environment} 的 {@code PropertySources}。
	 *
	 * @see #setPropertySources
	 * @see #postProcessBeanFactory
	 */
	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}


	/**
	 * 处理过程是通过在bean定义中替换 ${...} 占位符来解析每个占位符，
	 * 这些占位符根据该配置器的一组 {@link PropertySources} 进行解析，其中包括：
	 * <ul>
	 * <li>所有 {@linkplain org.springframework.core.env.ConfigurableEnvironment#getPropertySources
	 * 环境属性源}，如果存在 {@code Environment} {@linkplain #setEnvironment
	 * 设置了}
	 * <li>{@linkplain #mergeProperties 合并的本地属性}，如果 {@linkplain #setLocation 有任何}
	 * {@linkplain #setLocations 被}
	 * {@linkplain #setPropertiesArray 指定}
	 * <li>通过调用 {@link #setPropertySources} 设置的任何属性源
	 * </ul>
	 * <p>如果调用了 {@link #setPropertySources}，<strong>将忽略环境和本地属性</strong>。
	 * 此方法旨在为用户提供对属性源的细粒度控制，一旦设置，配置器就不会假设添加额外的源。
	 */
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (this.propertySources == null) {
			// 如果属性源为空，则创建一个 MutablePropertySources 实例。
			this.propertySources = new MutablePropertySources();
			// 如果环境不为空，则使用环境作为 PropertyResolver。
			if (this.environment != null) {
				PropertyResolver propertyResolver = this.environment;
				// 如果设置了 ignoreUnresolvablePlaceholders 标志为 true，则需要创建一个本地的 PropertyResolver 来强制该设置，
				// 因为环境很可能没有配置 ignoreUnresolvablePlaceholders 为 true。
				// 详情请参阅 https://github.com/spring-projects/spring-framework/issues/27947
				// 如果设置了 ignoreUnresolvablePlaceholders 标志为 true，并且环境是 ConfigurableEnvironment 的实例，则执行以下操作。
				if (this.ignoreUnresolvablePlaceholders && (this.environment instanceof ConfigurableEnvironment)) {
					// 将环境转换为 ConfigurableEnvironment 类型。
					ConfigurableEnvironment configurableEnvironment = (ConfigurableEnvironment) this.environment;
					// 创建一个 PropertySourcesPropertyResolver 实例，用于解析属性源中的属性。
					PropertySourcesPropertyResolver resolver =
							new PropertySourcesPropertyResolver(configurableEnvironment.getPropertySources());
					// 设置解析器以忽略不可解析的嵌套占位符。
					resolver.setIgnoreUnresolvableNestedPlaceholders(true);
					// 使用新的解析器作为属性解析器。
					propertyResolver = resolver;
				}

				// 将环境添加到属性源中。
				PropertyResolver propertyResolverToUse = propertyResolver;
				this.propertySources.addLast(
						new PropertySource<Environment>(ENVIRONMENT_PROPERTIES_PROPERTY_SOURCE_NAME, this.environment) {
							@Override
							@Nullable
							public String getProperty(String key) {
								return propertyResolverToUse.getProperty(key);
							}
						}
				);
			}
			try {
				// 创建一个本地属性源，将合并后的属性添加到属性源中。
				PropertySource<?> localPropertySource =
						new PropertiesPropertySource(LOCAL_PROPERTIES_PROPERTY_SOURCE_NAME, mergeProperties());
				if (this.localOverride) {
					// 如果设置了本地属性覆盖文件中的属性，则将本地属性源添加到属性源的第一个位置
					this.propertySources.addFirst(localPropertySource);
				} else {
					// 否则添加到最后一个位置。
					this.propertySources.addLast(localPropertySource);
				}
			} catch (IOException ex) {
				throw new BeanInitializationException("Could not load properties", ex);
			}
		}

		// 处理属性，使用 PropertySourcesPropertyResolver 解析属性值，并将当前属性源设置为已应用的属性源。
		processProperties(beanFactory, new PropertySourcesPropertyResolver(this.propertySources));
		this.appliedPropertySources = this.propertySources;
	}

	/**
	 * 访问给定的bean工厂中的每个bean定义，并尝试使用给定属性中的值替换 ${...} 属性占位符。
	 *
	 * @param beanFactoryToProcess 要处理的可配置的可列出的bean工厂
	 * @param propertyResolver 属性解析器，用于解析占位符和获取属性值
	 * @throws BeansException 如果无法处理属性占位符或解析属性值
	 */
	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess,
									 final ConfigurablePropertyResolver propertyResolver) throws BeansException {

		// 设置属性解析器的占位符前缀、后缀和值分隔符。
		propertyResolver.setPlaceholderPrefix(this.placeholderPrefix);
		propertyResolver.setPlaceholderSuffix(this.placeholderSuffix);
		propertyResolver.setValueSeparator(this.valueSeparator);

		// 创建一个 StringValueResolver 实例，用于解析属性值中的占位符。
		StringValueResolver valueResolver = strVal -> {
			// 解析属性值中的占位符，如果设置了 ignoreUnresolvablePlaceholders，则使用 resolvePlaceholders 方法解析；
			// 否则，使用 resolveRequiredPlaceholders 方法解析。
			String resolved = (this.ignoreUnresolvablePlaceholders ?
					propertyResolver.resolvePlaceholders(strVal) :
					propertyResolver.resolveRequiredPlaceholders(strVal));
			// 如果设置了 trimValues 标志为 true，则去除解析后的值的前后空格。
			if (this.trimValues) {
				resolved = resolved.trim();
			}
			// 如果解析后的值等于 nullValue，则返回 null；否则，返回解析后的值。
			return (resolved.equals(this.nullValue) ? null : resolved);
		};

		// 使用属性值解析器对 BeanFactory 进行属性值的解析。
		doProcessProperties(beanFactoryToProcess, valueResolver);
	}

	/**
	 * 为了与 org.springframework.beans.factory.config.PlaceholderConfigurerSupport 兼容而实现。
	 *
	 * @param beanFactory 要处理的可配置的可列出的bean工厂（在此实现中未使用）
	 * @param props 用于替换属性占位符的属性集（在此实现中未使用）
	 * @throws UnsupportedOperationException 在此实现中抛出
	 * @deprecated 推荐使用 processProperties(ConfigurableListableBeanFactory, ConfigurablePropertyResolver)
	 */
	@Override
	@Deprecated
	protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props) {
		throw new UnsupportedOperationException(
				"Call processProperties(ConfigurableListableBeanFactory, ConfigurablePropertyResolver) instead");
	}

	/**
	 * 返回在 postProcessBeanFactory(ConfigurableListableBeanFactory) 后实际应用的属性源。
	 *
	 * @return 应用的属性源
	 * @throws IllegalStateException 如果属性源尚未应用
	 * @since 4.0
	 */
	public PropertySources getAppliedPropertySources() throws IllegalStateException {
		Assert.state(this.appliedPropertySources != null, "PropertySources have not yet been applied");
		return this.appliedPropertySources;
	}

}
