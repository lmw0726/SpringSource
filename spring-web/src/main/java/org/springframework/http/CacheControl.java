/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 用于创建 "Cache-Control" HTTP 响应头的构建器。
 *
 * <p>将 Cache-Control 指令添加到 HTTP 响应中可以显著改善客户端与 Web 应用程序交互时的体验。
 * 此构建器创建具有响应指令的 "Cache-Control" 头，考虑了多种使用情况。
 *
 * <ul>
 * <li>使用 {@code CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS)} 缓存 HTTP 响应
 * 将产生 {@code Cache-Control: "max-age=3600"}</li>
 * <li>使用 {@code CacheControl cc = CacheControl.noStore()} 防止缓存
 * 将产生 {@code Cache-Control: "no-store"}</li>
 * <li>高级情况如 {@code CacheControl cc = CacheControl.maxAge(1, TimeUnit.HOURS).noTransform().cachePublic()}
 * 将产生 {@code Cache-Control: "max-age=3600, no-transform, public"}</li>
 * </ul>
 *
 * <p>注意，为了提高效率，Cache-Control 头应与 "Last-Modified" 或 "ETag" 头等 HTTP 验证器一起编写。
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2">rfc7234 第5.2.2节</a>
 * @see <a href="https://developers.google.com/web/fundamentals/performance/optimizing-content-efficiency/http-caching">
 * HTTP 缓存 - Google 开发者参考</a>
 * @see <a href="https://www.mnot.net/cache_docs/">Mark Nottingham 的缓存文档</a>
 * @since 4.2
 */
public class CacheControl {

	/**
	 * 资源可以缓存的最长时间。
	 */
	@Nullable
	private Duration maxAge;

	/**
	 * 资源是否不可以缓存。
	 */
	private boolean noCache = false;

	/**
	 * 资源是否不应被缓存。
	 */
	private boolean noStore = false;

	/**
	 * 缓存是否必须重新验证过期资源的有效性。
	 */
	private boolean mustRevalidate = false;

	/**
	 * 缓存是否不应转换资源的表示形式。
	 */
	private boolean noTransform = false;

	/**
	 * 资源是否可以被任何缓存存储。
	 */
	private boolean cachePublic = false;

	/**
	 * 资源是否只能被私有缓存存储。
	 */
	private boolean cachePrivate = false;

	/**
	 * 代理是否必须重新验证过期资源的有效性。
	 */
	private boolean proxyRevalidate = false;

	/**
	 * 资源在重新验证期间的过期时间。
	 */
	@Nullable
	private Duration staleWhileRevalidate;

	/**
	 * 资源在发生错误时的过期时间。
	 */
	@Nullable
	private Duration staleIfError;

	/**
	 * 共享缓存的最大存储时间。
	 */
	@Nullable
	private Duration sMaxAge;


	/**
	 * 创建一个空的 CacheControl 实例。
	 *
	 * @see #empty()
	 */
	protected CacheControl() {
	}


	/**
	 * 返回一个空指令。
	 * <p>这非常适合使用没有 "max-age"、"no-cache" 或 "no-store" 的其他可选指令。
	 *
	 * @return {@code this}, 以便于方法链调用
	 */
	public static CacheControl empty() {
		return new CacheControl();
	}

	/**
	 * 添加 "max-age=" 指令。
	 * <p>此指令非常适合公共缓存资源，知道它们在配置的时间内不会更改。还可以使用其他指令，
	 * 以防资源不应由共享缓存缓存 ({@link #cachePrivate()}) 或转换 ({@link #noTransform()})。
	 * <p>为了防止缓存即使在响应已过期（即 "max-age" 延迟已过）时仍然使用缓存响应，
	 * 应设置 "must-revalidate" 指令 ({@link #mustRevalidate()})。
	 *
	 * @param maxAge 响应应缓存的最大时间
	 * @param unit   {@code maxAge} 参数的时间单位
	 * @return {@code this}, 以便于方法链调用
	 * @see #maxAge(Duration)
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.8">rfc7234 第5.2.2.8节</a>
	 */
	public static CacheControl maxAge(long maxAge, TimeUnit unit) {
		return maxAge(Duration.ofSeconds(unit.toSeconds(maxAge)));
	}

