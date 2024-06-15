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

package org.springframework.http;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;

/**
 * 代表用于 HTTP {@code "Range"} 头的 HTTP（字节）范围。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see <a href="https://tools.ietf.org/html/rfc7233">HTTP/1.1: Range Requests</a>
 * @see HttpHeaders#setRange(List)
 * @see HttpHeaders#getRange()
 * @since 4.2
 */
public abstract class HttpRange {

	/**
	 * 每个请求的最大范围数。
	 */
	private static final int MAX_RANGES = 100;

	/**
	 * byte范围前缀
	 */
	private static final String BYTE_RANGE_PREFIX = "bytes=";


	/**
	 * 将给定的 {@code Resource} 使用当前 {@code HttpRange} 中包含的范围信息转换为 {@link ResourceRegion}。
	 *
	 * @param resource 要从中选择区域的 {@code Resource}
	 * @return 给定 {@code Resource} 的选定区域
	 * @since 4.3
	 */
	public ResourceRegion toResourceRegion(Resource resource) {
		// 不要尝试在 InputStreamResource 上确定 contentLength - 之后无法再次读取...
		// 注意: 自定义的 InputStreamResource 子类可以提供预先计算的 content length！
		Assert.isTrue(resource.getClass() != InputStreamResource.class,
				"Cannot convert an InputStreamResource to a ResourceRegion");
		// 获取资源的长度
		long contentLength = getLengthFor(resource);

		// 根据资源长度获取范围的起始位置
		long start = getRangeStart(contentLength);

		// 根据资源长度获取范围的结束位置
		long end = getRangeEnd(contentLength);

		// 确保起始位置小于资源的长度，否则抛出断言异常
		Assert.isTrue(start < contentLength, "'position' exceeds the resource length " + contentLength);

		// 返回一个包含指定范围的资源区域对象
		return new ResourceRegion(resource, start, end - start + 1);
	}

	/**
	 * 给定表示的总长度，返回范围的起始位置。
	 *
	 * @param length 表示的长度
	 * @return 表示的范围的起始位置
	 */
	public abstract long getRangeStart(long length);

	/**
	 * 给定表示的总长度，返回范围的结束位置（包含）。
	 *
	 * @param length 表示的长度
	 * @return 表示的范围的结束位置
	 */
	public abstract long getRangeEnd(long length);


	/**
	 * 从给定的位置到末尾创建一个 {@code HttpRange}。
	 *
	 * @param firstBytePos 第一个字节位置
	 * @return 一个范围从 {@code firstPos} 到末尾的字节范围
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 */
	public static HttpRange createByteRange(long firstBytePos) {
		return new ByteRange(firstBytePos, null);
	}

	/**
	 * 从给定的第一个到最后一个位置创建一个 {@code HttpRange}。
	 *
	 * @param firstBytePos 第一个字节位置
	 * @param lastBytePos  最后一个字节位置
	 * @return 一个范围从 {@code firstPos} 到 {@code lastPos} 的字节范围
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 */
	public static HttpRange createByteRange(long firstBytePos, long lastBytePos) {
		return new ByteRange(firstBytePos, lastBytePos);
	}

	/**
	 * 创建一个从最后给定的字节数开始的 {@code HttpRange}。
	 *
	 * @param suffixLength 范围中的字节数
	 * @return 一个范围从最后 {@code suffixLength} 数量的字节开始的字节范围
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">Byte Ranges</a>
	 */
	public static HttpRange createSuffixRange(long suffixLength) {
		return new SuffixByteRange(suffixLength);
	}

	/**
	 * 将给定的以逗号分隔的字符串解析为 {@code HttpRange} 对象的列表。
	 * <p>此方法可用于解析 {@code Range} 头。
	 *
	 * @param ranges 要解析的字符串
	 * @return 范围的列表
	 * @throws IllegalArgumentException 如果无法解析字符串或范围数量大于 100
	 */
	public static List<HttpRange> parseRanges(@Nullable String ranges) {
		// 如果范围字符串为空或者长度为0，返回空列表
		if (!StringUtils.hasLength(ranges)) {
			return Collections.emptyList();
		}

		// 如果范围字符串不以"bytes="开头，抛出异常
		if (!ranges.startsWith(BYTE_RANGE_PREFIX)) {
			throw new IllegalArgumentException("Range '" + ranges + "' does not start with 'bytes='");
		}

		// 去除范围字符串的"bytes="前缀
		ranges = ranges.substring(BYTE_RANGE_PREFIX.length());

		// 将范围字符串按逗号分隔成数组
		String[] tokens = StringUtils.tokenizeToStringArray(ranges, ",");

		// 如果范围数组的长度超过最大允许的范围数，抛出异常
		if (tokens.length > MAX_RANGES) {
			throw new IllegalArgumentException("Too many ranges: " + tokens.length);
		}

		// 创建一个空的 HttpRange 列表，用于存储解析后的范围信息
		List<HttpRange> result = new ArrayList<>(tokens.length);

		// 遍历范围数组，逐个解析并添加到结果列表中
		for (String token : tokens) {
			result.add(parseRange(token));
		}

		// 返回解析后的范围列表
		return result;
	}

