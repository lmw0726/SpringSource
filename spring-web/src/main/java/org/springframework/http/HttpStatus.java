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

package org.springframework.http;

import org.springframework.lang.Nullable;

/**
 * HTTP状态码的枚举。
 *
 * <p>可以通过 {@link #series()} 方法获取HTTP状态码系列。
 *
 * <p>此枚举定义了HTTP协议中常见的状态码及其含义，参考自
 * <a href="https://www.iana.org/assignments/http-status-codes">HTTP Status Code Registry</a>
 * 和 <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">List of HTTP status codes - Wikipedia</a>。
 *
 * @see HttpStatus.Series
 * @see <a href="https://www.iana.org/assignments/http-status-codes">HTTP状态代码注册表</a>
 * @see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">HTTP状态代码列表-维基百科</a>
 * @since 3.0
 */
public enum HttpStatus {

	// 1xx 信息性

	/**
	 * {@code 100 继续}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.2.1">HTTP/1.1: 语义和内容, 第 6.2.1 节</a>
	 */
	CONTINUE(100, Series.INFORMATIONAL, "Continue"),

	/**
	 * {@code 101 转换协议}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.2.2">HTTP/1.1: 语义和内容, 第 6.2.2 节</a>
	 */
	SWITCHING_PROTOCOLS(101, Series.INFORMATIONAL, "Switching Protocols"),

	/**
	 * {@code 102 处理中}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc2518#section-10.1">WebDAV</a>
	 */
	PROCESSING(102, Series.INFORMATIONAL, "Processing"),

	/**
	 * {@code 103 检查点}.
	 *
	 * @see <a href="https://code.google.com/p/gears/wiki/ResumableHttpRequestsProposal">HTTP/1.0 中支持可恢复的 POST/PUT HTTP 请求的提案</a>
	 */
	CHECKPOINT(103, Series.INFORMATIONAL, "Checkpoint"),

	// 2xx 成功

	/**
	 * {@code 200 OK}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.1">HTTP/1.1: 语义和内容, 第 6.3.1 节</a>
	 */
	OK(200, Series.SUCCESSFUL, "OK"),

	/**
	 * {@code 201 已创建}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.2">HTTP/1.1: 语义和内容, 第 6.3.2 节</a>
	 */
	CREATED(201, Series.SUCCESSFUL, "Created"),

	/**
	 * {@code 202 已接受}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.3">HTTP/1.1: 语义和内容, 第 6.3.3 节</a>
	 */
	ACCEPTED(202, Series.SUCCESSFUL, "Accepted"),

	/**
	 * {@code 203 非权威信息}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.4">HTTP/1.1: 语义和内容, 第 6.3.4 节</a>
	 */
	NON_AUTHORITATIVE_INFORMATION(203, Series.SUCCESSFUL, "Non-Authoritative Information"),

	/**
	 * {@code 204 无内容}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.5">HTTP/1.1: 语义和内容, 第 6.3.5 节</a>
	 */
	NO_CONTENT(204, Series.SUCCESSFUL, "No Content"),

	/**
	 * {@code 205 重置内容}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.6">HTTP/1.1: 语义和内容, 第 6.3.6 节</a>
	 */
	RESET_CONTENT(205, Series.SUCCESSFUL, "Reset Content"),

	/**
	 * {@code 206 部分内容}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-4.1">HTTP/1.1: 范围请求, 第 4.1 节</a>
	 */
	PARTIAL_CONTENT(206, Series.SUCCESSFUL, "Partial Content"),

	/**
	 * {@code 207 多状态}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc4918#section-13">WebDAV</a>
	 */
	MULTI_STATUS(207, Series.SUCCESSFUL, "Multi-Status"),

	/**
	 * {@code 208 已报告}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc5842#section-7.1">WebDAV 绑定扩展</a>
	 */
	ALREADY_REPORTED(208, Series.SUCCESSFUL, "Already Reported"),

	/**
	 * {@code 226 IM已使用}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc3229#section-10.4.1">HTTP 中的 Delta 编码</a>
	 */
	IM_USED(226, Series.SUCCESSFUL, "IM Used"),

	// 3xx 重定向

	/**
	 * {@code 300 多项选择}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.1">HTTP/1.1: 语义和内容, 第 6.4.1 节</a>
	 */
	MULTIPLE_CHOICES(300, Series.REDIRECTION, "Multiple Choices"),

