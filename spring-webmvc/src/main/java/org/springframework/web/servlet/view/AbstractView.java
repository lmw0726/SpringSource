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

package org.springframework.web.servlet.view;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.ContextExposingHttpServletRequest;
import org.springframework.web.context.support.WebApplicationObjectSupport;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.support.RequestContext;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * {@link org.springframework.web.servlet.View} 实现的抽象基类。
 * 子类应该是 JavaBean，以便于作为 Spring 管理的 Bean 实例方便配置。
 *
 * <p>提供对静态属性的支持，以便在视图中使用，有多种方式指定它们。
 * 静态属性将与给定的动态属性（控制器返回的模型）在每次渲染操作中合并。
 *
 * <p>扩展了 {@link WebApplicationObjectSupport}，对一些视图会很有帮助。子类只需实现实际的渲染即可。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setAttributes
 * @see #setAttributesMap
 * @see #renderMergedOutputModel
 */
public abstract class AbstractView extends WebApplicationObjectSupport implements View, BeanNameAware {

	/**
	 * 默认内容类型。可作为 Bean 属性进行重写。
	 */
	public static final String DEFAULT_CONTENT_TYPE = "text/html;charset=ISO-8859-1";

	/**
	 * 临时输出字节数组的初始大小（如果有）。
	 */
	private static final int OUTPUT_BYTE_ARRAY_INITIAL_SIZE = 4096;


	/**
	 * 内容类型
	 */
	@Nullable
	private String contentType = DEFAULT_CONTENT_TYPE;

	/**
	 * 请求上下文属性
	 */
	@Nullable
	private String requestContextAttribute;

	/**
	 * 静态属性
	 */
	private final Map<String, Object> staticAttributes = new LinkedHashMap<>();

	/**
	 * 暴露的路径变量
	 */
	private boolean exposePathVariables = true;

	/**
	 * 是否将上下文Bean暴露为属性
	 */
	private boolean exposeContextBeansAsAttributes = false;

	/**
	 * 暴露的上下文bean名称集合
	 */
	@Nullable
	private Set<String> exposedContextBeanNames;

	/**
	 * 视图的bean名称
	 */
	@Nullable
	private String beanName;


	/**
	 * 设置此视图的内容类型。
	 * 默认为 "text/html;charset=ISO-8859-1"。
	 * <p>如果视图本身被假定设置内容类型，则子类可能会忽略它，
	 * 例如在 JSP 的情况下。
	 */
	public void setContentType(@Nullable String contentType) {
		this.contentType = contentType;
	}

	/**
	 * 返回此视图的内容类型。
	 */
	@Override
	@Nullable
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * 设置此视图的 RequestContext 属性的名称。
	 * 默认为无。
	 */
	public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
	 * 返回 RequestContext 属性的名称，如果有的话。
	 */
	@Nullable
	public String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
	 * 将静态属性设置为 CSV 字符串。
	 * 格式为：attname0={value1},attname1={value1}
	 * <p>"静态"属性是在视图实例配置中指定的固定属性。
	 * 另一方面，“动态”属性是作为模型的一部分传递的值。
	 *
	 * @param propString CSV 字符串表示的属性
	 * @throws IllegalArgumentException 如果属性字符串为空，或者属性格式不正确
	 */
	public void setAttributesCSV(@Nullable String propString) throws IllegalArgumentException {
		if (propString != null) {
			// 使用逗号分隔属性字符串
			StringTokenizer st = new StringTokenizer(propString, ",");
			// 遍历每个属性
			while (st.hasMoreTokens()) {
				// 获取下一个属性
				String tok = st.nextToken();
				// 查找等号索引
				int eqIdx = tok.indexOf('=');
				// 如果没有找到等号，则抛出异常
				if (eqIdx == -1) {
					throw new IllegalArgumentException(
							"Expected '=' in attributes CSV string '" + propString + "'");
				}
				// 确保属性字符串至少包含 2 个字符
				if (eqIdx >= tok.length() - 2) {
					throw new IllegalArgumentException(
							"At least 2 characters ([]) required in attributes CSV string '" + propString + "'");
				}
				// 提取属性名
				String name = tok.substring(0, eqIdx);
				// 提取属性值（删除值的首尾字符 '{' 和 '}'）
				int beginIndex = eqIdx + 2;
				int endIndex = tok.length() - 1;
				String value = tok.substring(beginIndex, endIndex);

				// 添加静态属性
				addStaticAttribute(name, value);
			}
		}
	}

