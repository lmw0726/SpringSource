/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.core.convert;

import org.springframework.core.NestedRuntimeException;

/**
 * 转换系统抛出的异常的基类。
 *
 * @author Keith Donald
 * @since 3.0
 */
@SuppressWarnings("serial")
public abstract class ConversionException extends NestedRuntimeException {

	/**
	 * 构造新的转换异常。
	 * @param message 异常消息
	 */
	public ConversionException(String message) {
		super(message);
	}

	/**
	 * 构造新的转换异常。
	 * @param message 异常消息
	 * @param cause 原因
	 */
	public ConversionException(String message, Throwable cause) {
		super(message, cause);
	}

}
