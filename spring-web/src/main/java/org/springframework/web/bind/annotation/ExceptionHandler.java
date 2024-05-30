/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.bind.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于处理特定处理程序类和/或处理程序方法中的异常的注解。
 *
 * <p> 带有此注解的处理程序方法允许具有非常灵活的签名。它们可以具有以下类型的参数，顺序任意：
 * <ul>
 * <li>异常参数：声明为通用异常或更具体的异常。如果注解本身未通过其 {@link #value()} 缩小异常类型，则此参数还充当映射提示。
 *    您可以引用传播的顶级异常或包装异常内的嵌套原因。从5.3版开始，任何原因级别都会被公开，而以前只考虑了直接原因。
 * <li>请求和/或响应对象（通常来自Servlet API）。您可以选择任何特定的请求/响应类型，例如
 *    {@link javax.servlet.ServletRequest} / {@link javax.servlet.http.HttpServletRequest}。
 * <li>会话对象：通常为 {@link javax.servlet.http.HttpSession}。此类型的参数将强制存在相应的会话。因此，这样的参数永远不会为 {@code null}。
 *    <i>请注意，会话访问可能不是线程安全的，特别是在Servlet环境中：如果允许多个请求同时访问会话，请考虑将
 *    {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#setSynchronizeOnSession
 *    "synchronizeOnSession"} 标志切换为 "true"。</i>
 * <li>{@link org.springframework.web.context.request.WebRequest} 或
 *    {@link org.springframework.web.context.request.NativeWebRequest}。
 *    允许通用请求参数访问以及请求/会话属性访问，而不受本地Servlet API的约束。
 * <li>{@link java.util.Locale} 用于当前请求的语言环境
 *    （由最具体的语言环境解析器确定，即在Servlet环境中配置的
 *    {@link org.springframework.web.servlet.LocaleResolver}）。
 * <li>{@link java.io.InputStream} / {@link java.io.Reader} 用于访问请求的内容。这将是Servlet API公开的原始 InputStream/Reader。
 * <li>{@link java.io.OutputStream} / {@link java.io.Writer} 用于生成响应的内容。这将是Servlet API公开的原始 OutputStream/Writer。
 * <li>{@link org.springframework.ui.Model} 作为从处理程序方法返回模型映射的替代方法。
 *    请注意，所提供的模型不会预先填充常规模型属性，因此始终为空，以便为特定于异常的视图准备模型。
 * </ul>
 *
 * <p> 支持以下返回类型的处理程序方法：
 * <ul>
 * <li>{@code ModelAndView} 对象（来自Servlet MVC）。
 * <li>{@link org.springframework.ui.Model} 对象，视图名称通过 {@link org.springframework.web.servlet.RequestToViewNameTranslator} 隐式确定。
 * <li>{@link java.util.Map} 对象用于公开模型，视图名称通过
 *    {@link org.springframework.web.servlet.RequestToViewNameTranslator} 隐式确定。
 * <li>{@link org.springframework.web.servlet.View} 对象。
 * <li>作为视图名称解释的 {@link String} 值。
 * <li>{@link ResponseBody @ResponseBody} 注解的方法（仅Servlet）用于设置响应内容。返回值将使用
 *    {@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器} 转换为响应流。
 * <li>{@link org.springframework.http.HttpEntity HttpEntity&lt;?&gt;} 或
 *    {@link org.springframework.http.ResponseEntity ResponseEntity&lt;?&gt;} 对象
 *    （仅Servlet）用于设置响应标头和内容。ResponseEntity 主体将转换并写入响应流使用
 *    {@linkplain org.springframework.http.converter.HttpMessageConverter 消息转换器}。
 * <li>{@code void} 如果方法本身处理响应（通过直接编写响应内容，声明一个类型的参数
 *    {@link javax.servlet.ServletResponse} / {@link javax.servlet.http.HttpServletResponse}
 *    用于此目的）或者如果视图名称应该通过 {@link org.springframework.web.servlet.RequestToViewNameTranslator} 隐式确定
 *    （在处理程序方法签名中不声明响应参数）。
 * </ul>
 *
 * <p> 您可以将 {@code ExceptionHandler} 注解与 {@link ResponseStatus @ResponseStatus} 结合使用以指定特定的HTTP错误状态。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see ControllerAdvice
 * @see org.springframework.web.context.request.WebRequest
 * @since 3.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExceptionHandler {

	/**
	 * 被注解方法处理的异常。如果为空，则默认为方法参数列表中列出的任何异常。
	 */
	Class<? extends Throwable>[] value() default {};

}
