/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

import javax.servlet.ServletContext;

/**
 * 辅助类，用于解析文本中的占位符。通常应用于文件路径。
 *
 * <p>文本可以包含 {@code ${...}} 占位符，将其解析为 Servlet 上下文初始化参数或系统属性值：
 * 例如 {@code ${user.dir}}。可以使用 ":" 分隔符在键和值之间提供默认值。
 *
 * @author Juergen Hoeller
 * @author Marten Deinum
 * @see SystemPropertyUtils
 * @see ServletContext#getInitParameter(String)
 * @since 3.2.2
 */
public abstract class ServletContextPropertyUtils {

	/**
	 * 严格模式的属性占位符辅助类实例，用于解析占位符并替换为相应的值。
	 *
	 * <p>在严格模式下，如果占位符无法解析，则会抛出异常。
	 */
	private static final PropertyPlaceholderHelper strictHelper =
			new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX,
					SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR, false);

	/**
	 * 非严格模式的属性占位符辅助类实例，用于解析占位符并替换为相应的值。
	 *
	 * <p>在非严格模式下，如果占位符无法解析，则会忽略该占位符，并将其保持不变。
	 */
	private static final PropertyPlaceholderHelper nonStrictHelper =
			new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX,
					SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR, true);


	/**
	 * 解析给定文本中的 ${...} 占位符，将其替换为相应的 Servlet 上下文初始化参数或系统属性值。
	 *
	 * @param text           要解析的字符串
	 * @param servletContext 用于查找的 Servlet 上下文。
	 * @return 解析后的字符串
	 * @throws IllegalArgumentException 如果存在无法解析的占位符
	 * @see SystemPropertyUtils#PLACEHOLDER_PREFIX
	 * @see SystemPropertyUtils#PLACEHOLDER_SUFFIX
	 * @see SystemPropertyUtils#resolvePlaceholders(String, boolean)
	 */
	public static String resolvePlaceholders(String text, ServletContext servletContext) {
		return resolvePlaceholders(text, servletContext, false);
	}

	/**
	 * 解析给定文本中的 ${...} 占位符，将其替换为相应的 Servlet 上下文初始化参数或系统属性值。如果将标志设置为 true，则将无法解析的占位符无默认值地忽略并保持不变。
	 *
	 * @param text                           要解析的字符串
	 * @param servletContext                 用于查找的 Servlet 上下文。
	 * @param ignoreUnresolvablePlaceholders 标志，确定是否忽略无法解析的占位符
	 * @return 解析后的字符串
	 * @throws IllegalArgumentException 如果存在无法解析的占位符且标志为 false
	 * @see SystemPropertyUtils#PLACEHOLDER_PREFIX
	 * @see SystemPropertyUtils#PLACEHOLDER_SUFFIX
	 * @see SystemPropertyUtils#resolvePlaceholders(String, boolean)
	 */
	public static String resolvePlaceholders(
			String text, ServletContext servletContext, boolean ignoreUnresolvablePlaceholders) {

		// 如果文本为空，则直接返回
		if (text.isEmpty()) {
			return text;
		}
		// 根据是否忽略无法解析的占位符选择合适的 属性占位符助手
		PropertyPlaceholderHelper helper = (ignoreUnresolvablePlaceholders ? nonStrictHelper : strictHelper);
		// 使用 Servlet上下文占位符解析器 替换文本中的占位符并返回结果
		return helper.replacePlaceholders(text, new ServletContextPlaceholderResolver(text, servletContext));
	}


	private static class ServletContextPlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {
		/**
		 * 文本字符串
		 */
		private final String text;

		/**
		 * Servlet上下文
		 */
		private final ServletContext servletContext;

		public ServletContextPlaceholderResolver(String text, ServletContext servletContext) {
			this.text = text;
			this.servletContext = servletContext;
		}

		@Override
		@Nullable
		public String resolvePlaceholder(String placeholderName) {
			try {
				// 从Servlet上下文中，获取指定名称的初始化变量值
				String propVal = this.servletContext.getInitParameter(placeholderName);
				if (propVal == null) {
					// 回退到系统属性。
					propVal = System.getProperty(placeholderName);
					if (propVal == null) {
						// 回退到搜索系统环境。
						propVal = System.getenv(placeholderName);
					}
				}
				return propVal;
			} catch (Throwable ex) {
				System.err.println("Could not resolve placeholder '" + placeholderName + "' in [" +
						this.text + "] as ServletContext init-parameter or system property: " + ex);
				return null;
			}
		}
	}

}
