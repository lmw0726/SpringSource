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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.beans.PropertyChangeEvent;

/**
 * 在尝试设置 bean 属性时发生类型不匹配时抛出的异常。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class TypeMismatchException extends PropertyAccessException {

	/**
	 * 类型不匹配错误将注册的错误代码。
	 */
	public static final String ERROR_CODE = "typeMismatch";

	/**
	 * 属性名称
	 */
	@Nullable
	private String propertyName;

	/**
	 * 属性值
	 */
	@Nullable
	private final transient Object value;

	/**
	 * 所需的目标类型
	 */
	@Nullable
	private final Class<?> requiredType;


	/**
	 * 创建一个新的 {@code TypeMismatchException}。
	 * @param propertyChangeEvent 导致问题的 PropertyChangeEvent
	 * @param requiredType 所需的目标类型
	 */
	public TypeMismatchException(PropertyChangeEvent propertyChangeEvent, Class<?> requiredType) {
		this(propertyChangeEvent, requiredType, null);
	}

	/**
	 * 创建一个新的 {@code TypeMismatchException}。
	 * @param propertyChangeEvent 导致问题的 PropertyChangeEvent
	 * @param requiredType 所需的目标类型（如果未知，则为 {@code null}）
	 * @param cause 根本原因（可能为 {@code null}）
	 */
	public TypeMismatchException(PropertyChangeEvent propertyChangeEvent, @Nullable Class<?> requiredType,
								 @Nullable Throwable cause) {

		super(propertyChangeEvent,
				"Failed to convert property value of type '" +
				ClassUtils.getDescriptiveType(propertyChangeEvent.getNewValue()) + "'" +
				(requiredType != null ?
				" to required type '" + ClassUtils.getQualifiedName(requiredType) + "'" : "") +
				(propertyChangeEvent.getPropertyName() != null ?
				" for property '" + propertyChangeEvent.getPropertyName() + "'" : ""),
				cause);
		this.propertyName = propertyChangeEvent.getPropertyName();
		this.value = propertyChangeEvent.getNewValue();
		this.requiredType = requiredType;
	}

	/**
	 * 创建一个没有 {@code PropertyChangeEvent} 的新的 {@code TypeMismatchException}。
	 * @param value 无法转换的有问题的值（可能为 {@code null}）
	 * @param requiredType 所需的目标类型（如果未知，则为 {@code null}）
	 * @see #initPropertyName
	 */
	public TypeMismatchException(@Nullable Object value, @Nullable Class<?> requiredType) {
		this(value, requiredType, null);
	}

	/**
	 * 创建一个没有 {@code PropertyChangeEvent} 的新的 {@code TypeMismatchException}。
	 * @param value 无法转换的有问题的值（可能为 {@code null}）
	 * @param requiredType 所需的目标类型（如果未知，则为 {@code null}）
	 * @param cause 根本原因（可能为 {@code null}）
	 * @see #initPropertyName
	 */
	public TypeMismatchException(@Nullable Object value, @Nullable Class<?> requiredType, @Nullable Throwable cause) {
		super("Failed to convert value of type '" + ClassUtils.getDescriptiveType(value) + "'" +
				(requiredType != null ? " to required type '" + ClassUtils.getQualifiedName(requiredType) + "'" : ""),
				cause);
		this.value = value;
		this.requiredType = requiredType;
	}


	/**
	 * 初始化此异常的属性名称，以便通过 {@link #getPropertyName()} 公开，
	 * 作为通过 {@link PropertyChangeEvent} 初始化的替代方法。
	 * @param propertyName 要公开的属性名称
	 * @since 5.0.4
	 * @see #TypeMismatchException(Object, Class)
	 * @see #TypeMismatchException(Object, Class, Throwable)
	 */
	public void initPropertyName(String propertyName) {
		Assert.state(this.propertyName == null, "Property name already initialized");
		this.propertyName = propertyName;
	}

	/**
	 * 返回受影响的属性名称（如果可用）。
	 */
	@Override
	@Nullable
	public String getPropertyName() {
		return this.propertyName;
	}

	/**
	 * 返回有问题的值（可能为 {@code null}）。
	 */
	@Override
	@Nullable
	public Object getValue() {
		return this.value;
	}

	/**
	 * 返回所需的目标类型（如果有）。
	 */
	@Nullable
	public Class<?> getRequiredType() {
		return this.requiredType;
	}

	@Override
	public String getErrorCode() {
		return ERROR_CODE;
	}

}
