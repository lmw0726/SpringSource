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

package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 用于向 {@link Writer} 实例写入 HTML 内容的实用工具类。
 *
 * <p>旨在支持 JSP 标签库的输出。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 */
public class TagWriter {

	/**
	 * 要写入的 {@link SafeWriter}。
	 */
	private final SafeWriter writer;

	/**
	 * 存储 {@link TagStateEntry 标签状态}。栈模型自然支持标签嵌套。
	 */
	private final Deque<TagStateEntry> tagState = new ArrayDeque<>();


	/**
	 * 创建一个新的 {@link TagWriter} 类的实例，该实例将写入到提供的 {@link PageContext} 中。
	 *
	 * @param pageContext 用于获取 {@link Writer} 的 JSP PageContext
	 */
	public TagWriter(PageContext pageContext) {
		Assert.notNull(pageContext, "PageContext must not be null");
		this.writer = new SafeWriter(pageContext);
	}

	/**
	 * 创建一个新的 {@link TagWriter} 类的实例，该实例将写入到提供的 {@link Writer} 中。
	 *
	 * @param writer 要写入标签内容的 {@link Writer}
	 */
	public TagWriter(Writer writer) {
		Assert.notNull(writer, "Writer must not be null");
		this.writer = new SafeWriter(writer);
	}


	/**
	 * 使用提供的名称开始一个新的标签。保留标签打开状态，以便可以在其中编写属性、内部文本或嵌套标签。
	 *
	 * @see #endTag()
	 */
	public void startTag(String tagName) throws JspException {
		// 如果已经在标签中，则关闭当前标签并标记为块级标签
		if (inTag()) {
			closeTagAndMarkAsBlock();
		}

		// 压入新的标签名称
		push(tagName);
		// 写入新标签的起始部分
		this.writer.append("<").append(tagName);
	}

	/**
	 * 使用指定的名称和值写入 HTML 属性。
	 * <p>确保在写入任何内部文本或嵌套标签<strong>之前</strong>写入所有属性。
	 *
	 * @throws IllegalStateException 如果开放标签已关闭
	 */
	public void writeAttribute(String attributeName, String attributeValue) throws JspException {
		if (currentState().isBlockTag()) {
			throw new IllegalStateException("Cannot write attributes after opening tag is closed.");
		}
		this.writer.append(" ").append(attributeName).append("=\"")
				.append(attributeValue).append("\"");
	}

	/**
	 * {@link #writeAttribute(String, String)} 的变体，用于写入空的 HTML 属性，例如 {@code required}。
	 *
	 * @since 5.3.14
	 */
	public void writeAttribute(String attributeName) throws JspException {
		if (currentState().isBlockTag()) {
			throw new IllegalStateException("Cannot write attributes after opening tag is closed.");
		}
		this.writer.append(" ").append(attributeName);
	}

	/**
	 * 如果提供的值不为 {@code null} 或零长度，则写入 HTML 属性。
	 *
	 * @see #writeAttribute(String, String)
	 */
	public void writeOptionalAttributeValue(String attributeName, @Nullable String attributeValue) throws JspException {
		if (StringUtils.hasText(attributeValue)) {
			writeAttribute(attributeName, attributeValue);
		}
	}

	/**
	 * 关闭当前打开的标签（如果需要），并将提供的值附加为内部文本。
	 *
	 * @throws IllegalStateException 如果没有打开的标签
	 */
	public void appendValue(String value) throws JspException {
		if (!inTag()) {
			throw new IllegalStateException("Cannot write tag value. No open tag available.");
		}
		closeTagAndMarkAsBlock();
		this.writer.append(value);
	}


	/**
	 * 表示当前打开的标签应关闭，并标记为块级元素。
	 * <p>当您计划在当前 {@link TagWriter} 上下文之外的正文中编写其他内容时有用。
	 */
	public void forceBlock() throws JspException {
		if (currentState().isBlockTag()) {
			// 只需忽略，因为我们已经在块中了
			return;
		}
		// 关闭标记并标记为块
		closeTagAndMarkAsBlock();
	}

