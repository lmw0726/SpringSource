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

package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * 通过查看方法参数和参数值并决定应更新目标URL的哪一部分，以贡献于构建{@link UriComponents}的策略。
 *
 * @author Oliver Gierke
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface UriComponentsContributor {

	/**
	 * 判断该贡献者是否支持给定的方法参数。
	 */
	boolean supportsParameter(MethodParameter parameter);

	/**
	 * 处理给定的方法参数，并更新{@link UriComponentsBuilder}或添加到用于在所有参数处理后扩展URI的URI变量映射。
	 *
	 * @param parameter         控制器方法参数（从不为{@code null}）
	 * @param value             参数值（可能为{@code null}）
	 * @param builder           要更新的构建器（从不为{@code null}）
	 * @param uriVariables      要添加URI变量的映射（从不为{@code null}）
	 * @param conversionService 用于将值格式化为字符串的ConversionService
	 */
	void contributeMethodArgument(MethodParameter parameter, Object value, UriComponentsBuilder builder,
								  Map<String, Object> uriVariables, ConversionService conversionService);

}
