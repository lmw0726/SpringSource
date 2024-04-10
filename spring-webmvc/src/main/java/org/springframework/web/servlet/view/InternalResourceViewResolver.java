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

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * 方便的 {@link UrlBasedViewResolver} 子类，支持 {@link InternalResourceView}（即 Servlet 和 JSP）以及
 * {@link JstlView} 等子类。
 *
 * <p>通过 {@link #setViewClass} 可以指定此解析器生成的所有视图的视图类。
 * 有关详细信息，请参阅 {@link UrlBasedViewResolver} 的文档。
 * 默认为 {@link InternalResourceView}，如果 JSTL API 存在，则为 {@link JstlView}。
 *
 * <p>顺便说一句，将仅用作视图的 JSP 文件放在 WEB-INF 下是一种良好的做法，以防止它们被直接访问（例如，通过手动输入的 URL）。
 * 只有控制器才能访问它们。
 *
 * <p><b>注意：</b>在链接视图解析器时，{@link InternalResourceViewResolver} 必须始终放在最后，因为它将尝试解析任何视图名称，
 * 无论基础资源是否实际存在。
 *
 * @author Juergen Hoeller
 * @see #setViewClass
 * @see #setPrefix
 * @see #setSuffix
 * @see #setRequestContextAttribute
 * @see InternalResourceView
 * @see JstlView
 * @since 17.02.2003
 */
public class InternalResourceViewResolver extends UrlBasedViewResolver {

	/**
	 * jstl是否存在
	 */
	private static final boolean jstlPresent = ClassUtils.isPresent(
			"javax.servlet.jsp.jstl.core.Config", InternalResourceViewResolver.class.getClassLoader());

	/**
	 * 指定是否始终包含视图而不是转发到它。
	 */
	@Nullable
	private Boolean alwaysInclude;


	/**
	 * 将默认的 {@link #setViewClass 视图类} 设置为 {@link #requiredViewClass}：
	 * 默认为 {@link InternalResourceView}，如果存在 JSTL API，则为 {@link JstlView}。
	 */
	public InternalResourceViewResolver() {
		// 获取所需的视图类
		Class<?> viewClass = requiredViewClass();
		// 如果所需的视图类是 InternalResourceView，并且 JSTL 存在，则将视图类设置为 JstlView
		if (InternalResourceView.class == viewClass && jstlPresent) {
			viewClass = JstlView.class;
		}
		// 设置视图类
		setViewClass(viewClass);
	}

	/**
	 * 方便的构造函数，允许将 {@link #setPrefix 前缀} 和 {@link #setSuffix 后缀} 作为构造函数参数进行指定。
	 *
	 * @param prefix 构建 URL 时要添加到视图名称前面的前缀
	 * @param suffix 构建 URL 时要添加到视图名称后面的后缀
	 * @since 4.3
	 */
	public InternalResourceViewResolver(String prefix, String suffix) {
		this();
		setPrefix(prefix);
		setSuffix(suffix);
	}


	/**
	 * 指定是否始终包含视图而不是转发到它。
	 * <p>默认值为 "false"。将此标志打开以强制使用 Servlet 包含，即使可能可以使用转发。
	 *
	 * @see InternalResourceView#setAlwaysInclude
	 */
	public void setAlwaysInclude(boolean alwaysInclude) {
		this.alwaysInclude = alwaysInclude;
	}


	@Override
	protected Class<?> requiredViewClass() {
		return InternalResourceView.class;
	}

	@Override
	protected AbstractUrlBasedView instantiateView() {
		if (getViewClass() == InternalResourceView.class) {
			return new InternalResourceView();
		}
		if (getViewClass() == JstlView.class) {
			return new JstlView();
		}
		return super.instantiateView();
	}

	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		// 构建内部资源视图
		InternalResourceView view = (InternalResourceView) super.buildView(viewName);
		// 如果 alwaysInclude 属性不为 null，则设置视图的 alwaysInclude 属性
		if (this.alwaysInclude != null) {
			view.setAlwaysInclude(this.alwaysInclude);
		}
		// 设置阻止视图循环分发
		view.setPreventDispatchLoop(true);
		return view;
	}

}