	/**
	 * 添加 "max-age=" 指令。
	 * <p>此指令非常适合公共缓存资源，知道它们在配置的时间内不会更改。还可以使用其他指令，
	 * 以防资源不应由共享缓存缓存 ({@link #cachePrivate()}) 或转换 ({@link #noTransform()})。
	 * <p>为了防止缓存即使在响应已过期（即 "max-age" 延迟已过）时仍然使用缓存响应，
	 * 应设置 "must-revalidate" 指令 ({@link #mustRevalidate()})。
	 *
	 * @param maxAge 响应应缓存的最大时间
	 * @return {@code this}, 以便于方法链调用
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.8">rfc7234 第5.2.2.8节</a>
	 * @since 5.2
	 */
	public static CacheControl maxAge(Duration maxAge) {
		CacheControl cc = new CacheControl();
		cc.maxAge = maxAge;
		return cc;
	}

	/**
	 * 添加一个 "no-cache" 指令。
	 * <p>该指令适用于告知缓存系统响应只能在客户端重新验证服务器后才能重用。
	 * 该指令不会完全禁用缓存，并且可能导致客户端发送条件请求（带有 "ETag" 和 "If-Modified-Since" 头部），
	 * 服务器则响应 "304 - Not Modified" 状态。
	 * <p>为了禁用缓存并最小化请求/响应交换，应该使用 {@link #noStore()} 指令而不是 {@code #noCache()}。
	 *
	 * @return {@code this}，以便进行方法链调用
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.2">rfc7234 第5.2.2.2节</a>
	 */
	public static CacheControl noCache() {
		CacheControl cc = new CacheControl();
		cc.noCache = true;
		return cc;
	}

	/**
	 * 添加一个 "no-store" 指令。
	 * <p>该指令适用于防止缓存（浏览器和代理服务器）缓存响应内容。
	 *
	 * @return {@code this}，以便进行方法链调用
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.3">rfc7234 第5.2.2.3节</a>
	 */
	public static CacheControl noStore() {
		CacheControl cc = new CacheControl();
		cc.noStore = true;
		return cc;
	}


	/**
	 * 添加一个 "must-revalidate" 指令。
	 * <p>该指令指示缓存一旦变为陈旧，则必须在没有成功验证原始服务器的情况下，
	 * 不得使用该响应来满足后续请求。
	 *
	 * @return {@code this}，以便进行方法链调用
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.1">rfc7234 第5.2.2.1节</a>
	 */
	public CacheControl mustRevalidate() {
		this.mustRevalidate = true;
		return this;
	}

	/**
	 * 添加一个 "no-transform" 指令。
	 * <p>该指令指示中间设备（缓存和其他设备）不应转换响应内容。
	 * 这对于强制缓存和CDN不自动压缩或优化响应内容非常有用。
	 *
	 * @return {@code this}，以便进行方法链调用
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.4">rfc7234 第5.2.2.4节</a>
	 */
	public CacheControl noTransform() {
		this.noTransform = true;
		return this;
	}

	/**
	 * 添加一个 "public" 指令。
	 * <p>该指令指示任何缓存系统可以存储响应，即使该响应通常是不可缓存的或仅在私有缓存中可缓存。
	 *
	 * @return {@code this}，以便进行方法链调用
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.5">rfc7234 第5.2.2.5节</a>
	 */
	public CacheControl cachePublic() {
		this.cachePublic = true;
		return this;
	}

	/**
	 * 添加一个 "private" 指令。
	 * <p>该指令指示响应消息是为单个用户准备的，不得被共享缓存存储。
	 *
	 * @return {@code this}，以便进行方法链调用
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.6">rfc7234 第5.2.2.6节</a>
	 */
	public CacheControl cachePrivate() {
		this.cachePrivate = true;
		return this;
	}

