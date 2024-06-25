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

package org.springframework.http.codec.multipart;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 处理多部分解析的各种静态实用方法。
 *
 * @author Arjen Poutsma
 * @since 5.3
 */
abstract class MultipartUtils {

	/**
	 * 返回给定头部的字符集，如 {@link HttpHeaders#getContentType()} 头部定义。
	 *
	 * @param headers 头部信息
	 * @return 给定头部的字符集，如果未定义则返回 UTF-8
	 */
	public static Charset charset(HttpHeaders headers) {
		// 获取请求头中的 内容类型 
		MediaType contentType = headers.getContentType();
		// 如果 内容类型 不为null
		if (contentType != null) {
			// 获取 内容类型 中的字符集
			Charset charset = contentType.getCharset();
			// 如果字符集不为null，则返回该字符集
			if (charset != null) {
				return charset;
			}
		}
		// 如果没有找到字符集信息，则返回默认的UTF-8字符集
		return StandardCharsets.UTF_8;
	}

	/**
	 * 连接给定的字节数组。
	 *
	 * @param byteArrays 要连接的字节数组
	 * @return 连接后的结果字节数组
	 */
	public static byte[] concat(byte[]... byteArrays) {
		// 计算所有字节数组的总长度
		int len = 0;
		for (byte[] byteArray : byteArrays) {
			len += byteArray.length;
		}

		// 创建一个结果字节数组，长度为所有字节数组的总长度
		byte[] result = new byte[len];

		// 将每个字节数组的内容复制到结果数组中
		len = 0;
		for (byte[] byteArray : byteArrays) {
			System.arraycopy(byteArray, 0, result, len, byteArray.length);
			len += byteArray.length;
		}

		// 返回合并后的结果数组
		return result;
	}

	/**
	 * 将给定的缓冲区切片到指定索引（不包括该索引）。
	 *
	 * @param buf 缓冲区
	 * @param idx 索引位置
	 * @return 切片后的数据缓冲区
	 */
	public static DataBuffer sliceTo(DataBuffer buf, int idx) {
		// 获取 缓冲区 的读取位置
		int pos = buf.readPosition();
		// 计算从 读取位置 到 索引位置（包括 索引位置）之间的长度
		int len = idx - pos + 1;
		// 返回从 读取位置 开始，上面的长度的 缓冲区 的切片（并保持引用计数）
		return buf.retainedSlice(pos, len);
	}

	/**
	 * 从给定索引（包括该索引）开始切片给定的缓冲区。
	 *
	 * @param buf 缓冲区
	 * @param idx 索引位置
	 * @return 切片后的数据缓冲区
	 */
	public static DataBuffer sliceFrom(DataBuffer buf, int idx) {
		// 计算从 索引位置 + 1到buf的写入位置之间的长度
		int len = buf.writePosition() - idx - 1;
		// 返回从 索引位置 + 1开始，指定长度 的 缓冲区 的切片（并保持引用计数）
		return buf.retainedSlice(idx + 1, len);
	}

	/**
	 * 关闭给定的通道。
	 *
	 * @param channel 要关闭的通道
	 */
	public static void closeChannel(Channel channel) {
		try {
			if (channel.isOpen()) {
				// 如果通道打开，则关闭通道
				channel.close();
			}
		} catch (IOException ignore) {
			// 忽略异常
		}
	}

}
