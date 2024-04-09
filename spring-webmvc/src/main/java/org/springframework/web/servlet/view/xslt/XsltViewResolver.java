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

package org.springframework.web.servlet.view.xslt;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.URIResolver;
import java.util.Properties;

/**
 * {@link org.springframework.web.servlet.ViewResolver} 实现，通过将提供的视图名称转换为 XSLT 样式表的 URL，
 * 解析 {@link XsltView} 的实例。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class XsltViewResolver extends UrlBasedViewResolver {
	/**
	 * XSLT 源的模型属性的名称
	 */
	@Nullable
	private String sourceKey;

	/**
	 * URL解析器
	 */
	@Nullable
	private URIResolver uriResolver;

	/**
	 * 错误监听器
	 */
	@Nullable
	private ErrorListener errorListener;

	/**
	 * 设置 XSLT 转换器在输出结果树时是否可以添加额外的空格。
	 */
	private boolean indent = true;

	/**
	 * 输出的属性
	 */
	@Nullable
	private Properties outputProperties;

	/**
	 * 是否缓存模板
	 */
	private boolean cacheTemplates = true;


	/**
	 * 此解析器需要 {@link XsltView}。
	 */
	public XsltViewResolver() {
		setViewClass(requiredViewClass());
	}


	/**
	 * 设置表示 XSLT 源的模型属性的名称。
	 * 如果未指定，则将搜索模型映射以查找匹配的值类型。
	 * 支持以下源类型：{@link javax.xml.transform.Source}、{@link org.w3c.dom.Document}、
	 * {@link org.w3c.dom.Node}、{@link java.io.Reader}、{@link java.io.InputStream}
	 * 和 {@link org.springframework.core.io.Resource}。
	 */
	public void setSourceKey(String sourceKey) {
		this.sourceKey = sourceKey;
	}

	/**
	 * 设置用于转换的 URIResolver。
	 * URIResolver 处理对 XSLT {@code document()} 函数的调用。
	 */
	public void setUriResolver(URIResolver uriResolver) {
		this.uriResolver = uriResolver;
	}

	/**
	 * 设置实现 {@link javax.xml.transform.ErrorListener} 接口的错误处理和警告的自定义处理程序。
	 * 如果未设置，将使用默认的 {@link org.springframework.util.xml.SimpleTransformErrorListener}，
	 * 该监听器仅使用视图类的记录器实例记录警告，并重新抛出错误以终止 XML 转换。
	 */
	public void setErrorListener(ErrorListener errorListener) {
		this.errorListener = errorListener;
	}

	/**
	 * 设置 XSLT 转换器在输出结果树时是否可以添加额外的空格。
	 * 默认为 {@code true}（开启）；将其设置为 {@code false}（关闭）以不指定 "indent" 键，将选择权留给样式表。
	 */
	public void setIndent(boolean indent) {
		this.indent = indent;
	}

	/**
	 * 设置要应用于样式表的任意转换器输出属性。
	 * 此处指定的任何值将覆盖此视图在程序上设置的默认值。
	 */
	public void setOutputProperties(Properties outputProperties) {
		this.outputProperties = outputProperties;
	}

	/**
	 * 打开/关闭 XSLT 模板的缓存。
	 * 默认值为 "true"。仅在开发环境中将其设置为 "false"，其中缓存不会严重影响性能。
	 */
	public void setCacheTemplates(boolean cacheTemplates) {
		this.cacheTemplates = cacheTemplates;
	}


	@Override
	protected Class<?> requiredViewClass() {
		return XsltView.class;
	}

	@Override
	protected AbstractUrlBasedView instantiateView() {
		return (getViewClass() == XsltView.class ? new XsltView() : super.instantiateView());
	}

	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		// 构建 XSLT 视图
		XsltView view = (XsltView) super.buildView(viewName);
		// 设置各项属性
		if (this.sourceKey != null) {
			view.setSourceKey(this.sourceKey);
		}
		if (this.uriResolver != null) {
			view.setUriResolver(this.uriResolver);
		}
		if (this.errorListener != null) {
			view.setErrorListener(this.errorListener);
		}
		view.setIndent(this.indent);
		if (this.outputProperties != null) {
			view.setOutputProperties(this.outputProperties);
		}
		view.setCacheTemplates(this.cacheTemplates);
		return view;
	}

}
