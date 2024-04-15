/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.servlet.i18n;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

/**
 * {@link LocaleResolver} 实现的抽象基类。
 *
 * <p>提供对 {@linkplain #setDefaultLocale(Locale) 默认区域设置} 的支持。
 *
 * @author Juergen Hoeller
 * @see #setDefaultLocale
 * @since 1.2.9
 */
public abstract class AbstractLocaleResolver implements LocaleResolver {
	/**
	 * 默认的区域设置
	 */
	@Nullable
	private Locale defaultLocale;


	/**
	 * 设置默认的 {@link Locale}，如果没有找到其他区域设置，则此解析器将返回它。
	 */
	public void setDefaultLocale(@Nullable Locale defaultLocale) {
		this.defaultLocale = defaultLocale;
	}

	/**
	 * 获取默认的 {@link Locale}，如果有的话，此解析器应该返回它。
	 */
	@Nullable
	protected Locale getDefaultLocale() {
		return this.defaultLocale;
	}

}
