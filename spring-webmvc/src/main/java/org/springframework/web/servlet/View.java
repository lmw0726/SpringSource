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

package org.springframework.web.servlet;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Web 交互的 MVC 视图。实现类负责渲染内容，并公开模型。一个单独的视图公开多个模型属性。
 *
 * <p>这个类及与之相关的 MVC 方法在 Rod Johnson 的《Expert One-On-One J2EE Design and Development》
 * （Wrox, 2002）一书的第 12 章中有所讨论。
 *
 * <p>视图实现可能大不相同。一个明显的实现是基于 JSP 的。其他实现可能是基于 XSLT 的，或者使用 HTML 生成库。
 * 此接口旨在避免限制可能的实现范围。
 *
 * <p>视图应该是 bean。它们很可能会被 ViewResolver 实例化为 bean。由于此接口是无状态的，视图实现应该是线程安全的。
 *
 * @author Rod Johnson
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @see org.springframework.web.servlet.view.AbstractView
 * @see org.springframework.web.servlet.view.InternalResourceView
 */
public interface View {

	/**
	 * 包含响应状态码的 {@link HttpServletRequest} 属性的名称。
	 * <p>注意: 并不是所有的 View 实现都要求支持此属性。
	 *
	 * @since 3.0
	 */
	String RESPONSE_STATUS_ATTRIBUTE = View.class.getName() + ".responseStatus";

	/**
	 * 包含带有路径变量的 Map 的 {@link HttpServletRequest} 属性的名称。
	 * 该 Map 由基于 String 的 URI 模板变量名称作为键和它们对应的对象值组成，
	 * 这些值是从 URL 的段中提取的并进行类型转换的。
	 * <p>注意: 并不是所有的 View 实现都要求支持此属性。
	 *
	 * @since 3.1
	 */
	String PATH_VARIABLES = View.class.getName() + ".pathVariables";

	/**
	 * 内容协商期间选择的 {@link org.springframework.http.MediaType}，
	 * 可能比 View 配置的更具体。例如: "application/vnd.example-v1+xml" vs "application/*+xml"。
	 *
	 * @since 3.2
	 */
	String SELECTED_CONTENT_TYPE = View.class.getName() + ".selectedContentType";


	/**
	 * 如果预先确定了视图的内容类型，则返回视图的内容类型。
	 * <p>可以用来提前检查视图的内容类型，
	 * 即在实际渲染尝试之前。
	 *
	 * @return 内容类型字符串（可选包含字符集），
	 * 如果没有预先确定，则为 {@code null}
	 */
	@Nullable
	default String getContentType() {
		return null;
	}

	/**
	 * 根据指定的模型渲染视图。
	 * <p>第一步将是准备请求: 在 JSP 情况下，这意味着将模型对象设置为请求属性。
	 * 第二步将是实际渲染视图，例如通过 RequestDispatcher 包含 JSP。
	 *
	 * @param model    以名称字符串作为键和相应模型对象作为值的 Map
	 *                 （在空模型的情况下，Map 也可以为 {@code null}）
	 * @param request  当前 HTTP 请求
	 * @param response 正在构建的 HTTP 响应
	 * @throws Exception 如果渲染失败
	 */
	void render(@Nullable Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
