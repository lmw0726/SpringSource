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

package org.springframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

/**
 * 策略接口，用于持久化{@code java.util.Properties}，允许可插入的解析策略。
 *
 * 默认实现是DefaultPropertiesPersister，提供{@code java.util.Properties}的本机解析，
 * 但允许从任何阅读器读取和写入任何写入器（这允许为属性文件指定编码）。
 *
 * @author Juergen Hoeller
 * @see DefaultPropertiesPersister
 * @see org.springframework.core.io.support.ResourcePropertiesPersister
 * @see java.util.Properties
 * @since 2004-03-10
 */
public interface PropertiesPersister {

	/**
	 * 将属性从给定的InputStream加载到给定的属性对象中。
	 *
	 * @param props 要加载到的属性对象
	 * @param is    要从其中加载的InputStream
	 * @throws IOException 在IO错误的情况下
	 * @see java.util.Properties#load
	 */
	void load(Properties props, InputStream is) throws IOException;

	/**
	 * 将属性从给定的阅读器加载到给定的属性对象中。
	 *
	 * @param props  要加载到的属性对象
	 * @param reader 要加载的阅读器
	 * @throws IOException 在IO错误的情况下
	 */
	void load(Properties props, Reader reader) throws IOException;

	/**
	 * 将给定的Properties对象的内容写入给定的输出流。
	 *
	 * @param props  要存储的Properties对象
	 * @param os     要写入的输出流
	 * @param header 属性列表的描述
	 * @throws IOException 在I/O错误发生时
	 * @see java.util.Properties#store
	 */
	void store(Properties props, OutputStream os, String header) throws IOException;

	/**
	 * 将给定的Properties对象的内容写入给定的写入器。
	 *
	 * @param props  要存储的Properties对象
	 * @param writer 要写入的写入器
	 * @param header 属性列表的描述
	 * @throws IOException 在I/O错误发生时
	 */
	void store(Properties props, Writer writer, String header) throws IOException;

	/**
	 * 从给定的XML InputStream加载属性到给定的Properties对象中。
	 *
	 * @param props 要加载到的Properties对象
	 * @param is    要加载的InputStream
	 * @throws IOException 在I/O错误发生时
	 * @see java.util.Properties#loadFromXML(java.io.InputStream)
	 */
	void loadFromXml(Properties props, InputStream is) throws IOException;

	/**
	 * 将给定的Properties对象的内容写入给定的XML 输出流。
	 *
	 * @param props  要存储的Properties对象
	 * @param os     要写入的输出流
	 * @param header 属性列表的描述
	 * @throws IOException 在I/O错误发生时
	 * @see java.util.Properties#storeToXML(java.io.OutputStream, String)
	 */
	void storeToXml(Properties props, OutputStream os, String header) throws IOException;

	/**
	 * 将给定的Properties对象的内容写入给定的XML 输出流。
	 *
	 * @param props    要存储的Properties对象
	 * @param os       要写入的输出流
	 * @param encoding 要使用的编码
	 * @param header   属性列表的描述
	 * @throws IOException 在I/O错误发生时
	 * @see java.util.Properties#storeToXML(java.io.OutputStream, String, String)
	 */
	void storeToXml(Properties props, OutputStream os, String header, String encoding) throws IOException;

}
