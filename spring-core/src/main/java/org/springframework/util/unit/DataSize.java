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

package org.springframework.util.unit;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表示数据大小的类，如'12MB'。
 *
 * <p>此类以字节为单位建模数据大小，是不可变且线程安全的。
 *
 * <p>本类使用的术语和单位基于表示乘以2的幂次的
 * <a href="https://en.wikipedia.org/wiki/Binary_prefix">二进制前缀</a>。
 * 详情请参考下表及{@link DataUnit}的Javadoc。
 *
 * <p>
 * <table border="1">
 * <tr><th>术语</th><th>数据大小</th><th>字节大小</th></tr>
 * <tr><td>字节</td><td>1B</td><td>1</td></tr>
 * <tr><td>千字节</td><td>1KB</td><td>1,024</td></tr>
 * <tr><td>兆字节</td><td>1MB</td><td>1,048,576</td></tr>
 * <tr><td>吉字节</td><td>1GB</td><td>1,073,741,824</td></tr>
 * <tr><td>太字节</td><td>1TB</td><td>1,099,511,627,776</td></tr>
 * </table>
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 5.1
 * @see DataUnit
 */
@SuppressWarnings("serial")
public final class DataSize implements Comparable<DataSize>, Serializable {

	/**
	 * 每千字节的字节数。
	 */
	private static final long BYTES_PER_KB = 1024;

	/**
	 * 每兆字节的字节数。
	 */
	private static final long BYTES_PER_MB = BYTES_PER_KB * 1024;

	/**
	 * 每吉字节的字节数。
	 */
	private static final long BYTES_PER_GB = BYTES_PER_MB * 1024;

	/**
	 * 每太字节的字节数。
	 */
	private static final long BYTES_PER_TB = BYTES_PER_GB * 1024;


	private final long bytes;


	private DataSize(long bytes) {
		this.bytes = bytes;
	}


	/**
	 * 获取表示指定字节数的{@link DataSize}。
	 * @param bytes 字节数，可为正或负
	 * @return 对应的{@link DataSize}实例
	 */
	public static DataSize ofBytes(long bytes) {
		return new DataSize(bytes);
	}

	/**
	 * 获取表示指定千字节数的{@link DataSize}。
	 * @param kilobytes 千字节数，可为正或负
	 * @return 对应的{@link DataSize}实例
	 */
	public static DataSize ofKilobytes(long kilobytes) {
		return new DataSize(Math.multiplyExact(kilobytes, BYTES_PER_KB));
	}

	/**
	 * 获取表示指定兆字节数的{@link DataSize}。
	 * @param megabytes 兆字节数，可为正或负
	 * @return 对应的{@link DataSize}实例
	 */
	public static DataSize ofMegabytes(long megabytes) {
		return new DataSize(Math.multiplyExact(megabytes, BYTES_PER_MB));
	}

	/**
	 * 获取表示指定吉字节数的{@link DataSize}。
	 * @param gigabytes 吉字节数，可为正或负
	 * @return 对应的{@link DataSize}实例
	 */
	public static DataSize ofGigabytes(long gigabytes) {
		return new DataSize(Math.multiplyExact(gigabytes, BYTES_PER_GB));
	}

	/**
	 * 获取表示指定太字节数的{@link DataSize}。
	 * @param terabytes 太字节数，可为正或负
	 * @return 对应的{@link DataSize}实例
	 */
	public static DataSize ofTerabytes(long terabytes) {
		return new DataSize(Math.multiplyExact(terabytes, BYTES_PER_TB));
	}

	/**
	 * 获取表示指定单位和数量的{@link DataSize}。
	 * @param amount 大小数值，以指定单位计量，可为正或负
	 * @param unit 计量单位
	 * @return 对应的{@link DataSize}实例
	 * @throws IllegalArgumentException 如果单位为null
	 */
	public static DataSize of(long amount, DataUnit unit) {
		Assert.notNull(unit, "Unit must not be null");
		return new DataSize(Math.multiplyExact(amount, unit.size().toBytes()));
	}

