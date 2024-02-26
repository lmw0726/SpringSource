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

package org.springframework.beans;

import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;

/**
 * 接口封装了 PropertyAccessor 的配置方法。
 * 还扩展了 PropertyEditorRegistry 接口，该接口定义了 PropertyEditor 管理方法。
 *
 * <p>用作 BeanWrapper 的基本接口。
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @see BeanWrapper
 * @since 2.0
 */
public interface ConfigurablePropertyAccessor extends PropertyAccessor, PropertyEditorRegistry, TypeConverter {

	/**
	 * 指定一个 Spring 3.0 ConversionService 用于转换属性值，作为 JavaBeans PropertyEditors 的替代。
	 */
	void setConversionService(@Nullable ConversionService conversionService);

	/**
	 * 返回关联的 ConversionService（如果有）。
	 */
	@Nullable
	ConversionService getConversionService();

	/**
	 * 设置在为属性的新值应用属性编辑器时是否提取旧属性值。
	 */
	void setExtractOldValueForEditor(boolean extractOldValueForEditor);

	/**
	 * 返回在为属性的新值应用属性编辑器时是否提取旧属性值。
	 */
	boolean isExtractOldValueForEditor();

	/**
	 * 设置此实例是否应尝试“自动增长”包含 {@code null} 值的嵌套路径。
	 * <p>如果 {@code true}，则 {@code null} 路径位置将使用默认对象值进行填充并遍历，而不是导致 {@link NullValueInNestedPathException}。
	 * <p>在普通 PropertyAccessor 实例上默认为 {@code false}。
	 */
	void setAutoGrowNestedPaths(boolean autoGrowNestedPaths);

	/**
	 * 返回是否已激活“自动增长”嵌套路径。
	 */
	boolean isAutoGrowNestedPaths();

}
