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

package org.springframework.web.servlet.support;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.context.support.WebApplicationObjectSupport;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 用于任何类型的 Web 内容生成器的便捷超类，例如 {@link org.springframework.web.servlet.mvc.AbstractController}
 * 和 {@link org.springframework.web.servlet.mvc.WebContentInterceptor}。也可用于具有自己的
 * {@link org.springframework.web.servlet.HandlerAdapter} 的自定义处理程序。
 *
 * <p>支持 HTTP 缓存控制选项。可以通过 {@link #setCacheSeconds "cacheSeconds"}
 * 和 {@link #setCacheControl "cacheControl"} 属性控制使用相应的 HTTP 标头。
 *
 * <p><b>注意:</b> 从 Spring 4.2 开始，当仅使用 {@link #setCacheSeconds} 时，此生成器的默认行为已更改，
 * 发送与当前浏览器和代理实现相符的 HTTP 响应标头（不再发送 HTTP 1.0 标头）。可以通过使用新的已弃用方法之一
 * {@link #setUseExpiresHeader}、{@link #setUseCacheControlHeader}、
 * {@link #setUseCacheControlNoStore} 或 {@link #setAlwaysMustRevalidate} 来恢复到以前的行为。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @see #setCacheSeconds
 * @see #setCacheControl
 * @see #setRequireSession
 */
public abstract class WebContentGenerator extends WebApplicationObjectSupport {

	/**
	 * HTTP 方法 "GET"。
	 */
	public static final String METHOD_GET = "GET";

	/**
	 * HTTP 方法 "HEAD"。
	 */
	public static final String METHOD_HEAD = "HEAD";

	/**
	 * HTTP 方法 "POST"。
	 */
	public static final String METHOD_POST = "POST";

	/**
	 * 公开缓存请求头
	 */
	private static final String HEADER_PRAGMA = "Pragma";

	/**
	 * 过期时间请求头
	 */
	private static final String HEADER_EXPIRES = "Expires";

	/**
	 * 缓存控制请求头
	 */
	protected static final String HEADER_CACHE_CONTROL = "Cache-Control";


	/**
	 * 支持的 HTTP 方法集合。
	 */
	@Nullable
	private Set<String> supportedMethods;

	/**
	 * 允许的请求头
	 */
	@Nullable
	private String allowHeader;

	/**
	 * 是否需要 会话
	 */
	private boolean requireSession = false;

	/**
	 * 缓存控制
	 */
	@Nullable
	private CacheControl cacheControl;

	/**
	 * 缓存秒数
	 */
	private int cacheSeconds = -1;

	/**
	 * “Vary”响应头的配置的请求头名称
	 */
	@Nullable
	private String[] varyByRequestHeaders;


	// 已弃用的字段

	/**
	 * 使用 HTTP 1.0 的过期标头吗？
	 */
	private boolean useExpiresHeader = false;

	/**
	 * 使用 HTTP 1.1 的缓存控制标头吗？
	 */
	private boolean useCacheControlHeader = true;

	/**
	 * 使用 HTTP 1.1 的缓存控制标头值 "no-store" 吗？
	 */
	private boolean useCacheControlNoStore = true;

	/**
	 * 是否在每个 Cache-Control 标头中添加 'must-revalidate'。
	 */
	private boolean alwaysMustRevalidate = false;


	/**
	 * 创建一个新的 WebContentGenerator，默认情况下支持 HTTP 方法 GET、HEAD 和 POST。
	 */
	public WebContentGenerator() {
		this(true);
	}

	/**
	 * 创建一个新的 WebContentGenerator。
	 *
	 * @param restrictDefaultSupportedMethods {@code true} 表示此生成器默认支持 HTTP 方法 GET、HEAD 和 POST，
	 *                                        {@code false} 表示不受限制
	 */
	public WebContentGenerator(boolean restrictDefaultSupportedMethods) {
		if (restrictDefaultSupportedMethods) {
			this.supportedMethods = new LinkedHashSet<>(4);
			// 支持方法集合添加 GET、HEAD 和 POST 方法
			this.supportedMethods.add(METHOD_GET);
			this.supportedMethods.add(METHOD_HEAD);
			this.supportedMethods.add(METHOD_POST);
		}
		initAllowHeader();
	}

	/**
	 * 创建一个新的 WebContentGenerator。
	 *
	 * @param supportedMethods 此内容生成器支持的 HTTP 方法
	 */
	public WebContentGenerator(String... supportedMethods) {
		setSupportedMethods(supportedMethods);
	}


	/**
	 * 设置此内容生成器应支持的 HTTP 方法。
	 * <p>对于简单表单控制器类型，默认为 GET、HEAD 和 POST；对于一般控制器和拦截器，默认为不受限制。
	 */
	public final void setSupportedMethods(@Nullable String... methods) {
		if (!ObjectUtils.isEmpty(methods)) {
			this.supportedMethods = new LinkedHashSet<>(Arrays.asList(methods));
		} else {
			this.supportedMethods = null;
		}
		// 初始化允许请求头
		initAllowHeader();
	}

	/**
	 * 返回此内容生成器支持的 HTTP 方法。
	 */
	@Nullable
	public final String[] getSupportedMethods() {
		return (this.supportedMethods != null ? StringUtils.toStringArray(this.supportedMethods) : null);
	}

	private void initAllowHeader() {
		// 定义允许的方法集合
		Collection<String> allowedMethods;

		if (this.supportedMethods == null) {
			// 如果支持的方法为空
			allowedMethods = new ArrayList<>(HttpMethod.values().length - 1);
			for (HttpMethod method : HttpMethod.values()) {
				if (method != HttpMethod.TRACE) {
					// 只要不是Trace方法，都添加进允许方法中
					allowedMethods.add(method.name());
				}
			}
		} else if (this.supportedMethods.contains(HttpMethod.OPTIONS.name())) {
			// 如果支持的方法包含 OPTIONS 方法，则直接使用支持的方法集合
			allowedMethods = this.supportedMethods;
		} else {
			// 否则，将 OPTIONS 方法添加到支持的方法集合中，并使用该集合
			allowedMethods = new ArrayList<>(this.supportedMethods);
			allowedMethods.add(HttpMethod.OPTIONS.name());

		}

		// 将允许的方法集合转换为逗号分隔的字符串，用于设置 Allow 响应头
		this.allowHeader = StringUtils.collectionToCommaDelimitedString(allowedMethods);
	}

	/**
	 * 返回应在响应 HTTP OPTIONS 请求时使用的 "Allow" 标头值，
	 * 基于配置的 {@link #setSupportedMethods 支持的方法}，即使 "OPTIONS" 未显式列为受支持的方法，
	 * 也会自动将 "OPTIONS" 添加到列表中。这意味着只要在调用 {@link #checkRequest(HttpServletRequest)} 之前处理了 HTTP OPTIONS 请求，
	 * 子类就不必显式列出 "OPTIONS" 作为受支持的方法。
	 *
	 * @since 4.3
	 */
	@Nullable
	protected String getAllowHeader() {
		return this.allowHeader;
	}

	/**
	 * 设置是否需要会话来处理请求。
	 */
	public final void setRequireSession(boolean requireSession) {
		this.requireSession = requireSession;
	}

	/**
	 * 返回是否需要会话来处理请求。
	 */
	public final boolean isRequireSession() {
		return this.requireSession;
	}

	/**
	 * 设置用于构建 Cache-Control HTTP 响应头的 {@link org.springframework.http.CacheControl} 实例。
	 *
	 * @since 4.2
	 */
	public final void setCacheControl(@Nullable CacheControl cacheControl) {
		this.cacheControl = cacheControl;
	}

	/**
	 * 获取构建 Cache-Control HTTP 响应头的 {@link org.springframework.http.CacheControl} 实例。
	 *
	 * @since 4.2
	 */
	@Nullable
	public final CacheControl getCacheControl() {
		return this.cacheControl;
	}

	/**
	 * 缓存内容的秒数，通过向响应写入与缓存相关的 HTTP 标头：
	 * <ul>
	 * <li>seconds == -1（默认值）：不生成与缓存相关的标头</li>
	 * <li>seconds == 0： "Cache-Control: no-store" 将阻止缓存</li>
	 * <li>seconds > 0： "Cache-Control: max-age=seconds" 将要求缓存内容</li>
	 * </ul>
	 * <p>对于更具体的需求，应使用自定义的 {@link org.springframework.http.CacheControl}。
	 *
	 * @see #setCacheControl
	 */
	public final void setCacheSeconds(int seconds) {
		this.cacheSeconds = seconds;
	}

	/**
	 * 返回内容被缓存的秒数。
	 */
	public final int getCacheSeconds() {
		return this.cacheSeconds;
	}

	/**
	 * 配置一个或多个请求头名称（例如 "Accept-Language"），
	 * 以添加到 "Vary" 响应头中，以通知客户端响应受内容协商的影响，
	 * 并根据给定请求头的值变化。仅在响应 "Vary" 标头中不存在时，才会添加已配置的请求头名称。
	 *
	 * @param varyByRequestHeaders 一个或多个请求头名称
	 * @since 4.3
	 */
	public final void setVaryByRequestHeaders(@Nullable String... varyByRequestHeaders) {
		this.varyByRequestHeaders = varyByRequestHeaders;
	}

	/**
	 * 返回“Vary”响应头的配置的请求头名称。
	 *
	 * @since 4.3
	 */
	@Nullable
	public final String[] getVaryByRequestHeaders() {
		return this.varyByRequestHeaders;
	}

	/**
	 * 设置是否使用 HTTP 1.0 expires 标头。默认为“false”，自 4.2 起。
	 * <p>注意：仅当当前请求启用缓存（或明确阻止缓存）时，才会应用缓存标头。
	 *
	 * @deprecated 自 4.2 起，HTTP 1.1 cache-control 标头将是必需的，HTTP 1.0 标头将消失
	 */
	@Deprecated
	public final void setUseExpiresHeader(boolean useExpiresHeader) {
		this.useExpiresHeader = useExpiresHeader;
	}

	/**
	 * 返回是否使用 HTTP 1.0 expires 标头。
	 *
	 * @deprecated 自 4.2 起，建议使用 {@link #getCacheControl()}
	 */
	@Deprecated
	public final boolean isUseExpiresHeader() {
		return this.useExpiresHeader;
	}

	/**
	 * 设置是否使用 HTTP 1.1 cache-control 标头。默认为“true”。
	 * <p>注意：仅当当前请求启用缓存（或明确阻止缓存）时，才会应用缓存标头。
	 *
	 * @deprecated 自 4.2 起，HTTP 1.1 cache-control 标头将是必需的，HTTP 1.0 标头将消失
	 */
	@Deprecated
	public final void setUseCacheControlHeader(boolean useCacheControlHeader) {
		this.useCacheControlHeader = useCacheControlHeader;
	}

	/**
	 * 返回是否使用 HTTP 1.1 cache-control 标头。
	 *
	 * @deprecated 自 4.2 起，建议使用 {@link #getCacheControl()}
	 */
	@Deprecated
	public final boolean isUseCacheControlHeader() {
		return this.useCacheControlHeader;
	}

	/**
	 * 设置当阻止缓存时是否使用 HTTP 1.1 cache-control 标头值“no-store”。默认为“true”。
	 *
	 * @deprecated 自 4.2 起，建议使用 {@link #setCacheControl}
	 */
	@Deprecated
	public final void setUseCacheControlNoStore(boolean useCacheControlNoStore) {
		this.useCacheControlNoStore = useCacheControlNoStore;
	}

	/**
	 * 返回是否使用 HTTP 1.1 cache-control 标头值“no-store”。
	 *
	 * @deprecated 自 4.2 起，建议使用 {@link #getCacheControl()}
	 */
	@Deprecated
	public final boolean isUseCacheControlNoStore() {
		return this.useCacheControlNoStore;
	}

	/**
	 * 设置在每个 Cache-Control 标头中添加 'must-revalidate' 的选项。
	 * 这对于注解控制器方法可能很有用，这些方法可以根据描述的最后修改时间进行编程计算，如 {@link org.springframework.web.context.request.WebRequest#checkNotModified(long)}。
	 * <p>默认为“false”。
	 *
	 * @deprecated 自 4.2 起，建议使用 {@link #setCacheControl}
	 */
	@Deprecated
	public final void setAlwaysMustRevalidate(boolean mustRevalidate) {
		this.alwaysMustRevalidate = mustRevalidate;
	}

	/**
	 * 返回是否在每个 Cache-Control 标头中添加 'must-revalidate'。
	 *
	 * @deprecated 自 4.2 起，建议使用 {@link #getCacheControl()}
	 */
	@Deprecated
	public final boolean isAlwaysMustRevalidate() {
		return this.alwaysMustRevalidate;
	}


	/**
	 * 检查给定请求是否支持的方法和所需的会话（如果有）。
	 *
	 * @param request 当前 HTTP 请求
	 * @throws ServletException 如果由于检查失败而无法处理请求
	 * @since 4.2
	 */
	protected final void checkRequest(HttpServletRequest request) throws ServletException {
		// 检查我们是否应该支持请求方法。
		String method = request.getMethod();
		if (this.supportedMethods != null && !this.supportedMethods.contains(method)) {
			// 如果支持方法不为空，且支持方法集合不包含当前方法，则抛出异常
			throw new HttpRequestMethodNotSupportedException(method, this.supportedMethods);
		}

		// 检查是否需要会话。
		if (this.requireSession && request.getSession(false) == null) {
			throw new HttpSessionRequiredException("Pre-existing session required but none found");
		}
	}

	/**
	 * 根据此生成器的设置准备给定的响应。
	 * 应用此生成器指定的缓存秒数。
	 *
	 * @param response 当前 HTTP 响应
	 * @since 4.2
	 */
	protected final void prepareResponse(HttpServletResponse response) {
		if (this.cacheControl != null) {
			// 如果存在缓存控制，则应用缓存控制头
			if (logger.isTraceEnabled()) {
				logger.trace("Applying default " + getCacheControl());
			}
			applyCacheControl(response, this.cacheControl);
		} else {
			// 否则，应用缓存秒数
			if (logger.isTraceEnabled()) {
				logger.trace("Applying default cacheSeconds=" + this.cacheSeconds);
			}
			applyCacheSeconds(response, this.cacheSeconds);
		}

		if (this.varyByRequestHeaders != null) {
			// 如果需要根据请求头进行变化，则添加 Vary 响应头
			for (String value : getVaryRequestHeadersToAdd(response, this.varyByRequestHeaders)) {
				response.addHeader("Vary", value);
			}
		}
	}

	/**
	 * 根据给定的设置设置 HTTP Cache-Control 标头。
	 *
	 * @param response     当前 HTTP 响应
	 * @param cacheControl 预配置的缓存控制设置
	 * @since 4.2
	 */
	protected final void applyCacheControl(HttpServletResponse response, CacheControl cacheControl) {
		String ccValue = cacheControl.getHeaderValue();
		if (ccValue != null) {
			// 设置计算的 HTTP 1.1 Cache-Control 标头
			response.setHeader(HEADER_CACHE_CONTROL, ccValue);

			if (response.containsHeader(HEADER_PRAGMA)) {
				// 如果存在 Pramga，则重置 HTTP 1.0 Pragma 标头
				response.setHeader(HEADER_PRAGMA, "");
			}
			if (response.containsHeader(HEADER_EXPIRES)) {
				// 如果存在 Expires，则重置 HTTP 1.0 Expires 标头
				response.setHeader(HEADER_EXPIRES, "");
			}
		}
	}

	/**
	 * 应用给定的缓存秒数并生成相应的 HTTP 标头，
	 * 即如果给定正值，则允许在未来的指定秒数内缓存响应，如果给定 0 值，则防止缓存，否则不执行任何操作。
	 * 不会告诉浏览器重新验证资源。
	 *
	 * @param response     当前 HTTP 响应
	 * @param cacheSeconds 未来的正秒数，响应应该缓存，0 表示防止缓存
	 */
	@SuppressWarnings("deprecation")
	protected final void applyCacheSeconds(HttpServletResponse response, int cacheSeconds) {
		if (this.useExpiresHeader || !this.useCacheControlHeader) {
			// 如果使用 Expires 头，或者不使用 Cache-Control 头

			// 使用过时的 HTTP 1.0 缓存行为，与之前的 Spring 版本相同
			if (cacheSeconds > 0) {
				// 缓存一定的秒数
				cacheForSeconds(response, cacheSeconds);
			} else if (cacheSeconds == 0) {
				//如果缓存秒数为0，禁止缓存
				preventCaching(response);
			}
		} else {
			// 否则，使用 Cache-Control 头
			CacheControl cControl;
			if (cacheSeconds > 0) {
				// 如果缓存秒数大于 0
				cControl = CacheControl.maxAge(cacheSeconds, TimeUnit.SECONDS);
				if (this.alwaysMustRevalidate) {
					// 如果始终必须重新验证，则添加 must-revalidate 指令
					cControl = cControl.mustRevalidate();
				}
			} else if (cacheSeconds == 0) {
				// 如果缓存秒数为 0，如果使用缓存控制标头值 "no-store" ，则不存储。否则，不缓存。
				cControl = (this.useCacheControlNoStore ? CacheControl.noStore() : CacheControl.noCache());
			} else {
				// 否则，使用空的缓存控制头
				cControl = CacheControl.empty();
			}
			// 应用缓存控制头
			applyCacheControl(response, cControl);
		}
	}


	/**
	 * 检查并根据此生成器的设置准备给定的请求和响应。
	 *
	 * @see #checkRequest(HttpServletRequest)
	 * @see #prepareResponse(HttpServletResponse)
	 * @deprecated 自 4.2 起，{@code lastModified} 标志被有效忽略，只有在明确配置了 must-revalidate 标头时才会生成。
	 */
	@Deprecated
	protected final void checkAndPrepare(
			HttpServletRequest request, HttpServletResponse response, boolean lastModified) throws ServletException {
		// 检查请求
		checkRequest(request);
		// 准备响应
		prepareResponse(response);
	}

	/**
	 * 检查并根据此生成器的设置准备给定的请求和响应。
	 *
	 * @see #checkRequest(HttpServletRequest)
	 * @see #applyCacheSeconds(HttpServletResponse, int)
	 * @deprecated 自 4.2 起，{@code lastModified} 标志被有效忽略，只有在明确配置了 must-revalidate 标头时才会生成。
	 */
	@Deprecated
	protected final void checkAndPrepare(
			HttpServletRequest request, HttpServletResponse response, int cacheSeconds, boolean lastModified)
			throws ServletException {
		// 检查请求
		checkRequest(request);
		// 应用缓存秒数
		applyCacheSeconds(response, cacheSeconds);
	}

	/**
	 * 应用给定的缓存秒数并生成相应的 HTTP 标头。
	 * <p>即，如果给定正值，则在未来的指定秒数内允许缓存响应；如果给定 0 值，则防止缓存；否则不执行任何操作（即，将缓存留给客户端）。
	 *
	 * @param response       当前 HTTP 响应
	 * @param cacheSeconds   响应应该缓存的未来秒数；0 表示防止缓存；负值表示将缓存留给客户端。
	 * @param mustRevalidate 客户端是否应重新验证资源（通常仅对具有 last-modified 支持的控制器必要）
	 * @deprecated 自 4.2 起，使用 {@link #applyCacheControl}
	 */
	@Deprecated
	protected final void applyCacheSeconds(HttpServletResponse response, int cacheSeconds, boolean mustRevalidate) {
		if (cacheSeconds > 0) {
			// 如果缓存秒数大于0，则缓存一定秒数
			cacheForSeconds(response, cacheSeconds, mustRevalidate);
		} else if (cacheSeconds == 0) {
			// 如果缓存秒数为0，禁用缓存。
			preventCaching(response);
		}
	}

	/**
	 * 设置 HTTP 标头，允许在给定的秒数内进行缓存。
	 * 不会告诉浏览器重新验证资源。
	 *
	 * @param response 当前的 HTTP 响应
	 * @param seconds  响应应该被缓存的未来秒数
	 * @deprecated 自 4.2 起，使用 {@link #applyCacheControl}
	 */
	@Deprecated
	protected final void cacheForSeconds(HttpServletResponse response, int seconds) {
		cacheForSeconds(response, seconds, false);
	}

	/**
	 * 设置 HTTP 标头，允许在给定的秒数内进行缓存。
	 * 如果 mustRevalidate 为 {@code true}，则告诉浏览器重新验证资源。
	 *
	 * @param response       当前的 HTTP 响应
	 * @param seconds        响应应该被缓存的未来秒数
	 * @param mustRevalidate 客户端是否应重新验证资源（通常仅对具有 last-modified 支持的控制器必要）
	 * @deprecated 自 4.2 起，使用 {@link #applyCacheControl}
	 */
	@Deprecated
	protected final void cacheForSeconds(HttpServletResponse response, int seconds, boolean mustRevalidate) {
		if (this.useExpiresHeader) {
			// 如果使用 Expires 头
			// 设置 HTTP 1.0 Expires 标头
			response.setDateHeader(HEADER_EXPIRES, System.currentTimeMillis() + seconds * 1000L);
		} else if (response.containsHeader(HEADER_EXPIRES)) {
			// 否则，如果存在 Expires 标头，则重置
			response.setHeader(HEADER_EXPIRES, "");
		}

		if (this.useCacheControlHeader) {
			// 如果使用 Cache-Control 头
			// 设置 HTTP 1.1 Cache-Control 标头
			String headerValue = "max-age=" + seconds;
			if (mustRevalidate || this.alwaysMustRevalidate) {
				// 如果必须重新验证，则添加 must-revalidate 指令
				headerValue += ", must-revalidate";
			}
			response.setHeader(HEADER_CACHE_CONTROL, headerValue);
		}

		if (response.containsHeader(HEADER_PRAGMA)) {
			// 如果存在 Pragma 标头，则重置
			response.setHeader(HEADER_PRAGMA, "");
		}
	}

	/**
	 * 防止响应被缓存。
	 * 仅在 HTTP 1.0 兼容模式下调用。
	 * <p>参见 {@code https://www.mnot.net/cache_docs}。
	 *
	 * @deprecated 自 4.2 起，使用 {@link #applyCacheControl}
	 */
	@Deprecated
	protected final void preventCaching(HttpServletResponse response) {
		// 设置响应头 Pragma 值为 no-cache
		response.setHeader(HEADER_PRAGMA, "no-cache");

		if (this.useExpiresHeader) {
			// 如果使用 Expires 头
			// 设置 HTTP 1.0 Expires 标头，将其设置为过去的时间，强制客户端不缓存
			response.setDateHeader(HEADER_EXPIRES, 1L);
		}

		if (this.useCacheControlHeader) {
			// 如果使用 Cache-Control 头
			// 设置 HTTP 1.1 Cache-Control 标头
			// "no-cache" 是标准值，用于强制缓存服务器重新验证缓存
			response.setHeader(HEADER_CACHE_CONTROL, "no-cache");
			if (this.useCacheControlNoStore) {
				// 如果需要不存储缓存，则添加 "no-store" 指令
				response.addHeader(HEADER_CACHE_CONTROL, "no-store");
			}
		}
	}


	private Collection<String> getVaryRequestHeadersToAdd(HttpServletResponse response, String[] varyByRequestHeaders) {
		if (!response.containsHeader(HttpHeaders.VARY)) {
			// 如果响应头中不包含 Vary 头，则返回 varyByRequestHeaders 的列表
			return Arrays.asList(varyByRequestHeaders);
		}

		// 从响应头中解析 Vary 头的值
		Collection<String> result = new ArrayList<>(varyByRequestHeaders.length);
		Collections.addAll(result, varyByRequestHeaders);
		for (String header : response.getHeaders(HttpHeaders.VARY)) {
			// 遍历响应头中的 Vary 头值
			for (String existing : StringUtils.tokenizeToStringArray(header, ",")) {
				if ("*".equals(existing)) {
					// 如果存在通配符 "*"，表示响应可能根据请求的任意头来变化，因此返回空列表
					return Collections.emptyList();
				}
				// 检查响应头中的 Vary 头值是否在 varyByRequestHeaders 中，如果在则移除
				for (String value : varyByRequestHeaders) {
					if (value.equalsIgnoreCase(existing)) {
						result.remove(value);
					}
				}
			}
		}
		return result;
	}

}
