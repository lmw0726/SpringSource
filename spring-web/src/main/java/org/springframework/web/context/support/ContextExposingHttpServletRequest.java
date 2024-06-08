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

package org.springframework.web.context.support;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.HashSet;
import java.util.Set;

/**
 * 将给定 WebApplicationContext 中的所有 Spring bean 可访问为请求属性的 HttpServletRequest 装饰器，
 * 通过一次属性访问时的延迟检查。
 *
 * @author Juergen Hoeller
 * @since 2.5
 */
public class ContextExposingHttpServletRequest extends HttpServletRequestWrapper {
	/**
	 * Web应用程序上下文
	 */
	private final WebApplicationContext webApplicationContext;

	/**
	 * 公开的上下文Bean名称集合
	 */
	@Nullable
	private final Set<String> exposedContextBeanNames;

	/**
	 * 显式属性
	 */
	@Nullable
	private Set<String> explicitAttributes;


	/**
	 * 为给定请求创建一个新的 ContextExposingHttpServletRequest。
	 *
	 * @param originalRequest 原始 HttpServletRequest
	 * @param context         此请求运行的 WebApplicationContext
	 */
	public ContextExposingHttpServletRequest(HttpServletRequest originalRequest, WebApplicationContext context) {
		this(originalRequest, context, null);
	}

	/**
	 * 为给定请求创建一个新的 ContextExposingHttpServletRequest。
	 *
	 * @param originalRequest         原始 HttpServletRequest
	 * @param context                 此请求运行的 WebApplicationContext
	 * @param exposedContextBeanNames 应暴露的上下文中 bean 的名称（如果此参数为非空，则仅此集合中的 bean 可以作为属性暴露）
	 */
	public ContextExposingHttpServletRequest(HttpServletRequest originalRequest, WebApplicationContext context,
											 @Nullable Set<String> exposedContextBeanNames) {

		super(originalRequest);
		Assert.notNull(context, "WebApplicationContext must not be null");
		this.webApplicationContext = context;
		this.exposedContextBeanNames = exposedContextBeanNames;
	}


	/**
	 * 返回此请求运行的 WebApplicationContext。
	 */
	public final WebApplicationContext getWebApplicationContext() {
		return this.webApplicationContext;
	}


	@Override
	@Nullable
	public Object getAttribute(String name) {
		if ((this.explicitAttributes == null || !this.explicitAttributes.contains(name)) &&
				(this.exposedContextBeanNames == null || this.exposedContextBeanNames.contains(name)) &&
				this.webApplicationContext.containsBean(name)) {
			// 如果属性不是显式属性，或者不在 公开的上下文Bean名称 中，
			// 且在 Web应用程序上下文 中存在，则返回相应的 bean
			return this.webApplicationContext.getBean(name);
		} else {
			// 否则调用父类方法获取属性值
			return super.getAttribute(name);
		}
	}

	@Override
	public void setAttribute(String name, Object value) {
		// 调用父类方法设置属性值
		super.setAttribute(name, value);

		// 如果显式属性集合为空，则初始化为新的 HashSet
		if (this.explicitAttributes == null) {
			this.explicitAttributes = new HashSet<>(8);
		}

		// 将属性名称添加到显式属性集合中
		this.explicitAttributes.add(name);
	}

}
