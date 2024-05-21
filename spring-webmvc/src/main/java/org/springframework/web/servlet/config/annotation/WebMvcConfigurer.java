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

package org.springframework.web.servlet.config.annotation;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.List;

/**
 * 定义回调方法，用于自定义通过 {@code @EnableWebMvc} 启用的 Spring MVC 的基于 Java 的配置。
 *
 * <p>使用 {@code @EnableWebMvc} 注解的配置类可以实现此接口，以被回调并有机会自定义默认配置。
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author David Syer
 * @since 3.1
 */
public interface WebMvcConfigurer {

	/**
	 * 帮助配置 {@link HandlerMapping} 路径匹配选项，例如是否使用解析的 {@code PathPatterns}
	 * 或使用 {@code PathMatcher} 进行字符串模式匹配，是否匹配尾部斜杠等。
	 *
	 * @see PathMatchConfigurer
	 * @since 4.0.3
	 */
	default void configurePathMatch(PathMatchConfigurer configurer) {
	}

	/**
	 * 配置内容协商选项。
	 */
	default void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
	}

	/**
	 * 配置异步请求处理选项。
	 */
	default void configureAsyncSupport(AsyncSupportConfigurer configurer) {
	}

	/**
	 * 配置一个处理程序，将未处理的请求转发到 Servlet 容器的“默认”servlet。
	 * 一个常见的用例是在 {@link DispatcherServlet} 映射到 "/" 时，这样覆盖了
	 * Servlet 容器默认的静态资源处理。
	 */
	default void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
	}

	/**
	 * 添加 {@link Converter 转换器} 和 {@link Formatter 格式化器}，除了默认注册的那些。
	 */
	default void addFormatters(FormatterRegistry registry) {
	}

	/**
	 * 添加 Spring MVC 生命周期拦截器，用于控制器方法调用和资源处理请求的前置和后置处理。
	 * 拦截器可以注册应用于所有请求或限制为特定的 URL 模式子集。
	 */
	default void addInterceptors(InterceptorRegistry registry) {
	}

	/**
	 * 添加处理程序以从特定位置（如 Web 应用程序根目录、类路径等）提供静态资源，例如图像、js 和 css 文件。
	 *
	 * @see ResourceHandlerRegistry
	 */
	default void addResourceHandlers(ResourceHandlerRegistry registry) {
	}

	/**
	 * 配置“全局”跨域请求处理。配置的 CORS 映射适用于注解控制器、功能端点和静态资源。
	 * <p>注解控制器可以进一步通过 {@link org.springframework.web.bind.annotation.CrossOrigin @CrossOrigin}
	 * 声明更细粒度的配置。在这种情况下，此处声明的“全局”CORS 配置将与控制器方法上定义的本地 CORS 配置
	 * {@link org.springframework.web.cors.CorsConfiguration#combine(CorsConfiguration) 结合}。
	 *
	 * @see CorsRegistry
	 * @see CorsConfiguration#combine(CorsConfiguration)
	 * @since 4.2
	 */
	default void addCorsMappings(CorsRegistry registry) {
	}

	/**
	 * 配置简单的自动化控制器，预配置响应状态码和/或渲染响应主体的视图。
	 * 这在不需要自定义控制器逻辑的情况下非常有用——例如渲染主页、执行简单的站点 URL 重定向、返回带有 HTML 内容的 404 状态、无内容的 204 等。
	 *
	 * @see ViewControllerRegistry
	 */
	default void addViewControllers(ViewControllerRegistry registry) {
	}

	/**
	 * 配置视图解析器，将控制器返回的基于字符串的视图名称转换为具体的 {@link org.springframework.web.servlet.View} 实现以进行渲染。
	 *
	 * @since 4.1
	 */
	default void configureViewResolvers(ViewResolverRegistry registry) {
	}

	/**
	 * 添加解析器以支持自定义控制器方法参数类型。
	 * <p>这不会覆盖内置的处理程序方法参数解析支持。要自定义内置的参数解析支持，请直接配置 {@link RequestMappingHandlerAdapter}。
	 *
	 * @param resolvers 初始为空列表
	 */
	default void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
	}

	/**
	 * 添加处理程序以支持自定义控制器方法返回值类型。
	 * <p>使用此选项不会覆盖内置的返回值处理支持。要自定义内置的返回值处理支持，请直接配置 RequestMappingHandlerAdapter。
	 *
	 * @param handlers 初始为空列表
	 */
	default void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {
	}

	/**
	 * 配置 {@link HttpMessageConverter HttpMessageConverter}s 以读取请求主体和写入响应主体。
	 * <p>默认情况下，只要 Jackson JSON、JAXB2 等对应的第三方库在类路径上，所有内置的转换器都将被配置。
	 * <p><strong>注意</strong> 使用此方法将关闭默认转换器注册。或者，使用 {@link #extendMessageConverters(java.util.List)} 修改默认转换器列表。
	 *
	 * @param converters 初始为空列表
	 */
	default void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * 在 {@link #configureMessageConverters(List) 配置} 或初始化默认列表后，扩展或修改转换器列表。
	 * <p>请注意，转换器注册的顺序很重要。特别是在客户端接受 {@link org.springframework.http.MediaType#ALL} 的情况下，较早配置的转换器将被优先使用。
	 *
	 * @param converters 要扩展的已配置转换器列表
	 * @since 4.1.3
	 */
	default void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
	}

	/**
	 * 配置异常解析器。
	 * <p>给定的列表最初为空。如果保持为空，框架将配置一组默认的解析器，参见 {@link WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List, org.springframework.web.accept.ContentNegotiationManager)}。
	 * 或者，如果向列表添加任何异常解析器，则应用程序实际上将接管并必须提供完全初始化的异常解析器。
	 * <p>或者，您可以使用 {@link #extendHandlerExceptionResolvers(List)} 来扩展或修改默认配置的异常解析器列表。
	 *
	 * @param resolvers 初始为空列表
	 * @see #extendHandlerExceptionResolvers(List)
	 * @see WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List, org.springframework.web.accept.ContentNegotiationManager)
	 */
	default void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
	}

	/**
	 * 扩展或修改默认配置的异常解析器列表。这对于在不干扰默认解析器的情况下插入自定义异常解析器很有用。
	 *
	 * @param resolvers 要扩展的已配置解析器列表
	 * @see WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers(List, org.springframework.web.accept.ContentNegotiationManager)
	 * @since 4.3
	 */
	default void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
	}

	/**
	 * 提供自定义 {@link Validator} 以代替默认创建的验证器。默认实现是：
	 * {@link org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean}，前提是类路径上存在 JSR-303。
	 * 将返回值保留为 {@code null} 以保留默认值。
	 */
	@Nullable
	default Validator getValidator() {
		return null;
	}

	/**
	 * 提供自定义 {@link MessageCodesResolver}，用于从数据绑定和验证错误代码生成消息代码。
	 * 将返回值保留为 {@code null} 以保留默认值。
	 */
	@Nullable
	default MessageCodesResolver getMessageCodesResolver() {
		return null;
	}

}
