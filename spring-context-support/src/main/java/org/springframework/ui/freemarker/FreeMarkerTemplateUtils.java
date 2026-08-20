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

package org.springframework.ui.freemarker;

import java.io.IOException;
import java.io.StringWriter;

import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * 用于处理 FreeMarker 的实用工具类。
 * 提供了方便的方法来处理 FreeMarker 模板和模型。
 *
 * @author Juergen Hoeller
 * @since 14.03.2004
 */
public abstract class FreeMarkerTemplateUtils {

	/**
	 * 使用给定的模型处理指定的 FreeMarker 模板，并将结果写入给定的 Writer。
	 * <p>当使用此方法为通过 Spring 邮件支持发送的邮件准备文本时，请考虑将 IO/TemplateException 包装在 MailPreparationException 中。
	 * @param model 模型对象，通常是包含模型名称作为键、模型对象作为值的 Map
	 * @return 结果字符串
	 * @throws IOException 如果模板未找到或无法读取
	 * @throws freemarker.template.TemplateException 如果渲染失败
	 * @see org.springframework.mail.MailPreparationException
	 */
	public static String processTemplateIntoString(Template template, Object model)
			throws IOException, TemplateException {

		StringWriter result = new StringWriter(1024);
		template.process(model, result);
		return result.toString();
	}

}
