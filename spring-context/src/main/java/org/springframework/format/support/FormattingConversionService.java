/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.DecoratingProxy;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.format.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一个 {@link org.springframework.core.convert.ConversionService} 实现，
 * 旨在配置为 {@link FormatterRegistry}。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public class FormattingConversionService extends GenericConversionService
		implements FormatterRegistry, EmbeddedValueResolverAware {


	/**
	 * 用于解析字符串值的嵌入式值解析器。
	 */
	@Nullable
	private StringValueResolver embeddedValueResolver;

	/**
	 * 存储格式化转换器的映射。
	 * <p> 由格式化转换器的目标类型和要格式化的对象类型索引。
	 */
	private final Map<AnnotationConverterKey, GenericConverter> cachedPrinters = new ConcurrentHashMap<>(64);

	/**
	 * 存储解析器的映射。
	 * <p> 由格式化转换器的目标类型和要解析的字符串类型索引。
	 */
	private final Map<AnnotationConverterKey, GenericConverter> cachedParsers = new ConcurrentHashMap<>(64);


	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}


	@Override
	public void addPrinter(Printer<?> printer) {
		// 获取Printer类型
		Class<?> fieldType = getFieldType(printer, Printer.class);
		// 添加打印器转化器
		addConverter(new PrinterConverter(fieldType, printer, this));
	}

	@Override
	public void addParser(Parser<?> parser) {
		// 获取Parser接口的泛型参数类型
		Class<?> fieldType = getFieldType(parser, Parser.class);
		// 添加文本解析器转换器
		addConverter(new ParserConverter(fieldType, parser, this));
	}

	@Override
	public void addFormatter(Formatter<?> formatter) {
		addFormatterForFieldType(getFieldType(formatter), formatter);
	}

	@Override
	public void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter) {
		// 添加一个 PrinterConverter 对象，用于将 fieldType 类型的值转换为 String 类型
		addConverter(new PrinterConverter(fieldType, formatter, this));
		// 添加一个 ParserConverter 对象，用于将 String 类型的值转换为 fieldType 类型
		addConverter(new ParserConverter(fieldType, formatter, this));
	}

	@Override
	public void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser) {
		// 添加一个 PrinterConverter 对象，用于将 fieldType 类型的值转换为 String 类型
		addConverter(new PrinterConverter(fieldType, printer, this));
		// 添加一个 ParserConverter 对象，用于将 String 类型的值转换为 fieldType 类型
		addConverter(new ParserConverter(fieldType, parser, this));
	}

	@Override
	public void addFormatterForFieldAnnotation(AnnotationFormatterFactory<? extends Annotation> annotationFormatterFactory) {
		// 获取注解格式化工厂的注解类型
		Class<? extends Annotation> annotationType = getAnnotationType(annotationFormatterFactory);
		// 如果存在嵌入值解析器且注解格式化工厂实现了 EmbeddedValueResolverAware 接口，则设置嵌入值解析器
		if (this.embeddedValueResolver != null && annotationFormatterFactory instanceof EmbeddedValueResolverAware) {
			((EmbeddedValueResolverAware) annotationFormatterFactory).setEmbeddedValueResolver(this.embeddedValueResolver);
		}
		// 获取注解格式化工厂支持的字段类型集合
		Set<Class<?>> fieldTypes = annotationFormatterFactory.getFieldTypes();
		// 遍历字段类型集合，为每个字段类型添加注解打印转换器和注解解析转换器
		for (Class<?> fieldType : fieldTypes) {
			// 添加一个注解打印转换器对象，用于将注解类型的值转换为 String 类型
			addConverter(new AnnotationPrinterConverter(annotationType, annotationFormatterFactory, fieldType));
			// 添加一个注解解析转换器对象，用于将 String 类型的值转换为注解类型
			addConverter(new AnnotationParserConverter(annotationType, annotationFormatterFactory, fieldType));
		}
	}


	static Class<?> getFieldType(Formatter<?> formatter) {
		return getFieldType(formatter, Formatter.class);
	}

	private static <T> Class<?> getFieldType(T instance, Class<T> genericInterface) {
		// 解析泛型接口的类型参数
		Class<?> fieldType = GenericTypeResolver.resolveTypeArgument(instance.getClass(), genericInterface);
		// 如果无法解析类型参数且实例是装饰代理对象
		if (fieldType == null && instance instanceof DecoratingProxy) {
			// 从装饰代理对象中解析类型参数
			fieldType = GenericTypeResolver.resolveTypeArgument(
					((DecoratingProxy) instance).getDecoratedClass(), genericInterface);
		}
		// 断言字段类型非空
		Assert.notNull(fieldType, () -> "Unable to extract the parameterized field type from " +
				ClassUtils.getShortName(genericInterface) + " [" + instance.getClass().getName() +
				"]; does the class parameterize the <T> generic type?");
		// 返回字段类型
		return fieldType;
	}

	@SuppressWarnings("unchecked")
	static Class<? extends Annotation> getAnnotationType(AnnotationFormatterFactory<? extends Annotation> factory) {
		// 解析 AnnotationFormatterFactory 的参数化注解类型
		Class<? extends Annotation> annotationType = (Class<? extends Annotation>)
				GenericTypeResolver.resolveTypeArgument(factory.getClass(), AnnotationFormatterFactory.class);
		// 如果注解类型为 null，则抛出异常
		if (annotationType == null) {
			throw new IllegalArgumentException("Unable to extract parameterized Annotation type argument from " +
					"AnnotationFormatterFactory [" + factory.getClass().getName() +
					"]; does the factory parameterize the <A extends Annotation> generic type?");
		}
		return annotationType;
	}


	private static class PrinterConverter implements GenericConverter {
		/**
		 * 字段类型
		 */
		private final Class<?> fieldType;
		/**
		 * 打印器对象类型描述符
		 */
		private final TypeDescriptor printerObjectType;
		/**
		 * 打印器对象
		 */
		@SuppressWarnings("rawtypes")
		private final Printer printer;
		/**
		 * 转换服务
		 */
		private final ConversionService conversionService;

		public PrinterConverter(Class<?> fieldType, Printer<?> printer, ConversionService conversionService) {
			this.fieldType = fieldType;
			this.printerObjectType = TypeDescriptor.valueOf(resolvePrinterObjectType(printer));
			this.printer = printer;
			this.conversionService = conversionService;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(this.fieldType, String.class));
		}

		@Override
		@SuppressWarnings("unchecked")
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			// 如果源类型不可分配给打印对象类型，则进行类型转换
			if (!sourceType.isAssignableTo(this.printerObjectType)) {
				source = this.conversionService.convert(source, sourceType, this.printerObjectType);
			}
			// 如果源为空，则返回空字符串
			if (source == null) {
				return "";
			}
			// 使用本地化上下文的区域设置打印源对象
			return this.printer.print(source, LocaleContextHolder.getLocale());
		}

		@Nullable
		private Class<?> resolvePrinterObjectType(Printer<?> printer) {
			return GenericTypeResolver.resolveTypeArgument(printer.getClass(), Printer.class);
		}

		@Override
		public String toString() {
			return (this.fieldType.getName() + " -> " + String.class.getName() + " : " + this.printer);
		}
	}


	private static class ParserConverter implements GenericConverter {
		/**
		 * 字段类型
		 */
		private final Class<?> fieldType;

		/**
		 * 字符串解析器
		 */
		private final Parser<?> parser;
		/**
		 * 转换服务
		 */
		private final ConversionService conversionService;

		public ParserConverter(Class<?> fieldType, Parser<?> parser, ConversionService conversionService) {
			this.fieldType = fieldType;
			this.parser = parser;
			this.conversionService = conversionService;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(String.class, this.fieldType));
		}

		@Override
		@Nullable
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			// 将源对象转换为字符串类型
			String text = (String) source;
			// 如果文本为空，则返回null
			if (!StringUtils.hasText(text)) {
				return null;
			}
			Object result;
			try {
				// 尝试解析文本，并使用本地化上下文的区域设置
				result = this.parser.parse(text, LocaleContextHolder.getLocale());
			} catch (IllegalArgumentException ex) {
				// 如果解析失败，抛出IllegalArgumentException
				throw ex;
			} catch (Throwable ex) {
				// 如果发生其他异常，则抛出带有详细信息的IllegalArgumentException
				throw new IllegalArgumentException("Parse attempt failed for value [" + text + "]", ex);
			}
			// 获取解析结果的类型描述符
			TypeDescriptor resultType = TypeDescriptor.valueOf(result.getClass());
			// 如果解析结果类型不可分配给目标类型，则进行类型转换
			if (!resultType.isAssignableTo(targetType)) {
				result = this.conversionService.convert(result, resultType, targetType);
			}
			// 返回解析结果
			return result;
		}

		@Override
		public String toString() {
			return (String.class.getName() + " -> " + this.fieldType.getName() + ": " + this.parser);
		}
	}


	private class AnnotationPrinterConverter implements ConditionalGenericConverter {
		/**
		 * 注解类型
		 */
		private final Class<? extends Annotation> annotationType;
		/**
		 *注解格式化工厂
		 */
		@SuppressWarnings("rawtypes")
		private final AnnotationFormatterFactory annotationFormatterFactory;
		/**
		 * 字段类型
		 */
		private final Class<?> fieldType;

		public AnnotationPrinterConverter(Class<? extends Annotation> annotationType,
										  AnnotationFormatterFactory<?> annotationFormatterFactory, Class<?> fieldType) {

			this.annotationType = annotationType;
			this.annotationFormatterFactory = annotationFormatterFactory;
			this.fieldType = fieldType;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(this.fieldType, String.class));
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			return sourceType.hasAnnotation(this.annotationType);
		}

		@Override
		@SuppressWarnings("unchecked")
		@Nullable
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			// 获取源类型上的注解
			Annotation ann = sourceType.getAnnotation(this.annotationType);
			// 如果注解为null，则抛出异常
			if (ann == null) {
				throw new IllegalStateException(
						"Expected [" + this.annotationType.getName() + "] to be present on " + sourceType);
			}
			// 创建AnnotationConverterKey以查找缓存的打印器
			AnnotationConverterKey converterKey = new AnnotationConverterKey(ann, sourceType.getObjectType());
			// 从缓存中获取转换器
			GenericConverter converter = cachedPrinters.get(converterKey);
			// 如果转换器为null，则创建新的转换器并放入缓存中
			if (converter == null) {
				// 获取打印器
				Printer<?> printer = this.annotationFormatterFactory.getPrinter(
						converterKey.getAnnotation(), converterKey.getFieldType());
				// 创建新的转换器
				converter = new PrinterConverter(this.fieldType, printer, FormattingConversionService.this);
				// 将新转换器放入缓存中
				cachedPrinters.put(converterKey, converter);
			}
			// 使用转换器转换源对象
			return converter.convert(source, sourceType, targetType);
		}

		@Override
		public String toString() {
			return ("@" + this.annotationType.getName() + " " + this.fieldType.getName() + " -> " +
					String.class.getName() + ": " + this.annotationFormatterFactory);
		}
	}


	private class AnnotationParserConverter implements ConditionalGenericConverter {
		/**
		 * 注解类型
		 */
		private final Class<? extends Annotation> annotationType;
		/**
		 * 注解格式化工厂
		 */
		@SuppressWarnings("rawtypes")
		private final AnnotationFormatterFactory annotationFormatterFactory;
		/**
		 * 字段类型
		 */
		private final Class<?> fieldType;

		public AnnotationParserConverter(Class<? extends Annotation> annotationType,
										 AnnotationFormatterFactory<?> annotationFormatterFactory, Class<?> fieldType) {

			this.annotationType = annotationType;
			this.annotationFormatterFactory = annotationFormatterFactory;
			this.fieldType = fieldType;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(new ConvertiblePair(String.class, this.fieldType));
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			return targetType.hasAnnotation(this.annotationType);
		}

		@Override
		@SuppressWarnings("unchecked")
		@Nullable
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			// 获取目标类型上的注解
			Annotation ann = targetType.getAnnotation(this.annotationType);
			// 如果注解为null，则抛出异常
			if (ann == null) {
				throw new IllegalStateException(
						"Expected [" + this.annotationType.getName() + "] to be present on " + targetType);
			}
			// 创建AnnotationConverterKey以查找缓存的解析器
			AnnotationConverterKey converterKey = new AnnotationConverterKey(ann, targetType.getObjectType());
			// 从缓存中获取转换器
			GenericConverter converter = cachedParsers.get(converterKey);
			// 如果转换器为null，则创建新的转换器并放入缓存中
			if (converter == null) {
				// 获取解析器
				Parser<?> parser = this.annotationFormatterFactory.getParser(
						converterKey.getAnnotation(), converterKey.getFieldType());
				// 创建新的转换器
				converter = new ParserConverter(this.fieldType, parser, FormattingConversionService.this);
				// 将新转换器放入缓存中
				cachedParsers.put(converterKey, converter);
			}
			// 使用转换器转换源对象
			return converter.convert(source, sourceType, targetType);
		}

		@Override
		public String toString() {
			return (String.class.getName() + " -> @" + this.annotationType.getName() + " " +
					this.fieldType.getName() + ": " + this.annotationFormatterFactory);
		}
	}


	private static class AnnotationConverterKey {
		/**
		 * 注解
		 */
		private final Annotation annotation;

		/**
		 * 字段类型
		 */
		private final Class<?> fieldType;

		public AnnotationConverterKey(Annotation annotation, Class<?> fieldType) {
			this.annotation = annotation;
			this.fieldType = fieldType;
		}

		public Annotation getAnnotation() {
			return this.annotation;
		}

		public Class<?> getFieldType() {
			return this.fieldType;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof AnnotationConverterKey)) {
				return false;
			}
			AnnotationConverterKey otherKey = (AnnotationConverterKey) other;
			return (this.fieldType == otherKey.fieldType && this.annotation.equals(otherKey.annotation));
		}

		@Override
		public int hashCode() {
			return (this.fieldType.hashCode() * 29 + this.annotation.hashCode());
		}
	}

}
