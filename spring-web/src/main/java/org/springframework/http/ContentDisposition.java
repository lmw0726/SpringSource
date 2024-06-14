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

package org.springframework.http;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * 表示 RFC 6266 中定义的Content-Disposition类型和参数。
 * <p>
 * 该类用于处理Content-Disposition头部，包括类型和参数的解析。
 *
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Sergey Tsypanov
 * @see <a href="https://tools.ietf.org/html/rfc6266">RFC 6266</a>
 * @since 5.0
 */
public final class ContentDisposition {

	/**
	 * 正则表达式模式，用于匹配Base64编码的头字段参数。
	 */
	private final static Pattern BASE64_ENCODED_PATTERN =
			Pattern.compile("=\\?([0-9a-zA-Z-_]+)\\?B\\?([+/0-9a-zA-Z]+=*)\\?=");

	/**
	 * 无效的头字段参数格式（如RFC 5987定义）错误消息。
	 */
	private static final String INVALID_HEADER_FIELD_PARAMETER_FORMAT =
			"Invalid header field parameter format (as defined in RFC 5987)";


	/**
	 * 文件的类型。
	 */
	@Nullable
	private final String type;

	/**
	 * 文件的名称。
	 */
	@Nullable
	private final String name;

	/**
	 * 文件的原始文件名。
	 */
	@Nullable
	private final String filename;

	/**
	 * 文件的字符集。
	 */
	@Nullable
	private final Charset charset;

	/**
	 * 文件的大小。
	 */
	@Nullable
	private final Long size;

	/**
	 * 文件的创建日期。
	 */
	@Nullable
	private final ZonedDateTime creationDate;

	/**
	 * 文件的修改日期。
	 */
	@Nullable
	private final ZonedDateTime modificationDate;

	/**
	 * 文件的读取日期。
	 */
	@Nullable
	private final ZonedDateTime readDate;


	/**
	 * 私有构造函数。请参考本类中的静态工厂方法。
	 */
	private ContentDisposition(@Nullable String type, @Nullable String name, @Nullable String filename,
							   @Nullable Charset charset, @Nullable Long size, @Nullable ZonedDateTime creationDate,
							   @Nullable ZonedDateTime modificationDate, @Nullable ZonedDateTime readDate) {

		this.type = type;
		this.name = name;
		this.filename = filename;
		this.charset = charset;
		this.size = size;
		this.creationDate = creationDate;
		this.modificationDate = modificationDate;
		this.readDate = readDate;
	}


	/**
	 * 返回 {@link #getType() type} 是否为 {@literal "attachment"}。
	 *
	 * @since 5.3
	 */
	public boolean isAttachment() {
		return (this.type != null && this.type.equalsIgnoreCase("attachment"));
	}

	/**
	 * 返回 {@link #getType() type} 是否为 {@literal "form-data"}。
	 *
	 * @since 5.3
	 */
	public boolean isFormData() {
		return (this.type != null && this.type.equalsIgnoreCase("form-data"));
	}

	/**
	 * 返回 {@link #getType() type} 是否为 {@literal "inline"}。
	 *
	 * @since 5.3
	 */
	public boolean isInline() {
		return (this.type != null && this.type.equalsIgnoreCase("inline"));
	}

	/**
	 * 返回内容的类型。
	 *
	 * @see #isAttachment()
	 * @see #isFormData()
	 * @see #isInline()
	 */
	@Nullable
	public String getType() {
		return this.type;
	}

	/**
	 * 返回 {@literal name} 参数的值，如果未定义则返回 {@code null}。
	 */
	@Nullable
	public String getName() {
		return this.name;
	}

	/**
	 * 返回 {@literal filename} 参数的值，可能根据 RFC 2047 中的 BASE64 编码进行解码，
	 * 或者根据 RFC 5987 进行解码。
	 */
	@Nullable
	public String getFilename() {
		return this.filename;
	}

	/**
	 * 返回 {@literal filename*} 参数中定义的字符集，如果未定义则返回 {@code null}。
	 */
	@Nullable
	public Charset getCharset() {
		return this.charset;
	}

	/**
	 * 返回 {@literal size} 参数的值，如果未定义则返回 {@code null}。
	 *
	 * @deprecated 自5.2.3起，根据
	 * <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266，附录B</a>，
	 * 将在将来的版本中删除。
	 */
	@Deprecated
	@Nullable
	public Long getSize() {
		return this.size;
	}