	/**
	 * {@code 301 永久移动}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.2">HTTP/1.1: 语义和内容, 第 6.4.2 节</a>
	 */
	MOVED_PERMANENTLY(301, Series.REDIRECTION, "Moved Permanently"),

	/**
	 * {@code 302 已找到}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.3">HTTP/1.1: 语义和内容, 第 6.4.3 节</a>
	 */
	FOUND(302, Series.REDIRECTION, "Found"),

	/**
	 * {@code 302 暂时移动}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc1945#section-9.3">HTTP/1.0, 第 9.3 节</a>
	 * @deprecated 推荐使用 {@link #FOUND}，将从 {@code HttpStatus.valueOf(302)} 返回
	 */
	@Deprecated
	MOVED_TEMPORARILY(302, Series.REDIRECTION, "Moved Temporarily"),

	/**
	 * {@code 303 查看其他}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.4">HTTP/1.1: 语义和内容, 第 6.4.4 节</a>
	 */
	SEE_OTHER(303, Series.REDIRECTION, "See Other"),

	/**
	 * {@code 304 未修改}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-4.1">HTTP/1.1: 条件请求, 第 4.1 节</a>
	 */
	NOT_MODIFIED(304, Series.REDIRECTION, "Not Modified"),

	/**
	 * {@code 305 使用代理}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.5">HTTP/1.1: 语义和内容, 第 6.4.5 节</a>
	 * @deprecated 由于涉及代理的配置安全问题，已不推荐使用
	 */
	@Deprecated
	USE_PROXY(305, Series.REDIRECTION, "Use Proxy"),

	/**
	 * {@code 307 临时重定向}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.7">HTTP/1.1: 语义和内容, 第 6.4.7 节</a>
	 */
	TEMPORARY_REDIRECT(307, Series.REDIRECTION, "Temporary Redirect"),

	/**
	 * {@code 308 永久重定向}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7238">RFC 7238</a>
	 */
	PERMANENT_REDIRECT(308, Series.REDIRECTION, "Permanent Redirect"),

	// --- 4xx 客户端错误 ---

	/**
	 * {@code 400 请求错误}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.1">HTTP/1.1: 语义和内容，第6.5.1节</a>
	 */
	BAD_REQUEST(400, Series.CLIENT_ERROR, "Bad Request"),

	/**
	 * {@code 401 未授权}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-3.1">HTTP/1.1: 身份验证，第3.1节</a>
	 */
	UNAUTHORIZED(401, Series.CLIENT_ERROR, "Unauthorized"),

	/**
	 * {@code 402 需要付款}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.2">HTTP/1.1: 语义和内容，第6.5.2节</a>
	 */
	PAYMENT_REQUIRED(402, Series.CLIENT_ERROR, "Payment Required"),

	/**
	 * {@code 403 禁止访问}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.3">HTTP/1.1: 语义和内容，第6.5.3节</a>
	 */
	FORBIDDEN(403, Series.CLIENT_ERROR, "Forbidden"),

	/**
	 * {@code 404 未找到资源}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.4">HTTP/1.1: 语义和内容，第6.5.4节</a>
	 */
	NOT_FOUND(404, Series.CLIENT_ERROR, "Not Found"),

	/**
	 * {@code 405 不允许使用该方法}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.5">HTTP/1.1: 语义和内容，第6.5.5节</a>
	 */
	METHOD_NOT_ALLOWED(405, Series.CLIENT_ERROR, "Method Not Allowed"),

	/**
	 * {@code 406 不可接受的请求}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.6">HTTP/1.1: 语义和内容，第6.5.6节</a>
	 */
	NOT_ACCEPTABLE(406, Series.CLIENT_ERROR, "Not Acceptable"),

	/**
	 * {@code 407 需要代理授权}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7235#section-3.2">HTTP/1.1: 身份验证，第3.2节</a>
	 */
	PROXY_AUTHENTICATION_REQUIRED(407, Series.CLIENT_ERROR, "Proxy Authentication Required"),

	/**
	 * {@code 408 请求超时}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.7">HTTP/1.1: 语义和内容，第6.5.7节</a>
	 */
	REQUEST_TIMEOUT(408, Series.CLIENT_ERROR, "Request Timeout"),

	/**
	 * {@code 409 请求冲突}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.8">HTTP/1.1: 语义和内容，第6.5.8节</a>
	 */
	CONFLICT(409, Series.CLIENT_ERROR, "Conflict"),

