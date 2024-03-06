/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.lang.Nullable;

/**
 * 配置接口，大多数（如果不是全部）{@link PropertyResolver} 类型都要实现该接口。
 * 提供访问和定制 {@link org.springframework.core.convert.ConversionService ConversionService} 的功能，
 * 用于在不同类型之间进行属性值转换。
 *
 * @author Chris Beams
 * @since 3.1
 */
public interface ConfigurablePropertyResolver extends PropertyResolver {

	/**
	 * 返回在属性类型转换时使用的 {@link ConfigurableConversionService}。
	 * <p>返回的可配置转换服务的特性允许方便地添加和移除单个 {@code Converter} 实例：
	 * <pre class="code">
	 * ConfigurableConversionService cs = env.getConversionService();
	 * cs.addConverter(new FooConverter());
	 * </pre>
	 *
	 * @return 用于属性类型转换的 {@link ConfigurableConversionService}
	 * @see PropertyResolver#getProperty(String, Class)
	 * @see org.springframework.core.convert.converter.ConverterRegistry#addConverter
	 */
	ConfigurableConversionService getConversionService();

	/**
	 * 设置在属性类型转换时使用的 {@link ConfigurableConversionService}。
	 * <p><strong>注意：</strong>作为替代完全替换 {@code ConversionService}，
	 * 可以考虑通过进入 {@link #getConversionService()} 并调用 {@code #addConverter} 等方法来添加或删除单个 {@code Converter} 实例。
	 *
	 * @param conversionService 要使用的 {@link ConfigurableConversionService}
	 * @see PropertyResolver#getProperty(String, Class)
	 * @see #getConversionService()
	 * @see org.springframework.core.convert.converter.ConverterRegistry#addConverter
	 */
	void setConversionService(ConfigurableConversionService conversionService);

	/**
	 * 设置此解析器替换的占位符必须以何种前缀开头。
	 *
	 * @param placeholderPrefix 占位符前缀
	 */
	void setPlaceholderPrefix(String placeholderPrefix);

	/**
	 * 设置此解析器替换的占位符必须以何种后缀结尾。
	 *
	 * @param placeholderSuffix 占位符后缀
	 */
	void setPlaceholderSuffix(String placeholderSuffix);

	/**
	 * 指定占位符解析器替换的占位符与其关联的默认值之间的分隔字符，
	 * 如果不应处理此类特殊字符作为值分隔符，则为 {@code null}。
	 *
	 * @param valueSeparator 分隔字符
	 */
	void setValueSeparator(@Nullable String valueSeparator);

	/**
	 * 设置当在给定属性的值中遇到无法解析的占位符时是否抛出异常。
	 * {@code false} 表示严格解析，即将抛出异常。
	 * {@code true} 表示无法解析的嵌套占位符应以未解析的 ${...} 形式传递。
	 * <p>{@link #getProperty(String)} 及其变体的实现必须检查此处设置的值，以确定当属性值包含无法解析的占位符时的正确行为。
	 *
	 * @param ignoreUnresolvableNestedPlaceholders 是否忽略无法解析的嵌套占位符
	 * @since 3.2
	 */
	void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders);

	/**
	 * 指定必须存在的属性，由 {@link #validateRequiredProperties()} 验证。
	 *
	 * @param requiredProperties 必须存在的属性数组
	 */
	void setRequiredProperties(String... requiredProperties);

	/**
	 * 验证由 {@link #setRequiredProperties} 指定的每个属性是否存在并解析为非 {@code null} 值。
	 *
	 * @throws MissingRequiredPropertiesException 如果任何必需属性不可解析。
	 */
	void validateRequiredProperties() throws MissingRequiredPropertiesException;

}