	/**
	 * 添加一个 "proxy-revalidate" 指令。
	 * <p>该指令具有与 "must-revalidate" 指令相同的意义，但它不适用于私有缓存（例如浏览器，HTTP客户端）。
	 *
	 * @return {@code this}，以便进行方法链调用
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.7">rfc7234 第5.2.2.7节</a>
	 */
	public CacheControl proxyRevalidate() {
		this.proxyRevalidate = true;
		return this;
	}

	/**
	 * 添加一个 "s-maxage" 指令。
	 * <p>该指令指示在共享缓存中，该指令指定的最大年龄将覆盖其他指令指定的最大年龄。
	 *
	 * @param sMaxAge 响应应该被缓存的最长时间
	 * @param unit    {@code sMaxAge} 参数的时间单位
	 * @return {@code this}，以便进行方法链调用
	 * @see #sMaxAge(Duration)
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.9">rfc7234 第5.2.2.9节</a>
	 */
	public CacheControl sMaxAge(long sMaxAge, TimeUnit unit) {
		return sMaxAge(Duration.ofSeconds(unit.toSeconds(sMaxAge)));
	}

	/**
	 * 添加一个 "s-maxage" 指令。
	 * <p>该指令指示在共享缓存中，该指令指定的最大年龄将覆盖其他指令指定的最大年龄。
	 *
	 * @param sMaxAge 响应应该被缓存的最长时间
	 * @return {@code this}，以便进行方法链调用
	 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.2.9">rfc7234 第5.2.2.9节</a>
	 * @since 5.2
	 */
	public CacheControl sMaxAge(Duration sMaxAge) {
		this.sMaxAge = sMaxAge;
		return this;
	}

	/**
	 * 添加一个 "stale-while-revalidate" 指令。
	 * <p>该指令指示缓存系统可以在响应变为陈旧后，在指示的秒数内继续提供该响应。
	 * 如果由于该扩展存在而提供了过时的缓存响应，缓存应尝试在继续提供过时响应的同时进行重新验证（即不阻塞）。
	 *
	 * @param staleWhileRevalidate 响应在重新验证期间可以使用的最长时间
	 * @param unit                 {@code staleWhileRevalidate} 参数的时间单位
	 * @return {@code this}，以便进行方法链调用
	 * @see #staleWhileRevalidate(Duration)
	 * @see <a href="https://tools.ietf.org/html/rfc5861#section-3">rfc5861 第3节</a>
	 */
	public CacheControl staleWhileRevalidate(long staleWhileRevalidate, TimeUnit unit) {
		return staleWhileRevalidate(Duration.ofSeconds(unit.toSeconds(staleWhileRevalidate)));
	}

	/**
	 * 添加一个 "stale-while-revalidate" 指令。
	 * <p>该指令指示缓存系统可以在响应变为陈旧后，在指示的秒数内继续提供该响应。
	 * 如果由于该扩展存在而提供了过时的缓存响应，缓存应尝试在继续提供过时响应的同时进行重新验证（即不阻塞）。
	 *
	 * @param staleWhileRevalidate 响应在重新验证期间可以使用的最长时间
	 * @return {@code this}，以便进行方法链调用
	 * @see <a href="https://tools.ietf.org/html/rfc5861#section-3">rfc5861 第3节</a>
	 * @since 5.2
	 */
	public CacheControl staleWhileRevalidate(Duration staleWhileRevalidate) {
		this.staleWhileRevalidate = staleWhileRevalidate;
		return this;
	}

	/**
	 * 添加一个 "stale-if-error" 指令。
	 * <p>该指令指示当遇到错误时，可以使用缓存的陈旧响应来满足请求，而不考虑其他新鲜度信息。
	 *
	 * @param staleIfError 在遇到错误时可以使用响应的最长时间
	 * @param unit         {@code staleIfError} 参数的时间单位
	 * @return {@code this}，以便进行方法链调用
	 * @see #staleIfError(Duration)
	 * @see <a href="https://tools.ietf.org/html/rfc5861#section-4">rfc5861 第4节</a>
	 */
	public CacheControl staleIfError(long staleIfError, TimeUnit unit) {
		return staleIfError(Duration.ofSeconds(unit.toSeconds(staleIfError)));
	}

