/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.http.converter;

import org.springframework.core.NestedRuntimeException;
import org.springframework.lang.Nullable;

/**
 * 当转换尝试失败时，由 {@link HttpMessageConverter} 实现抛出。
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 3.0
 */
@SuppressWarnings("serial")
public class HttpMessageConversionException extends NestedRuntimeException {

	/**
	 * 创建一个新的 HttpMessageConversionException。
	 *
	 * @param msg 详细信息
	 */
	public HttpMessageConversionException(String msg) {
		super(msg);
	}

	/**
	 * 创建一个新的 HttpMessageConversionException。
	 *
	 * @param msg   详细信息
	 * @param cause 根本原因（如果有）
	 */
	public HttpMessageConversionException(String msg, @Nullable Throwable cause) {
		super(msg, cause);
	}

}
