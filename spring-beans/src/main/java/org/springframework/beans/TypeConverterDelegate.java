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

package org.springframework.beans;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.NumberUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditor;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * 用于将属性值转换为目标类型的内部辅助类。
 *
 * <p>在给定的 {@link PropertyEditorRegistrySupport} 实例上工作。
 * 由 {@link BeanWrapperImpl} 和 {@link SimpleTypeConverter} 使用作为委托。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @see BeanWrapperImpl
 * @see SimpleTypeConverter
 * @since 2.0
 */
class TypeConverterDelegate {

	private static final Log logger = LogFactory.getLog(TypeConverterDelegate.class);

	/**
	 * 属性编辑器注册支持
	 */
	private final PropertyEditorRegistrySupport propertyEditorRegistry;

	/**
	 * 要操作的目标对象
	 */
	@Nullable
	private final Object targetObject;


	/**
	 * 为给定的编辑器注册表创建一个新的 TypeConverterDelegate。
	 *
	 * @param propertyEditorRegistry 要使用的编辑器注册表
	 */
	public TypeConverterDelegate(PropertyEditorRegistrySupport propertyEditorRegistry) {
		this(propertyEditorRegistry, null);
	}

	/**
	 * 为给定的编辑器注册表和 bean 实例创建一个新的 TypeConverterDelegate。
	 *
	 * @param propertyEditorRegistry 要使用的编辑器注册表
	 * @param targetObject           要操作的目标对象（作为可传递给编辑器的上下文）
	 */
	public TypeConverterDelegate(PropertyEditorRegistrySupport propertyEditorRegistry, @Nullable Object targetObject) {
		this.propertyEditorRegistry = propertyEditorRegistry;
		this.targetObject = targetObject;
	}


	/**
	 * 根据指定的属性将值转换为所需的类型。
	 *
	 * @param propertyName 属性名称
	 * @param oldValue     先前的值（如果有）（可以为null）
	 * @param newValue     提议的新值
	 * @param requiredType 必须转换为的类型
	 *                     （如果不知道，则为null，例如在集合元素的情况下）
	 * @return 新值，可能是类型转换的结果
	 * @throws IllegalArgumentException 如果类型转换失败
	 */
	@Nullable
	public <T> T convertIfNecessary(@Nullable String propertyName, @Nullable Object oldValue,
									Object newValue, @Nullable Class<T> requiredType) throws IllegalArgumentException {

		return convertIfNecessary(propertyName, oldValue, newValue, requiredType, TypeDescriptor.valueOf(requiredType));
	}

