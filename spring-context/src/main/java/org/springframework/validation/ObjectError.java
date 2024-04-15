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

package org.springframework.validation;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 封装对象错误，即拒绝对象的全局原因。
 *
 * <p>有关如何为 {@code ObjectError} 构建消息代码列表的详细信息，请参见 {@link DefaultMessageCodesResolver} 的 javadoc。
 *
 * @author Juergen Hoeller
 * @see FieldError
 * @see DefaultMessageCodesResolver
 * @since 10.03.2003
 */
@SuppressWarnings("serial")
public class ObjectError extends DefaultMessageSourceResolvable {
	/**
	 * 对象名称
	 */
	private final String objectName;

	/**
	 * 源对象
	 */
	@Nullable
	private transient Object source;


	/**
	 * 创建 ObjectError 类的新实例。
	 *
	 * @param objectName     受影响对象的名称
	 * @param defaultMessage 用于解析此消息的默认消息
	 */
	public ObjectError(String objectName, String defaultMessage) {
		this(objectName, null, null, defaultMessage);
	}

	/**
	 * 创建 ObjectError 类的新实例。
	 *
	 * @param objectName     受影响对象的名称
	 * @param codes          用于解析此消息的代码
	 * @param arguments      用于解析此消息的参数数组
	 * @param defaultMessage 用于解析此消息的默认消息
	 */
	public ObjectError(
			String objectName, @Nullable String[] codes, @Nullable Object[] arguments, @Nullable String defaultMessage) {

		super(codes, arguments, defaultMessage);
		Assert.notNull(objectName, "Object name must not be null");
		this.objectName = objectName;
	}


	/**
	 * 返回受影响对象的名称。
	 */
	public String getObjectName() {
		return this.objectName;
	}

	/**
	 * 保留此错误背后的源对象：可能是一个{@link Exception}（通常是{@link org.springframework.beans.PropertyAccessException}），
	 * 或者是一个Bean Validation {@link javax.validation.ConstraintViolation}。
	 * <p>注意，任何这样的源对象都将被存储为瞬态：也就是说，它不会成为序列化错误表示的一部分。
	 *
	 * @param source 源对象
	 * @since 5.0.4
	 */
	public void wrap(Object source) {
		if (this.source != null) {
			throw new IllegalStateException("Already wrapping " + this.source);
		}
		this.source = source;
	}

	/**
	 * 解包此错误背后的源对象：可能是一个{@link Exception}（通常是{@link org.springframework.beans.PropertyAccessException}），
	 * 或者是一个Bean Validation {@link javax.validation.ConstraintViolation}。
	 * <p>最外层异常的原因也将被检查，例如，底层转换异常或从setter抛出的异常
	 * （而不必逐个解包{@code PropertyAccessException}）。
	 *
	 * @return 给定类型的源对象
	 * @throws IllegalArgumentException 如果没有可用的源对象（即未指定或在反序列化后不再可用）
	 * @since 5.0.4
	 */
	public <T> T unwrap(Class<T> sourceType) {
		if (sourceType.isInstance(this.source)) {
			return sourceType.cast(this.source);
		} else if (this.source instanceof Throwable) {
			Throwable cause = ((Throwable) this.source).getCause();
			if (sourceType.isInstance(cause)) {
				return sourceType.cast(cause);
			}
		}
		throw new IllegalArgumentException("No source object of the given type available: " + sourceType);
	}

	/**
	 * 检查此错误是否由给定类型的源对象引起：可能是一个{@link Exception}（通常是{@link org.springframework.beans.PropertyAccessException}），
	 * 或者是一个Bean Validation {@link javax.validation.ConstraintViolation}。
	 * <p>最外层异常的原因也将被检查，例如，底层转换异常或从setter抛出的异常
	 * （而不必逐个解包{@code PropertyAccessException}）。
	 *
	 * @return 是否此错误已由给定类型的源对象引起
	 * @since 5.0.4
	 */
	public boolean contains(Class<?> sourceType) {
		return (sourceType.isInstance(this.source) ||
				(this.source instanceof Throwable && sourceType.isInstance(((Throwable) this.source).getCause())));
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || other.getClass() != getClass() || !super.equals(other)) {
			return false;
		}
		ObjectError otherError = (ObjectError) other;
		return getObjectName().equals(otherError.getObjectName());
	}

	@Override
	public int hashCode() {
		return (29 * super.hashCode() + getObjectName().hashCode());
	}

	@Override
	public String toString() {
		return "Error in object '" + this.objectName + "': " + resolvableToString();
	}

}
