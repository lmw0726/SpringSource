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

package org.springframework.beans;

import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * PropertyAccessor接口的抽象实现。
 * 提供所有便利方法的基本实现，实际属性访问的实现留给子类。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see #getPropertyValue
 * @see #setPropertyValue
 * @since 2.0
 */
public abstract class AbstractPropertyAccessor extends TypeConverterSupport implements ConfigurablePropertyAccessor {

	/**
	 * 在为属性的新值应用属性编辑器时是否提取旧属性值。
	 */
	private boolean extractOldValueForEditor = false;

	/**
	 * 是否已激活“自动增长”嵌套路径。
	 */
	private boolean autoGrowNestedPaths = false;

	/**
	 * 忽略不可写属性异常标志为 true
	 */
	boolean suppressNotWritablePropertyException = false;


	@Override
	public void setExtractOldValueForEditor(boolean extractOldValueForEditor) {
		this.extractOldValueForEditor = extractOldValueForEditor;
	}

	@Override
	public boolean isExtractOldValueForEditor() {
		return this.extractOldValueForEditor;
	}

	@Override
	public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
		this.autoGrowNestedPaths = autoGrowNestedPaths;
	}

	@Override
	public boolean isAutoGrowNestedPaths() {
		return this.autoGrowNestedPaths;
	}


	@Override
	public void setPropertyValue(PropertyValue pv) throws BeansException {
		setPropertyValue(pv.getName(), pv.getValue());
	}

	@Override
	public void setPropertyValues(Map<?, ?> map) throws BeansException {
		setPropertyValues(new MutablePropertyValues(map));
	}

	@Override
	public void setPropertyValues(PropertyValues pvs) throws BeansException {
		setPropertyValues(pvs, false, false);
	}

	@Override
	public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown) throws BeansException {
		setPropertyValues(pvs, ignoreUnknown, false);
	}

	@Override
	public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid)
			throws BeansException {

		// 存储属性访问异常的列表
		List<PropertyAccessException> propertyAccessExceptions = null;

		// 获取属性值列表
		List<PropertyValue> propertyValues = (pvs instanceof MutablePropertyValues ?
				((MutablePropertyValues) pvs).getPropertyValueList() : Arrays.asList(pvs.getPropertyValues()));

		// 如果需要忽略未知属性，则设置忽略不可写属性异常标志为 true
		if (ignoreUnknown) {
			this.suppressNotWritablePropertyException = true;
		}
		try {
			// 遍历属性值列表
			for (PropertyValue pv : propertyValues) {
				// setPropertyValue 可能抛出任何 BeansException，如果出现关键失败（例如找不到匹配字段），
				// 则不会在此处捕获。我们只能尝试处理较轻微的异常。
				try {
					setPropertyValue(pv);
				} catch (NotWritablePropertyException ex) {
					// 如果不需要忽略未知属性，则抛出异常
					if (!ignoreUnknown) {
						throw ex;
					}
					// 否则，忽略异常并继续...
				} catch (NullValueInNestedPathException ex) {
					// 如果不需要忽略无效值异常，则抛出异常
					if (!ignoreInvalid) {
						throw ex;
					}
					// 否则，忽略异常并继续...
				} catch (PropertyAccessException ex) {
					// 如果 propertyAccessExceptions 为 null，则初始化为 ArrayList
					if (propertyAccessExceptions == null) {
						propertyAccessExceptions = new ArrayList<>();
					}
					// 将异常添加到列表中
					propertyAccessExceptions.add(ex);
				}
			}
		} finally {
			// 如果需要忽略未知属性，则将忽略不可写属性异常标志重置为 false
			if (ignoreUnknown) {
				this.suppressNotWritablePropertyException = false;
			}
		}

		// 如果存在单独的异常，则抛出组合异常
		if (propertyAccessExceptions != null) {
			PropertyAccessException[] paeArray = propertyAccessExceptions.toArray(new PropertyAccessException[0]);
			throw new PropertyBatchUpdateException(paeArray);
		}
	}


	// 重新定义为公共可见性。
	/**
	 * 确定给定属性路径的属性类型。
	 * <p>如果未指定所需类型，则由{@link #findCustomEditor}调用，
	 * 以便能够查找特定于类型的编辑器，即使只给出属性路径。
	 * <p>默认实现始终返回{@code null}。
	 * BeanWrapperImpl使用BeanWrapper接口定义的标准{@code getPropertyType}方法重写此方法。
	 *
	 * @param propertyPath 要确定类型的属性路径
	 * @return 属性的类型，如果无法确定则为{@code null}
	 * @see BeanWrapper#getPropertyType(String)
	 */
	@Override
	@Nullable
	public Class<?> getPropertyType(String propertyPath) {
		return null;
	}

	/**
	 * 实际获取属性值。
	 *
	 * @param propertyName 要获取值的属性的名称
	 * @return 属性的值
	 * @throws InvalidPropertyException 如果没有这样的属性或属性不可读
	 * @throws PropertyAccessException 如果属性有效但访问器方法失败
	 */
	@Override
	@Nullable
	public abstract Object getPropertyValue(String propertyName) throws BeansException;

	/**
	 * 实际设置属性值。
	 *
	 * @param propertyName 要设置值的属性的名称
	 * @param value        新值
	 * @throws InvalidPropertyException 如果没有这样的属性或属性不可写
	 * @throws PropertyAccessException 如果属性有效但访问器方法失败或类型不匹配
	 */
	@Override
	public abstract void setPropertyValue(String propertyName, @Nullable Object value) throws BeansException;

}
