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

package org.springframework.web.servlet.handler;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 实现了 {@link org.springframework.web.servlet.HandlerMapping} 接口，
 * 将 URL 映射到以斜杠 ("/") 开头的 bean 名称，类似于 Struts 将 URL 映射到动作名称的方式。
 *
 * <p>这是 {@link org.springframework.web.servlet.DispatcherServlet} 默认使用的实现，
 * 与 {@link org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping}
 * 一起使用。或者，{@link SimpleUrlHandlerMapping} 允许以声明方式自定义处理程序映射。
 *
 * <p>映射是从 URL 到 bean 名称。
 * 因此，传入的 URL "/foo" 将映射到名为 "/foo" 的处理程序，
 * 或者在单个处理程序上进行多次映射时将映射到 "/foo /foo2"。
 *
 * <p>支持直接匹配（给定 "/test" -&gt; 注册的 "/test"）和 "*" 匹配（给定 "/test" -&gt; 注册的 "/t*"）。
 * 请注意，默认情况下将在当前 Servlet 映射范围内进行映射；
 * 有关详细信息，请参阅 {@link #setAlwaysUseFullPath "alwaysUseFullPath"} 属性。
 * 有关模式选项的详细信息，请参阅 {@link org.springframework.util.AntPathMatcher} javadoc。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see SimpleUrlHandlerMapping
 */
public class BeanNameUrlHandlerMapping extends AbstractDetectingUrlHandlerMapping {

	/**
	 * 检查给定 bean 的名称和别名是否以 "/" 开头的 URL。
	 */
	@Override
	protected String[] determineUrlsForHandler(String beanName) {
		// 创建保存 URL 的列表
		List<String> urls = new ArrayList<>();
		// 如果 bean名称 以斜杠开头，则将其添加到 URL 列表中
		if (beanName.startsWith("/")) {
			urls.add(beanName);
		}
		// 获取 bean名称 的所有别名
		String[] aliases = obtainApplicationContext().getAliases(beanName);
		// 遍历别名
		for (String alias : aliases) {
			// 如果别名以斜杠开头，则将其添加到 URL 列表中
			if (alias.startsWith("/")) {
				urls.add(alias);
			}
		}
		// 将 URL 列表转换为字符串数组并返回
		return StringUtils.toStringArray(urls);
	}

}
