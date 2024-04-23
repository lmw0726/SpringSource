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

package org.springframework.web.servlet;

import org.springframework.lang.Nullable;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * 请求和处理器对象之间定义映射关系的对象必须实现的接口。
 *
 * <p>虽然应用程序开发人员可以实现此接口，但这并非必需，因为框架中包含了
 * {@link org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping}
 * 和 {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping}。
 * <p>
 * 如果在应用程序上下文中没有注册
 * HandlerMapping bean，则前者是默认的。
 *
 * <p>HandlerMapping 实现可以支持映射的拦截器，但并非必须。
 * 一个处理器始终会被包装在一个 {@link HandlerExecutionChain} 实例中，可选地伴随一些{@link HandlerInterceptor} 实例。
 * DispatcherServlet 首先按给定顺序调用每个 HandlerInterceptor 的 {@code preHandle} 方法，如果所有
 * {@code preHandle} 方法都返回 {@code true}，则最终调用处理器本身。
 *
 * <p>此映射的参数化能力是此 MVC 框架的强大且不寻常的功能。例如，可以基于会话状态、Cookie 状态或许多其他变量编写自定义映射。
 * 似乎没有其他 MVC 框架具有同样的灵活性。
 *
 * <p>注意：实现可以实现 {@link org.springframework.core.Ordered} 接口以指定排序顺序和由 DispatcherServlet 应用的优先级。
 * 未排序的实例将被视为最低优先级。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.core.Ordered
 * @see org.springframework.web.servlet.handler.AbstractHandlerMapping
 * @see org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
 */
public interface HandlerMapping {

	/**
	 * 包含最佳匹配模式的映射处理程序的 {@link HttpServletRequest} 属性的名称。
	 *
	 * @since 4.3.21
	 */
	String BEST_MATCHING_HANDLER_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingHandler";

	/**
	 * 包含用于查找匹配处理程序的路径的 {@link HttpServletRequest} 属性的名称，
	 * 根据配置的 {@link org.springframework.web.util.UrlPathHelper} 可能是完整路径或没有上下文路径、已解码或未解码等。
	 *
	 * @since 5.2
	 * @deprecated 自 5.3 起弃用，改用 {@link org.springframework.web.util.UrlPathHelper#PATH_ATTRIBUTE} 和
	 * {@link org.springframework.web.util.ServletRequestPathUtils#PATH_ATTRIBUTE}。
	 * 要访问用于请求映射的缓存路径，请使用 {@link org.springframework.web.util.ServletRequestPathUtils#getCachedPathValue(ServletRequest)}。
	 */
	@Deprecated
	String LOOKUP_PATH = HandlerMapping.class.getName() + ".lookupPath";

	/**
	 * 包含处理程序映射中路径的 {@link HttpServletRequest} 属性的名称（如果是模式匹配），
	 * 或在其他情况下是完整相关 URI（通常在 DispatcherServlet 的映射内）。
	 * <p>注意：并非所有 HandlerMapping 实现都需要支持此属性。
	 * 基于 URL 的 HandlerMapping 通常会支持它，但处理器不应该在所有场景中都期望此请求属性存在。
	 */
	String PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE = HandlerMapping.class.getName() + ".pathWithinHandlerMapping";

	/**
	 * 包含处理程序映射中最佳匹配模式的 {@link HttpServletRequest} 属性的名称。
	 * <p>注意：并非所有 HandlerMapping 实现都需要支持此属性。
	 * 基于 URL 的 HandlerMapping 通常会支持它，但处理器不应该在所有场景中都期望此请求属性存在。
	 */
	String BEST_MATCHING_PATTERN_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingPattern";

	/**
	 * 指示是否应检查类型级别映射的布尔 {@link HttpServletRequest} 属性的名称。
	 * <p>注意：并非所有 HandlerMapping 实现都需要支持此属性。
	 */
	String INTROSPECT_TYPE_LEVEL_MAPPING = HandlerMapping.class.getName() + ".introspectTypeLevelMapping";

	/**
	 * 包含 URI 模板映射的 {@link HttpServletRequest} 属性的名称，将变量名称映射到值。
	 * <p>注意：并非所有 HandlerMapping 实现都需要支持此属性。
	 * 基于 URL 的 HandlerMapping 通常会支持它，但处理器不应该在所有场景中都期望此请求属性存在。
	 */
	String URI_TEMPLATE_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".uriTemplateVariables";

	/**
	 * 包含 URI 变量名称和相应的 MultiValueMap 的映射的 {@link HttpServletRequest} 属性的名称，用于每个 URI 变量。
	 * <p>注意：并非所有 HandlerMapping 实现都需要支持此属性，
	 * 并且取决于 HandlerMapping 是否配置为保留矩阵变量内容，也可能不存在。
	 */
	String MATRIX_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".matrixVariables";

	/**
	 * 包含适用于映射处理程序的可生成 MediaType 集的 {@link HttpServletRequest} 属性的名称。
	 * <p>注意：并非所有 HandlerMapping 实现都需要支持此属性。处理器不应该在所有场景中都期望此请求属性存在。
	 */
	String PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE = HandlerMapping.class.getName() + ".producibleMediaTypes";


	/**
	 * 此 {@code HandlerMapping} 实例是否已启用以使用解析的 {@link org.springframework.web.util.pattern.PathPattern}，
	 * 在这种情况下，{@link DispatcherServlet} 自动{@link org.springframework.web.util.ServletRequestPathUtils#parseAndCache 解析}
	 * {@code RequestPath} 以使其在 {@code HandlerMapping}、{@code HandlerInterceptor} 和其他组件中可用。
	 *
	 * @since 5.3
	 */
	default boolean usesPathPatterns() {
		return false;
	}

	/**
	 * 返回此请求的处理程序和任何拦截器。选择可以基于请求 URL、会话状态或实现类选择的任何因素进行。
	 * <p>返回的 HandlerExecutionChain 包含一个处理程序对象，而不是甚至是标记接口，因此处理程序没有任何限制。
	 * 例如，可以编写一个 HandlerAdapter，允许使用另一个框架的处理程序对象。
	 * <p>如果找不到匹配项，则返回 {@code null}。这不是错误。DispatcherServlet 将查询所有已注册的 HandlerMapping bean 来查找匹配项，
	 * 仅当找不到处理程序时才决定存在错误。
	 *
	 * @param request 当前的 HTTP 请求
	 * @return 包含处理程序对象和任何拦截器的 HandlerExecutionChain 实例，如果未找到映射，则为 {@code null}
	 * @throws Exception 如果存在内部错误
	 */
	@Nullable
	HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;

}