	/**
	 * {@code 410 请求的资源不可用}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.9">HTTP/1.1: 语义和内容，第6.5.9节</a>
	 */
	GONE(410, Series.CLIENT_ERROR, "Gone"),

	/**
	 * {@code 411 缺少请求的内容长度信息}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.10">HTTP/1.1: 语义和内容，第6.5.10节</a>
	 */
	LENGTH_REQUIRED(411, Series.CLIENT_ERROR, "Length Required"),

	/**
	 * {@code 412 请求头中指定的一些前提条件失败}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7232#section-4.2">HTTP/1.1: 条件请求，第4.2节</a>
	 */
	PRECONDITION_FAILED(412, Series.CLIENT_ERROR, "Precondition Failed"),

	/**
	 * {@code 413 请求的实体过大}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.11">HTTP/1.1: 语义和内容，第6.5.11节</a>
	 * @since 4.1
	 */
	PAYLOAD_TOO_LARGE(413, Series.CLIENT_ERROR, "Payload Too Large"),

	/**
	 * {@code 414 请求的URI过长}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.12">HTTP/1.1: 语义和内容，第6.5.12节</a>
	 * @since 4.1
	 */
	URI_TOO_LONG(414, Series.CLIENT_ERROR, "URI Too Long"),

	/**
	 * {@code 415 不支持的媒体类型}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.13">HTTP/1.1: 语义和内容，第6.5.13节</a>
	 */
	UNSUPPORTED_MEDIA_TYPE(415, Series.CLIENT_ERROR, "Unsupported Media Type"),

	/**
	 * {@code 416 请求范围不符合要求}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7233#section-4.4">HTTP/1.1: 范围请求，第4.4节</a>
	 */
	REQUESTED_RANGE_NOT_SATISFIABLE(416, Series.CLIENT_ERROR, "Requested range not satisfiable"),

	/**
	 * {@code 417 请求头中的期望未达成}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.14">HTTP/1.1: 语义和内容，第6.5.14节 </a>
	 */
	EXPECTATION_FAILED(417, Series.CLIENT_ERROR, "Expectation Failed"),

	/**
	 * {@code 418 我是一个茶壶}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc2324#section-2.3.2">HTCPCP/1.0</a>
	 */
	I_AM_A_TEAPOT(418, Series.CLIENT_ERROR, "I'm a teapot"),

	/**
	 * @deprecated See
	 * <a href="https://tools.ietf.org/rfcdiff?difftype=--hwdiff&amp;url2=draft-ietf-webdav-protocol-06.txt">
	 * WebDAV 更改草稿</a>
	 */
	@Deprecated
	INSUFFICIENT_SPACE_ON_RESOURCE(419, Series.CLIENT_ERROR, "Insufficient Space On Resource"),

	/**
	 * @deprecated See
	 * <a href="https://tools.ietf.org/rfcdiff?difftype=--hwdiff&amp;url2=draft-ietf-webdav-protocol-06.txt">
	 * WebDAV 更改草稿</a>
	 */
	@Deprecated
	METHOD_FAILURE(420, Series.CLIENT_ERROR, "Method Failure"),

	/**
	 * @deprecated See <a href="https://tools.ietf.org/rfcdiff?difftype=--hwdiff&amp;url2=draft-ietf-webdav-protocol-06.txt">
	 * WebDAV 更改草稿</a>
	 */
	@Deprecated
	DESTINATION_LOCKED(421, Series.CLIENT_ERROR, "Destination Locked"),

	/**
	 * {@code 422 Unprocessable Entity}.
	 * <p>请求格式正确，但是由于含有语义错误，无法响应。</p>
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.2">WebDAV</a>
	 */
	UNPROCESSABLE_ENTITY(422, Series.CLIENT_ERROR, "Unprocessable Entity"),

	/**
	 * {@code 423 资源被锁定}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.3">WebDAV</a>
	 */
	LOCKED(423, Series.CLIENT_ERROR, "Locked"),

	/**
	 * {@code 424 操作失败的依赖关系}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.4">WebDAV</a>
	 */
	FAILED_DEPENDENCY(424, Series.CLIENT_ERROR, "Failed Dependency"),

	/**
	 * {@code 425 请求太早}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc8470">RFC 8470</a>
	 * @since 5.2
	 */
	TOO_EARLY(425, Series.CLIENT_ERROR, "Too Early"),