	/**
	 * 从 {@code java.util.Properties} 对象设置此视图的静态属性。
	 * <p>“静态”属性是在视图实例配置中指定的固定属性。另一方面，“动态”属性是作为模型的一部分传递的值。
	 * <p>这是设置静态属性的最方便的方法。请注意，如果模型中包含具有相同名称的值，则静态属性可以被动态属性覆盖。
	 * <p>可以使用字符串“value”（通过 PropertiesEditor 解析）或 XML bean 定义中的“props”元素填充。
	 *
	 * @see org.springframework.beans.propertyeditors.PropertiesEditor
	 */
	public void setAttributes(Properties attributes) {
		CollectionUtils.mergePropertiesIntoMap(attributes, this.staticAttributes);
	}

	/**
	 * 从 Map 设置此视图的静态属性。这允许设置任何类型的属性值，例如 bean 引用。
	 * <p>“静态”属性是在视图实例配置中指定的固定属性。另一方面，“动态”属性是作为模型的一部分传递的值。
	 * <p>可以使用 XML bean 定义中的“map”或“props”元素填充。
	 *
	 * @param attributes 一个 Map，其中键为名称字符串，值为属性对象
	 */
	public void setAttributesMap(@Nullable Map<String, ?> attributes) {
		if (attributes != null) {
			attributes.forEach(this::addStaticAttribute);
		}
	}

	/**
	 * 允许访问此视图的静态属性的 Map，可选择添加或覆盖特定条目。
	 * <p>通过"attributesMap[myKey]"等方式直接指定条目非常有用。
	 * 这对于在子视图定义中添加或覆盖条目特别有用。
	 *
	 * @return 此视图的静态属性 Map
	 */
	public Map<String, Object> getAttributesMap() {
		return this.staticAttributes;
	}

	/**
	 * 向此视图添加静态数据，使每个视图都可以访问。
	 * <p>"静态"属性是在视图实例配置中指定的固定属性。
	 * 另一方面，“动态”属性是作为模型的一部分传递的值。
	 * <p>必须在调用 {@code render} 之前调用。
	 *
	 * @param name  要公开的属性名称
	 * @param value 要公开的属性值
	 * @see #render
	 */
	public void addStaticAttribute(String name, Object value) {
		this.staticAttributes.put(name, value);
	}

	/**
	 * 返回此视图的静态属性。用于测试很方便。
	 * <p>返回一个不可修改的 Map，因为这不是用于操作 Map，而是用于仅仅检查其内容。
	 *
	 * @return 此视图中的静态属性
	 */
	public Map<String, Object> getStaticAttributes() {
		return Collections.unmodifiableMap(this.staticAttributes);
	}

	/**
	 * 指定是否向模型添加路径变量。
	 * <p>路径变量通常通过 {@code @PathVariable} 注解绑定到 URI 模板变量。
	 * 它们实际上是应用于它们的类型转换以得到类型化的 Object 值的 URI 模板变量。
	 * 这样的值经常在视图中用于构建链接到相同和其他 URL。
	 * <p>添加到模型的路径变量会覆盖静态属性（参见 {@link #setAttributes(Properties)}），
	 * 但不会覆盖模型中已经存在的属性。
	 * <p>默认情况下，此标志设置为 {@code true}。具体的视图类型可以覆盖此设置。
	 *
	 * @param exposePathVariables {@code true} 表示公开路径变量，{@code false} 表示不公开
	 */
	public void setExposePathVariables(boolean exposePathVariables) {
		this.exposePathVariables = exposePathVariables;
	}

