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

package org.springframework.beans.factory.parsing;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 表示bean定义配置的问题。
 * 主要用作传递给{@link ProblemReporter}的通用参数。
 *
 * <p>可能表示潜在的致命问题（错误）或仅仅是警告。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see ProblemReporter
 */
public class Problem {

	private final String message;

	private final Location location;

	@Nullable
	private final ParseState parseState;

	@Nullable
	private final Throwable rootCause;


	/**
	 * 创建{@link Problem}类的新实例。
	 * @param message 详细描述问题的消息
	 * @param location bean配置源中触发错误的位置
	 */
	public Problem(String message, Location location) {
		this(message, location, null, null);
	}

	/**
	 * 创建{@link Problem}类的新实例。
	 * @param message 详细描述问题的消息
	 * @param parseState 错误发生时的{@link ParseState}
	 * @param location bean配置源中触发错误的位置
	 */
	public Problem(String message, Location location, ParseState parseState) {
		this(message, location, parseState, null);
	}

	/**
	 * 创建{@link Problem}类的新实例。
	 * @param message 详细描述问题的消息
	 * @param rootCause 导致错误的底层异常（可能为{@code null}）
	 * @param parseState 错误发生时的{@link ParseState}
	 * @param location bean配置源中触发错误的位置
	 */
	public Problem(String message, Location location, @Nullable ParseState parseState, @Nullable Throwable rootCause) {
		Assert.notNull(message, "Message must not be null");
		Assert.notNull(location, "Location must not be null");
		this.message = message;
		this.location = location;
		this.parseState = parseState;
		this.rootCause = rootCause;
	}


	/**
	 * 获取详细描述问题的消息。
	 */
	public String getMessage() {
		return this.message;
	}

	/**
	 * 获取bean配置源中触发错误的位置。
	 */
	public Location getLocation() {
		return this.location;
	}

	/**
	 * 获取触发错误的bean配置源的描述，
	 * 该描述包含在此Problem的Location对象中。
	 * @see #getLocation()
	 */
	public String getResourceDescription() {
		return getLocation().getResource().getDescription();
	}

	/**
	 * 获取错误发生时的{@link ParseState}（可能为{@code null}）。
	 */
	@Nullable
	public ParseState getParseState() {
		return this.parseState;
	}

	/**
	 * 获取导致错误的底层异常（可能为{@code null}）。
	 */
	@Nullable
	public Throwable getRootCause() {
		return this.rootCause;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Configuration problem: ");
		sb.append(getMessage());
		sb.append("\nOffending resource: ").append(getResourceDescription());
		if (getParseState() != null) {
			sb.append('\n').append(getParseState());
		}
		return sb.toString();
	}

}
