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

package org.springframework.mock.http.server.reactive;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.*;
import org.springframework.http.server.reactive.AbstractServerHttpRequest;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 用于测试中的 {@link AbstractServerHttpRequest} 的模拟扩展，不需要实际的服务器。使用静态方法来获取构建器。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class MockServerHttpRequest extends AbstractServerHttpRequest {

	/**
	 * HTTP 方法的字符串表示，可以是 {@link HttpMethod} 的一种，或者是非空的自定义方法（例如 <i>CONNECT</i>）。
	 */
	private final String httpMethod;

	/**
	 * HTTP 请求中包含的 Cookie 的多值映射。
	 */
	private final MultiValueMap<String, HttpCookie> cookies;

	/**
	 * 本地地址，如果可用。
	 */
	@Nullable
	private final InetSocketAddress localAddress;

	/**
	 * 远程地址，如果可用。
	 */
	@Nullable
	private final InetSocketAddress remoteAddress;

	/**
	 * SSL 相关信息，如果请求通过安全协议传输。
	 */
	@Nullable
	private final SslInfo sslInfo;

	/**
	 * 请求体的数据流。
	 */
	private final Flux<DataBuffer> body;

	private MockServerHttpRequest(String httpMethod,
								  URI uri, @Nullable String contextPath, HttpHeaders headers, MultiValueMap<String, HttpCookie> cookies,
								  @Nullable InetSocketAddress localAddress, @Nullable InetSocketAddress remoteAddress,
								  @Nullable SslInfo sslInfo, Publisher<? extends DataBuffer> body) {

		super(uri, contextPath, headers);
		Assert.isTrue(StringUtils.hasText(httpMethod), "HTTP method is required.");
		this.httpMethod = httpMethod;
		this.cookies = cookies;
		this.localAddress = localAddress;
		this.remoteAddress = remoteAddress;
		this.sslInfo = sslInfo;
		this.body = Flux.from(body);
	}


	@Override
	@Nullable
	public HttpMethod getMethod() {
		return HttpMethod.resolve(this.httpMethod);
	}

	@Override
	public String getMethodValue() {
		return this.httpMethod;
	}

	@Override
	@Nullable
	public InetSocketAddress getLocalAddress() {
		return this.localAddress;
	}

	@Override
	@Nullable
	public InetSocketAddress getRemoteAddress() {
		return this.remoteAddress;
	}

	@Override
	@Nullable
	protected SslInfo initSslInfo() {
		return this.sslInfo;
	}

	@Override
	public Flux<DataBuffer> getBody() {
		return this.body;
	}

	@Override
	protected MultiValueMap<String, HttpCookie> initCookies() {
		return this.cookies;
	}

	@Override
	public <T> T getNativeRequest() {
		throw new IllegalStateException("This is a mock. No running server, no native request.");
	}


// 静态构建器方法

	/**
	 * 创建一个使用指定 URI 模板的 HTTP GET 请求构建器。
	 * 给定的 URI 可能包含查询参数，或者可以稍后通过 {@link BaseBuilder#queryParam queryParam} 构建器方法添加。
	 *
	 * @param urlTemplate URL 模板；生成的 URL 将被编码
	 * @param uriVars     零个或多个 URI 变量
	 * @return 创建的构建器
	 */
	public static BaseBuilder<?> get(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.GET, urlTemplate, uriVars);
	}

	/**
	 * HTTP HEAD 请求的变体。详见 {@link #get(String, Object...)} 获取一般信息。
	 *
	 * @param urlTemplate URL 模板；生成的 URL 将被编码
	 * @param uriVars     零个或多个 URI 变量
	 * @return 创建的构建器
	 */
	public static BaseBuilder<?> head(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.HEAD, urlTemplate, uriVars);
	}

	/**
	 * HTTP POST 请求的变体。详见 {@link #get(String, Object...)} 获取一般信息。
	 *
	 * @param urlTemplate URL 模板；生成的 URL 将被编码
	 * @param uriVars     零个或多个 URI 变量
	 * @return 创建的构建器
	 */
	public static BodyBuilder post(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.POST, urlTemplate, uriVars);
	}

	/**
	 * HTTP PUT 请求的变体。详见 {@link #get(String, Object...)} 获取一般信息。
	 *
	 * @param urlTemplate URL 模板；生成的 URL 将被编码
	 * @param uriVars     零个或多个 URI 变量
	 * @return 创建的构建器
	 */
	public static BodyBuilder put(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.PUT, urlTemplate, uriVars);
	}

	/**
	 * HTTP PATCH 请求的变体。详见 {@link #get(String, Object...)} 获取一般信息。
	 *
	 * @param urlTemplate URL 模板；生成的 URL 将被编码
	 * @param uriVars     零个或多个 URI 变量
	 * @return 创建的构建器
	 */
	public static BodyBuilder patch(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.PATCH, urlTemplate, uriVars);
	}

	/**
	 * HTTP DELETE 请求的变体。详见 {@link #get(String, Object...)} 获取一般信息。
	 *
	 * @param urlTemplate URL 模板；生成的 URL 将被编码
	 * @param uriVars     零个或多个 URI 变量
	 * @return 创建的构建器
	 */
	public static BaseBuilder<?> delete(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.DELETE, urlTemplate, uriVars);
	}

	/**
	 * HTTP OPTIONS 请求的变体。详见 {@link #get(String, Object...)} 获取一般信息。
	 *
	 * @param urlTemplate URL 模板；生成的 URL 将被编码
	 * @param uriVars     零个或多个 URI 变量
	 * @return 创建的构建器
	 */
	public static BaseBuilder<?> options(String urlTemplate, Object... uriVars) {
		return method(HttpMethod.OPTIONS, urlTemplate, uriVars);
	}

	/**
	 * 使用给定的 HTTP 方法和 {@link URI} 创建一个构建器。
	 *
	 * @param method HTTP 方法 (GET, POST 等)
	 * @param url    URL
	 * @return 创建的构建器
	 */
	public static BodyBuilder method(HttpMethod method, URI url) {
		Assert.notNull(method, "HTTP method is required. " +
				"For a custom HTTP method, please provide a String HTTP method value.");
		return new DefaultBodyBuilder(method.name(), url);
	}

	/**
	 * {@link #method(HttpMethod, URI)} 的替代方法，接受 URI 模板。
	 * 给定的 URI 可能包含查询参数，或者可以稍后通过 {@link BaseBuilder#queryParam queryParam} 构建器方法添加。
	 *
	 * @param method HTTP 方法 (GET, POST 等)
	 * @param uri    目标 URL 的 URI 模板
	 * @param vars   要扩展到模板中的变量
	 * @return 创建的构建器
	 */
	public static BodyBuilder method(HttpMethod method, String uri, Object... vars) {
		return method(method, toUri(uri, vars));
	}

	/**
	 * 创建一个具有原始 HTTP 方法值的构建器，该值超出了 {@link HttpMethod} 枚举值的范围。
	 *
	 * @param httpMethod HTTP 方法值
	 * @param uri        目标 URL 的 URI 模板
	 * @param vars       要扩展到模板中的变量
	 * @return 创建的构建器
	 * @since 5.2.7
	 */
	public static BodyBuilder method(String httpMethod, String uri, Object... vars) {
		return new DefaultBodyBuilder(httpMethod, toUri(uri, vars));
	}

	/**
	 * 将 URI 字符串和变量转换为 URI。
	 *
	 * @param uri  URI 字符串
	 * @param vars 要扩展到 URI 模板中的变量
	 * @return 转换后的 URI
	 */
	private static URI toUri(String uri, Object[] vars) {
		return UriComponentsBuilder.fromUriString(uri).buildAndExpand(vars).encode().toUri();
	}


	/**
	 * 请求构建器，公开与主体无关的属性。
	 *
	 * @param <B> 构建器子类
	 */
	public interface BaseBuilder<B extends BaseBuilder<B>> {

		/**
		 * 设置要返回的 contextPath。
		 *
		 * @param contextPath 上下文路径
		 * @return 构建器实例
		 */
		B contextPath(String contextPath);

		/**
		 * 将给定的查询参数追加到现有的查询参数中。
		 * 如果没有提供值，则生成的 URI 只包含查询参数名
		 * （即 {@code ?foo} 而不是 {@code ?foo=bar}）。
		 * <p>提供的查询名称和值将被编码。
		 *
		 * @param name   查询参数名
		 * @param values 查询参数值
		 * @return 构建器实例
		 */
		B queryParam(String name, Object... values);

		/**
		 * 添加给定的查询参数和值。提供的查询名称和对应的值将被编码。
		 *
		 * @param params 查询参数
		 * @return 构建器实例
		 */
		B queryParams(MultiValueMap<String, String> params);

		/**
		 * 设置要返回的远程地址。
		 *
		 * @param remoteAddress 远程地址
		 * @return 构建器实例
		 */
		B remoteAddress(InetSocketAddress remoteAddress);

		/**
		 * 设置要返回的本地地址。
		 *
		 * @param localAddress 本地地址
		 * @return 构建器实例
		 * @since 5.2.3
		 */
		B localAddress(InetSocketAddress localAddress);

		/**
		 * 设置 SSL 会话信息和证书。
		 *
		 * @param sslInfo SSL 信息
		 */
		void sslInfo(SslInfo sslInfo);

		/**
		 * 添加一个或多个 cookie。
		 *
		 * @param cookie cookie 实例
		 * @return 构建器实例
		 */
		B cookie(HttpCookie... cookie);

		/**
		 * 添加给定的 cookie。
		 *
		 * @param cookies cookie 集合
		 * @return 构建器实例
		 */
		B cookies(MultiValueMap<String, HttpCookie> cookies);

		/**
		 * 在给定名称下添加单个 header 值。
		 *
		 * @param headerName   header 名称
		 * @param headerValues header 值
		 * @return 构建器实例
		 * @see HttpHeaders#add(String, String)
		 */
		B header(String headerName, String... headerValues);

		/**
		 * 添加给定的 header 值。
		 *
		 * @param headers header 集合
		 * @return 构建器实例
		 */
		B headers(MultiValueMap<String, String> headers);

		/**
		 * 设置可接受的 {@linkplain MediaType 媒体类型} 列表，指定在 {@code Accept} 头中。
		 *
		 * @param acceptableMediaTypes 可接受的媒体类型
		 * @return 构建器实例
		 */
		B accept(MediaType... acceptableMediaTypes);

		/**
		 * 设置可接受的 {@linkplain Charset 字符集} 列表，指定在 {@code Accept-Charset} 头中。
		 *
		 * @param acceptableCharsets 可接受的字符集
		 * @return 构建器实例
		 */
		B acceptCharset(Charset... acceptableCharsets);

		/**
		 * 设置可接受的 {@linkplain Locale 语言环境} 列表，指定在 {@code Accept-Languages} 头中。
		 *
		 * @param acceptableLocales 可接受的语言环境
		 * @return 构建器实例
		 */
		B acceptLanguageAsLocales(Locale... acceptableLocales);

		/**
		 * 设置 {@code If-Modified-Since} 头的值。
		 * <p>日期应指定为自 1970 年 1 月 1 日 GMT 以来的毫秒数。
		 *
		 * @param ifModifiedSince 头的新值
		 * @return 构建器实例
		 */
		B ifModifiedSince(long ifModifiedSince);

		/**
		 * 设置 {@code If-Unmodified-Since} 头的值。
		 * <p>日期应指定为自 1970 年 1 月 1 日 GMT 以来的毫秒数。
		 *
		 * @param ifUnmodifiedSince 头的新值
		 * @return 构建器实例
		 * @see HttpHeaders#setIfUnmodifiedSince(long)
		 */
		B ifUnmodifiedSince(long ifUnmodifiedSince);

		/**
		 * 设置 {@code If-None-Match} 头的值。
		 *
		 * @param ifNoneMatches 头的新值
		 * @return 构建器实例
		 */
		B ifNoneMatch(String... ifNoneMatches);

		/**
		 * 设置 Range 头的新值。
		 *
		 * @param ranges HTTP 范围
		 * @return 构建器实例
		 * @see HttpHeaders#setRange(List)
		 */
		B range(HttpRange... ranges);

		/**
		 * 构建不带主体的请求。
		 *
		 * @return 请求
		 * @see BodyBuilder#body(Publisher)
		 * @see BodyBuilder#body(String)
		 */
		MockServerHttpRequest build();
	}


	/**
	 * 一个为请求添加主体的构建器。
	 */
	public interface BodyBuilder extends BaseBuilder<BodyBuilder> {

		/**
		 * 设置主体的字节长度，如 {@code Content-Length} 头所指定的。
		 *
		 * @param contentLength 主体长度
		 * @return 构建器实例
		 * @see HttpHeaders#setContentLength(long)
		 */
		BodyBuilder contentLength(long contentLength);

		/**
		 * 设置主体的 {@linkplain MediaType 媒体类型}，如 {@code Content-Type} 头所指定的。
		 *
		 * @param contentType 内容类型
		 * @return 构建器实例
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		BodyBuilder contentType(MediaType contentType);

		/**
		 * 设置请求的主体并构建它。
		 *
		 * @param body 主体
		 * @return 构建的请求实体
		 */
		MockServerHttpRequest body(Publisher<? extends DataBuffer> body);

		/**
		 * 设置请求的主体并构建它。
		 * <p>除非请求有带有字符集属性的 "content-type" 头，否则该字符串被假定为 UTF-8 编码。
		 *
		 * @param body 文本形式的主体
		 * @return 构建的请求实体
		 */
		MockServerHttpRequest body(String body);
	}


	private static class DefaultBodyBuilder implements BodyBuilder {

		/**
		 * HTTP 方法的字符串表示。
		 */
		private final String methodValue;

		/**
		 * 请求的 URI。
		 */
		private final URI url;

		/**
		 * 上下文路径。
		 */
		@Nullable
		private String contextPath;

		/**
		 * 用于构建查询参数的 UriComponentsBuilder 实例。
		 */
		private final UriComponentsBuilder queryParamsBuilder = UriComponentsBuilder.newInstance();

		/**
		 * 请求头的 HttpHeaders 实例。
		 */
		private final HttpHeaders headers = new HttpHeaders();

		/**
		 * 请求中的 HTTP cookie 的 MultiValueMap 实例。
		 */
		private final MultiValueMap<String, HttpCookie> cookies = new LinkedMultiValueMap<>();

		/**
		 * 远程地址。
		 */
		@Nullable
		private InetSocketAddress remoteAddress;

		/**
		 * 本地地址。
		 */
		@Nullable
		private InetSocketAddress localAddress;

		/**
		 * SSL 会话信息。
		 */
		@Nullable
		private SslInfo sslInfo;

		DefaultBodyBuilder(String method, URI url) {
			this.methodValue = method;
			this.url = url;
		}

		@Override
		public BodyBuilder contextPath(String contextPath) {
			this.contextPath = contextPath;
			return this;
		}

		@Override
		public BodyBuilder queryParam(String name, Object... values) {
			this.queryParamsBuilder.queryParam(name, values);
			return this;
		}

		@Override
		public BodyBuilder queryParams(MultiValueMap<String, String> params) {
			this.queryParamsBuilder.queryParams(params);
			return this;
		}

		@Override
		public BodyBuilder remoteAddress(InetSocketAddress remoteAddress) {
			this.remoteAddress = remoteAddress;
			return this;
		}

		@Override
		public BodyBuilder localAddress(InetSocketAddress localAddress) {
			this.localAddress = localAddress;
			return this;
		}

		@Override
		public void sslInfo(SslInfo sslInfo) {
			this.sslInfo = sslInfo;
		}

		@Override
		public BodyBuilder cookie(HttpCookie... cookies) {
			Arrays.stream(cookies).forEach(cookie -> this.cookies.add(cookie.getName(), cookie));
			return this;
		}

		@Override
		public BodyBuilder cookies(MultiValueMap<String, HttpCookie> cookies) {
			this.cookies.putAll(cookies);
			return this;
		}

		@Override
		public BodyBuilder header(String headerName, String... headerValues) {
			// 遍历所有的头部值
			for (String headerValue : headerValues) {
				// 将每个头部值添加到指定的头部名称中
				this.headers.add(headerName, headerValue);
			}

			// 返回当前对象
			return this;
		}

		@Override
		public BodyBuilder headers(MultiValueMap<String, String> headers) {
			this.headers.putAll(headers);
			return this;
		}

		@Override
		public BodyBuilder accept(MediaType... acceptableMediaTypes) {
			this.headers.setAccept(Arrays.asList(acceptableMediaTypes));
			return this;
		}

		@Override
		public BodyBuilder acceptCharset(Charset... acceptableCharsets) {
			this.headers.setAcceptCharset(Arrays.asList(acceptableCharsets));
			return this;
		}

		@Override
		public BodyBuilder acceptLanguageAsLocales(Locale... acceptableLocales) {
			this.headers.setAcceptLanguageAsLocales(Arrays.asList(acceptableLocales));
			return this;
		}

		@Override
		public BodyBuilder contentLength(long contentLength) {
			this.headers.setContentLength(contentLength);
			return this;
		}

		@Override
		public BodyBuilder contentType(MediaType contentType) {
			this.headers.setContentType(contentType);
			return this;
		}

		@Override
		public BodyBuilder ifModifiedSince(long ifModifiedSince) {
			this.headers.setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifUnmodifiedSince(long ifUnmodifiedSince) {
			this.headers.setIfUnmodifiedSince(ifUnmodifiedSince);
			return this;
		}

		@Override
		public BodyBuilder ifNoneMatch(String... ifNoneMatches) {
			this.headers.setIfNoneMatch(Arrays.asList(ifNoneMatches));
			return this;
		}

		@Override
		public BodyBuilder range(HttpRange... ranges) {
			this.headers.setRange(Arrays.asList(ranges));
			return this;
		}

		@Override
		public MockServerHttpRequest build() {
			return body(Flux.empty());
		}

		@Override
		public MockServerHttpRequest body(String body) {
			// 将字符串body转换为字节数组，使用指定的字符集
			byte[] bytes = body.getBytes(getCharset());

			// 使用默认的数据缓冲区工厂，将字节数组包装成数据缓冲区
			DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.wrap(bytes);

			// 返回一个包含数据缓冲区的Flux流
			return body(Flux.just(buffer));
		}

		private Charset getCharset() {
			return Optional.ofNullable(this.headers.getContentType())
					.map(MimeType::getCharset).orElse(StandardCharsets.UTF_8);
		}

		@Override
		public MockServerHttpRequest body(Publisher<? extends DataBuffer> body) {
			// 如果需要的话，应用Cookies
			applyCookiesIfNecessary();

			// 返回一个新的MockServerHttpRequest对象，传入相应的参数
			return new MockServerHttpRequest(this.methodValue, getUrlToUse(), this.contextPath,
					this.headers, this.cookies, this.localAddress, this.remoteAddress, this.sslInfo, body);
		}

		private void applyCookiesIfNecessary() {
			// 如果HTTP头部中不存在COOKIE头部
			if (this.headers.get(HttpHeaders.COOKIE) == null) {
				// 遍历cookies集合中的所有Cookie
				this.cookies.values().stream()
						// 展平集合，得到每个单独的Cookie
						.flatMap(Collection::stream)
						// 将每个Cookie添加到HTTP头部中的COOKIE头部
						.forEach(cookie -> this.headers.add(HttpHeaders.COOKIE, cookie.toString()));
			}
		}

		private URI getUrlToUse() {
			// 使用查询参数构建器构建并展开查询参数
			MultiValueMap<String, String> params =
					this.queryParamsBuilder.buildAndExpand().encode().getQueryParams();

			// 如果查询参数不为空
			if (!params.isEmpty()) {
				// 使用UriComponentsBuilder从当前的URL(this.url)构建URI，并设置查询参数(params)
				return UriComponentsBuilder.fromUri(this.url).queryParams(params).build(true).toUri();
			}

			// 如果查询参数为空，直接返回当前的URL(this.url)
			return this.url;
		}
	}

}
