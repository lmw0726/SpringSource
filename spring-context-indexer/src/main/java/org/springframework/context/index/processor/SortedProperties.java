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

package org.springframework.context.index.processor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * {@link Properties} 的特化实现，根据键名按字母数字顺序对属性进行排序。
 *
 * <p>在将 {@link Properties} 实例存储到属性文件时，这非常有用，
 * 因为它允许以可重复的方式生成此类文件，并保持属性的一致排序。
 *
 * <p>生成的属性文件中的注释也可以选择性地省略。
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
	 * 构造一个新的 {@code SortedProperties} 实例，并遵循提供的 {@code omitComments} 标志。
	 * @param omitComments 当存储属性到文件时是否应省略注释，{@code true} 表示省略
	 */
	SortedProperties(boolean omitComments) {
		this.omitComments = omitComments;
	}

	/**
	 * 构造一个新的 {@code SortedProperties} 实例，从提供的 {@link Properties} 对象填充属性，
	 * 并遵循提供的 {@code omitComments} 标志。
	 * <p>提供的 {@code Properties} 对象中的默认属性不会被复制。
	 * @param properties 用于复制初始属性的 {@code Properties} 对象
	 * @param omitComments 当存储属性到文件时是否应省略注释，{@code true} 表示省略
	 */
	SortedProperties(Properties properties, boolean omitComments) {
		this(omitComments);
		putAll(properties);
	}


	@Override
	public void store(OutputStream out, String comments) throws IOException {
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
	public void store(Writer writer, String comments) throws IOException {
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
	public void storeToXML(OutputStream out, String comments) throws IOException {
		super.storeToXML(out, (this.omitComments ? null : comments));
	}

	@Override
	public void storeToXML(OutputStream out, String comments, String encoding) throws IOException {
		super.storeToXML(out, (this.omitComments ? null : comments), encoding);
	}

	/**
	 * 返回此 {@link Properties} 对象中键的排序枚举。
	 * @see #keySet()
	 */
	@Override
	public synchronized Enumeration<Object> keys() {
		return Collections.enumeration(keySet());
	}

	/**
	 * 返回此 {@link Properties} 对象中键的排序集合。
	 * <p>如果需要，键将使用 {@link String#valueOf(Object)} 转换为字符串，
	 * 并根据字符串的自然顺序按字母数字顺序排序。
	 */
	@Override
	public Set<Object> keySet() {
		Set<Object> sortedKeys = new TreeSet<>(keyComparator);
		sortedKeys.addAll(super.keySet());
		return Collections.synchronizedSet(sortedKeys);
	}

	/**
	 * 返回此 {@link Properties} 对象中条目的排序集合。
	 * <p>条目将根据其键进行排序，如果需要，键将使用 {@link String#valueOf(Object)} 转换为字符串，
	 * 并根据字符串的自然顺序按字母数字顺序进行比较。
	 */
	@Override
	public Set<Entry<Object, Object>> entrySet() {
		Set<Entry<Object, Object>> sortedEntries = new TreeSet<>(entryComparator);
		sortedEntries.addAll(super.entrySet());
		return Collections.synchronizedSet(sortedEntries);
	}

}
