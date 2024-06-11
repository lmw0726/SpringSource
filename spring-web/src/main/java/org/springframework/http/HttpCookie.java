/*
 * Copyright 2002-2017 the original author or authors.
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

/**
 * 表示 HTTP cookie，作为与“Cookie”请求头内容一致的名称-值对。{@link ResponseCookie}子类具有预期的“Set-Cookie”响应头中的其他属性。
 *
 * @author Rossen Stoyanchev
 * @see <a href="https://tools.ietf.org/html/rfc6265">RFC 6265</a>
 * @since 5.0
 */
public class HttpCookie {
	/**
	 * Cookie名称
	 */
	private final String name;

	/**
	 * Cookie值
	 */
	private final String value;


	public HttpCookie(String name, @Nullable String value) {
		Assert.hasLength(name, "'name' is required and must not be empty.");
		this.name = name;
		this.value = (value != null ? value : "");
	}

	/**
	 * 返回 cookie 名称。
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 返回 cookie 值或空字符串（永不为 {@code null}）。
	 */
	public String getValue() {
		return this.value;
	}


	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HttpCookie)) {
			return false;
		}
		HttpCookie otherCookie = (HttpCookie) other;
		return (this.name.equalsIgnoreCase(otherCookie.getName()));
	}

	@Override
	public String toString() {
		return this.name + '=' + this.value;
	}

}
