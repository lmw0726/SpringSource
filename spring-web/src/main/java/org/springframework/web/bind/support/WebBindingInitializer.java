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

package org.springframework.web.bind.support;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.WebRequest;

/**
 * 用于在特定 Web 请求上下文中初始化 {@link WebDataBinder} 以执行数据绑定的回调接口。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 2.5
 */
public interface WebBindingInitializer {

	/**
	 * 初始化给定的 DataBinder。
	 *
	 * @param binder 要初始化的 DataBinder
	 * @since 5.0
	 */
	void initBinder(WebDataBinder binder);

	/**
	 * 为给定的（Servlet）请求初始化给定的 DataBinder。
	 *
	 * @param binder  要初始化的 DataBinder
	 * @param request 数据绑定发生所在的 web 请求
	 * @deprecated 从 5.0 开始，建议使用 {@link #initBinder(WebDataBinder)}
	 */
	@Deprecated
	default void initBinder(WebDataBinder binder, WebRequest request) {
		initBinder(binder);
	}

}
