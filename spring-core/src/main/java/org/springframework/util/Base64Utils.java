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

package org.springframework.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 用于 Base64 编码和解码的简单工具类。
 *
 * <p>以便捷的方式适配 Java 8 的 {@link java.util.Base64}。
 *
 * @author Juergen Hoeller
 * @author Gary Russell
 * @since 4.1
 * @see java.util.Base64
 */
public abstract class Base64Utils {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;


	/**
	 * 对给定的字节数组进行 Base64 编码。
	 * @param src 原始字节数组
	 * @return 编码后的字节数组
	 */
	public static byte[] encode(byte[] src) {
		if (src.length == 0) {
			return src;
		}
		return Base64.getEncoder().encode(src);
	}

	/**
	 * 对给定的字节数组进行 Base64 解码。
	 * @param src 编码后的字节数组
	 * @return 原始字节数组
	 */
	public static byte[] decode(byte[] src) {
		if (src.length == 0) {
			return src;
		}
		return Base64.getDecoder().decode(src);
	}

	/**
	 * 使用 RFC 4648 "URL 和文件名安全字母表"对给定字节数组进行 Base64 编码。
	 * @param src 原始字节数组
	 * @return 编码后的字节数组
	 * @since 4.2.4
	 */
	public static byte[] encodeUrlSafe(byte[] src) {
		if (src.length == 0) {
			return src;
		}
		return Base64.getUrlEncoder().encode(src);
	}

	/**
	 * 使用 RFC 4648 "URL 和文件名安全字母表"对给定字节数组进行 Base64 解码。
	 * @param src 编码后的字节数组
	 * @return 原始字节数组
	 * @since 4.2.4
	 */
	public static byte[] decodeUrlSafe(byte[] src) {
		if (src.length == 0) {
			return src;
		}
		return Base64.getUrlDecoder().decode(src);
	}

	/**
	 * 将给定的字节数组 Base64 编码为字符串。
	 * @param src 原始字节数组
	 * @return 编码后的 UTF-8 字符串
	 */
	public static String encodeToString(byte[] src) {
		if (src.length == 0) {
			return "";
		}
		return new String(encode(src), DEFAULT_CHARSET);
	}

	/**
	 * 从 UTF-8 字符串 Base64 解码为字节数组。
	 * @param src 编码后的 UTF-8 字符串
	 * @return 原始字节数组
	 */
	public static byte[] decodeFromString(String src) {
		if (src.isEmpty()) {
			return new byte[0];
		}
		return decode(src.getBytes(DEFAULT_CHARSET));
	}

	/**
	 * 使用 RFC 4648 "URL 和文件名安全字母表"将给定字节数组 Base64 编码为字符串。
	 * @param src 原始字节数组
	 * @return 编码后的 UTF-8 字符串
	 */
	public static String encodeToUrlSafeString(byte[] src) {
		return new String(encodeUrlSafe(src), DEFAULT_CHARSET);
	}

	/**
	 * 使用 RFC 4648 "URL 和文件名安全字母表"从 UTF-8 字符串 Base64 解码为字节数组。
	 * @param src 编码后的 UTF-8 字符串
	 * @return 原始字节数组
	 */
	public static byte[] decodeFromUrlSafeString(String src) {
		return decodeUrlSafe(src.getBytes(DEFAULT_CHARSET));
	}

}
