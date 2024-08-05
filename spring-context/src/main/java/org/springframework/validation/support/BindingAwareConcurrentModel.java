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
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BindingResult;

import java.util.Map;

/**
 * {@link ConcurrentModel} 的子类，在通过常规 {@link Map} 操作替换对应的目标属性时，自动移除 {@link BindingResult} 对象。
 *
 * <p>这是由 Spring WebFlux 暴露给处理方法的类，通常通过在 {@link org.springframework.ui.Model} 接口中声明为参数类型来使用。
 * 用户代码中通常无需创建它。如果需要，处理方法可以返回一个常规的 {@code java.util.Map}，可能是一个 {@code java.util.ConcurrentMap}，用于预定模型。
 *
 * @author Rossen Stoyanchev
 * @see BindingResult
 * @see BindingAwareModelMap
 * @since 5.0
 */
@SuppressWarnings("serial")
public class BindingAwareConcurrentModel extends ConcurrentModel {

	@Override
	@Nullable
	public Object put(String key, @Nullable Object value) {
		// 如有必要去除绑定结果
		removeBindingResultIfNecessary(key, value);
		return super.put(key, value);
	}

	private void removeBindingResultIfNecessary(String key, @Nullable Object value) {
		if (!key.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
			// 如果键不是以模型键前缀开头
			// 拼接模型建和键名作为结果键。
			String resultKey = BindingResult.MODEL_KEY_PREFIX + key;
			// 获取绑定结果
			BindingResult result = (BindingResult) get(resultKey);
			if (result != null && result.getTarget() != value) {
				// 如果绑定结果不为工，并且绑定结果的目标值不是当前值，则去除结果键。
				remove(resultKey);
			}
		}
	}

}
