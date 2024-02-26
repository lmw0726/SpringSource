/*
 * Copyright 2002-2012 the original author or authors.
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

import java.beans.PropertyEditor;

import org.springframework.lang.Nullable;

/**
 * 封装了注册JavaBeans PropertyEditors的方法。
 * 这是PropertyEditorRegistrar操作的中心接口。
 *
 * 扩展自BeanWrapper；由BeanWrapperImpl和org.springframework.validation.DataBinder实现。
 *
 * @author Juergen Hoeller
 * @since 1.2.6
 * @see java.beans.PropertyEditor
 * @see PropertyEditorRegistrar
 * @see BeanWrapper
 * @see org.springframework.validation.DataBinder
 */
public interface PropertyEditorRegistry {

	/**
	 * 为给定类型的所有属性注册自定义属性编辑器。
	 * @param requiredType 属性的类型
	 * @param propertyEditor 要注册的编辑器
	 */
	void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor);

	/**
	 * 为给定类型和属性，或给定类型的所有属性注册自定义属性编辑器。
	 * 如果属性路径表示数组或Collection属性，则编辑器将被应用于数组/Collection本身（PropertyEditor必须创建数组或Collection值）或每个元素（PropertyEditor必须创建元素类型），取决于指定的requiredType。
	 * 注意：每个属性路径仅支持一个单独注册的自定义编辑器。
	 * 对于Collection/array，请不要在相同属性的Collection/array和每个元素上注册编辑器。
	 * 例如，如果要为"items[n].quantity"（所有值n）注册编辑器，则可以使用"items.quantity"作为此方法的'propertyPath'参数的值。
	 * @param requiredType 属性的类型。如果已指定属性，则可能为null，但无论如何都应指定，特别是在Collection的情况下-明确指定编辑器是应该应用于整个Collection本身还是应用于它的每个条目。因此，作为一般规则：在Collection/array的情况下，请不要在此处指定null！
	 * @param propertyPath 属性的路径（名称或嵌套路径），如果注册给定类型的所有属性的编辑器，则为null
	 * @param propertyEditor 要注册的编辑器
	 */
	void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath, PropertyEditor propertyEditor);

	/**
	 * 查找给定类型和属性的自定义属性编辑器。
	 * @param requiredType 属性的类型（如果给定属性可以为null，但应始终指定以进行一致性检查）
	 * @param propertyPath 属性的路径（名称或嵌套路径），如果查找给定类型的所有属性的编辑器，则为null
	 * @return 已注册的编辑器，如果没有则为null
	 */
	@Nullable
	PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath);

}
