/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.validation.support;

import org.springframework.lang.Nullable;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;

import java.util.Map;

/**
 * {@link org.springframework.ui.ExtendedModelMap} 的子类，如果通过常规 {@link Map} 操作替换了相应的目标属性，
 * 则自动移除 {@link org.springframework.validation.BindingResult} 对象。
 *
 * <p>这是 Spring MVC 暴露给处理器方法的类，通常通过声明 {@link org.springframework.ui.Model} 接口来使用。
 * 无需在用户代码中构建它；一个普通的 {@link org.springframework.ui.ModelMap} 或者甚至只是一个带有字符串键的常规 {@link Map}
 * 就足以返回用户模型。
 *
 * @author Juergen Hoeller
 * @see org.springframework.validation.BindingResult
 * @since 2.5.6
 */
@SuppressWarnings("serial")
public class BindingAwareModelMap extends ExtendedModelMap {

	@Override
	public Object put(String key, @Nullable Object value) {
		removeBindingResultIfNecessary(key, value);
		return super.put(key, value);
	}

	@Override
	public void putAll(Map<? extends String, ?> map) {
		map.forEach(this::removeBindingResultIfNecessary);
		super.putAll(map);
	}

	private void removeBindingResultIfNecessary(Object key, @Nullable Object value) {
		// 如果键是字符串类型
		if (key instanceof String) {
			// 将键强制转换为字符串，并赋值给属性名称
			String attributeName = (String) key;
			// 检查属性名称是否不以 模型键前缀 开头
			if (!attributeName.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
				// 创建新的键值，它是前缀和属性名称的组合
				String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + attributeName;
				// 从集合中获取与绑定结果键相关的绑定结果对象
				BindingResult bindingResult = (BindingResult) get(bindingResultKey);
				// 如果获取的绑定结果不为空，且其目标对象与当前值不同
				if (bindingResult != null && bindingResult.getTarget() != value) {
					// 从集合中移除与绑定结果键相关的条目
					remove(bindingResultKey);
				}
			}
		}
	}

}
