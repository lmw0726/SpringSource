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

package org.springframework.http.codec.protobuf;

import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 提供 Protobuf 编码和解码的支持方法的基础类。
 *
 * @author Sebastien Deleuze
 * @since 5.1
 */
public abstract class ProtobufCodecSupport {

	/**
	 * 支持的 MIME 类型列表，不可修改。
	 */
	static final List<MimeType> MIME_TYPES = Collections.unmodifiableList(
			Arrays.asList(
					new MimeType("application", "x-protobuf"),
					new MimeType("application", "octet-stream"),
					new MimeType("application", "vnd.google.protobuf")));

	/**
	 * MIME类型中的分隔键。
	 */
	static final String DELIMITED_KEY = "delimited";

	/**
	 * 分隔键的值。
	 */
	static final String DELIMITED_VALUE = "true";


	/**
	 * 检查是否支持指定的 MIME 类型。
	 *
	 * @param mimeType 要检查的 MIME 类型
	 * @return 如果 MIME 类型为空或与支持的 MIME 类型之一兼容，则返回 true；否则返回 false
	 */
	protected boolean supportsMimeType(@Nullable MimeType mimeType) {
		return (mimeType == null || MIME_TYPES.stream().anyMatch(m -> m.isCompatibleWith(mimeType)));
	}

	/**
	 * 返回支持的 MIME 类型列表。
	 *
	 * @return 支持的 MIME 类型列表
	 */
	protected List<MimeType> getMimeTypes() {
		return MIME_TYPES;
	}

}