	/**
	 * 返回 {@literal creation-date} 参数的值，如果未定义则返回 {@code null}。
	 *
	 * @deprecated 自5.2.3起，根据
	 * <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266，附录B</a>，
	 * 将在将来的版本中删除。
	 */
	@Deprecated
	@Nullable
	public ZonedDateTime getCreationDate() {
		return this.creationDate;
	}

	/**
	 * 返回 {@literal modification-date} 参数的值，如果未定义则返回 {@code null}。
	 *
	 * @deprecated 自5.2.3起，根据
	 * <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266，附录B</a>，
	 * 将在将来的版本中删除。
	 */
	@Deprecated
	@Nullable
	public ZonedDateTime getModificationDate() {
		return this.modificationDate;
	}

	/**
	 * 返回 {@literal read-date} 参数的值，如果未定义则返回 {@code null}。
	 *
	 * @deprecated 自5.2.3起，根据
	 * <a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266，附录B</a>，
	 * 将在将来的版本中删除。
	 */
	@Deprecated
	@Nullable
	public ZonedDateTime getReadDate() {
		return this.readDate;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ContentDisposition)) {
			return false;
		}
		ContentDisposition otherCd = (ContentDisposition) other;
		return (ObjectUtils.nullSafeEquals(this.type, otherCd.type) &&
				ObjectUtils.nullSafeEquals(this.name, otherCd.name) &&
				ObjectUtils.nullSafeEquals(this.filename, otherCd.filename) &&
				ObjectUtils.nullSafeEquals(this.charset, otherCd.charset) &&
				ObjectUtils.nullSafeEquals(this.size, otherCd.size) &&
				ObjectUtils.nullSafeEquals(this.creationDate, otherCd.creationDate) &&
				ObjectUtils.nullSafeEquals(this.modificationDate, otherCd.modificationDate) &&
				ObjectUtils.nullSafeEquals(this.readDate, otherCd.readDate));
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(this.type);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.name);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.filename);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.charset);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.size);
		result = 31 * result + (this.creationDate != null ? this.creationDate.hashCode() : 0);
		result = 31 * result + (this.modificationDate != null ? this.modificationDate.hashCode() : 0);
		result = 31 * result + (this.readDate != null ? this.readDate.hashCode() : 0);
		return result;
	}

	/**
	 * 返回此内容分发的头部值，如 RFC 6266 中定义。
	 *
	 * @see #parse(String)
	 */
	@Override
	public String toString() {
		// 创建一个 StringBuilder 对象，用于拼接头部信息的字符串
		StringBuilder sb = new StringBuilder();

		// 如果类型不为空，则添加类型信息
		if (this.type != null) {
			sb.append(this.type);
		}

		// 如果名称不为空，则添加名称信息
		if (this.name != null) {
			sb.append("; name=\"");
			sb.append(this.name).append('\"');
		}

		// 如果文件名不为空，则根据字符集情况添加文件名信息
		if (this.filename != null) {
			if (this.charset == null || StandardCharsets.US_ASCII.equals(this.charset)) {
				// 如果字符集为空或者为 US_ASCII，则添加普通的 filename="..." 形式
				sb.append("; filename=\"");
				sb.append(escapeQuotationsInFilename(this.filename)).append('\"');
			} else {
				// 否则，使用 filename*=... 形式，并编码文件名
				sb.append("; filename*=");
				sb.append(encodeFilename(this.filename, this.charset));
			}
		}

		// 如果大小不为空，则添加大小信息
		if (this.size != null) {
			sb.append("; size=");
			sb.append(this.size);
		}

		// 如果创建日期不为空，则添加创建日期信息
		if (this.creationDate != null) {
			sb.append("; creation-date=\"");
			sb.append(RFC_1123_DATE_TIME.format(this.creationDate));
			sb.append('\"');
		}

		// 如果修改日期不为空，则添加修改日期信息
		if (this.modificationDate != null) {
			sb.append("; modification-date=\"");
			sb.append(RFC_1123_DATE_TIME.format(this.modificationDate));
			sb.append('\"');
		}

		// 如果阅读日期不为空，则添加阅读日期信息
		if (this.readDate != null) {
			sb.append("; read-date=\"");
			sb.append(RFC_1123_DATE_TIME.format(this.readDate));
			sb.append('\"');
		}

		// 返回拼接完成的头部信息字符串
		return sb.toString();
	}


	/**
	 * 返回一个类型为 {@literal "attachment"} 的 {@code ContentDisposition} 的构建器。
	 *
	 * @since 5.3
	 */
	public static Builder attachment() {
		return builder("attachment");
	}

	/**
	 * 返回一个类型为 {@literal "form-data"} 的 {@code ContentDisposition} 的构建器。
	 *
	 * @since 5.3
	 */
	public static Builder formData() {
		return builder("form-data");
	}

	/**
	 * 返回一个类型为 {@literal "inline"} 的 {@code ContentDisposition} 的构建器。
	 *
	 * @since 5.3
	 */
	public static Builder inline() {
		return builder("inline");
	}

	/**
	 * 返回一个 {@code ContentDisposition} 的构建器。
	 *
	 * @param type 内容分发类型，例如 {@literal inline}、{@literal attachment} 或 {@literal form-data}
	 * @return 构建器
	 */
	public static Builder builder(String type) {
		return new BuilderImpl(type);
	}

	/**
	 * 返回一个空的内容分发。
	 */
	public static ContentDisposition empty() {
		return new ContentDisposition("", null, null, null, null, null, null, null);
	}

	/**
	 * 解析 RFC 2183 中定义的 {@literal Content-Disposition} 头部值。
	 *
	 * @param contentDisposition {@literal Content-Disposition} 头部值
	 * @return 解析后的内容分发对象
	 * @see #toString()
	 */
	public static ContentDisposition parse(String contentDisposition) {
		// 将 Content-Disposition 头部字符串解析为部分列表
		List<String> parts = tokenize(contentDisposition);

		// 第一个部分是类型，例如 "form-data" 或 "attachment"
		String type = parts.get(0);

		// 初始化变量用于存储解析后的属性值
		String name = null;
		String filename = null;
		Charset charset = null;
		Long size = null;
		ZonedDateTime creationDate = null;
		ZonedDateTime modificationDate = null;
		ZonedDateTime readDate = null;

		// 遍历 Content-Disposition 头部的每个部分（属性）
		for (int i = 1; i < parts.size(); i++) {
			String part = parts.get(i);
			int eqIndex = part.indexOf('=');

			// 检查是否存在等号，如果不存在则抛出异常
			if (eqIndex != -1) {
				String attribute = part.substring(0, eqIndex);
				String value = (part.startsWith("\"", eqIndex + 1) && part.endsWith("\"") ?
						part.substring(eqIndex + 2, part.length() - 1) :
						part.substring(eqIndex + 1));

				// 根据属性名称解析对应的值
				if (attribute.equals("name")) {
					name = value;
				} else if (attribute.equals("filename*")) {
					// 解析包含字符集信息的文件名
					int idx1 = value.indexOf('\'');
					int idx2 = value.indexOf('\'', idx1 + 1);
					if (idx1 != -1 && idx2 != -1) {
						charset = Charset.forName(value.substring(0, idx1).trim());
						Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset),
								"Charset should be UTF-8 or ISO-8859-1");
						filename = decodeFilename(value.substring(idx2 + 1), charset);
					} else {
						// 默认使用 US ASCII 解码文件名
						filename = decodeFilename(value, StandardCharsets.US_ASCII);
					}
				} else if (attribute.equals("filename") && (filename == null)) {
					// 解析普通的文件名属性值
					// 检查值是否以 "=?" 开头，表示可能使用了Base64编码
					if (value.startsWith("=?")) {
						// 使用正则表达式匹配Base64编码的格式
						Matcher matcher = BASE64_ENCODED_PATTERN.matcher(value);
						if (matcher.find()) {
							// 提取匹配的编码字符集（match1）和Base64编码的文件名部分（match2）
							String match1 = matcher.group(1);
							String match2 = matcher.group(2);

							// 使用指定的字符集解码Base64编码的文件名
							filename = new String(Base64.getDecoder().decode(match2), Charset.forName(match1));
						} else {
							// 如果正则匹配失败，直接使用原始值作为文件名
							filename = value;
						}
					} else {
						// 如果值不以 "=?" 开头，直接使用原始值作为文件名
						filename = value;
					}
				} else if (attribute.equals("size")) {
					// 解析文件大小
					size = Long.parseLong(value);
				} else if (attribute.equals("creation-date")) {
					// 解析创建日期
					try {
						creationDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
					} catch (DateTimeParseException ex) {
						// 如果日期格式错误，则忽略异常
					}
				} else if (attribute.equals("modification-date")) {
					// 解析修改日期
					try {
						modificationDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
					} catch (DateTimeParseException ex) {
						// 如果日期格式错误，则忽略异常
					}
				} else if (attribute.equals("read-date")) {
					// 解析阅读日期
					try {
						readDate = ZonedDateTime.parse(value, RFC_1123_DATE_TIME);
					} catch (DateTimeParseException ex) {
						// 如果日期格式错误，则忽略异常
					}
				}
			} else {
				// 如果属性部分格式不正确，则抛出异常
				throw new IllegalArgumentException("Invalid content disposition format");
			}
		}

		// 返回封装了所有解析属性的 ContentDisposition 对象
		return new ContentDisposition(type, name, filename, charset, size, creationDate, modificationDate, readDate);
	}

	private static List<String> tokenize(String headerValue) {
		// 查找第一个分号的索引位置
		int index = headerValue.indexOf(';');

		// 提取类型部分（如果有分号则截取分号前的部分，否则整个字符串），并去除两侧空白
		String type = (index >= 0 ? headerValue.substring(0, index) : headerValue).trim();

		// 如果类型部分为空，则抛出异常
		if (type.isEmpty()) {
			throw new IllegalArgumentException("Content-Disposition header must not be empty");
		}

		// 创建一个列表来存储类型和参数
		List<String> parts = new ArrayList<>();
		parts.add(type);

		// 如果存在分号
		if (index >= 0) {
			// 循环处理分号后的参数部分
			do {
				int nextIndex = index + 1;
				// 是否在双引号内部
				boolean quoted = false;
				// 是否有转义字符
				boolean escaped = false;
				// 寻找下一个分号或字符串末尾
				while (nextIndex < headerValue.length()) {
					char ch = headerValue.charAt(nextIndex);
					if (ch == ';') {
						if (!quoted) {
							break;
						}
					} else if (!escaped && ch == '"') {
						// 切换双引号状态
						quoted = !quoted;
					}
					// 检查是否有转义字符
					escaped = (!escaped && ch == '\\');
					nextIndex++;
				}
				// 提取并去除参数部分两侧空白
				String part = headerValue.substring(index + 1, nextIndex).trim();
				// 如果参数部分非空，则加入到列表中
				if (!part.isEmpty()) {
					parts.add(part);
				}
				// 更新索引到下一个分号位置
				index = nextIndex;
				// 循环直到处理完整个字符串
			}
			while (index < headerValue.length());
		}

		// 返回解析后的类型和参数列表
		return parts;
	}

	/**
	 * 根据 RFC 5987 描述，解码给定的头字段参数。
	 * <p>仅支持 US-ASCII、UTF-8 和 ISO-8859-1 字符集。
	 *
	 * @param filename 文件名
	 * @param charset  文件名的字符集
	 * @return 解码后的头字段参数
	 * @see <a href="https://tools.ietf.org/html/rfc5987">RFC 5987</a>
	 */
	private static String decodeFilename(String filename, Charset charset) {
		Assert.notNull(filename, "'input' String` should not be null");
		Assert.notNull(charset, "'charset' should not be null");
		// 将文件名按照指定字符集转换成字节数组
		byte[] value = filename.getBytes(charset);

		// 创建一个 ByteArrayOutputStream 对象
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		// 初始化索引
		int index = 0;

		// 循环处理字节数组中的每个字节
		while (index < value.length) {
			byte b = value[index];

			// 如果当前字节是 RFC 5987 中允许的字符
			if (isRFC5987AttrChar(b)) {
				// 将当前字节写入 ByteArrayOutputStream
				baos.write((char) b);
				index++;
			}
			// 如果当前字节是 '%'，并且后面还有足够的字节
			else if (b == '%' && index < value.length - 2) {
				// 获取 '%' 后面的两个字符，构成十六进制表示的字节数组
				char[] array = new char[]{(char) value[index + 1], (char) value[index + 2]};
				try {
					// 将十六进制表示的字节数组转换为整数，并写入 ByteArrayOutputStream
					baos.write(Integer.parseInt(String.valueOf(array), 16));
				} catch (NumberFormatException ex) {
					// 如果转换失败，抛出异常
					throw new IllegalArgumentException(INVALID_HEADER_FIELD_PARAMETER_FORMAT, ex);
				}
				index += 3; // 移动索引到下一个字节
			}
			// 如果不符合 RFC 5987 规范的字符
			else {
				throw new IllegalArgumentException(INVALID_HEADER_FIELD_PARAMETER_FORMAT);
			}
		}

		// 将 ByteArrayOutputStream 中的内容以指定字符集转换成字符串并返回
		return StreamUtils.copyToString(baos, charset);
	}

	private static boolean isRFC5987AttrChar(byte c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
				c == '!' || c == '#' || c == '$' || c == '&' || c == '+' || c == '-' ||
				c == '.' || c == '^' || c == '_' || c == '`' || c == '|' || c == '~';
	}

	private static String escapeQuotationsInFilename(String filename) {
		// 检查文件名中是否不包含双引号和反斜杠
		if (filename.indexOf('"') == -1 && filename.indexOf('\\') == -1) {
			// 如果不包含，直接返回文件名
			return filename;
		}

		// 标志用于指示当前字符是否已经被转义
		boolean escaped = false;

		// 创建一个 字符串构建器 对象来构建处理后的文件名
		StringBuilder sb = new StringBuilder();

		// 遍历文件名中的每个字符
		for (int i = 0; i < filename.length(); i++) {
			char c = filename.charAt(i);

			// 如果当前字符未被转义且为双引号
			if (!escaped && c == '"') {
				// 将双引号转义为 "\""
				sb.append("\\\"");
			} else {
				// 否则直接将字符添加到 字符串构建器 中
				sb.append(c);
			}

			// 更新转义标志，如果当前字符为反斜杠且前面没有转义过
			escaped = (!escaped && c == '\\');
		}

		// 如果最后一个字符是反斜杠，删除该反斜杠
		if (escaped) {
			sb.deleteCharAt(sb.length() - 1);
		}

		// 返回处理后的文件名字符串
		return sb.toString();
	}

	/**
	 * 根据RFC 5987描述，对给定的头字段参数进行编码。
	 *
	 * @param input   头字段参数
	 * @param charset 头字段参数字符串的字符集，仅支持US-ASCII、UTF-8和ISO-8859-1字符集
	 * @return 编码后的头字段参数
	 * @see <a href="https://tools.ietf.org/html/rfc5987">RFC 5987</a>
	 */
	private static String encodeFilename(String input, Charset charset) {
		Assert.notNull(input, "`input` is required");
		Assert.notNull(charset, "`charset` is required");
		Assert.isTrue(!StandardCharsets.US_ASCII.equals(charset), "ASCII does not require encoding");
		Assert.isTrue(UTF_8.equals(charset) || ISO_8859_1.equals(charset), "Only UTF-8 and ISO-8859-1 supported.");
		// 将输入的字符串按指定字符集转换成字节数组
		byte[] source = input.getBytes(charset);

		// 获取字节数组的长度
		int len = source.length;

		// 创建一个初始容量为原字节数组长度左移一位的 字符串构建器 对象
		StringBuilder sb = new StringBuilder(len << 1);

		// 在 字符串构建器 中追加字符集的名称
		sb.append(charset.name());

		// 追加 RFC 5987 中的分隔符
		sb.append("''");

		// 遍历字节数组中的每个字节
		for (byte b : source) {
			// 如果字节是 RFC 5987 中允许的字符
			if (isRFC5987AttrChar(b)) {
				// 直接追加字符
				sb.append((char) b);
			} else {
				// 否则，按 RFC 5987 规定进行编码
				sb.append('%');
				// 获取字节的高四位并转换为大写十六进制字符
				char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, 16));
				// 获取字节的低四位并转换为大写十六进制字符
				char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, 16));
				// 追加十六进制字符到 字符串构建器 中
				sb.append(hex1);
				sb.append(hex2);
			}
		}

		// 返回 字符串构建器 对象转换成的字符串
		return sb.toString();
	}


	/**
	 * {@code ContentDisposition}的可变构建器。
	 */
	public interface Builder {

		/**
		 * 设置{@literal name}参数的值。
		 */
		Builder name(String name);

		/**
		 * 设置{@literal filename}参数的值。给定的文件名将被格式化为引用字符串，
		 * 如RFC 2616第2.2节中定义的，并且文件名值中的任何引号字符都将用反斜杠转义，
		 * 例如{@code "foo\"bar.txt"} 变成 {@code "foo\\\"bar.txt"}。
		 */
		Builder filename(String filename);

		/**
		 * 设置将根据RFC 5987定义编码的{@code filename}的值。
		 * 仅支持US-ASCII、UTF-8和ISO-8859-1字符集。
		 * <p><strong>注意：</strong>不要用于{@code "multipart/form-data"}请求，
		 * 因为<a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578，第4.2节</a>
		 * 和RFC 5987提到它不适用于多部分请求。
		 */
		Builder filename(String filename, Charset charset);

		/**
		 * 设置{@literal size}参数的值。
		 *
		 * @deprecated 自5.2.3起，根据<a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266，附录B</a>，
		 * 在将来的版本中将被移除。
		 */
		@Deprecated
		Builder size(Long size);

		/**
		 * 设置{@literal creation-date}参数的值。
		 *
		 * @deprecated 自5.2.3起，根据<a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266，附录B</a>，
		 * 在将来的版本中将被移除。
		 */
		@Deprecated
		Builder creationDate(ZonedDateTime creationDate);

		/**
		 * 设置{@literal modification-date}参数的值。
		 *
		 * @deprecated 自5.2.3起，根据<a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266，附录B</a>，
		 * 在将来的版本中将被移除。
		 */
		@Deprecated
		Builder modificationDate(ZonedDateTime modificationDate);

		/**
		 * 设置{@literal read-date}参数的值。
		 *
		 * @deprecated 自5.2.3起，根据<a href="https://tools.ietf.org/html/rfc6266#appendix-B">RFC 6266，附录B</a>，
		 * 在将来的版本中将被移除。
		 */
		@Deprecated
		Builder readDate(ZonedDateTime readDate);

		/**
		 * 构建内容描述。
		 */
		ContentDisposition build();
	}


	private static class BuilderImpl implements Builder {

		/**
		 * 表示一个文件的元数据。
		 * 包括类型、名称、文件名、字符集、大小、创建日期、修改日期和读取日期等信息。
		 */
		private final String type;

		/**
		 * 文件的名称。
		 */
		@Nullable
		private String name;

		/**
		 * 文件的原始文件名。
		 */
		@Nullable
		private String filename;

		/**
		 * 文件的字符集。
		 */
		@Nullable
		private Charset charset;

		/**
		 * 文件的大小。
		 */
		@Nullable
		private Long size;

		/**
		 * 文件的创建日期。
		 */
		@Nullable
		private ZonedDateTime creationDate;

		/**
		 * 文件的修改日期。
		 */
		@Nullable
		private ZonedDateTime modificationDate;

		/**
		 * 文件的读取日期。
		 */
		@Nullable
		private ZonedDateTime readDate;

		public BuilderImpl(String type) {
			Assert.hasText(type, "'type' must not be not empty");
			this.type = type;
		}

		@Override
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		@Override
		public Builder filename(String filename) {
			Assert.hasText(filename, "No filename");
			this.filename = filename;
			return this;
		}

		@Override
		public Builder filename(String filename, Charset charset) {
			Assert.hasText(filename, "No filename");
			this.filename = filename;
			this.charset = charset;
			return this;
		}

		@Override
		@SuppressWarnings("deprecation")
		public Builder size(Long size) {
			this.size = size;
			return this;
		}

		@Override
		@SuppressWarnings("deprecation")
		public Builder creationDate(ZonedDateTime creationDate) {
			this.creationDate = creationDate;
			return this;
		}

		@Override
		@SuppressWarnings("deprecation")
		public Builder modificationDate(ZonedDateTime modificationDate) {
			this.modificationDate = modificationDate;
			return this;
		}

		@Override
		@SuppressWarnings("deprecation")
		public Builder readDate(ZonedDateTime readDate) {
			this.readDate = readDate;
			return this;
		}

		@Override
		public ContentDisposition build() {
			return new ContentDisposition(this.type, this.name, this.filename, this.charset,
					this.size, this.creationDate, this.modificationDate, this.readDate);
		}
	}

}
