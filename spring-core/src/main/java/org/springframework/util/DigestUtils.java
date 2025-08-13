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

package org.springframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 计算摘要的各种杂项方法。
 *
 * <p>主要供框架内部使用；如果需要更全面的摘要工具集，
 * 请考虑使用
 * <a href="https://commons.apache.org/codec/">Apache Commons Codec</a>。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Craig Andrews
 * @since 3.0
 */
public abstract class DigestUtils {

	private static final String MD5_ALGORITHM_NAME = "MD5";

	private static final char[] HEX_CHARS =
			{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};


	/**
	 * 计算给定字节数组的 MD5 摘要。
	 * @param bytes 要计算摘要的字节数组
	 * @return 摘要字节数组
	 */
	public static byte[] md5Digest(byte[] bytes) {
		return digest(MD5_ALGORITHM_NAME, bytes);
	}

	/**
	 * 计算给定输入流的 MD5 摘要。
	 * <p>此方法<strong>不会</strong>关闭输入流。
	 * @param inputStream 要计算摘要的输入流
	 * @return 摘要字节数组
	 * @since 4.2
	 */
	public static byte[] md5Digest(InputStream inputStream) throws IOException {
		return digest(MD5_ALGORITHM_NAME, inputStream);
	}

	/**
	 * 返回给定字节数组的 MD5 摘要的十六进制字符串表示。
	 * @param bytes 要计算摘要的字节数组
	 * @return 十六进制的摘要字符串
	 */
	public static String md5DigestAsHex(byte[] bytes) {
		return digestAsHexString(MD5_ALGORITHM_NAME, bytes);
	}

	/**
	 * 返回给定输入流的 MD5 摘要的十六进制字符串表示。
	 * <p>此方法<strong>不会</strong>关闭输入流。
	 * @param inputStream 要计算摘要的输入流
	 * @return 十六进制的摘要字符串
	 * @since 4.2
	 */
	public static String md5DigestAsHex(InputStream inputStream) throws IOException {
		return digestAsHexString(MD5_ALGORITHM_NAME, inputStream);
	}

	/**
	 * 将给定字节数组的 MD5 摘要的十六进制字符串表示追加到指定的 {@link StringBuilder} 中。
	 * @param bytes 要计算摘要的字节数组
	 * @param builder 用于追加摘要的字符串构建器
	 * @return 传入的字符串构建器
	 */
	public static StringBuilder appendMd5DigestAsHex(byte[] bytes, StringBuilder builder) {
		return appendDigestAsHex(MD5_ALGORITHM_NAME, bytes, builder);
	}

	/**
	 * 将给定输入流的 MD5 摘要的十六进制字符串表示追加到指定的 {@link StringBuilder} 中。
	 * <p>此方法<strong>不会</strong>关闭输入流。
	 * @param inputStream 要计算摘要的输入流
	 * @param builder 用于追加摘要的字符串构建器
	 * @return 传入的字符串构建器
	 * @since 4.2
	 */
	public static StringBuilder appendMd5DigestAsHex(InputStream inputStream, StringBuilder builder) throws IOException {
		return appendDigestAsHex(MD5_ALGORITHM_NAME, inputStream, builder);
	}


	/**
	 * 使用指定的算法创建一个新的 {@link MessageDigest} 实例。
	 * <p>此方法是必要的，因为 {@code MessageDigest} 不是线程安全的。
	 */
	private static MessageDigest getDigest(String algorithm) {
		try {
			return MessageDigest.getInstance(algorithm);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("Could not find MessageDigest with algorithm \"" + algorithm + "\"", ex);
		}
	}

	private static byte[] digest(String algorithm, byte[] bytes) {
		return getDigest(algorithm).digest(bytes);
	}

	private static byte[] digest(String algorithm, InputStream inputStream) throws IOException {
		MessageDigest messageDigest = getDigest(algorithm);
		if (inputStream instanceof UpdateMessageDigestInputStream){
			((UpdateMessageDigestInputStream) inputStream).updateMessageDigest(messageDigest);
			return messageDigest.digest();
		}
		else {
			final byte[] buffer = new byte[StreamUtils.BUFFER_SIZE];
			int bytesRead = -1;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				messageDigest.update(buffer, 0, bytesRead);
			}
			return messageDigest.digest();
		}
	}

	private static String digestAsHexString(String algorithm, byte[] bytes) {
		char[] hexDigest = digestAsHexChars(algorithm, bytes);
		return new String(hexDigest);
	}

	private static String digestAsHexString(String algorithm, InputStream inputStream) throws IOException {
		char[] hexDigest = digestAsHexChars(algorithm, inputStream);
		return new String(hexDigest);
	}

	private static StringBuilder appendDigestAsHex(String algorithm, byte[] bytes, StringBuilder builder) {
		char[] hexDigest = digestAsHexChars(algorithm, bytes);
		return builder.append(hexDigest);
	}

	private static StringBuilder appendDigestAsHex(String algorithm, InputStream inputStream, StringBuilder builder)
			throws IOException {

		char[] hexDigest = digestAsHexChars(algorithm, inputStream);
		return builder.append(hexDigest);
	}

	private static char[] digestAsHexChars(String algorithm, byte[] bytes) {
		byte[] digest = digest(algorithm, bytes);
		return encodeHex(digest);
	}

	private static char[] digestAsHexChars(String algorithm, InputStream inputStream) throws IOException {
		byte[] digest = digest(algorithm, inputStream);
		return encodeHex(digest);
	}

	private static char[] encodeHex(byte[] bytes) {
		char[] chars = new char[32];
		for (int i = 0; i < chars.length; i = i + 2) {
			byte b = bytes[i / 2];
			chars[i] = HEX_CHARS[(b >>> 0x4) & 0xf];
			chars[i + 1] = HEX_CHARS[b & 0xf];
		}
		return chars;
	}

}
