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

package org.springframework.web.filter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet Filter，允许指定请求的字符编码。
 * 这很有用，因为当前的浏览器通常即使在 HTML 页面或表单中指定了字符编码，也不会设置字符编码。
 *
 * <p>此过滤器可以在请求尚未指定编码时应用其编码，或者无论如何都强制此过滤器的编码（"forceEncoding"="true"）。
 * 在后一种情况下，编码还将作为默认响应编码应用（尽管这通常会被视图中设置的完整内容类型覆盖）。
 *
 * @author Juergen Hoeller
 * @see #setEncoding
 * @see #setForceEncoding
 * @see javax.servlet.http.HttpServletRequest#setCharacterEncoding
 * @see javax.servlet.http.HttpServletResponse#setCharacterEncoding
 * @since 15.03.2004
 */
public class CharacterEncodingFilter extends OncePerRequestFilter {
	/**
	 * 编码方法
	 */
	@Nullable
	private String encoding;

	/**
	 * 是否强制对请求编码
	 */
	private boolean forceRequestEncoding = false;

	/**
	 * 是否强制对响应编码
	 */
	private boolean forceResponseEncoding = false;


	/**
	 * 创建默认的 {@code CharacterEncodingFilter}，编码将通过 {@link #setEncoding} 进行设置。
	 *
	 * @see #setEncoding
	 */
	public CharacterEncodingFilter() {
	}

	/**
	 * 为给定的编码创建 {@code CharacterEncodingFilter}。
	 *
	 * @param encoding 要应用的编码
	 * @see #setEncoding
	 * @since 4.2.3
	 */
	public CharacterEncodingFilter(String encoding) {
		this(encoding, false);
	}

	/**
	 * 为给定的编码创建 {@code CharacterEncodingFilter}。
	 *
	 * @param encoding      要应用的编码
	 * @param forceEncoding 指定的编码是否应该覆盖现有的请求和响应编码
	 * @see #setEncoding
	 * @see #setForceEncoding
	 * @since 4.2.3
	 */
	public CharacterEncodingFilter(String encoding, boolean forceEncoding) {
		this(encoding, forceEncoding, forceEncoding);
	}

	/**
	 * 为给定的编码创建 {@code CharacterEncodingFilter}。
	 *
	 * @param encoding              要应用的编码
	 * @param forceRequestEncoding  指定的编码是否应该覆盖现有的请求编码
	 * @param forceResponseEncoding 指定的编码是否应该覆盖现有的响应编码
	 * @see #setEncoding
	 * @see #setForceRequestEncoding(boolean)
	 * @see #setForceResponseEncoding(boolean)
	 * @since 4.3
	 */
	public CharacterEncodingFilter(String encoding, boolean forceRequestEncoding, boolean forceResponseEncoding) {
		Assert.hasLength(encoding, "Encoding must not be empty");
		this.encoding = encoding;
		this.forceRequestEncoding = forceRequestEncoding;
		this.forceResponseEncoding = forceResponseEncoding;
	}


	/**
	 * 设置要用于请求的编码。此编码将传递到 {@link javax.servlet.http.HttpServletRequest#setCharacterEncoding} 调用中。
	 * <p>此编码是否将覆盖现有的请求编码（以及是否将其作为默认响应编码应用）取决于 {@link #setForceEncoding "forceEncoding"} 标志。
	 */
	public void setEncoding(@Nullable String encoding) {
		this.encoding = encoding;
	}

	/**
	 * 返回配置的请求和/或响应的编码。
	 *
	 * @since 4.3
	 */
	@Nullable
	public String getEncoding() {
		return this.encoding;
	}

	/**
	 * 设置此过滤器的配置的{@link #setEncoding 编码}是否应该覆盖现有的请求和响应编码。
	 * <p>默认为"false"，即如果{@link javax.servlet.http.HttpServletRequest#getCharacterEncoding()} 返回非空值，则不修改编码。
	 * 将其切换为"true"，以在任何情况下强制指定的编码，并将其作为默认的响应编码应用。
	 * <p>这相当于同时设置 {@link #setForceRequestEncoding(boolean)} 和 {@link #setForceResponseEncoding(boolean)}。
	 *
	 * @see #setForceRequestEncoding(boolean)
	 * @see #setForceResponseEncoding(boolean)
	 */
	public void setForceEncoding(boolean forceEncoding) {
		this.forceRequestEncoding = forceEncoding;
		this.forceResponseEncoding = forceEncoding;
	}

	/**
	 * 设置此过滤器的配置的{@link #setEncoding 编码}是否应该覆盖现有的请求编码。
	 * <p>默认为"false"，即如果{@link javax.servlet.http.HttpServletRequest#getCharacterEncoding()} 返回非空值，则不修改编码。
	 * 将其切换为"true"，以在任何情况下强制指定的编码。
	 *
	 * @since 4.3
	 */
	public void setForceRequestEncoding(boolean forceRequestEncoding) {
		this.forceRequestEncoding = forceRequestEncoding;
	}

	/**
	 * 返回编码是否应该强制应用于请求。
	 *
	 * @since 4.3
	 */
	public boolean isForceRequestEncoding() {
		return this.forceRequestEncoding;
	}

	/**
	 * 设置此过滤器的配置的{@link #setEncoding 编码}是否应该覆盖现有的响应编码。
	 * <p>默认为"false"，即不修改编码。
	 * 将其切换为"true"，以在任何情况下强制指定的编码应用于响应。
	 *
	 * @since 4.3
	 */
	public void setForceResponseEncoding(boolean forceResponseEncoding) {
		this.forceResponseEncoding = forceResponseEncoding;
	}

	/**
	 * 返回编码是否应该强制应用于响应。
	 *
	 * @since 4.3
	 */
	public boolean isForceResponseEncoding() {
		return this.forceResponseEncoding;
	}


	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		// 获取请求的编码
		String encoding = getEncoding();

		// 如果编码不为 null
		if (encoding != null) {
			// 如果设置了强制请求编码或请求的字符编码为 null
			if (isForceRequestEncoding() || request.getCharacterEncoding() == null) {
				// 设置请求的字符编码为指定的编码
				request.setCharacterEncoding(encoding);
			}
			// 如果设置了强制响应编码
			if (isForceResponseEncoding()) {
				// 设置响应的字符编码为指定的编码
				response.setCharacterEncoding(encoding);
			}
		}

		// 调用过滤器链的下一个过滤器
		filterChain.doFilter(request, response);
	}

}
