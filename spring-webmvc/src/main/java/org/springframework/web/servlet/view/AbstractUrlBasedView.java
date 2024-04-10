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

package org.springframework.web.servlet.view;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

import java.util.Locale;

/**
 * 基于URL的视图的抽象基类。提供了一种一致的方式来保存视图包装的URL，以"url" bean属性的形式。
 *
 * @author Juergen Hoeller
 * @since 2003年12月13日
 */
public abstract class AbstractUrlBasedView extends AbstractView implements InitializingBean {

	/**
	 * URL
	 */
	@Nullable
	private String url;


	/**
	 * 用作bean的构造函数。
	 */
	protected AbstractUrlBasedView() {
	}

	/**
	 * 使用给定的URL创建一个新的AbstractUrlBasedView。
	 * @param url 要转发到的URL
	 */
	protected AbstractUrlBasedView(String url) {
		this.url = url;
	}


	/**
	 * 设置此视图包装的资源的URL。
	 * URL必须适用于具体的视图实现。
	 */
	public void setUrl(@Nullable String url) {
		this.url = url;
	}

	/**
	 * 返回此视图包装的资源的URL。
	 */
	@Nullable
	public String getUrl() {
		return this.url;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (isUrlRequired() && getUrl() == null) {
			throw new IllegalArgumentException("Property 'url' is required");
		}
	}

	/**
	 * 返回是否需要"url"属性。
	 * <p>默认实现返回{@code true}。
	 * 可以在子类中进行覆盖。
	 */
	protected boolean isUrlRequired() {
		return true;
	}

	/**
	 * 检查配置的URL指向的底层资源是否确实存在。
	 * @param locale 我们正在查找的期望Locale
	 * @return 如果资源存在（或假定存在）则为{@code true}；如果我们知道它不存在，则为{@code false}
	 * @throws Exception 如果资源存在但无效（例如，无法解析）
	 */
	public boolean checkResource(Locale locale) throws Exception {
		return true;
	}

	@Override
	public String toString() {
		return super.toString() + "; URL [" + getUrl() + "]";
	}

}
