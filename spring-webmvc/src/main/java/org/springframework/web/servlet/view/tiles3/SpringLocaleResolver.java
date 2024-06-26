/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.locale.impl.DefaultLocaleResolver;
import org.apache.tiles.request.Request;
import org.apache.tiles.request.servlet.NotAServletEnvironmentException;
import org.apache.tiles.request.servlet.ServletUtil;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

/**
 * Tiles LocaleResolver适配器，委托给Spring的{@link org.springframework.web.servlet.LocaleResolver}，
 * 提供DispatcherServlet管理的区域设置。
 *
 * <p>此适配器由{@link TilesConfigurer}自动注册。
 *
 * @author Nicolas Le Bas
 * @see org.apache.tiles.definition.UrlDefinitionsFactory#LOCALE_RESOLVER_IMPL_PROPERTY
 * @since 3.2
 */
public class SpringLocaleResolver extends DefaultLocaleResolver {

	@Override
	public Locale resolveLocale(Request request) {
		try {
			HttpServletRequest servletRequest = ServletUtil.getServletRequest(request).getRequest();
			if (servletRequest != null) {
				return RequestContextUtils.getLocale(servletRequest);
			}
		} catch (NotAServletEnvironmentException ex) {
			// 忽略
		}
		return super.resolveLocale(request);
	}

}
