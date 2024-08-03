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

package org.springframework.validation;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Map;

/**
 * 基于 Map 的 {@link BindingResult} 接口实现，
 * 支持在 Map 属性上注册和评估绑定错误。
 *
 * <p>可以用作自定义绑定到 Map 的错误持有器，例如在对 Map 对象调用 Validator 时。
 *
 * @author Juergen Hoeller
 * @see java.util.Map
 * @since 2.0
 */
@SuppressWarnings("serial")
public class MapBindingResult extends AbstractBindingResult implements Serializable {
	/**
	 * 目标Map
	 */
	private final Map<?, ?> target;


	/**
	 * 创建一个新的 {@link MapBindingResult} 实例。
	 *
	 * @param target     目标 Map
	 * @param objectName 目标对象的名称
	 */
	public MapBindingResult(Map<?, ?> target, String objectName) {
		super(objectName);
		Assert.notNull(target, "Target Map must not be null");
		this.target = target;
	}


	/**
	 * 返回要绑定到的目标 Map。
	 */
	public final Map<?, ?> getTargetMap() {
		return this.target;
	}

	@Override
	@NonNull
	public final Object getTarget() {
		return this.target;
	}

	@Override
	@Nullable
	protected Object getActualFieldValue(String field) {
		return this.target.get(field);
	}

}
