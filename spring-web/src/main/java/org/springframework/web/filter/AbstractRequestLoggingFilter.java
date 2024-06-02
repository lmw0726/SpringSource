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

package org.springframework.web.filter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.WebUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.function.Predicate;

/**
 * 执行日志操作前后请求处理的{@code Filter}的基类。
 *
 * <p>子类应覆盖{@code beforeRequest(HttpServletRequest, String)}和{@code afterRequest(HttpServletRequest, String)}方法，
 * 以在请求周围执行实际的日志记录。
 *
 * <p>子类在{@code beforeRequest}和{@code afterRequest}方法中传递要写入日志的消息。
 * 默认情况下，仅记录请求的URI。但是，将{@code includeQueryString}属性设置为{@code true}将导致还包括请求的查询字符串；
 * 这可以通过{@code includeClientInfo}和{@code includeHeaders}进一步扩展。
 * 可以通过{@code includePayload}标志记录请求的有效负载（正文内容）：请注意，这只会记录实际已读取的有效负载的部分，而不一定是请求的整个正文。
 *
 * <p>可以使用{@code beforeMessagePrefix}、{@code afterMessagePrefix}、{@code beforeMessageSuffix}和{@code afterMessageSuffix}属性配置前后消息的前缀和后缀。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see #beforeRequest
 * @see #afterRequest
 * @since 1.2.5
 */
public abstract class AbstractRequestLoggingFilter extends OncePerRequestFilter {

	/**
	 * 在处理请求<i>之前</i>写入的日志消息的默认值前置。
	 */
	public static final String DEFAULT_BEFORE_MESSAGE_PREFIX = "Before request [";

	/**
	 * 在处理请求<i>之前</i>写入的日志消息的默认值后置。
	 */
	public static final String DEFAULT_BEFORE_MESSAGE_SUFFIX = "]";

	/**
	 * 在处理请求<i>后</i>写入的日志消息的默认值前置。
	 */
	public static final String DEFAULT_AFTER_MESSAGE_PREFIX = "After request [";

	/**
	 * 在处理请求<i>后</i>写入的日志消息的默认值后置。
	 */
	public static final String DEFAULT_AFTER_MESSAGE_SUFFIX = "]";

	/**
	 * 默认最大有效负载长度。
	 */
	private static final int DEFAULT_MAX_PAYLOAD_LENGTH = 50;

	/**
	 * 是否包含查询字符串，默认为false。
	 */
	private boolean includeQueryString = false;

	/**
	 * 是否包含客户端信息，默认为false。
	 */
	private boolean includeClientInfo = false;

	/**
	 * 是否包含请求头，默认为false。
	 */
	private boolean includeHeaders = false;

	/**
	 * 是否包含有效负载（请求正文内容），默认为false。
	 */
	private boolean includePayload = false;

	/**
	 * 头部谓词，可为null。
	 */
	@Nullable
	private Predicate<String> headerPredicate;

	/**
	 * 最大有效负载长度，默认为{@link #DEFAULT_MAX_PAYLOAD_LENGTH}。
	 */
	private int maxPayloadLength = DEFAULT_MAX_PAYLOAD_LENGTH;

	/**
	 * 处理请求<i>之前</i>写入的日志消息的默认前缀。
	 */
	private String beforeMessagePrefix = DEFAULT_BEFORE_MESSAGE_PREFIX;

	/**
	 * 处理请求<i>之前</i>写入的日志消息的默认后缀。
	 */
	private String beforeMessageSuffix = DEFAULT_BEFORE_MESSAGE_SUFFIX;

	/**
	 * 处理请求<i>后</i>写入的日志消息的默认前缀。
	 */
	private String afterMessagePrefix = DEFAULT_AFTER_MESSAGE_PREFIX;

	/**
	 * 处理请求<i>后</i>写入的日志消息的默认后缀。
	 */
	private String afterMessageSuffix = DEFAULT_AFTER_MESSAGE_SUFFIX;


	/**
	 * 设置是否应将查询字符串包含在日志消息中。
	 * <p>应该使用{@code web.xml}中的过滤器定义中的参数名“includeQueryString”的{@code <init-param>}进行配置。
	 */
	public void setIncludeQueryString(boolean includeQueryString) {
		this.includeQueryString = includeQueryString;
	}

