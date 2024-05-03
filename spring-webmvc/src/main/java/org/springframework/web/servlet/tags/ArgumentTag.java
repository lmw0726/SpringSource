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

import org.springframework.lang.Nullable;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * {@code <argument>} 标签基于 JSTL 的 {@code fmt:param} 标签。
 * 其目的是支持消息和主题标签中的参数。
 *
 * <p>此标签必须嵌套在一个支持参数的标签下。
 *
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th>属性</th>
 * <th>是否必需</th>
 * <th>运行时表达式</th>
 * <th>描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>value</td>
 * <td>false</td>
 * <td>true</td>
 * <td>参数的值。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Nicholas Williams
 * @see MessageTag
 * @see ThemeTag
 * @since 4.0
 */
@SuppressWarnings("serial")
public class ArgumentTag extends BodyTagSupport {

	/**
	 * 属性值
	 */
	@Nullable
	private Object value;

	/**
	 * 是否设置属性
	 */
	private boolean valueSet;


	/**
	 * 设置参数的值（可选）。
	 * 如果未设置，则将评估标签的正文内容。
	 *
	 * @param value 参数值
	 */
	public void setValue(Object value) {
		this.value = value;
		this.valueSet = true;
	}


	@Override
	public int doEndTag() throws JspException {
		Object argument = null;
		if (this.valueSet) {
			// 如果设置了属性，参数值则是属性值
			argument = this.value;
		} else if (getBodyContent() != null) {
			// 从标签正文获取值
			argument = getBodyContent().getString().trim();
		}

		// 查找支持参数的祖先
		ArgumentAware argumentAwareTag = (ArgumentAware) findAncestorWithClass(this, ArgumentAware.class);
		if (argumentAwareTag == null) {
			throw new JspException("The argument tag must be a descendant of a tag that supports arguments");
		}
		//将属性值添加到属性值意识器标签中
		argumentAwareTag.addArgument(argument);

		return EVAL_PAGE;
	}

	@Override
	public void release() {
		super.release();
		this.value = null;
		this.valueSet = false;
	}

}
