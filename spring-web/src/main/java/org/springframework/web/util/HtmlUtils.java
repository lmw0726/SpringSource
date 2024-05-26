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

package org.springframework.web.util;

import org.springframework.util.Assert;

/**
 * HTML转义的实用工具类。
 *
 * <p>根据W3C HTML 4.01建议进行转义和反转义，处理字符实体引用。
 *
 * <p>参考：
 * <a href="https://www.w3.org/TR/html4/charset.html">https://www.w3.org/TR/html4/charset.html</a>
 *
 * <p>对于一套全面的字符串转义实用工具，请考虑使用
 * <a href="https://commons.apache.org/proper/commons-text/">Apache Commons Text</a>
 * 及其 {@code StringEscapeUtils} 类。我们这里没有使用该类，以避免在仅用于HTML转义时对 Commons Text 进行运行时依赖。
 * 此外，Spring的HTML转义更加灵活，而且完全符合HTML 4.0规范。
 *
 * @author Juergen Hoeller
 * @author Martin Kersten
 * @author Craig Andrews
 * @since 01.03.2003
 */
public abstract class HtmlUtils {

	/**
	 * 预解析的HTML字符实体引用的共享实例。
	 */
	private static final HtmlCharacterEntityReferences characterEntityReferences =
			new HtmlCharacterEntityReferences();


