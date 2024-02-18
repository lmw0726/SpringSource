/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.core.Constants;
import org.springframework.core.SpringProperties;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.util.StringValueResolver;

import java.util.Properties;

/**
 * {@link PlaceholderConfigurerSupport} 的子类，用于解析 ${...} 占位符针对 {@link #setLocation 本地} {@link #setProperties 属性} 和/或系统属性以及环境变量。
 *
 * <p>当以下情况仍然适用于使用 {@code PropertyPlaceholderConfigurer}：
 * <ul>
 * <li> {@code spring-context} 模块不可用（即，使用 Spring 的 {@code BeanFactory} API 而不是 {@code ApplicationContext}）。
 * <li>现有配置使用 {@link #setSystemPropertiesMode(int) "systemPropertiesMode"} 和/或 {@link #setSystemPropertiesModeName(String) "systemPropertiesModeName"} 属性。鼓励用户摆脱使用这些设置，而是通过容器的 {@code Environment} 配置属性源搜索顺序；然而，通过继续使用 {@code PropertyPlaceholderConfigurer} 可以保持确切的功能保留。
 * </ul>
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see #setSystemPropertiesModeName
 * @see PlaceholderConfigurerSupport
 * @see PropertyOverrideConfigurer
 * @since 02.10.2003
 * @deprecated 从 5.2 开始；使用 {@code org.springframework.context.support.PropertySourcesPlaceholderConfigurer} 代替，它更加灵活，通过利用 {@link org.springframework.core.env.Environment} 和 {@link org.springframework.core.env.PropertySource} 机制。
 */
@Deprecated
public class PropertyPlaceholderConfigurer extends PlaceholderConfigurerSupport {

	/**
	 * 从不检查系统属性。
	 */
	public static final int SYSTEM_PROPERTIES_MODE_NEVER = 0;

	/**
	 * 如果在指定的属性中无法解析，则检查系统属性。这是默认值。
	 */
	public static final int SYSTEM_PROPERTIES_MODE_FALLBACK = 1;

	/**
	 * 首先检查系统属性，然后再尝试指定的属性。这允许系统属性覆盖任何其他属性源。
	 */
	public static final int SYSTEM_PROPERTIES_MODE_OVERRIDE = 2;

	/**
	 * 常量
	 */
	private static final Constants constants = new Constants(PropertyPlaceholderConfigurer.class);

	/**
	 * 系统属性模式，默认为回退模式
	 */
	private int systemPropertiesMode = SYSTEM_PROPERTIES_MODE_FALLBACK;

	/**
	 * 是否搜索系统环境
	 */
	private boolean searchSystemEnvironment =
			!SpringProperties.getFlag(AbstractEnvironment.IGNORE_GETENV_PROPERTY_NAME);


	/**
	 * 根据相应常量的名称设置系统属性模式，例如"SYSTEM_PROPERTIES_MODE_OVERRIDE"。
	 *
	 * @param constantName 常量的名称
	 * @throws IllegalArgumentException 如果常量名无效
	 * @see #setSystemPropertiesMode
	 */
	public void setSystemPropertiesModeName(String constantName) throws IllegalArgumentException {
		this.systemPropertiesMode = constants.asNumber(constantName).intValue();
	}

	/**
	 * 设置如何检查系统属性：作为后备、作为覆盖，或者从不检查。
	 * 例如，将会将 ${user.dir} 解析为 "user.dir" 系统属性。
	 * <p>默认是 "fallback"：如果无法使用指定的属性解析占位符，则尝试使用系统属性。
	 * "override" 将首先检查系统属性，然后再尝试指定的属性。"never" 将完全不检查系统属性。
	 *
	 * @param systemPropertiesMode 检查系统属性的模式
	 * @see #SYSTEM_PROPERTIES_MODE_NEVER
	 * @see #SYSTEM_PROPERTIES_MODE_FALLBACK
	 * @see #SYSTEM_PROPERTIES_MODE_OVERRIDE
	 * @see #setSystemPropertiesModeName
	 */
	public void setSystemPropertiesMode(int systemPropertiesMode) {
		this.systemPropertiesMode = systemPropertiesMode;
	}

	/**
	 * 设置是否在未找到匹配的系统属性时搜索匹配的系统环境变量。
	 * 仅当 "systemPropertyMode" 激活时（即 "fallback" 或 "override"）应用，紧接在检查 JVM 系统属性之后。
	 * <p>默认值为 "true"。关闭此设置以永远不会解析占位符为系统环境变量。
	 * 请注意，通常建议将外部值传递为 JVM 系统属性：这可以很容易地在启动脚本中实现，即使是对于现有环境变量。
	 *
	 * @param searchSystemEnvironment 是否搜索系统环境变量
	 * @see #setSystemPropertiesMode
	 * @see System#getProperty(String)
	 * @see System#getenv(String)
	 */
	public void setSearchSystemEnvironment(boolean searchSystemEnvironment) {
		this.searchSystemEnvironment = searchSystemEnvironment;
	}

