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
 * 表示输入对象流编码时出现问题，重点是无法编码对象。
 * 与更一般的 I/O 错误或 {@link CodecException}（例如编码器也可能引发的配置问题）相反。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see Encoder
 */
@SuppressWarnings("serial")
public class EncodingException extends CodecException {

	/**
	 * 创建一个新的 EncodingException。
	 * @param msg 详细消息
	 */
	public EncodingException(String msg) {
		super(msg);
	}

	/**
	 * 创建一个新的 EncodingException。
	 * @param msg 详细消息
	 * @param cause 异常的根本原因（如果有）
	 */
	public EncodingException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
