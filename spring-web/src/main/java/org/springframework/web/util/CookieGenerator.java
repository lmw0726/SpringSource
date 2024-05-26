/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * CookieGenerator的辅助类，通过bean属性传递cookie描述符设置，并能够将cookie添加到给定的响应中或从中删除。
 *
 * <p>可以作为生成特定cookie的组件的基类，例如CookieLocaleResolver和CookieThemeResolver。
 *
 * @author Juergen Hoeller
 * @see #addCookie
 * @see #removeCookie
 * @see org.springframework.web.servlet.i18n.CookieLocaleResolver
 * @see org.springframework.web.servlet.theme.CookieThemeResolver
 * @since 1.1.4
 */
public class CookieGenerator {

	/**
	 * 默认的cookie路径："/"，即整个服务器可见。
	 */
	public static final String DEFAULT_COOKIE_PATH = "/";

	/**
	 * 日志记录器
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * Cookie名称
	 */
	@Nullable
	private String cookieName;

	/**
	 * Cookie域名
	 */
	@Nullable
	private String cookieDomain;

	/**
	 * Cookie生效的路径
	 */
	private String cookiePath = DEFAULT_COOKIE_PATH;

	/**
	 * Cookie最大生存时间
	 */
	@Nullable
	private Integer cookieMaxAge;

	/**
	 * Cookie是否安全
	 */
	private boolean cookieSecure = false;

	/**
	 * Cookie是否标记为“HttpOnly”属性。
	 */
	private boolean cookieHttpOnly = false;


	/**
	 * 使用给定的名称为此生成器创建的cookie。
	 *
	 * @param cookieName cookie的名称
	 * @see javax.servlet.http.Cookie#getName()
	 */
	public void setCookieName(@Nullable String cookieName) {
		this.cookieName = cookieName;
	}

	/**
	 * 返回此生成器创建的cookie的给定名称。
	 */
	@Nullable
	public String getCookieName() {
		return this.cookieName;
	}

	/**
	 * 使用给定的域为此生成器创建的cookie。
	 * 该cookie仅对该域中的服务器可见。
	 *
	 * @param cookieDomain cookie的域
	 * @see javax.servlet.http.Cookie#setDomain
	 */
	public void setCookieDomain(@Nullable String cookieDomain) {
		this.cookieDomain = cookieDomain;
	}

	/**
	 * 返回此生成器创建的cookie的域，如果有的话。
	 */
	@Nullable
	public String getCookieDomain() {
		return this.cookieDomain;
	}

	/**
	 * 使用给定的路径为此生成器创建的cookie。
	 * 该cookie仅对该路径及其以下的URL可见。
	 *
	 * @param cookiePath cookie的路径
	 * @see javax.servlet.http.Cookie#setPath
	 */
	public void setCookiePath(String cookiePath) {
		this.cookiePath = cookiePath;
	}

	/**
	 * 返回此生成器创建的cookie的路径。
	 */
	public String getCookiePath() {
		return this.cookiePath;
	}

	/**
	 * 使用给定的最大年龄（以秒为单位）为此生成器创建的cookie。
	 * 有用的特殊值：-1 ... 非持久化，客户端关闭时删除。
	 * <p>默认为没有特定的最大年龄，使用Servlet容器的默认值。
	 *
	 * @param cookieMaxAge cookie的最大年龄
	 * @see javax.servlet.http.Cookie#setMaxAge
	 */
	public void setCookieMaxAge(@Nullable Integer cookieMaxAge) {
		this.cookieMaxAge = cookieMaxAge;
	}

	/**
	 * 返回此生成器创建的cookie的最大年龄。
	 */
	@Nullable
	public Integer getCookieMaxAge() {
		return this.cookieMaxAge;
	}

	/**
	 * 设置cookie是否只能使用安全协议（如HTTPS）发送。
	 * 这是给接收方浏览器的指示，而不是由HTTP服务器本身处理的。
	 * <p>默认为“false”。
	 *
	 * @see javax.servlet.http.Cookie#setSecure
	 */
	public void setCookieSecure(boolean cookieSecure) {
		this.cookieSecure = cookieSecure;
	}

	/**
	 * 返回cookie是否只能使用安全协议（如HTTPS）发送。
	 */
	public boolean isCookieSecure() {
		return this.cookieSecure;
	}

