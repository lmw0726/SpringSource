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

package org.springframework.core.convert.support;

import org.springframework.core.DecoratingProxy;
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.*;
import org.springframework.core.convert.converter.*;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 适用于大多数环境的基础{@link ConversionService}实现。
 * 通过{@link ConfigurableConversionService}接口间接实现{@link ConverterRegistry}作为注册API。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @author David Haraburda
 * @since 3.0
 */
public class GenericConversionService implements ConfigurableConversionService {

	/**
	 * 不需要转换时使用的通用无操作转换器。
	 */
	private static final GenericConverter NO_OP_CONVERTER = new NoOpConverter("NO_OP");

	/**
	 * 当没有转换器可用时用作缓存条目。此转换器永远不会返回。
	 */
	private static final GenericConverter NO_MATCH = new NoOpConverter("NO_MATCH");

	/**
	 * 转换器管理容器
	 */
	private final Converters converters = new Converters();
	/**
	 * 源类型描述符和目标类型描述符-转换器 缓存
	 */
	private final Map<ConverterCacheKey, GenericConverter> converterCache = new ConcurrentReferenceHashMap<>(64);


	// ConverterRegistry implementation

	@Override
	public void addConverter(Converter<?, ?> converter) {
		// 获取转换器的所需类型信息。
		ResolvableType[] typeInfo = getRequiredTypeInfo(converter.getClass(), Converter.class);
		if (typeInfo == null && converter instanceof DecoratingProxy) {
			// 如果转换器是 DecoratingProxy 的实例，则获取装饰的类的所需类型信息。
			typeInfo = getRequiredTypeInfo(((DecoratingProxy) converter).getDecoratedClass(), Converter.class);
		}
		if (typeInfo == null) {
			// 如果无法确定源类型 <S> 和目标类型 <T>，则抛出异常。
			throw new IllegalArgumentException("Unable to determine source type <S> and target type <T> for your " +
					"Converter [" + converter.getClass().getName() + "]; does the class parameterize those types?");
		}
		// 将转换器添加到转换器适配器中。
		addConverter(new ConverterAdapter(converter, typeInfo[0], typeInfo[1]));
	}

	@Override
	public <S, T> void addConverter(Class<S> sourceType, Class<T> targetType, Converter<? super S, ? extends T> converter) {
		// 使用 ResolvableType 定义源类型和目标类型，将转换器添加到转换器适配器中
		addConverter(new ConverterAdapter(
				converter, ResolvableType.forClass(sourceType), ResolvableType.forClass(targetType)));
	}

	@Override
	public void addConverter(GenericConverter converter) {
		this.converters.add(converter);
		// 清空缓存
		invalidateCache();
	}

	@Override
	public void addConverterFactory(ConverterFactory<?, ?> factory) {
		// 获取工厂类的源类型和目标类型信息
		ResolvableType[] typeInfo = getRequiredTypeInfo(factory.getClass(), ConverterFactory.class);
		// 如果工厂类是装饰代理，则获取装饰类的源类型和目标类型信息
		if (typeInfo == null && factory instanceof DecoratingProxy) {
			typeInfo = getRequiredTypeInfo(((DecoratingProxy) factory).getDecoratedClass(), ConverterFactory.class);
		}
		// 如果类型信息为null，则抛出异常
		if (typeInfo == null) {
			throw new IllegalArgumentException("Unable to determine source type <S> and target type <T> for your " +
					"ConverterFactory [" + factory.getClass().getName() + "]; does the class parameterize those types?");
		}
		// 使用 ResolvableType 定义源类型和目标类型，将工厂类添加到转换器工厂适配器中
		addConverter(new ConverterFactoryAdapter(factory,
				new ConvertiblePair(typeInfo[0].toClass(), typeInfo[1].toClass())));
	}

	@Override
	public void removeConvertible(Class<?> sourceType, Class<?> targetType) {
		this.converters.remove(sourceType, targetType);
		// 清空缓存
		invalidateCache();
	}


	// ConversionService implementation