	/**
	 * 根据指定的属性将值转换为所需的类型（如果必要，从字符串转换）。
	 *
	 * @param propertyName   属性名称
	 * @param oldValue       先前的值（如果有）（可以为{@code null}）
	 * @param newValue       提议的新值
	 * @param requiredType   必须转换为的类型
	 *                       （如果不知道，则为{@code null}，例如在集合元素的情况下）
	 * @param typeDescriptor 目标属性或字段的描述符
	 * @return 新值，可能是类型转换的结果
	 * @throws IllegalArgumentException 如果类型转换失败
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public <T> T convertIfNecessary(@Nullable String propertyName, @Nullable Object oldValue, @Nullable Object newValue,
									@Nullable Class<T> requiredType, @Nullable TypeDescriptor typeDescriptor) throws IllegalArgumentException {

		// 获取给定属性的自定义编辑器。
		PropertyEditor editor = this.propertyEditorRegistry.findCustomEditor(requiredType, propertyName);

		ConversionFailedException conversionAttemptEx = null;

		ConversionService conversionService = this.propertyEditorRegistry.getConversionService();
		if (editor == null && conversionService != null && newValue != null && typeDescriptor != null) {
			// 如果自定义的属性编辑器不存在，但存在自定义 ConversionService，并且存在预估值和目标字段的描述符
			// 获取预估值的属性描述符
			TypeDescriptor sourceTypeDesc = TypeDescriptor.forObject(newValue);
			if (conversionService.canConvert(sourceTypeDesc, typeDescriptor)) {
				// 如果预估值的描述符可以转换为目标字段的描述符
				try {
					// 尝试使用自定义的ConversionService进行转换
					return (T) conversionService.convert(newValue, sourceTypeDesc, typeDescriptor);
				} catch (ConversionFailedException ex) {
					// 回退到使用默认的转换逻辑
					conversionAttemptEx = ex;
				}
			}
		}

		Object convertedValue = newValue;

		if (editor != null || (requiredType != null && !ClassUtils.isAssignableValue(requiredType, convertedValue))) {
			// 如果存在自定义属性编辑器。
			// 或者所需类型存在，并且不是预估值的类型不同
			if (typeDescriptor != null && requiredType != null && Collection.class.isAssignableFrom(requiredType) &&
					convertedValue instanceof String) {
				//如果属性描述符存在，所需类型为数组类型，并且预估值为字符串类型
				// 获取属性描述符的元素类型描述符
				TypeDescriptor elementTypeDesc = typeDescriptor.getElementTypeDescriptor();
				if (elementTypeDesc != null) {
					// 如果元素类型描述符存在，则获取他的类
					Class<?> elementType = elementTypeDesc.getType();
					if (Class.class == elementType || Enum.class.isAssignableFrom(elementType)) {
						// 如果该类是Class类或者是枚举类，则使用StringUtils.commaDelimitedListToStringArray进行转换
						convertedValue = StringUtils.commaDelimitedListToStringArray((String) convertedValue);
					}
				}
			}
			// 如果不存在自定义编辑器，则尝试查找默认属性编辑器。
			if (editor == null) {
				editor = findDefaultEditor(requiredType);
			}
			// 进行转换
			convertedValue = doConvertValue(oldValue, convertedValue, requiredType, editor);
		}

		boolean standardConversion = false;

		if (requiredType != null) {
			// 如果所需类型不为空

			if (convertedValue != null) {
				if (Object.class == requiredType) {
					//如果所需类型为 Object 类型，直接返回
					return (T) convertedValue;
				} else if (requiredType.isArray()) {
					// 如果所需类型是数组类型
					if (convertedValue instanceof String && Enum.class.isAssignableFrom(requiredType.getComponentType())) {
						// 如果转换后的值为字符串类型，并且所需类型为枚举类型，则使用 StringUtils.commaDelimitedListToStringArray 转换类型
						convertedValue = StringUtils.commaDelimitedListToStringArray((String) convertedValue);
					}
					// 转换为数组类型
					return (T) convertToTypedArray(convertedValue, propertyName, requiredType.getComponentType());
				} else if (convertedValue instanceof Collection) {
					// 将集合元素转换为目标类型（如果确定）。
					convertedValue = convertToTypedCollection(
							(Collection<?>) convertedValue, propertyName, requiredType, typeDescriptor);
					// 标准转换
					standardConversion = true;
				} else if (convertedValue instanceof Map) {
					// 将键和值转换为相应的目标类型（如果确定）。
					convertedValue = convertToTypedMap(
							(Map<?, ?>) convertedValue, propertyName, requiredType, typeDescriptor);
					// 标准转换
					standardConversion = true;
				}
				if (convertedValue.getClass().isArray() && Array.getLength(convertedValue) == 1) {
					// 如果是单个元素的数组，获取第一个元素
					convertedValue = Array.get(convertedValue, 0);
					// 标准转换
					standardConversion = true;
				}
				if (String.class == requiredType && ClassUtils.isPrimitiveOrWrapper(convertedValue.getClass())) {
					// 所需类型为字符串类型，并且住转换后的类型是基本类型或者是它的包装类型，直接调用toString方法转换。
					// 可以将任何基本类型值转换为字符串...
					return (T) convertedValue.toString();
				} else if (convertedValue instanceof String && !requiredType.isInstance(convertedValue)) {
					// 如果转换后的类型是字符串类型，并且不是所需类型的实例
					if (conversionAttemptEx == null && !requiredType.isInterface() && !requiredType.isEnum()) {
						// 如果使用自定义的ConversionService转换成功了，并且所需类型不是一个接口类，并且不是枚举类型
						try {
							// 获取所需类型的含有String类型的构造函数
							Constructor<T> strCtor = requiredType.getConstructor(String.class);
							// 通过该构造函数实例化转换后的值
							return BeanUtils.instantiateClass(strCtor, convertedValue);
						} catch (NoSuchMethodException ex) {
							// 继续进行字段查找
							if (logger.isTraceEnabled()) {
								logger.trace("No String constructor found on type [" + requiredType.getName() + "]", ex);
							}
						} catch (Exception ex) {
							if (logger.isDebugEnabled()) {
								logger.debug("Construction via String failed for type [" + requiredType.getName() + "]", ex);
							}
						}
					}
					// 裁剪前后的空格
					String trimmedValue = ((String) convertedValue).trim();
					if (requiredType.isEnum() && trimmedValue.isEmpty()) {
						// 如果所需类型是枚举类型，且裁剪后的字符串为空，则返回null
						// 它是空的枚举标识符：将枚举值重置为 null。
						return null;
					}
					// 尝试将字符转换为枚举类型
					convertedValue = attemptToConvertStringToEnum(requiredType, trimmedValue, convertedValue);
					// 标准转换
					standardConversion = true;
				} else if (convertedValue instanceof Number && Number.class.isAssignableFrom(requiredType)) {
					// 如果转换后的值是数字类型，并且所需类型是Number的子类，使用NumberUtils的convertNumberToTargetClass进行转换
					convertedValue = NumberUtils.convertNumberToTargetClass(
							(Number) convertedValue, (Class<Number>) requiredType);
					// 标准转换
					standardConversion = true;
				}
			} else {
				// convertedValue == null，即仍然没有转换类型成功
				if (requiredType == Optional.class) {
					// 如果所需类型为Optional类型，则返回 Optional.empty()
					convertedValue = Optional.empty();
				}
			}

			// 如果不是所需类型，则抛出 IllegalArgumentException 或 IllegalStateException 异常。
			if (!ClassUtils.isAssignableValue(requiredType, convertedValue)) {
				if (conversionAttemptEx != null) {
					// 之前 ConversionService 调用的原始异常...
					throw conversionAttemptEx;
				} else if (conversionService != null && typeDescriptor != null) {
					// 如果存在自定义的ConversionService，以及存在属性描述符
					// 尚未尝试 ConversionService，可能找到了自定义编辑器，但编辑器无法生成所需类型...
					TypeDescriptor sourceTypeDesc = TypeDescriptor.forObject(newValue);
					if (conversionService.canConvert(sourceTypeDesc, typeDescriptor)) {
						// 如果这两个描述符可以转换
						// 使用自定义的ConversionService进行转化预估值
						return (T) conversionService.convert(newValue, sourceTypeDesc, typeDescriptor);
					}
				}

				// 肯定不匹配：抛出 IllegalArgumentException 或 IllegalStateException 异常
				StringBuilder msg = new StringBuilder();
				msg.append("Cannot convert value of type '").append(ClassUtils.getDescriptiveType(newValue));
				msg.append("' to required type '").append(ClassUtils.getQualifiedName(requiredType)).append('\'');
				if (propertyName != null) {
					msg.append(" for property '").append(propertyName).append('\'');
				}
				if (editor != null) {
					msg.append(": PropertyEditor [").append(editor.getClass().getName()).append(
							"] returned inappropriate value of type '").append(
							ClassUtils.getDescriptiveType(convertedValue)).append('\'');
					throw new IllegalArgumentException(msg.toString());
				} else {
					msg.append(": no matching editors or conversion strategy found");
					throw new IllegalStateException(msg.toString());
				}
			}
		}

		if (conversionAttemptEx != null) {
			// 没有转换成功
			if (editor == null && !standardConversion && requiredType != null && Object.class != requiredType) {
				// 属性编辑器为空，且不是标准转换，并且存在所需类型，且所需类型不是Object类型，则抛出原来的转换异常
				throw conversionAttemptEx;
			}
			logger.debug("Original ConversionService attempt failed - ignored since " +
					"PropertyEditor based conversion eventually succeeded", conversionAttemptEx);
		}

		return (T) convertedValue;
	}

	/**
	 * 尝试将字符串转换为枚举
	 *
	 * @param requiredType          所需类型
	 * @param trimmedValue          裁剪后的字符串
	 * @param currentConvertedValue 当前转换后的值
	 * @return 转换后的值
	 */
	private Object attemptToConvertStringToEnum(Class<?> requiredType, String trimmedValue, Object currentConvertedValue) {
		// 保存当前转换后的值
		Object convertedValue = currentConvertedValue;

		// 如果目标类型是枚举且目标对象不为空，则尝试将修剪后的值解析为枚举常量
		if (Enum.class == requiredType && this.targetObject != null) {
			int index = trimmedValue.lastIndexOf('.');
			if (index > -1) {
				// 获取枚举类名和字段名
				String enumType = trimmedValue.substring(0, index);
				String fieldName = trimmedValue.substring(index + 1);
				ClassLoader cl = this.targetObject.getClass().getClassLoader();
				try {
					// 加载枚举类
					Class<?> enumValueType = ClassUtils.forName(enumType, cl);
					// 获取枚举字段并设置为转换后的值
					Field enumField = enumValueType.getField(fieldName);
					convertedValue = enumField.get(null);
				} catch (ClassNotFoundException ex) {
					if (logger.isTraceEnabled()) {
						logger.trace("Enum class [" + enumType + "] cannot be loaded", ex);
					}
				} catch (Throwable ex) {
					if (logger.isTraceEnabled()) {
						logger.trace("Field [" + fieldName + "] isn't an enum value for type [" + enumType + "]", ex);
					}
				}
			}
		}

		// 如果转换后的值仍然等于之前的值，则尝试通过字段查找作为回退：
		// 对于 JDK 1.5 枚举或定义为静态字段的自定义枚举。仍然需要检查结果值，因此我们不会立即返回它。
		if (convertedValue == currentConvertedValue) {
			try {
				// 尝试通过字段查找枚举值
				Field enumField = requiredType.getField(trimmedValue);
				ReflectionUtils.makeAccessible(enumField);
				convertedValue = enumField.get(null);
			} catch (Throwable ex) {
				if (logger.isTraceEnabled()) {
					logger.trace("Field [" + convertedValue + "] isn't an enum value", ex);
				}
			}
		}

		// 返回转换后的值
		return convertedValue;
	}

