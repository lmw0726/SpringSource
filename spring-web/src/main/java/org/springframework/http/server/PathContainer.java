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

import org.springframework.util.MultiValueMap;

import java.util.List;

/**
 * URI路径的结构化表示形式，通过 {@link #parsePath(String)} 解析为 {@link Separator} 和 {@link PathSegment} 元素的序列。
 *
 * <p>每个 {@link PathSegment} 都以解码形式公开其内容，并删除了路径参数。
 * 这样可以安全地逐个匹配路径段，而不会因解码的保留字符而改变路径的结构。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface PathContainer {

	/**
	 * 此实例解析的原始路径。
	 *
	 * @return 原始路径字符串
	 */
	String value();

	/**
	 * 包含的路径元素，可以是 {@link Separator} 或 {@link PathSegment}。
	 *
	 * @return 路径元素列表
	 */
	List<Element> elements();

	/**
	 * 从给定的偏移量提取子路径到元素列表中。
	 *
	 * @param index 开始元素的索引（包括）
	 * @return 子路径
	 */
	default PathContainer subPath(int index) {
		return subPath(index, elements().size());
	}

	/**
	 * 从给定的起始偏移量提取子路径到元素列表中（包括起始偏移量），直到结束偏移量（不包括）。
	 *
	 * @param startIndex 开始元素的索引（包括）
	 * @param endIndex   结束元素的索引（不包括）
	 * @return 子路径
	 */
	default PathContainer subPath(int startIndex, int endIndex) {
		return DefaultPathContainer.subPath(this, startIndex, endIndex);
	}


	/**
	 * 将路径值解析为 {@code "/"} {@link Separator Separator} 和 {@link PathSegment PathSegment} 元素序列。
	 *
	 * @param path 要解析的编码、原始路径值
	 * @return 解析后的路径
	 */
	static PathContainer parsePath(String path) {
		return DefaultPathContainer.createFromUrlPath(path, Options.HTTP_PATH);
	}

	/**
	 * 将路径值解析为 {@link Separator Separator} 和 {@link PathSegment PathSegment} 元素序列。
	 *
	 * @param path    要解析的编码、原始路径值
	 * @param options 自定义解析选项
	 * @return 解析后的路径
	 * @since 5.2
	 */
	static PathContainer parsePath(String path, Options options) {
		return DefaultPathContainer.createFromUrlPath(path, options);
	}


	/**
	 * 表示路径元素，可以是分隔符或路径段。
	 */
	interface Element {

		/**
		 * 获取此元素的未修改的原始值。
		 *
		 * @return 元素的原始值
		 */
		String value();
	}


	/**
	 * 路径分隔符元素。
	 */
	interface Separator extends Element {
	}


	/**
	 * 路径段元素。
	 */
	interface PathSegment extends Element {

		/**
		 * 返回路径段的值，用于路径匹配时解码和清理。
		 *
		 * @return 路径段的值
		 */
		String valueToMatch();

		/**
		 * 将 {@link #valueToMatch()} 以字符数组形式公开。
		 *
		 * @return 字符数组形式的路径段值
		 */
		char[] valueToMatchAsChars();

		/**
		 * 与此路径段关联的路径参数。
		 *
		 * @return 包含参数的不可修改的映射
		 */
		MultiValueMap<String, String> parameters();
	}


	/**
	 * 用于根据输入路径类型自定义解析选项的选项。
	 *
	 * @since 5.2
	 */
	public class Options {

		/**
		 * HTTP URL路径的选项。
		 * <p>使用分隔符 '/' 进行URL解码，并解析路径参数。</p>
		 */
		public final static Options HTTP_PATH = Options.create('/', true);

		/**
		 * 消息路由的选项。
		 * <p>使用分隔符 '.'，既不进行URL解码，也不解析路径参数。
		 * 在段值中，分隔符字符的转义序列仍然会被解码。</p>
		 */
		public final static Options MESSAGE_ROUTE = Options.create('.', false);
		/**
		 * 分割符
		 */
		private final char separator;

		/**
		 * 是否应对路径段值进行URL解码和路径参数解析
		 */
		private final boolean decodeAndParseSegments;

		private Options(char separator, boolean decodeAndParseSegments) {
			this.separator = separator;
			this.decodeAndParseSegments = decodeAndParseSegments;
		}

		/**
		 * 返回用于解析路径成段的分隔符。
		 *
		 * @return 解析路径成段的分隔符
		 */
		public char separator() {
			return this.separator;
		}

		/**
		 * 返回是否应对路径段值进行URL解码和路径参数解析。
		 *
		 * @return 如果应解码和解析路径段，则返回 true；否则返回 false。
		 */
		public boolean shouldDecodeAndParseSegments() {
			return this.decodeAndParseSegments;
		}

		/**
		 * 使用给定的设置创建一个 {@link Options} 实例。
		 *
		 * @param separator              解析路径成段所用的分隔符；目前必须是斜杠 '/' 或点 '.'
		 * @param decodeAndParseSegments 是否对路径段值进行URL解码和路径参数解析。
		 *                               如果设置为 false，则仅解码段值中分隔符字符的转义序列。
		 * @return 包含给定设置的 {@link Options} 实例
		 */
		public static Options create(char separator, boolean decodeAndParseSegments) {
			return new Options(separator, decodeAndParseSegments);
		}
	}

}