	/**
	 * {@code 426 需要升级协议}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc2817#section-6">在HTTP/1.1中升级到TLS</a>
	 */
	UPGRADE_REQUIRED(426, Series.CLIENT_ERROR, "Upgrade Required"),

	/**
	 * {@code 428 要求先决条件}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc6585#section-3">其他HTTP状态代码</a>
	 */
	PRECONDITION_REQUIRED(428, Series.CLIENT_ERROR, "Precondition Required"),

	/**
	 * {@code 429 请求过多}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc6585#section-4">其他HTTP状态代码</a>
	 */
	TOO_MANY_REQUESTS(429, Series.CLIENT_ERROR, "Too Many Requests"),

	/**
	 * {@code 431 请求头字段过大}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc6585#section-5">其他HTTP状态代码</a>
	 */
	REQUEST_HEADER_FIELDS_TOO_LARGE(431, Series.CLIENT_ERROR, "Request Header Fields Too Large"),

	/**
	 * {@code 451 因法律原因不可用}.
	 *
	 * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-legally-restricted-status-04">
	 * 报告法律障碍的HTTP状态代码</a>
	 * @since 4.3
	 */
	UNAVAILABLE_FOR_LEGAL_REASONS(451, Series.CLIENT_ERROR, "Unavailable For Legal Reasons"),

	// --- 5xx 服务器错误 ---

	/**
	 * {@code 500 服务器内部错误}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.1">HTTP/1.1: 语义和内容，第6.6.1节</a>
	 */
	INTERNAL_SERVER_ERROR(500, Series.SERVER_ERROR, "Internal Server Error"),

	/**
	 * {@code 501 未实现}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.2">HTTP/1.1: 语义和内容，第6.6.2节</a>
	 */
	NOT_IMPLEMENTED(501, Series.SERVER_ERROR, "Not Implemented"),

	/**
	 * {@code 502 网关错误}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.3">HTTP/1.1: 语义和内容，第6.6.3节</a>
	 */
	BAD_GATEWAY(502, Series.SERVER_ERROR, "Bad Gateway"),

	/**
	 * {@code 503 服务不可用}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.4">HTTP/1.1: 语义和内容，第6.6.4节</a>
	 */
	SERVICE_UNAVAILABLE(503, Series.SERVER_ERROR, "Service Unavailable"),

	/**
	 * {@code 504 网关超时}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.5">HTTP/1.1: 语义和内容，第6.6.5节</a>
	 */
	GATEWAY_TIMEOUT(504, Series.SERVER_ERROR, "Gateway Timeout"),

	/**
	 * {@code 505 HTTP版本不支持}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.6">HTTP/1.1: 语义和内容，第6.6.6节</a>
	 */
	HTTP_VERSION_NOT_SUPPORTED(505, Series.SERVER_ERROR, "HTTP Version not supported"),

	/**
	 * {@code 506 协商内容变体}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc2295#section-8.1">透明的内容协商 </a>
	 */
	VARIANT_ALSO_NEGOTIATES(506, Series.SERVER_ERROR, "Variant Also Negotiates"),

	/**
	 * {@code 507 存储空间不足}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.5">WebDAV</a>
	 */
	INSUFFICIENT_STORAGE(507, Series.SERVER_ERROR, "Insufficient Storage"),

	/**
	 * {@code 508 检测到循环}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc5842#section-7.2">WebDAV 绑定扩展</a>
	 */
	LOOP_DETECTED(508, Series.SERVER_ERROR, "Loop Detected"),

	/**
	 * {@code 509 超出带宽限制}.
	 */
	BANDWIDTH_LIMIT_EXCEEDED(509, Series.SERVER_ERROR, "Bandwidth Limit Exceeded"),

	/**
	 * {@code 510 未扩展}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc2774#section-7">HTTP扩展框架</a>
	 */
	NOT_EXTENDED(510, Series.SERVER_ERROR, "Not Extended"),
	/**
	 * {@code 511 要求网络认证。}.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc6585#section-6">其他HTTP状态代码</a>
	 */
	NETWORK_AUTHENTICATION_REQUIRED(511, Series.SERVER_ERROR, "Network Authentication Required");

	/**
	 * Http状态码数组
	 */
	private static final HttpStatus[] VALUES;

	static {
		VALUES = values();
	}

	/**
	 * 状态值
	 */
	private final int value;

