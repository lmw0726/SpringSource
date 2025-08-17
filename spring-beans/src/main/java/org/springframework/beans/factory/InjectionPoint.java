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

package org.springframework.beans.factory;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;

/**
 * 注入点的简单描述符，指向方法/构造函数参数或字段。
 * 由{@link UnsatisfiedDependencyException}暴露。
 * 也可作为工厂方法的参数，响应请求的注入点以构建定制的bean实例。
 *
 * @author Juergen Hoeller
 * @since 4.3
 * @see UnsatisfiedDependencyException#getInjectionPoint()
 * @see org.springframework.beans.factory.config.DependencyDescriptor
 */
public class InjectionPoint {

	@Nullable
	protected MethodParameter methodParameter;

	@Nullable
	protected Field field;

	@Nullable
	private volatile Annotation[] fieldAnnotations;


	/**
	 * 为方法或构造函数参数创建注入点描述符。
	 * @param methodParameter 要包装的MethodParameter
	 */
	public InjectionPoint(MethodParameter methodParameter) {
		Assert.notNull(methodParameter, "MethodParameter must not be null");
		this.methodParameter = methodParameter;
	}

	/**
	 * 为字段创建注入点描述符。
	 * @param field 要包装的字段
	 */
	public InjectionPoint(Field field) {
		Assert.notNull(field, "Field must not be null");
		this.field = field;
	}

	/**
	 * 复制构造函数。
	 * @param original 要创建副本的原始描述符
	 */
	protected InjectionPoint(InjectionPoint original) {
		this.methodParameter = (original.methodParameter != null ?
				new MethodParameter(original.methodParameter) : null);
		this.field = original.field;
		this.fieldAnnotations = original.fieldAnnotations;
	}

	/**
	 * 仅用于子类中的序列化目的。
	 */
	protected InjectionPoint() {
	}


	/**
	 * 返回包装的MethodParameter（如果有的话）。
	 * <p>注意：MethodParameter或Field二者之一可用。
	 * @return MethodParameter，如果没有则返回{@code null}
	 */
	@Nullable
	public MethodParameter getMethodParameter() {
		return this.methodParameter;
	}

	/**
	 * 返回包装的Field（如果有的话）。
	 * <p>注意：MethodParameter或Field二者之一可用。
	 * @return Field，如果没有则返回{@code null}
	 */
	@Nullable
	public Field getField() {
		return this.field;
	}

	/**
	 * 返回包装的MethodParameter，假设它存在。
	 * @return MethodParameter（永不为{@code null}）
	 * @throws IllegalStateException 如果没有可用的MethodParameter
	 * @since 5.0
	 */
	protected final MethodParameter obtainMethodParameter() {
		Assert.state(this.methodParameter != null, "Neither Field nor MethodParameter");
		return this.methodParameter;
	}

	/**
	 * 获取与包装字段或方法/构造函数参数关联的注解。
	 */
	public Annotation[] getAnnotations() {
		if (this.field != null) {
			Annotation[] fieldAnnotations = this.fieldAnnotations;
			if (fieldAnnotations == null) {
				fieldAnnotations = this.field.getAnnotations();
				this.fieldAnnotations = fieldAnnotations;
			}
			return fieldAnnotations;
		}
		else {
			return obtainMethodParameter().getParameterAnnotations();
		}
	}

	/**
	 * 检索给定类型的字段/参数注解（如果有的话）。
	 * @param annotationType 要检索的注解类型
	 * @return 注解实例，如果未找到则返回{@code null}
	 * @since 4.3.9
	 */
	@Nullable
	public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
		return (this.field != null ? this.field.getAnnotation(annotationType) :
				obtainMethodParameter().getParameterAnnotation(annotationType));
	}

	/**
	 * 返回由底层字段或方法/构造函数参数声明的类型，表示注入类型。
	 */
	public Class<?> getDeclaredType() {
		return (this.field != null ? this.field.getType() : obtainMethodParameter().getParameterType());
	}

	/**
	 * 返回包装的成员，包含注入点。
	 * @return 作为Member的Field / Method / Constructor
	 */
	public Member getMember() {
		return (this.field != null ? this.field : obtainMethodParameter().getMember());
	}

	/**
	 * 返回包装的带注解元素。
	 * <p>注意：在方法/构造函数参数的情况下，这会暴露在方法或构造函数本身上
	 * 声明的注解（即在方法/构造函数级别，而不是在参数级别）。
	 * 在这种场景下，使用{@link #getAnnotations()}来获取参数级别的注解，
	 * 与相应的字段注解透明地配合。
	 * @return 作为AnnotatedElement的Field / Method / Constructor
	 */
	public AnnotatedElement getAnnotatedElement() {
		return (this.field != null ? this.field : obtainMethodParameter().getAnnotatedElement());
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		InjectionPoint otherPoint = (InjectionPoint) other;
		return (ObjectUtils.nullSafeEquals(this.field, otherPoint.field) &&
				ObjectUtils.nullSafeEquals(this.methodParameter, otherPoint.methodParameter));
	}

	@Override
	public int hashCode() {
		return (this.field != null ? this.field.hashCode() : ObjectUtils.nullSafeHashCode(this.methodParameter));
	}

	@Override
	public String toString() {
		return (this.field != null ? "field '" + this.field.getName() + "'" : String.valueOf(this.methodParameter));
	}

}