	/**
	 * 使用给定的属性解析给定的占位符，并根据给定的模式执行系统属性检查。
	 * <p>默认实现在系统属性检查之前/之后委托给 {@code resolvePlaceholder(placeholder, props)}。
	 * <p>子类可以重写此方法以实现自定义的解析策略，包括自定义的系统属性检查点。
	 *
	 * @param placeholder          要解析的占位符
	 * @param props                此配置器的合并属性
	 * @param systemPropertiesMode 系统属性模式，根据此类中的常量
	 * @return 解析后的值，如果没有则为 null
	 * @see #setSystemPropertiesMode
	 * @see System#getProperty
	 * @see #resolvePlaceholder(String, java.util.Properties)
	 */
	@Nullable
	protected String resolvePlaceholder(String placeholder, Properties props, int systemPropertiesMode) {
		// 初始化属性值为空。
		String propVal = null;
		// 如果系统属性模式为 SYSTEM_PROPERTIES_MODE_OVERRIDE，则尝试从系统属性中解析占位符。
		if (systemPropertiesMode == SYSTEM_PROPERTIES_MODE_OVERRIDE) {
			propVal = resolveSystemProperty(placeholder);
		}
		// 如果属性值仍为空，则尝试从属性文件中解析占位符。
		if (propVal == null) {
			propVal = resolvePlaceholder(placeholder, props);
		}
		// 如果属性值仍为空且系统属性模式为 SYSTEM_PROPERTIES_MODE_FALLBACK，则再次尝试从系统属性中解析占位符。
		if (propVal == null && systemPropertiesMode == SYSTEM_PROPERTIES_MODE_FALLBACK) {
			propVal = resolveSystemProperty(placeholder);
		}
		// 返回解析后的属性值。
		return propVal;
	}

	/**
	 * 使用给定的属性解析给定的占位符。
	 * 默认实现只检查相应的属性键。
	 * <p>子类可以重写此方法以进行自定义的占位符到键的映射或自定义的解析策略，可能只使用给定的属性作为回退。
	 * <p>请注意，根据系统属性模式，在调用此方法之前或之后仍将检查系统属性。
	 *
	 * @param placeholder 要解析的占位符
	 * @param props       此配置器的合并属性
	 * @return 解析后的值，如果没有则为 {@code null}
	 * @see #setSystemPropertiesMode
	 */
	@Nullable
	protected String resolvePlaceholder(String placeholder, Properties props) {
		return props.getProperty(placeholder);
	}

	/**
	 * 将给定的键解析为 JVM 系统属性，并在没有找到匹配的系统属性时可选择作为系统环境变量。
	 *
	 * @param key 要解析为系统属性键的占位符
	 * @return 系统属性值，如果未找到则为 {@code null}
	 * @see #setSearchSystemEnvironment
	 * @see System#getProperty(String)
	 * @see System#getenv(String)
	 */
	@Nullable
	protected String resolveSystemProperty(String key) {
		try {
			String value = System.getProperty(key);
			if (value == null && this.searchSystemEnvironment) {
				value = System.getenv(key);
			}
			return value;
		} catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not access system property '" + key + "': " + ex);
			}
			return null;
		}
	}


	/**
	 * 访问给定的 Bean 工厂中的每个 Bean 定义，并尝试使用给定属性的值替换 ${...} 属性占位符。
	 *
	 * @param beanFactoryToProcess 要处理的 Bean 工厂
	 * @param props                包含属性值的属性集合
	 * @throws BeansException 如果在替换过程中发生异常
	 */
	@Override
	protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props)
			throws BeansException {

		// 创建一个 PlaceholderResolvingStringValueResolver 实例，用于解析占位符。
		StringValueResolver valueResolver = new PlaceholderResolvingStringValueResolver(props);
		// 使用创建的 valueResolver 对象处理属性值。
		doProcessProperties(beanFactoryToProcess, valueResolver);
	}


	private class PlaceholderResolvingStringValueResolver implements StringValueResolver {

		/**
		 * 属性占位符辅助器
		 */
		private final PropertyPlaceholderHelper helper;

		/**
		 * 占位符解析器
		 */
		private final PlaceholderResolver resolver;

		public PlaceholderResolvingStringValueResolver(Properties props) {
			this.helper = new PropertyPlaceholderHelper(
					placeholderPrefix, placeholderSuffix, valueSeparator, ignoreUnresolvablePlaceholders);
			this.resolver = new PropertyPlaceholderConfigurerResolver(props);
		}

		@Override
		@Nullable
		public String resolveStringValue(String strVal) throws BeansException {
			// 使用 PropertyPlaceholderHelper 实例替换属性占位符。
			String resolved = this.helper.replacePlaceholders(strVal, this.resolver);
			// 如果设置了 trimValues 标志，则去除解析后的值两端的空白字符。
			if (trimValues) {
				resolved = resolved.trim();
			}
			// 如果解析后的值等于 nullValue，则返回 null，否则返回解析后的值。
			return (resolved.equals(nullValue) ? null : resolved);
		}
	}


	private final class PropertyPlaceholderConfigurerResolver implements PlaceholderResolver {

		/**
		 * 属性值
		 */
		private final Properties props;

		private PropertyPlaceholderConfigurerResolver(Properties props) {
			this.props = props;
		}

		/**
		 * 解析占位符
		 *
		 * @param placeholderName 要解析的占位符的名称
		 * @return 解析后的值
		 */
		@Override
		@Nullable
		public String resolvePlaceholder(String placeholderName) {
			return PropertyPlaceholderConfigurer.this.resolvePlaceholder(placeholderName,
					this.props, systemPropertiesMode);
		}
	}

}
