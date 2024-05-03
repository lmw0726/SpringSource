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

package org.springframework.web.servlet.tags;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.support.JspAwareRequestContext;
import org.springframework.web.servlet.support.RequestContext;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

/**
 * 所有需要 {@link RequestContext} 的标签的超类。
 *
 * <p>{@code RequestContext} 实例提供了对当前状态的简单访问，
 * 如 {@link org.springframework.web.context.WebApplicationContext}、
 * {@link java.util.Locale}、{@link org.springframework.ui.context.Theme} 等。
 *
 * <p>主要用于 {@link org.springframework.web.servlet.DispatcherServlet} 请求；
 * 在 {@code DispatcherServlet} 外部使用时会使用回退。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.web.servlet.support.RequestContext
 * @see org.springframework.web.servlet.DispatcherServlet
 */
@SuppressWarnings("serial")
public abstract class RequestContextAwareTag extends TagSupport implements TryCatchFinally {

	/**
	 * 用于页面级别的 {@link RequestContext} 实例的 {@link javax.servlet.jsp.PageContext} 属性。
	 */
	public static final String REQUEST_CONTEXT_PAGE_ATTRIBUTE =
			"org.springframework.web.servlet.tags.REQUEST_CONTEXT";


	/**
	 * 子类可用的日志记录器。
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 请求上下文
	 */
	@Nullable
	private RequestContext requestContext;


	/**
	 * 创建并公开当前 RequestContext。
	 * 委托给 {@link #doStartTagInternal()} 执行实际工作。
	 *
	 * @see #REQUEST_CONTEXT_PAGE_ATTRIBUTE
	 * @see org.springframework.web.servlet.support.JspAwareRequestContext
	 */
	@Override
	public final int doStartTag() throws JspException {
		try {
			// 获取请求上下文
			this.requestContext = (RequestContext) this.pageContext.getAttribute(REQUEST_CONTEXT_PAGE_ATTRIBUTE);
			// 如果请求上下文为空，则创建一个新的JspAwareRequestContext
			if (this.requestContext == null) {
				this.requestContext = new JspAwareRequestContext(this.pageContext);
				// 将请求上下文设置到页面上下文中
				this.pageContext.setAttribute(REQUEST_CONTEXT_PAGE_ATTRIBUTE, this.requestContext);
			}
			// 执行内部开始标签处理逻辑
			return doStartTagInternal();
		} catch (JspException | RuntimeException ex) {
			// 捕获JspException或RuntimeException并记录错误日志
			logger.error(ex.getMessage(), ex);
			// 抛出异常
			throw ex;
		} catch (Exception ex) {
			// 捕获其他异常并记录错误日志
			logger.error(ex.getMessage(), ex);
			// 抛出JspTagException异常
			throw new JspTagException(ex.getMessage());
		}
	}

	/**
	 * 返回当前 RequestContext。
	 */
	protected final RequestContext getRequestContext() {
		Assert.state(this.requestContext != null, "No current RequestContext");
		return this.requestContext;
	}

	/**
	 * 由 doStartTag 调用以执行实际工作。
	 *
	 * @return 与 TagSupport.doStartTag 相同
	 * @throws Exception 任何异常，除了 JspException 之外的任何已检查异常都会由 doStartTag 包装在 JspException 中
	 * @see javax.servlet.jsp.tagext.TagSupport#doStartTag
	 */
	protected abstract int doStartTagInternal() throws Exception;


	@Override
	public void doCatch(Throwable throwable) throws Throwable {
		throw throwable;
	}

	@Override
	public void doFinally() {
		this.requestContext = null;
	}

}
