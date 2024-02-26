/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;

import java.util.Map;

/**
 * 可以访问命名属性的类的通用接口（例如对象的bean属性或对象中的字段）。
 * <p>
 * 作为BeanWrapper的基本接口。
 *
 * @author Juergen Hoeller
 * @see BeanWrapper
 * @see PropertyAccessorFactory#forBeanPropertyAccess
 * @see PropertyAccessorFactory#forDirectFieldAccess
 * @since 1.1
 */
public interface PropertyAccessor {
	/**
	 * 嵌套属性的路径分隔符。
	 * 遵循常规的Java约定：getFoo().getBar() 将是 "foo.bar"。
	 */
	String NESTED_PROPERTY_SEPARATOR = ".";

	/**
	 * 嵌套属性的路径分隔符。
	 * 遵循常规的Java约定：getFoo().getBar() 将是 "foo.bar"。
	 */
	char NESTED_PROPERTY_SEPARATOR_CHAR = '.';

	/**
	 * 表示索引或映射属性键的起始标记，例如 "person.addresses[0]"。
	 */
	String PROPERTY_KEY_PREFIX = "[";

	/**
	 * 表示索引或映射属性键的起始标记，例如 "person.addresses[0]"。
	 */
	char PROPERTY_KEY_PREFIX_CHAR = '[';

	/**
	 * 表示索引或映射属性键的结束标记，例如 "person.addresses[0]"。
	 */
	String PROPERTY_KEY_SUFFIX = "]";

	/**
	 * 表示索引或映射属性键的结束标记，例如 "person.addresses[0]"。
	 */
	char PROPERTY_KEY_SUFFIX_CHAR = ']';


	/**
	 * 确定指定的属性是否可读。
	 * <p>如果属性不存在，则返回 {@code false}。
	 *
	 * @param propertyName 要检查的属性（可能是嵌套路径和/或索引/映射属性）
	 * @return 属性是否可读
	 */
	boolean isReadableProperty(String propertyName);

	/**
	 * 确定指定的属性是否可写。
	 * <p>如果属性不存在，则返回 {@code false}。
	 *
	 * @param propertyName 要检查的属性（可能是嵌套路径和/或索引/映射属性）
	 * @return 属性是否可写
	 */
	boolean isWritableProperty(String propertyName);

	/**
	 * 确定指定属性的属性类型，
	 * 要么检查属性描述符，要么在索引或映射元素的情况下检查值。
	 *
	 * @param propertyName 要检查的属性（可能是嵌套路径和/或索引/映射属性）
	 * @return 特定属性的属性类型，如果无法确定则返回 {@code null}
	 * @throws PropertyAccessException 如果属性有效但访问器方法失败
	 */
	@Nullable
	Class<?> getPropertyType(String propertyName) throws BeansException;

	/**
	 * 返回指定属性的类型描述符：
	 * 首选从读方法获取，然后退回到写方法。
	 *
	 * @param propertyName 要检查的属性（可能是嵌套路径和/或索引/映射属性）
	 * @return 特定属性的属性类型，如果无法确定则返回 {@code null}
	 * @throws PropertyAccessException 如果属性有效但访问器方法失败
	 */
	@Nullable
	TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException;

	/**
	 * 获取指定属性的当前值。
	 *
	 * @param propertyName 要获取其值的属性的名称（可能是嵌套路径和/或索引/映射属性）
	 * @return 属性的值
	 * @throws InvalidPropertyException 如果没有这样的属性或属性不可读
	 * @throws PropertyAccessException  如果属性有效但访问器方法失败
	 */
	@Nullable
	Object getPropertyValue(String propertyName) throws BeansException;

	/**
	 * 将指定值设置为当前属性值。
	 *
	 * @param propertyName 要设置值的属性的名称（可能是嵌套路径和/或索引/映射属性）
	 * @param value        新值
	 * @throws InvalidPropertyException 如果没有这样的属性或属性不可写
	 * @throws PropertyAccessException  如果属性有效但访问器方法失败或类型不匹配
	 */
	void setPropertyValue(String propertyName, @Nullable Object value) throws BeansException;

