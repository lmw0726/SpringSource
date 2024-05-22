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

package org.springframework.web.servlet.config.annotation;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

/**
 * 辅助注册单个视图控制器。
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @since 3.1
 */
public class ViewControllerRegistration {
	/**
	 * URL路径
	 */
	private final String urlPath;

	/**
	 * 参数化视图控制器
	 */
	private final ParameterizableViewController controller = new ParameterizableViewController();


	public ViewControllerRegistration(String urlPath) {
		Assert.notNull(urlPath, "'urlPath' is required.");
		this.urlPath = urlPath;
	}


	/**
	 * 设置要在响应中设置的状态码。可选的。
	 * <p>如果未设置，则响应状态将为 200 (OK)。
	 */
	public ViewControllerRegistration setStatusCode(HttpStatus statusCode) {
		this.controller.setStatusCode(statusCode);
		return this;
	}

	/**
	 * 设置要返回的视图名称。可选的。
	 * <p>如果未指定，视图控制器将以 {@code null} 作为视图名称返回，在这种情况下，配置的 {@link RequestToViewNameTranslator}
	 * 将选择视图名称。例如，{@code DefaultRequestToViewNameTranslator} 将 "/foo/bar" 翻译为 "foo/bar"。
	 *
	 * @see org.springframework.web.servlet.view.DefaultRequestToViewNameTranslator
	 */
	public void setViewName(String viewName) {
		this.controller.setViewName(viewName);
	}

	protected void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.controller.setApplicationContext(applicationContext);
	}

	protected String getUrlPath() {
		return this.urlPath;
	}

	protected ParameterizableViewController getViewController() {
		return this.controller;
	}

}
