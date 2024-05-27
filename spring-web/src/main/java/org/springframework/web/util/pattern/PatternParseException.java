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

package org.springframework.web.util.pattern;

import java.text.MessageFormat;

/**
 * 在解析模式时出现问题时抛出的异常。
 *
 * @author Andy Clement
 * @since 5.0
 */
@SuppressWarnings("serial")
public class PatternParseException extends IllegalArgumentException {

	/**
	 * 解析异常发生的位置。
	 */
	private final int position;

	/**
	 * 引发解析异常的模式。
	 */
	private final char[] pattern;

	/**
	 * 解析异常的消息类型。
	 */
	private final PatternMessage messageType;

	/**
	 * 用于格式化异常消息的插入项。
	 */
	private final Object[] inserts;


	PatternParseException(int pos, char[] pattern, PatternMessage messageType, Object... inserts) {
		super(messageType.formatMessage(inserts));
		this.position = pos;
		this.pattern = pattern;
		this.messageType = messageType;
		this.inserts = inserts;
	}

	PatternParseException(Throwable cause, int pos, char[] pattern, PatternMessage messageType, Object... inserts) {
		super(messageType.formatMessage(inserts), cause);
		this.position = pos;
		this.pattern = pattern;
		this.messageType = messageType;
		this.inserts = inserts;
	}


	/**
	 * 返回带有插入项应用的格式化消息。
	 */
	@Override
	public String getMessage() {
		return this.messageType.formatMessage(this.inserts);
	}

	/**
	 * 返回包含原始模式文本以及指向错误位置的指针的详细消息，
	 * 以及错误消息。
	 */
	public String toDetailedString() {
		// 创建一个 StringBuilder 对象
		StringBuilder sb = new StringBuilder();
		// 将模式添加到 StringBuilder 中，并添加换行符
		sb.append(this.pattern).append('\n');
		// 添加空格和 '^' 符号，指示匹配位置
		for (int i = 0; i < this.position; i++) {
			sb.append(' ');
		}
		sb.append("^\n");
		// 添加异常消息
		sb.append(getMessage());
		// 返回 StringBuilder 对象转换为字符串后的结果
		return sb.toString();
	}

	public int getPosition() {
		return this.position;
	}

	public PatternMessage getMessageType() {
		return this.messageType;
	}

	public Object[] getInserts() {
		return this.inserts;
	}


	/**
	 * 在解析失败时可以包含在{@link PatternParseException}中的消息。
	 */
	public enum PatternMessage {

		MISSING_CLOSE_CAPTURE("Expected close capture character after variable name '}'"),
		MISSING_OPEN_CAPTURE("Missing preceding open capture character before variable name'{'"),
		ILLEGAL_NESTED_CAPTURE("Not allowed to nest variable captures"),
		CANNOT_HAVE_ADJACENT_CAPTURES("Adjacent captures are not allowed"),
		ILLEGAL_CHARACTER_AT_START_OF_CAPTURE_DESCRIPTOR("Char ''{0}'' not allowed at start of captured variable name"),
		ILLEGAL_CHARACTER_IN_CAPTURE_DESCRIPTOR("Char ''{0}'' is not allowed in a captured variable name"),
		NO_MORE_DATA_EXPECTED_AFTER_CAPTURE_THE_REST("No more pattern data allowed after '{*...}' or '**' pattern element"),
		BADLY_FORMED_CAPTURE_THE_REST("Expected form when capturing the rest of the path is simply '{*...}'"),
		MISSING_REGEX_CONSTRAINT("Missing regex constraint on capture"),
		ILLEGAL_DOUBLE_CAPTURE("Not allowed to capture ''{0}'' twice in the same pattern"),
		REGEX_PATTERN_SYNTAX_EXCEPTION("Exception occurred in regex pattern compilation"),
		CAPTURE_ALL_IS_STANDALONE_CONSTRUCT("'{*...}' can only be preceded by a path separator");

		private final String message;

		PatternMessage(String message) {
			this.message = message;
		}

		public String formatMessage(Object... inserts) {
			return MessageFormat.format(this.message, inserts);
		}
	}

}
