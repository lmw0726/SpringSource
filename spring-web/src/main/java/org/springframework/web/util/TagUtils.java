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

package org.springframework.web.util;

import org.springframework.util.Assert;

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

/**
 * 用于标签库相关代码的实用工具类，提供将 {@link String 字符串} 转换为 Web 作用域等功能。
 *
 * <p>
 * <ul>
 * <li>{@code page} 将被转换为 {@link javax.servlet.jsp.PageContext#PAGE_SCOPE PageContext.PAGE_SCOPE}
 * <li>{@code request} 将被转换为 {@link javax.servlet.jsp.PageContext#REQUEST_SCOPE PageContext.REQUEST_SCOPE}
 * <li>{@code session} 将被转换为 {@link javax.servlet.jsp.PageContext#SESSION_SCOPE PageContext.SESSION_SCOPE}
 * <li>{@code application} 将被转换为 {@link javax.servlet.jsp.PageContext#APPLICATION_SCOPE PageContext.APPLICATION_SCOPE}
 * </ul>
 *
 * @author Alef Arendsen
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public abstract class TagUtils {

	/**
	 * 表示页面作用域的常量。
	 */
	public static final String SCOPE_PAGE = "page";

	/**
	 * 表示请求作用域的常量。
	 */
	public static final String SCOPE_REQUEST = "request";

	/**
	 * 表示会话作用域的常量。
	 */
	public static final String SCOPE_SESSION = "session";

	/**
	 * 表示应用程序作用域的常量。
	 */
	public static final String SCOPE_APPLICATION = "application";


	/**
	 * 确定给定输入 {@code String} 的作用域。
	 * <p>如果 {@code String} 不匹配 'request'、'session'、'page' 或 'application'，
	 * 方法将返回 {@link PageContext#PAGE_SCOPE}。
	 *
	 * @param scope 要检查的 {@code String}
	 * @return 找到的作用域，如果没有匹配的作用域，则返回 {@link PageContext#PAGE_SCOPE}
	 * @throws IllegalArgumentException 如果提供的 {@code scope} 为 {@code null}
	 */
	public static int getScope(String scope) {
		Assert.notNull(scope, "Scope to search for cannot be null");
		if (scope.equals(SCOPE_REQUEST)) {
			// 如果范围等于请求，则返回页面上下文的请求范围
			return PageContext.REQUEST_SCOPE;
		} else if (scope.equals(SCOPE_SESSION)) {
			// 如果范围等于会话，则返回页面上下文的会话范围
			return PageContext.SESSION_SCOPE;
		} else if (scope.equals(SCOPE_APPLICATION)) {
			// 如果范围等于应用程序，则返回页面上下文的应用程序范围
			return PageContext.APPLICATION_SCOPE;
		} else {
			// 否则返回页面上下文的页面范围
			return PageContext.PAGE_SCOPE;
		}
	}

	/**
	 * 确定提供的 {@link Tag} 是否有指定类型的任何祖先标签。
	 *
	 * @param tag              要检查其祖先的标签
	 * @param ancestorTagClass 要搜索的祖先 {@link Class}
	 * @return 如果提供的 {@link Tag} 有指定类型的任何祖先标签，则返回 {@code true}
	 * @throws IllegalArgumentException 如果提供的任一参数为 {@code null}；
	 *                                  或者如果提供的 {@code ancestorTagClass} 不可分配给 {@link Tag} 类
	 */
	public static boolean hasAncestorOfType(Tag tag, Class<?> ancestorTagClass) {
		Assert.notNull(tag, "Tag cannot be null");
		Assert.notNull(ancestorTagClass, "Ancestor tag class cannot be null");
		// 如果祖先标签类不是标签的子类，则抛出异常
		if (!Tag.class.isAssignableFrom(ancestorTagClass)) {
			throw new IllegalArgumentException(
					"Class '" + ancestorTagClass.getName() + "' is not a valid Tag type");
		}
		// 获取标签的祖先标签
		Tag ancestor = tag.getParent();
		// 循环遍历祖先标签，直到为空
		while (ancestor != null) {
			// 如果祖先标签类是祖先的类的父类，则返回true
			if (ancestorTagClass.isAssignableFrom(ancestor.getClass())) {
				return true;
			}
			ancestor = ancestor.getParent();
		}
		// 如果未找到匹配的祖先标签类，则返回false
		return false;
	}

	/**
	 * 确定提供的 {@link Tag} 是否有指定类型的任何祖先标签，如果没有，则抛出 {@link IllegalStateException}。
	 *
	 * @param tag              要检查其祖先的标签
	 * @param ancestorTagClass 要搜索的祖先 {@link Class}
	 * @param tagName          标签的名称；例如 '{@code option}'
	 * @param ancestorTagName  祖先标签的名称；例如 '{@code select}'
	 * @throws IllegalStateException    如果提供的 {@code tag} 没有作为祖先具有提供的 {@code parentTagClass}
	 * @throws IllegalArgumentException 如果任一提供的参数为 {@code null}，
	 *                                  或在 {@link String} 类型参数的情况下，由空白字符完全组成；
	 *                                  或者如果提供的 {@code ancestorTagClass} 不可分配给 {@link Tag} 类
	 * @see #hasAncestorOfType(javax.servlet.jsp.tagext.Tag, Class)
	 */
	public static void assertHasAncestorOfType(Tag tag, Class<?> ancestorTagClass, String tagName,
											   String ancestorTagName) {

		Assert.hasText(tagName, "'tagName' must not be empty");
		Assert.hasText(ancestorTagName, "'ancestorTagName' must not be empty");
		if (!TagUtils.hasAncestorOfType(tag, ancestorTagClass)) {
			// 如果标签没有指定祖先标签类的祖先，则抛出异常
			throw new IllegalStateException("The '" + tagName +
					"' tag can only be used inside a valid '" + ancestorTagName + "' tag.");
		}
	}

}
