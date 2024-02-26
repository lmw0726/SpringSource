/*
 * Copyright 2002-2021 the original author or authors.
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
import org.springframework.core.ResolvableType;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.*;
import java.security.PrivilegedActionException;
import java.util.*;

/**
 * 一个基本的 {@link ConfigurablePropertyAccessor}，为所有典型用例提供必要的基础设施。
 *
 * <p>此访问器将将集合和数组值转换为相应的目标集合或数组（如果需要）。
 * 处理集合或数组的自定义属性编辑器可以通过PropertyEditor的 {@code setValue}编写，
 * 或者针对逗号分隔的字符串通过 {@code setAsText} 编写，
 * 因为如果数组本身不可分配，则String数组会以这种格式进行转换。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Rod Johnson
 * @author Rob Harrop
 * @see #registerCustomEditor
 * @see #setPropertyValues
 * @see #setPropertyValue
 * @see #getPropertyValue
 * @see #getPropertyType
 * @see BeanWrapper
 * @see PropertyEditorRegistrySupport
 * @since 4.2
 */
public abstract class AbstractNestablePropertyAccessor extends AbstractPropertyAccessor {

	/**
	 * 我们将创建很多这样的对象，所以我们不希望每次都有一个新的记录器。
	 */
	private static final Log logger = LogFactory.getLog(AbstractNestablePropertyAccessor.class);
	/**
	 * 自动增长限额
	 */
	private int autoGrowCollectionLimit = Integer.MAX_VALUE;
	/**
	 * 包装对象
	 */
	@Nullable
	Object wrappedObject;

	/**
	 * 嵌套路径
	 */
	private String nestedPath = "";
	/**
	 * 根对象
	 */
	@Nullable
	Object rootObject;

	/**
	 * 缓存的嵌套访问器映射：嵌套路径 -> 访问器实例。
	 */
	@Nullable
	private Map<String, AbstractNestablePropertyAccessor> nestedPropertyAccessors;


	/**
	 * 创建一个新的空访问器。需要在之后设置包装实例。注册默认编辑器。
	 *
	 * @see #setWrappedInstance
	 */
	protected AbstractNestablePropertyAccessor() {
		this(true);
	}

	/**
	 * 创建一个新的空访问器。需要在之后设置包装实例。
	 *
	 * @param registerDefaultEditors 是否注册默认编辑器
	 *                               （如果访问器不需要任何类型转换，则可以取消注册）
	 * @see #setWrappedInstance
	 */
	protected AbstractNestablePropertyAccessor(boolean registerDefaultEditors) {
		if (registerDefaultEditors) {
			registerDefaultEditors();
		}
		this.typeConverterDelegate = new TypeConverterDelegate(this);
	}

	/**
	 * 为给定对象创建一个新的访问器。
	 *
	 * @param object 由此访问器包装的对象
	 */
	protected AbstractNestablePropertyAccessor(Object object) {
		registerDefaultEditors();
		setWrappedInstance(object);
	}

	/**
	 * 创建一个新的访问器，包装指定类的新实例。
	 *
	 * @param clazz 要实例化和包装的类
	 */
	protected AbstractNestablePropertyAccessor(Class<?> clazz) {
		registerDefaultEditors();
		setWrappedInstance(BeanUtils.instantiateClass(clazz));
	}

	/**
	 * 为给定对象创建一个新的访问器，注册对象所在的嵌套路径。
	 *
	 * @param object     由此访问器包装的对象
	 * @param nestedPath 对象的嵌套路径
	 * @param rootObject 路径顶部的根对象
	 */
	protected AbstractNestablePropertyAccessor(Object object, String nestedPath, Object rootObject) {
		registerDefaultEditors();
		setWrappedInstance(object, nestedPath, rootObject);
	}

	/**
	 * 为给定对象创建一个新的访问器，并注册对象所在的嵌套路径。
	 *
	 * @param object     由此访问器包装的对象
	 * @param nestedPath 对象的嵌套路径
	 * @param parent     包含的访问器（不能为空）
	 */
	protected AbstractNestablePropertyAccessor(Object object, String nestedPath, AbstractNestablePropertyAccessor parent) {
		setWrappedInstance(object, nestedPath, parent.getWrappedInstance());
		setExtractOldValueForEditor(parent.isExtractOldValueForEditor());
		setAutoGrowNestedPaths(parent.isAutoGrowNestedPaths());
		setAutoGrowCollectionLimit(parent.getAutoGrowCollectionLimit());
		setConversionService(parent.getConversionService());
	}


	/**
	 * 指定数组和集合自动增长的限制。
	 * <p>默认情况下，普通访问器的限制是无限制的。
	 */
	public void setAutoGrowCollectionLimit(int autoGrowCollectionLimit) {
		this.autoGrowCollectionLimit = autoGrowCollectionLimit;
	}

	/**
	 * 返回数组和集合自动增长的限制。
	 */
	public int getAutoGrowCollectionLimit() {
		return this.autoGrowCollectionLimit;
	}

	/**
	 * 切换目标对象，仅在新对象的类与替换对象的类不同时替换缓存的内省结果。
	 *
	 * @param object 新的目标对象
	 */
	public void setWrappedInstance(Object object) {
		setWrappedInstance(object, "", null);
	}

	/**
	 * 切换目标对象，仅在新对象的类与替换对象的类不同时替换缓存的内省结果。
	 *
	 * @param object     新的目标对象
	 * @param nestedPath 对象的嵌套路径
	 * @param rootObject 路径顶部的根对象
	 */
	public void setWrappedInstance(Object object, @Nullable String nestedPath, @Nullable Object rootObject) {
		this.wrappedObject = ObjectUtils.unwrapOptional(object);
		Assert.notNull(this.wrappedObject, "Target object must not be null");
		this.nestedPath = (nestedPath != null ? nestedPath : "");
		this.rootObject = (!this.nestedPath.isEmpty() ? rootObject : this.wrappedObject);
		this.nestedPropertyAccessors = null;
		this.typeConverterDelegate = new TypeConverterDelegate(this, this.wrappedObject);
	}

