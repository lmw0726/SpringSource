/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.jndi;

import java.beans.PropertyEditorSupport;
import java.util.Properties;

import org.springframework.beans.propertyeditors.PropertiesEditor;
import org.springframework.lang.Nullable;

/**
 * JndiTemplate 对象的属性编辑器。允许使用属性格式字符串填充 JndiTemplate 类型的属性。
 *
 * @author Rod Johnson
 * @since 09.05.2003
 */
public class JndiTemplateEditor extends PropertyEditorSupport {

	private final PropertiesEditor propertiesEditor = new PropertiesEditor();

	@Override
	public void setAsText(@Nullable String text) throws IllegalArgumentException {
		if (text == null) {
			throw new IllegalArgumentException("JndiTemplate cannot be created from null string");
		}
		if (text.isEmpty()) {
			// 空环境
			setValue(new JndiTemplate());
		}
		else {
			// 我们有一个非空的属性字符串
			this.propertiesEditor.setAsText(text);
			Properties props = (Properties) this.propertiesEditor.getValue();
			setValue(new JndiTemplate(props));
		}
	}

}