	/**
	 * 返回是否向模型添加路径变量。
	 *
	 * @return 是否向模型公开路径变量
	 */
	public boolean isExposePathVariables() {
		return this.exposePathVariables;
	}

	/**
	 * 设置是否使应用程序上下文中的所有 Spring Bean 可访问为请求属性，
	 * 通过在访问属性时进行延迟检查。
	 * <p>这将使所有这些 Bean 在 JSP 2.0 页面中的普通 ${...} 表达式以及
	 * JSTL 的 c:out 值表达式中可访问。
	 * <p>默认为 "false"。将此标志打开以透明地公开请求属性命名空间中的所有 Spring Bean。
	 * <p><b>注意:</b> 上下文 Bean 将覆盖手动添加的具有相同名称的任何自定义请求或会话属性。
	 * 但是，模型属性（作为对此视图明确公开的）的相同名称将始终覆盖上下文 Bean。
	 *
	 * @see #getRequestToExpose
	 */
	public void setExposeContextBeansAsAttributes(boolean exposeContextBeansAsAttributes) {
		this.exposeContextBeansAsAttributes = exposeContextBeansAsAttributes;
	}

	/**
	 * 指定上下文中应该公开的 Bean 的名称。如果此值非空，则只有指定的 Bean 有资格作为属性公开。
	 * <p>如果要公开应用程序上下文中的所有 Spring Bean，请打开
	 * {@link #setExposeContextBeansAsAttributes "exposeContextBeansAsAttributes"} 标志，
	 * 但不要为此属性列出特定的 Bean 名称。
	 */
	public void setExposedContextBeanNames(String... exposedContextBeanNames) {
		this.exposedContextBeanNames = new HashSet<>(Arrays.asList(exposedContextBeanNames));
	}

	/**
	 * 设置视图的名称。有助于跟踪。
	 * <p>框架代码在构造视图时必须调用此方法。
	 */
	@Override
	public void setBeanName(@Nullable String beanName) {
		this.beanName = beanName;
	}

	/**
	 * 返回视图的名称。如果视图已正确配置，则不应为 {@code null}。
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}


	/**
	 * 根据指定的模型准备视图，将其与静态属性和 RequestContext 属性合并，如果需要的话。
	 * 将实际的渲染委托给 renderMergedOutputModel。
	 *
	 * @param model    传入的模型
	 * @param request  HttpServletRequest 对象
	 * @param response HttpServletResponse 对象
	 * @throws Exception 如果渲染过程中出现异常
	 * @see #renderMergedOutputModel
	 */
	@Override
	public void render(@Nullable Map<String, ?> model, HttpServletRequest request,
					   HttpServletResponse response) throws Exception {

		if (logger.isDebugEnabled()) {
			// 记录调试信息：视图名称、模型数据以及静态属性
			logger.debug("View " + formatViewName() +
					", model " + (model != null ? model : Collections.emptyMap()) +
					(this.staticAttributes.isEmpty() ? "" : ", static attributes " + this.staticAttributes));
		}

		// 创建合并后的输出模型
		Map<String, Object> mergedModel = createMergedOutputModel(model, request, response);
		// 准备响应
		prepareResponse(request, response);
		// 渲染合并后的输出模型
		renderMergedOutputModel(mergedModel, getRequestToExpose(request), response);
	}