	private static HttpRange parseRange(String range) {
		Assert.hasLength(range, "Range String must not be empty");
		// 查找 范围字符串 中的第一个连字符 "-" 的索引位置
		int dashIdx = range.indexOf('-');

		// 如果找到了连字符 "-"，且不在字符串开头
		if (dashIdx > 0) {
			// 解析连字符 "-" 前面的部分为起始位置
			long firstPos = Long.parseLong(range.substring(0, dashIdx));

			// 如果连字符 "-" 后面还有内容
			if (dashIdx < range.length() - 1) {
				// 解析连字符 "-" 后面的部分为结束位置
				Long lastPos = Long.parseLong(range.substring(dashIdx + 1));
				// 创建并返回 ByteRange 对象
				return new ByteRange(firstPos, lastPos);
			} else {
				// 创建并返回没有结束位置的 ByteRange 对象
				return new ByteRange(firstPos, null);
			}
		} else if (dashIdx == 0) {
			// 如果连字符 "-" 在字符串开头
			// 解析从第二个字符开始的部分为后缀长度
			long suffixLength = Long.parseLong(range.substring(1));
			// 创建并返回 SuffixByteRange 对象
			return new SuffixByteRange(suffixLength);
		} else {
			// 如果字符串中不包含连字符 "-"，抛出异常
			throw new IllegalArgumentException("Range '" + range + "' does not contain \"-\"");
		}
	}

	/**
	 * 将每个 {@code HttpRange} 转换为 {@code ResourceRegion}，使用 HTTP 范围信息选择给定 {@code Resource} 的适当部分。
	 *
	 * @param ranges   范围列表
	 * @param resource 要从中选择区域的资源
	 * @return 给定资源的区域列表
	 * @throws IllegalArgumentException 如果所有范围的总和超过资源长度
	 * @since 4.3
	 */
	public static List<ResourceRegion> toResourceRegions(List<HttpRange> ranges, Resource resource) {
		// 如果 范围 列表为空，则返回一个空列表
		if (CollectionUtils.isEmpty(ranges)) {
			return Collections.emptyList();
		}

		// 创建 区域列表，大小与 范围列表 相同
		List<ResourceRegion> regions = new ArrayList<>(ranges.size());
		// 遍历范围列表
		for (HttpRange range : ranges) {
			// 将每个 Http范围 转换为 ResourceRegion 并添加到 区域 列表中
			regions.add(range.toResourceRegion(resource));
		}

		// 如果 范围列表 中有多个范围，则进行以下检查
		if (ranges.size() > 1) {
			// 获取资源的总长度
			long length = getLengthFor(resource);

			// 初始化总计变量
			long total = 0;

			// 计算所有范围的总计长度
			for (ResourceRegion region : regions) {
				total += region.getCount();
			}

			// 如果所有范围的总计长度大于或等于资源的总长度，则抛出异常
			if (total >= length) {
				throw new IllegalArgumentException("The sum of all ranges (" + total +
						") should be less than the resource length (" + length + ")");
			}
		}

		// 返回计算得到的 区域列表
		return regions;
	}

	private static long getLengthFor(Resource resource) {
		try {
			// 获取资源的内容长度
			long contentLength = resource.contentLength();

			// 断言内容长度必须大于 0，如果不满足则抛出异常
			Assert.isTrue(contentLength > 0, "Resource content length should be > 0");

			// 返回内容长度
			return contentLength;
		} catch (IOException ex) {
			// 捕获 IO 异常，并抛出 IllegalArgumentException 异常
			throw new IllegalArgumentException("Failed to obtain Resource content length", ex);
		}
	}