	public final Object getWrappedInstance() {
		Assert.state(this.wrappedObject != null, "No wrapped object");
		return this.wrappedObject;
	}

	public final Class<?> getWrappedClass() {
		return getWrappedInstance().getClass();
	}

	/**
	 * 返回由此访问器包装的对象的嵌套路径。
	 */
	public final String getNestedPath() {
		return this.nestedPath;
	}

	/**
	 * 返回此访问器路径顶部的根对象。
	 *
	 * @see #getNestedPath
	 */
	public final Object getRootInstance() {
		Assert.state(this.rootObject != null, "No root object");
		return this.rootObject;
	}

	/**
	 * 返回此访问器路径顶部根对象的类。
	 *
	 * @see #getNestedPath
	 */
	public final Class<?> getRootClass() {
		return getRootInstance().getClass();
	}


	@Override
	public void setPropertyValue(String propertyName, @Nullable Object value) throws BeansException {
		AbstractNestablePropertyAccessor nestedPa;
		try {
			// 获取属性路径的属性访问器
			nestedPa = getPropertyAccessorForPropertyPath(propertyName);
		} catch (NotReadablePropertyException ex) {
			// 如果属性路径不可读，则抛出不可写属性异常
			throw new NotWritablePropertyException(getRootClass(), this.nestedPath + propertyName,
					"Nested property in path '" + propertyName + "' does not exist", ex);
		}
		// 获取属性路径的属性名称令牌
		PropertyTokenHolder tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
		// 设置属性值
		nestedPa.setPropertyValue(tokens, new PropertyValue(propertyName, value));
	}

	@Override
	public void setPropertyValue(PropertyValue pv) throws BeansException {
		// 获取属性名称令牌
		PropertyTokenHolder tokens = (PropertyTokenHolder) pv.resolvedTokens;
		// 如果令牌为空
		if (tokens == null) {
			// 获取属性名称
			String propertyName = pv.getName();
			AbstractNestablePropertyAccessor nestedPa;
			try {
				// 获取属性路径的属性访问器
				nestedPa = getPropertyAccessorForPropertyPath(propertyName);
			} catch (NotReadablePropertyException ex) {
				// 如果属性路径不可读，则抛出不可写属性异常
				throw new NotWritablePropertyException(getRootClass(), this.nestedPath + propertyName,
						"Nested property in path '" + propertyName + "' does not exist", ex);
			}
			// 获取属性路径的属性名称令牌
			tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
			// 如果属性路径的属性访问器与当前访问器相同
			if (nestedPa == this) {
				// 将令牌解析后的原始属性值设置为令牌
				pv.getOriginalPropertyValue().resolvedTokens = tokens;
			}
			// 设置属性值
			nestedPa.setPropertyValue(tokens, pv);
		} else {
			// 否则，直接设置属性值
			setPropertyValue(tokens, pv);
		}
	}

	protected void setPropertyValue(PropertyTokenHolder tokens, PropertyValue pv) throws BeansException {
		// 如果令牌中包含键
		if (tokens.keys != null) {
			// 处理键值对属性
			processKeyedProperty(tokens, pv);
		} else {
			// 否则，处理本地属性
			processLocalProperty(tokens, pv);
		}
	}

