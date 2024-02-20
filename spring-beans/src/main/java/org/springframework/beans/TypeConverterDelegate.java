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

	private Object attemptToConvertStringToEnum(Class<?> requiredType, String trimmedValue, Object currentConvertedValue) {
		Object convertedValue = currentConvertedValue;

		if (Enum.class == requiredType && this.targetObject != null) {
			// target type is declared as raw enum, treat the trimmed value as <enum.fqn>.FIELD_NAME
			int index = trimmedValue.lastIndexOf('.');
			if (index > -1) {
				String enumType = trimmedValue.substring(0, index);
				String fieldName = trimmedValue.substring(index + 1);
				ClassLoader cl = this.targetObject.getClass().getClassLoader();
				try {
					Class<?> enumValueType = ClassUtils.forName(enumType, cl);
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

		if (convertedValue == currentConvertedValue) {
			// Try field lookup as fallback: for JDK 1.5 enum or custom enum
			// with values defined as static fields. Resulting value still needs
			// to be checked, hence we don't return it right away.
			try {
				Field enumField = requiredType.getField(trimmedValue);
				ReflectionUtils.makeAccessible(enumField);
				convertedValue = enumField.get(null);
			} catch (Throwable ex) {
				if (logger.isTraceEnabled()) {
					logger.trace("Field [" + convertedValue + "] isn't an enum value", ex);
				}
			}
		}

		return convertedValue;
	}

	/**
	 * Find a default editor for the given type.
	 *
	 * @param requiredType the type to find an editor for
	 * @return the corresponding editor, or {@code null} if none
	 */
	@Nullable
	private PropertyEditor findDefaultEditor(@Nullable Class<?> requiredType) {
		PropertyEditor editor = null;
		if (requiredType != null) {
			// No custom editor -> check BeanWrapperImpl's default editors.
			editor = this.propertyEditorRegistry.getDefaultEditor(requiredType);
			if (editor == null && String.class != requiredType) {
				// No BeanWrapper default editor -> check standard JavaBean editor.
				editor = BeanUtils.findEditorByConvention(requiredType);
			}
		}
		return editor;
	}

	/**
	 * Convert the value to the required type (if necessary from a String),
	 * using the given property editor.
	 *
	 * @param oldValue     the previous value, if available (may be {@code null})
	 * @param newValue     the proposed new value
	 * @param requiredType the type we must convert to
	 *                     (or {@code null} if not known, for example in case of a collection element)
	 * @param editor       the PropertyEditor to use
	 * @return the new value, possibly the result of type conversion
	 * @throws IllegalArgumentException if type conversion failed
	 */
	@Nullable
	private Object doConvertValue(@Nullable Object oldValue, @Nullable Object newValue,
								  @Nullable Class<?> requiredType, @Nullable PropertyEditor editor) {

		Object convertedValue = newValue;

		if (editor != null && !(convertedValue instanceof String)) {
			// Not a String -> use PropertyEditor's setValue.
			// With standard PropertyEditors, this will return the very same object;
			// we just want to allow special PropertyEditors to override setValue
			// for type conversion from non-String values to the required type.
			try {
				editor.setValue(convertedValue);
				Object newConvertedValue = editor.getValue();
				if (newConvertedValue != convertedValue) {
					convertedValue = newConvertedValue;
					// Reset PropertyEditor: It already did a proper conversion.
					// Don't use it again for a setAsText call.
					editor = null;
				}
			} catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("PropertyEditor [" + editor.getClass().getName() + "] does not support setValue call", ex);
				}
				// Swallow and proceed.
			}
		}

		Object returnValue = convertedValue;

		if (requiredType != null && !requiredType.isArray() && convertedValue instanceof String[]) {
			// Convert String array to a comma-separated String.
			// Only applies if no PropertyEditor converted the String array before.
			// The CSV String will be passed into a PropertyEditor's setAsText method, if any.
			if (logger.isTraceEnabled()) {
				logger.trace("Converting String array to comma-delimited String [" + convertedValue + "]");
			}
			convertedValue = StringUtils.arrayToCommaDelimitedString((String[]) convertedValue);
		}

		if (convertedValue instanceof String) {
			if (editor != null) {
				// Use PropertyEditor's setAsText in case of a String value.
				if (logger.isTraceEnabled()) {
					logger.trace("Converting String to [" + requiredType + "] using property editor [" + editor + "]");
				}
				String newTextValue = (String) convertedValue;
				return doConvertTextValue(oldValue, newTextValue, editor);
			} else if (String.class == requiredType) {
				returnValue = convertedValue;
			}
		}

		return returnValue;
	}

	/**
	 * Convert the given text value using the given property editor.
	 *
	 * @param oldValue     the previous value, if available (may be {@code null})
	 * @param newTextValue the proposed text value
	 * @param editor       the PropertyEditor to use
	 * @return the converted value
	 */
	private Object doConvertTextValue(@Nullable Object oldValue, String newTextValue, PropertyEditor editor) {
		try {
			editor.setValue(oldValue);
		} catch (Exception ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("PropertyEditor [" + editor.getClass().getName() + "] does not support setValue call", ex);
			}
			// Swallow and proceed.
		}
		editor.setAsText(newTextValue);
		return editor.getValue();
	}

	private Object convertToTypedArray(Object input, @Nullable String propertyName, Class<?> componentType) {
		if (input instanceof Collection) {
			// Convert Collection elements to array elements.
			Collection<?> coll = (Collection<?>) input;
			Object result = Array.newInstance(componentType, coll.size());
			int i = 0;
			for (Iterator<?> it = coll.iterator(); it.hasNext(); i++) {
				Object value = convertIfNecessary(
						buildIndexedPropertyName(propertyName, i), null, it.next(), componentType);
				Array.set(result, i, value);
			}
			return result;
		} else if (input.getClass().isArray()) {
			// Convert array elements, if necessary.
			if (componentType.equals(input.getClass().getComponentType()) &&
					!this.propertyEditorRegistry.hasCustomEditorForElement(componentType, propertyName)) {
				return input;
			}
			int arrayLength = Array.getLength(input);
			Object result = Array.newInstance(componentType, arrayLength);
			for (int i = 0; i < arrayLength; i++) {
				Object value = convertIfNecessary(
						buildIndexedPropertyName(propertyName, i), null, Array.get(input, i), componentType);
				Array.set(result, i, value);
			}
			return result;
		} else {
			// A plain value: convert it to an array with a single component.
			Object result = Array.newInstance(componentType, 1);
			Object value = convertIfNecessary(
					buildIndexedPropertyName(propertyName, 0), null, input, componentType);
			Array.set(result, 0, value);
			return result;
		}
	}

	@SuppressWarnings("unchecked")
	private Collection<?> convertToTypedCollection(Collection<?> original, @Nullable String propertyName,
												   Class<?> requiredType, @Nullable TypeDescriptor typeDescriptor) {

		if (!Collection.class.isAssignableFrom(requiredType)) {
			return original;
		}

		boolean approximable = CollectionFactory.isApproximableCollectionType(requiredType);
		if (!approximable && !canCreateCopy(requiredType)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Custom Collection type [" + original.getClass().getName() +
						"] does not allow for creating a copy - injecting original Collection as-is");
			}
			return original;
		}

		boolean originalAllowed = requiredType.isInstance(original);
		TypeDescriptor elementType = (typeDescriptor != null ? typeDescriptor.getElementTypeDescriptor() : null);
		if (elementType == null && originalAllowed &&
				!this.propertyEditorRegistry.hasCustomEditorForElement(null, propertyName)) {
			return original;
		}

		Iterator<?> it;
		try {
			it = original.iterator();
		} catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Cannot access Collection of type [" + original.getClass().getName() +
						"] - injecting original Collection as-is: " + ex);
			}
			return original;
		}

		Collection<Object> convertedCopy;
		try {
			if (approximable) {
				convertedCopy = CollectionFactory.createApproximateCollection(original, original.size());
			} else {
				convertedCopy = (Collection<Object>)
						ReflectionUtils.accessibleConstructor(requiredType).newInstance();
			}
		} catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Cannot create copy of Collection type [" + original.getClass().getName() +
						"] - injecting original Collection as-is: " + ex);
			}
			return original;
		}

		for (int i = 0; it.hasNext(); i++) {
			Object element = it.next();
			String indexedPropertyName = buildIndexedPropertyName(propertyName, i);
			Object convertedElement = convertIfNecessary(indexedPropertyName, null, element,
					(elementType != null ? elementType.getType() : null), elementType);
			try {
				convertedCopy.add(convertedElement);
			} catch (Throwable ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Collection type [" + original.getClass().getName() +
							"] seems to be read-only - injecting original Collection as-is: " + ex);
				}
				return original;
			}
			originalAllowed = originalAllowed && (element == convertedElement);
		}
		return (originalAllowed ? original : convertedCopy);
	}

	@SuppressWarnings("unchecked")
	private Map<?, ?> convertToTypedMap(Map<?, ?> original, @Nullable String propertyName,
										Class<?> requiredType, @Nullable TypeDescriptor typeDescriptor) {

		if (!Map.class.isAssignableFrom(requiredType)) {
			return original;
		}

		boolean approximable = CollectionFactory.isApproximableMapType(requiredType);
		if (!approximable && !canCreateCopy(requiredType)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Custom Map type [" + original.getClass().getName() +
						"] does not allow for creating a copy - injecting original Map as-is");
			}
			return original;
		}

		boolean originalAllowed = requiredType.isInstance(original);
		TypeDescriptor keyType = (typeDescriptor != null ? typeDescriptor.getMapKeyTypeDescriptor() : null);
		TypeDescriptor valueType = (typeDescriptor != null ? typeDescriptor.getMapValueTypeDescriptor() : null);
		if (keyType == null && valueType == null && originalAllowed &&
				!this.propertyEditorRegistry.hasCustomEditorForElement(null, propertyName)) {
			return original;
		}

		Iterator<?> it;
		try {
			it = original.entrySet().iterator();
		} catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Cannot access Map of type [" + original.getClass().getName() +
						"] - injecting original Map as-is: " + ex);
			}
			return original;
		}

		Map<Object, Object> convertedCopy;
		try {
			if (approximable) {
				convertedCopy = CollectionFactory.createApproximateMap(original, original.size());
			} else {
				convertedCopy = (Map<Object, Object>)
						ReflectionUtils.accessibleConstructor(requiredType).newInstance();
			}
		} catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Cannot create copy of Map type [" + original.getClass().getName() +
						"] - injecting original Map as-is: " + ex);
			}
			return original;
		}

		while (it.hasNext()) {
			Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
			Object key = entry.getKey();
			Object value = entry.getValue();
			String keyedPropertyName = buildKeyedPropertyName(propertyName, key);
			Object convertedKey = convertIfNecessary(keyedPropertyName, null, key,
					(keyType != null ? keyType.getType() : null), keyType);
			Object convertedValue = convertIfNecessary(keyedPropertyName, null, value,
					(valueType != null ? valueType.getType() : null), valueType);
			try {
				convertedCopy.put(convertedKey, convertedValue);
			} catch (Throwable ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Map type [" + original.getClass().getName() +
							"] seems to be read-only - injecting original Map as-is: " + ex);
				}
				return original;
			}
			originalAllowed = originalAllowed && (key == convertedKey) && (value == convertedValue);
		}
		return (originalAllowed ? original : convertedCopy);
	}

	@Nullable
	private String buildIndexedPropertyName(@Nullable String propertyName, int index) {
		return (propertyName != null ?
				propertyName + PropertyAccessor.PROPERTY_KEY_PREFIX + index + PropertyAccessor.PROPERTY_KEY_SUFFIX :
				null);
	}

	@Nullable
	private String buildKeyedPropertyName(@Nullable String propertyName, Object key) {
		return (propertyName != null ?
				propertyName + PropertyAccessor.PROPERTY_KEY_PREFIX + key + PropertyAccessor.PROPERTY_KEY_SUFFIX :
				null);
	}

	private boolean canCreateCopy(Class<?> requiredType) {
		return (!requiredType.isInterface() && !Modifier.isAbstract(requiredType.getModifiers()) &&
				Modifier.isPublic(requiredType.getModifiers()) && ClassUtils.hasConstructor(requiredType));
	}

}
