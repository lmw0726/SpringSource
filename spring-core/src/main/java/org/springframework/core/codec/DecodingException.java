/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core.codec;

import org.springframework.lang.Nullable;

/**
 * 表示输入流解码时出现问题，重点在于内容相关的问题，例如解析失败。
 * 与更一般的 I/O 错误、非法状态，或 {@link CodecException}（例如解码器可能引发的配置问题）相反。
 *
 * <p>例如在服务器 Web 应用程序中，一个 {@code DecodingException} 将转化为一个 400（错误输入）状态的响应，
 * 而 {@code CodecException} 将转化为一个 500（服务器错误）状态的响应。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see Decoder
 */
@SuppressWarnings("serial")
public class DecodingException extends CodecException {

	/**
	 * 创建一个新的 DecodingException。
	 * @param msg 详细消息
	 */
	public DecodingException(String msg) {
		super(msg);
	}

	/**
	 * 创建一个新的 DecodingException。
	 * @param msg 详细消息
	 * @param cause 异常的根本原因（如果有）
	 */
	public DecodingException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
