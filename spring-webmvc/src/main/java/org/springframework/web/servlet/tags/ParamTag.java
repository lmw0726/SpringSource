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
 * {@code <param>} 标签收集名称-值参数并将它们传递给标签层次结构中的 {@link ParamAware} 祖先。
 *
 * <p>此标签必须嵌套在一个参数感知标签下。
 *
 * <table>
 * <caption>属性摘要</caption>
 * <thead>
 * <tr>
 * <th>属性</th>
 * <th>是否必需</th>
 * <th>运行时表达式？</th>
 * <th>描述</th>
 * </tr>
 * </thead>
 * <tbody>
 * <tr>
 * <td>name</td>
 * <td>true</td>
 * <td>true</td>
 * <td>参数的名称。</td>
 * </tr>
 * <tr>
 * <td>value</td>
 * <td>false</td>
 * <td>true</td>
 * <td>参数的值。</td>
 * </tr>
 * </tbody>
 * </table>
 *
 * @author Scott Andrews
 * @author Nicholas Williams
 * @see Param
 * @see UrlTag
 * @since 3.0
 */
@SuppressWarnings("serial")
public class ParamTag extends BodyTagSupport {
	/**
	 * 参数名称
	 */
	private String name = "";

	/**
	 * 参数值
	 */
	@Nullable
	private String value;

	/**
	 * 是否设置了参数值
	 */
	private boolean valueSet;


	/**
	 * 设置参数的名称（必需）。
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 设置参数的值（可选）。
	 */
	public void setValue(String value) {
		this.value = value;
		this.valueSet = true;
	}


	@Override
	public int doEndTag() throws JspException {
		// 创建 Param 对象并设置名称。
		Param param = new Param();
		param.setName(this.name);

		if (this.valueSet) {
			// 如果已设置值，则将其设置为参数的值。
			param.setValue(this.value);
		} else if (getBodyContent() != null) {
			// 如果存在标签主体，则从标签主体获取值并设置。
			param.setValue(getBodyContent().getString().trim());
		}

		// 查找 参数意识器
		ParamAware paramAwareTag = (ParamAware) findAncestorWithClass(this, ParamAware.class);
		if (paramAwareTag == null) {
			// 如果找不到 参数意识器 的标签，则抛出异常。
			throw new JspException("The param tag must be a descendant of a tag that supports parameters");
		}

		// 将 Param 对象添加到参数意识器标签中
		paramAwareTag.addParam(param);

		// 返回 评估页面
		return EVAL_PAGE;
	}

	@Override
	public void release() {
		super.release();
		this.name = "";
		this.value = null;
		this.valueSet = false;
	}

}
