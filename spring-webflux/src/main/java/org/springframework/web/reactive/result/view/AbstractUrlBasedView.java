/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

import java.util.Locale;

/**
 * 基于 URL 的视图的抽象基类。提供了一种持有视图包装的 URL 的一致方式，以“url” bean 属性的形式。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class AbstractUrlBasedView extends AbstractView implements InitializingBean {

	@Nullable
	private String url;

	/**
	 * 作为 bean 使用的构造方法。
	 */
	protected AbstractUrlBasedView() {
	}

	/**
	 * 使用给定的 URL 创建新的 AbstractUrlBasedView。
	 */
	protected AbstractUrlBasedView(String url) {
		this.url = url;
	}

	/**
	 * 设置此视图包装的资源的 URL。URL 必须适用于具体的 View 实现。
	 */
	public void setUrl(@Nullable String url) {
		this.url = url;
	}

	/**
	 * 返回此视图包装的资源的 URL。
	 */
	@Nullable
	public String getUrl() {
		return this.url;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (getUrl() == null) {
			throw new IllegalArgumentException("Property 'url' is required");
		}
	}

	/**
	 * 检查配置的 URL 的资源是否实际存在。
	 *
	 * @param locale 我们正在查找的期望 Locale
	 * @return 如果资源存在，则为 {@code false}，如果我们知道它不存在，则为 {@code false}
	 * @throws Exception 如果资源存在但无效（例如，无法解析）
	 */
	public abstract boolean checkResourceExists(Locale locale) throws Exception;


	@Override
	public String toString() {
		return super.toString() + "; URL [" + getUrl() + "]";
	}

}
