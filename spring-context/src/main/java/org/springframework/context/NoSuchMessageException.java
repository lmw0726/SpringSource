/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.context;

import java.util.Locale;

/**
 * 当消息无法被解析时抛出的异常。
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class NoSuchMessageException extends RuntimeException {

	/**
	 * 创建一个新的异常。
	 * @param code 无法为给定语言环境解析的代码
	 * @param locale 用于在其中搜索该代码的语言环境
	 */
	public NoSuchMessageException(String code, Locale locale) {
		super("No message found under code '" + code + "' for locale '" + locale + "'.");
	}

	/**
	 * 创建一个新的异常。
	 * @param code 无法为给定语言环境解析的代码
	 */
	public NoSuchMessageException(String code) {
		super("No message found under code '" + code + "' for locale '" + Locale.getDefault() + "'.");
	}

}

