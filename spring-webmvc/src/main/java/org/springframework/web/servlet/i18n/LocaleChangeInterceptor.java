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

package org.springframework.web.servlet.i18n;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * 拦截器，允许通过可配置的请求参数（默认参数名为 "locale"）在每个请求上更改当前区域设置。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see org.springframework.web.servlet.LocaleResolver
 * @since 20.06.2003
 */
public class LocaleChangeInterceptor implements HandlerInterceptor {

	/**
	 * 包含区域设置规范的参数的默认名称："locale"。
	 */
	public static final String DEFAULT_PARAM_NAME = "locale";


	protected final Log logger = LogFactory.getLog(getClass());
	/**
	 * 参数名
	 */
	private String paramName = DEFAULT_PARAM_NAME;
	/**
	 * HTTP方法
	 */
	@Nullable
	private String[] httpMethods;

	/**
	 * 是否忽视无效的locale值
	 */
	private boolean ignoreInvalidLocale = false;


	/**
	 * 设置包含区域设置规范的参数的名称。默认为 "locale"。
	 */
	public void setParamName(String paramName) {
		this.paramName = paramName;
	}

	/**
	 * 返回包含区域设置规范的参数的名称。
	 */
	public String getParamName() {
		return this.paramName;
	}

	/**
	 * 配置可更改区域设置的 HTTP 方法。
	 *
	 * @param httpMethods 方法
	 * @since 4.2
	 */
	public void setHttpMethods(@Nullable String... httpMethods) {
		this.httpMethods = httpMethods;
	}

	/**
	 * 返回配置的 HTTP 方法。
	 *
	 * @since 4.2
	 */
	@Nullable
	public String[] getHttpMethods() {
		return this.httpMethods;
	}

	/**
	 * 设置是否忽略区域设置参数的无效值。
	 *
	 * @since 4.2.2
	 */
	public void setIgnoreInvalidLocale(boolean ignoreInvalidLocale) {
		this.ignoreInvalidLocale = ignoreInvalidLocale;
	}

	/**
	 * 返回是否忽略区域设置参数的无效值。
	 *
	 * @since 4.2.2
	 */
	public boolean isIgnoreInvalidLocale() {
		return this.ignoreInvalidLocale;
	}

	/**
	 * 指定是否将请求参数值解析为 BCP 47 语言标签，而不是 Java 的传统区域设置格式。
	 * <p><b>注意：从 5.1 开始，此解析器宽松接受传统的 {@link Locale#toString} 格式以及 BCP 47 语言标签。</b>
	 *
	 * @see Locale#forLanguageTag(String)
	 * @see Locale#toLanguageTag()
	 * @since 4.3
	 * @deprecated 自 5.1 开始，只接受 {@code true}，因此已不建议使用
	 */
	@Deprecated
	public void setLanguageTagCompliant(boolean languageTagCompliant) {
		if (!languageTagCompliant) {
			throw new IllegalArgumentException("LocaleChangeInterceptor always accepts BCP 47 language tags");
		}
	}

	/**
	 * 返回是否使用 BCP 47 语言标签而不是 Java 的传统区域设置格式。
	 *
	 * @since 4.3
	 * @deprecated 自 5.1 开始，始终返回 {@code true}，因此已不建议使用
	 */
	@Deprecated
	public boolean isLanguageTagCompliant() {
		return true;
	}


	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws ServletException {

		// 从请求参数中获取新的语言环境
		String newLocale = request.getParameter(getParamName());
		if (newLocale != null) {
			// 检查 HTTP 方法是否符合要求
			if (checkHttpMethod(request.getMethod())) {
				// 获取 LocaleResolver
				LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
				if (localeResolver == null) {
					// 如果找不到 LocaleResolver，则抛出异常
					throw new IllegalStateException(
							"No LocaleResolver found: not in a DispatcherServlet request?");
				}
				try {
					// 设置新的语言环境
					localeResolver.setLocale(request, response, parseLocaleValue(newLocale));
				} catch (IllegalArgumentException ex) {
					// 如果语言环境值无效，则根据配置选择是否忽略
					if (isIgnoreInvalidLocale()) {
						// 如果配置为忽略无效的语言环境值，则记录调试日志
						if (logger.isDebugEnabled()) {
							logger.debug("Ignoring invalid locale value [" + newLocale + "]: " + ex.getMessage());
						}
					} else {
						// 如果配置为不忽略无效的语言环境值，则抛出异常
						throw ex;
					}
				}
			}
		}
		// 无论如何都继续执行
		return true;
	}

	private boolean checkHttpMethod(String currentMethod) {
		// 获取配置的 HTTP 方法
		String[] configuredMethods = getHttpMethods();
		if (ObjectUtils.isEmpty(configuredMethods)) {
			// 如果配置的方法为空，则返回 true
			return true;
		}
		// 遍历配置的方法
		for (String configuredMethod : configuredMethods) {
			// 如果当前方法与配置的方法匹配（忽略大小写），则返回 true
			if (configuredMethod.equalsIgnoreCase(currentMethod)) {
				return true;
			}
		}
		// 如果当前方法与任何配置的方法都不匹配，则返回 false
		return false;
	}

	/**
	 * 将给定的区域设置值解析为来自请求参数。
	 * <p>默认实现调用 {@link StringUtils#parseLocale(String)}，接受 {@link Locale#toString} 格式以及 BCP 47 语言标签。
	 *
	 * @param localeValue 要解析的区域设置值
	 * @return 相应的 {@code Locale} 实例
	 * @since 4.3
	 */
	@Nullable
	protected Locale parseLocaleValue(String localeValue) {
		return StringUtils.parseLocale(localeValue);
	}

}