	/**
	 * 返回给定的 {@code HttpRange} 对象列表的字符串表示。
	 * <p>此方法可用于生成 {@code Range} 头。
	 *
	 * @param ranges 要生成字符串的范围集合
	 * @return 字符串表示
	 */
	public static String toString(Collection<HttpRange> ranges) {
		Assert.notEmpty(ranges, "Ranges Collection must not be empty");
		// 创建一个 字符串连接器 对象，用逗号和空格做分隔符，前缀为 “bytes=”，后缀为空字符串
		StringJoiner builder = new StringJoiner(", ", BYTE_RANGE_PREFIX, "");

		// 遍历 范围 列表中的每个 Http范围 对象
		for (HttpRange range : ranges) {
			// 将每个 Http范围 对象的字符串表示形式添加到 字符串连接器 中
			builder.add(range.toString());
		}

		// 返回拼接完成的字符串
		return builder.toString();
	}


	/**
	 * 表示 HTTP/1.1 的字节范围，包含一个起始位置和一个可选的结束位置。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">字节范围</a>
	 * @see HttpRange#createByteRange(long)
	 * @see HttpRange#createByteRange(long, long)
	 */
	private static class ByteRange extends HttpRange {
		/**
		 * 第一个byte位置
		 */
		private final long firstPos;

		/**
		 * 最后一个byte位置
		 */
		@Nullable
		private final Long lastPos;

		public ByteRange(long firstPos, @Nullable Long lastPos) {
			assertPositions(firstPos, lastPos);
			this.firstPos = firstPos;
			this.lastPos = lastPos;
		}

		private void assertPositions(long firstBytePos, @Nullable Long lastBytePos) {
			if (firstBytePos < 0) {
				// 如果第一个byte位置小于0，抛出异常
				throw new IllegalArgumentException("Invalid first byte position: " + firstBytePos);
			}
			if (lastBytePos != null && lastBytePos < firstBytePos) {
				// 如果最后一个byte位置存在，且该位置小于第一个byte位置，抛出异常。
				throw new IllegalArgumentException("firstBytePosition=" + firstBytePos +
						" should be less then or equal to lastBytePosition=" + lastBytePos);
			}
		}

		@Override
		public long getRangeStart(long length) {
			return this.firstPos;
		}

		@Override
		public long getRangeEnd(long length) {
			if (this.lastPos != null && this.lastPos < length) {
				// 如果最后一个byte位置存在，且该位置小于资源长度，返回最后一个byte位置
				return this.lastPos;
			} else {
				// 否则返回长度减一
				return length - 1;
			}
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ByteRange)) {
				return false;
			}
			ByteRange otherRange = (ByteRange) other;
			return (this.firstPos == otherRange.firstPos &&
					ObjectUtils.nullSafeEquals(this.lastPos, otherRange.lastPos));
		}

		@Override
		public int hashCode() {
			return (ObjectUtils.nullSafeHashCode(this.firstPos) * 31 +
					ObjectUtils.nullSafeHashCode(this.lastPos));
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append(this.firstPos);
			builder.append('-');
			if (this.lastPos != null) {
				builder.append(this.lastPos);
			}
			return builder.toString();
		}
	}


	/**
	 * 表示 HTTP/1.1 的后缀字节范围，包含一定数量的后缀字节。
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-2.1">字节范围</a>
	 * @see HttpRange#createSuffixRange(long)
	 */
	private static class SuffixByteRange extends HttpRange {
		/**
		 * 后缀长度
		 */
		private final long suffixLength;

		public SuffixByteRange(long suffixLength) {
			// 如果 后缀长度 小于 0
			if (suffixLength < 0) {
				// 抛出 IllegalArgumentException 异常，并显示错误信息
				throw new IllegalArgumentException("Invalid suffix length: " + suffixLength);
			}

			this.suffixLength = suffixLength;
		}

		@Override
		public long getRangeStart(long length) {
			// 如果对象的 后缀长度 小于传入的 长度
			if (this.suffixLength < length) {
				// 返回 长度 减去 后缀长度 的结果
				return length - this.suffixLength;
			} else {
				// 否则返回 0
				return 0;
			}
		}

		@Override
		public long getRangeEnd(long length) {
			return length - 1;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof SuffixByteRange)) {
				return false;
			}
			SuffixByteRange otherRange = (SuffixByteRange) other;
			return (this.suffixLength == otherRange.suffixLength);
		}

		@Override
		public int hashCode() {
			return Long.hashCode(this.suffixLength);
		}

		@Override
		public String toString() {
			return "-" + this.suffixLength;
		}
	}

}
