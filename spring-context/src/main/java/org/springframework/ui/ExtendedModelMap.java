/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.ui;

import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * {@link ModelMap} 的子类，实现了 {@link Model} 接口。
 *
 * <p>这是一个由 Spring MVC 暴露给处理器方法的实现类，通常通过声明 {@link org.springframework.ui.Model} 接口。
 * 无需在用户代码中构建它；一个普通的 {@link org.springframework.ui.ModelMap} 或者甚至只是一个带有字符串键的常规 {@link Map}
 * 就足以返回用户模型。
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 */
@SuppressWarnings("serial")
public class ExtendedModelMap extends ModelMap implements Model {

	@Override
	public ExtendedModelMap addAttribute(String attributeName, @Nullable Object attributeValue) {
		super.addAttribute(attributeName, attributeValue);
		return this;
	}

	@Override
	public ExtendedModelMap addAttribute(Object attributeValue) {
		super.addAttribute(attributeValue);
		return this;
	}

	@Override
	public ExtendedModelMap addAllAttributes(@Nullable Collection<?> attributeValues) {
		super.addAllAttributes(attributeValues);
		return this;
	}

	@Override
	public ExtendedModelMap addAllAttributes(@Nullable Map<String, ?> attributes) {
		super.addAllAttributes(attributes);
		return this;
	}

	@Override
	public ExtendedModelMap mergeAttributes(@Nullable Map<String, ?> attributes) {
		super.mergeAttributes(attributes);
		return this;
	}

	@Override
	public Map<String, Object> asMap() {
		return this;
	}

}
