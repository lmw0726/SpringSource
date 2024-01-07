/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.web.reactive.socket;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * WebSocket “close” 状态码和原因的表示。协议预定义了 1xxx 范围内的状态码。
 *
 * @author Rossen Stoyanchev
 * @see <a href="https://tools.ietf.org/html/rfc6455#section-7.4.1">
 *      RFC 6455, Section 7.4.1 "Defined Status Codes"</a>
 * @since 5.0
 */
public final class CloseStatus {

	/**
	 * "1000 表示正常关闭，意味着建立连接的目的已经完成。"
	 */
	public static final CloseStatus NORMAL = new CloseStatus(1000);

	/**
	 * "1001 表示一个端点正在“离开”，比如服务器关闭或者浏览器离开页面。"
	 */
	public static final CloseStatus GOING_AWAY = new CloseStatus(1001);

	/**
	 * "1002 表示一个端点因协议错误而终止连接。"
	 */
	public static final CloseStatus PROTOCOL_ERROR = new CloseStatus(1002);

	/**
	 * "1003 表示一个端点因接收到无法接受的数据类型而终止连接（例如，一个只接受文本数据的端点接收到二进制消息时可能会发送这个状态码）。"
	 */
	public static final CloseStatus NOT_ACCEPTABLE = new CloseStatus(1003);

	// 10004: 保留。
	// 具体含义可能在未来定义。

	/**
	 * "1005 是一个保留值，不得由端点在 Close 控制帧中设置为状态码。它用于期望状态码指示实际上未出现状态码的应用程序。"
	 */
	public static final CloseStatus NO_STATUS_CODE = new CloseStatus(1005);

	/**
	 * "1006 是一个保留值，不得由端点在 Close 控制帧中设置为状态码。它用于期望状态码指示连接异常关闭，例如，在未发送或接收 Close 控制帧的情况下。"
	 */
	public static final CloseStatus NO_CLOSE_FRAME = new CloseStatus(1006);

	/**
	 * "1007 表示一个端点因消息内容与消息类型不一致而终止连接（例如，在文本消息中包含非 UTF-8 [RFC3629] 数据）。"
	 */
	public static final CloseStatus BAD_DATA = new CloseStatus(1007);

	/**
	 * "1008 表示一个端点因接收到违反其策略的消息而终止连接。这是一个通用状态码，当没有其他更合适的状态码（例如，1003 或 1009）或者需要隐藏策略的具体细节时可以返回。"
	 */
	public static final CloseStatus POLICY_VIOLATION = new CloseStatus(1008);

	/**
	 * "1009 表示一个端点因接收到过大无法处理的消息而终止连接。"
	 */
	public static final CloseStatus TOO_BIG_TO_PROCESS = new CloseStatus(1009);

	/**
	 * "1010 表示客户端因期望服务器协商一个或多个扩展，但服务器在 WebSocket 握手的响应消息中未返回这些扩展。所需扩展的列表应出现在 Close 帧的 /reason/ 部分。注意，服务器不使用此状态码，因为它可以使 WebSocket 握手失败。"
	 */
	public static final CloseStatus REQUIRED_EXTENSION = new CloseStatus(1010);

	/**
	 * "1011 表示服务器因遇到意外条件而无法完成请求而终止连接。"
	 */
	public static final CloseStatus SERVER_ERROR = new CloseStatus(1011);

	/**
	 * "1012 表示服务已重新启动。客户端可以重新连接，如果选择这样做，应该使用 5 - 30 秒的随机延迟重新连接。"
	 */
	public static final CloseStatus SERVICE_RESTARTED = new CloseStatus(1012);

	/**
	 * "1013 表示服务正在经历过载。客户端只应在用户操作时连接到不同的 IP（当目标有多个 IP 时）或重新连接到相同的 IP。"
	 */
	public static final CloseStatus SERVICE_OVERLOAD = new CloseStatus(1013);

	/**
	 * "1015 是一个保留值，不得由端点在 Close 控制帧中设置为状态码。它用于期望状态码指示连接由于 TLS 握手失败（例如，服务器证书无法验证）而关闭。"
	 */
	public static final CloseStatus TLS_HANDSHAKE_FAILURE = new CloseStatus(1015);


	private final int code;

	@Nullable
	private final String reason;


	/**
	 * 创建一个新的{@link CloseStatus}实例。
	 *
	 * @param code 状态码
	 */
	public CloseStatus(int code) {
		this(code, null);
	}

	/**
	 * 创建一个新的{@link CloseStatus}实例。
	 *
	 * @param code   状态码
	 * @param reason 原因
	 */
	public CloseStatus(int code, @Nullable String reason) {
		Assert.isTrue((code >= 1000 && code < 5000), "Invalid status code");
		this.code = code;
		this.reason = reason;
	}


	/**
	 * 返回状态码。
	 */
	public int getCode() {
		return this.code;
	}

	/**
	 * 返回原因，如果没有则返回{@code null}。
	 */
	@Nullable
	public String getReason() {
		return this.reason;
	}

	/**
	 * 使用指定的原因从当前{@link CloseStatus}创建一个新的{@link CloseStatus}实例。
	 *
	 * @param reason 原因
	 * @return 新的{@link CloseStatus}实例
	 */
	public CloseStatus withReason(String reason) {
		Assert.hasText(reason, "Reason must not be empty");
		return new CloseStatus(this.code, reason);
	}

	/**
	 * @deprecated 从 5.3 开始已弃用，建议直接比较状态码
	 */
	@Deprecated
	public boolean equalsCode(CloseStatus other) {
		return (this.code == other.code);
	}


	/**
	 * 根据给定的代码返回一个常量，如果代码不匹配或有原因，则创建一个新实例。
	 *
	 * @since 5.3
	 */
	public static CloseStatus create(int code, @Nullable String reason) {
		if (!StringUtils.hasText(reason)) {
			switch (code) {
				case 1000:
					return NORMAL;
				case 1001:
					return GOING_AWAY;
				case 1002:
					return PROTOCOL_ERROR;
				case 1003:
					return NOT_ACCEPTABLE;
				case 1005:
					return NO_STATUS_CODE;
				case 1006:
					return NO_CLOSE_FRAME;
				case 1007:
					return BAD_DATA;
				case 1008:
					return POLICY_VIOLATION;
				case 1009:
					return TOO_BIG_TO_PROCESS;
				case 1010:
					return REQUIRED_EXTENSION;
				case 1011:
					return SERVER_ERROR;
				case 1012:
					return SERVICE_RESTARTED;
				case 1013:
					return SERVICE_OVERLOAD;
			}
		}
		return new CloseStatus(code, reason);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof CloseStatus)) {
			return false;
		}
		CloseStatus otherStatus = (CloseStatus) other;
		return (this.code == otherStatus.code &&
				ObjectUtils.nullSafeEquals(this.reason, otherStatus.reason));
	}

	@Override
	public int hashCode() {
		return this.code * 29 + ObjectUtils.nullSafeHashCode(this.reason);
	}

	@Override
	public String toString() {
		return "CloseStatus[code=" + this.code + ", reason=" + this.reason + "]";
	}

}