	@Override
	public boolean canConvert(@Nullable Class<?> sourceType, Class<?> targetType) {
		// 确保目标类型不为null
		Assert.notNull(targetType, "Target type to convert to cannot be null");
		// 调用重载的 canConvert 方法，将源类型和目标类型转换为 TypeDescriptor 后进行判断
		return canConvert((sourceType != null ? TypeDescriptor.valueOf(sourceType) : null),
				TypeDescriptor.valueOf(targetType));
	}

	@Override
	public boolean canConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 确保目标类型不为null
		Assert.notNull(targetType, "Target type to convert to cannot be null");
		// 如果源类型为null，则直接返回true，因为无需转换
		if (sourceType == null) {
			return true;
		}
		// 获取源类型到目标类型的转换器
		GenericConverter converter = getConverter(sourceType, targetType);
		// 如果找到对应的转换器，则返回true，表示可以进行转换；否则返回false
		return (converter != null);
	}

	/**
	 * 返回是否可以跳过源类型和目标类型之间的转换。
	 * <p>更准确地说，如果可以通过返回源对象不变将源类型的对象转换为目标类型，则此方法将返回true。
	 *
	 * @param sourceType 要转换的源类型的上下文（如果源为{@code null}，则可以为{@code null}）
	 * @param targetType 要转换为的目标类型的上下文（必需）
	 * @return {@code true}表示可以跳过转换；否则为{@code false}
	 * @throws IllegalArgumentException 如果 targetType 为 {@code null}
	 * @since 3.2
	 */
	public boolean canBypassConvert(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 确保目标类型不为null
		Assert.notNull(targetType, "Target type to convert to cannot be null");
		// 如果源类型为null，则直接返回true，因为无需转换
		if (sourceType == null) {
			return true;
		}
		// 获取源类型到目标类型的转换器
		GenericConverter converter = getConverter(sourceType, targetType);
		// 如果转换器为NO_OP_CONVERTER，表示不需要进行任何转换，返回true；否则返回false
		return (converter == NO_OP_CONVERTER);
	}

	@Override
	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T convert(@Nullable Object source, Class<T> targetType) {
		Assert.notNull(targetType, "Target type to convert to cannot be null");
		//获取源对象的类型描述符
		TypeDescriptor sourceTypeDescriptor = TypeDescriptor.forObject(source);
		//获取目标类型的类型描述符
		TypeDescriptor targetTypeDescriptor = TypeDescriptor.valueOf(targetType);
		return (T) convert(source, sourceTypeDescriptor, targetTypeDescriptor);
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
		Assert.notNull(targetType, "Target type to convert to cannot be null");
		if (sourceType == null) {
			//如果源对象类型描述符为空，检查源对象是否为空，如果为空，则抛出IllegalArgumentException
			Assert.isTrue(source == null, "Source must be [null] if source type == [null]");
			//委托给handleResult方法处理
			return handleResult(null, targetType, convertNullSource(null, targetType));
		}
		if (source != null && !sourceType.getObjectType().isInstance(source)) {
			//如果源对象不为空，且源对象不是源对象包装器类型的实例，抛出IllegalArgumentException
			throw new IllegalArgumentException("Source to convert from must be an instance of [" +
					sourceType + "]; instead it was a [" + source.getClass().getName() + "]");
		}
		//根据两个类型描述符获取转换器
		GenericConverter converter = getConverter(sourceType, targetType);
		if (converter != null) {
			//如果转换器不为空，委托给ConversionUtils.invokeConverter方法处理
			Object result = ConversionUtils.invokeConverter(converter, source, sourceType, targetType);
			return handleResult(sourceType, targetType, result);
		}
		//处理转换未找到的情况
		return handleConverterNotFound(source, sourceType, targetType);
	}

	/**
	 * 便利操作，用于将源对象转换为指定的目标类型，其中目标类型是提供额外转换上下文的描述符。
	 * 简单委托给{@link #convert(Object, TypeDescriptor, TypeDescriptor)}，并封装了使用{@link TypeDescriptor#forObject(Object)}构造源类型描述符。
	 *
	 * @param source     源对象
	 * @param targetType 目标类型
	 * @return 转换后的值
	 * @throws ConversionException      如果发生转换异常
	 * @throws IllegalArgumentException 如果targetType为{@code null}，
	 *                                  或者sourceType为{@code null}，但source不为{@code null}
	 */
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor targetType) {
		return convert(source, TypeDescriptor.forObject(source), targetType);
	}

	@Override
	public String toString() {
		return this.converters.toString();
	}


	// 受保护的模板方法

	/**
	 * 用于转换 {@code null} 源的模板方法。
	 * <p> 如果目标类型为 {@code Java.util.Optionalempty()}，则默认实现返回 {@code null} 或java 8 {@link java.util.Optional#empty()} 实例。
	 * 子类可能会覆盖此内容，以返回特定目标类型的自定义 {@code null} 对象。
	 *
	 * @param sourceType the source type to convert from
	 * @param targetType the target type to convert to
	 * @return the converted null object
	 */
	@Nullable
	protected Object convertNullSource(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (targetType.getObjectType() == Optional.class) {
			//如果目标类型是Optional类型，返回Optional.empty()
			return Optional.empty();
		}
		//否则返回null
		return null;
	}

	/**
	 * 挂钩方法查找给定sourceType/targetType对的转换器。首先查询这个转换服务的转换器缓存。
	 * 在缓存未命中时，然后对匹配转换器执行详尽搜索。如果没有转换器匹配，则返回默认转换器。
	 *
	 * @param sourceType 要转换的源类型
	 * @param targetType 要转换为的目标类型
	 * @return 将执行转换的通用转换器，或者如果找不到合适的转换器，则 {@code null}
	 * @see #getDefaultConverter(TypeDescriptor, TypeDescriptor)
	 */
	@Nullable
	protected GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
		//将两个类型描述符包装成一个key
		ConverterCacheKey key = new ConverterCacheKey(sourceType, targetType);
		//查找缓存中的转换器
		GenericConverter converter = this.converterCache.get(key);
		if (converter != null) {
			//如果转换器不为空，且不是未匹配到的，返回目标转换器，否则返回空
			//这里运用到了缓存空对象，不是null
			return (converter != NO_MATCH ? converter : null);
		}

		// 从转换器管理容器中寻找转换器
		converter = this.converters.find(sourceType, targetType);
		if (converter == null) {
			// 如果转换器管理容器不存在该转换器，则寻找默认的转换器
			converter = getDefaultConverter(sourceType, targetType);
		}

		if (converter != null) {
			//默认的转换器不为空，加入缓存中
			this.converterCache.put(key, converter);
			return converter;
		}
		//否则添加NO_MATCH的转换器到缓存中
		this.converterCache.put(key, NO_MATCH);
		return null;
	}

	/**
	 * 如果未找到给定sourcetype/targettype对的转换器，则返回默认转换器。
	 * <p> 如果源类型可分配给目标类型，则返回NO_OP转换器。否则返回 {@code null}，指示找不到合适的转换器。
	 *
	 * @param sourceType 源类型
	 * @param targetType 目标类型
	 * @return 将执行转换的默认通用转换器
	 */
	@Nullable
	protected GenericConverter getDefaultConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
		//如果源类型描述符可分配给目标类型描述符，则返回NO_OP转换器。否则返回null
		return (sourceType.isAssignableTo(targetType) ? NO_OP_CONVERTER : null);
	}


	// Internal helpers

	@Nullable
	private ResolvableType[] getRequiredTypeInfo(Class<?> converterClass, Class<?> genericIfc) {
		// 为Converter类获取ResolvableType实例
		ResolvableType resolvableType = ResolvableType.forClass(converterClass).as(genericIfc);
		// 获取ResolvableType实例的泛型信息
		ResolvableType[] generics = resolvableType.getGenerics();
		// 如果泛型信息长度小于2，则返回null，因为Converter需要两个泛型参数：源类型和目标类型
		if (generics.length < 2) {
			return null;
		}
		// 解析源类型和目标类型的Class对象
		Class<?> sourceType = generics[0].resolve();
		Class<?> targetType = generics[1].resolve();
		// 如果源类型或目标类型为null，则返回null
		if (sourceType == null || targetType == null) {
			return null;
		}
		// 返回解析的泛型信息数组
		return generics;
	}

	/**
	 * 清空转换器缓存
	 */
	private void invalidateCache() {
		this.converterCache.clear();
	}

	@Nullable
	private Object handleConverterNotFound(
			@Nullable Object source, @Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {

		if (source == null) {
			//如果源对象为空，目标对象是基本类型，抛出异常，否则返回null
			assertNotPrimitiveTargetType(sourceType, targetType);
			return null;
		}
		if ((sourceType == null || sourceType.isAssignableTo(targetType)) &&
				targetType.getObjectType().isInstance(source)) {
			//如果源类型描述符为空，或者源类型描述符是目标类型描述符的子类且源对象是目标类型的实例，返回目标对象
			return source;
		}
		//抛出无法转换异常
		throw new ConverterNotFoundException(sourceType, targetType);
	}

	@Nullable
	private Object handleResult(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType, @Nullable Object result) {
		if (result == null) {
			//如果转换后的对象为空，且目标类型是原始类型，抛出异常
			assertNotPrimitiveTargetType(sourceType, targetType);
		}
		return result;
	}

	private void assertNotPrimitiveTargetType(@Nullable TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (targetType.isPrimitive()) {
			//如果目标类型是原始类型，则抛出转换失败异常
			throw new ConversionFailedException(sourceType, targetType, null,
					new IllegalArgumentException("A null value cannot be assigned to a primitive type"));
		}
	}


	/**
	 * 将 {@link Converter} 转换为 {@link GenericConverter}。
	 */
	@SuppressWarnings("unchecked")
	private final class ConverterAdapter implements ConditionalGenericConverter {

		/**
		 * 转换器
		 */
		private final Converter<Object, Object> converter;

		/**
		 * 类型信息
		 */
		private final ConvertiblePair typeInfo;

		/**
		 * 目标类型
		 */
		private final ResolvableType targetType;

		public ConverterAdapter(Converter<?, ?> converter, ResolvableType sourceType, ResolvableType targetType) {
			this.converter = (Converter<Object, Object>) converter;
			this.typeInfo = new ConvertiblePair(sourceType.toClass(), targetType.toClass());
			this.targetType = targetType;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(this.typeInfo);
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			// 首先检查原始类型...
			if (this.typeInfo.getTargetType() != targetType.getObjectType()) {
				//如果目标类型与目标类型的包装类型不相等，则返回false
				return false;
			}
			// 需要全面检查复杂的泛型类型匹配？
			//获取类型描述符对应的底层类型
			ResolvableType rt = targetType.getResolvableType();
			if (rt.getType() instanceof Class || rt.isAssignableFrom(this.targetType) ||
					this.targetType.hasUnresolvableGenerics()) {
				//如果底层类型是Class类型，或者目标类型是底层类型的子类，或者是目标类型含有无法解析的泛型
				if (this.converter instanceof ConditionalConverter) {
					if (((ConditionalConverter) this.converter).matches(sourceType, targetType)) {
						return true;
					}
					//类型无法转换，返回false
					return false;
				} else {
					//如果不是ConditionalConverter，则返回true
					return true;
				}
			} else {
				return false;
			}
		}

		@Override
		@Nullable
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null) {
				return convertNullSource(sourceType, targetType);
			}
			return this.converter.convert(source);
		}

		@Override
		public String toString() {
			return (this.typeInfo + " : " + this.converter);
		}
	}


	/**
	 * 将 {@link ConverterFactory} 转换为 {@link GenericConverter}。
	 */
	@SuppressWarnings("unchecked")
	private final class ConverterFactoryAdapter implements ConditionalGenericConverter {

		/**
		 * 转换器工厂
		 */
		private final ConverterFactory<Object, Object> converterFactory;

		/**
		 * 类型信息
		 */
		private final ConvertiblePair typeInfo;

		public ConverterFactoryAdapter(ConverterFactory<?, ?> converterFactory, ConvertiblePair typeInfo) {
			this.converterFactory = (ConverterFactory<Object, Object>) converterFactory;
			this.typeInfo = typeInfo;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(this.typeInfo);
		}

		@Override
		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			// 默认匹配结果为true
			boolean matches = true;
			// 如果ConverterFactory是ConditionalConverter类型，则调用matches方法检查是否匹配
			if (this.converterFactory instanceof ConditionalConverter) {
				matches = ((ConditionalConverter) this.converterFactory).matches(sourceType, targetType);
			}
			// 如果匹配，继续检查Converter
			if (matches) {
				// 获取ConverterFactory中对应目标类型的Converter
				Converter<?, ?> converter = this.converterFactory.getConverter(targetType.getType());
				// 如果Converter也是ConditionalConverter类型，则调用matches方法检查是否匹配
				if (converter instanceof ConditionalConverter) {
					matches = ((ConditionalConverter) converter).matches(sourceType, targetType);
				}
			}
			// 返回最终匹配结果
			return matches;
		}

		@Override
		@Nullable
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null) {
				// 如果源对象为空，则调用convertNullSource方法处理
				return convertNullSource(sourceType, targetType);
			}
			// 否则，使用ConverterFactory获取对应目标类型的Converter进行转换
			return this.converterFactory.getConverter(targetType.getObjectType()).convert(source);
		}

		@Override
		public String toString() {
			return (this.typeInfo + " : " + this.converterFactory);
		}
	}


	/**
	 * 与转换器缓存一起使用的键。
	 */
	private static final class ConverterCacheKey implements Comparable<ConverterCacheKey> {

		/**
		 * 源类型的属性描述符
		 */
		private final TypeDescriptor sourceType;

		/**
		 * 目标类型的属性描述符
		 */
		private final TypeDescriptor targetType;

		public ConverterCacheKey(TypeDescriptor sourceType, TypeDescriptor targetType) {
			this.sourceType = sourceType;
			this.targetType = targetType;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ConverterCacheKey)) {
				return false;
			}
			ConverterCacheKey otherKey = (ConverterCacheKey) other;
			return (this.sourceType.equals(otherKey.sourceType)) &&
					this.targetType.equals(otherKey.targetType);
		}

		@Override
		public int hashCode() {
			return (this.sourceType.hashCode() * 29 + this.targetType.hashCode());
		}

		@Override
		public String toString() {
			return ("ConverterCacheKey [sourceType = " + this.sourceType +
					", targetType = " + this.targetType + "]");
		}

		@Override
		public int compareTo(ConverterCacheKey other) {
			int result = this.sourceType.getResolvableType().toString().compareTo(
					other.sourceType.getResolvableType().toString());
			if (result == 0) {
				result = this.targetType.getResolvableType().toString().compareTo(
						other.targetType.getResolvableType().toString());
			}
			return result;
		}
	}


	/**
	 * 管理向服务注册的所有转换器。
	 */
	private static class Converters {
		/**
		 * 全局的转换器，使用CopyOnWriteArraySet保证并发问题
		 */
		private final Set<GenericConverter> globalConverters = new CopyOnWriteArraySet<>();

		/**
		 * 类型转换器对，与转换器映射
		 */
		private final Map<ConvertiblePair, ConvertersForPair> converters = new ConcurrentHashMap<>(256);

		public void add(GenericConverter converter) {
			Set<ConvertiblePair> convertibleTypes = converter.getConvertibleTypes();
			if (convertibleTypes == null) {
				// 如果可转换类型为null，则必须是ConditionalConverter，否则报错
				Assert.state(converter instanceof ConditionalConverter,
						"Only conditional converters may return null convertible types");
				// 将此类转换器添加到全局转换器中
				this.globalConverters.add(converter);
			} else {
				// 否则，将可转换类型转换成ConvertiblePair并存储
				for (ConvertiblePair convertiblePair : convertibleTypes) {
					getMatchableConverters(convertiblePair).add(converter);
				}
			}
		}

		private ConvertersForPair getMatchableConverters(ConvertiblePair convertiblePair) {
			return this.converters.computeIfAbsent(convertiblePair, k -> new ConvertersForPair());
		}

		public void remove(Class<?> sourceType, Class<?> targetType) {
			this.converters.remove(new ConvertiblePair(sourceType, targetType));
		}

		/**
		 * 找到一个给定源和目标类型的 {@link GenericConverter}。
		 * <p> 此方法将通过遍历类型的类和接口层次结构来尝试匹配所有可能的转换器。
		 *
		 * @param sourceType 源类型
		 * @param targetType 目标类型
		 * @return 匹配到的 {@link GenericConverter}，如果找不到则为 {@code null}
		 */
		@Nullable
		public GenericConverter find(TypeDescriptor sourceType, TypeDescriptor targetType) {
			// 搜索完整类型层次结构
			List<Class<?>> sourceCandidates = getClassHierarchy(sourceType.getType());
			List<Class<?>> targetCandidates = getClassHierarchy(targetType.getType());
			for (Class<?> sourceCandidate : sourceCandidates) {
				for (Class<?> targetCandidate : targetCandidates) {
					//两两配对，组成转换器对
					ConvertiblePair convertiblePair = new ConvertiblePair(sourceCandidate, targetCandidate);
					//获取注册的转换器
					GenericConverter converter = getRegisteredConverter(sourceType, targetType, convertiblePair);
					if (converter != null) {
						//如果找到了，直接返回
						return converter;
					}
				}
			}
			return null;
		}

		@Nullable
		private GenericConverter getRegisteredConverter(TypeDescriptor sourceType,
														TypeDescriptor targetType, ConvertiblePair convertiblePair) {

			// 检查专门注册的转换器
			//获取转换后的类型对
			ConvertersForPair convertersForPair = this.converters.get(convertiblePair);
			if (convertersForPair != null) {
				//通过转换后的类型对来获取转换器
				GenericConverter converter = convertersForPair.getConverter(sourceType, targetType);
				if (converter != null) {
					return converter;
				}
			}
			// 检查ConditionalConverters是否有动态匹配
			for (GenericConverter globalConverter : this.globalConverters) {
				//可以转换，直接返回转换器
				if (((ConditionalConverter) globalConverter).matches(sourceType, targetType)) {
					return globalConverter;
				}
			}
			return null;
		}

		/**
		 * 返回给定类型的有序类层次结构。
		 *
		 * @param type 类型
		 * @return 给定类型扩展或实现的所有类的有序列表
		 */
		private List<Class<?>> getClassHierarchy(Class<?> type) {
			// 用于存储类层次结构的列表，初始容量设置为20
			List<Class<?>> hierarchy = new ArrayList<>(20);
			// 用于跟踪已访问过的类，防止循环引用
			Set<Class<?>> visited = new HashSet<>(20);
			// 将指定类型及其父类逐级添加到类层次结构中
			addToClassHierarchy(0, ClassUtils.resolvePrimitiveIfNecessary(type), false, hierarchy, visited);
			// 检查类型是否为数组
			boolean array = type.isArray();

			int i = 0;
			// 循环直到遍历完整个类层次结构
			while (i < hierarchy.size()) {
				Class<?> candidate = hierarchy.get(i);
				// 如果是数组类型，则获取其组件类型，否则保持不变
				candidate = (array ? candidate.getComponentType() : ClassUtils.resolvePrimitiveIfNecessary(candidate));
				// 获取候选类的父类
				Class<?> superclass = candidate.getSuperclass();
				// 如果候选类有父类且不是Object类或Enum类，则将其父类添加到类层次结构中
				if (superclass != null && superclass != Object.class && superclass != Enum.class) {
					addToClassHierarchy(i + 1, candidate.getSuperclass(), array, hierarchy, visited);
				}
				// 将候选类实现的接口添加到类层次结构中
				addInterfacesToClassHierarchy(candidate, array, hierarchy, visited);
				i++;
			}

			// 如果类型是枚举类型
			if (Enum.class.isAssignableFrom(type)) {
				// 将Enum类及其父类Enum添加到类层次结构中
				addToClassHierarchy(hierarchy.size(), Enum.class, array, hierarchy, visited);
				addToClassHierarchy(hierarchy.size(), Enum.class, false, hierarchy, visited);
				// 将Enum类实现的接口添加到类层次结构中
				addInterfacesToClassHierarchy(Enum.class, array, hierarchy, visited);
			}

			// 将Object类及其父类Object添加到类层次结构中
			addToClassHierarchy(hierarchy.size(), Object.class, array, hierarchy, visited);
			addToClassHierarchy(hierarchy.size(), Object.class, false, hierarchy, visited);
			// 返回类层次结构列表
			return hierarchy;
		}

		/**
		 * 将接口添加到类层次结构中。
		 *
		 * @param type 类型
		 * @param asArray 是否作为数组
		 * @param hierarchy 类层次结构列表
		 * @param visited 已访问的类集合
		 */
		private void addInterfacesToClassHierarchy(Class<?> type, boolean asArray, List<Class<?>> hierarchy, Set<Class<?>> visited) {
			for (Class<?> implementedInterface : type.getInterfaces()) {
				addToClassHierarchy(hierarchy.size(), implementedInterface, asArray, hierarchy, visited);
			}
		}

		/**
		 * 将类添加到类层次结构中。
		 *
		 * @param index 要添加的索引位置
		 * @param type 要添加的类
		 * @param asArray 是否作为数组
		 * @param hierarchy 类层次结构列表
		 * @param visited 已访问的类集合
		 */
		private void addToClassHierarchy(int index, Class<?> type, boolean asArray, List<Class<?>> hierarchy, Set<Class<?>> visited) {
			// 如果需要返回数组类型
			if (asArray) {
				// 创建一个空数组实例并获取其类对象
				type = Array.newInstance(type, 0).getClass();
			}
			// 如果类型尚未被访问过，则将其添加到类层次结构中
			if (visited.add(type)) {
				hierarchy.add(index, type);
			}
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ConversionService converters =\n");
			for (String converterString : getConverterStrings()) {
				builder.append('\t').append(converterString).append('\n');
			}
			return builder.toString();
		}

		/**
		 * 获取转换器字符串列表。
		 *
		 * @return 转换器字符串列表
		 */
		private List<String> getConverterStrings() {
			// 创建一个存储转换器字符串的列表
			List<String> converterStrings = new ArrayList<>();
			// 遍历所有转换器对
			for (ConvertersForPair convertersForPair : this.converters.values()) {
				// 将转换器对的字符串表示添加到列表中
				converterStrings.add(convertersForPair.toString());
			}
			// 对转换器字符串进行排序
			Collections.sort(converterStrings);
			// 返回排序后的转换器字符串列表
			return converterStrings;
		}
	}


	/**
	 * 管理针对特定 {@link ConvertiblePair} 注册的转换器。
	 */
	private static class ConvertersForPair {
		/**
		 * 转换器队列，使用双向链表结构的无界队列，解决并发问题
		 */
		private final Deque<GenericConverter> converters = new ConcurrentLinkedDeque<>();

		public void add(GenericConverter converter) {
			this.converters.addFirst(converter);
		}

		@Nullable
		public GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
			for (GenericConverter converter : this.converters) {
				//如果转换器不是带有条件的类型转换器或者两个类型可以转换，则返回转换器
				boolean isNotConditionalGenericConverter = !(converter instanceof ConditionalGenericConverter);
				boolean canConvert = ((ConditionalGenericConverter) converter).matches(sourceType, targetType);
				if (isNotConditionalGenericConverter || canConvert) {
					return converter;
				}
			}
			return null;
		}

		@Override
		public String toString() {
			return StringUtils.collectionToCommaDelimitedString(this.converters);
		}
	}


	/**
	 * 内部转换器，不执行任何操作。
	 */
	private static class NoOpConverter implements GenericConverter {

		private final String name;

		public NoOpConverter(String name) {
			this.name = name;
		}

		@Override
		@Nullable
		public Set<ConvertiblePair> getConvertibleTypes() {
			return null;
		}

		@Override
		@Nullable
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return source;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

}
