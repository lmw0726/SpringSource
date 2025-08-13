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

import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

/**
 * 表示在对象流进行编码和解码过程中出现的问题的通用错误。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@SuppressWarnings("serial")
public class CodecException extends NestedRuntimeException {

	/**
	 * 创建一个新的 CodecException。
	 * @param msg 详细消息
	 */
	public CodecException(String msg) {
		super(msg);
	}

	/**
	 * 创建一个新的 CodecException。
	 * @param msg 详细消息
	 * @param cause 异常的根本原因（如果有）
	 */
	public CodecException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
