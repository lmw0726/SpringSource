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
import java.beans.PropertyEditor;

/**
 * 由 JSP 标签实现的接口，用于公开它们当前绑定到的属性的 PropertyEditor。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see BindTag
 * @see org.springframework.web.servlet.tags.form.AbstractDataBoundFormElementTag
 */
public interface EditorAwareTag {

	/**
	 * 检索此标签当前绑定到的属性的 PropertyEditor。用于协作嵌套标签。
	 * @return 当前的 PropertyEditor，如果没有则为 {@code null}
	 * @throws JspException 如果解析编辑器失败
	 */
	@Nullable
	PropertyEditor getEditor() throws JspException;

}
