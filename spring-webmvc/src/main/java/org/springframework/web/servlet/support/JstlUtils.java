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

package org.springframework.web.servlet.support;

import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.lang.Nullable;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;

/**
 * 辅助类，用于准备 JSTL 视图，特别是用于暴露 JSTL 本地化上下文。
 *
 * @author Juergen Hoeller
 * @since 20.08.2003
 */
public abstract class JstlUtils {

	/**
	 * 检查 JSTL 的 "javax.servlet.jsp.jstl.fmt.localizationContext" 上下文参数，并创建相应的子消息源，
	 * 并使用提供的 Spring 定义的 MessageSource 作为父级。
	 *
	 * @param servletContext 我们正在运行的 ServletContext
	 *                       （用于检查 {@code web.xml} 中的 JSTL 相关上下文参数）
	 * @param messageSource  要暴露的 MessageSource，通常是当前 DispatcherServlet 的 ApplicationContext
	 * @return 要暴露给 JSTL 的 MessageSource；首先检查 JSTL 定义的 bundle，然后是 Spring 定义的 MessageSource
	 * @see org.springframework.context.ApplicationContext
	 */
	public static MessageSource getJstlAwareMessageSource(
			@Nullable ServletContext servletContext, MessageSource messageSource) {

		// 如果存在 Servlet上下文
		if (servletContext != null) {
			// 获取web.xml中配置的JSTL上下文参数
			String jstlInitParam = servletContext.getInitParameter(Config.FMT_LOCALIZATION_CONTEXT);
			// 如果获取到了JSTL上下文参数
			if (jstlInitParam != null) {
				// 创建一个ResourceBundleMessageSource，用于从JSTL资源文件加载消息，并将其设置为messageSource的父级
				ResourceBundleMessageSource jstlBundleWrapper = new ResourceBundleMessageSource();
				jstlBundleWrapper.setBasename(jstlInitParam);
				jstlBundleWrapper.setParentMessageSource(messageSource);
				// 返回JSTL包装后的MessageSource
				return jstlBundleWrapper;
			}
		}

		// 如果没有JSTL上下文参数，则直接返回messageSource
		return messageSource;
	}

	/**
	 * 暴露 JSTL 特定的请求属性，指定 JSTL 的格式化和消息标签的区域设置和资源包，
	 * 使用 Spring 的区域设置和 MessageSource。
	 *
	 * @param request       当前 HTTP 请求
	 * @param messageSource 要暴露的 MessageSource，通常是当前 ApplicationContext（可以为 {@code null}）
	 * @see #exposeLocalizationContext(RequestContext)
	 */
	public static void exposeLocalizationContext(HttpServletRequest request, @Nullable MessageSource messageSource) {
		// 获取JSTL的 区域设置
		Locale jstlLocale = RequestContextUtils.getLocale(request);
		// 设置到请求属性中
		Config.set(request, Config.FMT_LOCALE, jstlLocale);

		// 获取JSTL的 时区
		TimeZone timeZone = RequestContextUtils.getTimeZone(request);
		if (timeZone != null) {
			// 时区不为空，将其设置到请求属性中
			Config.set(request, Config.FMT_TIME_ZONE, timeZone);
		}

		if (messageSource != null) {
			// 如果存在消息源
			// 创建SpringLocalizationContext对象
			LocalizationContext jstlContext = new SpringLocalizationContext(messageSource, request);
			// 并将其设置到请求属性中
			Config.set(request, Config.FMT_LOCALIZATION_CONTEXT, jstlContext);
		}
	}

	/**
	 * 暴露 JSTL 特定的请求属性，指定 JSTL 的格式化和消息标签的区域设置和资源包，
	 * 使用 Spring 的区域设置和 MessageSource。
	 *
	 * @param requestContext 当前 HTTP 请求的上下文，包括要暴露为 MessageSource 的 ApplicationContext
	 */
	public static void exposeLocalizationContext(RequestContext requestContext) {
		// 将获取到的 区域设置 到请求属性中
		Config.set(requestContext.getRequest(), Config.FMT_LOCALE, requestContext.getLocale());

		// 获取请求上下文的逝去
		TimeZone timeZone = requestContext.getTimeZone();
		if (timeZone != null) {
			// 如果时区不为空，则设置到请求属性中
			Config.set(requestContext.getRequest(), Config.FMT_TIME_ZONE, timeZone);
		}

		// 获取JSTL感知的消息源
		MessageSource messageSource = getJstlAwareMessageSource(
				requestContext.getServletContext(), requestContext.getMessageSource());

		// 创建SpringLocalizationContext对象
		LocalizationContext jstlContext = new SpringLocalizationContext(messageSource, requestContext.getRequest());
		// 并将其设置到请求属性中
		Config.set(requestContext.getRequest(), Config.FMT_LOCALIZATION_CONTEXT, jstlContext);
	}


	/**
	 * Spring 特定的 LocalizationContext 适配器，将会话范围的 JSTL LocalizationContext/Locale
	 * 属性与本地 Spring 请求上下文合并。
	 */
	private static class SpringLocalizationContext extends LocalizationContext {

		/**
		 * 消息源
		 */
		private final MessageSource messageSource;

		/**
		 * Http请求
		 */
		private final HttpServletRequest request;

		public SpringLocalizationContext(MessageSource messageSource, HttpServletRequest request) {
			this.messageSource = messageSource;
			this.request = request;
		}

		@Override
		public ResourceBundle getResourceBundle() {
			// 获取请求中的Http会话
			HttpSession session = this.request.getSession(false);

			if (session != null) {
				// 如果会话存在，根据JSTL配置和Http会话获取本地化上下文对象
				Object lcObject = Config.get(session, Config.FMT_LOCALIZATION_CONTEXT);
				if (lcObject instanceof LocalizationContext) {
					// 如果 本地化上下文对象 是LocalizationContext类型
					// 获取绑定资源
					ResourceBundle lcBundle = ((LocalizationContext) lcObject).getResourceBundle();
					// 返回使用 消息源、区域设置、绑定资源 构建的MessageSourceResourceBundle对象
					return new MessageSourceResourceBundle(this.messageSource, getLocale(), lcBundle);
				}
			}

			// 返回使用 消息源、区域设置 构建的MessageSourceResourceBundle对象
			return new MessageSourceResourceBundle(this.messageSource, getLocale());
		}

		@Override
		public Locale getLocale() {
			// 获取Http会话
			HttpSession session = this.request.getSession(false);
			if (session != null) {
				// 如果会话存在，根据JSTL配置和Http会话获取本地化对象
				Object localeObject = Config.get(session, Config.FMT_LOCALE);
				if (localeObject instanceof Locale) {
					// 如果本地化对象是Locale实例，则返回该对象
					return (Locale) localeObject;
				}
			}
			// 使用Http请求获取区域设置
			return RequestContextUtils.getLocale(this.request);
		}
	}

}
