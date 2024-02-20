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

package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.lang.Nullable;

import java.nio.charset.Charset;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

/**
 * DefaultConversionService 的特化版本，通过默认配置适用于大多数环境的转换器。
 *
 * <p>设计为直接实例化，但也公开了静态的 {@link #addDefaultConverters(ConverterRegistry)} 实用方法，
 * 用于针对任何 {@code ConverterRegistry} 实例的临时使用。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
public class DefaultConversionService extends GenericConversionService {

	/**
	 * 默认转换器服务，共享实例
	 */
	@Nullable
	private static volatile DefaultConversionService sharedInstance;


	/**
	 * 使用一组 {@linkplain DefaultConversionService#addDefaultConverters(ConverterRegistry) 默认转换器} 创建一个新的 {@code DefaultConversionService}。
	 */
	public DefaultConversionService() {
		addDefaultConverters(this);
	}


	/**
	 * 返回一个共享的默认 {@code ConversionService} 实例，一旦需要就懒加载地构建它。
	 * <p><b> 注意:</b> 我们强烈建议为定制目的构造单个 {@code ConversionService} 实例。
	 * 此访问器仅表示需要简单类型强制但无法以其他任何方式访问寿命更长的 {@code ConversionService} 实例的代码路径的后备。
	 *
	 * @return 共享的{@code ConversionService} 实例 (从不为 {@code null})
	 * @since 4.3.5
	 */
	public static ConversionService getSharedInstance() {
		DefaultConversionService cs = sharedInstance;
		//使用双重检测初始化共享实例
		if (cs == null) {
			synchronized (DefaultConversionService.class) {
				cs = sharedInstance;
				if (cs == null) {
					cs = new DefaultConversionService();
					sharedInstance = cs;
				}
			}
		}
		return cs;
	}

	/**
	 * 向大多数环境添加适当的转换器。
	 *
	 * @param converterRegistry 要添加到的转换器注册表
	 *                          (必须也可转换为 ConversionService，例如是 {@link ConfigurableConversionService})
	 * @throws ClassCastException 如果给定的 ConverterRegistry 无法转换为 ConversionService
	 */
	public static void addDefaultConverters(ConverterRegistry converterRegistry) {
		addScalarConverters(converterRegistry);
		addCollectionConverters(converterRegistry);

		converterRegistry.addConverter(new ByteBufferConverter((ConversionService) converterRegistry));
		converterRegistry.addConverter(new StringToTimeZoneConverter());
		converterRegistry.addConverter(new ZoneIdToTimeZoneConverter());
		converterRegistry.addConverter(new ZonedDateTimeToCalendarConverter());

		converterRegistry.addConverter(new ObjectToObjectConverter());
		converterRegistry.addConverter(new IdToEntityConverter((ConversionService) converterRegistry));
		converterRegistry.addConverter(new FallbackObjectToStringConverter());
		converterRegistry.addConverter(new ObjectToOptionalConverter((ConversionService) converterRegistry));
	}

	/**
	 * 添加常见的集合转换器。
	 *
	 * @param converterRegistry 要添加到的转换器注册表
	 *                          (必须也可转换为 ConversionService，例如是 {@link ConfigurableConversionService})
	 * @throws ClassCastException 如果给定的 ConverterRegistry 无法转换为 ConversionService
	 * @since 4.2.3
	 */
	public static void addCollectionConverters(ConverterRegistry converterRegistry) {
		ConversionService conversionService = (ConversionService) converterRegistry;

		converterRegistry.addConverter(new ArrayToCollectionConverter(conversionService));
		converterRegistry.addConverter(new CollectionToArrayConverter(conversionService));

		converterRegistry.addConverter(new ArrayToArrayConverter(conversionService));
		converterRegistry.addConverter(new CollectionToCollectionConverter(conversionService));
		converterRegistry.addConverter(new MapToMapConverter(conversionService));

		converterRegistry.addConverter(new ArrayToStringConverter(conversionService));
		converterRegistry.addConverter(new StringToArrayConverter(conversionService));

		converterRegistry.addConverter(new ArrayToObjectConverter(conversionService));
		converterRegistry.addConverter(new ObjectToArrayConverter(conversionService));

		converterRegistry.addConverter(new CollectionToStringConverter(conversionService));
		converterRegistry.addConverter(new StringToCollectionConverter(conversionService));

		converterRegistry.addConverter(new CollectionToObjectConverter(conversionService));
		converterRegistry.addConverter(new ObjectToCollectionConverter(conversionService));

		converterRegistry.addConverter(new StreamConverter(conversionService));
	}

	/**
	 * 添加标量转换器。
	 *
	 * @param converterRegistry 要添加到的转换器注册表
	 *                          (必须也可转换为 ConversionService，例如是 {@link ConfigurableConversionService})
	 * @throws ClassCastException 如果给定的 ConverterRegistry 无法转换为 ConversionService
	 */
	private static void addScalarConverters(ConverterRegistry converterRegistry) {
		converterRegistry.addConverterFactory(new NumberToNumberConverterFactory());

		converterRegistry.addConverterFactory(new StringToNumberConverterFactory());
		converterRegistry.addConverter(Number.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToCharacterConverter());
		converterRegistry.addConverter(Character.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new NumberToCharacterConverter());
		converterRegistry.addConverterFactory(new CharacterToNumberFactory());

		converterRegistry.addConverter(new StringToBooleanConverter());
		converterRegistry.addConverter(Boolean.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverterFactory(new StringToEnumConverterFactory());
		converterRegistry.addConverter(new EnumToStringConverter((ConversionService) converterRegistry));

		converterRegistry.addConverterFactory(new IntegerToEnumConverterFactory());
		converterRegistry.addConverter(new EnumToIntegerConverter((ConversionService) converterRegistry));

		converterRegistry.addConverter(new StringToLocaleConverter());
		converterRegistry.addConverter(Locale.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToCharsetConverter());
		converterRegistry.addConverter(Charset.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToCurrencyConverter());
		converterRegistry.addConverter(Currency.class, String.class, new ObjectToStringConverter());

		converterRegistry.addConverter(new StringToPropertiesConverter());
		converterRegistry.addConverter(new PropertiesToStringConverter());

		converterRegistry.addConverter(new StringToUUIDConverter());
		converterRegistry.addConverter(UUID.class, String.class, new ObjectToStringConverter());
	}

}
