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

package org.springframework.format.support;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.DateFormatterRegistrar;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.number.NumberFormatAnnotationFormatterFactory;
import org.springframework.format.number.money.CurrencyUnitFormatter;
import org.springframework.format.number.money.Jsr354NumberFormatAnnotationFormatterFactory;
import org.springframework.format.number.money.MonetaryAmountFormatter;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringValueResolver;

/**
 * {@link FormattingConversionService} 的特化版本，默认配置了适用于大多数应用程序的转换器和格式化程序。
 *
 * <p>设计用于直接实例化，但也公开了静态的 {@link #addDefaultFormatters} 实用程序方法，用于临时针对任何 {@code FormatterRegistry} 实例的使用，就像 {@code DefaultConversionService} 公开了自己的 {@link DefaultConversionService#addDefaultConverters addDefaultConverters} 方法一样。
 *
 * <p>根据类路径上的相应 API 的存在自动注册 JSR-354 Money & Currency、JSR-310 Date-Time 和/或 Joda-Time 2.x 的格式化程序。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 */
public class DefaultFormattingConversionService extends FormattingConversionService {
	/**
	 * JSR-354是否存在
	 */
	private static final boolean jsr354Present;
	/**
	 * jodaTime是否存在
	 */
	private static final boolean jodaTimePresent;

	static {
		ClassLoader classLoader = DefaultFormattingConversionService.class.getClassLoader();
		jsr354Present = ClassUtils.isPresent("javax.money.MonetaryAmount", classLoader);
		jodaTimePresent = ClassUtils.isPresent("org.joda.time.YearMonth", classLoader);
	}

	/**
	 * 使用一组 {@linkplain DefaultConversionService#addDefaultConverters 默认转换器} 和
	 * {@linkplain #addDefaultFormatters 默认格式化程序} 创建一个新的 {@code DefaultFormattingConversionService}。
	 */
	public DefaultFormattingConversionService() {
		this(null, true);
	}

	/**
	 * 根据 {@code registerDefaultFormatters} 的值，使用一组 {@linkplain DefaultConversionService#addDefaultConverters 默认转换器} 和
	 * （如果为 true）一组 {@linkplain #addDefaultFormatters 默认格式化程序} 创建一个新的 {@code DefaultFormattingConversionService}。
	 *
	 * @param registerDefaultFormatters 是否注册默认格式化程序
	 */
	public DefaultFormattingConversionService(boolean registerDefaultFormatters) {
		this(null, registerDefaultFormatters);
	}

	/**
	 * 根据 {@code registerDefaultFormatters} 的值，使用一组 {@linkplain DefaultConversionService#addDefaultConverters 默认转换器} 和
	 * （如果为 true）一组 {@linkplain #addDefaultFormatters 默认格式化程序} 创建一个新的 {@code DefaultFormattingConversionService}。
	 *
	 * @param embeddedValueResolver     在调用 {@link #addDefaultFormatters} 之前委托给
	 *                                  {@link #setEmbeddedValueResolver(StringValueResolver)}。
	 * @param registerDefaultFormatters 是否注册默认格式化程序
	 */
	public DefaultFormattingConversionService(
			@Nullable StringValueResolver embeddedValueResolver, boolean registerDefaultFormatters) {

		// 如果嵌入值解析器不为null，则设置嵌入值解析器
		if (embeddedValueResolver != null) {
			setEmbeddedValueResolver(embeddedValueResolver);
		}
		// 将默认转换器添加到此服务中
		DefaultConversionService.addDefaultConverters(this);
		// 如果注册默认格式化程序标志为true，则添加默认格式化程序
		if (registerDefaultFormatters) {
			addDefaultFormatters(this);
		}
	}


	/**
	 * 向大多数环境添加适当的格式化程序：包括数字格式化程序、JSR-354 Money &amp; Currency 格式化程序、JSR-310 Date-Time 和/或 Joda-Time 格式化程序，取决于类路径上是否存在相应的 API。
	 *
	 * @param formatterRegistry 用于注册默认格式化程序的服务
	 */
	@SuppressWarnings("deprecation")
	public static void addDefaultFormatters(FormatterRegistry formatterRegistry) {
		// 默认处理数字值
		formatterRegistry.addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory());

		// 默认处理货币值
		if (jsr354Present) {
			formatterRegistry.addFormatter(new CurrencyUnitFormatter());
			formatterRegistry.addFormatter(new MonetaryAmountFormatter());
			formatterRegistry.addFormatterForFieldAnnotation(new Jsr354NumberFormatAnnotationFormatterFactory());
		}

		// 默认处理日期时间值

		// 仅处理JSR-310特定的日期和时间类型
		new DateTimeFormatterRegistrar().registerFormatters(formatterRegistry);

		if (jodaTimePresent) {
			// 处理Joda特定类型以及Date、Calendar、Long类型
			new org.springframework.format.datetime.joda.JodaTimeFormatterRegistrar().registerFormatters(formatterRegistry);
		} else {
			// 基于常规DateFormat的Date、Calendar、Long转换器
			new DateFormatterRegistrar().registerFormatters(formatterRegistry);
		}
	}

}
