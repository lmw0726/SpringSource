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

package org.springframework.core;

import org.springframework.lang.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

/**
 * {@link Properties} 的特化实现，按照键的字母顺序进行排序。
 *
 * <p>当将 {@link Properties} 实例存储到属性文件中时，这会很有用，
 * 因为它允许生成的文件具有一致的属性顺序，从而可重复生成。
 *
 * <p>生成的属性文件中的注释也可以选择省略。
 *
 * @author Sam Brannen
 * @since 5.2
 * @see java.util.Properties
 */
@SuppressWarnings("serial")
class SortedProperties extends Properties {

	static final String EOL = System.lineSeparator();

	private static final Comparator<Object> keyComparator = Comparator.comparing(String::valueOf);

	private static final Comparator<Entry<Object, Object>> entryComparator = Entry.comparingByKey(keyComparator);


	private final boolean omitComments;


	/**
	 * 构造一个新的 {@code SortedProperties} 实例，并根据提供的
	 * {@code omitComments} 标志进行设置。
	 * @param omitComments 如果在将属性存储到文件时应省略注释，则为 {@code true}
	 */
	SortedProperties(boolean omitComments) {
		this.omitComments = omitComments;
	}

	/**
	 * 构造一个新的 {@code SortedProperties} 实例，并从提供的
	 * {@link Properties} 对象中填充属性，同时根据提供的
	 * {@code omitComments} 标志进行设置。
	 * <p>不会复制提供的 {@code Properties} 对象中的默认属性。
	 * @param properties 要复制初始属性的 {@code Properties} 对象
	 * @param omitComments 如果在将属性存储到文件时应省略注释，则为 {@code true}
	 */
	SortedProperties(Properties properties, boolean omitComments) {
		this(omitComments);
		putAll(properties);
	}


	@Override
	public void store(OutputStream out, @Nullable String comments) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		super.store(baos, (this.omitComments ? null : comments));
		String contents = baos.toString(StandardCharsets.ISO_8859_1.name());
		for (String line : contents.split(EOL)) {
			if (!(this.omitComments && line.startsWith("#"))) {
				out.write((line + EOL).getBytes(StandardCharsets.ISO_8859_1));
			}
		}
	}

	@Override
	public void store(Writer writer, @Nullable String comments) throws IOException {
		StringWriter stringWriter = new StringWriter();
		super.store(stringWriter, (this.omitComments ? null : comments));
		String contents = stringWriter.toString();
		for (String line : contents.split(EOL)) {
			if (!(this.omitComments && line.startsWith("#"))) {
				writer.write(line + EOL);
			}
		}
	}

	@Override
	public void storeToXML(OutputStream out, @Nullable String comments) throws IOException {
		super.storeToXML(out, (this.omitComments ? null : comments));
	}

	@Override
	public void storeToXML(OutputStream out, @Nullable String comments, String encoding) throws IOException {
		super.storeToXML(out, (this.omitComments ? null : comments), encoding);
	}

	/**
	 * 返回此 {@link Properties} 对象中已排序的键的枚举。
	 * @see #keySet()
	 */
	@Override
	public synchronized Enumeration<Object> keys() {
		return Collections.enumeration(keySet());
	}

	/**
	 * 返回此 {@link Properties} 对象中已排序的键的集合。
	 * <p>如有必要，会将键转换为字符串（使用 {@link String#valueOf(Object)}），
	 * 并根据字符串的自然顺序按字母顺序进行排序。
	 */
	@Override
	public Set<Object> keySet() {
		Set<Object> sortedKeys = new TreeSet<>(keyComparator);
		sortedKeys.addAll(super.keySet());
		return Collections.synchronizedSet(sortedKeys);
	}

	/**
	 * 返回此 {@link Properties} 对象中已排序的条目集合。
	 * <p>条目将基于其键进行排序，如有必要会将键转换为字符串
	 * （使用 {@link String#valueOf(Object)}），并根据字符串的自然顺序进行字母排序。
	 */
	@Override
	public Set<Entry<Object, Object>> entrySet() {
		Set<Entry<Object, Object>> sortedEntries = new TreeSet<>(entryComparator);
		sortedEntries.addAll(super.entrySet());
		return Collections.synchronizedSet(sortedEntries);
	}

}