	/**
	 * 添加一个 "stale-if-error" 指令。
	 * <p>该指令指示当遇到错误时，可以使用缓存的陈旧响应来满足请求，而不考虑其他新鲜度信息。
	 *
	 * @param staleIfError 在遇到错误时可以使用响应的最长时间
	 * @return {@code this}，以便进行方法链调用
	 * @see <a href="https://tools.ietf.org/html/rfc5861#section-4">rfc5861 第4节</a>
	 * @since 5.2
	 */
	public CacheControl staleIfError(Duration staleIfError) {
		this.staleIfError = staleIfError;
		return this;
	}

	/**
	 * 返回 "Cache-Control" 头的值（如果有）。
	 *
	 * @return 头部的值，或者如果没有添加指令则返回 {@code null}
	 */
	@Nullable
	public String getHeaderValue() {
		// 转换为头部值
		String headerValue = toHeaderValue();
		// 如果头部值存在，则返回该头部值。否则返回null。
		return (StringUtils.hasText(headerValue) ? headerValue : null);
	}

	/**
	 * 返回 "Cache-Control" 头的值。
	 *
	 * @return 头部的值（可能为空）
	 */
	private String toHeaderValue() {
		// 头部值构建器
		StringBuilder headerValue = new StringBuilder();
		if (this.maxAge != null) {
			// 如果资源可以缓存的最长时间不为空，添加 max-age 指令
			appendDirective(headerValue, "max-age=" + this.maxAge.getSeconds());
		}
		if (this.noCache) {
			// 如果资源不能缓存，添加 no-cache 指令
			appendDirective(headerValue, "no-cache");
		}
		if (this.noStore) {
			// 如果资源不能存储，添加 no-store 指令
			appendDirective(headerValue, "no-store");
		}
		if (this.mustRevalidate) {
			// 如果资源必须重新验证，添加 must-revalidate 指令
			appendDirective(headerValue, "must-revalidate");
		}
		if (this.noTransform) {
			// 如果资源不能转换，添加 no-transform 指令
			appendDirective(headerValue, "no-transform");
		}
		if (this.cachePublic) {
			// 如果资源可以公开缓存，添加 public 指令
			appendDirective(headerValue, "public");
		}
		if (this.cachePrivate) {
			// 如果资源只能被私有缓存存储，添加 private 指令
			appendDirective(headerValue, "private");
		}
		if (this.proxyRevalidate) {
			// 如果代理必须重新验证过期资源的有效性，添加 proxy-revalidate 指令
			appendDirective(headerValue, "proxy-revalidate");
		}
		if (this.sMaxAge != null) {
			// 如果共享缓存的最大存储时间不为空，添加 s-maxage 指令
			appendDirective(headerValue, "s-maxage=" + this.sMaxAge.getSeconds());
		}
		if (this.staleIfError != null) {
			// 如果资源在发生错误时的过期时间不为空，添加 stale-if-error 指令
			appendDirective(headerValue, "stale-if-error=" + this.staleIfError.getSeconds());
		}
		if (this.staleWhileRevalidate != null) {
			// 如果资源在重新验证期间的过期时间不为空，添加 stale-while-revalidate 指令
			appendDirective(headerValue, "stale-while-revalidate=" + this.staleWhileRevalidate.getSeconds());
		}
		return headerValue.toString();
	}

	private void appendDirective(StringBuilder builder, String value) {
		if (builder.length() > 0) {
			// 如果builder不为空，则添加","
			builder.append(", ");
		}
		builder.append(value);
	}


	@Override
	public String toString() {
		return "CacheControl [" + toHeaderValue() + "]";
	}

}
