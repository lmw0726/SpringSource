/*
 * Copyright 2002-2018 the original author or authors.
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

/**
 * {@link java.io.InputStream} 的扩展，允许对消息摘要进行优化实现。
 *
 * @author Craig Andrews
 * @since 4.2
 */
abstract class UpdateMessageDigestInputStream extends InputStream {

	/**
	 * 使用该流中剩余的字节更新消息摘要。
	 * <p>使用此方法更加高效，因为它避免了每次调用时创建新的字节数组。
	 * @param messageDigest 要更新的消息摘要
	 * @throws IOException 当由 {@link #read()} 抛出异常时传播
	 */
	public void updateMessageDigest(MessageDigest messageDigest) throws IOException {
		int data;
		while ((data = read()) != -1) {
			messageDigest.update((byte) data);
		}
	}

	/**
	 * 使用该流中接下来的 len 个字节更新消息摘要。
	 * <p>使用此方法更加高效，因为它避免了每次调用时创建新的字节数组。
	 * @param messageDigest 要更新的消息摘要
	 * @param len 从该流中读取的字节数，用于更新消息摘要
	 * @throws IOException 当由 {@link #read()} 抛出异常时传播
	 */
	public void updateMessageDigest(MessageDigest messageDigest, int len) throws IOException {
		int data;
		int bytesRead = 0;
		while (bytesRead < len && (data = read()) != -1) {
			messageDigest.update((byte) data);
			bytesRead++;
		}
	}

}