	/**
	 * 返回是否应将查询字符串包含在日志消息中。
	 */
	protected boolean isIncludeQueryString() {
		return this.includeQueryString;
	}

	/**
	 * 设置是否应在日志消息中包含客户端地址和会话ID。
	 * <p>应该使用{@code web.xml}中的过滤器定义中的参数名“includeClientInfo”的{@code <init-param>}进行配置。
	 */
	public void setIncludeClientInfo(boolean includeClientInfo) {
		this.includeClientInfo = includeClientInfo;
	}

	/**
	 * 返回是否应在日志消息中包含客户端地址和会话ID。
	 */
	protected boolean isIncludeClientInfo() {
		return this.includeClientInfo;
	}

	/**
	 * 设置是否将请求头包含在日志消息中。
	 * <p>应该使用{@code <init-param>}配置参数名为"includeHeaders"的方式在{@code web.xml}中的过滤器定义中进行配置。
	 *
	 * @param includeHeaders 是否包含请求头
	 * @since 4.3
	 */
	public void setIncludeHeaders(boolean includeHeaders) {
		this.includeHeaders = includeHeaders;
	}

	/**
	 * 返回是否将请求头包含在日志消息中。
	 *
	 * @return 是否包含请求头
	 * @since 4.3
	 */
	protected boolean isIncludeHeaders() {
		return this.includeHeaders;
	}

	/**
	 * 设置是否将请求有效负载（主体）包含在日志消息中。
	 * <p>应该使用{@code <init-param>}配置参数名为"includePayload"的方式在{@code web.xml}中的过滤器定义中进行配置。
	 *
	 * @param includePayload 是否包含请求有效负载
	 * @since 3.0
	 */
	public void setIncludePayload(boolean includePayload) {
		this.includePayload = includePayload;
	}

	/**
	 * 返回是否将请求有效负载（主体）包含在日志消息中。
	 *
	 * @return 是否包含请求有效负载
	 * @since 3.0
	 */
	protected boolean isIncludePayload() {
		return this.includePayload;
	}

	/**
	 * 配置用于选择哪些请求头应在{@link #setIncludeHeaders(boolean)}设置为{@code true}时记录的谓词。
	 * <p>默认情况下，未设置任何内容，因此所有头都将被记录。
	 *
	 * @param headerPredicate 要使用的谓词
	 * @since 5.2
	 */
	public void setHeaderPredicate(@Nullable Predicate<String> headerPredicate) {
		this.headerPredicate = headerPredicate;
	}

	/**
	 * 配置的{@link #setHeaderPredicate(Predicate) headerPredicate}。
	 *
	 * @return 配置的头部谓词
	 * @since 5.2
	 */
	@Nullable
	protected Predicate<String> getHeaderPredicate() {
		return this.headerPredicate;
	}

	/**
	 * 设置要包含在日志消息中的有效负载主体的最大长度。
	 * 默认为50个字符。
	 *
	 * @param maxPayloadLength 要设置的最大有效负载长度
	 * @since 3.0
	 */
	public void setMaxPayloadLength(int maxPayloadLength) {
		Assert.isTrue(maxPayloadLength >= 0, "'maxPayloadLength' should be larger than or equal to 0");
		this.maxPayloadLength = maxPayloadLength;
	}

	/**
	 * 返回要包含在日志消息中的有效负载主体的最大长度。
	 *
	 * @return 最大有效负载长度
	 * @since 3.0
	 */
	protected int getMaxPayloadLength() {
		return this.maxPayloadLength;
	}

	/**
	 * 设置要在处理请求<i>之前</i>写入的日志消息中添加的前缀。
	 *
	 * @param beforeMessagePrefix 要设置的前缀值
	 */
	public void setBeforeMessagePrefix(String beforeMessagePrefix) {
		this.beforeMessagePrefix = beforeMessagePrefix;
	}

	/**
	 * 设置要在处理请求<i>之前</i>写入的日志消息中添加的后缀。
	 *
	 * @param beforeMessageSuffix 要设置的后缀值
	 */
	public void setBeforeMessageSuffix(String beforeMessageSuffix) {
		this.beforeMessageSuffix = beforeMessageSuffix;
	}

