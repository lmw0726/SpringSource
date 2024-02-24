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

import java.util.TimeZone;

/**
 * {@link LocaleContext} 的扩展，增加了对当前时区的感知。
 *
 * <p>如果将这个变体的 LocaleContext 设置为 {@link LocaleContextHolder}，意味着一些支持时区的基础设施已经配置好，
 * 即使此刻可能无法产生一个非空的 TimeZone。
 *
 * @author Juergen Hoeller
 * @author Nicholas Williams
 * @since 4.0
 * @see LocaleContextHolder#getTimeZone()
 */
public interface TimeZoneAwareLocaleContext extends LocaleContext {

	/**
	 * 返回当前的 TimeZone，它可以是固定的，也可以根据实现策略动态确定。
	 * @return 当前的 TimeZone，如果没有特定的 TimeZone 关联，则返回 {@code null}
	 */
	@Nullable
	TimeZone getTimeZone();

}
