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

package org.springframework.web.servlet.handler;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * {@link org.springframework.web.servlet.HandlerExceptionResolver} 实现，
 * 允许将异常类名映射到视图名称，可以为一组给定的处理程序或 DispatcherServlet 中的所有处理程序。
 *
 * <p>错误视图类似于错误页面 JSP，但可以与任何类型的异常一起使用，包括任何已检查的异常，
 * 并为特定处理程序提供细粒度的映射。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @see org.springframework.web.servlet.DispatcherServlet
 * @since 22.11.2003
 */
public class SimpleMappingExceptionResolver extends AbstractHandlerExceptionResolver {

	/**
	 * 异常属性的默认名称："exception"。
	 */
	public static final String DEFAULT_EXCEPTION_ATTRIBUTE = "exception";

	/**
	 * 异常类名与错误视图名称之间的映射关系
	 */
	@Nullable
	private Properties exceptionMappings;

	/**
	 * 从异常映射中排除的一个或多个异常
	 */
	@Nullable
	private Class<?>[] excludedExceptions;

	/**
	 * 默认错误视图的名称
	 */
	@Nullable
	private String defaultErrorView;

	/**
	 * 异常解析器将应用的默认 HTTP 状态代码
	 */
	@Nullable
	private Integer defaultStatusCode;

	/**
	 * 视图名称与状态码映射
	 */
	private Map<String, Integer> statusCodes = new HashMap<>();

	/**
	 * 将异常公开为的模型属性的名称
	 */
	@Nullable
	private String exceptionAttribute = DEFAULT_EXCEPTION_ATTRIBUTE;


	/**
	 * 设置异常类名与错误视图名称之间的映射关系。
	 * 异常类名可以是子字符串，目前不支持通配符。
	 * 例如，值为 "ServletException" 将匹配 {@code javax.servlet.ServletException} 及其子类。
	 * <p><b>注意：</b>请仔细考虑模式的具体性，以及是否包含包信息（这不是必需的）。
	 * 例如，"Exception" 将匹配几乎任何异常，并可能隐藏其他规则。
	 * 如果 "Exception" 意味着为所有已检查的异常定义规则，则 "java.lang.Exception" 将是正确的。
	 * 对于更不寻常的异常名称，如 "BaseBusinessException"，不需要使用全限定名。
	 *
	 * @param mappings 异常模式（也可以是完全限定的类名）作为键，错误视图名称作为值
	 */
	public void setExceptionMappings(Properties mappings) {
		this.exceptionMappings = mappings;
	}

	/**
	 * 设置要从异常映射中排除的一个或多个异常。
	 * 首先检查排除的异常，如果其中一个等于实际异常，则该异常将保持未解析状态。
	 *
	 * @param excludedExceptions 要排除的一个或多个异常类型
	 */
	public void setExcludedExceptions(Class<?>... excludedExceptions) {
		this.excludedExceptions = excludedExceptions;
	}

	/**
	 * 设置默认错误视图的名称。
	 * 如果找不到特定映射，将返回此视图。
	 * <p>默认为无。
	 */
	public void setDefaultErrorView(String defaultErrorView) {
		this.defaultErrorView = defaultErrorView;
	}

	/**
	 * 设置此异常解析器为给定已解析错误视图应用的 HTTP 状态码。
	 * 键是视图名称；值是状态码。
	 * <p>请注意，在顶级请求的情况下才会应用此错误代码。
	 * 在 include 请求的情况下，不会设置该错误代码，因为无法在 include 中修改 HTTP 状态。
	 * <p>如果未指定，默认状态码将被应用。
	 *
	 * @see #setDefaultStatusCode(int)
	 */
	public void setStatusCodes(Properties statusCodes) {
		for (Enumeration<?> enumeration = statusCodes.propertyNames(); enumeration.hasMoreElements(); ) {
			// 获取视图名称
			String viewName = (String) enumeration.nextElement();
			// 根据视图名称获取Properties中的状态码
			Integer statusCode = Integer.valueOf(statusCodes.getProperty(viewName));
			// 将视图视图名称和状态码添加到状态码映射中
			this.statusCodes.put(viewName, statusCode);
		}
	}

	/**
	 * 用于 Java 配置的 {@link #setStatusCodes(Properties)} 的替代方法。
	 */
	public void addStatusCode(String viewName, int statusCode) {
		this.statusCodes.put(viewName, statusCode);
	}

	/**
	 * 返回通过 {@link #setStatusCodes(Properties)} 提供的 HTTP 状态码。
	 * 键是视图名称；值是状态码。
	 */
	public Map<String, Integer> getStatusCodesAsMap() {
		return Collections.unmodifiableMap(this.statusCodes);
	}