	/**
	 * 设置cookie是否应标记为“HttpOnly”属性。
	 * <p>默认为“false”。
	 *
	 * @see javax.servlet.http.Cookie#setHttpOnly
	 */
	public void setCookieHttpOnly(boolean cookieHttpOnly) {
		this.cookieHttpOnly = cookieHttpOnly;
	}

	/**
	 * 返回cookie是否应标记为“HttpOnly”属性。
	 */
	public boolean isCookieHttpOnly() {
		return this.cookieHttpOnly;
	}

	/**
	 * 向响应添加具有给定值的cookie，使用此生成器的cookie描述符设置。
	 * <p>将委派给{@link #createCookie}进行cookie创建。
	 *
	 * @param response    要添加cookie的HTTP响应
	 * @param cookieValue 要添加的cookie的值
	 * @see #setCookieName
	 * @see #setCookieDomain
	 * @see #setCookiePath
	 * @see #setCookieMaxAge
	 */
	public void addCookie(HttpServletResponse response, String cookieValue) {
		Assert.notNull(response, "HttpServletResponse must not be null");
		// 创建 Cookie 对象
		Cookie cookie = createCookie(cookieValue);
		// 获取 Cookie 的最大存活时间
		Integer maxAge = getCookieMaxAge();
		// 如果最大存活时间不为空，则设置到 Cookie 中
		if (maxAge != null) {
			cookie.setMaxAge(maxAge);
		}
		// 如果 Cookie 需要安全连接，则设置为安全 Cookie
		if (isCookieSecure()) {
			cookie.setSecure(true);
		}
		// 如果 Cookie 需要设置为 HttpOnly，则设置为 HttpOnly Cookie
		if (isCookieHttpOnly()) {
			cookie.setHttpOnly(true);
		}
		// 将 Cookie 添加到 HttpServletResponse 中
		response.addCookie(cookie);
		// 如果日志级别为 trace，则记录添加的 Cookie
		if (logger.isTraceEnabled()) {
			logger.trace("Added cookie [" + getCookieName() + "=" + cookieValue + "]");
		}
	}

	/**
	 * 从响应中删除此生成器描述的cookie。
	 * 将生成一个值为空且最大年龄为0的cookie。
	 * <p>将委派给{@link #createCookie}进行cookie创建。
	 *
	 * @param response 要从中删除cookie的HTTP响应
	 * @see #setCookieName
	 * @see #setCookieDomain
	 * @see #setCookiePath
	 */
	public void removeCookie(HttpServletResponse response) {
		Assert.notNull(response, "HttpServletResponse must not be null");
		// 创建一个空值的 Cookie
		Cookie cookie = createCookie("");
		// 将 Cookie 的最大存活时间设置为 0，即立即过期
		cookie.setMaxAge(0);
		// 如果 Cookie 需要安全连接，则设置为安全 Cookie
		if (isCookieSecure()) {
			cookie.setSecure(true);
		}
		// 如果 Cookie 需要设置为 HttpOnly，则设置为 HttpOnly Cookie
		if (isCookieHttpOnly()) {
			cookie.setHttpOnly(true);
		}
		// 将 Cookie 添加到 HttpServletResponse 中
		response.addCookie(cookie);
		// 如果日志级别为 trace，则记录移除的 Cookie
		if (logger.isTraceEnabled()) {
			logger.trace("Removed cookie '" + getCookieName() + "'");
		}
	}

	/**
	 * 使用给定的值创建一个cookie，使用此生成器的cookie描述符设置（除了“cookieMaxAge”）。
	 *
	 * @param cookieValue 要创建的cookie的值
	 * @return cookie
	 * @see #setCookieName
	 * @see #setCookieDomain
	 * @see #setCookiePath
	 */
	protected Cookie createCookie(String cookieValue) {
		// 创建一个 Cookie 对象
		Cookie cookie = new Cookie(getCookieName(), cookieValue);
		// 如果设置了 Cookie 的域名，则设置 Cookie 的域名
		if (getCookieDomain() != null) {
			cookie.setDomain(getCookieDomain());
		}
		// 设置 Cookie 的路径
		cookie.setPath(getCookiePath());
		// 返回创建的 Cookie 对象
		return cookie;
	}


	@Override
	public String toString() {
		return "CookieGenerator with name '" + getCookieName() + "'";
	}
}