	/**
	 * 创建一个组合输出 Map（永远不会为 {@code null}），其中包含动态值和静态属性。
	 * 动态值优先于静态属性。
	 *
	 * @param model    模型数据，可以为 {@code null}
	 * @param request  HttpServletRequest 对象
	 * @param response HttpServletResponse 对象
	 * @return 组合后的输出 Map
	 */
	protected Map<String, Object> createMergedOutputModel(@Nullable Map<String, ?> model,
														  HttpServletRequest request, HttpServletResponse response) {

		@SuppressWarnings("unchecked")
		// 如果配置了暴露路径变量，则从请求中获取路径变量
		Map<String, Object> pathVars = (this.exposePathVariables ?
				(Map<String, Object>) request.getAttribute(View.PATH_VARIABLES) : null);

		// 合并静态和动态模型属性
		int size = this.staticAttributes.size();
		size += (model != null ? model.size() : 0);
		size += (pathVars != null ? pathVars.size() : 0);

		// 创建新的 LinkedHashMap 来存储合并后的模型数据
		Map<String, Object> mergedModel = CollectionUtils.newLinkedHashMap(size);
		// 添加静态属性
		mergedModel.putAll(this.staticAttributes);
		if (pathVars != null) {
			// 添加路径变量
			mergedModel.putAll(pathVars);
		}
		if (model != null) {
			// 添加动态模型属性
			mergedModel.putAll(model);
		}

		// 如果配置了请求上下文属性，则将请求上下文添加到合并模型中
		if (this.requestContextAttribute != null) {
			mergedModel.put(this.requestContextAttribute, createRequestContext(request, response, mergedModel));
		}

		return mergedModel;
	}

	/**
	 * 创建 RequestContext，并以指定的属性名暴露。
	 * <p>默认实现为给定请求和模型创建标准的 RequestContext 实例。可以在子类中进行覆盖以创建自定义实例。
	 *
	 * @param request  当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @param model    组合后的输出 Map，动态值优先于静态属性
	 * @return RequestContext 实例
	 */
	protected RequestContext createRequestContext(
			HttpServletRequest request, HttpServletResponse response, Map<String, Object> model) {

		return new RequestContext(request, response, getServletContext(), model);
	}

	/**
	 * 为渲染准备给定的响应。
	 * <p>默认实现对通过 HTTPS 发送下载内容的 IE 错误应用了一种解决方法。
	 *
	 * @param request  当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 */
	protected void prepareResponse(HttpServletRequest request, HttpServletResponse response) {
		if (generatesDownloadContent()) {
			response.setHeader("Pragma", "private");
			response.setHeader("Cache-Control", "private, must-revalidate");
		}
	}

	/**
	 * 返回此视图是否生成下载内容
	 * （通常是二进制内容，如 PDF 或 Excel 文件）。
	 * <p>默认实现返回 {@code false}。鼓励子类在此处返回 {@code true}，
	 * 如果它们知道它们生成的是需要在客户端临时缓存的下载内容，
	 * 通常是通过响应 OutputStream。
	 *
	 * @return 是否生成下载内容
	 * @see #prepareResponse
	 * @see javax.servlet.http.HttpServletResponse#getOutputStream()
	 */
	protected boolean generatesDownloadContent() {
		return false;
	}

	/**
	 * 获取要暴露给 {@link #renderMergedOutputModel}（即视图）的请求句柄。
	 * <p>默认实现包装原始请求，以便将 Spring Bean 暴露为请求属性（如果需要）。
	 *
	 * @param originalRequest 引擎提供的原始 Servlet 请求
	 * @return 包装后的请求，如果不需要包装，则返回原始请求
	 * @see #setExposeContextBeansAsAttributes
	 * @see #setExposedContextBeanNames
	 * @see org.springframework.web.context.support.ContextExposingHttpServletRequest
	 */
	protected HttpServletRequest getRequestToExpose(HttpServletRequest originalRequest) {
		// 如果配置了暴露上下文 Bean 属性或者指定了要暴露的上下文 Bean 名称
		if (this.exposeContextBeansAsAttributes || this.exposedContextBeanNames != null) {
			// 获取 WebApplicationContext
			WebApplicationContext wac = getWebApplicationContext();
			Assert.state(wac != null, "No WebApplicationContext");
			// 创建 ContextExposingHttpServletRequest 对象，并将原始请求和 WebApplicationContext 传入
			return new ContextExposingHttpServletRequest(originalRequest, wac, this.exposedContextBeanNames);
		}
		// 如果未配置暴露上下文 Bean 属性，则直接返回原始请求
		return originalRequest;
	}

