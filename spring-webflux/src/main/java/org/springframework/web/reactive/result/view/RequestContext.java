/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.EscapedErrors;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * 请求特定状态的上下文持有者，比如要使用的 {@link MessageSource}、当前区域设置、绑定错误等等。
 * 提供对本地化消息和 Errors 实例的轻松访问。
 * <p>
 * 适用于暴露给视图以及在 FreeMarker 模板和标签库中使用。
 * <p>
 * 可以手动实例化，也可以通过 AbstractView 的 "requestContextAttribute" 属性自动暴露给视图作为模型属性。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RequestContext {

	/**
	 * 服务器 Web 交换对象
	 */
	private final ServerWebExchange exchange;

	/**
	 * 模型数据
	 */
	private final Map<String, Object> model;

	/**
	 * 消息源
	 */
	private final MessageSource messageSource;

	/**
	 * 区域设置
	 */
	private Locale locale;

	/**
	 * 时区
	 */
	private TimeZone timeZone;

	/**
	 * 默认 HTML 转义标志
	 */
	@Nullable
	private Boolean defaultHtmlEscape;

	/**
	 * 错误映射
	 */
	@Nullable
	private Map<String, Errors> errorsMap;

	/**
	 * 请求数据值处理器
	 */
	@Nullable
	private RequestDataValueProcessor dataValueProcessor;


	public RequestContext(ServerWebExchange exchange, Map<String, Object> model, MessageSource messageSource) {
		this(exchange, model, messageSource, null);
	}

	public RequestContext(ServerWebExchange exchange, Map<String, Object> model, MessageSource messageSource,
						  @Nullable RequestDataValueProcessor dataValueProcessor) {

		Assert.notNull(exchange, "ServerWebExchange is required");
		Assert.notNull(model, "Model is required");
		Assert.notNull(messageSource, "MessageSource is required");
		this.exchange = exchange;
		this.model = model;
		this.messageSource = messageSource;

		LocaleContext localeContext = exchange.getLocaleContext();
		Locale locale = localeContext.getLocale();
		this.locale = (locale != null ? locale : Locale.getDefault());
		TimeZone timeZone = (localeContext instanceof TimeZoneAwareLocaleContext ?
				((TimeZoneAwareLocaleContext) localeContext).getTimeZone() : null);
		this.timeZone = (timeZone != null ? timeZone : TimeZone.getDefault());

		this.defaultHtmlEscape = null;  // TODO
		this.dataValueProcessor = dataValueProcessor;
	}


	protected final ServerWebExchange getExchange() {
		return this.exchange;
	}

	/**
	 * 返回此请求中使用的 MessageSource。
	 */
	public MessageSource getMessageSource() {
		return this.messageSource;
	}

	/**
	 * 返回此 RequestContext 封装的模型 Map（如果有）。
	 *
	 * @return 已填充的模型 Map，如果没有则为 {@code null}
	 */
	@Nullable
	public Map<String, Object> getModel() {
		return this.model;
	}

	/**
	 * 返回当前的区域设置。
	 */
	public final Locale getLocale() {
		return this.locale;
	}

	/**
	 * 返回当前的时区。
	 */
	public TimeZone getTimeZone() {
		return this.timeZone;
	}

	/**
	 * 将当前区域设置更改为指定的区域设置。
	 */
	public void changeLocale(Locale locale) {
		this.locale = locale;
	}

	/**
	 * 将当前的区域设置更改为指定的区域设置和时区上下文。
	 */
	public void changeLocale(Locale locale, TimeZone timeZone) {
		this.locale = locale;
		this.timeZone = timeZone;
	}

	/**
	 * （取消）激活消息和错误的默认 HTML 转义，用于此 RequestContext 的范围。
	 * <p>TODO：目前还没有应用程序范围的设置...
	 */
	public void setDefaultHtmlEscape(boolean defaultHtmlEscape) {
		this.defaultHtmlEscape = defaultHtmlEscape;
	}

	/**
	 * 默认的 HTML 转义是否处于激活状态？如果没有显式的默认设置，则回退为 {@code false}。
	 */
	public boolean isDefaultHtmlEscape() {
		return (this.defaultHtmlEscape != null && this.defaultHtmlEscape.booleanValue());
	}

	/**
	 * 返回默认的 HTML 转义设置，区分是否指定了默认设置和指定的值。
	 *
	 * @return 默认的 HTML 转义是否启用（null = 没有显式的默认设置）
	 */
	@Nullable
	public Boolean getDefaultHtmlEscape() {
		return this.defaultHtmlEscape;
	}

	/**
	 * 返回要应用于表单标签库和重定向 URL 的 {@link RequestDataValueProcessor} 实例。
	 */
	@Nullable
	public RequestDataValueProcessor getRequestDataValueProcessor() {
		return this.dataValueProcessor;
	}

	/**
	 * 返回当前 Web 应用程序的上下文路径。这对于构建指向应用程序内其他资源的链接很有用。
	 * <p>委托给 {@link ServerHttpRequest#getPath()}。
	 */
	public String getContextPath() {
		return this.exchange.getRequest().getPath().contextPath().value();
	}

	/**
	 * 返回给定相对 URL 的上下文感知 URL。
	 *
	 * @param relativeUrl 相对 URL 部分
	 * @return 一个 URL，指向当前 Web 应用程序的绝对路径，并根据需要进行 URL 编码
	 */
	public String getContextUrl(String relativeUrl) {
		String url = StringUtils.applyRelativePath(getContextPath() + "/", relativeUrl);
		return getExchange().transformUrl(url);
	}

	/**
	 * 返回给定带有占位符的相对 URL 的上下文感知 URL，占位符是用大括号 {@code {}} 括起来的命名键。例如，发送一个相对 URL {@code foo/{bar}?spam={spam}} 和一个参数映射 {@code {bar=baz,spam=nuts}}，结果将是 {@code [contextpath]/foo/baz?spam=nuts}。
	 *
	 * @param relativeUrl 相对 URL 部分
	 * @param params      要作为占位符插入 URL 中的参数映射
	 * @return 一个 URL，指向当前 Web 应用程序的绝对路径，并根据需要进行 URL 编码
	 */
	public String getContextUrl(String relativeUrl, Map<String, ?> params) {
		String url = StringUtils.applyRelativePath(getContextPath() + "/", relativeUrl);
		url = UriComponentsBuilder.fromUriString(url).buildAndExpand(params).encode().toUri().toASCIIString();
		return getExchange().transformUrl(url);
	}

	/**
	 * 返回请求的请求路径。这对于 HTML 表单的操作目标很有用，也可以与原始查询字符串结合使用。
	 */
	public String getRequestPath() {
		return this.exchange.getRequest().getURI().getPath();
	}

	/**
	 * 返回当前请求的查询字符串。这对于与原始请求路径结合使用以构建 HTML 表单动作目标非常有用。
	 */
	public String getQueryString() {
		return this.exchange.getRequest().getURI().getQuery();
	}

	/**
	 * 使用“默认的HTML转义”设置，检索给定代码的消息。
	 *
	 * @param code           消息的代码
	 * @param defaultMessage 如果查找失败，则返回的字符串
	 * @return 消息
	 */
	public String getMessage(String code, String defaultMessage) {
		return getMessage(code, null, defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * 使用“默认的HTML转义”设置，检索给定代码的消息。
	 *
	 * @param code           消息的代码
	 * @param args           消息的参数，如果没有则为{@code null}
	 * @param defaultMessage 如果查找失败，则返回的字符串
	 * @return 消息
	 */
	public String getMessage(String code, @Nullable Object[] args, String defaultMessage) {
		return getMessage(code, args, defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * 使用“默认的HTML转义”设置，检索给定代码的消息。
	 *
	 * @param code           消息的代码
	 * @param args           消息的参数列表，如果没有则为{@code null}
	 * @param defaultMessage 如果查找失败，则返回的字符串
	 * @return 消息
	 */
	public String getMessage(String code, @Nullable List<?> args, String defaultMessage) {
		return getMessage(code, (args != null ? args.toArray() : null), defaultMessage, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定代码的消息。
	 *
	 * @param code           消息的代码
	 * @param args           消息的参数，如果没有则为{@code null}
	 * @param defaultMessage 如果查找失败，则返回的字符串
	 * @param htmlEscape     是否应该对消息进行 HTML 转义
	 * @return 消息
	 */
	public String getMessage(String code, @Nullable Object[] args, String defaultMessage, boolean htmlEscape) {
		String msg = this.messageSource.getMessage(code, args, defaultMessage, this.locale);
		if (msg == null) {
			return "";
		}
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * 使用“默认的HTML转义”设置，检索给定代码的消息。
	 *
	 * @param code 消息的代码
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果找不到消息
	 */
	public String getMessage(String code) throws NoSuchMessageException {
		return getMessage(code, null, isDefaultHtmlEscape());
	}

	/**
	 * 使用“默认的HTML转义”设置，检索给定代码的消息。
	 *
	 * @param code 消息的代码
	 * @param args 消息的参数，如果没有则为{@code null}
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果找不到消息
	 */
	public String getMessage(String code, @Nullable Object[] args) throws NoSuchMessageException {
		return getMessage(code, args, isDefaultHtmlEscape());
	}

	/**
	 * 使用“默认的HTML转义”设置，检索给定代码的消息。
	 *
	 * @param code 消息的代码
	 * @param args 消息的参数列表，如果没有则为{@code null}
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果找不到消息
	 */
	public String getMessage(String code, @Nullable List<?> args) throws NoSuchMessageException {
		return getMessage(code, (args != null ? args.toArray() : null), isDefaultHtmlEscape());
	}

	/**
	 * 检索给定代码的消息。
	 *
	 * @param code       消息的代码
	 * @param args       消息的参数，如果没有则为{@code null}
	 * @param htmlEscape 如果消息应该进行 HTML 转义
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果找不到消息
	 */
	public String getMessage(String code, @Nullable Object[] args, boolean htmlEscape) throws NoSuchMessageException {
		String msg = this.messageSource.getMessage(code, args, this.locale);
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * 使用“默认的HTML转义”设置，检索给定的 MessageSourceResolvable（例如，ObjectError 实例）。
	 *
	 * @param resolvable MessageSourceResolvable
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果找不到消息
	 */
	public String getMessage(MessageSourceResolvable resolvable) throws NoSuchMessageException {
		return getMessage(resolvable, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定的 MessageSourceResolvable（例如，ObjectError 实例）。
	 *
	 * @param resolvable MessageSourceResolvable
	 * @param htmlEscape 如果消息应该进行 HTML 转义
	 * @return 消息
	 * @throws org.springframework.context.NoSuchMessageException 如果找不到消息
	 */
	public String getMessage(MessageSourceResolvable resolvable, boolean htmlEscape) throws NoSuchMessageException {
		String msg = this.messageSource.getMessage(resolvable, this.locale);
		return (htmlEscape ? HtmlUtils.htmlEscape(msg) : msg);
	}

	/**
	 * 使用“默认的HTML转义”设置，检索给定绑定对象的 Errors 实例。
	 *
	 * @param name 绑定对象的名称
	 * @return Errors 实例，如果未找到则为{@code null}
	 */
	@Nullable
	public Errors getErrors(String name) {
		return getErrors(name, isDefaultHtmlEscape());
	}

	/**
	 * 检索给定绑定对象的 Errors 实例。
	 *
	 * @param name       绑定对象的名称
	 * @param htmlEscape 是否创建一个自动进行 HTML 转义的 Errors 实例
	 * @return Errors 实例，如果未找到则为{@code null}
	 */
	@Nullable
	public Errors getErrors(String name, boolean htmlEscape) {
		if (this.errorsMap == null) {
			this.errorsMap = new HashMap<>();
		}

		Errors errors = this.errorsMap.get(name);
		if (errors == null) {
			errors = getModelObject(BindingResult.MODEL_KEY_PREFIX + name);
			if (errors == null) {
				return null;
			}
		}

		if (errors instanceof BindException) {
			errors = ((BindException) errors).getBindingResult();
		}

		if (htmlEscape && !(errors instanceof EscapedErrors)) {
			errors = new EscapedErrors(errors);
		} else if (!htmlEscape && errors instanceof EscapedErrors) {
			errors = ((EscapedErrors) errors).getSource();
		}

		this.errorsMap.put(name, errors);
		return errors;
	}

	/**
	 * 从模型或请求属性中检索给定模型名称的模型对象。
	 *
	 * @param modelName 模型对象的名称
	 * @return 模型对象
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	protected <T> T getModelObject(String modelName) {
		T modelObject = (T) this.model.get(modelName);
		if (modelObject == null) {
			modelObject = this.exchange.getAttribute(modelName);
		}
		return modelObject;
	}

	/**
	 * 使用“默认的HTML转义”设置为给定绑定对象创建 BindStatus。
	 *
	 * @param path 要解析值和错误的 bean 和属性路径（例如："person.age"）
	 * @return 新的 BindStatus 实例
	 * @throws IllegalStateException 如果找不到对应的 Errors 对象
	 */
	public BindStatus getBindStatus(String path) throws IllegalStateException {
		return new BindStatus(this, path, isDefaultHtmlEscape());
	}

	/**
	 * 使用“默认的HTML转义”设置为给定绑定对象创建 BindStatus。
	 *
	 * @param path       要解析值和错误的 bean 和属性路径（例如："person.age"）
	 * @param htmlEscape 是否创建一个自动进行 HTML 转义的 BindStatus
	 * @return 新的 BindStatus 实例
	 * @throws IllegalStateException 如果找不到对应的 Errors 对象
	 */
	public BindStatus getBindStatus(String path, boolean htmlEscape) throws IllegalStateException {
		return new BindStatus(this, path, htmlEscape);
	}

}
