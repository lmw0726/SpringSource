/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.core.convert.converter.ConverterRegistry;

import java.lang.annotation.Annotation;

/**
 * 字段格式化逻辑的注册表。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public interface FormatterRegistry extends ConverterRegistry {

	/**
	 * 添加一个Printer以打印特定类型的字段。
	 * 字段类型由参数化的Printer实例暗示。
	 *
	 * @param printer 要添加的打印机
	 * @see #addFormatter(Formatter)
	 * @since 5.2
	 */
	void addPrinter(Printer<?> printer);

	/**
	 * 添加一个Parser以解析特定类型的字段。
	 * 字段类型由参数化的Parser实例暗示。
	 *
	 * @param parser 要添加的解析器
	 * @see #addFormatter(Formatter)
	 * @since 5.2
	 */
	void addParser(Parser<?> parser);

	/**
	 * 添加一个Formatter以格式化特定类型的字段。
	 * 字段类型由参数化的Formatter实例暗示。
	 *
	 * @param formatter 要添加的格式化程序
	 * @see #addFormatterForFieldType(Class, Formatter)
	 * @since 3.1
	 */
	void addFormatter(Formatter<?> formatter);

	/**
	 * 添加一个Formatter以格式化给定类型的字段。
	 * <p>在打印时，如果Formatter的类型T已声明并且fieldType不可分配给T，则将尝试强制转换为T，然后再委托给formatter打印字段值。
	 * 在解析时，如果由formatter返回的解析对象不可分配给运行时字段类型，则将尝试强制转换为字段类型，然后再返回解析的字段值。
	 *
	 * @param fieldType 要格式化的字段类型
	 * @param formatter 要添加的格式化程序
	 */
	void addFormatterForFieldType(Class<?> fieldType, Formatter<?> formatter);

	/**
	 * 添加一个Printer/Parser对以格式化特定类型的字段。
	 * 格式化程序将委托给指定的打印机打印和指定的解析器解析。
	 * <p>在打印时，如果Printer的类型T已声明并且fieldType不可分配给T，则将尝试强制转换为T，然后再委托给printer打印字段值。
	 * 在解析时，如果解析器返回的对象不可分配给运行时字段类型，则将尝试强制转换为字段类型，然后再返回解析的字段值。
	 *
	 * @param fieldType 要格式化的字段类型
	 * @param printer   格式化程序的打印部分
	 * @param parser    格式化程序的解析部分
	 */
	void addFormatterForFieldType(Class<?> fieldType, Printer<?> printer, Parser<?> parser);

	/**
	 * 添加一个Formatter以格式化带有特定格式注释的字段。
	 *
	 * @param annotationFormatterFactory 要添加的注释格式化程序工厂
	 */
	void addFormatterForFieldAnnotation(AnnotationFormatterFactory<? extends Annotation> annotationFormatterFactory);

}