	/**
	 * 查找给定类型的默认编辑器。
	 *
	 * @param requiredType 要查找编辑器的类型
	 * @return 相应的编辑器；如果没有则返回 {@code null}
	 */
	@Nullable
	private PropertyEditor findDefaultEditor(@Nullable Class<?> requiredType) {
		PropertyEditor editor = null;
		if (requiredType != null) {
			// 没有自定义编辑器 -> 检查 BeanWrapperImpl 的默认编辑器。
			editor = this.propertyEditorRegistry.getDefaultEditor(requiredType);
			if (editor == null && String.class != requiredType) {
				// 没有 BeanWrapper 默认编辑器 -> 检查标准 JavaBean 编辑器。
				editor = BeanUtils.findEditorByConvention(requiredType);
			}
		}
		return editor;
	}

	/**
	 * 使用给定的属性编辑器将值转换为所需类型（如果必要，从字符串转换）。
	 *
	 * @param oldValue     先前的值，如果可用（可能为 {@code null}）
	 * @param newValue     提议的新值
	 * @param requiredType 我们必须转换为的类型
	 *                     （如果不知道，则为 {@code null}，例如在集合元素的情况下）
	 * @param editor       要使用的 PropertyEditor
	 * @return 新值，可能是类型转换的结果
	 * @throws IllegalArgumentException 如果类型转换失败
	 */
	@Nullable
	private Object doConvertValue(@Nullable Object oldValue, @Nullable Object newValue,
								  @Nullable Class<?> requiredType, @Nullable PropertyEditor editor) {

		// 保存转换后的值到新变量
		Object convertedValue = newValue;

		// 如果存在编辑器，并且转换后的值不是字符串，则使用属性编辑器的 setValue 方法进行设置
		if (editor != null && !(convertedValue instanceof String)) {
			try {
				// 使用编辑器的 setValue 方法
				editor.setValue(convertedValue);
				// 获取新的转换后的值
				Object newConvertedValue = editor.getValue();
				// 如果新的转换后的值不等于之前的值，则更新转换后的值，并将编辑器设置为 null
				if (newConvertedValue != convertedValue) {
					convertedValue = newConvertedValue;
					editor = null;
				}
			} catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("PropertyEditor [" + editor.getClass().getName() + "] does not support setValue call", ex);
				}
				// 忽略异常，继续执行
			}
		}

		// 将转换后的值保存到返回值变量中
		Object returnValue = convertedValue;

		// 如果目标类型不为空且不是数组，并且转换后的值是字符串数组
		if (requiredType != null && !requiredType.isArray() && convertedValue instanceof String[]) {
			if (logger.isTraceEnabled()) {
				logger.trace("Converting String array to comma-delimited String [" + convertedValue + "]");
			}
			// 则将字符串数组转换为逗号分隔的字符串
			convertedValue = StringUtils.arrayToCommaDelimitedString((String[]) convertedValue);
		}

		// 如果转换后的值是字符串，并且编辑器不为空，则使用属性编辑器的 setAsText 方法进行转换
		if (convertedValue instanceof String) {
			if (editor != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Converting String to [" + requiredType + "] using property editor [" + editor + "]");
				}
				String newTextValue = (String) convertedValue;
				// 调用 doConvertTextValue 方法进行转换
				return doConvertTextValue(oldValue, newTextValue, editor);
			} else if (String.class == requiredType) {
				// 如果目标类型是字符串，则直接返回转换后的值
				returnValue = convertedValue;
			}
		}

		// 返回结果值
		return returnValue;
	}

	/**
	 * 使用给定的属性编辑器将给定的文本值转换为值。
	 *
	 * @param oldValue     先前的值，如果可用（可能为 {@code null}）
	 * @param newTextValue 提议的文本值
	 * @param editor       要使用的 PropertyEditor
	 * @return 转换后的值
	 */
	private Object doConvertTextValue(@Nullable Object oldValue, String newTextValue, PropertyEditor editor) {
		try {
			// 尝试设置旧值到编辑器
			editor.setValue(oldValue);
		} catch (Exception ex) {
			// 如果编辑器不支持 setValue 调用，则忽略异常
			if (logger.isDebugEnabled()) {
				logger.debug("PropertyEditor [" + editor.getClass().getName() + "] does not support setValue call", ex);
			}
			// 继续执行
		}

		// 设置新的文本值到编辑器，并返回编辑器的值
		editor.setAsText(newTextValue);
		return editor.getValue();
	}

	/**
	 * 将输入转换为指定类型的数组。
	 *
	 * @param input         要转换的输入
	 * @param propertyName 属性名称（可为 {@code null}）
	 * @param componentType 数组元素的类型
	 * @return 转换后的数组
	 */
	private Object convertToTypedArray(Object input, @Nullable String propertyName, Class<?> componentType) {
		if (input instanceof Collection) {
			// 如果输入是集合，则将集合元素转换为数组元素
			Collection<?> coll = (Collection<?>) input;
			// 创建一个新的数组，用于存储转换后的元素
			Object result = Array.newInstance(componentType, coll.size());
			int i = 0;
			// 遍历集合并转换元素
			for (Iterator<?> it = coll.iterator(); it.hasNext(); i++) {
				// 转换集合中的每个元素
				Object value = convertIfNecessary(
						buildIndexedPropertyName(propertyName, i), null, it.next(), componentType);
				// 将转换后的元素存储到数组中
				Array.set(result, i, value);
			}
			return result;
		} else if (input.getClass().isArray()) {
			// 如果输入是数组，则转换数组元素（如果需要）
			if (componentType.equals(input.getClass().getComponentType()) &&
					!this.propertyEditorRegistry.hasCustomEditorForElement(componentType, propertyName)) {
				return input;
			}
			// 获取数组的长度
			int arrayLength = Array.getLength(input);
			// 创建一个新的数组，用于存储转换后的元素
			Object result = Array.newInstance(componentType, arrayLength);
			// 遍历数组并转换元素
			for (int i = 0; i < arrayLength; i++) {
				// 转换数组中的每个元素
				Object value = convertIfNecessary(
						buildIndexedPropertyName(propertyName, i), null, Array.get(input, i), componentType);
				// 将转换后的元素存储到数组中
				Array.set(result, i, value);
			}
			return result;
		} else {
			// 如果输入是普通值，则将其转换为只包含一个组件的数组
			Object result = Array.newInstance(componentType, 1);
			// 转换输入的值
			Object value = convertIfNecessary(
					buildIndexedPropertyName(propertyName, 0), null, input, componentType);
			// 将转换后的值存储到数组中
			Array.set(result, 0, value);
			return result;
		}
	}

	/**
	 * 将原始集合转换为指定类型的集合。
	 *
	 * @param original        原始集合
	 * @param propertyName   属性名称（可为 {@code null}）
	 * @param requiredType   所需类型
	 * @param typeDescriptor 类型描述符（可为 {@code null}）
	 * @return 转换后的集合
	 */
	@SuppressWarnings("unchecked")
	private Collection<?> convertToTypedCollection(Collection<?> original, @Nullable String propertyName,
												   Class<?> requiredType, @Nullable TypeDescriptor typeDescriptor) {

		if (!Collection.class.isAssignableFrom(requiredType)) {
			// 如果目标类型不是 Collection 类型，则返回原始值
			return original;
		}

		// 检查是否可以创建副本或近似类型的集合
		boolean approximable = CollectionFactory.isApproximableCollectionType(requiredType);
		if (!approximable && !canCreateCopy(requiredType)) {
			// 如果无法创建副本，并且不能创建近似类型的集合，则返回原始值
			if (logger.isDebugEnabled()) {
				logger.debug("Custom Collection type [" + original.getClass().getName() +
						"] does not allow for creating a copy - injecting original Collection as-is");
			}
			return original;
		}

		// 检查原始值是否兼容目标类型
		boolean originalAllowed = requiredType.isInstance(original);
		// 获取集合元素的类型描述符
		TypeDescriptor elementType = (typeDescriptor != null ? typeDescriptor.getElementTypeDescriptor() : null);
		if (elementType == null && originalAllowed &&
				!this.propertyEditorRegistry.hasCustomEditorForElement(null, propertyName)) {
			// 如果原始值兼容目标类型且不存在自定义元素编辑器，则返回原始值
			return original;
		}

		Iterator<?> it;
		try {
			// 获取原始集合的迭代器
			it = original.iterator();
		} catch (Throwable ex) {
			// 捕获可能的异常
			if (logger.isDebugEnabled()) {
				logger.debug("Cannot access Collection of type [" + original.getClass().getName() +
						"] - injecting original Collection as-is: " + ex);
			}
			return original;
		}

		// 创建用于存储转换后集合的变量
		Collection<Object> convertedCopy;
		try {
			// 如果目标类型是近似类型的集合，则使用近似类型的集合工厂创建集合副本
			if (approximable) {
				convertedCopy = CollectionFactory.createApproximateCollection(original, original.size());
			} else {
				// 否则，使用目标类型的可访问构造方法创建集合副本
				convertedCopy = (Collection<Object>)
						ReflectionUtils.accessibleConstructor(requiredType).newInstance();
			}
		} catch (Throwable ex) {
			// 捕获可能的异常
			if (logger.isDebugEnabled()) {
				logger.debug("Cannot create copy of Collection type [" + original.getClass().getName() +
						"] - injecting original Collection as-is: " + ex);
			}
			// 如果出现异常，则返回原始集合
			return original;
		}

		// 遍历原始集合并转换元素
		for (int i = 0; it.hasNext(); i++) {
			Object element = it.next();
			String indexedPropertyName = buildIndexedPropertyName(propertyName, i);
			// 转换集合中的每个元素
			Object convertedElement = convertIfNecessary(indexedPropertyName, null, element,
					(elementType != null ? elementType.getType() : null), elementType);
			try {
				// 将转换后的元素添加到副本集合中
				convertedCopy.add(convertedElement);
			} catch (Throwable ex) {
				// 捕获可能的异常
				if (logger.isDebugEnabled()) {
					logger.debug("Collection type [" + original.getClass().getName() +
							"] seems to be read-only - injecting original Collection as-is: " + ex);
				}
				return original;
			}
			// 检查原始值是否允许转换后的值相等
			originalAllowed = originalAllowed && (element == convertedElement);
		}
		// 如果原始值允许转换后的值相等，则返回原始值，否则返回副本集合
		return (originalAllowed ? original : convertedCopy);
	}

	/**
	 * 将原始映射转换为指定类型的映射。
	 *
	 * @param original        原始映射
	 * @param propertyName   属性名称（可为 {@code null}）
	 * @param requiredType   所需类型
	 * @param typeDescriptor 类型描述符（可为 {@code null}）
	 * @return 转换后的映射
	 */
	@SuppressWarnings("unchecked")
	private Map<?, ?> convertToTypedMap(Map<?, ?> original, @Nullable String propertyName,
										Class<?> requiredType, @Nullable TypeDescriptor typeDescriptor) {

		// 如果目标类型不是 Map 类型，则直接返回原始值
		if (!Map.class.isAssignableFrom(requiredType)) {
			return original;
		}

		// 检查目标类型是否是近似类型的 Map，并且是否可以创建副本
		boolean approximable = CollectionFactory.isApproximableMapType(requiredType);
		if (!approximable && !canCreateCopy(requiredType)) {
			// 如果目标类型不允许创建副本，则直接返回原始值
			if (logger.isDebugEnabled()) {
				logger.debug("Custom Map type [" + original.getClass().getName() +
						"] does not allow for creating a copy - injecting original Map as-is");
			}
			return original;
		}

		// 检查原始值是否是目标类型的实例
		boolean originalAllowed = requiredType.isInstance(original);

		// 获取键和值的类型描述符
		TypeDescriptor keyType = (typeDescriptor != null ? typeDescriptor.getMapKeyTypeDescriptor() : null);
		TypeDescriptor valueType = (typeDescriptor != null ? typeDescriptor.getMapValueTypeDescriptor() : null);

		// 如果键和值的类型都为空，并且原始值是目标类型的实例，并且不存在自定义编辑器，则直接返回原始值
		if (keyType == null && valueType == null && originalAllowed &&
				!this.propertyEditorRegistry.hasCustomEditorForElement(null, propertyName)) {
			return original;
		}

		Iterator<?> it;
		try {
			// 获取原始 Map 的迭代器
			it = original.entrySet().iterator();
		} catch (Throwable ex) {
			// 如果无法访问原始 Map，则返回原始值
			if (logger.isDebugEnabled()) {
				logger.debug("Cannot access Map of type [" + original.getClass().getName() +
						"] - injecting original Map as-is: " + ex);
			}
			return original;
		}

		// 创建用于存储转换后 Map 的变量
		Map<Object, Object> convertedCopy;
		try {
			// 如果目标类型是近似类型的 Map，则使用近似类型的 Map 工厂创建 Map 副本
			if (approximable) {
				convertedCopy = CollectionFactory.createApproximateMap(original, original.size());
			} else {
				// 否则，使用目标类型的可访问构造方法创建 Map 副本
				convertedCopy = (Map<Object, Object>)
						ReflectionUtils.accessibleConstructor(requiredType).newInstance();
			}
		} catch (Throwable ex) {
			// 如果无法创建副本，则返回原始值
			if (logger.isDebugEnabled()) {
				logger.debug("Cannot create copy of Map type [" + original.getClass().getName() +
						"] - injecting original Map as-is: " + ex);
			}
			return original;
		}

		// 遍历原始 Map，将键和值转换后放入转换后的 Map 中
		while (it.hasNext()) {
			// 获取 Map 中的条目，包括键和值
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
			// 获取条目的键
			Object key = entry.getKey();
			// 获取条目的值
			Object value = entry.getValue();
			// 构建键值对应的属性名称，用于类型转换
			String keyedPropertyName = buildKeyedPropertyName(propertyName, key);
			// 转换键的类型
			Object convertedKey = convertIfNecessary(keyedPropertyName, null, key,
					(keyType != null ? keyType.getType() : null), keyType);
			// 转换值的类型
			Object convertedValue = convertIfNecessary(keyedPropertyName, null, value,
					(valueType != null ? valueType.getType() : null), valueType);
			try {
				// 将转换后的键值对放入转换后的 Map 中
				convertedCopy.put(convertedKey, convertedValue);
			} catch (Throwable ex) {
				// 如果无法将键值对放入 Map，则返回原始值
				if (logger.isDebugEnabled()) {
					logger.debug("Map type [" + original.getClass().getName() +
							"] seems to be read-only - injecting original Map as-is: " + ex);
				}
				return original;
			}
			// 检查键和值是否与转换后的键值对相同
			originalAllowed = originalAllowed && (key == convertedKey) && (value == convertedValue);
		}
		// 如果键值对与转换后的键值对相同，则返回原始值；否则返回转换后的 Map
		return (originalAllowed ? original : convertedCopy);
	}

	/**
	 * 构建索引属性名称。
	 *
	 * @param propertyName 属性名称（可为 {@code null}）
	 * @param index        索引
	 * @return 构建的索引属性名称
	 */
	@Nullable
	private String buildIndexedPropertyName(@Nullable String propertyName, int index) {
		return (propertyName != null ?
				propertyName + PropertyAccessor.PROPERTY_KEY_PREFIX + index + PropertyAccessor.PROPERTY_KEY_SUFFIX :
				null);
	}

	/**
	 * 构建键值属性名称。
	 *
	 * @param propertyName 属性名称（可为 {@code null}）
	 * @param key          键值
	 * @return 构建的键值属性名称
	 */
	@Nullable
	private String buildKeyedPropertyName(@Nullable String propertyName, Object key) {
		return (propertyName != null ?
				propertyName + PropertyAccessor.PROPERTY_KEY_PREFIX + key + PropertyAccessor.PROPERTY_KEY_SUFFIX :
				null);
	}

	/**
	 * 检查是否可以创建指定类型的副本。
	 *
	 * @param requiredType 要检查的类型
	 * @return 如果可以创建副本，则为 {@code true}；否则为 {@code false}
	 */
	private boolean canCreateCopy(Class<?> requiredType) {
		return (!requiredType.isInterface() && !Modifier.isAbstract(requiredType.getModifiers()) &&
				Modifier.isPublic(requiredType.getModifiers()) && ClassUtils.hasConstructor(requiredType));
	}

}