	/**
	 * 将指定值设置为当前属性值。
	 *
	 * @param pv 包含新属性值的对象
	 * @throws InvalidPropertyException 如果没有这样的属性或属性不可写
	 * @throws PropertyAccessException  如果属性有效但访问器方法失败或类型不匹配
	 */
	void setPropertyValue(PropertyValue pv) throws BeansException;

	/**
	 * 从 Map 执行批量更新。
	 * <p>从 PropertyValues 进行的批量更新更加强大：此方法提供了方便性。
	 * 行为将与 {@link #setPropertyValues(PropertyValues)} 方法相同。
	 *
	 * @param map 从中获取属性的 Map。包含以属性名称为键的属性值对象
	 * @throws InvalidPropertyException     如果没有这样的属性或属性不可写
	 * @throws PropertyBatchUpdateException 如果在批量更新期间对特定属性出现一个或多个 PropertyAccessExceptions。
	 *                                      此异常捆绑了所有单个 PropertyAccessExceptions。所有其他属性将已成功更新。
	 */
	void setPropertyValues(Map<?, ?> map) throws BeansException;

	/**
	 * 执行批量更新的首选方法。
	 * <p>请注意，执行批量更新与执行单个更新不同，因为在遇到<b>可恢复</b>错误（例如类型不匹配，但<b>不是</b>无效字段名称等）时，
	 * 此类的实现将继续更新属性，抛出包含所有个别错误的 {@link PropertyBatchUpdateException}。
	 * 稍后可以检查此异常以查看所有绑定错误。已成功更新的属性保持更改。
	 * <p>不允许未知字段或无效字段。
	 *
	 * @param pvs 要设置在目标对象上的 PropertyValues
	 * @throws InvalidPropertyException     如果没有这样的属性或属性不可写
	 * @throws PropertyBatchUpdateException 如果在批量更新期间对特定属性出现一个或多个 PropertyAccessExceptions。
	 *                                      此异常捆绑了所有单个 PropertyAccessExceptions。所有其他属性将已成功更新。
	 * @see #setPropertyValues(PropertyValues, boolean, boolean)
	 */
	void setPropertyValues(PropertyValues pvs) throws BeansException;

	/**
	 * 执行批量更新，并对行为进行更多控制。
	 * <p>请注意，执行批量更新与执行单个更新不同，因为此类的实现将继续更新属性，如果遇到<b>可恢复</b>错误
	 * （例如类型不匹配，但<b>不是</b>无效字段名称等），则会抛出一个 {@link PropertyBatchUpdateException}，其中包含所有单个错误。
	 * 稍后可以检查此异常以查看所有绑定错误。已成功更新的属性保持更改。
	 *
	 * @param pvs           要设置在目标对象上的 PropertyValues
	 * @param ignoreUnknown 是否应忽略未知属性（在 bean 中找不到）
	 * @throws InvalidPropertyException     如果没有这样的属性或如果属性不可写
	 * @throws PropertyBatchUpdateException 如果在批量更新期间对特定属性出现一个或多个 PropertyAccessExceptions。
	 *                                      此异常捆绑了所有单个 PropertyAccessExceptions。所有其他属性将已成功更新。
	 * @see #setPropertyValues(PropertyValues, boolean, boolean)
	 */
	void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown)
			throws BeansException;

	/**
	 * 执行具有完全控制的批量更新。
	 * <p>请注意，执行批量更新与执行单个更新不同，因为此类的实现将继续更新属性，如果遇到<b>可恢复</b>错误
	 * （例如类型不匹配，但<b>不是</b>无效字段名称等），则会抛出一个 {@link PropertyBatchUpdateException}，其中包含所有单个错误。
	 * 稍后可以检查此异常以查看所有绑定错误。已成功更新的属性保持更改。
	 *
	 * @param pvs           要设置在目标对象上的 PropertyValues
	 * @param ignoreUnknown 是否应忽略未知属性（在 bean 中找不到）
	 * @param ignoreInvalid 是否应忽略无效属性（找到但无法访问）
	 * @throws InvalidPropertyException     如果没有这样的属性或如果属性不可写
	 * @throws PropertyBatchUpdateException 如果在批量更新期间对特定属性出现一个或多个 PropertyAccessExceptions。
	 *                                      此异常捆绑了所有单个 PropertyAccessExceptions。所有其他属性将已成功更新。
	 */
	void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid)
			throws BeansException;

}