	/**
	 * 设置要在处理请求<i>之后</i>写入的日志消息中添加的前缀。
	 *
	 * @param afterMessagePrefix 要设置的前缀值
	 */
	public void setAfterMessagePrefix(String afterMessagePrefix) {
		this.afterMessagePrefix = afterMessagePrefix;
	}

	/**
	 * 设置要在处理请求<i>之后</i>写入的日志消息中添加的后缀。
	 *
	 * @param afterMessageSuffix 要设置的后缀值
	 */
	public void setAfterMessageSuffix(String afterMessageSuffix) {
		this.afterMessageSuffix = afterMessageSuffix;
	}


	/**
	 * 默认值为"false"，以便过滤器可以在请求处理开始时记录“before”消息，
	 * 并在最后一个异步调度线程退出时从末端记录“after”消息。
	 */
	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return false;
	}

	/**
	 * 转发请求到链中的下一个过滤器，并委托给子类执行实际的请求日志记录，包括请求处理前后的日志记录。
	 *
	 * @param request     当前请求
	 * @param response    当前响应
	 * @param filterChain 过滤器链
	 * @throws ServletException 如果servlet出现错误
	 * @throws IOException      如果IO异常发生
	 * @see #beforeRequest
	 * @see #afterRequest
	 */
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// 检查当前请求是否为第一个请求
		boolean isFirstRequest = !isAsyncDispatch(request);

		// 将请求对象设置为要使用的请求
		HttpServletRequest requestToUse = request;

		// 如果启用了请求体缓存且是第一个请求，并且请求对象不是 ContentCachingRequestWrapper 类型的
		if (isIncludePayload() && isFirstRequest && !(request instanceof ContentCachingRequestWrapper)) {
			// 创建一个新的 ContentCachingRequestWrapper 对象
			requestToUse = new ContentCachingRequestWrapper(request, getMaxPayloadLength());
		}

		// 检查是否应该记录请求
		boolean shouldLog = shouldLog(requestToUse);

		// 如果应该记录请求且是第一个请求
		if (shouldLog && isFirstRequest) {
			// 调用beforeRequest方法
			beforeRequest(requestToUse, getBeforeMessage(requestToUse));
		}

		try {
			// 调用过滤器链的下一个过滤器
			filterChain.doFilter(requestToUse, response);
		} finally {
			// 如果应该记录请求且请求未异步启动
			if (shouldLog && !isAsyncStarted(requestToUse)) {
				// 调用afterRequest方法
				afterRequest(requestToUse, getAfterMessage(requestToUse));
			}
		}
	}

	/**
	 * 获取请求处理前要写入日志的消息。
	 *
	 * @param request 当前请求
	 * @return 要写入日志的消息
	 * @see #createMessage
	 */
	private String getBeforeMessage(HttpServletRequest request) {
		return createMessage(request, this.beforeMessagePrefix, this.beforeMessageSuffix);
	}

	/**
	 * 获取请求处理后要写入日志的消息。
	 *
	 * @param request 当前请求
	 * @return 要写入日志的消息
	 * @see #createMessage
	 */
	private String getAfterMessage(HttpServletRequest request) {
		return createMessage(request, this.afterMessagePrefix, this.afterMessageSuffix);
	}

	/**
	 * 创建给定请求、前缀和后缀的日志消息。
	 * <p>如果{@code includeQueryString}为{@code true}，则内部部分的日志消息将采用{@code request_uri?query_string}的形式；
	 * 否则消息将简单地采用{@code request_uri}的形式。
	 * <p>最终的消息由上述内部部分和提供的前缀和后缀组成。
	 *
	 * @param request 当前请求
	 * @param prefix  日志消息的前缀
	 * @param suffix  日志消息的后缀
	 * @return 构建的日志消息
	 */
	protected String createMessage(HttpServletRequest request, String prefix, String suffix) {
		// 创建一个 StringBuilder 对象
		StringBuilder msg = new StringBuilder();

		// 添加前缀
		msg.append(prefix);

		// 添加请求方法和请求 URI
		msg.append(request.getMethod()).append(' ');
		msg.append(request.getRequestURI());

		// 如果包含查询字符串，则添加查询字符串
		if (isIncludeQueryString()) {
			String queryString = request.getQueryString();
			if (queryString != null) {
				msg.append('?').append(queryString);
			}
		}

		// 如果包含客户端信息，则添加客户端信息
		if (isIncludeClientInfo()) {
			// 添加客户端
			String client = request.getRemoteAddr();
			if (StringUtils.hasLength(client)) {
				msg.append(", client=").append(client);
			}
			// 添加session信息
			HttpSession session = request.getSession(false);
			if (session != null) {
				msg.append(", session=").append(session.getId());
			}
			// 添加用户
			String user = request.getRemoteUser();
			if (user != null) {
				msg.append(", user=").append(user);
			}
		}

		// 如果包含请求头，则添加请求头
		if (isIncludeHeaders()) {
			// 获取请求头
			HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
			if (getHeaderPredicate() != null) {
				// 如果存在请求头断言，遍历请求头名称
				Enumeration<String> names = request.getHeaderNames();
				while (names.hasMoreElements()) {
					String header = names.nextElement();
					if (!getHeaderPredicate().test(header)) {
						// 如果未通过断言，则设置该请求头为 masked
						headers.set(header, "masked");
					}
				}
			}
			// 添加请求头信息
			msg.append(", headers=").append(headers);
		}

		// 如果包含请求体，则添加请求体
		if (isIncludePayload()) {
			// 添加请求体
			String payload = getMessagePayload(request);
			if (payload != null) {
				msg.append(", payload=").append(payload);
			}
		}

		// 添加后缀
		msg.append(suffix);

		// 将 StringBuilder 对象转换为字符串并返回
		return msg.toString();
	}

	/**
	 * 当{@link #isIncludePayload()}返回true时，从{@link #createMessage(HttpServletRequest, String, String)}创建的消息中提取消息载荷部分。
	 *
	 * @param request 当前请求
	 * @return 消息载荷部分，如果为null则返回null
	 * @since 5.0.3
	 */
	@Nullable
	protected String getMessagePayload(HttpServletRequest request) {
		// 从 request 中获取 ContentCachingRequestWrapper 对象
		ContentCachingRequestWrapper wrapper = WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);

		// 如果 wrapper 不为空
		if (wrapper != null) {
			// 获取请求体的字节数组
			byte[] buf = wrapper.getContentAsByteArray();
			// 如果字节数组的长度大于 0
			if (buf.length > 0) {
				// 获取要读取的长度，取 buf.length 和最大载荷长度中较小的一个
				int length = Math.min(buf.length, getMaxPayloadLength());
				try {
					// 将字节数组转换为字符串，并返回
					return new String(buf, 0, length, wrapper.getCharacterEncoding());
				} catch (UnsupportedEncodingException ex) {
					// 如果转换时出现异常，则返回 "[unknown]"
					return "[unknown]";
				}
			}
		}

		// 如果 wrapper 为空或者请求体为空，则返回 null
		return null;
	}


	/**
	 * 确定当前请求是否应调用{@link #beforeRequest}/{@link #afterRequest}方法，即当前是否处于活动日志记录状态（并且值得构建日志消息）。
	 * <p>默认实现始终返回{@code true}。子类可以覆盖此方法以进行日志级别检查。
	 *
	 * @param request 当前HTTP请求
	 * @return 如果应调用before/after方法则为{@code true}；否则为{@code false}
	 * @since 4.1.5
	 */
	protected boolean shouldLog(HttpServletRequest request) {
		return true;
	}

	/**
	 * 具体的子类应实现此方法，以在处理请求<i>之前</i>写入日志消息。
	 *
	 * @param request 当前HTTP请求
	 * @param message 要记录的消息
	 */
	protected abstract void beforeRequest(HttpServletRequest request, String message);

	/**
	 * 具体的子类应实现此方法，以在处理请求<i>之后</i>写入日志消息。
	 *
	 * @param request 当前HTTP请求
	 * @param message 要记录的消息
	 */
	protected abstract void afterRequest(HttpServletRequest request, String message);

}