	/**
	 * 设置此异常解析器将应用的默认 HTTP 状态代码，如果它解析了一个错误视图，并且没有定义状态代码映射。
	 * <p>请注意，在顶级请求的情况下才会应用此错误代码。
	 * 在 include 请求的情况下，不会设置该错误代码，因为无法在 include 中修改 HTTP 状态。
	 * <p>如果未指定，将不会应用任何状态代码，要么将其留给控制器或视图，要么保持 servlet 引擎的默认值为 200（OK）。
	 *
	 * @param defaultStatusCode HTTP 状态码值，例如 500（{@link HttpServletResponse#SC_INTERNAL_SERVER_ERROR}）或 404（{@link HttpServletResponse#SC_NOT_FOUND}）
	 * @see #setStatusCodes(Properties)
	 */
	public void setDefaultStatusCode(int defaultStatusCode) {
		this.defaultStatusCode = defaultStatusCode;
	}

	/**
	 * 设置应将异常公开为的模型属性的名称。
	 * 默认为 "exception"。
	 * <p>可以将其设置为不同的属性名称，也可以设置为 {@code null}，表示根本不公开异常属性。
	 *
	 * @see #DEFAULT_EXCEPTION_ATTRIBUTE
	 */
	public void setExceptionAttribute(@Nullable String exceptionAttribute) {
		this.exceptionAttribute = exceptionAttribute;
	}