	@SuppressWarnings("unchecked")
	private void processKeyedProperty(PropertyTokenHolder tokens, PropertyValue pv) {
		// 获取属性持有的值
		Object propValue = getPropertyHoldingValue(tokens);
		// 获取本地属性处理器
		PropertyHandler ph = getLocalPropertyHandler(tokens.actualName);
		// 如果本地属性处理器为空，则抛出无效属性异常
		if (ph == null) {
			throw new InvalidPropertyException(
					getRootClass(), this.nestedPath + tokens.actualName, "No property handler found");
		}
		// 确定最后一个键
		Assert.state(tokens.keys != null, "No token keys");
		String lastKey = tokens.keys[tokens.keys.length - 1];

		// 如果属性值为数组
		if (propValue.getClass().isArray()) {
			// 获取数组的组件类型和索引
			Class<?> requiredType = propValue.getClass().getComponentType();
			int arrayIndex = Integer.parseInt(lastKey);
			Object oldValue = null;
			try {
				// 如果需要提取旧值并且索引小于数组长度，则获取旧值
				if (isExtractOldValueForEditor() && arrayIndex < Array.getLength(propValue)) {
					oldValue = Array.get(propValue, arrayIndex);
				}
				// 将属性值转换为指定类型的值
				Object convertedValue = convertIfNecessary(tokens.canonicalName, oldValue, pv.getValue(),
						requiredType, ph.nested(tokens.keys.length));
				// 获取数组的长度
				int length = Array.getLength(propValue);
				// 如果索引大于等于数组长度并且小于自动增长集合限制
				if (arrayIndex >= length && arrayIndex < this.autoGrowCollectionLimit) {
					// 创建新数组并拷贝旧数组元素
					Class<?> componentType = propValue.getClass().getComponentType();
					Object newArray = Array.newInstance(componentType, arrayIndex + 1);
					System.arraycopy(propValue, 0, newArray, 0, length);
					// 设置属性值为新数组
					int lastKeyIndex = tokens.canonicalName.lastIndexOf('[');
					String propName = tokens.canonicalName.substring(0, lastKeyIndex);
					setPropertyValue(propName, newArray);
					propValue = getPropertyValue(propName);
				}
				// 设置数组元素值
				Array.set(propValue, arrayIndex, convertedValue);
			} catch (IndexOutOfBoundsException ex) {
				throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
						"Invalid array index in property path '" + tokens.canonicalName + "'", ex);
			}
		} else if (propValue instanceof List) {
			// 如果属性值为 List
			Class<?> requiredType = ph.getCollectionType(tokens.keys.length);
			List<Object> list = (List<Object>) propValue;
			int index = Integer.parseInt(lastKey);
			Object oldValue = null;
			if (isExtractOldValueForEditor() && index < list.size()) {
				oldValue = list.get(index);
			}
			// 将属性值转换为指定类型的值
			Object convertedValue = convertIfNecessary(tokens.canonicalName, oldValue, pv.getValue(),
					requiredType, ph.nested(tokens.keys.length));
			// 获取 List 的大小
			int size = list.size();
			// 如果索引大于等于 List 大小并且小于自动增长集合限制
			if (index >= size && index < this.autoGrowCollectionLimit) {
				// 将 List 扩展到索引处，并添加元素
				for (int i = size; i < index; i++) {
					try {
						list.add(null);
					} catch (NullPointerException ex) {
						throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
								"Cannot set element with index " + index + " in List of size " +
										size + ", accessed using property path '" + tokens.canonicalName +
										"': List does not support filling up gaps with null elements");
					}
				}
				list.add(convertedValue);
			} else {
				// 否则，设置 List 元素值
				try {
					list.set(index, convertedValue);
				} catch (IndexOutOfBoundsException ex) {
					throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
							"Invalid list index in property path '" + tokens.canonicalName + "'", ex);
				}
			}
		} else if (propValue instanceof Map) {
			// 如果属性值为 Map
			Class<?> mapKeyType = ph.getMapKeyType(tokens.keys.length);
			Class<?> mapValueType = ph.getMapValueType(tokens.keys.length);
			Map<Object, Object> map = (Map<Object, Object>) propValue;
			// 获取键类型和值类型
			TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(mapKeyType);
			// 将键转换为指定类型的值
			Object convertedMapKey = convertIfNecessary(null, null, lastKey, mapKeyType, typeDescriptor);
			Object oldValue = null;
			if (isExtractOldValueForEditor()) {
				oldValue = map.get(convertedMapKey);
			}
			// 将属性值转换为指定类型的值
			Object convertedMapValue = convertIfNecessary(tokens.canonicalName, oldValue, pv.getValue(),
					mapValueType, ph.nested(tokens.keys.length));
			// 将键值对添加到 Map 中
			map.put(convertedMapKey, convertedMapValue);
		} else {
			// 否则，抛出无效属性异常
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
					"Property referenced in indexed property path '" + tokens.canonicalName +
							"' is neither an array nor a List nor a Map; returned value was [" + propValue + "]");
		}
	}

	private Object getPropertyHoldingValue(PropertyTokenHolder tokens) {
		// 应用索引和映射键：获取除最后一个键以外的所有键的值
		Assert.state(tokens.keys != null, "No token keys");
		// 创建新的属性令牌持有者，用于获取属性值
		PropertyTokenHolder getterTokens = new PropertyTokenHolder(tokens.actualName);
		// 设置属性令牌持有者的规范名称和键
		getterTokens.canonicalName = tokens.canonicalName;
		getterTokens.keys = new String[tokens.keys.length - 1];
		System.arraycopy(tokens.keys, 0, getterTokens.keys, 0, tokens.keys.length - 1);

		Object propValue;
		try {
			// 获取属性值
			propValue = getPropertyValue(getterTokens);
		} catch (NotReadablePropertyException ex) {
			// 如果属性不可读，则抛出不可写属性异常
			throw new NotWritablePropertyException(getRootClass(), this.nestedPath + tokens.canonicalName,
					"Cannot access indexed value in property referenced " +
							"in indexed property path '" + tokens.canonicalName + "'", ex);
		}

		if (propValue == null) {
			// 如果属性值为 null
			if (isAutoGrowNestedPaths()) {
				// 如果自动增长嵌套路径，则设置默认值
				int lastKeyIndex = tokens.canonicalName.lastIndexOf('[');
				getterTokens.canonicalName = tokens.canonicalName.substring(0, lastKeyIndex);
				propValue = setDefaultValue(getterTokens);
			} else {
				// 否则，抛出嵌套路径中的空值异常
				throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + tokens.canonicalName,
						"Cannot access indexed value in property referenced " +
								"in indexed property path '" + tokens.canonicalName + "': returned null");
			}
		}
		return propValue;
	}

	private void processLocalProperty(PropertyTokenHolder tokens, PropertyValue pv) {
		// 获取局部属性处理器，并检查是否可写
		PropertyHandler ph = getLocalPropertyHandler(tokens.actualName);
		if (ph == null || !ph.isWritable()) {
			// 如果不存在局部属性处理器，或者该处理器不可以写
			if (pv.isOptional()) {
				// 可选属性且属性未找到时，忽略
				if (logger.isDebugEnabled()) {
					logger.debug("Ignoring optional value for property '" + tokens.actualName +
							"' - property not found on bean class [" + getRootClass().getName() + "]");
				}
				return;
			}
			if (this.suppressNotWritablePropertyException) {
				// 当忽略未知属性时，优化以避免异常抛出
				return;
			}
			// 抛出不可写属性异常
			throw createNotWritablePropertyException(tokens.canonicalName);
		}

		Object oldValue = null;
		try {
			Object originalValue = pv.getValue();
			Object valueToApply = originalValue;
			if (!Boolean.FALSE.equals(pv.conversionNecessary)) {
				if (pv.isConverted()) {
					// 如果已转换，直接应用转换后的值
					valueToApply = pv.getConvertedValue();
				} else {
					// 否则进行属性值的转换
					if (isExtractOldValueForEditor() && ph.isReadable()) {
						// 如果需要提取旧值且属性可读，尝试获取旧值
						try {
							oldValue = ph.getValue();
						} catch (Exception ex) {
							if (ex instanceof PrivilegedActionException) {
								ex = ((PrivilegedActionException) ex).getException();
							}
							if (logger.isDebugEnabled()) {
								logger.debug("Could not read previous value of property '" +
										this.nestedPath + tokens.canonicalName + "'", ex);
							}
						}
					}
					// 进行属性值的转换
					valueToApply = convertForProperty(
							tokens.canonicalName, oldValue, originalValue, ph.toTypeDescriptor());
				}
				// 标记属性值是否已转换
				pv.getOriginalPropertyValue().conversionNecessary = (valueToApply != originalValue);
			}
			// 应用属性值
			ph.setValue(valueToApply);
		} catch (TypeMismatchException ex) {
			throw ex;
		} catch (InvocationTargetException ex) {
			// 处理方法调用异常
			PropertyChangeEvent propertyChangeEvent = new PropertyChangeEvent(
					getRootInstance(), this.nestedPath + tokens.canonicalName, oldValue, pv.getValue());
			if (ex.getTargetException() instanceof ClassCastException) {
				// 如果目标异常为类转换异常，抛出类型不匹配异常
				throw new TypeMismatchException(propertyChangeEvent, ph.getPropertyType(), ex.getTargetException());
			} else {
				Throwable cause = ex.getTargetException();
				if (cause instanceof UndeclaredThrowableException) {
					// 在某些情况下可能出现的未声明的可抛出异常
					cause = cause.getCause();
				}
				// 抛出方法调用异常
				throw new MethodInvocationException(propertyChangeEvent, cause);
			}
		} catch (Exception ex) {
			// 处理其他异常
			PropertyChangeEvent pce = new PropertyChangeEvent(
					getRootInstance(), this.nestedPath + tokens.canonicalName, oldValue, pv.getValue());
			throw new MethodInvocationException(pce, ex);
		}
	}

	@Override
	@Nullable
	public Class<?> getPropertyType(String propertyName) throws BeansException {
		try {
			// 尝试获取属性处理器
			PropertyHandler ph = getPropertyHandler(propertyName);
			if (ph != null) {
				// 如果存在属性处理器，返回属性类型
				return ph.getPropertyType();
			} else {
				// 可能是索引/映射属性...
				Object value = getPropertyValue(propertyName);
				if (value != null) {
					// 如果属性值不为空，返回其类类型
					return value.getClass();
				}
				// 检查是否存在自定义编辑器，可能会提供所需的目标类型信息
				Class<?> editorType = guessPropertyTypeFromEditors(propertyName);
				if (editorType != null) {
					// 如果存在自定义编辑器，返回其类型
					return editorType;
				}
			}
		} catch (InvalidPropertyException ex) {
			// 当属性无效时，视为无法确定属性类型
		}
		// 默认返回null
		return null;
	}

	@Override
	@Nullable
	public TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException {
		try {
			// 获取指定属性路径的属性访问器
			AbstractNestablePropertyAccessor nestedPa = getPropertyAccessorForPropertyPath(propertyName);
			// 获取最终路径
			String finalPath = getFinalPath(nestedPa, propertyName);
			// 获取属性名称标记
			PropertyTokenHolder tokens = getPropertyNameTokens(finalPath);
			// 获取局部属性处理器
			PropertyHandler ph = nestedPa.getLocalPropertyHandler(tokens.actualName);
			if (ph != null) {
				if (tokens.keys != null) {
					// 如果存在索引或映射键
					if (ph.isReadable() || ph.isWritable()) {
						// 如果属性可读或可写，返回嵌套类型描述符
						return ph.nested(tokens.keys.length);
					}
				} else {
					// 如果不存在索引或映射键
					if (ph.isReadable() || ph.isWritable()) {
						// 如果属性可读或可写，返回类型描述符
						return ph.toTypeDescriptor();
					}
				}
			}
		} catch (InvalidPropertyException ex) {
			// 当属性无效时，视为无法确定属性类型
		}
		// 默认返回null
		return null;
	}

	@Override
	public boolean isReadableProperty(String propertyName) {
		try {
			// 获取属性处理器
			PropertyHandler ph = getPropertyHandler(propertyName);
			if (ph != null) {
				// 如果属性处理器不为空，返回属性是否可读
				return ph.isReadable();
			} else {
				// 可能是索引/映射属性...
				getPropertyValue(propertyName);
				// 如果成功获取属性值，说明属性可读
				return true;
			}
		} catch (InvalidPropertyException ex) {
			// 属性无效，无法确定是否可读
		}
		// 默认返回false
		return false;
	}

	@Override
	public boolean isWritableProperty(String propertyName) {
		try {
			// 获取属性处理器
			PropertyHandler ph = getPropertyHandler(propertyName);
			if (ph != null) {
				// 如果属性处理器不为空，返回属性是否可写
				return ph.isWritable();
			} else {
				// 可能是索引/映射属性...
				getPropertyValue(propertyName);
				// 如果成功获取属性值，说明属性可写
				return true;
			}
		} catch (InvalidPropertyException ex) {
			// 属性无效，无法确定是否可写
		}
		// 默认返回false
		return false;
	}

	@Nullable
	private Object convertIfNecessary(@Nullable String propertyName, @Nullable Object oldValue,
									  @Nullable Object newValue, @Nullable Class<?> requiredType, @Nullable TypeDescriptor td)
			throws TypeMismatchException {

		Assert.state(this.typeConverterDelegate != null, "No TypeConverterDelegate");
		try {
			return this.typeConverterDelegate.convertIfNecessary(propertyName, oldValue, newValue, requiredType, td);
		} catch (ConverterNotFoundException | IllegalStateException ex) {
			PropertyChangeEvent pce =
					new PropertyChangeEvent(getRootInstance(), this.nestedPath + propertyName, oldValue, newValue);
			throw new ConversionNotSupportedException(pce, requiredType, ex);
		} catch (ConversionException | IllegalArgumentException ex) {
			PropertyChangeEvent pce =
					new PropertyChangeEvent(getRootInstance(), this.nestedPath + propertyName, oldValue, newValue);
			throw new TypeMismatchException(pce, requiredType, ex);
		}
	}

	@Nullable
	protected Object convertForProperty(
			String propertyName, @Nullable Object oldValue, @Nullable Object newValue, TypeDescriptor td)
			throws TypeMismatchException {

		return convertIfNecessary(propertyName, oldValue, newValue, td.getType(), td);
	}

	@Override
	@Nullable
	public Object getPropertyValue(String propertyName) throws BeansException {
		// 获取属性访问器
		AbstractNestablePropertyAccessor nestedPa = getPropertyAccessorForPropertyPath(propertyName);
		// 获取属性名对应的属性令牌
		PropertyTokenHolder tokens = getPropertyNameTokens(getFinalPath(nestedPa, propertyName));
		// 通过属性访问器获取属性值
		return nestedPa.getPropertyValue(tokens);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	protected Object getPropertyValue(PropertyTokenHolder tokens) throws BeansException {
		// 获取属性名
		String propertyName = tokens.canonicalName;
		// 获取实际名称
		String actualName = tokens.actualName;
		// 获取属性处理程序
		PropertyHandler ph = getLocalPropertyHandler(actualName);
		// 检查属性处理程序是否可读
		if (ph == null || !ph.isReadable()) {
			throw new NotReadablePropertyException(getRootClass(), this.nestedPath + propertyName);
		}
		try {
			Object value = ph.getValue();
			// 如果有索引或键，则对属性值应用索引和映射键
			if (tokens.keys != null) {
				// 如果属性值为null
				if (value == null) {
					// 如果自动增长嵌套路径
					if (isAutoGrowNestedPaths()) {
						// 设置默认值
						value = setDefaultValue(new PropertyTokenHolder(tokens.actualName));
					} else {
						// 抛出异常，无法访问属性的索引值
						throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + propertyName,
								"Cannot access indexed value of property referenced in indexed " +
										"property path '" + propertyName + "': returned null");
					}
				}
				StringBuilder indexedPropertyName = new StringBuilder(tokens.actualName);
				// 遍历所有键
				for (int i = 0; i < tokens.keys.length; i++) {
					String key = tokens.keys[i];
					// 如果属性值为数组
					if (value == null) {
						// 抛出异常，无法访问属性的索引值
						throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + propertyName,
								"Cannot access indexed value of property referenced in indexed " +
										"property path '" + propertyName + "': returned null");
					} else if (value.getClass().isArray()) {
						// 将属性值扩展为数组（如果必要），并获取索引处的值
						int index = Integer.parseInt(key);
						value = growArrayIfNecessary(value, index, indexedPropertyName.toString());
						value = Array.get(value, index);
					} else if (value instanceof List) {
						// 将属性值扩展为列表（如果必要），并获取索引处的值
						int index = Integer.parseInt(key);
						List<Object> list = (List<Object>) value;
						growCollectionIfNecessary(list, index, indexedPropertyName.toString(), ph, i + 1);
						value = list.get(index);
					} else if (value instanceof Set) {
						// 将属性值扩展为集合（如果必要），并获取索引处的值
						Set<Object> set = (Set<Object>) value;
						int index = Integer.parseInt(key);
						if (index < 0 || index >= set.size()) {
							// 抛出异常，无法从集合中获取指定索引的元素
							throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
									"Cannot get element with index " + index + " from Set of size " +
											set.size() + ", accessed using property path '" + propertyName + "'");
						}
						Iterator<Object> it = set.iterator();
						for (int j = 0; it.hasNext(); j++) {
							Object elem = it.next();
							if (j == index) {
								value = elem;
								break;
							}
						}
					} else if (value instanceof Map) {
						// 将属性值扩展为映射（如果必要），并获取索引处的值
						Map<Object, Object> map = (Map<Object, Object>) value;
						Class<?> mapKeyType = ph.getResolvableType().getNested(i + 1).asMap().resolveGeneric(0);
						// 创建类型描述符
						TypeDescriptor typeDescriptor = TypeDescriptor.valueOf(mapKeyType);
						// 将键转换为适当的类型，并获取对应的值
						Object convertedMapKey = convertIfNecessary(null, null, key, mapKeyType, typeDescriptor);
						value = map.get(convertedMapKey);
					} else {
						// 抛出异常，无法获取属性的索引值
						throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
								"Property referenced in indexed property path '" + propertyName +
										"' is neither an array nor a List nor a Set nor a Map; returned value was [" + value + "]");
					}
					// 将当前键追加到属性名中
					indexedPropertyName.append(PROPERTY_KEY_PREFIX).append(key).append(PROPERTY_KEY_SUFFIX);
				}
			}
			// 返回属性值
			return value;
		} catch (IndexOutOfBoundsException ex) {
			// 抛出异常，属性路径中的索引越界
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
					"Index of out of bounds in property path '" + propertyName + "'", ex);
		} catch (NumberFormatException | TypeMismatchException ex) {
			// 抛出异常，属性路径中的索引无效
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
					"Invalid index in property path '" + propertyName + "'", ex);
		} catch (InvocationTargetException ex) {
			// 抛出异常，属性的getter方法引发异常
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
					"Getter for property '" + actualName + "' threw exception", ex);
		} catch (Exception ex) {
			// 抛出异常，非法获取属性值
			throw new InvalidPropertyException(getRootClass(), this.nestedPath + propertyName,
					"Illegal attempt to get property '" + actualName + "' threw exception", ex);
		}
	}


	/**
	 * 返回指定的 propertyName 的 {@link PropertyHandler}，如有必要进行导航。
	 * 如果未找到，则返回 {@code null} 而不是抛出异常。
	 *
	 * @param propertyName 要获取描述符的属性
	 * @return 指定属性的属性描述符，如果未找到则为 {@code null}
	 * @throws BeansException 如果内省失败
	 */
	@Nullable
	protected PropertyHandler getPropertyHandler(String propertyName) throws BeansException {
		Assert.notNull(propertyName, "Property name must not be null");
		AbstractNestablePropertyAccessor nestedPa = getPropertyAccessorForPropertyPath(propertyName);
		return nestedPa.getLocalPropertyHandler(getFinalPath(nestedPa, propertyName));
	}

	/**
	 * 返回指定本地 propertyName 的 {@link PropertyHandler}。仅用于访问当前上下文中可用的属性。
	 *
	 * @param propertyName 本地属性的名称
	 * @return 该属性的处理程序，如果未找到则为 {@code null}
	 */
	@Nullable
	protected abstract PropertyHandler getLocalPropertyHandler(String propertyName);

	/**
	 * 创建一个新的嵌套属性访问器实例。可以在子类中重写以创建 PropertyAccessor 子类。
	 *
	 * @param object     此 PropertyAccessor 包装的对象
	 * @param nestedPath 对象的嵌套路径
	 * @return 嵌套 PropertyAccessor 实例
	 */
	protected abstract AbstractNestablePropertyAccessor newNestedPropertyAccessor(Object object, String nestedPath);

	/**
	 * 为指定的属性创建 {@link NotWritablePropertyException}。
	 */
	protected abstract NotWritablePropertyException createNotWritablePropertyException(String propertyName);


	private Object growArrayIfNecessary(Object array, int index, String name) {
		// 如果不自动增长嵌套路径，则直接返回数组
		if (!isAutoGrowNestedPaths()) {
			return array;
		}
		// 获取数组的长度
		int length = Array.getLength(array);
		// 如果索引超出数组长度并且小于自动增长集合限制
		if (index >= length && index < this.autoGrowCollectionLimit) {
			// 获取数组的组件类型
			Class<?> componentType = array.getClass().getComponentType();
			// 创建新的数组，并将旧数组的内容复制到新数组中
			Object newArray = Array.newInstance(componentType, index + 1);
			System.arraycopy(array, 0, newArray, 0, length);
			// 将新数组中未初始化的元素设置为默认值
			for (int i = length; i < Array.getLength(newArray); i++) {
				Array.set(newArray, i, newValue(componentType, null, name));
			}
			// 设置属性值为新数组
			setPropertyValue(name, newArray);
			// 获取新数组的默认值
			Object defaultValue = getPropertyValue(name);
			// 断言默认值不为空
			Assert.state(defaultValue != null, "Default value must not be null");
			return defaultValue;
		} else {
			// 否则直接返回数组
			return array;
		}
	}

	private void growCollectionIfNecessary(Collection<Object> collection, int index, String name,
										   PropertyHandler ph, int nestingLevel) {

		// 如果不自动增长嵌套路径，则直接返回
		if (!isAutoGrowNestedPaths()) {
			return;
		}
		// 获取集合的大小
		int size = collection.size();
		// 如果索引超出集合大小并且小于自动增长集合限制
		if (index >= size && index < this.autoGrowCollectionLimit) {
			// 获取元素类型
			Class<?> elementType = ph.getResolvableType().getNested(nestingLevel).asCollection().resolveGeneric();
			// 如果元素类型不为空
			if (elementType != null) {
				// 将集合增长到索引位置并设置默认值
				for (int i = collection.size(); i < index + 1; i++) {
					collection.add(newValue(elementType, null, name));
				}
			}
		}
	}

	/**
	 * 获取路径的最后一个组件。如果未嵌套，也适用。
	 *
	 * @param pa         要操作的属性访问器
	 * @param nestedPath 我们知道是嵌套的属性路径
	 * @return 路径的最后一个组件（目标 bean 上的属性）
	 */
	protected String getFinalPath(AbstractNestablePropertyAccessor pa, String nestedPath) {
		if (pa == this) {
			return nestedPath;
		}
		return nestedPath.substring(PropertyAccessorUtils.getLastNestedPropertySeparatorIndex(nestedPath) + 1);
	}

	/**
	 * 递归导航以返回嵌套属性路径的属性访问器。
	 *
	 * @param propertyPath 属性路径，可能是嵌套的
	 * @return 目标 bean 的属性访问器
	 */
	protected AbstractNestablePropertyAccessor getPropertyAccessorForPropertyPath(String propertyPath) {
		// 获取第一个嵌套属性分隔符的位置
		int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(propertyPath);
		// 如果找到嵌套属性分隔符
		if (pos > -1) {
			// 获取嵌套属性和嵌套路径
			String nestedProperty = propertyPath.substring(0, pos);
			String nestedPath = propertyPath.substring(pos + 1);
			// 获取嵌套属性访问器
			AbstractNestablePropertyAccessor nestedPa = getNestedPropertyAccessor(nestedProperty);
			// 递归处理嵌套属性
			return nestedPa.getPropertyAccessorForPropertyPath(nestedPath);
		} else {
			// 如果没有找到嵌套属性分隔符，则返回当前属性访问器
			return this;
		}
	}

	/**
	 * 检索给定嵌套属性的属性访问器。
	 * 如果在缓存中找不到，则创建一个新的。
	 * 注意：现在缓存嵌套属性访问器是必要的，以保持嵌套属性的注册自定义编辑器。
	 *
	 * @param nestedProperty 要为其创建属性访问器的属性
	 * @return PropertyAccessor 实例，可以是缓存的也可以是新创建的
	 */
	private AbstractNestablePropertyAccessor getNestedPropertyAccessor(String nestedProperty) {
		// 如果嵌套属性访问器映射为空，则创建一个新的映射
		if (this.nestedPropertyAccessors == null) {
			this.nestedPropertyAccessors = new HashMap<>();
		}
		// 获取属性名的标记信息
		PropertyTokenHolder tokens = getPropertyNameTokens(nestedProperty);
		String canonicalName = tokens.canonicalName;
		// 获取属性值
		Object value = getPropertyValue(tokens);
		// 如果属性值为空或为Optional且不包含值
		if (value == null || (value instanceof Optional && !((Optional<?>) value).isPresent())) {
			// 如果自动增长嵌套路径，则设置默认值
			if (isAutoGrowNestedPaths()) {
				value = setDefaultValue(tokens);
			} else {
				// 否则抛出空值异常
				throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + canonicalName);
			}
		}

		// 查找缓存的子属性访问器，如果未找到则创建一个新的
		AbstractNestablePropertyAccessor nestedPa = this.nestedPropertyAccessors.get(canonicalName);
		// 如果未找到或找到的值与当前属性值不同
		if (nestedPa == null || nestedPa.getWrappedInstance() != ObjectUtils.unwrapOptional(value)) {
			// 如果日志记录为跟踪级别
			if (logger.isTraceEnabled()) {
				logger.trace("Creating new nested " + getClass().getSimpleName() + " for property '" + canonicalName + "'");
			}
			// 创建新的嵌套属性访问器
			nestedPa = newNestedPropertyAccessor(value, this.nestedPath + canonicalName + NESTED_PROPERTY_SEPARATOR);
			// 继承所有类型特定的属性编辑器
			copyDefaultEditorsTo(nestedPa);
			copyCustomEditorsTo(nestedPa, canonicalName);
			// 将新创建的嵌套属性访问器放入缓存中
			this.nestedPropertyAccessors.put(canonicalName, nestedPa);
		} else {
			// 如果找到缓存的嵌套属性访问器，则记录跟踪日志
			if (logger.isTraceEnabled()) {
				logger.trace("Using cached nested property accessor for property '" + canonicalName + "'");
			}
		}
		// 返回嵌套属性访问器
		return nestedPa;
	}

	private Object setDefaultValue(PropertyTokenHolder tokens) {
		// 创建默认的属性值对象
		PropertyValue pv = createDefaultPropertyValue(tokens);
		// 设置属性值
		setPropertyValue(tokens, pv);
		// 获取属性值
		Object defaultValue = getPropertyValue(tokens);
		// 断言默认值不为空
		Assert.state(defaultValue != null, "Default value must not be null");
		// 返回默认值
		return defaultValue;
	}

	private PropertyValue createDefaultPropertyValue(PropertyTokenHolder tokens) {
		// 获取属性的类型描述符
		TypeDescriptor desc = getPropertyTypeDescriptor(tokens.canonicalName);
		// 如果描述符为空，则抛出异常
		if (desc == null) {
			throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + tokens.canonicalName,
					"Could not determine property type for auto-growing a default value");
		}
		// 根据类型描述符创建一个新值作为默认值
		Object defaultValue = newValue(desc.getType(), desc, tokens.canonicalName);
		// 创建一个新的属性值对象，使用属性的规范名称和默认值
		return new PropertyValue(tokens.canonicalName, defaultValue);
	}

	private Object newValue(Class<?> type, @Nullable TypeDescriptor desc, String name) {
		try {
			if (type.isArray()) {
				// 处理数组类型
				Class<?> componentType = type.getComponentType();
				// TODO - 只处理二维数组
				if (componentType.isArray()) {
					// 创建一个二维数组
					Object array = Array.newInstance(componentType, 1);
					Array.set(array, 0, Array.newInstance(componentType.getComponentType(), 0));
					return array;
				} else {
					// 创建一个一维数组
					return Array.newInstance(componentType, 0);
				}
			} else if (Collection.class.isAssignableFrom(type)) {
				// 处理集合类型
				TypeDescriptor elementDesc = (desc != null ? desc.getElementTypeDescriptor() : null);
				return CollectionFactory.createCollection(type, (elementDesc != null ? elementDesc.getType() : null), 16);
			} else if (Map.class.isAssignableFrom(type)) {
				// 处理映射类型
				TypeDescriptor keyDesc = (desc != null ? desc.getMapKeyTypeDescriptor() : null);
				return CollectionFactory.createMap(type, (keyDesc != null ? keyDesc.getType() : null), 16);
			} else {
				// 对于其他类型，尝试调用无参构造函数进行实例化
				Constructor<?> ctor = type.getDeclaredConstructor();
				if (Modifier.isPrivate(ctor.getModifiers())) {
					throw new IllegalAccessException("Auto-growing not allowed with private constructor: " + ctor);
				}
				return BeanUtils.instantiateClass(ctor);
			}
		} catch (Throwable ex) {
			// 捕获异常，抛出新的异常
			throw new NullValueInNestedPathException(getRootClass(), this.nestedPath + name,
					"Could not instantiate property type [" + type.getName() + "] to auto-grow nested property path", ex);
		}
	}

	/**
	 * 将给定的属性名解析为相应的属性名标记。
	 *
	 * @param propertyName 要解析的属性名
	 * @return 解析后的属性标记的表示形式
	 */
	private PropertyTokenHolder getPropertyNameTokens(String propertyName) {
		String actualName = null;
		List<String> keys = new ArrayList<>(2);
		int searchIndex = 0;
		while (searchIndex != -1) {
			// 查找属性名中的键的起始位置
			int keyStart = propertyName.indexOf(PROPERTY_KEY_PREFIX, searchIndex);
			searchIndex = -1;
			if (keyStart != -1) {
				// 获取键的结束位置
				int keyEnd = getPropertyNameKeyEnd(propertyName, keyStart + PROPERTY_KEY_PREFIX.length());
				if (keyEnd != -1) {
					// 如果找到键，则提取实际名称
					if (actualName == null) {
						actualName = propertyName.substring(0, keyStart);
					}
					// 提取键值，并移除包围键值的引号
					String key = propertyName.substring(keyStart + PROPERTY_KEY_PREFIX.length(), keyEnd);
					if (key.length() > 1 && ((key.startsWith("'") && key.endsWith("'")) ||
							(key.startsWith("\"") && key.endsWith("\"")))) {
						key = key.substring(1, key.length() - 1);
					}
					// 将键值添加到列表中
					keys.add(key);
					// 更新搜索索引
					searchIndex = keyEnd + PROPERTY_KEY_SUFFIX.length();
				}
			}
		}
		// 构建 PropertyTokenHolder 对象
		PropertyTokenHolder tokens = new PropertyTokenHolder(actualName != null ? actualName : propertyName);
		if (!keys.isEmpty()) {
			// 将键值添加到属性名称中
			tokens.canonicalName += PROPERTY_KEY_PREFIX +
					StringUtils.collectionToDelimitedString(keys, PROPERTY_KEY_SUFFIX + PROPERTY_KEY_PREFIX) +
					PROPERTY_KEY_SUFFIX;
			// 将键值列表转换为数组
			tokens.keys = StringUtils.toStringArray(keys);
		}
		return tokens;
	}

	private int getPropertyNameKeyEnd(String propertyName, int startIndex) {
		// 未闭合的前缀数初始化为0
		int unclosedPrefixes = 0;
		// 获取属性名的长度
		int length = propertyName.length();
		// 从起始索引开始遍历属性名
		for (int i = startIndex; i < length; i++) {
			// 根据当前索引处的字符判断
			switch (propertyName.charAt(i)) {
				case PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR:
					// 如果当前字符是属性键前缀字符
					// 属性名包含未闭合的前缀
					unclosedPrefixes++;
					break;
				case PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR:
					// 如果当前字符是属性键后缀字符
					// 如果属性名中没有未闭合的前缀
					if (unclosedPrefixes == 0) {
						// 属性名中没有未闭合的前缀（左边）->
						// 这是我们要查找的后缀
						return i;
					} else {
						// 此后缀不关闭初始前缀，而是在属性名中出现的前缀
						unclosedPrefixes--;
					}
					break;
			}
		}
		// 未找到匹配的后缀，返回-1
		return -1;
	}


	@Override
	public String toString() {
		String className = getClass().getName();
		if (this.wrappedObject == null) {
			return className + ": no wrapped object set";
		}
		return className + ": wrapping object [" + ObjectUtils.identityToString(this.wrappedObject) + ']';
	}


	/**
	 * 特定属性的处理程序。
	 */
	protected abstract static class PropertyHandler {

		/**
		 * 属性类型
		 */
		private final Class<?> propertyType;

		/**
		 * 是否可读
		 */
		private final boolean readable;

		/**
		 * 是否可写
		 */
		private final boolean writable;

		public PropertyHandler(Class<?> propertyType, boolean readable, boolean writable) {
			this.propertyType = propertyType;
			this.readable = readable;
			this.writable = writable;
		}

		public Class<?> getPropertyType() {
			return this.propertyType;
		}

		public boolean isReadable() {
			return this.readable;
		}

		public boolean isWritable() {
			return this.writable;
		}

		public abstract TypeDescriptor toTypeDescriptor();

		public abstract ResolvableType getResolvableType();

		@Nullable
		public Class<?> getMapKeyType(int nestingLevel) {
			return getResolvableType().getNested(nestingLevel).asMap().resolveGeneric(0);
		}

		@Nullable
		public Class<?> getMapValueType(int nestingLevel) {
			return getResolvableType().getNested(nestingLevel).asMap().resolveGeneric(1);
		}

		@Nullable
		public Class<?> getCollectionType(int nestingLevel) {
			return getResolvableType().getNested(nestingLevel).asCollection().resolveGeneric();
		}

		@Nullable
		public abstract TypeDescriptor nested(int level);

		@Nullable
		public abstract Object getValue() throws Exception;

		public abstract void setValue(@Nullable Object value) throws Exception;
	}


	/**
	 * 用于存储属性标记的持有者类。
	 */
	protected static class PropertyTokenHolder {


		public PropertyTokenHolder(String name) {
			this.actualName = name;
			this.canonicalName = name;
		}

		/**
		 * 实际名称
		 */
		public String actualName;

		/**
		 * 规范名称
		 */
		public String canonicalName;

		/**
		 * 键值
		 */
		@Nullable
		public String[] keys;
	}

}
