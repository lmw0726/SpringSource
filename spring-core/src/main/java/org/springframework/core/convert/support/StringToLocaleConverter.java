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

package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 将字符串转换为 {@link java.util.Locale}。
 *
 * <p>接受经典的 {@link Locale} 字符串格式 ({@link Locale#toString()})
 * 以及 BCP 47 语言标签 ({@link Locale#forLanguageTag})。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see StringUtils#parseLocale
 */
final class StringToLocaleConverter implements Converter<String, Locale> {

	@Override
	@Nullable
	public Locale convert(String source) {
		return StringUtils.parseLocale(source);
	}

}