	/**
	 * 状态系列
	 */
	private final Series series;

	/**
	 * 理由短语
	 */
	private final String reasonPhrase;

	HttpStatus(int value, Series series, String reasonPhrase) {
		this.value = value;
		this.series = series;
		this.reasonPhrase = reasonPhrase;
	}


	/**
	 * 返回此状态码的整数值。
	 *
	 * @return 整数值
	 */
	public int value() {
		return this.value;
	}

	/**
	 * 返回此状态码的HTTP状态系列。
	 *
	 * @return HTTP状态系列
	 * @see HttpStatus.Series
	 */
	public Series series() {
		return this.series;
	}

	/**
	 * 返回此状态码的原因短语。
	 *
	 * @return 原因短语
	 */
	public String getReasonPhrase() {
		return this.reasonPhrase;
	}

	/**
	 * 检查此状态码是否属于HTTP系列 {@link org.springframework.http.HttpStatus.Series#INFORMATIONAL}。
	 * <p>这是检查 {@link #series()} 值的快捷方式。</p>
	 *
	 * @return 如果状态码属于 {@link org.springframework.http.HttpStatus.Series#INFORMATIONAL} 返回 {@code true}
	 * @see #series()
	 * @since 4.0
	 */
	public boolean is1xxInformational() {
		return (series() == Series.INFORMATIONAL);
	}

	/**
	 * 检查此状态码是否属于HTTP系列 {@link org.springframework.http.HttpStatus.Series#SUCCESSFUL}。
	 * <p>这是检查 {@link #series()} 值的快捷方式。</p>
	 *
	 * @return 如果状态码属于 {@link org.springframework.http.HttpStatus.Series#SUCCESSFUL} 返回 {@code true}
	 * @see #series()
	 * @since 4.0
	 */
	public boolean is2xxSuccessful() {
		return (series() == Series.SUCCESSFUL);
	}

	/**
	 * 检查此状态码是否属于HTTP系列 {@link org.springframework.http.HttpStatus.Series#REDIRECTION}。
	 * <p>这是检查 {@link #series()} 值的快捷方式。</p>
	 *
	 * @return 如果状态码属于 {@link org.springframework.http.HttpStatus.Series#REDIRECTION} 返回 {@code true}
	 * @see #series()
	 * @since 4.0
	 */
	public boolean is3xxRedirection() {
		return (series() == Series.REDIRECTION);
	}

	/**
	 * 检查此状态码是否属于HTTP系列 {@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR}。
	 * <p>这是检查 {@link #series()} 值的快捷方式。</p>
	 *
	 * @return 如果状态码属于 {@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR} 返回 {@code true}
	 * @see #series()
	 * @since 4.0
	 */
	public boolean is4xxClientError() {
		return (series() == Series.CLIENT_ERROR);
	}

	/**
	 * 检查此状态码是否属于HTTP系列 {@link org.springframework.http.HttpStatus.Series#SERVER_ERROR}。
	 * <p>这是检查 {@link #series()} 值的快捷方式。</p>
	 *
	 * @return 如果状态码属于 {@link org.springframework.http.HttpStatus.Series#SERVER_ERROR} 返回 {@code true}
	 * @see #series()
	 * @since 4.0
	 */
	public boolean is5xxServerError() {
		return (series() == Series.SERVER_ERROR);
	}

	/**
	 * 检查此状态码是否属于HTTP系列 {@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR} 或 {@link org.springframework.http.HttpStatus.Series#SERVER_ERROR}。
	 * <p>这是检查 {@link #series()} 值的快捷方式。</p>
	 *
	 * @return 如果状态码属于 {@link org.springframework.http.HttpStatus.Series#CLIENT_ERROR} 或 {@link org.springframework.http.HttpStatus.Series#SERVER_ERROR} 返回 {@code true}
	 * @see #is4xxClientError()
	 * @see #is5xxServerError()
	 * @since 5.0
	 */
	public boolean isError() {
		return (is4xxClientError() || is5xxServerError());
	}

	/**
	 * 返回此状态码的字符串表示形式。
	 *
	 * @return 字符串表示形式
	 */
	@Override
	public String toString() {
		return this.value + " " + name();
	}

