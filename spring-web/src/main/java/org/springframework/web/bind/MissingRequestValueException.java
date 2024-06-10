/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.bind;

/**
 * {@link ServletRequestBindingException}的基类，用于处理因请求值是必需的但缺失或在转换后解析为{@code null}而无法绑定的情况。
 *
 * @author Rossen Stoyanchev
 * @since 5.3.6
 */
@SuppressWarnings("serial")
public class MissingRequestValueException extends ServletRequestBindingException {
	/**
	 * 请求值是否存在但转换为null
	 */
	private final boolean missingAfterConversion;

	/**
	 * 使用指定的消息创建一个新的MissingRequestValueException实例。
	 *
	 * @param msg 详细信息
	 */
	public MissingRequestValueException(String msg) {
		this(msg, false);
	}

	/**
	 * 使用指定的消息和转换后是否缺失的标志创建一个新的MissingRequestValueException实例。
	 *
	 * @param msg                    详细信息
	 * @param missingAfterConversion 值在转换后是否变为null
	 */
	public MissingRequestValueException(String msg, boolean missingAfterConversion) {
		super(msg);
		this.missingAfterConversion = missingAfterConversion;
	}


	/**
	 * 请求值是否存在但转换为{@code null}，例如通过{@code org.springframework.core.convert.support.IdToEntityConverter}。
	 *
	 * @return 如果值在转换后变为null，则返回true
	 */
	public boolean isMissingAfterConversion() {
		return this.missingAfterConversion;
	}

}