	/**
	 * 从文本字符串(如{@code 12MB})解析获取{@link DataSize}，
	 * 如果未指定单位则使用{@link DataUnit#BYTES}。
	 * <p>
	 * 示例:
	 * <pre>
	 * "12KB" -- 解析为"12千字节"
	 * "5MB"  -- 解析为"5兆字节"
	 * "20"   -- 解析为"20字节"
	 * </pre>
	 * @param text 要解析的文本
	 * @return 解析后的{@link DataSize}
	 * @see #parse(CharSequence, DataUnit)
	 */
	public static DataSize parse(CharSequence text) {
		return parse(text, null);
	}

	/**
	 * 从文本字符串(如{@code 12MB})解析获取{@link DataSize}，
	 * 如果未指定单位则使用指定的默认{@link DataUnit}。
	 * <p>
	 * 字符串以数字开头，后跟可选的单位(匹配支持的{@linkplain DataUnit 后缀}之一)。
	 * <p>
	 * 示例:
	 * <pre>
	 * "12KB" -- 解析为"12千字节"
	 * "5MB"  -- 解析为"5兆字节"
	 * "20"   -- 解析为"20千字节" (当{@code defaultUnit}为{@link DataUnit#KILOBYTES}时)
	 * </pre>
	 * @param text 要解析的文本
	 * @param defaultUnit 默认单位(可为null)
	 * @return 解析后的{@link DataSize}
	 */
	public static DataSize parse(CharSequence text, @Nullable DataUnit defaultUnit) {
		Assert.notNull(text, "Text must not be null");
		try {
			Matcher matcher = DataSizeUtils.PATTERN.matcher(text);
			Assert.state(matcher.matches(), "Does not match data size pattern");
			DataUnit unit = DataSizeUtils.determineDataUnit(matcher.group(2), defaultUnit);
			long amount = Long.parseLong(matcher.group(1));
			return DataSize.of(amount, unit);
		}
		catch (Exception ex) {
			throw new IllegalArgumentException("'" + text + "' is not a valid data size", ex);
		}
	}

	/**
	 * 检查当前大小是否为负数（不包括零）。
	 * @return 如果当前大小小于零字节则返回true
	 */
	public boolean isNegative() {
		return this.bytes < 0;
	}

	/**
	 * 返回当前实例的字节数。
	 * @return 字节数
	 */
	public long toBytes() {
		return this.bytes;
	}

	/**
	 * 返回当前实例的千字节数。
	 * @return 千字节数
	 */
	public long toKilobytes() {
		return this.bytes / BYTES_PER_KB;
	}

	/**
	 * 返回当前实例的兆字节数。
	 * @return 兆字节数
	 */
	public long toMegabytes() {
		return this.bytes / BYTES_PER_MB;
	}

	/**
	 * 返回当前实例的吉字节数。
	 * @return 吉字节数
	 */
	public long toGigabytes() {
		return this.bytes / BYTES_PER_GB;
	}

	/**
	 * 返回当前实例的太字节数。
	 * @return 太字节数
	 */
	public long toTerabytes() {
		return this.bytes / BYTES_PER_TB;
	}

	@Override
	public int compareTo(DataSize other) {
		return Long.compare(this.bytes, other.bytes);
	}

	@Override
	public String toString() {
		return String.format("%dB", this.bytes);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		DataSize otherSize = (DataSize) other;
		return (this.bytes == otherSize.bytes);
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.bytes);
	}


	/**
	 * 静态嵌套类，用于支持{@link #PATTERN}的延迟加载。
	 * @since 5.3.21
	 */
	private static class DataSizeUtils {

		/**
		 * 用于解析的正则表达式模式。
		 */
		private static final Pattern PATTERN = Pattern.compile("^([+\\-]?\\d+)([a-zA-Z]{0,2})$");

		private static DataUnit determineDataUnit(String suffix, @Nullable DataUnit defaultUnit) {
			DataUnit defaultUnitToUse = (defaultUnit != null ? defaultUnit : DataUnit.BYTES);
			return (StringUtils.hasLength(suffix) ? DataUnit.fromSuffix(suffix) : defaultUnitToUse);
		}

	}

}
