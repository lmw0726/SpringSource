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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.*;

/**
 * 各种 {@link String} 工具方法。
 *
 * <p>主要供框架内部使用；对于更全面的 {@code String} 工具集，
 * 请考虑使用
 * <a href="https://commons.apache.org/proper/commons-lang/">Apache 的 Commons Lang</a>。
 *
 * <p>本类提供一些简单功能，这些功能实际上应该由核心 Java 的 {@link String} 和 {@link StringBuilder}
 * 类来提供。它还提供了便捷方法，用于在分隔字符串（如 CSV 字符串）与集合和数组之间转换。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Rick Evans
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 2001年4月16日
 */
public abstract class StringUtils {

	private static final String[] EMPTY_STRING_ARRAY = {};

	private static final String FOLDER_SEPARATOR = "/";

	private static final char FOLDER_SEPARATOR_CHAR = '/';

	private static final String WINDOWS_FOLDER_SEPARATOR = "\\";

	private static final String TOP_PATH = "..";

	private static final String CURRENT_PATH = ".";

	private static final char EXTENSION_SEPARATOR = '.';


	//---------------------------------------------------------------------
	// 用于处理字符串的一般便捷方法
	//---------------------------------------------------------------------

	/**
	 * 检查给定的对象（可能是 {@code String}）是否为空。
	 * 这实际上是 {@code !hasLength(String)} 的快捷方式。
	 * <p>该方法接受任何对象作为参数，将其与 {@code null} 和空字符串进行比较。
	 * 因此，该方法对于非空的非字符串对象永远不会返回 {@code true}。
	 * <p>此对象签名对通用属性处理代码很有用，
	 * 这类代码通常处理字符串，但通常需要遍历对象，
	 * 因为属性也可能是基本值对象等。
	 * <p><b>注意：如果对象事先已声明为 {@code String} 类型，
	 * 建议使用 {@link #hasLength(String)} 或 {@link #hasText(String)}。</b>
	 *
	 * @param str 待检查的对象（可能是 {@code String}）
	 * @since 3.2.1
	 * @deprecated 从5.3版本开始，建议使用 {@link #hasLength(String)} 和 {@link #hasText(String)}（或 {@link ObjectUtils#isEmpty(Object)}）
	 */
	@Deprecated
	public static boolean isEmpty(@Nullable Object str) {
		return (str == null || "".equals(str));
	}

	/**
	 * 检查给定的 {@code CharSequence} 是否既不为 {@code null}，也不为空长度。
	 * <p>注意：该方法对仅由空白字符组成的 {@code CharSequence} 也返回 {@code true}。
	 * <p><pre class="code">
	 * StringUtils.hasLength(null) = false
	 * StringUtils.hasLength("") = false
	 * StringUtils.hasLength(" ") = true
	 * StringUtils.hasLength("Hello") = true
	 * </pre>
	 *
	 * @param str 要检查的 {@code CharSequence}（可能为 {@code null}）
	 * @return 如果 {@code CharSequence} 不为 {@code null} 且长度大于0，则返回 {@code true}
	 * @see #hasLength(String)
	 * @see #hasText(CharSequence)
	 */
	public static boolean hasLength(@Nullable CharSequence str) {
		return (str != null && str.length() > 0);
	}

	/**
	 * 检查给定的 {@code String} 是否既不为 {@code null}，也不为空长度。
	 * <p>注意：该方法对仅由空白字符组成的 {@code String} 也返回 {@code true}。
	 *
	 * @param str 要检查的 {@code String}（可能为 {@code null}）
	 * @return 如果 {@code String} 不为 {@code null} 且长度大于0，则返回 {@code true}
	 * @see #hasLength(CharSequence)
	 * @see #hasText(String)
	 */
	public static boolean hasLength(@Nullable String str) {
		return (str != null && !str.isEmpty());
	}

	/**
	 * 检查给定的 {@code CharSequence} 是否包含实际的 <em>文本</em>。
	 * <p>更具体地说，如果 {@code CharSequence} 不为 {@code null}，长度大于0，且包含至少一个非空白字符，则返回 {@code true}。
	 * <p><pre class="code">
	 * StringUtils.hasText(null) = false
	 * StringUtils.hasText("") = false
	 * StringUtils.hasText(" ") = false
	 * StringUtils.hasText("12345") = true
	 * StringUtils.hasText(" 12345 ") = true
	 * </pre>
	 *
	 * @param str 要检查的 {@code CharSequence}（可能为 {@code null}）
	 * @return 如果 {@code CharSequence} 不为 {@code null}，长度大于0，且不全是空白字符，则返回 {@code true}
	 * @see #hasText(String)
	 * @see #hasLength(CharSequence)
	 * @see Character#isWhitespace
	 */
	public static boolean hasText(@Nullable CharSequence str) {
		return (str != null && str.length() > 0 && containsText(str));
	}

	/**
	 * 检查给定的 {@code String} 是否包含实际的 <em> 文本 <em>。
	 * <p> 更具体地说，如果 {@code String} 不是 {@code null}，其长度大于0，并且至少包含一个非空格字符，则此方法返回 {@code true}。
	 *
	 * @param str 要检查的 {@code String} (可能是 {@code null})
	 * @return {@code true} 如果 {@code String} 不是 {@code null}，则其长度大于0，并且仅不包含空格
	 * @see #hasText(CharSequence)
	 * @see #hasLength(String)
	 * @see Character#isWhitespace
	 */
	public static boolean hasText(@Nullable String str) {
		return (str != null && !str.isEmpty() && containsText(str));
	}

