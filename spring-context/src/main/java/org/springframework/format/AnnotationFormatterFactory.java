/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.format;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * 创建格式化程序以格式化使用特定{@link Annotation}注释的字段的值的工厂。
 *
 * 例如，{@code DateTimeFormatAnnotationFormatterFactory} 可能会创建一个格式化器，
 * 用于格式化使用 {@code @DateTimeFormat} 注释的字段上设置的 {@code Date} 值。
 *
 * @author Keith Donald
 * @since 3.0
 * @param <A> 应触发格式化的注释类型
 */
public interface AnnotationFormatterFactory<A extends Annotation> {

	/**
	 * 可以用&lt;A&gt;注释的字段的类型。
	 */
	Set<Class<?>> getFieldTypes();

	/**
	 * 获取用于打印 {@code fieldType} 字段的值的打印机，该字段带有 {@code annotation} 注释。
	 * <p>如果打印机接受的类型 T 不能赋值给 {@code fieldType}，则在调用打印机之前将尝试从 {@code fieldType} 到 T 的强制转换。
	 *
	 * @param annotation 注释实例
	 * @param fieldType  被注释的字段的类型
	 * @return 打印机
	 */
	Printer<?> getPrinter(A annotation, Class<?> fieldType);

	/**
	 * 获取解析器以解析用于带有 {@code annotation} 注释的 {@code fieldType} 字段的提交值。
	 * <p>如果解析器返回的对象不能赋值给 {@code fieldType}，则在设置字段之前将尝试强制转换为 {@code fieldType}。
	 *
	 * @param annotation 注释实例
	 * @param fieldType  被注释的字段的类型
	 * @return 解析器
	 */
	Parser<?> getParser(A annotation, Class<?> fieldType);

}
