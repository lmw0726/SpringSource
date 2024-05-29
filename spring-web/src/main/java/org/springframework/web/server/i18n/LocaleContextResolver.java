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

package org.springframework.web.server.i18n;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

/**
 * 用于基于Web的区域设置上下文解析策略的接口，允许通过请求进行区域设置上下文解析和通过HTTP交换修改区域设置上下文。
 *
 * <p>{@link org.springframework.context.i18n.LocaleContext}对象可能包括关联的时区和其他区域设置相关信息。
 *
 * @author Sebastien Deleuze
 * @see LocaleContext
 * @since 5.0
 */
public interface LocaleContextResolver {

	/**
	 * 通过给定的交换解析当前的区域设置上下文。
	 * <p>返回的上下文可以是{@link org.springframework.context.i18n.TimeZoneAwareLocaleContext}，
	 * 包含具有关联时区信息的区域设置。只需应用{@code instanceof}检查并相应地向下转换。
	 * <p>自定义解析器实现还可以在返回的上下文中返回额外的设置，再次可以通过向下转换进行访问。
	 *
	 * @param exchange 当前服务器交换
	 * @return 当前的区域设置上下文（永远不会为{@code null}）
	 */
	LocaleContext resolveLocaleContext(ServerWebExchange exchange);

	/**
	 * 将当前的区域设置上下文设置为给定的区域设置上下文，
	 * 可能包括具有关联时区信息的区域设置。
	 *
	 * @param exchange      当前服务器交换
	 * @param localeContext 新的区域设置上下文，或{@code null}以清除区域设置
	 * @throws UnsupportedOperationException 如果LocaleResolver实现不支持动态更改区域设置或时区
	 * @see org.springframework.context.i18n.SimpleLocaleContext
	 * @see org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext
	 */
	void setLocaleContext(ServerWebExchange exchange, @Nullable LocaleContext localeContext);

}