	/**
	 * 关闭当前标签。
	 * <p>如果没有编写任何内部文本或嵌套标签，则正确地写入空标签。
	 */
	public void endTag() throws JspException {
		endTag(false);
	}

	/**
	 * 关闭当前标签，允许强制完整关闭标签。
	 * <p>如果没有编写任何内部文本或嵌套标签，则正确地写入空标签。
	 *
	 * @param enforceClosingTag 是否应强制在任何情况下渲染完整的闭合标签，即使在非块标签的情况下也是如此
	 */
	public void endTag(boolean enforceClosingTag) throws JspException {
		// 如果没有打开的标签可用，则抛出异常
		if (!inTag()) {
			throw new IllegalStateException("Cannot write end of tag. No open tag available.");
		}

		// 默认渲染结束标签
		boolean renderClosingTag = true;

		// 如果当前状态不是块级标签
		if (!currentState().isBlockTag()) {
			// 对于非块级标签，仍然需要关闭...
			if (enforceClosingTag) {
				// 如果需要强制关闭标签，则写入">"
				this.writer.append(">");
			} else {
				// 否则，写入自闭合标签
				this.writer.append("/>");
				renderClosingTag = false;
			}
		}

		// 如果需要渲染结束标签
		if (renderClosingTag) {
			// 写入结束标签
			this.writer.append("</").append(currentState().getTagName()).append(">");
		}

		// 弹出当前标签状态
		this.tagState.pop();
	}


	/**
	 * 将提供的标签名称添加到 {@link #tagState 标签状态}。
	 */
	private void push(String tagName) {
		this.tagState.push(new TagStateEntry(tagName));
	}

	/**
	 * 关闭当前打开的标签，并将其标记为块级标签。
	 */
	private void closeTagAndMarkAsBlock() throws JspException {
		if (!currentState().isBlockTag()) {
			// 如果当前标签不是块标签，将当前状态设置为块标签
			currentState().markAsBlockTag();
			// 添加 ">" 结束标签
			this.writer.append(">");
		}
	}

	private boolean inTag() {
		return !this.tagState.isEmpty();
	}

	private TagStateEntry currentState() {
		return this.tagState.element();
	}


	/**
	 * 存储有关标签及其呈现行为的状态。
	 */
	private static class TagStateEntry {

		/**
		 * 标签名称
		 */
		private final String tagName;

		/**
		 * 是否是块标签
		 */
		private boolean blockTag;

		public TagStateEntry(String tagName) {
			this.tagName = tagName;
		}

		public String getTagName() {
			return this.tagName;
		}

		public void markAsBlockTag() {
			this.blockTag = true;
		}

		public boolean isBlockTag() {
			return this.blockTag;
		}
	}


	/**
	 * 将所有 {@link IOException IOExceptions} 包装在
	 * {@link JspException JspExceptions} 中的简单 {@link Writer} 包装器。
	 */
	private static final class SafeWriter {

		/**
		 * 页面上下文
		 */
		@Nullable
		private PageContext pageContext;

		/**
		 * 写入器
		 */
		@Nullable
		private Writer writer;

		public SafeWriter(PageContext pageContext) {
			this.pageContext = pageContext;
		}

		public SafeWriter(Writer writer) {
			this.writer = writer;
		}

		public SafeWriter append(String value) throws JspException {
			try {
				getWriterToUse().write(String.valueOf(value));
				return this;
			} catch (IOException ex) {
				throw new JspException("Unable to write to JspWriter", ex);
			}
		}

		private Writer getWriterToUse() {
			// 获取写入器
			Writer writer = (this.pageContext != null ? this.pageContext.getOut() : this.writer);
			Assert.state(writer != null, "No Writer available");
			return writer;
		}
	}

}