	/**
	 * 根据指定的数值返回对应的 {@code HttpStatus} 枚举常量。
	 *
	 * @param statusCode 要返回的枚举常量的数值
	 * @return 指定数值对应的枚举常量
	 * @throws IllegalArgumentException 如果枚举中没有指定数值对应的常量
	 */
	public static HttpStatus valueOf(int statusCode) {
		// 根据状态码解析出对应的 Http状态码 枚举值
		HttpStatus status = resolve(statusCode);

		// 如果解析出的枚举值为 null
		if (status == null) {
			// 则抛出异常，表示没有匹配的常量
			throw new IllegalArgumentException("No matching constant for [" + statusCode + "]");
		}

		// 返回解析出的 Http状态码 枚举值
		return status;
	}

	/**
	 * 将给定的状态码解析为对应的 {@code HttpStatus} 枚举常量。
	 *
	 * @param statusCode HTTP状态码（可能非标准）
	 * @return 对应的 {@code HttpStatus}，如果未找到则返回 {@code null}
	 * @since 5.0
	 */
	@Nullable
	public static HttpStatus resolve(int statusCode) {
		// 使用缓存的 VALUES 而不是 values() 来避免数组分配。
		// 遍历 Http状态码 枚举类型中定义的所有常量
		for (HttpStatus status : VALUES) {
			// 如果枚举常量的值与给定的 状态码值 相等，则返回该枚举常量
			if (status.value == statusCode) {
				return status;
			}
		}

		// 如果未找到与 状态码值 匹配的枚举常量，则返回 null
		return null;
	}


	/**
	 * HTTP状态系列的枚举。
	 * <p>可通过 {@link HttpStatus#series()} 获取。</p>
	 */
	public enum Series {

		/**
		 * 信息性状态码系列，对应HTTP状态码以1开头的情况。
		 */
		INFORMATIONAL(1),

		/**
		 * 成功状态码系列，对应HTTP状态码以2开头的情况。
		 */
		SUCCESSFUL(2),

		/**
		 * 重定向状态码系列，对应HTTP状态码以3开头的情况。
		 */
		REDIRECTION(3),

		/**
		 * 客户端错误状态码系列，对应HTTP状态码以4开头的情况。
		 */
		CLIENT_ERROR(4),

		/**
		 * 服务器错误状态码系列，对应HTTP状态码以5开头的情况。
		 */
		SERVER_ERROR(5);

		private final int value;

		Series(int value) {
			this.value = value;
		}

		/**
		 * 返回此状态系列的整数值。取值范围从1到5。
		 *
		 * @return 整数值
		 */
		public int value() {
			return this.value;
		}

		/**
		 * 根据给定的 {@code HttpStatus} 返回对应的 {@code Series} 枚举常量。
		 *
		 * @param status 标准的HTTP状态枚举常量
		 * @return 给定 {@code HttpStatus} 对应的 {@code Series} 枚举常量
		 * @deprecated 自5.3起，建议直接调用 {@link HttpStatus#series()}
		 */
		@Deprecated
		public static Series valueOf(HttpStatus status) {
			return status.series();
		}

		/**
		 * 根据给定的状态码返回对应的 {@code Series} 枚举常量。
		 *
		 * @param statusCode HTTP状态码（可能非标准）
		 * @return 给定状态码对应的 {@code Series} 枚举常量
		 * @throws IllegalArgumentException 如果枚举中没有对应的常量
		 */
		public static Series valueOf(int statusCode) {
			// 根据状态码解析出对应的 Series 枚举值
			Series series = resolve(statusCode);

			// 如果解析出的枚举值为 null
			if (series == null) {
				// 则抛出异常，表示没有匹配的常量
				throw new IllegalArgumentException("No matching constant for [" + statusCode + "]");
			}

			// 返回解析出的 Series 枚举值
			return series;
		}

		/**
		 * 将给定的状态码解析为 {@code HttpStatus.Series}。
		 *
		 * @param statusCode HTTP状态码（可能非标准）
		 * @return 相应的 {@code Series}，如果未找到则返回 {@code null}
		 * @since 5.1.3
		 */
		@Nullable
		public static Series resolve(int statusCode) {
			// 计算状态码的系列号
			int seriesCode = statusCode / 100;

			// 遍历所有定义的 Series 枚举值
			for (Series series : values()) {
				// 如果枚举值的系列号与计算得到的系列号相等，则返回该枚举值
				if (series.value == seriesCode) {
					return series;
				}
			}

			// 如果未找到对应的系列号枚举值，则返回 null
			return null;
		}
	}

}
