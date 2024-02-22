/*
 * Copyright 2002-2017 the original author or authors.
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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.format.*;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

import java.util.Set;

/**
 * 提供方便访问配置了针对常见类型（如数字和日期时间）的转换器和格式化器的 FormattingConversionService 的工厂。
 *
 * <p>额外的转换器和格式化器可以通过 {@link #setConverters(Set)} 和 {@link #setFormatters(Set)} 声明性地注册。
 * 另一种选择是通过实现 {@link FormatterRegistrar} 接口在代码中注册转换器和格式化器。
 * 您可以通过 {@link #setFormatterRegistrars(Set)} 配置提供要使用的注册器集。
 *
 * <p>在代码中注册转换器和格式化器的一个很好的例子是 {@code JodaTimeFormatterRegistrar}，
 * 它注册了许多与日期相关的格式化器和转换器。有关更详细的用例列表，请参阅 {@link #setFormatterRegistrars(Set)}
 *
 * <p>与所有 {@code FactoryBean} 实现一样，当使用 Spring {@code <beans>} XML 配置 Spring 应用程序上下文时，
 * 此类都很适用。当使用 {@link org.springframework.context.annotation.Configuration @Configuration} 类配置容器时，
 * 只需从 {@link org.springframework.context.annotation.Bean @Bean} 方法中实例化、配置并返回适当的
 * {@code FormattingConversionService} 对象即可。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Chris Beams
 * @since 3.0
 */
public class FormattingConversionServiceFactoryBean
		implements FactoryBean<FormattingConversionService>, EmbeddedValueResolverAware, InitializingBean {

	/**
	 * 转换器集合，可以包含实现了 {@link Converter}、{@link ConverterFactory} 或 {@link GenericConverter} 的对象。
	 */
	@Nullable
	private Set<?> converters;

	/**
	 * 格式化器集合，可以包含实现了 {@link Formatter} 或 {@link AnnotationFormatterFactory} 的对象。
	 */
	@Nullable
	private Set<?> formatters;

	/**
	 * 格式化器注册器集合，可以包含实现了 {@link FormatterRegistrar} 接口的对象。
	 */
	@Nullable
	private Set<FormatterRegistrar> formatterRegistrars;

	/**
	 * 是否注册默认格式化器。
	 */
	private boolean registerDefaultFormatters = true;

	/**
	 * 内嵌值解析器，用于解析嵌入式值。
	 */
	@Nullable
	private StringValueResolver embeddedValueResolver;

	/**
	 * 格式化转换服务。
	 */
	@Nullable
	private FormattingConversionService conversionService;


	/**
	 * 配置应添加的自定义转换器对象集合。
	 *
	 * @param converters 任意实现以下接口的实例集合：
	 *                   {@link Converter}，
	 *                   {@link org.springframework.core.convert.converter.ConverterFactory}，
	 *                   {@link org.springframework.core.convert.converter.GenericConverter}
	 */
	public void setConverters(Set<?> converters) {
		this.converters = converters;
	}

	/**
	 * 配置应添加的自定义格式化器对象集合。
	 *
	 * @param formatters {@link Formatter} 或 {@link AnnotationFormatterFactory} 的实例集合
	 */
	public void setFormatters(Set<?> formatters) {
		this.formatters = formatters;
	}

	/**
	 * 配置要调用的 FormatterRegistrar 集合，用于注册转换器和格式化器，除了通过 {@link #setConverters(Set)} 和 {@link #setFormatters(Set)} 声明性地添加的之外。
	 * <p>
	 * FormatterRegistrar 在注册多个相关的转换器和格式化器以支持格式化类别（例如日期格式化）时非常有用。可以从一个位置注册所有需要支持格式化类别的相关类型。
	 * <p>
	 * FormatterRegistrar 还可用于将格式化器注册到不同于其自身 &lt;T&gt; 的特定字段类型下，或者在从打印机/解析器对中注册格式化器时使用。
	 *
	 * @see FormatterRegistry#addFormatterForFieldType(Class, Formatter)
	 * @see FormatterRegistry#addFormatterForFieldType(Class, Printer, Parser)
	 */
	public void setFormatterRegistrars(Set<FormatterRegistrar> formatterRegistrars) {
		this.formatterRegistrars = formatterRegistrars;
	}

	/**
	 * 指示是否应注册默认格式化器。
	 *
	 * 默认情况下，会注册内置的格式化器。此标志可用于关闭注册，并仅依赖于显式注册的格式化器。
	 *
	 * @see #setFormatters(Set)
	 * @see #setFormatterRegistrars(Set)
	 */
	public void setRegisterDefaultFormatters(boolean registerDefaultFormatters) {
		this.registerDefaultFormatters = registerDefaultFormatters;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver embeddedValueResolver) {
		this.embeddedValueResolver = embeddedValueResolver;
	}


	@Override
	public void afterPropertiesSet() {
		// 创建默认格式转换服务并注册转换器和格式化器
		this.conversionService = new DefaultFormattingConversionService(this.embeddedValueResolver, this.registerDefaultFormatters);
		// 使用转换器列表注册转换服务
		ConversionServiceFactory.registerConverters(this.converters, this.conversionService);
		// 使用注册的格式化器注册格式转换服务
		registerFormatters(this.conversionService);
	}

	private void registerFormatters(FormattingConversionService conversionService) {
		// 如果自定义格式化器列表不为空，则遍历注册到格式转换服务中
		if (this.formatters != null) {
			for (Object formatter : this.formatters) {
				if (formatter instanceof Formatter<?>) {
					// 如果格式化器是 Formatter 的实例，则将其添加到格式转换服务中
					conversionService.addFormatter((Formatter<?>) formatter);
				} else if (formatter instanceof AnnotationFormatterFactory<?>) {
					// 如果格式化器是 AnnotationFormatterFactory 的实例，则将其添加为字段注解的格式化器到格式转换服务中
					conversionService.addFormatterForFieldAnnotation((AnnotationFormatterFactory<?>) formatter);
				} else {
					// 如果格式化器既不是 Formatter 的实例也不是 AnnotationFormatterFactory 的实例，则抛出异常
					throw new IllegalArgumentException(
							"Custom formatters must be implementations of Formatter or AnnotationFormatterFactory");
				}
			}
		}
		// 如果格式化器注册器列表不为空，则遍历注册格式化器到格式转换服务中
		if (this.formatterRegistrars != null) {
			for (FormatterRegistrar registrar : this.formatterRegistrars) {
				registrar.registerFormatters(conversionService);
			}
		}
	}


	@Override
	@Nullable
	public FormattingConversionService getObject() {
		return this.conversionService;
	}

	@Override
	public Class<? extends FormattingConversionService> getObjectType() {
		return FormattingConversionService.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
