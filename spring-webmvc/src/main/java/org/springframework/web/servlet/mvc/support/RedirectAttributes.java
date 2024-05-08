/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.mvc.support;

import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.web.servlet.FlashMap;

import java.util.Collection;
import java.util.Map;

/**
 * {@link Model} 接口的专门化，控制器可以使用它来为重定向场景选择属性。
 * 由于添加重定向属性的意图非常明确 -- 即用于重定向 URL，属性值可以格式化为字符串并以此方式存储，以使其符合在 {@code org.springframework.web.servlet.view.RedirectView} 中附加到查询字符串或扩展为 URI 变量的条件。
 *
 * <p>该接口还提供了一种添加 Flash 属性的方式。有关 Flash 属性的概述，请参阅 {@link FlashMap}。您可以使用 {@link RedirectAttributes} 来存储 Flash 属性，它们将自动传播到当前请求的 "output" FlashMap。
 *
 * <p>在 {@code @Controller} 中的示例用法：
 * <pre class="code">
 * &#064;RequestMapping(value = "/accounts", method = RequestMethod.POST)
 * public String handle(Account account, BindingResult result, RedirectAttributes redirectAttrs) {
 *   if (result.hasErrors()) {
 *     return "accounts/new";
 *   }
 *   // Save account ...
 *   redirectAttrs.addAttribute("id", account.getId()).addFlashAttribute("message", "Account created!");
 *   return "redirect:/accounts/{id}";
 * }
 * </pre>
 *
 * <p>当调用该方法时，RedirectAttributes 模型为空，并且除非方法返回重定向视图名称或 RedirectView，否则永远不会使用它。
 *
 * <p>在重定向后，Flash 属性会自动添加到提供目标 URL 的控制器的模型中。
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public interface RedirectAttributes extends Model {

	@Override
	RedirectAttributes addAttribute(String attributeName, @Nullable Object attributeValue);

	@Override
	RedirectAttributes addAttribute(Object attributeValue);

	@Override
	RedirectAttributes addAllAttributes(Collection<?> attributeValues);

	@Override
	RedirectAttributes mergeAttributes(Map<String, ?> attributes);

	/**
	 * 添加给定的 Flash 属性。
	 * @param attributeName 属性名称；永远不会为 {@code null}
	 * @param attributeValue 属性值；可以为 {@code null}
	 */
	RedirectAttributes addFlashAttribute(String attributeName, @Nullable Object attributeValue);

	/**
	 * 使用 {@link org.springframework.core.Conventions#getVariableName 生成的名称} 添加给定的 Flash 存储。
	 * @param attributeValue Flash 属性值；永远不会为 {@code null}
	 */
	RedirectAttributes addFlashAttribute(Object attributeValue);

	/**
	 * 返回用于 Flash 存储的属性候选项或空 Map。
	 */
	Map<String, ?> getFlashAttributes();
}