	/**
	 * 将特殊字符转换为HTML字符引用。
	 * <p>处理HTML 4.01建议中定义的完整字符集。
	 * <p>将所有特殊字符转义为其对应的实体引用（例如 {@code &lt;}）。
	 * <p>参考：
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 *
	 * @param input 输入字符串（未转义）
	 * @return 转义后的字符串
	 */
	public static String htmlEscape(String input) {
		return htmlEscape(input, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * 将特殊字符转换为HTML字符引用。
	 * <p>处理HTML 4.01建议中定义的完整字符集。
	 * <p>至少根据指定的编码要求，将所有特殊字符转义为其对应的实体引用（例如 {@code &lt;}）。
	 * <p>参考：
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 *
	 * @param input    输入字符串（未转义）
	 * @param encoding 支持的 {@link java.nio.charset.Charset charset} 的名称
	 * @return 转义后的字符串
	 * @since 4.1.2
	 */
	public static String htmlEscape(String input, String encoding) {
		Assert.notNull(input, "Input is required");
		Assert.notNull(encoding, "Encoding is required");
		// 创建一个 StringBuilder，其初始容量为输入长度的两倍
		StringBuilder escaped = new StringBuilder(input.length() * 2);
		// 遍历输入的每个字符
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			// 将字符转换为字符实体引用
			String reference = characterEntityReferences.convertToReference(character, encoding);
			// 如果字符实体引用不为空，则追加到 StringBuilder 中
			if (reference != null) {
				escaped.append(reference);
			} else {
				// 否则直接追加字符
				escaped.append(character);
			}
		}
		// 返回转义后的字符串
		return escaped.toString();
	}

	/**
	 * 将特殊字符转换为HTML字符引用。
	 * <p>处理HTML 4.01建议中定义的完整字符集。
	 * <p>将所有特殊字符转义为其对应的十进制数引用（例如 {@code &amp;#68;}）。
	 * <p>参考：
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 *
	 * @param input 输入字符串（未转义）
	 * @return 转义后的字符串
	 */
	public static String htmlEscapeDecimal(String input) {
		return htmlEscapeDecimal(input, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * 将特殊字符转换为HTML字符引用。
	 * <p>处理HTML 4.01建议中定义的完整字符集。
	 * <p>至少根据指定的编码要求，将所有特殊字符转义为其对应的十进制数引用（例如 {@code &amp;#68;}）。
	 * <p>参考：
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 *
	 * @param input    输入字符串（未转义）
	 * @param encoding 支持的 {@link java.nio.charset.Charset charset} 的名称
	 * @return 转义后的字符串
	 * @since 4.1.2
	 */
	public static String htmlEscapeDecimal(String input, String encoding) {
		Assert.notNull(input, "Input is required");
		Assert.notNull(encoding, "Encoding is required");
		// 创建一个 StringBuilder，其初始容量为输入长度的两倍
		StringBuilder escaped = new StringBuilder(input.length() * 2);
		// 遍历输入的每个字符
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			// 如果字符能映射到字符实体引用
			if (characterEntityReferences.isMappedToReference(character, encoding)) {
				// 将字符转换为十进制字符实体引用
				escaped.append(HtmlCharacterEntityReferences.DECIMAL_REFERENCE_START);
				// 添加当前字符
				escaped.append((int) character);
				escaped.append(HtmlCharacterEntityReferences.REFERENCE_END);
			} else {
				// 否则直接追加字符
				escaped.append(character);
			}
		}
		// 返回转义后的字符串
		return escaped.toString();
	}

	/**
	 * 将特殊字符转换为HTML字符引用。
	 * <p>处理HTML 4.01建议中定义的完整字符集。
	 * <p>将所有特殊字符转义为其对应的十六进制数引用（例如 {@code &amp;#x<i>Hex</i>;}）。
	 * <p>参考：
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 *
	 * @param input 输入字符串（未转义）
	 * @return 转义后的字符串
	 */
	public static String htmlEscapeHex(String input) {
		return htmlEscapeHex(input, WebUtils.DEFAULT_CHARACTER_ENCODING);
	}

	/**
	 * 将特殊字符转换为HTML字符引用。
	 * <p>处理HTML 4.01建议中定义的完整字符集。
	 * <p>至少根据指定的编码要求，将所有特殊字符转义为其对应的十六进制数引用（例如 {@code &amp;#x<i>Hex</i>;}）。
	 * <p>参考：
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 *
	 * @param input    输入字符串（未转义）
	 * @param encoding 支持的 {@link java.nio.charset.Charset charset} 的名称
	 * @return 转义后的字符串
	 * @since 4.1.2
	 */
	public static String htmlEscapeHex(String input, String encoding) {
		Assert.notNull(input, "Input is required");
		Assert.notNull(encoding, "Encoding is required");
		// 创建一个 StringBuilder，其初始容量为输入长度的两倍
		StringBuilder escaped = new StringBuilder(input.length() * 2);
		// 遍历输入的每个字符
		for (int i = 0; i < input.length(); i++) {
			char character = input.charAt(i);
			// 如果字符能映射到字符实体引用
			if (characterEntityReferences.isMappedToReference(character, encoding)) {
				// 将字符转换为十六进制字符实体引用
				escaped.append(HtmlCharacterEntityReferences.HEX_REFERENCE_START);
				// 将当前字符转为十六进制，并添加到StringBuilder中
				escaped.append(Integer.toString(character, 16));
				escaped.append(HtmlCharacterEntityReferences.REFERENCE_END);
			} else {
				// 否则直接追加字符
				escaped.append(character);
			}
		}
		// 返回转义后的字符串
		return escaped.toString();
	}

	/**
	 * 将HTML字符引用转换为其纯文本UNICODE等效形式。
	 * <p>处理HTML 4.01建议中定义的完整字符集和所有引用类型（十进制、十六进制和实体）。
	 * <p>正确转换以下格式：
	 * <blockquote>
	 * &amp;#<i>Entity</i>; - <i>（示例：&amp;amp;）区分大小写</i>
	 * &amp;#<i>Decimal</i>; - <i>（示例：&amp;#68;）</i><br>
	 * &amp;#x<i>Hex</i>; - <i>（示例：&amp;#xE5;）不区分大小写</i><br>
	 * </blockquote>
	 * <p>通过将原始字符复制为遇到时的原样，优雅地处理格式不正确的字符引用。
	 * <p>参考：
	 * <a href="https://www.w3.org/TR/html4/sgml/entities.html">
	 * https://www.w3.org/TR/html4/sgml/entities.html
	 * </a>
	 *
	 * @param input 输入字符串（已转义）
	 * @return 未转义的字符串
	 */
	public static String htmlUnescape(String input) {
		return new HtmlCharacterEntityDecoder(characterEntityReferences, input).decode();
	}

}
