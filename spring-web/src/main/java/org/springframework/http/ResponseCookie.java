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
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * "Set-Cookie" 响应头中允许的附加属性的 {@code HttpCookie} 子类。要构建实例，请使用 {@link #from} 静态方法。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @see <a href="https://tools.ietf.org/html/rfc6265">RFC 6265</a>
 * @since 5.0
 */
public final class ResponseCookie extends HttpCookie {

	/**
	 * Cookie 的最大年龄。
	 */
	private final Duration maxAge;

	/**
	 * Cookie 的域。
	 */
	@Nullable
	private final String domain;

	/**
	 * Cookie 的路径。
	 */
	@Nullable
	private final String path;

	/**
	 * 如果 Cookie 具有 "安全" 属性，则为 true。
	 */
	private final boolean secure;

	/**
	 * 如果 Cookie 具有 "HttpOnly" 属性，则为 true。
	 */
	private final boolean httpOnly;

	/**
	 * Cookie 的 同一站点 属性，如果未设置则为 null。
	 */
	@Nullable
	private final String sameSite;


	/**
	 * 私有构造函数。参见 {@link #from(String, String)}。
	 */
	private ResponseCookie(String name, String value, Duration maxAge, @Nullable String domain,
						   @Nullable String path, boolean secure, boolean httpOnly, @Nullable String sameSite) {

		super(name, value);
		Assert.notNull(maxAge, "Max age must not be null");

		this.maxAge = maxAge;
		this.domain = domain;
		this.path = path;
		this.secure = secure;
		this.httpOnly = httpOnly;
		this.sameSite = sameSite;

		Rfc6265Utils.validateCookieName(name);
		Rfc6265Utils.validateCookieValue(value);
		Rfc6265Utils.validateDomain(domain);
		Rfc6265Utils.validatePath(path);
	}


	/**
	 * 返回 cookie 的 "Max-Age" 属性（以秒为单位）。
	 * <p>正值表示 cookie 相对于当前时间的过期时间。值为 0 表示 cookie 应立即过期。
	 * 负值表示没有 "Max-Age" 属性，在这种情况下，当关闭浏览器时，cookie 将被删除。
	 */
	public Duration getMaxAge() {
		return this.maxAge;
	}

	/**
	 * 返回 cookie 的 "Domain" 属性，如果未设置则返回 {@code null}。
	 */
	@Nullable
	public String getDomain() {
		return this.domain;
	}

	/**
	 * 返回 cookie 的 "Path" 属性，如果未设置则返回 {@code null}。
	 */
	@Nullable
	public String getPath() {
		return this.path;
	}

	/**
	 * 如果 cookie 具有 "Secure" 属性，则返回 {@code true}。
	 */
	public boolean isSecure() {
		return this.secure;
	}

	/**
	 * 如果 cookie 具有 "HttpOnly" 属性，则返回 {@code true}。
	 *
	 * @see <a href="https://www.owasp.org/index.php/HTTPOnly">https://www.owasp.org/index.php/HTTPOnly</a>
	 */
	public boolean isHttpOnly() {
		return this.httpOnly;
	}

	/**
	 * 返回 cookie 的 "SameSite" 属性，如果未设置则返回 {@code null}。
	 * <p>这限制了 cookie 的作用域，使其仅在同一站点请求时（如果为 {@code "Strict"}）或跨站点请求时（如果为 {@code "Lax"}）附加到请求中。
	 *
	 * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-rfc6265bis#section-4.1.2.7">RFC6265 bis</a>
	 * @since 5.1
	 */
	@Nullable
	public String getSameSite() {
		return this.sameSite;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ResponseCookie)) {
			return false;
		}
		ResponseCookie otherCookie = (ResponseCookie) other;
		return (getName().equalsIgnoreCase(otherCookie.getName()) &&
				ObjectUtils.nullSafeEquals(this.path, otherCookie.getPath()) &&
				ObjectUtils.nullSafeEquals(this.domain, otherCookie.getDomain()));
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.domain);
		result = 31 * result + ObjectUtils.nullSafeHashCode(this.path);
		return result;
	}

	@Override
	public String toString() {
		// 创建一个 字符串构建器 对象
		StringBuilder sb = new StringBuilder();
		// 添加Cookie名称和值
		sb.append(getName()).append('=').append(getValue());
		// 如果路径不为空，则添加路径
		if (StringUtils.hasText(getPath())) {
			sb.append("; Path=").append(getPath());
		}
		// 如果域名不为空，则添加域名
		if (StringUtils.hasText(this.domain)) {
			sb.append("; Domain=").append(this.domain);
		}
		// 如果最大年龄不为负值，则添加最大年龄和过期时间
		if (!this.maxAge.isNegative()) {
			sb.append("; Max-Age=").append(this.maxAge.getSeconds());
			sb.append("; Expires=");
			long millis = this.maxAge.getSeconds() > 0 ? System.currentTimeMillis() + this.maxAge.toMillis() : 0;
			sb.append(HttpHeaders.formatDate(millis));
		}
		// 如果是安全的Cookie，则添加Secure属性
		if (this.secure) {
			sb.append("; Secure");
		}
		// 如果是HttpOnly的Cookie，则添加HttpOnly属性
		if (this.httpOnly) {
			sb.append("; HttpOnly");
		}
		// 如果SameSite属性不为空，则添加SameSite属性
		if (StringUtils.hasText(this.sameSite)) {
			sb.append("; SameSite=").append(this.sameSite);
		}
		// 返回构建的字符串
		return sb.toString();
	}


	/**
	 * 获取一个用于构建服务器定义的 Cookie 的构建器的工厂方法，该 Cookie 以名称-值对开头，还可以包括属性。
	 *
	 * @param name  Cookie 名称
	 * @param value Cookie 值
	 * @return 用于创建 Cookie 的构建器
	 */
	public static ResponseCookieBuilder from(final String name, final String value) {
		return from(name, value, false);
	}

	/**
	 * 获取一个用于构建服务器定义的 Cookie 的构建器的工厂方法。与 {@link #from(String, String)} 不同，
	 * 此选项假定来自远程服务器的输入，可以更宽松地处理，例如忽略具有双引号的空域名。
	 *
	 * @param name  Cookie 名称
	 * @param value Cookie 值
	 * @return 用于创建 Cookie 的构建器
	 * @since 5.2.5
	 */
	public static ResponseCookieBuilder fromClientResponse(final String name, final String value) {
		return from(name, value, true);
	}


	private static ResponseCookieBuilder from(final String name, final String value, boolean lenient) {

		return new ResponseCookieBuilder() {

			/**
			 * Cookie 的最大存活时间，默认为 -1，表示在浏览器关闭时删除。
			 */
			private Duration maxAge = Duration.ofSeconds(-1);

			/**
			 * Cookie 的域。如果不设置，则使用默认的域。
			 */
			@Nullable
			private String domain;

			/**
			 * Cookie 的路径。如果不设置，则使用默认的路径。
			 */
			@Nullable
			private String path;

			/**
			 * 是否将 Cookie 标记为安全。默认为 false。
			 */
			private boolean secure;

			/**
			 * 是否将 Cookie 设置为 HttpOnly。默认为 false。
			 */
			private boolean httpOnly;

			/**
			 * Cookie 的 SameSite 属性。可以设置为 "Strict"、"Lax" 或 "None"。如果不设置，则不指定 SameSite 属性。
			 */
			@Nullable
			private String sameSite;

			@Override
			public ResponseCookieBuilder maxAge(Duration maxAge) {
				this.maxAge = maxAge;
				return this;
			}

			@Override
			public ResponseCookieBuilder maxAge(long maxAgeSeconds) {
				this.maxAge = maxAgeSeconds >= 0 ? Duration.ofSeconds(maxAgeSeconds) : Duration.ofSeconds(-1);
				return this;
			}

			@Override
			public ResponseCookieBuilder domain(String domain) {
				this.domain = initDomain(domain);
				return this;
			}

			@Nullable
			private String initDomain(String domain) {
				// 如果是宽松模式并且域名非空
				if (lenient && StringUtils.hasLength(domain)) {
					// 去除首尾空格
					String str = domain.trim();
					// 如果以双引号开头且以双引号结尾
					if (str.startsWith("\"") && str.endsWith("\"")) {
						// 去除首尾双引号，并去除首尾空格后为空，则返回 null
						if (str.substring(1, str.length() - 1).trim().isEmpty()) {
							return null;
						}
					}
				}
				// 返回域名
				return domain;
			}

			@Override
			public ResponseCookieBuilder path(String path) {
				this.path = path;
				return this;
			}

			@Override
			public ResponseCookieBuilder secure(boolean secure) {
				this.secure = secure;
				return this;
			}

			@Override
			public ResponseCookieBuilder httpOnly(boolean httpOnly) {
				this.httpOnly = httpOnly;
				return this;
			}

			@Override
			public ResponseCookieBuilder sameSite(@Nullable String sameSite) {
				this.sameSite = sameSite;
				return this;
			}

			@Override
			public ResponseCookie build() {
				return new ResponseCookie(name, value, this.maxAge, this.domain, this.path,
						this.secure, this.httpOnly, this.sameSite);
			}
		};
	}


	/**
	 * 一个包含属性的服务器定义的 HttpCookie 的构建器。
	 */
	public interface ResponseCookieBuilder {

		/**
		 * 设置 Cookie 的 "Max-Age" 属性。
		 *
		 * <p>正值表示相对于当前时间 Cookie 应该何时过期。值为 0 表示 Cookie 应立即过期。负值导致没有 "Max-Age" 属性，
		 * 在这种情况下，当浏览器关闭时将删除 Cookie。
		 */
		ResponseCookieBuilder maxAge(Duration maxAge);

		/**
		 * {@link #maxAge(Duration)} 的变体，接受以秒为单位的值。
		 */
		ResponseCookieBuilder maxAge(long maxAgeSeconds);

		/**
		 * 设置 Cookie 的 "Path" 属性。
		 */
		ResponseCookieBuilder path(String path);

		/**
		 * 设置 Cookie 的 "Domain" 属性。
		 */
		ResponseCookieBuilder domain(String domain);

		/**
		 * 将 "Secure" 属性添加到 Cookie。
		 */
		ResponseCookieBuilder secure(boolean secure);

		/**
		 * 将 "HttpOnly" 属性添加到 Cookie。
		 *
		 * @see <a href="https://www.owasp.org/index.php/HTTPOnly">https://www.owasp.org/index.php/HTTPOnly</a>
		 */
		ResponseCookieBuilder httpOnly(boolean httpOnly);

		/**
		 * 将 "SameSite" 属性添加到 Cookie。
		 * <p>这限制了 Cookie 的范围，使其仅在同站点请求（如果为 "Strict"）或跨站点请求（如果为 "Lax"）时附加。
		 *
		 * @see <a href="https://tools.ietf.org/html/draft-ietf-httpbis-rfc6265bis#section-4.1.2.7">RFC6265 bis</a>
		 * @since 5.1
		 */
		ResponseCookieBuilder sameSite(@Nullable String sameSite);

		/**
		 * 创建 HttpCookie。
		 */
		ResponseCookie build();
	}


	private static class Rfc6265Utils {

		/**
		 * 表示用作分隔符的字符集合。
		 */
		private static final String SEPARATOR_CHARS = new String(new char[]{
				'(', ')', '<', '>', '@', ',', ';', ':', '\\', '"', '/', '[', ']', '?', '=', '{', '}', ' '
		});

		/**
		 * 表示允许在域名中出现的字符集合。
		 */
		private static final String DOMAIN_CHARS =
				"0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.-";


		public static void validateCookieName(String name) {
			// 遍历名称中的每个字符
			for (int i = 0; i < name.length(); i++) {
				char c = name.charAt(i);
				// 控制字符检查，如果字符小于等于0x1F或等于0x7F，抛出异常
				if (c <= 0x1F || c == 0x7F) {
					throw new IllegalArgumentException(
							name + ": RFC2616 token cannot have control chars");
				}
				// 分隔符检查，如果字符是分隔符，抛出异常
				if (SEPARATOR_CHARS.indexOf(c) >= 0) {
					throw new IllegalArgumentException(
							name + ": RFC2616 token cannot have separator chars such as '" + c + "'");
				}
				// ASCII字符检查，如果字符大于等于0x80，抛出异常
				if (c >= 0x80) {
					throw new IllegalArgumentException(
							name + ": RFC2616 token can only have US-ASCII: 0x" + Integer.toHexString(c));
				}
			}

		}

		public static void validateCookieValue(@Nullable String value) {
			// 如果值为空，则直接返回
			if (value == null) {
				return;
			}

			// 初始化起始索引和结束索引
			int start = 0;
			int end = value.length();

			// 如果值的长度大于1，并且第一个字符是双引号，最后一个字符也是双引号
			if (end > 1 && value.charAt(0) == '"' && value.charAt(end - 1) == '"') {
				// 则将起始索引设置为1，结束索引减1
				start = 1;
				end--;
			}

			// 遍历值中的每个字符
			for (int i = start; i < end; i++) {
				char c = value.charAt(i);
				// 如果字符小于0x21，或者等于0x22，0x2c，0x3b，0x5c，0x7f中的任意一个
				if (c < 0x21 || c == 0x22 || c == 0x2c || c == 0x3b || c == 0x5c || c == 0x7f) {
					// 则抛出异常，因为 RFC2616 cookie 值不能包含这些字符
					throw new IllegalArgumentException(
							"RFC2616 cookie value cannot have '" + c + "'");
				}
				// 如果字符的 ASCII 码大于等于0x80
				if (c >= 0x80) {
					// 则抛出异常，因为 RFC2616 cookie 值只能包含 US-ASCII 字符
					throw new IllegalArgumentException(
							"RFC2616 cookie value can only have US-ASCII chars: 0x" + Integer.toHexString(c));
				}
			}
		}

		public static void validateDomain(@Nullable String domain) {
			// 如果域名为空，则直接返回
			if (!StringUtils.hasLength(domain)) {
				return;
			}
			// 获取域名的第一个字符和最后一个字符的ASCII码值
			int char1 = domain.charAt(0);
			int charN = domain.charAt(domain.length() - 1);
			// 如果第一个字符是'-'，或者最后一个字符是'.'或'-'，抛出异常
			if (char1 == '-' || charN == '.' || charN == '-') {
				throw new IllegalArgumentException("Invalid first/last char in cookie domain: " + domain);
			}
			// 遍历域名中的每个字符
			for (int i = 0, c = -1; i < domain.length(); i++) {
				int p = c;
				c = domain.charAt(i);
				// 如果当前字符不是域名合法字符，或者前一个字符是'.'并且当前字符是'.'或'-'，或者前一个字符是'-'并且当前字符是'.'，抛出异常
				if (DOMAIN_CHARS.indexOf(c) == -1 || (p == '.' && (c == '.' || c == '-')) || (p == '-' && c == '.')) {
					throw new IllegalArgumentException(domain + ": invalid cookie domain char '" + c + "'");
				}
			}
		}

		public static void validatePath(@Nullable String path) {
			// 如果路径为空，则直接返回
			if (path == null) {
				return;
			}

			// 遍历路径中的每个字符
			for (int i = 0; i < path.length(); i++) {
				char c = path.charAt(i);
				// 如果字符小于0x20，大于0x7E，或者等于分号';'
				if (c < 0x20 || c > 0x7E || c == ';') {
					// 则抛出异常，因为路径中不能包含这些字符
					throw new IllegalArgumentException(path + ": Invalid cookie path char '" + c + "'");
				}
			}
		}
	}

}
