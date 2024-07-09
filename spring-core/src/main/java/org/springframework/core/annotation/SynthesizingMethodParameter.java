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

package org.springframework.core.annotation;

import org.springframework.core.MethodParameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 通过 {@link AliasFor @AliasFor} 合成属性别名声明的 {@link MethodParameter} 变体。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see AnnotationUtils#synthesizeAnnotation
 * @see AnnotationUtils#synthesizeAnnotationArray
 * @since 4.2
 */
public class SynthesizingMethodParameter extends MethodParameter {

	/**
	 * 使用给定的方法创建一个新的 {@code SynthesizingMethodParameter}，嵌套级别为1。
	 *
	 * @param method         要指定参数的方法
	 * @param parameterIndex 参数的索引：-1 表示方法返回类型；0 表示第一个方法参数；1 表示第二个方法参数，依此类推。
	 */
	public SynthesizingMethodParameter(Method method, int parameterIndex) {
		super(method, parameterIndex);
	}

	/**
	 * 使用给定的方法创建一个新的 {@code SynthesizingMethodParameter}。
	 *
	 * @param method         要指定参数的方法
	 * @param parameterIndex 参数的索引：-1 表示方法返回类型；0 表示第一个方法参数；1 表示第二个方法参数，依此类推。
	 * @param nestingLevel   目标类型的嵌套级别
	 *                       （通常为1；例如，对于列表的列表，1 表示嵌套列表，2 表示嵌套列表的元素）
	 */
	public SynthesizingMethodParameter(Method method, int parameterIndex, int nestingLevel) {
		super(method, parameterIndex, nestingLevel);
	}

	/**
	 * 使用给定的构造函数创建一个新的 {@code SynthesizingMethodParameter}，嵌套级别为1。
	 *
	 * @param constructor    要指定参数的构造函数
	 * @param parameterIndex 参数的索引
	 */
	public SynthesizingMethodParameter(Constructor<?> constructor, int parameterIndex) {
		super(constructor, parameterIndex);
	}

	/**
	 * 使用给定的构造函数创建一个新的 {@code SynthesizingMethodParameter}。
	 *
	 * @param constructor    要指定参数的构造函数
	 * @param parameterIndex 参数的索引
	 * @param nestingLevel   目标类型的嵌套级别
	 *                       （通常为1；例如，对于列表的列表，1 表示嵌套列表，2 表示嵌套列表的元素）
	 */
	public SynthesizingMethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
		super(constructor, parameterIndex, nestingLevel);
	}

	/**
	 * 复制构造函数，创建一个独立的 {@code SynthesizingMethodParameter} 对象，
	 * 基于与原始对象相同的元数据和缓存状态。
	 *
	 * @param original 要复制的原始 SynthesizingMethodParameter 对象
	 */
	protected SynthesizingMethodParameter(SynthesizingMethodParameter original) {
		super(original);
	}


	@Override
	protected <A extends Annotation> A adaptAnnotation(A annotation) {
		return AnnotationUtils.synthesizeAnnotation(annotation, getAnnotatedElement());
	}

	@Override
	protected Annotation[] adaptAnnotationArray(Annotation[] annotations) {
		return AnnotationUtils.synthesizeAnnotationArray(annotations, getAnnotatedElement());
	}

	@Override
	public SynthesizingMethodParameter clone() {
		return new SynthesizingMethodParameter(this);
	}


	/**
	 * 为给定的方法或构造函数创建一个新的 SynthesizingMethodParameter。
	 * <p>这是一个便利的工厂方法，用于处理以通用方式处理方法或构造函数引用的场景。
	 *
	 * @param executable     要指定参数的方法或构造函数
	 * @param parameterIndex 参数的索引
	 * @return 相应的 SynthesizingMethodParameter 实例
	 * @since 5.0
	 */
	public static SynthesizingMethodParameter forExecutable(Executable executable, int parameterIndex) {
		if (executable instanceof Method) {
			return new SynthesizingMethodParameter((Method) executable, parameterIndex);
		} else if (executable instanceof Constructor) {
			return new SynthesizingMethodParameter((Constructor<?>) executable, parameterIndex);
		} else {
			throw new IllegalArgumentException("Not a Method/Constructor: " + executable);
		}
	}

	/**
	 * 为给定的参数描述符创建一个新的 SynthesizingMethodParameter。
	 * <p>这是一个便利的工厂方法，用于已经可用的 Java 8 {@link Parameter} 描述符的场景。
	 *
	 * @param parameter 参数描述符
	 * @return 相应的 SynthesizingMethodParameter 实例
	 * @since 5.0
	 */
	public static SynthesizingMethodParameter forParameter(Parameter parameter) {
		return forExecutable(parameter.getDeclaringExecutable(), findParameterIndex(parameter));
	}

}
