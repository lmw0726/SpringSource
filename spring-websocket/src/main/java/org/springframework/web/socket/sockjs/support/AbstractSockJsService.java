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

package org.springframework.web.socket.sockjs.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.*;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.sockjs.SockJsException;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 一个抽象基类，提供SockJS路径解析和处理静态SockJS请求（例如"/info"，"/iframe.html"等）的实现。
 * 子类必须处理会话URL（即特定于传输的请求）。
 * 默认情况下，只允许同源请求。使用{@link #setAllowedOrigins}来指定允许的源列表（包含"*"的列表将允许所有源）。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 4.0
 */
public abstract class AbstractSockJsService implements SockJsService, CorsConfigurationSource {

	/**
	 * X-Frame-Options HTTP头
	 */
	private static final String XFRAME_OPTIONS_HEADER = "X-Frame-Options";

	/**
	 * 一年的秒数
	 */
	private static final long ONE_YEAR = TimeUnit.DAYS.toSeconds(365);

	/**
	 * 随机数生成器
	 */
	private static final Random random = new Random();

	/**
	 * 日志记录器
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 任务调度器
	 */
	private final TaskScheduler taskScheduler;

	/**
	 * 服务名称，默认使用对象的唯一标识符
	 */
	private String name = "SockJSService@" + ObjectUtils.getIdentityHexString(this);

	/**
	 * 客户端库URL，默认指向SockJS的CDN
	 */
	private String clientLibraryUrl = "https://cdn.jsdelivr.net/sockjs/1.0.0/sockjs.min.js";

	/**
	 * 流字节限制，默认128KB
	 */
	private int streamBytesLimit = 128 * 1024;

	/**
	 * 会话是否需要cookie，默认需要
	 */
	private boolean sessionCookieNeeded = true;

	/**
	 * 心跳时间，默认25秒
	 */
	private long heartbeatTime = TimeUnit.SECONDS.toMillis(25);

	/**
	 * 断开延迟时间，默认5秒
	 */
	private long disconnectDelay = TimeUnit.SECONDS.toMillis(5);

	/**
	 * HTTP消息缓存大小，默认100
	 */
	private int httpMessageCacheSize = 100;

	/**
	 * 是否启用WebSocket，默认启用
	 */
	private boolean webSocketEnabled = true;

	/**
	 * 是否禁止CORS，默认不禁止
	 */
	private boolean suppressCors = false;

	/**
	 * CORS配置
	 */
	protected final CorsConfiguration corsConfiguration;

	/**
	 * SockJS请求处理器 - info处理器
	 */
	private final SockJsRequestHandler infoHandler = new InfoHandler();

	/**
	 * SockJS请求处理器 - iframe处理器
	 */
	private final SockJsRequestHandler iframeHandler = new IframeHandler();


	/**
	 * 创建一个 AbstractSockJsService 实例。
	 *
	 * @param scheduler 要使用的 TaskScheduler 实例，不能为 null
	 */
	public AbstractSockJsService(TaskScheduler scheduler) {
		Assert.notNull(scheduler, "TaskScheduler must not be null");
		this.taskScheduler = scheduler;
		this.corsConfiguration = initCorsConfiguration();
	}

	/**
	 * 初始化 CORS 配置。
	 *
	 * @return 配置好的 CorsConfiguration 实例
	 */
	private static CorsConfiguration initCorsConfiguration() {
		CorsConfiguration config = new CorsConfiguration();
		config.addAllowedMethod("*");
		config.setAllowedOrigins(Collections.emptyList());
		config.setAllowedOriginPatterns(Collections.emptyList());
		config.setAllowCredentials(true);
		config.setMaxAge(ONE_YEAR);
		config.addAllowedHeader("*");
		return config;
	}


	/**
	 * 获取用于调度心跳消息的调度程序实例。
	 *
	 * @return TaskScheduler 实例
	 */
	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	/**
	 * 设置此服务的唯一名称（主要用于日志记录）。
	 *
	 * @param name 服务的唯一名称
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 返回与此服务关联的唯一名称。
	 *
	 * @return 服务的唯一名称
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 没有原生跨域通信的传输（例如 "eventsource", "htmlfile"）必须从"外部"域中获取一个简单的页面，
	 * 并放置在一个不可见的 iframe 中，以便 iframe 中的代码可以从本地 SockJS 服务器的域运行。
	 * 由于 iframe 需要加载 SockJS JavaScript 客户端库，因此此属性允许指定从哪里加载它。
	 * <p>默认情况下，这指向 "https://cdn.jsdelivr.net/sockjs/1.0.0/sockjs.min.js"。
	 * 但是，也可以将其设置为指向由应用程序提供的 URL。
	 * <p>请注意，可以指定相对 URL，在这种情况下，URL 必须相对于 iframe URL。
	 * 例如，假设 SockJS 端点映射到 "/sockjs"，并且生成的 iframe URL 为 "/sockjs/iframe.html"，则相对 URL 必须以 "../../" 开头，
	 * 以遍历到 SockJS 映射之上的位置。在基于前缀的 Servlet 映射的情况下，可能需要多一次遍历。
	 */
	public void setSockJsClientLibraryUrl(String clientLibraryUrl) {
		this.clientLibraryUrl = clientLibraryUrl;
	}

	/**
	 * 返回 SockJS JavaScript 客户端库的 URL。
	 */
	public String getSockJsClientLibraryUrl() {
		return this.clientLibraryUrl;
	}

	/**
	 * 流式传输会在客户端保存响应，并且不会释放已传递消息所使用的内存。
	 * 这种传输方式需要不时地回收连接。此属性设置可以通过单个 HTTP 流请求发送的最小字节数，
	 * 在此之后客户端将打开一个新请求。将此值设置为 1 实际上禁用了流传输，
	 * 并使流传输方式表现得像轮询传输方式。
	 * <p>默认值为 128K（即 128 * 1024）。
	 */
	public void setStreamBytesLimit(int streamBytesLimit) {
		this.streamBytesLimit = streamBytesLimit;
	}

	/**
	 * 返回可以通过单个 HTTP 流请求发送的最小字节数，在此之后请求将被关闭。
	 */
	public int getStreamBytesLimit() {
		return this.streamBytesLimit;
	}

	/**
	 * SockJS 协议要求服务器响应客户端的初始 "/info" 请求时包含 "cookie_needed" 布尔属性，
	 * 该属性指示应用程序是否需要使用 JSESSIONID cookie 才能正常运行，
	 * 例如用于负载均衡或在 Java Servlet 容器中使用 HTTP 会话。
	 * <p>这对于支持 XDomainRequest（修改后的 AJAX/XHR）的 IE 8,9 尤其重要，
	 * 它可以跨域请求，但不发送任何 cookie。在这些情况下，SockJS 客户端更喜欢使用 "iframe-htmlfile" 传输，
	 * 而不是 "xdr-streaming"，以便能够发送 cookie。
	 * <p>当此属性设置为 true 时，SockJS 协议还期望 SockJS 服务回显 JSESSIONID cookie。
	 * 但是，在 Servlet 容器中运行时，这是不必要的，因为容器会处理它。
	 * <p>默认值为 "true"，以最大限度地提高应用程序在支持 cookie（特别是 JSESSIONID cookie）的 IE 8,9 中正常工作的机会。
	 * 但是，如果不需要使用 cookie（和 HTTP 会话），应用程序可以选择将其设置为 "false"。
	 */
	public void setSessionCookieNeeded(boolean sessionCookieNeeded) {
		this.sessionCookieNeeded = sessionCookieNeeded;
	}

	/**
	 * 返回应用程序正常运行是否需要 JSESSIONID cookie。
	 */
	public boolean isSessionCookieNeeded() {
		return this.sessionCookieNeeded;
	}

	/**
	 * 指定在服务器未发送任何消息之后的时间（以毫秒为单位），服务器应向客户端发送心跳帧，
	 * 以防止连接中断。
	 * <p>默认值为 25,000（25 秒）。
	 */
	public void setHeartbeatTime(long heartbeatTime) {
		this.heartbeatTime = heartbeatTime;
	}

	/**
	 * 返回服务器未发送任何消息之后的时间（以毫秒为单位）。
	 */
	public long getHeartbeatTime() {
		return this.heartbeatTime;
	}

	/**
	 * 客户端在没有接收连接（即服务器可以向客户端发送数据的活动连接）的情况下，
	 * 被认为断开连接之前的时间（以毫秒为单位）。
	 * <p>默认值为 5000 毫秒。
	 */
	public void setDisconnectDelay(long disconnectDelay) {
		this.disconnectDelay = disconnectDelay;
	}

	/**
	 * 返回客户端被认为断开连接之前的时间（以毫秒为单位）。
	 */
	public long getDisconnectDelay() {
		return this.disconnectDelay;
	}

	/**
	 * 在等待来自客户端的下一个 HTTP 轮询请求时，会话可以缓存的服务器到客户端消息的数量。
	 * 所有 HTTP 传输都使用此属性，因为即使是流传输也会定期回收 HTTP 请求。
	 * <p>HTTP 请求之间的时间间隔应相对较短，并且不会超过允许的断开连接延迟（参见 {@link #setDisconnectDelay(long)}）；
	 * 默认值为 5 秒。
	 * <p>默认大小为 100。
	 */
	public void setHttpMessageCacheSize(int httpMessageCacheSize) {
		this.httpMessageCacheSize = httpMessageCacheSize;
	}

	/**
	 * 返回 HTTP 消息缓存的大小。
	 */
	public int getHttpMessageCacheSize() {
		return this.httpMessageCacheSize;
	}

	/**
	 * 一些负载均衡器不支持 WebSocket。此选项可用于在服务器端禁用 WebSocket 传输。
	 * <p>默认值为 "true"。
	 */
	public void setWebSocketEnabled(boolean webSocketEnabled) {
		this.webSocketEnabled = webSocketEnabled;
	}

	/**
	 * 返回是否启用了 WebSocket 传输。
	 */
	public boolean isWebSocketEnabled() {
		return this.webSocketEnabled;
	}

	/**
	 * 此选项可用于禁用 SockJS 请求的自动添加 CORS 头。
	 * <p>默认值为 "false"。
	 *
	 * @since 4.1.2
	 */
	public void setSuppressCors(boolean suppressCors) {
		this.suppressCors = suppressCors;
	}

	/**
	 * 返回是否已禁用 CORS 头的自动添加。
	 *
	 * @see #setSuppressCors
	 * @since 4.1.2
	 */
	public boolean shouldSuppressCors() {
		return this.suppressCors;
	}

	/**
	 * 设置允许浏览器跨域请求的源。
	 * 请参考 {@link CorsConfiguration#setAllowedOrigins(List)} 了解格式细节和注意事项，
	 * 并记住 CORS 规范不允许在 {@code allowCredentials=true} 的情况下使用 {@code "*"}。
	 * 要使用更灵活的源模式，请改用 {@link #setAllowedOriginPatterns}。
	 *
	 * <p>默认情况下，不允许任何源。当 {@link #setAllowedOriginPatterns(Collection) allowedOriginPatterns}
	 * 也被设置时，它优先于此属性。
	 *
	 * <p>注意，当启用 SockJS 并且源受到限制时，某些传输类型（基于 Iframe 的传输）由于无法检查请求源将被禁用。
	 * 因此，当源受到限制时，不支持 IE 6 到 9。
	 *
	 * @see #setAllowedOriginPatterns(Collection)
	 * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
	 * @see <a href="https://github.com/sockjs/sockjs-client#supported-transports-by-browser-html-served-from-http-or-https">SockJS supported transports by browser</a>
	 * @since 4.1.2
	 */
	public void setAllowedOrigins(Collection<String> allowedOrigins) {
		Assert.notNull(allowedOrigins, "Allowed origins Collection must not be null");
		this.corsConfiguration.setAllowedOrigins(new ArrayList<>(allowedOrigins));
	}

	/**
	 * 返回 {@link #setAllowedOrigins(Collection)} 配置的允许源。
	 *
	 * @since 4.1.2
	 */
	@SuppressWarnings("ConstantConditions")
	public Collection<String> getAllowedOrigins() {
		return this.corsConfiguration.getAllowedOrigins();
	}

	/**
	 * {@link #setAllowedOrigins(Collection)} 的替代方法，支持更灵活的模式来指定允许浏览器跨域请求的源。
	 * 请参考 {@link CorsConfiguration#setAllowedOriginPatterns(List)} 了解格式细节和其他注意事项。
	 * <p>默认情况下未设置此属性。
	 *
	 * @since 5.2.3
	 */
	public void setAllowedOriginPatterns(Collection<String> allowedOriginPatterns) {
		Assert.notNull(allowedOriginPatterns, "Allowed origin patterns Collection must not be null");
		this.corsConfiguration.setAllowedOriginPatterns(new ArrayList<>(allowedOriginPatterns));
	}

	/**
	 * 返回 {@link #setAllowedOriginPatterns(Collection)} 配置的源模式。
	 *
	 * @since 5.3.2
	 */
	@SuppressWarnings("ConstantConditions")
	public Collection<String> getAllowedOriginPatterns() {
		return this.corsConfiguration.getAllowedOriginPatterns();
	}


	/**
	 * 此方法确定 SockJS 路径并处理 SockJS 静态 URL。
	 * 会话 URL 和原始 WebSocket 请求将委托给抽象方法。
	 */
	@Override
	public final void handleRequest(ServerHttpRequest request, ServerHttpResponse response,
									@Nullable String sockJsPath, WebSocketHandler wsHandler) throws SockJsException {

		// 如果 sockJs路径 为空
		if (sockJsPath == null) {
			// 如果日志级别是警告，记录警告日志
			if (logger.isWarnEnabled()) {
				logger.warn(LogFormatUtils.formatValue(
						"Expected SockJS path. Failing request: " + request.getURI(), -1, true));
			}
			// 设置响应状态为 404 Not Found
			response.setStatusCode(HttpStatus.NOT_FOUND);
			return;
		}

		try {
			// 尝试获取请求头
			request.getHeaders();
		} catch (InvalidMediaTypeException ex) {
			// 根据 SockJS 协议，可以忽略 content-type（总是 json）
		}

		// 如果日志级别是调试，记录请求方法和 URI
		String requestInfo = (logger.isDebugEnabled() ? request.getMethod() + " " + request.getURI() : null);

		try {
			// 如果 sockJs路径 为空或等于 "/"
			if (sockJsPath.isEmpty() || sockJsPath.equals("/")) {
				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				// 如果请求头中的 Upgrade 是 "websocket"
				if ("websocket".equalsIgnoreCase(request.getHeaders().getUpgrade())) {
					// 将响应状态码设置为 400 Bad Request
					response.setStatusCode(HttpStatus.BAD_REQUEST);
					return;
				}
				// 设置响应内容类型为 text/plain，编码为 UTF-8
				response.getHeaders().setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
				// 响应体写入 "Welcome to SockJS!\n"
				response.getBody().write("Welcome to SockJS!\n".getBytes(StandardCharsets.UTF_8));
			} else if (sockJsPath.equals("/info")) {
				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				// 处理 /info 请求
				this.infoHandler.handle(request, response);
			} else if (sockJsPath.matches("/iframe[0-9-.a-z_]*.html")) {
				// 如果允许的源列表不为空且不包含 "*"，或者允许的源模式列表不为空
				if (!getAllowedOrigins().isEmpty() && !getAllowedOrigins().contains("*") ||
						!getAllowedOriginPatterns().isEmpty()) {
					if (requestInfo != null) {
						logger.debug("Iframe support is disabled when an origin check is required. " +
								"Ignoring transport request: " + requestInfo);
					}
					// 将响应状态码设置为 404 Not Found
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}
				// 如果允许的源列表为空，设置 X-Frame-Options 头为 SAMEORIGIN
				if (getAllowedOrigins().isEmpty()) {
					response.getHeaders().add(XFRAME_OPTIONS_HEADER, "SAMEORIGIN");
				}
				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				// 处理 iframe 请求
				this.iframeHandler.handle(request, response);
			} else if (sockJsPath.equals("/websocket")) {
				// 如果 WebSocket 启用，处理原始 WebSocket 请求
				if (isWebSocketEnabled()) {
					if (requestInfo != null) {
						logger.debug("Processing transport request: " + requestInfo);
					}
					// 处理原始 WebSocket 请求
					handleRawWebSocketRequest(request, response, wsHandler);
				} else if (requestInfo != null) {
					logger.debug("WebSocket disabled. Ignoring transport request: " + requestInfo);
				}
			} else {
				// 解析路径段
				String[] pathSegments = StringUtils.tokenizeToStringArray(sockJsPath.substring(1), "/");
				if (pathSegments.length != 3) {
					// 如果路径段的数量不是3个
					if (logger.isWarnEnabled()) {
						logger.warn(LogFormatUtils.formatValue("Invalid SockJS path '" + sockJsPath + "' - " +
								"required to have 3 path segments", -1, true));
					}
					if (requestInfo != null) {
						logger.debug("Ignoring transport request: " + requestInfo);
					}
					// 将响应状态码设置为 404 Not Found
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}
				// 获取服务器编号
				String serverId = pathSegments[0];
				// 获取会话编号
				String sessionId = pathSegments[1];
				// 获取传输
				String transport = pathSegments[2];

				// 如果 WebSocket 未启用且传输是 "websocket"
				if (!isWebSocketEnabled() && transport.equals("websocket")) {
					if (requestInfo != null) {
						logger.debug("WebSocket disabled. Ignoring transport request: " + requestInfo);
					}
					// 将响应状态码设置为 404 Not Found
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				} else if (!validateRequest(serverId, sessionId, transport) || !validatePath(request)) {
					// 如果请求无效或者路径无效，记录警告日志
					if (requestInfo != null) {
						logger.debug("Ignoring transport request: " + requestInfo);
					}
					// 将响应状态码设置为 404 Not Found
					response.setStatusCode(HttpStatus.NOT_FOUND);
					return;
				}

				if (requestInfo != null) {
					logger.debug("Processing transport request: " + requestInfo);
				}
				// 处理传输请求
				handleTransportRequest(request, response, wsHandler, sessionId, transport);
			}
			response.close();
		} catch (IOException ex) {
			throw new SockJsException("Failed to write to the response", null, ex);
		}
	}

	protected boolean validateRequest(String serverId, String sessionId, String transport) {
		// 检查 服务器编号、会话编号 和 传输 是否都有文本内容，如果有任何一个没有文本内容，则返回 false
		if (!StringUtils.hasText(serverId) || !StringUtils.hasText(sessionId) || !StringUtils.hasText(transport)) {
			// 如果任何一个路径段缺少文本内容，则记录警告日志并返回 false
			logger.warn("No server, session, or transport path segment in SockJS request.");
			return false;
		}

		// 服务器和会话 ID 不得包含 "."
		if (serverId.contains(".") || sessionId.contains(".")) {
			// 如果服务器或会话包含 "."，则记录警告日志并返回 false
			logger.warn("Either server or session contains a \".\" which is not allowed by SockJS protocol.");
			return false;
		}

		// 如果上述条件都满足，则返回 true
		return true;
	}

	/**
	 * 确保路径中不包含文件扩展名，无论是在文件名中（例如 "/jsonp.bat"），
	 * 还是可能在路径参数之后（"/jsonp;Setup.bat"），这可能会被用于 RFD 攻击。
	 * <p>由于路径的最后一部分预期是传输类型，因此存在扩展名是不可行的。我们需要做的就是检查是否有任何路径参数，
	 * 这些参数在请求映射期间会从 SockJS 路径中移除，如果找到则拒绝请求。
	 */
	private boolean validatePath(ServerHttpRequest request) {
		// 获取请求的 URI 路径
		String path = request.getURI().getPath();
		// 找到路径中最后一个 '/' 的位置，并加 1
		int index = path.lastIndexOf('/') + 1;
		// 检查从最后一个 '/' 位置开始是否存在 ';' 字符，如果不存在返回 true，否则返回 false
		return (path.indexOf(';', index) == -1);
	}

	protected boolean checkOrigin(ServerHttpRequest request, ServerHttpResponse response, HttpMethod... httpMethods)
			throws IOException {

		// 如果请求是同源的，直接返回 true
		if (WebUtils.isSameOrigin(request)) {
			return true;
		}

		// 检查 CORS 配置是否允许请求的 Origin
		if (this.corsConfiguration.checkOrigin(request.getHeaders().getOrigin()) == null) {
			// 如果不允许，并且日志级别为警告或更高，则记录警告日志
			if (logger.isWarnEnabled()) {
				logger.warn("Origin header value '" + request.getHeaders().getOrigin() + "' not allowed.");
			}
			// 设置响应状态为 403 Forbidden
			response.setStatusCode(HttpStatus.FORBIDDEN);
			// 返回 false 表示请求不被允许
			return false;
		}

		// 返回 true 表示请求被允许
		return true;
	}

	@Override
	@Nullable
	public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
		// 如果不抑制 CORS 并且请求头中包含 Origin 字段
		if (!this.suppressCors && (request.getHeader(HttpHeaders.ORIGIN) != null)) {
			// 返回 CORS 配置
			return this.corsConfiguration;
		}

		// 否则返回 null
		return null;
	}

	protected void addCacheHeaders(ServerHttpResponse response) {
		// 设置响应头部的 Cache-Control 字段，使其缓存为公开，最大有效期为一年
		response.getHeaders().setCacheControl("public, max-age=" + ONE_YEAR);

		// 设置响应头部的 Expires 字段，使其过期时间为当前时间加上一年（以毫秒为单位）
		response.getHeaders().setExpires(System.currentTimeMillis() + ONE_YEAR * 1000);
	}

	protected void addNoCacheHeaders(ServerHttpResponse response) {
		response.getHeaders().setCacheControl("no-store, no-cache, must-revalidate, max-age=0");
	}

	protected void sendMethodNotAllowed(ServerHttpResponse response, HttpMethod... httpMethods) {
		// 记录警告日志，表示正在发送 405 方法不允许的响应
		logger.warn("Sending Method Not Allowed (405)");

		// 设置响应状态码为 405 方法不允许
		response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);

		// 设置响应头部的 Allow 字段，指明允许的方法
		response.getHeaders().setAllow(new LinkedHashSet<>(Arrays.asList(httpMethods)));
	}


	/**
	 * 处理原始 WebSocket 通信请求，即没有任何 SockJS 消息框架的请求。
	 */
	protected abstract void handleRawWebSocketRequest(ServerHttpRequest request,
													  ServerHttpResponse response, WebSocketHandler webSocketHandler) throws IOException;

	/**
	 * 处理 SockJS 会话 URL（即特定传输方式的请求）。
	 */
	protected abstract void handleTransportRequest(ServerHttpRequest request, ServerHttpResponse response,
												   WebSocketHandler webSocketHandler, String sessionId, String transport) throws SockJsException;


	private interface SockJsRequestHandler {

		void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException;
	}


	private class InfoHandler implements SockJsRequestHandler {

		private static final String INFO_CONTENT =
				"{\"entropy\":%s,\"origins\":[\"*:*\"],\"cookie_needed\":%s,\"websocket\":%s}";

		@Override
		public void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
			if (request.getMethod() == HttpMethod.GET) {
				// 如果请求的方法是 GET
				// 添加不缓存的头部信息
				addNoCacheHeaders(response);
				// 检查请求的来源是否合法
				if (checkOrigin(request, response)) {
					// 设置响应的 Content-Type 头部为 application/json，字符集为 UTF-8
					response.getHeaders().setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
					// 创建返回的 JSON 内容
					String content = String.format(
							INFO_CONTENT, random.nextInt(), isSessionCookieNeeded(), isWebSocketEnabled());
					// 将 JSON 内容写入响应体
					response.getBody().write(content.getBytes());
				}
			} else if (request.getMethod() == HttpMethod.OPTIONS) {
				// 如果请求的方法是 OPTIONS
				// 检查请求的来源是否合法
				if (checkOrigin(request, response)) {
					// 添加缓存的头部信息
					addCacheHeaders(response);
					// 设置响应状态为 204 No Content
					response.setStatusCode(HttpStatus.NO_CONTENT);
				}
			} else {
				// 如果请求的方法既不是 GET 也不是 OPTIONS
				// 发送 405 Method Not Allowed 响应，并指明允许的方法是 GET 和 OPTIONS
				sendMethodNotAllowed(response, HttpMethod.GET, HttpMethod.OPTIONS);
			}
		}
	}


	private class IframeHandler implements SockJsRequestHandler {

		private static final String IFRAME_CONTENT =
				"<!DOCTYPE html>\n" +
						"<html>\n" +
						"<head>\n" +
						"  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\" />\n" +
						"  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
						"  <script>\n" +
						"    document.domain = document.domain;\n" +
						"    _sockjs_onload = function(){SockJS.bootstrap_iframe();};\n" +
						"  </script>\n" +
						"  <script src=\"%s\"></script>\n" +
						"</head>\n" +
						"<body>\n" +
						"  <h2>Don't panic!</h2>\n" +
						"  <p>This is a SockJS hidden iframe. It's used for cross domain magic.</p>\n" +
						"</body>\n" +
						"</html>";

		@Override
		public void handle(ServerHttpRequest request, ServerHttpResponse response) throws IOException {
			if (request.getMethod() != HttpMethod.GET) {
				// 如果请求的方法不是 GET，发送 405 Method Not Allowed 响应
				sendMethodNotAllowed(response, HttpMethod.GET);
				return;
			}

			// 创建 IFrame 内容的字符串
			String content = String.format(IFRAME_CONTENT, getSockJsClientLibraryUrl());

			// 将内容转换为字节数组
			byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

			// 构建 ETag 值
			StringBuilder builder = new StringBuilder("\"0");
			DigestUtils.appendMd5DigestAsHex(contentBytes, builder);
			builder.append('"');
			String etagValue = builder.toString();

			// 获取请求中的 If-None-Match 头部
			List<String> ifNoneMatch = request.getHeaders().getIfNoneMatch();

			if (!CollectionUtils.isEmpty(ifNoneMatch) && ifNoneMatch.get(0).equals(etagValue)) {
				// 如果 If-None-Match 头部不为空且匹配 ETag 值，设置 304 Not Modified 响应状态
				response.setStatusCode(HttpStatus.NOT_MODIFIED);
				return;
			}

			// 设置响应的 Content-Type 头部为 text/html，字符集为 UTF-8
			response.getHeaders().setContentType(new MediaType("text", "html", StandardCharsets.UTF_8));

			// 设置响应的 Content-Length 头部为内容字节数组的长度
			response.getHeaders().setContentLength(contentBytes.length);

			// 为了每次都检查 IFrame 是否被授权，设置不缓存的头部
			addNoCacheHeaders(response);

			// 设置响应的 ETag 头部
			response.getHeaders().setETag(etagValue);

			// 将内容字节数组写入响应体
			response.getBody().write(contentBytes);
		}
	}

}
