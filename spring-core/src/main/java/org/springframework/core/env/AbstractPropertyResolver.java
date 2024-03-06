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

package org.springframework.core.env;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 抽象基类，用于根据任何底层源解析属性。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class AbstractPropertyResolver implements ConfigurablePropertyResolver {

	protected final Log logger = LogFactory.getLog(getClass());

	/*
	 * 可配置转换服务，用于执行类型转换。
	 */
	@Nullable
	private volatile ConfigurableConversionService conversionService;

	/*
	 * 非严格属性占位符辅助器，用于处理属性占位符替换。
	 */
	@Nullable
	private PropertyPlaceholderHelper nonStrictHelper;

	/*
	 * 严格属性占位符辅助器，用于处理属性占位符替换，但更严格地执行。
	 */
	@Nullable
	private PropertyPlaceholderHelper strictHelper;

	/*
	 * 是否忽略无法解析的嵌套属性占位符。
	 */
	private boolean ignoreUnresolvableNestedPlaceholders = false;

	/*
	 * 占位符前缀，默认为系统属性工具类定义的占位符前缀。
	 */
	private String placeholderPrefix = SystemPropertyUtils.PLACEHOLDER_PREFIX;

	/*
	 * 占位符后缀，默认为系统属性工具类定义的占位符后缀。
	 */
	private String placeholderSuffix = SystemPropertyUtils.PLACEHOLDER_SUFFIX;

	/*
	 * 值分隔符，用于将多个值分隔开。
	 */
	@Nullable
	private String valueSeparator = SystemPropertyUtils.VALUE_SEPARATOR;

	/*
	 * 必需属性集合，用于存储需要被替换的属性。
	 */
	private final Set<String> requiredProperties = new LinkedHashSet<>();


	@Override
	public ConfigurableConversionService getConversionService() {
		// 需要提供一个独立的 DefaultConversionService，而不是 PropertySourcesPropertyResolver 使用的共享 DefaultConversionService。
		ConfigurableConversionService cs = this.conversionService;
		// 如果 conversionService 为 null，则创建一个新的 DefaultConversionService 实例
		if (cs == null) {
			// 使用双重检查锁定确保线程安全地创建 DefaultConversionService
			synchronized (this) {
				cs = this.conversionService;
				if (cs == null) {
					cs = new DefaultConversionService();
					this.conversionService = cs;
				}
			}
		}
		return cs;
	}

	@Override
	public void setConversionService(ConfigurableConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
	}

	/**
	 * 设置此解析器替换的占位符必须以的前缀。
	 * <p>默认值为 "${"。
	 *
	 * @see org.springframework.util.SystemPropertyUtils#PLACEHOLDER_PREFIX
	 */
	@Override
	public void setPlaceholderPrefix(String placeholderPrefix) {
		Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
		this.placeholderPrefix = placeholderPrefix;
	}

	/**
	 * 设置此解析器替换的占位符必须以的后缀。
	 * <p>默认值为 "}"。
	 *
	 * @see org.springframework.util.SystemPropertyUtils#PLACEHOLDER_SUFFIX
	 */
	@Override
	public void setPlaceholderSuffix(String placeholderSuffix) {
		Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
		this.placeholderSuffix = placeholderSuffix;
	}

	/**
	 * 指定此解析器替换的占位符与其关联的默认值之间的分隔字符，如果不应将此类特殊字符处理为值分隔符，则为{@code null}。
	 * <p>默认值为 ":"。
	 *
	 * @see org.springframework.util.SystemPropertyUtils#VALUE_SEPARATOR
	 */
	@Override
	public void setValueSeparator(@Nullable String valueSeparator) {
		this.valueSeparator = valueSeparator;
	}

	/**
	 * 设置是否在遇到给定属性值中的无法解析的嵌套占位符时抛出异常。 {@code false} 值表示严格解析，即将抛出异常。
	 * {@code true} 值表示无法解析的嵌套占位符应以其未解析的 ${...} 形式传递。
	 * <p>默认值为 {@code false}。
	 *
	 * @since 3.2
	 */
	@Override
	public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {
		this.ignoreUnresolvableNestedPlaceholders = ignoreUnresolvableNestedPlaceholders;
	}

	@Override
	public void setRequiredProperties(String... requiredProperties) {
		Collections.addAll(this.requiredProperties, requiredProperties);
	}

	@Override
	public void validateRequiredProperties() {
		// 创建 MissingRequiredPropertiesException 实例
		MissingRequiredPropertiesException ex = new MissingRequiredPropertiesException();
		// 检查每个必需的属性是否存在，如果不存在，则将其添加到异常中
		for (String key : this.requiredProperties) {
			if (this.getProperty(key) == null) {
				ex.addMissingRequiredProperty(key);
			}
		}
		// 如果存在缺失的必需属性，则抛出异常
		if (!ex.getMissingRequiredProperties().isEmpty()) {
			throw ex;
		}
	}

	@Override
	public boolean containsProperty(String key) {
		return (getProperty(key) != null);
	}

	@Override
	@Nullable
	public String getProperty(String key) {
		return getProperty(key, String.class);
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		String value = getProperty(key);
		return (value != null ? value : defaultValue);
	}

	@Override
	public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
		T value = getProperty(key, targetType);
		return (value != null ? value : defaultValue);
	}

	@Override
	public String getRequiredProperty(String key) throws IllegalStateException {
		String value = getProperty(key);
		if (value == null) {
			throw new IllegalStateException("Required key '" + key + "' not found");
		}
		return value;
	}

	@Override
	public <T> T getRequiredProperty(String key, Class<T> valueType) throws IllegalStateException {
		T value = getProperty(key, valueType);
		if (value == null) {
			throw new IllegalStateException("Required key '" + key + "' not found");
		}
		return value;
	}

	@Override
	public String resolvePlaceholders(String text) {
		if (this.nonStrictHelper == null) {
			//如果非严格模式助手不存在,则创建一个非严格模式的占位符占位符助手
			this.nonStrictHelper = createPlaceholderHelper(true);
		}
		//使用非严格模式解析占位符
		return doResolvePlaceholders(text, this.nonStrictHelper);
	}

	@Override
	public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
		if (this.strictHelper == null) {
			//如果严格模式助手不存在，创建一个非严格模式的占位符助手
			this.strictHelper = createPlaceholderHelper(false);
		}
		//使用严格模式解析占位符
		return doResolvePlaceholders(text, this.strictHelper);
	}

	/**
	 * 解析给定字符串中的占位符，推迟到 {@link #setIgnoreUnresolvableNestedPlaceholders} 的值，
	 * 以确定任何不可解析的占位符应该引发异常还是被忽略。
	 * <p>从 {@link #getProperty} 及其变体调用，隐式解析嵌套占位符。
	 * 相反，{@link #resolvePlaceholders} 和 {@link #resolveRequiredPlaceholders} 不会委托给此方法，
	 * 而是执行它们自己对不可解析的占位符的处理，如每个方法所指定的。
	 *
	 * @see #setIgnoreUnresolvableNestedPlaceholders
	 * @since 3.2
	 */
	protected String resolveNestedPlaceholders(String value) {
		if (value.isEmpty()) {
			return value;
		}
		return (this.ignoreUnresolvableNestedPlaceholders ?
				//如果忽略或无法解析的嵌套占位符，则调用resolvePlaceholders方法，否则调用resolveRequiredPlaceholders方法
				resolvePlaceholders(value) : resolveRequiredPlaceholders(value));
	}

	private PropertyPlaceholderHelper createPlaceholderHelper(boolean ignoreUnresolvablePlaceholders) {
		return new PropertyPlaceholderHelper(this.placeholderPrefix, this.placeholderSuffix,
				this.valueSeparator, ignoreUnresolvablePlaceholders);
	}

	private String doResolvePlaceholders(String text, PropertyPlaceholderHelper helper) {
		return helper.replacePlaceholders(text, this::getPropertyAsRawString);
	}

	/**
	 * 如有必要，将给定值转换为指定的目标类型。
	 *
	 * @param value      原始属性值
	 * @param targetType 属性检索的指定目标类型
	 * @return 转换后的值，如果不需要转换，则为原始值
	 * @since 4.3.5
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	protected <T> T convertValueIfNecessary(Object value, @Nullable Class<T> targetType) {
		if (targetType == null) {
			//如果目标类型为空，则强转为返回类型
			return (T) value;
		}
		//正在使用的类型转换服务
		ConversionService conversionServiceToUse = this.conversionService;
		if (conversionServiceToUse == null) {
			// 避免初始化共享DefaultConversionService，如果首先不需要标准类型转换...
			if (ClassUtils.isAssignableValue(targetType, value)) {
				//如果value是目标类型的实例，强转
				return (T) value;
			}
			conversionServiceToUse = DefaultConversionService.getSharedInstance();
		}
		return conversionServiceToUse.convert(value, targetType);
	}


	/**
	 * 以原始字符串的形式检索指定的属性，即没有嵌套占位符的解析。
	 *
	 * @param key 要解析的属性名称
	 * @return 属性值，如果找不到则为{@code null}
	 */
	@Nullable
	protected abstract String getPropertyAsRawString(String key);

}