	/**
	 * 实际解析在处理程序执行期间抛出的给定异常，如果适用，则返回表示特定错误页面的 ModelAndView。
	 * <p>可以在子类中重写，以应用特定的异常检查。
	 * 请注意，此模板方法将在检查此解析器是否适用（"mappedHandlers" 等）之后被调用，因此实现可能只需继续其实际的异常处理。
	 *
	 * @param request  当前的 HTTP 请求
	 * @param response 当前的 HTTP 响应
	 * @param handler  已执行的处理程序，如果在异常发生时没有选择任何处理程序（例如，如果多部分解析失败），则为 {@code null}
	 * @param ex       在处理程序执行期间抛出的异常
	 * @return 相应的 {@code ModelAndView}，用于转发，或者在解析链中进行默认处理时返回 {@code null}
	 */
	@Override
	@Nullable
	protected ModelAndView doResolveException(
			HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {

		// 为选择的错误视图公开 ModelAndView。
		// 根据异常和Http请求推断视图名称
		String viewName = determineViewName(ex, request);
		if (viewName != null) {
			// 如果指定了错误视图的 HTTP 状态代码，则应用它。
			// 仅当我们处理顶级请求时才应用它。
			Integer statusCode = determineStatusCode(request, viewName);
			if (statusCode != null) {
				// 如果存在状态码，将状态码应用到Http响应中
				applyStatusCodeIfPossible(request, response, statusCode);
			}
			// 获取模型和视图
			return getModelAndView(viewName, ex, request);
		} else {
			// 视图未知，返回 null
			return null;
		}
	}

	/**
	 * 确定给定异常的视图名称，首先检查是否与 {@link #setExcludedExceptions(Class[]) "excludedExecptions"} 匹配，
	 * 然后搜索 {@link #setExceptionMappings "exceptionMappings"}，最后将 {@link #setDefaultErrorView "defaultErrorView"} 用作后备。
	 *
	 * @param ex      在处理程序执行期间抛出的异常
	 * @param request 当前的 HTTP 请求（用于获取元数据）
	 * @return 已解析的视图名称；如果被排除或找不到，则返回 {@code null}
	 */
	@Nullable
	protected String determineViewName(Exception ex, HttpServletRequest request) {
		String viewName = null;
		if (this.excludedExceptions != null) {
			// 如果要排除的异常存在
			for (Class<?> excludedEx : this.excludedExceptions) {
				// 遍历要排除的异常列表，获取要排除的异常类
				if (excludedEx.equals(ex.getClass())) {
					//  如果异常类与当前异常相同，则返回 null
					return null;
				}
			}
		}
		// 检查特定异常映射。
		if (this.exceptionMappings != null) {
			// 如果异常映射存在，则寻找匹配的视图名称
			viewName = findMatchingViewName(this.exceptionMappings, ex);
		}
		// 返回默认错误视图，如果已定义。
		if (viewName == null && this.defaultErrorView != null) {
			// 如果视图名称找不到，并且默认的错误视图存在，则返回默认的错误视图
			if (logger.isDebugEnabled()) {
				logger.debug("Resolving to default view '" + this.defaultErrorView + "'");
			}
			viewName = this.defaultErrorView;
		}
		return viewName;
	}

	/**
	 * 在给定的异常映射中查找匹配的视图名称。
	 *
	 * @param exceptionMappings 异常类名与错误视图名称之间的映射关系
	 * @param ex                在处理程序执行期间抛出的异常
	 * @return 视图名称；如果找不到，则返回 {@code null}
	 * @see #setExceptionMappings
	 */
	@Nullable
	protected String findMatchingViewName(Properties exceptionMappings, Exception ex) {
		// 初始化视图名称和主要映射
		String viewName = null;
		String dominantMapping = null;
		// 初始化最大深度为整数最大值
		int deepest = Integer.MAX_VALUE;
		// 遍历异常映射的属性名
		for (Enumeration<?> names = exceptionMappings.propertyNames(); names.hasMoreElements(); ) {
			// 获取异常映射的属性名
			String exceptionMapping = (String) names.nextElement();
			// 计算当前异常映射的深度
			int depth = getDepth(exceptionMapping, ex);
			// 如果深度大于等于0，且（深度小于最大深度或（深度等于最大深度，且主要映射不为空，且异常映射的长度大于主要映射的长度））
			if (depth >= 0 && (depth < deepest || (depth == deepest &&
					dominantMapping != null && exceptionMapping.length() > dominantMapping.length()))) {
				// 更新最大深度、主要映射和视图名称
				deepest = depth;
				dominantMapping = exceptionMapping;
				// 更新视图名称
				viewName = exceptionMappings.getProperty(exceptionMapping);
			}
		}
		// 如果视图名称不为空且日志记录级别为DEBUG，则打印调试信息
		if (viewName != null && logger.isDebugEnabled()) {
			logger.debug("Resolving to view '" + viewName + "' based on mapping [" + dominantMapping + "]");
		}
		// 返回视图名称
		return viewName;

	}

	/**
	 * 返回匹配的超类的深度。
	 * <p>0 表示 ex 完全匹配。如果没有匹配，则返回 -1。
	 * 否则，返回深度。最低深度获胜。
	 */
	protected int getDepth(String exceptionMapping, Exception ex) {
		return getDepth(exceptionMapping, ex.getClass(), 0);
	}

	private int getDepth(String exceptionMapping, Class<?> exceptionClass, int depth) {
		if (exceptionClass.getName().contains(exceptionMapping)) {
			// 找到了！
			return depth;
		}
		// 如果我们已经走到尽头但还没找到...
		if (exceptionClass == Throwable.class) {
			return -1;
		}
		// 递归获取父类，并取得匹配类型的最大深度。
		return getDepth(exceptionMapping, exceptionClass.getSuperclass(), depth + 1);
	}

	/**
	 * 确定要应用于给定错误视图的 HTTP 状态代码。
	 * <p>默认实现返回给定视图名称的状态代码（通过 {@link #setStatusCodes(Properties) statusCodes} 属性指定），
	 * 如果没有匹配，则回退到 {@link #setDefaultStatusCode defaultStatusCode}。
	 * <p>在自定义子类中重写此方法以自定义此行为。
	 *
	 * @param request  当前的 HTTP 请求
	 * @param viewName 错误视图的名称
	 * @return 要使用的 HTTP 状态代码；如果是 servlet 容器的默认值，则返回 {@code null}（标准错误视图的情况下为 200）
	 * @see #setDefaultStatusCode
	 * @see #applyStatusCodeIfPossible
	 */
	@Nullable
	protected Integer determineStatusCode(HttpServletRequest request, String viewName) {
		if (this.statusCodes.containsKey(viewName)) {
			// 如果状态码映射包含这个视图名，则返回该视图名对应的状态码
			return this.statusCodes.get(viewName);
		}
		// 否则，返回默认的状态码
		return this.defaultStatusCode;
	}

	/**
	 * 如果可能的话（即不在包含请求中执行），将指定的 HTTP 状态代码应用于给定的响应。
	 *
	 * @param request    当前的 HTTP 请求
	 * @param response   当前的 HTTP 响应
	 * @param statusCode 要应用的状态代码
	 * @see #determineStatusCode
	 * @see #setDefaultStatusCode
	 * @see HttpServletResponse#setStatus
	 */
	protected void applyStatusCodeIfPossible(HttpServletRequest request, HttpServletResponse response, int statusCode) {
		// 检查请求是否不是包含请求
		if (!WebUtils.isIncludeRequest(request)) {
			// 如果日志记录级别为DEBUG，则打印调试信息
			if (logger.isDebugEnabled()) {
				logger.debug("Applying HTTP status " + statusCode);
			}
			// 设置响应状态码
			response.setStatus(statusCode);
			// 在请求中设置错误状态码属性
			request.setAttribute(WebUtils.ERROR_STATUS_CODE_ATTRIBUTE, statusCode);
		}
	}

	/**
	 * 为给定的请求、视图名称和异常返回一个 ModelAndView。
	 * <p>默认实现委托给 {@link #getModelAndView(String, Exception)}。
	 *
	 * @param viewName 错误视图的名称
	 * @param ex       在处理程序执行期间抛出的异常
	 * @param request  当前的 HTTP 请求（用于获取元数据）
	 * @return ModelAndView 实例
	 */
	protected ModelAndView getModelAndView(String viewName, Exception ex, HttpServletRequest request) {
		return getModelAndView(viewName, ex);
	}

	/**
	 * 为给定的视图名称和异常返回一个 ModelAndView。
	 * <p>默认实现添加指定的异常属性。
	 * 可以在子类中重写。
	 *
	 * @param viewName 错误视图的名称
	 * @param ex       在处理程序执行期间抛出的异常
	 * @return ModelAndView 实例
	 * @see #setExceptionAttribute
	 */
	protected ModelAndView getModelAndView(String viewName, Exception ex) {
		// 根据视图名称构建 ModelAndView
		ModelAndView mv = new ModelAndView(viewName);
		if (this.exceptionAttribute != null) {
			// 异常属性名存在，添加异常属性名和异常
			mv.addObject(this.exceptionAttribute, ex);
		}
		// 返回 ModelAndView
		return mv;
	}

}
