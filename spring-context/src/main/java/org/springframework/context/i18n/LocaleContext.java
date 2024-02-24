/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.context.i18n;

import org.springframework.lang.Nullable;

import java.util.Locale;

/**
 * 确定当前 Locale 的策略接口。
 *
 * <p>通过 LocaleContextHolder 类，可以将 LocaleContext 实例与线程关联。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see LocaleContextHolder#getLocale()
 * @see TimeZoneAwareLocaleContext
 */
public interface LocaleContext {

	/**
	 * 返回当前 Locale，其可以是固定的或根据实现策略动态确定的。
	 *
	 * @return 当前 Locale，如果没有关联特定的 Locale，则返回 {@code null}
	 */
	@Nullable
	Locale getLocale();

}
