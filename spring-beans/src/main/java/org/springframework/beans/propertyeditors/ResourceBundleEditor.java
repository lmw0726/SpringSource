/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * 针对标准 JDK {@link java.util.ResourceBundle ResourceBundles} 的 {@link java.beans.PropertyEditor} 实现。
 *
 * <p>仅支持从字符串转换 <i>到</i> ResourceBundle，不支持从 ResourceBundle 转换 <i>到</i> 字符串。
 *
 * 以下示例展示了在使用 XML 配置的 Spring 容器中如何使用此类：
 *
 * <pre class="code"> &lt;bean id="errorDialog" class="..."&gt;
 *    &lt;!--
 *        'messages' 属性类型为 java.util.ResourceBundle。
 *        'DialogMessages.properties' 文件位于 CLASSPATH 根目录
 *    --&gt;
 *    &lt;property name="messages" value="DialogMessages"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <pre class="code"> &lt;bean id="errorDialog" class="..."&gt;
 *    &lt;!--
 *        'DialogMessages.properties' 文件位于 'com/messages' 包中
 *    --&gt;
 *    &lt;property name="messages" value="com/messages/DialogMessages"/&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>一个“配置正确”的 Spring {@link org.springframework.context.ApplicationContext 容器}
 * 可能包含 {@link org.springframework.beans.factory.config.CustomEditorConfigurer} 定义，
 * 以便透明地进行转换：
 *
 * <pre class="code"> &lt;bean class="org.springframework.beans.factory.config.CustomEditorConfigurer"&gt;
 *    &lt;property name="customEditors"&gt;
 *        &lt;map&gt;
 *            &lt;entry key="java.util.ResourceBundle"&gt;
 *                &lt;bean class="org.springframework.beans.propertyeditors.ResourceBundleEditor"/&gt;
 *            &lt;/entry&gt;
 *        &lt;/map&gt;
 *    &lt;/property&gt;
 * &lt;/bean&gt;</pre>
 *
 * <p>请注意，此 {@link java.beans.PropertyEditor} 默认不会被 Spring 基础设施注册。
 *
 * <p>感谢 David Leal Valmana 提出的建议及初始原型。
 *
 * @author Rick Evans
 * @author Juergen Hoeller
 * @since 2.0
 */
public class ResourceBundleEditor extends PropertyEditorSupport {

	/**
	 * 当从字符串 {@link #setAsText(String) 转换} 时，用于区分基名和区域设置（如果有）的分隔符。
	 */
	public static final String BASE_NAME_SEPARATOR = "_";


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		Assert.hasText(text, "'text' must not be empty");
		String name = text.trim();

		int separator = name.indexOf(BASE_NAME_SEPARATOR);
		if (separator == -1) {
			setValue(ResourceBundle.getBundle(name));
		}
		else {
			// 名称可能包含区域设置信息
			String baseName = name.substring(0, separator);
			if (!StringUtils.hasText(baseName)) {
				throw new IllegalArgumentException("Invalid ResourceBundle name: '" + text + "'");
			}
			String localeString = name.substring(separator + 1);
			Locale locale = StringUtils.parseLocaleString(localeString);
			setValue(locale != null ? ResourceBundle.getBundle(baseName, locale) : ResourceBundle.getBundle(baseName));
		}
	}

}
