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

package org.springframework.validation;

import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于在模型 Map 中查找 BindingResult 的便捷方法。
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see BindingResult#MODEL_KEY_PREFIX
 */
public abstract class BindingResultUtils {

	/**
	 * 在给定模型中查找给定名称的 BindingResult。
	 * @param model 要搜索的模型
	 * @param name 要查找 BindingResult 的目标对象的名称
	 * @return BindingResult，如果未找到则返回 {@code null}
	 * @throws IllegalStateException 如果找到的属性不是 BindingResult 类型
	 */
	@Nullable
	public static BindingResult getBindingResult(Map<?, ?> model, String name) {
		Assert.notNull(model, "Model map must not be null");
		Assert.notNull(name, "Name must not be null");
		Object attr = model.get(BindingResult.MODEL_KEY_PREFIX + name);
		if (attr != null && !(attr instanceof BindingResult)) {
			throw new IllegalStateException("BindingResult attribute is not of type BindingResult: " + attr);
		}
		return (BindingResult) attr;
	}

	/**
	 * 在给定模型中查找给定名称的必需 BindingResult。
	 * @param model 要搜索的模型
	 * @param name 要查找 BindingResult 的目标对象的名称
	 * @return BindingResult（永远不为 {@code null}）
	 * @throws IllegalStateException 如果未找到 BindingResult
	 */
	public static BindingResult getRequiredBindingResult(Map<?, ?> model, String name) {
		BindingResult bindingResult = getBindingResult(model, name);
		if (bindingResult == null) {
			throw new IllegalStateException("No BindingResult attribute found for name '" + name +
					"'- have you exposed the correct model?");
		}
		return bindingResult;
	}

}