	private static boolean containsText(CharSequence str) {
		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查给定的 {@code CharSequence} 是否包含任何空白字符。
	 *
	 * @param str 要检查的 {@code CharSequence}（可能为 {@code null}）
	 * @return 如果 {@code CharSequence} 不为空且包含至少一个空白字符，则返回 {@code true}
	 * @see Character#isWhitespace
	 */
	public static boolean containsWhitespace(@Nullable CharSequence str) {
		if (!hasLength(str)) {
			return false;
		}

		int strLen = str.length();
		for (int i = 0; i < strLen; i++) {
			if (Character.isWhitespace(str.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 检查给定的 {@code String} 是否包含任何空白字符。
	 *
	 * @param str 要检查的 {@code String}（可能为 {@code null}）
	 * @return 如果 {@code String} 不为空且包含至少一个空白字符，则返回 {@code true}
	 * @see #containsWhitespace(CharSequence)
	 */
	public static boolean containsWhitespace(@Nullable String str) {
		return containsWhitespace((CharSequence) str);
	}

	/**
	 * 去除给定 {@code String} 的首尾空白字符。
	 *
	 * @param str 要检查的 {@code String}
	 * @return 去除首尾空白后的 {@code String}
	 * @see java.lang.Character#isWhitespace
	 */
	public static String trimWhitespace(String str) {
		if (!hasLength(str)) {
			return str;
		}

		int beginIndex = 0;
		int endIndex = str.length() - 1;

		while (beginIndex <= endIndex && Character.isWhitespace(str.charAt(beginIndex))) {
			beginIndex++;
		}

		while (endIndex > beginIndex && Character.isWhitespace(str.charAt(endIndex))) {
			endIndex--;
		}

		return str.substring(beginIndex, endIndex + 1);
	}

	/**
	 * 从给定的 {@code String} 中修剪 <i> 所有 <i> 空格: 前置字符，后置字符和字符之间的字符。
	 *
	 * @param str 要检查的 {@code String}
	 * @return 裁剪后的 {@code String}
	 * @see java.lang.Character#isWhitespace
	 */
	public static String trimAllWhitespace(String str) {
		if (!hasLength(str)) {
			//如果没有长度，直接返回原字符串
			return str;
		}

		int len = str.length();
		StringBuilder sb = new StringBuilder(str.length());
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			if (!Character.isWhitespace(c)) {
				//如果字符不是空表字符，则添加到StringBuilder中
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * 去除给定 {@code String} 开头的空白字符。
	 *
	 * @param str 要检查的 {@code String}
	 * @return 去除开头空白后的 {@code String}
	 * @see java.lang.Character#isWhitespace
	 */
	public static String trimLeadingWhitespace(String str) {
		if (!hasLength(str)) {
			return str;
		}

		int beginIdx = 0;
		while (beginIdx < str.length() && Character.isWhitespace(str.charAt(beginIdx))) {
			beginIdx++;
		}
		return str.substring(beginIdx);
	}

	/**
	 * 去除给定 {@code String} 末尾的空白字符。
	 *
	 * @param str 要检查的 {@code String}
	 * @return 去除末尾空白后的 {@code String}
	 * @see java.lang.Character#isWhitespace
	 */
	public static String trimTrailingWhitespace(String str) {
		if (!hasLength(str)) {
			return str;
		}

		int endIdx = str.length() - 1;
		while (endIdx >= 0 && Character.isWhitespace(str.charAt(endIdx))) {
			endIdx--;
		}
		return str.substring(0, endIdx + 1);
	}

	/**
	 * 去除给定 {@code String} 开头的所有指定字符。
	 *
	 * @param str              要检查的 {@code String}
	 * @param leadingCharacter 要去除的开头字符
	 * @return 去除开头指定字符后的 {@code String}
	 */
	public static String trimLeadingCharacter(String str, char leadingCharacter) {
		if (!hasLength(str)) {
			return str;
		}

		int beginIdx = 0;
		while (beginIdx < str.length() && leadingCharacter == str.charAt(beginIdx)) {
			beginIdx++;
		}
		return str.substring(beginIdx);
	}

	/**
	 * 去除给定 {@code String} 末尾的所有指定字符。
	 *
	 * @param str               要检查的 {@code String}
	 * @param trailingCharacter 要去除的末尾字符
	 * @return 去除末尾指定字符后的 {@code String}
	 */
	public static String trimTrailingCharacter(String str, char trailingCharacter) {
		if (!hasLength(str)) {
			return str;
		}

		int endIdx = str.length() - 1;
		while (endIdx >= 0 && trailingCharacter == str.charAt(endIdx)) {
			endIdx--;
		}
		return str.substring(0, endIdx + 1);
	}

	/**
	 * 判断给定的 {@code String} 是否匹配给定的单个字符。
	 *
	 * @param str             要检查的 {@code String}
	 * @param singleCharacter 要比较的字符
	 * @since 5.2.9
	 */
	public static boolean matchesCharacter(@Nullable String str, char singleCharacter) {
		return (str != null && str.length() == 1 && str.charAt(0) == singleCharacter);
	}

	/**
	 * 判断给定的 {@code String} 是否以指定的前缀开始，忽略大小写。
	 *
	 * @param str    要检查的 {@code String}
	 * @param prefix 要查找的前缀
	 * @see java.lang.String#startsWith
	 */
	public static boolean startsWithIgnoreCase(@Nullable String str, @Nullable String prefix) {
		return (str != null && prefix != null && str.length() >= prefix.length() &&
				str.regionMatches(true, 0, prefix, 0, prefix.length()));
	}

	/**
	 * 判断给定的 {@code String} 是否以指定的后缀结束，忽略大小写。
	 *
	 * @param str    要检查的 {@code String}
	 * @param suffix 要查找的后缀
	 * @see java.lang.String#endsWith
	 */
	public static boolean endsWithIgnoreCase(@Nullable String str, @Nullable String suffix) {
		return (str != null && suffix != null && str.length() >= suffix.length() &&
				str.regionMatches(true, str.length() - suffix.length(), suffix, 0, suffix.length()));
	}

	/**
	 * 测试给定字符串是否与给定索引处的给定子字符串匹配。
	 *
	 * @param str       原始字符串 (或StringBuilder)
	 * @param index     要开始匹配的原始字符串中的索引
	 * @param substring 在给定索引处匹配的子字符串
	 */
	public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
		if (index + substring.length() > str.length()) {
			//如果遍历的位置+子字符串的长度大于原始字符串的长度,则返回false
			return false;
		}
		for (int i = 0; i < substring.length(); i++) {
			if (str.charAt(index + i) != substring.charAt(i)) {
				//任意位置的字符不匹配，返回false
				return false;
			}
		}
		return true;
	}

	/**
	 * 统计字符串 {@code str} 中子字符串 {@code sub} 出现的次数。
	 *
	 * @param str 要搜索的字符串
	 * @param sub 要查找的子字符串
	 */
	public static int countOccurrencesOf(String str, String sub) {
		if (!hasLength(str) || !hasLength(sub)) {
			return 0;
		}

		int count = 0;
		int pos = 0;
		int idx;
		while ((idx = str.indexOf(sub, pos)) != -1) {
			++count;
			pos = idx + sub.length();
		}
		return count;
	}

	/**
	 * 替换字符串中所有出现的子字符串为另一个字符串。
	 *
	 * @param inString   要检查的 {@code String}
	 * @param oldPattern 要替换的 {@code String}
	 * @param newPattern 要插入的 {@code String}
	 * @return 经过替换后的 {@code String}
	 */
	public static String replace(String inString, String oldPattern, @Nullable String newPattern) {
		if (!hasLength(inString) || !hasLength(oldPattern) || newPattern == null) {
			return inString;
		}
		int index = inString.indexOf(oldPattern);
		if (index == -1) {
			// 没有出现 -> 可以直接返回输入
			return inString;
		}

		int capacity = inString.length();
		if (newPattern.length() > oldPattern.length()) {
			capacity += 16;
		}
		StringBuilder sb = new StringBuilder(capacity);

		int pos = 0;  // 我们在旧字符串中的位置
		int patLen = oldPattern.length();
		while (index >= 0) {
			sb.append(inString, pos, index);
			sb.append(newPattern);
			pos = index + patLen;
			index = inString.indexOf(oldPattern, pos);
		}

		// 追加匹配右边的所有字符
		sb.append(inString, pos, inString.length());
		return sb.toString();
	}

	/**
	 * 删除给定子字符串的所有出现。
	 *
	 * @param inString 原始 {@code String}
	 * @param pattern  要删除所有出现的模式
	 * @return 结果 {@code String}
	 */
	public static String delete(String inString, String pattern) {
		return replace(inString, pattern, "");
	}

	/**
	 * 删除给定 {@code String} 中的任何字符。
	 *
	 * @param inString      原始 {@code String}
	 * @param charsToDelete 要删除的一组字符。例如。“az\n” 将删除 “a” 、 “z” 和新行。
	 * @return {@code String}结果
	 */
	public static String deleteAny(String inString, @Nullable String charsToDelete) {
		if (!hasLength(inString) || !hasLength(charsToDelete)) {
			//如果原始字符串为空，或者要删除的字符为空，返回原字符串
			return inString;
		}

		int lastCharIndex = 0;
		char[] result = new char[inString.length()];
		for (int i = 0; i < inString.length(); i++) {
			char c = inString.charAt(i);
			if (charsToDelete.indexOf(c) == -1) {
				//如果要删除的字符串没有当前位置的字符，将该字符添加到字符数组中
				result[lastCharIndex++] = c;
			}
		}
		if (lastCharIndex == inString.length()) {
			//如果最后一个字符的位置与原始字符长度相同，则返回原始字符串
			return inString;
		}
		//构建未删除的字符串
		return new String(result, 0, lastCharIndex);
	}

	//---------------------------------------------------------------------
	// 处理格式化字符串的便捷方法
	//---------------------------------------------------------------------

	/**
	 * 用单引号引用给定的 {@code String}。
	 *
	 * @param str 输入的 {@code String}（例如 "myString"）
	 * @return 带引号的 {@code String}（例如 "'myString'"），
	 * 如果输入为 {@code null} 则返回 {@code null}
	 */
	@Nullable
	public static String quote(@Nullable String str) {
		return (str != null ? "'" + str + "'" : null);
	}

	/**
	 * 如果输入对象是 {@code String}，则将其转换为带单引号的 {@code String}；
	 * 否则保持对象原样。
	 *
	 * @param obj 输入对象（例如 "myString"）
	 * @return 带引号的 {@code String}（例如 "'myString'"），
	 * 如果不是 {@code String} 则返回输入对象本身
	 */
	@Nullable
	public static Object quoteIfString(@Nullable Object obj) {
		return (obj instanceof String ? quote((String) obj) : obj);
	}

	/**
	 * 去除由 '.' 点字符限定的字符串的限定部分。例如，
	 * "this.name.is.qualified" 返回 "qualified"。
	 *
	 * @param qualifiedName 限定名称
	 */
	public static String unqualify(String qualifiedName) {
		return unqualify(qualifiedName, '.');
	}

	/**
	 * 去除由指定分隔符限定的字符串的限定部分。例如，
	 * 如果使用 ':' 作为分隔符，"this:name:is:qualified" 返回 "qualified"。
	 *
	 * @param qualifiedName 限定名称
	 * @param separator 分隔符
	 */
	public static String unqualify(String qualifiedName, char separator) {
		return qualifiedName.substring(qualifiedName.lastIndexOf(separator) + 1);
	}

	/**
	 * 将 {@code String} 首字母大写，
	 * 根据 {@link Character#toUpperCase(char)} 转换首字母为大写。
	 * 其他字母不变。
	 *
	 * @param str 要大写首字母的 {@code String}
	 * @return 首字母大写的 {@code String}
	 */
	public static String capitalize(String str) {
		return changeFirstCharacterCase(str, true);
	}

	/**
	 * 取消大写 {@code String}，根据 {@link Character#toLowerCase(char)} 将第一个字母更改为小写。没有其他字母更改。
	 *
	 * @param str 要取消大写的 {@code String}
	 * @return 取消大写的{@code String}
	 */
	public static String uncapitalize(String str) {
		return changeFirstCharacterCase(str, false);
	}

	private static String changeFirstCharacterCase(String str, boolean capitalize) {
		if (!hasLength(str)) {
			return str;
		}
		//第一个字符串
		char baseChar = str.charAt(0);
		char updatedChar;
		if (capitalize) {
			updatedChar = Character.toUpperCase(baseChar);
		} else {
			updatedChar = Character.toLowerCase(baseChar);
		}
		if (baseChar == updatedChar) {
			return str;
		}

		char[] chars = str.toCharArray();
		chars[0] = updatedChar;
		return new String(chars);
	}

	/**
	 * 从给定的 Java 资源路径中提取文件名，
	 * 例如 {@code "mypath/myfile.txt" → "myfile.txt"}。
	 *
	 * @param path 文件路径（可能为 {@code null}）
	 * @return 提取的文件名，如果没有则返回 {@code null}
	 */
	@Nullable
	public static String getFilename(@Nullable String path) {
		if (path == null) {
			return null;
		}

		int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR_CHAR);
		return (separatorIndex != -1 ? path.substring(separatorIndex + 1) : path);
	}

	/**
	 * 从给定的 Java 资源路径中提取文件扩展名，
	 * 例如 "mypath/myfile.txt" → "txt"。
	 *
	 * @param path 文件路径（可能为 {@code null}）
	 * @return 提取的文件扩展名，如果没有则返回 {@code null}
	 */
	@Nullable
	public static String getFilenameExtension(@Nullable String path) {
		if (path == null) {
			return null;
		}

		int extIndex = path.lastIndexOf(EXTENSION_SEPARATOR);
		if (extIndex == -1) {
			return null;
		}

		int folderIndex = path.lastIndexOf(FOLDER_SEPARATOR_CHAR);
		if (folderIndex > extIndex) {
			return null;
		}

		return path.substring(extIndex + 1);
	}

	/**
	 * 从给定的 Java 资源路径中剥离文件扩展名，
	 * 例如 "mypath/myfile.txt" → "mypath/myfile"。
	 *
	 * @param path 文件路径
	 * @return 剥离文件扩展名后的路径
	 */
	public static String stripFilenameExtension(String path) {
		int extIndex = path.lastIndexOf(EXTENSION_SEPARATOR);
		if (extIndex == -1) {
			return path;
		}

		int folderIndex = path.lastIndexOf(FOLDER_SEPARATOR_CHAR);
		if (folderIndex > extIndex) {
			return path;
		}

		return path.substring(0, extIndex);
	}

	/**
	 * 将给定的相对路径应用于给定的Java资源路径，假设标准的Java文件夹分离 (即 “/” 分隔符)。
	 *
	 * @param path         开始的路径 (通常是完整的文件路径)
	 * @param relativePath 要应用的相对路径 (相对于上面的完整文件路径)
	 * @return 应用相对路径产生的完整文件路径
	 */
	public static String applyRelativePath(String path, String relativePath) {
		//获取最后一个 / 符号的位置
		int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR_CHAR);
		if (separatorIndex != -1) {
			//获取 / 符号之前的路径
			String newPath = path.substring(0, separatorIndex);
			if (!relativePath.startsWith(FOLDER_SEPARATOR)) {
				//如果相关路径不是以文件夹分隔符 / 开头，上面的新路径添加上文件夹分隔符 /
				newPath += FOLDER_SEPARATOR_CHAR;
			}
			//再加上相关路径
			return newPath + relativePath;
		} else {
			//如果该位置为空，则返回相关路径
			return relativePath;
		}
	}

	/**
	 * 通过抑制诸如“path/..”和内部简单点之类的序列来规范化路径。
	 * 结果便于路径比较。 对于其他用途，请注意 Windows 分隔符 ("\") 被简单的斜杠替换。
	 * 请注意，在安全上下文中不应依赖 cleanPath。 应该使用其他机制来防止路径遍历问题。
	 *
	 * @param path 原始路径
	 * @return 规范化路径
	 */
	public static String cleanPath(String path) {
		if (!hasLength(path)) {
			return path;
		}

		String normalizedPath = replace(path, WINDOWS_FOLDER_SEPARATOR, FOLDER_SEPARATOR);
		String pathToUse = normalizedPath;

		// 如果路径中没有点，直接返回
		if (pathToUse.indexOf('.') == -1) {
			return pathToUse;
		}

		// 从路径中剥离前缀，避免将其视为第一个路径元素的一部分。
		// 这样可以正确解析像 "file:core/../core/io/Resource.class" 这样的路径，
		// 其中 ".." 应该只去掉第一个 "core" 目录，而保留 "file:" 前缀。
		int prefixIndex = pathToUse.indexOf(':');
		String prefix = "";
		if (prefixIndex != -1) {
			prefix = pathToUse.substring(0, prefixIndex + 1);
			if (prefix.contains(FOLDER_SEPARATOR)) {
				prefix = "";
			} else {
				pathToUse = pathToUse.substring(prefixIndex + 1);
			}
		}
		if (pathToUse.startsWith(FOLDER_SEPARATOR)) {
			prefix = prefix + FOLDER_SEPARATOR;
			pathToUse = pathToUse.substring(1);
		}

		String[] pathArray = delimitedListToStringArray(pathToUse, FOLDER_SEPARATOR);
		// 元素数量不会超过 pathArray，通常相等
		Deque<String> pathElements = new ArrayDeque<>(pathArray.length);
		int tops = 0;

		for (int i = pathArray.length - 1; i >= 0; i--) {
			String element = pathArray[i];
			if (CURRENT_PATH.equals(element)) {
				// 当前目录，忽略
			} else if (TOP_PATH.equals(element)) {
				// 记录上级路径符号 ".."
				tops++;
			} else {
				if (tops > 0) {
					// 将路径元素与对应的上级路径合并
					tops--;
				} else {
					// 普通路径元素，添加到结果中
					pathElements.addFirst(element);
				}
			}
		}

		// 所有路径元素保持不变，直接返回标准化路径
		if (pathArray.length == pathElements.size()) {
			return normalizedPath;
		}
		// 还剩余的上级路径需要保留
		for (int i = 0; i < tops; i++) {
			pathElements.addFirst(TOP_PATH);
		}
		// 如果最终为空路径，显式添加当前路径符号
		if (pathElements.size() == 1 && pathElements.getLast().isEmpty() && !prefix.endsWith(FOLDER_SEPARATOR)) {
			pathElements.addFirst(CURRENT_PATH);
		}

		final String joined = collectionToDelimitedString(pathElements, FOLDER_SEPARATOR);
		// 避免空前缀的字符串拼接
		return prefix.isEmpty() ? joined : prefix + joined;
	}

	/**
	 * 比较两个路径，先对其进行标准化。
	 *
	 * @param path1 第一个路径
	 * @param path2 第二个路径
	 * @return 两个路径标准化后是否相等
	 */
	public static boolean pathEquals(String path1, String path2) {
		return cleanPath(path1).equals(cleanPath(path2));
	}

	/**
	 * 解码给定的编码后的 URI 组件值。基于以下规则：
	 * <ul>
	 * <li>字母数字字符 {@code "a"} 到 {@code "z"}，{@code "A"} 到 {@code "Z"}，
	 * 以及 {@code "0"} 到 {@code "9"} 保持不变。</li>
	 * <li>特殊字符 {@code "-"}、{@code "_"}、{@code "."} 和 {@code "*"} 保持不变。</li>
	 * <li>序列 "{@code %<i>xy</i>}" 被解释为该字符的十六进制表示。</li>
	 * </ul>
	 *
	 * @param source  编码后的字符串
	 * @param charset 字符集
	 * @return 解码后的值
	 * @throws IllegalArgumentException 当给定的 source 包含无效的编码序列时抛出
	 * @see java.net.URLDecoder#decode(String, String)
	 * @since 5.0
	 */
	public static String uriDecode(String source, Charset charset) {
		int length = source.length();
		if (length == 0) {
			return source;
		}
		Assert.notNull(charset, "Charset must not be null");

		ByteArrayOutputStream baos = new ByteArrayOutputStream(length);
		boolean changed = false;
		for (int i = 0; i < length; i++) {
			int ch = source.charAt(i);
			if (ch == '%') {
				if (i + 2 < length) {
					char hex1 = source.charAt(i + 1);
					char hex2 = source.charAt(i + 2);
					int u = Character.digit(hex1, 16);
					int l = Character.digit(hex2, 16);
					if (u == -1 || l == -1) {
						throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
					}
					baos.write((char) ((u << 4) + l));
					i += 2;
					changed = true;
				} else {
					throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
				}
			} else {
				baos.write(ch);
			}
		}
		return (changed ? StreamUtils.copyToString(baos, charset) : source);
	}

	/**
	 * 将给定的 {@code String} 值解析为 {@link Locale}，接受
	 * {@link Locale#toString} 格式以及由 {@link Locale#forLanguageTag} 指定的 BCP 47 语言标签。
	 *
	 * @param localeValue locale 值：遵循 {@code Locale} 的 {@code toString()} 格式（如 "en", "en_UK" 等），
	 *                    也接受空格作为分隔符（作为下划线的替代），或者 BCP 47 格式（例如 "en-UK"）
	 * @return 对应的 {@code Locale} 实例，或 {@code null} 如果无效
	 * @throws IllegalArgumentException 当 locale 规范无效时抛出
	 * @see #parseLocaleString
	 * @see Locale#forLanguageTag
	 * @since 5.0.4
	 */
	@Nullable
	public static Locale parseLocale(String localeValue) {
		String[] tokens = tokenizeLocaleSource(localeValue);
		if (tokens.length == 1) {
			validateLocalePart(localeValue);
			Locale resolved = Locale.forLanguageTag(localeValue);
			if (resolved.getLanguage().length() > 0) {
				return resolved;
			}
		}
		return parseLocaleTokens(localeValue, tokens);
	}

	/**
	 * 将给定的 {@code String} 表示解析为 {@link Locale}。
	 * <p>对于许多解析场景，这相当于 {@link Locale#toString Locale 的 toString} 方法的逆操作，
	 * 但更宽松。
	 * 本方法不追求严格的 {@code Locale} 设计规范；
	 * 它专为典型的 Spring 解析需求而设计。
	 * <p><b>注意：该方法不接受 BCP 47 语言标签格式。
	 * 请使用 {@link #parseLocale} 来宽松解析这两种格式。</b>
	 *
	 * @param localeString locale 字符串：遵循 {@code Locale} 的 {@code toString()} 格式（如 "en", "en_UK" 等），
	 *                     也接受空格作为分隔符（作为下划线的替代）
	 * @return 对应的 {@code Locale} 实例，或 {@code null} 如果无效
	 * @throws IllegalArgumentException 当 locale 规范无效时抛出
	 */
	@Nullable
	public static Locale parseLocaleString(String localeString) {
		return parseLocaleTokens(localeString, tokenizeLocaleSource(localeString));
	}

	private static String[] tokenizeLocaleSource(String localeSource) {
		return tokenizeToStringArray(localeSource, "_ ", false, false);
	}

	@Nullable
	private static Locale parseLocaleTokens(String localeString, String[] tokens) {
		String language = (tokens.length > 0 ? tokens[0] : "");
		String country = (tokens.length > 1 ? tokens[1] : "");
		validateLocalePart(language);
		validateLocalePart(country);

		String variant = "";
		if (tokens.length > 2) {
			// 肯定有变体，它是国家代码之后的所有内容，不包括国家代码和变体之间的分隔符
			int endIndexOfCountryCode = localeString.indexOf(country, language.length()) + country.length();
			// 去掉开头的 '_' 和空白，剩下的是变体
			variant = trimLeadingWhitespace(localeString.substring(endIndexOfCountryCode));
			if (variant.startsWith("_")) {
				variant = trimLeadingCharacter(variant, '_');
			}
		}

		if (variant.isEmpty() && country.startsWith("#")) {
			variant = country;
			country = "";
		}

		return (language.length() > 0 ? new Locale(language, country, variant) : null);
	}

	private static void validateLocalePart(String localePart) {
		for (int i = 0; i < localePart.length(); i++) {
			char ch = localePart.charAt(i);
			if (ch != ' ' && ch != '_' && ch != '-' && ch != '#' && !Character.isLetterOrDigit(ch)) {
				throw new IllegalArgumentException(
						"Locale part \"" + localePart + "\" contains invalid characters");
			}
		}
	}

	/**
	 * 确定符合 RFC 3066 标准的语言标签，
	 * 用于 HTTP 的 "Accept-Language" 头部。
	 *
	 * @param locale 要转换为语言标签的 {@link Locale}
	 * @return 符合 RFC 3066 标准的语言标签字符串
	 * @deprecated 从 5.0.4 版本起，建议使用 {@link Locale#toLanguageTag()}
	 */
	@Deprecated
	public static String toLanguageTag(Locale locale) {
		return locale.getLanguage() + (hasText(locale.getCountry()) ? "-" + locale.getCountry() : "");
	}

	/**
	 * 将给定的 {@code timeZoneString} 字符串解析成一个 {@link TimeZone} 实例。
	 *
	 * @param timeZoneString 时区字符串，符合 {@link TimeZone#getTimeZone(String)} 规范，
	 *                       但在无效时区规范时会抛出 {@link IllegalArgumentException}
	 * @return 对应的 {@link TimeZone} 实例
	 * @throws IllegalArgumentException 当时区规范无效时抛出
	 */
	public static TimeZone parseTimeZoneString(String timeZoneString) {
		TimeZone timeZone = TimeZone.getTimeZone(timeZoneString);
		if ("GMT".equals(timeZone.getID()) && !timeZoneString.startsWith("GMT")) {
			// 不希望出现 GMT 的回退行为...
			throw new IllegalArgumentException("Invalid time zone specification '" + timeZoneString + "'");
		}
		return timeZone;
	}


	//---------------------------------------------------------------------
	// 处理字符串数组的便捷方法
	//---------------------------------------------------------------------

	/**
	 * 将给定的 {@link Collection} 复制成 {@code String} 数组。
	 * <p>该 {@code Collection} 必须仅包含 {@code String} 元素。
	 *
	 * @param collection 要复制的 {@code Collection}（可能为 {@code null} 或空）
	 * @return 生成的 {@code String} 数组
	 */
	public static String[] toStringArray(@Nullable Collection<String> collection) {
		return (CollectionUtils.isEmpty(collection) ? EMPTY_STRING_ARRAY : collection.toArray(EMPTY_STRING_ARRAY));
	}

	/**
	 * 将给定的 {@link Enumeration} 复制为一个 {@code String} 数组。
	 * <p>该 {@code Enumeration} 中必须仅包含 {@code String} 元素。
	 *
	 * @param enumeration 要复制的 {@code Enumeration}
	 *                    （可能为 {@code null} 或空）
	 * @return 转换后的 {@code String} 数组
	 */
	public static String[] toStringArray(@Nullable Enumeration<String> enumeration) {
		return (enumeration != null ? toStringArray(Collections.list(enumeration)) : EMPTY_STRING_ARRAY);
	}

	/**
	 * 将给定的 {@code String} 添加到指定的 {@code String} 数组中，
	 * 返回一个包含原数组元素和附加字符串的新数组。
	 *
	 * @param array 要追加的数组（可以为 {@code null}）
	 * @param str   要追加的 {@code String}
	 * @return 新数组（永远不为 {@code null}）
	 */
	public static String[] addStringToArray(@Nullable String[] array, String str) {
		if (ObjectUtils.isEmpty(array)) {
			return new String[]{str};
		}

		String[] newArr = new String[array.length + 1];
		System.arraycopy(array, 0, newArr, 0, array.length);
		newArr[array.length] = str;
		return newArr;
	}

	/**
	 * 合并给定的两个 {@code String} 数组为一个数组，
	 * 所有元素都会包含，即使存在重复。
	 * <p>保留原数组中元素的顺序。
	 *
	 * @param array1 第一个数组（可以为 {@code null}）
	 * @param array2 第二个数组（可以为 {@code null}）
	 * @return 合并后的新数组（如果两个数组都为 {@code null}，则返回 {@code null}）
	 */
	@Nullable
	public static String[] concatenateStringArrays(@Nullable String[] array1, @Nullable String[] array2) {
		if (ObjectUtils.isEmpty(array1)) {
			return array2;
		}
		if (ObjectUtils.isEmpty(array2)) {
			return array1;
		}

		String[] newArr = new String[array1.length + array2.length];
		System.arraycopy(array1, 0, newArr, 0, array1.length);
		System.arraycopy(array2, 0, newArr, array1.length, array2.length);
		return newArr;
	}

	/**
	 * 合并给定的两个 {@code String} 数组为一个，重复元素只保留一次。
	 * <p>保留原数组中元素的顺序（重复元素仅保留首次出现的位置）。
	 *
	 * @param array1 第一个数组（可以为 {@code null}）
	 * @param array2 第二个数组（可以为 {@code null}）
	 * @return 合并后的新数组（如果两个数组都为 {@code null}，则返回 {@code null}）
	 * @deprecated 自 4.3.15 起已弃用，建议使用 {@link LinkedHashSet} 手动合并，
	 * 以确保所有元素最多出现一次（包括第一个数组中的重复项）
	 */
	@Deprecated
	@Nullable
	public static String[] mergeStringArrays(@Nullable String[] array1, @Nullable String[] array2) {
		if (ObjectUtils.isEmpty(array1)) {
			return array2;
		}
		if (ObjectUtils.isEmpty(array2)) {
			return array1;
		}

		List<String> result = new ArrayList<>(Arrays.asList(array1));
		for (String str : array2) {
			if (!result.contains(str)) {
				result.add(str);
			}
		}
		return toStringArray(result);
	}

	/**
	 * 如有必要，对给定的 {@code String} 数组进行排序。
	 *
	 * @param array 原始数组（可能为空）
	 * @return 排序后的数组（永不为 {@code null}）
	 */
	public static String[] sortStringArray(String[] array) {
		if (ObjectUtils.isEmpty(array)) {
			return array;
		}

		Arrays.sort(array);
		return array;
	}

	/**
	 * 去除给定 {@code String} 数组中每个元素的首尾空白，
	 * 对每个非 null 元素调用 {@code String.trim()}。
	 *
	 * @param array 原始 {@code String} 数组（可能为空）
	 * @return 处理后的数组（与原数组长度相同），元素已去除空白
	 */
	public static String[] trimArrayElements(String[] array) {
		if (ObjectUtils.isEmpty(array)) {
			return array;
		}

		String[] result = new String[array.length];
		for (int i = 0; i < array.length; i++) {
			String element = array[i];
			result[i] = (element != null ? element.trim() : null);
		}
		return result;
	}

	/**
	 * 移除给定数组中的重复字符串。
	 * <p>自 4.2 起，该方法使用 {@link LinkedHashSet}，因此可以保留原始顺序。
	 *
	 * @param array {@code String} 数组（可能为空）
	 * @return 去重后的数组，按自然顺序排列
	 */
	public static String[] removeDuplicateStrings(String[] array) {
		if (ObjectUtils.isEmpty(array)) {
			return array;
		}

		Set<String> set = new LinkedHashSet<>(Arrays.asList(array));
		return toStringArray(set);
	}

	/**
	 * 在第一次出现指定分隔符的位置将 {@code String} 拆分。
	 * 返回的结果不包含分隔符本身。
	 *
	 * @param toSplit   要拆分的字符串（可能为 {@code null} 或空）
	 * @param delimiter 用作拆分依据的分隔符（可能为 {@code null} 或空）
	 * @return 包含两个元素的数组：索引 0 为分隔符前的部分，索引 1 为分隔符后的部分（两部分均不包含分隔符）；
	 * 如果输入字符串中未找到分隔符，则返回 {@code null}
	 */
	@Nullable
	public static String[] split(@Nullable String toSplit, @Nullable String delimiter) {
		if (!hasLength(toSplit) || !hasLength(delimiter)) {
			return null;
		}
		int offset = toSplit.indexOf(delimiter);
		if (offset < 0) {
			return null;
		}

		String beforeDelimiter = toSplit.substring(0, offset);
		String afterDelimiter = toSplit.substring(offset + delimiter.length());
		return new String[]{beforeDelimiter, afterDelimiter};
	}

	/**
	 * 将字符串数组中每个元素根据给定分隔符进行拆分。
	 * 然后构建一个 {@code Properties} 实例，分隔符左侧作为 key，右侧作为 value。
	 * <p>添加到 {@code Properties} 之前，会对 key 和 value 都进行去除首尾空白处理。
	 *
	 * @param array     要处理的字符串数组
	 * @param delimiter 用于拆分每个元素的分隔符（通常是等号符号）
	 * @return 表示数组内容的 {@code Properties} 实例；
	 * 如果输入数组为 {@code null} 或空，则返回 {@code null}
	 */
	@Nullable
	public static Properties splitArrayElementsIntoProperties(String[] array, String delimiter) {
		return splitArrayElementsIntoProperties(array, delimiter, null);
	}

	/**
	 * 将字符串数组中每个元素根据给定分隔符进行拆分。
	 * 然后构建一个 {@code Properties} 实例，分隔符左侧作为 key，右侧作为 value。
	 * <p>添加到 {@code Properties} 之前，会对 key 和 value 都进行去除首尾空白处理。
	 *
	 * @param array         要处理的字符串数组
	 * @param delimiter     用于拆分每个元素的分隔符（通常是等号符号）
	 * @param charsToDelete 在尝试拆分前，需要从每个元素中移除的字符（通常是引号等特殊符号），
	 *                      如果不需要删除字符则为 {@code null}
	 * @return 表示数组内容的 {@code Properties} 实例；
	 * 如果输入数组为 {@code null} 或空，则返回 {@code null}
	 */
	@Nullable
	public static Properties splitArrayElementsIntoProperties(
			String[] array, String delimiter, @Nullable String charsToDelete) {

		if (ObjectUtils.isEmpty(array)) {
			return null;
		}

		Properties result = new Properties();
		for (String element : array) {
			if (charsToDelete != null) {
				element = deleteAny(element, charsToDelete);
			}
			String[] splittedElement = split(element, delimiter);
			if (splittedElement == null) {
				continue;
			}
			result.setProperty(splittedElement[0].trim(), splittedElement[1].trim());
		}
		return result;
	}

	/**
	 * 通过 {@link StringTokenizer} 将给定的 {@code String} 标记为 {@code String} 数组。
	 * <p>修剪令牌并省略空令牌。
	 * <p>给定的 {@code delimiters} 字符串可以由任意数量的分隔符字符组成。这些字符中的每一个都可以用来分隔令牌。
	 * 分隔符始终是单个字符;
	 * 对于多字符分隔符，请考虑使用 {@link #delimitedListToStringArray}。
	 *
	 * @param str        标记为令牌的 {@code String} (可能为 {@code null} 或空)
	 * @param delimiters 分隔符字符，组装为 {@code String} (每个字符单独视为分隔符)
	 * @return 令牌数组
	 * @see java.util.StringTokenizer
	 * @see String#trim()
	 * @see #delimitedListToStringArray
	 */
	public static String[] tokenizeToStringArray(@Nullable String str, String delimiters) {
		return tokenizeToStringArray(str, delimiters, true, true);
	}

	/**
	 * 通过 {@link StringTokenizer} 将给定的 {@code String} 标记为 {@code String} 数组。
	 * <p>给定的 {@code delimiters} 字符串可以由任意数量的分隔符字符组成。
	 * 这些字符中的每一个都可以用来分隔令牌。分隔符始终是单个字符;
	 * 对于多字符分隔符，请考虑使用 {@link #delimitedListToStringArray}。
	 *
	 * @param str               标记为令牌的 {@code String} (可能为 {@code null} 或空)
	 * @param delimiters        分隔符字符，组装为 {@code String} (每个字符单独视为分隔符)
	 * @param trimTokens        通过{@link String#trim()} 裁剪令牌
	 * @param ignoreEmptyTokens 从结果数组中省略空令牌 (仅适用于修剪后为空的令牌; StringTokenizer首先不会将后续分隔符视为令牌)。
	 * @return 令牌数组
	 * @see java.util.StringTokenizer
	 * @see String#trim()
	 * @see #delimitedListToStringArray
	 */
	public static String[] tokenizeToStringArray(
			@Nullable String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {

		if (str == null) {
			return EMPTY_STRING_ARRAY;
		}

		StringTokenizer st = new StringTokenizer(str, delimiters);
		List<String> tokens = new ArrayList<>();
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			if (trimTokens) {
				token = token.trim();
			}
			if (!ignoreEmptyTokens || token.length() > 0) {
				//如果忽略了空令牌或者令牌不为空，添加令牌。
				tokens.add(token);
			}
		}
		//将List转为数组
		return toStringArray(tokens);
	}

	/**
	 * 取一个 {@code String}，它是一个分隔列表，并将其转换为 {@code String} 数组。
	 * <p>单个 {@code String} 可能包含多个字符，但与 {@link #tokenizeToStringArray}
	 * 相反，它仍将被视为单个分隔符字符串，而不是一堆潜在的分隔符字符。
	 *
	 * @param str       输入 {@code String} (可能为 {@code null} 或空)
	 * @param delimiter 元素之间的分隔符 (这是单个分隔符，而不是一堆单独的分隔符字符)
	 * @return 列表中的令牌数组
	 * @see #tokenizeToStringArray
	 */
	public static String[] delimitedListToStringArray(@Nullable String str, @Nullable String delimiter) {
		return delimitedListToStringArray(str, delimiter, null);
	}

	/**
	 * 取一个 {@code String}，它是一个分隔列表，并将其转换为 {@code String} 数组。
	 * <p>单个 {@code 分隔符} 可能包含多个字符，但与 {@link #tokenizeToStringArray} 相反，
	 * 它仍将被视为单个分隔符字符串，而不是一堆潜在的分隔符字符。
	 *
	 * @param str           输入 {@code String} (可能为 {@code null} 或空)
	 * @param delimiter     元素之间的分隔符 (这是单个分隔符，而不是一堆单独的分隔符字符)
	 * @param charsToDelete 要删除的一组字符; 用于删除不需要的换行符: 例如 “\r \n \f” 将删除 {@code String} 中的所有新行和换行符
	 * @return 列表中的令牌数组
	 * @see #tokenizeToStringArray
	 */
	public static String[] delimitedListToStringArray(
			@Nullable String str, @Nullable String delimiter, @Nullable String charsToDelete) {

		if (str == null) {
			//原字符串为空，返回空的字符串数组
			return EMPTY_STRING_ARRAY;
		}
		if (delimiter == null) {
			//分隔符为空，返回含有原字符串的字符串数组
			return new String[]{str};
		}

		List<String> result = new ArrayList<>();
		if (delimiter.isEmpty()) {
			//如果分割符为空字符窜，
			for (int i = 0; i < str.length(); i++) {
				//删除指定的字符，并将删除后的字符串添加到结果
				result.add(deleteAny(str.substring(i, i + 1), charsToDelete));
			}
		} else {
			int pos = 0;
			int delPos;
			while ((delPos = str.indexOf(delimiter, pos)) != -1) {
				//如果下一个分割符位置存在，将与前一个分割符的字符串删除掉指定字符后，添加到结果中
				result.add(deleteAny(str.substring(pos, delPos), charsToDelete));
				//将位置设置为删除位置+分隔符长度
				pos = delPos + delimiter.length();
			}
			if (str.length() > 0 && pos <= str.length()) {
				//如果原字符串有长度，且最后一个分割符的位置小于等于该长度，添加上最后一部分的字符串
				// 添加字符串的其余部分，但在输入为空的情况下不添加。
				result.add(deleteAny(str.substring(pos), charsToDelete));
			}
		}
		//将List转为数组返回
		return toStringArray(result);
	}

	/**
	 * 将逗号分隔的列表 (例如，来自CSV文件的行) 转换为字符串数组。
	 *
	 * @param str 输入 {@code String} (可能是{@code null} 或空)
	 * @return 字符串数组，或者在输入为空的情况下的空数组
	 */
	public static String[] commaDelimitedListToStringArray(@Nullable String str) {
		return delimitedListToStringArray(str, ",");
	}

	/**
	 * 将逗号分隔的列表（例如 CSV 文件中的一行）转换为一个集合。
	 * <p>请注意，该方法会去除重复项；自 4.2 起，返回的集合使用 {@link LinkedHashSet}，
	 * 能够保留原始字符串中元素的顺序。
	 *
	 * @param str 输入的 {@code String}（可能为 {@code null} 或空）
	 * @return 字符串列表中的元素组成的集合
	 * @see #removeDuplicateStrings(String[])
	 */
	public static Set<String> commaDelimitedListToSet(@Nullable String str) {
		String[] tokens = commaDelimitedListToStringArray(str);
		return new LinkedHashSet<>(Arrays.asList(tokens));
	}

	/**
	 * 将 {@link Collection} 转换为使用分隔符连接的 {@code String}（例如 CSV）。
	 * <p>可用于实现 {@code toString()} 方法时的字符串构建。
	 *
	 * @param coll   要转换的 {@code Collection}（可能为 {@code null} 或空）
	 * @param delim  要使用的分隔符（通常是 ","）
	 * @param prefix 每个元素前缀字符串
	 * @param suffix 每个元素后缀字符串
	 * @return 用分隔符连接的字符串
	 */
	public static String collectionToDelimitedString(
			@Nullable Collection<?> coll, String delim, String prefix, String suffix) {

		if (CollectionUtils.isEmpty(coll)) {
			return "";
		}

		int totalLength = coll.size() * (prefix.length() + suffix.length()) + (coll.size() - 1) * delim.length();
		for (Object element : coll) {
			totalLength += String.valueOf(element).length();
		}

		StringBuilder sb = new StringBuilder(totalLength);
		Iterator<?> it = coll.iterator();
		while (it.hasNext()) {
			sb.append(prefix).append(it.next()).append(suffix);
			if (it.hasNext()) {
				sb.append(delim);
			}
		}
		return sb.toString();
	}

	/**
	 * 将 {@code Collection} 转换为使用分隔符连接的 {@code String}（例如 CSV）。
	 * <p>可用于实现 {@code toString()} 方法时的字符串构建。
	 *
	 * @param coll  要转换的 {@code Collection}（可能为 {@code null} 或空）
	 * @param delim 要使用的分隔符（通常是 ","）
	 * @return 用分隔符连接的字符串
	 */
	public static String collectionToDelimitedString(@Nullable Collection<?> coll, String delim) {
		return collectionToDelimitedString(coll, delim, "", "");
	}

	/**
	 * 将 {@code Collection} 转换为使用分隔符连接的 {@code String}（例如 CSV 格式）。
	 * <p>可用于实现 {@code toString()} 方法时的字符串构建。
	 *
	 * @param coll 要转换的 {@code Collection}（可能为 {@code null} 或空）
	 * @return 用分隔符连接的字符串
	 */
	public static String collectionToCommaDelimitedString(@Nullable Collection<?> coll) {
		return collectionToDelimitedString(coll, ",");
	}

	/**
	 * 将 {@code String} 数组转换为使用指定分隔符连接的 {@code String}（例如 CSV）。
	 * <p>可用于实现 {@code toString()} 方法时的字符串构建。
	 *
	 * @param arr   要展示的数组（可能为 {@code null} 或空）
	 * @param delim 要使用的分隔符（通常是 ","）
	 * @return 用分隔符连接的字符串
	 */
	public static String arrayToDelimitedString(@Nullable Object[] arr, String delim) {
		if (ObjectUtils.isEmpty(arr)) {
			return "";
		}
		if (arr.length == 1) {
			return ObjectUtils.nullSafeToString(arr[0]);
		}

		StringJoiner sj = new StringJoiner(delim);
		for (Object elem : arr) {
			sj.add(String.valueOf(elem));
		}
		return sj.toString();
	}

	/**
	 * 将 {@code String} 数组转换为逗号分隔的 {@code String}（即 CSV 格式）。
	 * <p>可用于实现 {@code toString()} 方法时的字符串构建。
	 *
	 * @param arr 要展示的数组（可能为 {@code null} 或空）
	 * @return 逗号分隔的字符串
	 */
	public static String arrayToCommaDelimitedString(@Nullable Object[] arr) {
		return arrayToDelimitedString(arr, ",");
	}

}
