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
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * 辅助注册单个重定向视图控制器。
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class RedirectViewControllerRegistration {
	/**
	 * URL路径
	 */
	private final String urlPath;

	/**
	 * 重定向视图
	 */
	private final RedirectView redirectView;

	/**
	 * 参数化视图控制器
	 */
	private final ParameterizableViewController controller = new ParameterizableViewController();


	public RedirectViewControllerRegistration(String urlPath, String redirectUrl) {
		Assert.notNull(urlPath, "'urlPath' is required.");
		Assert.notNull(redirectUrl, "'redirectUrl' is required.");
		this.urlPath = urlPath;
		this.redirectView = new RedirectView(redirectUrl);
		this.redirectView.setContextRelative(true);
		this.controller.setView(this.redirectView);
	}


	/**
	 * 设置要使用的特定重定向 3xx 状态码。
	 * <p>如果未设置，{@link org.springframework.web.servlet.view.RedirectView}
	 * 将默认选择 {@code HttpStatus.MOVED_TEMPORARILY (302)}。
	 */
	public RedirectViewControllerRegistration setStatusCode(HttpStatus statusCode) {
		Assert.isTrue(statusCode.is3xxRedirection(), "Not a redirect status code");
		this.redirectView.setStatusCode(statusCode);
		return this;
	}

	/**
	 * 是否将以斜杠 ("/") 开头的给定重定向 URL 解释为相对于当前 ServletContext，
	 * 即相对于 Web 应用程序根的路径。
	 * <p>默认为 {@code true}。
	 */
	public RedirectViewControllerRegistration setContextRelative(boolean contextRelative) {
		this.redirectView.setContextRelative(contextRelative);
		return this;
	}

	/**
	 * 是否将当前请求的查询参数传递到目标重定向 URL。
	 * <p>默认为 {@code false}。
	 */
	public RedirectViewControllerRegistration setKeepQueryParams(boolean propagate) {
		this.redirectView.setPropagateQueryParams(propagate);
		return this;
	}

	protected void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.controller.setApplicationContext(applicationContext);
		this.redirectView.setApplicationContext(applicationContext);
	}

	protected String getUrlPath() {
		return this.urlPath;
	}

	protected ParameterizableViewController getViewController() {
		return this.controller;
	}

}
