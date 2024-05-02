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

package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;
import java.io.IOException;

/**
 * {@link AbstractHtmlElementTag AbstractHtmlElementTag} 的许多 HTML 标签渲染内容时使用数据绑定功能的方便的超类。
 * 子标签唯一需要做的是重写 {@link #renderDefaultContent(TagWriter)}。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractHtmlElementBodyTag extends AbstractHtmlElementTag implements BodyTag {

	/**
	 * 请求体内容
	 */
	@Nullable
	private BodyContent bodyContent;

	/**
	 * 标签写入器
	 */
	@Nullable
	private TagWriter tagWriter;


	@Override
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		// 调用 onWriteTagContent 方法
		onWriteTagContent();
		// 设置标签写入器
		this.tagWriter = tagWriter;
		// 如果应该呈现
		if (shouldRender()) {
			// 暴露属性
			exposeAttributes();
			// 返回评估正文的结果
			return EVAL_BODY_BUFFERED;
		} else {
			// 否则跳过正文
			return SKIP_BODY;
		}
	}

	/**
	 * 如果 {@link #shouldRender 渲染}，则刷新任何缓冲的 {@link BodyContent}，
	 * 或者如果没有提供 {@link BodyContent}，则 {@link #renderDefaultContent 渲染默认内容}。
	 *
	 * @return {@link javax.servlet.jsp.tagext.Tag#EVAL_PAGE} 结果
	 */
	@Override
	public int doEndTag() throws JspException {
		// 如果应该呈现
		if (shouldRender()) {
			// 断言确保标签写入器不为空
			Assert.state(this.tagWriter != null, "No TagWriter set");
			// 如果 请求体内容 不为空且包含文本内容
			if (this.bodyContent != null && StringUtils.hasText(this.bodyContent.getString())) {
				// 从 请求体内容 中呈现内容
				renderFromBodyContent(this.bodyContent, this.tagWriter);
			} else {
				// 否则呈现默认内容
				renderDefaultContent(this.tagWriter);
			}
		}
		// 返回评估页面的结果
		return EVAL_PAGE;
	}

	/**
	 * 基于提供的 {@link BodyContent} 渲染标签内容。
	 * <p>默认实现直接将 {@link BodyContent} 刷新到输出。
	 * 子类可以选择重写此方法以向输出添加其他内容。
	 */
	protected void renderFromBodyContent(BodyContent bodyContent, TagWriter tagWriter) throws JspException {
		flushBufferedBodyContent(bodyContent);
	}

	/**
	 * 清理任何属性和存储的资源。
	 */
	@Override
	public void doFinally() {
		super.doFinally();
		removeAttributes();
		this.tagWriter = null;
		this.bodyContent = null;
	}


	//---------------------------------------------------------------------
	// 模板方法
	//---------------------------------------------------------------------

	/**
	 * 在 {@link #writeTagContent} 开始时调用，允许子类执行任何前提条件检查或设置任务。
	 */
	protected void onWriteTagContent() {
	}

	/**
	 * 是否应该完全渲染此标签。默认返回 '{@code true}'，始终导致渲染。
	 * 如果提供条件渲染，则子类可以重写此方法。
	 */
	protected boolean shouldRender() throws JspException {
		return true;
	}

	/**
	 * 在 {@link #writeTagContent} 期间调用，允许子类根据需要向 {@link javax.servlet.jsp.PageContext} 添加任何属性。
	 */
	protected void exposeAttributes() throws JspException {
	}

	/**
	 * 由 {@link #doFinally} 调用，允许子类根据需要从 {@link javax.servlet.jsp.PageContext} 中移除任何属性。
	 */
	protected void removeAttributes() {
	}

	/**
	 * 用户自定义了错误消息的输出 - 将缓冲的内容刷新到主 {@link javax.servlet.jsp.JspWriter} 中。
	 */
	protected void flushBufferedBodyContent(BodyContent bodyContent) throws JspException {
		try {
			bodyContent.writeOut(bodyContent.getEnclosingWriter());
		} catch (IOException ex) {
			throw new JspException("Unable to write buffered body content.", ex);
		}
	}

	protected abstract void renderDefaultContent(TagWriter tagWriter) throws JspException;

	//---------------------------------------------------------------------
	// BodyTag 实现
	//---------------------------------------------------------------------

	@Override
	public void doInitBody() throws JspException {
		// no op
	}

	@Override
	public void setBodyContent(BodyContent bodyContent) {
		this.bodyContent = bodyContent;
	}

}
