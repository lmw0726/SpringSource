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

package org.springframework.util.unit;

/**
 * 一组标准的{@link DataSize}数据单位。
 *
 * <p>本类中使用的单位前缀是表示乘以2的幂次的
 * <a href="https://en.wikipedia.org/wiki/Binary_prefix">二进制前缀</a>。
 * 下表展示了本类中定义的枚举常量及其对应值：
 *
 * <p>
 * <table border="1">
 * <tr><th>常量</th><th>数据大小</th><th>2的幂次</th><th>字节大小</th></tr>
 * <tr><td>{@link #BYTES}</td><td>1B</td><td>2^0</td><td>1</td></tr>
 * <tr><td>{@link #KILOBYTES}</td><td>1KB</td><td>2^10</td><td>1,024</td></tr>
 * <tr><td>{@link #MEGABYTES}</td><td>1MB</td><td>2^20</td><td>1,048,576</td></tr>
 * <tr><td>{@link #GIGABYTES}</td><td>1GB</td><td>2^30</td><td>1,073,741,824</td></tr>
 * <tr><td>{@link #TERABYTES}</td><td>1TB</td><td>2^40</td><td>1,099,511,627,776</td></tr>
 * </table>
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 5.1
 * @see DataSize
 */
public enum DataUnit {

	/**
	 * 字节，用后缀{@code B}表示。
	 */
	BYTES("B", DataSize.ofBytes(1)),

	/**
	 * 千字节，用后缀{@code KB}表示。
	 */
	KILOBYTES("KB", DataSize.ofKilobytes(1)),

	/**
	 * 兆字节，用后缀{@code MB}表示。
	 */
	MEGABYTES("MB", DataSize.ofMegabytes(1)),

	/**
	 * 吉字节，用后缀{@code GB}表示。
	 */
	GIGABYTES("GB", DataSize.ofGigabytes(1)),

	/**
	 * 太字节，用后缀{@code TB}表示。
	 */
	TERABYTES("TB", DataSize.ofTerabytes(1));


	private final String suffix;

	private final DataSize size;


	DataUnit(String suffix, DataSize size) {
		this.suffix = suffix;
		this.size = size;
	}

	DataSize size() {
		return this.size;
	}

	/**
	 * 返回与指定{@code suffix}匹配的{@link DataUnit}。
	 * @param suffix 标准后缀之一
	 * @return 匹配指定{@code suffix}的{@link DataUnit}
	 * @throws IllegalArgumentException 如果后缀不匹配本枚举任何常量的后缀
	 */
	public static DataUnit fromSuffix(String suffix) {
		for (DataUnit candidate : values()) {
			if (candidate.suffix.equals(suffix)) {
				return candidate;
			}
		}
		throw new IllegalArgumentException("Unknown data unit suffix '" + suffix + "'");
	}

}
