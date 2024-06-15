/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http.server;

import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link PathContainer}的默认实现。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
final class DefaultPathContainer implements PathContainer {
	/**
	 * 空路径的路径容器
	 */
	private static final PathContainer EMPTY_PATH = new DefaultPathContainer("", Collections.emptyList());

	/**
	 * 字符集和分隔符映射
	 */
	private static final Map<Character, DefaultSeparator> SEPARATORS = new HashMap<>(2);

	static {
		// 添加 ‘/’ 和 '.' 两种分隔符
		SEPARATORS.put('/', new DefaultSeparator('/', "%2F"));
		SEPARATORS.put('.', new DefaultSeparator('.', "%2E"));
	}

	/**
	 * 路径
	 */
	private final String path;

	/**
	 * 元素列表
	 */
	private final List<Element> elements;


	private DefaultPathContainer(String path, List<Element> elements) {
		this.path = path;
		this.elements = Collections.unmodifiableList(elements);
	}


	@Override
	public String value() {
		return this.path;
	}

	@Override
	public List<Element> elements() {
		return this.elements;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PathContainer)) {
			return false;
		}
		return value().equals(((PathContainer) other).value());
	}

	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

	@Override
	public String toString() {
		return value();
	}


	static PathContainer createFromUrlPath(String path, Options options) {
		// 如果路径字符串为空，则返回一个表示空路径的路径容器
		if (path.isEmpty()) {
			return EMPTY_PATH;
		}

		// 获取路径分隔符
		char separator = options.separator();

		// 根据分隔符获取对应的分隔符元素
		DefaultSeparator separatorElement = SEPARATORS.get(separator);

		// 如果分隔符元素为 null，则抛出异常，表示分隔符不合法
		if (separatorElement == null) {
			throw new IllegalArgumentException("Unexpected separator: '" + separator + "'");
		}

		// 创建一个空的元素列表，用于存储路径的各个部分
		List<Element> elements = new ArrayList<>();

		// 初始化起始位置
		int begin;

		// 如果路径的第一个字符是分隔符
		if (path.charAt(0) == separator) {
			// 则从第二个字符开始处理
			begin = 1;
			// 并添加一个分隔符元素到元素列表中
			elements.add(separatorElement);
		} else {
			// 否则从第一个字符开始处理
			begin = 0;
		}

		// 循环处理路径字符串
		while (begin < path.length()) {
			// 查找下一个分隔符的位置
			int end = path.indexOf(separator, begin);

			// 提取当前段的字符串
			String segment = (end != -1 ? path.substring(begin, end) : path.substring(begin));

			// 如果段字符串不为空，则根据选项判断是否需要解码和解析段
			if (!segment.isEmpty()) {
				elements.add(options.shouldDecodeAndParseSegments() ?
						decodeAndParsePathSegment(segment) :
						DefaultPathSegment.from(segment, separatorElement));
			}

			// 如果已经到达路径末尾，则结束循环
			if (end == -1) {
				break;
			}

			// 添加分隔符元素到元素列表中
			elements.add(separatorElement);

			// 更新起始位置为下一个段的起始位置
			begin = end + 1;
		}

		// 返回一个新的默认路径容器，包含原始路径字符串和处理后的元素列表
		return new DefaultPathContainer(path, elements);
	}

	private static PathSegment decodeAndParsePathSegment(String segment) {
		// 指定字符集为 UTF-8
		Charset charset = StandardCharsets.UTF_8;

		// 查找路径段中的分号位置
		int index = segment.indexOf(';');

		// 如果没有找到分号，则说明路径段不包含参数
		if (index == -1) {
			// 对路径段进行 URI 解码，并作为匹配值创建默认路径段对象
			String valueToMatch = StringUtils.uriDecode(segment, charset);
			return DefaultPathSegment.from(segment, valueToMatch);
		} else {
			// 如果找到分号，则分割路径段为值和参数部分
			// 解码分号前的部分作为匹配值
			String valueToMatch = StringUtils.uriDecode(segment.substring(0, index), charset);
			// 获取分号后的参数部分
			String pathParameterContent = segment.substring(index);
			// 解析路径参数
			MultiValueMap<String, String> parameters = parsePathParams(pathParameterContent, charset);
			// 创建默认路径段对象，包括原始路径段、匹配值和参数
			return DefaultPathSegment.from(segment, valueToMatch, parameters);
		}
	}

	private static MultiValueMap<String, String> parsePathParams(String input, Charset charset) {
		// 创建一个空的 MultiValueMap，用于存储解析后的参数和对应的值列表
		MultiValueMap<String, String> result = new LinkedMultiValueMap<>();

		// 初始化起始位置
		int begin = 1;

		// 循环处理输入字符串
		while (begin < input.length()) {
			// 查找下一个分号 ';' 的位置
			int end = input.indexOf(';', begin);

			// 提取当前参数字符串
			String param = (end != -1 ? input.substring(begin, end) : input.substring(begin));

			// 解析当前参数字符串的值，并将解析结果存储到 结果列表 中
			parsePathParamValues(param, charset, result);

			// 如果已经到达字符串末尾，则结束循环
			if (end == -1) {
				break;
			}

			// 更新起始位置为下一个参数的起始位置
			begin = end + 1;
		}

		// 返回解析后的结果映射
		return result;
	}

	private static void parsePathParamValues(String input, Charset charset, MultiValueMap<String, String> output) {
		// 如果输入字符串不为空或不为空白
		if (StringUtils.hasText(input)) {
			// 查找等号 '=' 的位置
			int index = input.indexOf('=');
			// 如果找到等号
			if (index != -1) {
				// 提取参数名部分
				String name = input.substring(0, index);
				// 对参数名进行 URI 解码
				name = StringUtils.uriDecode(name, charset);
				// 如果解码后的参数名不为空或不为空白
				if (StringUtils.hasText(name)) {
					// 提取参数值部分
					String value = input.substring(index + 1);
					// 将参数值按逗号分隔，并对每个值进行 URI 解码
					for (String v : StringUtils.commaDelimitedListToStringArray(value)) {
						// 添加到输出对象中
						output.add(name, StringUtils.uriDecode(v, charset));
					}
				}
			} else {
				// 如果未找到等号，则整个输入字符串作为参数名，对其进行 URI 解码
				String name = StringUtils.uriDecode(input, charset);
				if (StringUtils.hasText(name)) {
					// 如果解码后的名称不为空，将空字符串作为值添加到输出对象中
					output.add(input, "");
				}
			}
		}
	}

	static PathContainer subPath(PathContainer container, int fromIndex, int toIndex) {
		// 获取路径容器中的所有元素列表
		List<Element> elements = container.elements();

		// 如果提取的子路径恰好是整个路径容器的范围，则直接返回原路径容器
		if (fromIndex == 0 && toIndex == elements.size()) {
			return container;
		}

		// 如果 开始索引 等于 结束索引，则返回表示空路径的路径容器
		if (fromIndex == toIndex) {
			return EMPTY_PATH;
		}

		Assert.isTrue(fromIndex >= 0 && fromIndex < elements.size(), () -> "Invalid fromIndex: " + fromIndex);
		Assert.isTrue(toIndex >= 0 && toIndex <= elements.size(), () -> "Invalid toIndex: " + toIndex);
		Assert.isTrue(fromIndex < toIndex, () -> "fromIndex: " + fromIndex + " should be < toIndex " + toIndex);

		// 提取子列表，包括 开始索引 到 结束索引 之间的元素
		List<Element> subList = elements.subList(fromIndex, toIndex);

		// 将子列表中的元素值拼接成一个新的路径字符串
		String path = subList.stream().map(Element::value).collect(Collectors.joining(""));

		// 返回一个新的默认路径容器，包含提取的子路径字符串和子列表元素
		return new DefaultPathContainer(path, subList);
	}


	private static class DefaultSeparator implements Separator {
		/**
		 * 分隔符
		 */
		private final String separator;

		/**
		 * 编码序列
		 */
		private final String encodedSequence;


		DefaultSeparator(char separator, String encodedSequence) {
			this.separator = String.valueOf(separator);
			this.encodedSequence = encodedSequence;
		}


		@Override
		public String value() {
			return this.separator;
		}

		public String encodedSequence() {
			return this.encodedSequence;
		}
	}


	private static final class DefaultPathSegment implements PathSegment {
		/**
		 * 空的参数值
		 */
		private static final MultiValueMap<String, String> EMPTY_PARAMS =
				CollectionUtils.unmodifiableMultiValueMap(new LinkedMultiValueMap<>());

		/**
		 * 值
		 */
		private final String value;

		/**
		 * 要匹配的值
		 */
		private final String valueToMatch;

		/**
		 * 参数映射
		 */
		private final MultiValueMap<String, String> parameters;

		/**
		 * 创建一个不需要解码和解析的路径段。
		 *
		 * @param value     路径段的原始值
		 * @param separator 分隔符
		 * @return 创建的路径段对象
		 */
		static DefaultPathSegment from(String value, DefaultSeparator separator) {
			String valueToMatch = value.contains(separator.encodedSequence()) ?
					value.replaceAll(separator.encodedSequence(), separator.value()) : value;
			return from(value, valueToMatch);
		}

		/**
		 * 创建一个已解码和解析的路径段。
		 *
		 * @param value        路径段的原始值
		 * @param valueToMatch 匹配值
		 * @return 创建的路径段对象
		 */
		static DefaultPathSegment from(String value, String valueToMatch) {
			return new DefaultPathSegment(value, valueToMatch, EMPTY_PARAMS);
		}

		/**
		 * 创建一个已解码和解析的路径段，并附带路径参数。
		 *
		 * @param value        路径段的原始值
		 * @param valueToMatch 匹配值
		 * @param params       路径段的参数
		 * @return 创建的路径段对象
		 */
		static DefaultPathSegment from(String value, String valueToMatch, MultiValueMap<String, String> params) {
			return new DefaultPathSegment(value, valueToMatch, CollectionUtils.unmodifiableMultiValueMap(params));
		}

		private DefaultPathSegment(String value, String valueToMatch, MultiValueMap<String, String> params) {
			this.value = value;
			this.valueToMatch = valueToMatch;
			this.parameters = params;
		}


		@Override
		public String value() {
			return this.value;
		}

		@Override
		public String valueToMatch() {
			return this.valueToMatch;
		}

		@Override
		public char[] valueToMatchAsChars() {
			return this.valueToMatch.toCharArray();
		}

		@Override
		public MultiValueMap<String, String> parameters() {
			return this.parameters;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof PathSegment)) {
				return false;
			}
			return value().equals(((PathSegment) other).value());
		}

		@Override
		public int hashCode() {
			return this.value.hashCode();
		}

		@Override
		public String toString() {
			return "[value='" + this.value + "']";
		}
	}

}

