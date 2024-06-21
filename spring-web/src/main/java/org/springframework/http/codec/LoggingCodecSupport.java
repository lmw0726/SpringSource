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

package org.springframework.http.codec;

import org.apache.commons.logging.Log;

import org.springframework.http.HttpLogging;

/**
 * 用于 {@link org.springframework.core.codec.Encoder}、
 * {@link org.springframework.core.codec.Decoder}、{@link HttpMessageReader} 或
 * {@link HttpMessageWriter} 的基础类，使用日志记录器并显示可能包含敏感请求数据。
 *
 * @author Rossen Stoyanchev
 * @since 5.1
 */
public class LoggingCodecSupport {
	/**
	 * 日志记录器
	 */
	protected final Log logger = HttpLogging.forLogName(getClass());

	/**
	 * 是否记录可能包含敏感信息的日志（在 DEBUG 级别记录表单数据，在 TRACE 级别记录头部信息）。
	 */
	private boolean enableLoggingRequestDetails = false;


	/**
	 * 设置是否记录表单数据（DEBUG 级别）和头部信息（TRACE 级别）的日志。
	 * 这两者可能包含敏感信息。
	 * <p>默认设置为 {@code false}，因此不显示请求详情。
	 *
	 * @param enable 是否启用
	 */
	public void setEnableLoggingRequestDetails(boolean enable) {
		this.enableLoggingRequestDetails = enable;
	}

	/**
	 * 返回是否显式禁用任何编码或解码值的日志记录，无论日志级别如何。
	 */
	public boolean isEnableLoggingRequestDetails() {
		return this.enableLoggingRequestDetails;
	}

}