	/**
	 * 子类必须实现此方法以实际渲染视图。
	 * <p>第一步将是准备请求：在 JSP 情况下，这意味着将模型对象设置为请求属性。
	 * 第二步将是实际渲染视图，例如通过 RequestDispatcher 包含 JSP。
	 *
	 * @param model    组合后的输出 Map（永远不会为 {@code null}），
	 *                 动态值优先于静态属性
	 * @param request  当前 HTTP 请求
	 * @param response 当前 HTTP 响应
	 * @throws Exception 如果渲染失败
	 */
	protected abstract void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception;


	/**
	 * 将给定映射中的模型对象作为请求属性暴露。
	 * 名称将从模型 Map 中获取。
	 * 此方法适用于所有可由 {@link javax.servlet.RequestDispatcher} 访问的资源。
	 *
	 * @param model   要暴露的模型对象 Map
	 * @param request 当前 HTTP 请求
	 */
	protected void exposeModelAsRequestAttributes(Map<String, Object> model,
												  HttpServletRequest request) throws Exception {

		model.forEach((name, value) -> {
			if (value != null) {
				// 如果值不为空，则设置属性名和属性值
				request.setAttribute(name, value);
			} else {
				// 否则移除该属性名
				request.removeAttribute(name);
			}
		});
	}

	/**
	 * 为此视图创建临时的 OutputStream。
	 * <p>这通常用作 IE 的解决方法，用于在将内容实际写入 HTTP 响应之前，
	 * 从临时流中设置内容长度标头。
	 */
	protected ByteArrayOutputStream createTemporaryOutputStream() {
		return new ByteArrayOutputStream(OUTPUT_BYTE_ARRAY_INITIAL_SIZE);
	}

	/**
	 * 将给定的临时 OutputStream 写入 HTTP 响应。
	 *
	 * @param response 当前 HTTP 响应
	 * @param baos     要写入的临时 OutputStream
	 * @throws IOException 如果写入/刷新失败
	 */
	protected void writeToResponse(HttpServletResponse response, ByteArrayOutputStream baos) throws IOException {
		// 写入内容类型以及长度（通过字节数组确定）。
		response.setContentType(getContentType());
		response.setContentLength(baos.size());

		// 将字节数组刷新到 Servlet 输出流中。
		ServletOutputStream out = response.getOutputStream();
		baos.writeTo(out);
		out.flush();
	}

	/**
	 * 将响应的内容类型设置为配置的 {@link #setContentType(String) 内容类型}，
	 * 除非 {@link View#SELECTED_CONTENT_TYPE} 请求属性存在且设置为具体的媒体类型。
	 */
	protected void setResponseContentType(HttpServletRequest request, HttpServletResponse response) {
		// 从请求属性中获取 媒体类型
		MediaType mediaType = (MediaType) request.getAttribute(View.SELECTED_CONTENT_TYPE);
		// 如果 媒体类型 存在且为具体的媒体类型
		if (mediaType != null && mediaType.isConcrete()) {
			// 将响应的内容类型设置为 媒体类型 的字符串表示形式
			response.setContentType(mediaType.toString());
		} else {
			// 否则将响应的内容类型设置为配置的默认内容类型
			response.setContentType(getContentType());
		}
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + formatViewName();
	}

	protected String formatViewName() {
		return (getBeanName() != null ? "name '" + getBeanName() + "'" : "[" + getClass().getSimpleName() + "]");
	}

}
