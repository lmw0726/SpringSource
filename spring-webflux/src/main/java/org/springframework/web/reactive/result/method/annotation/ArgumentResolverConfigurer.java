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

package org.springframework.web.reactive.result.method.annotation;

import org.springframework.util.Assert;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 帮助配置控制器方法参数的解析器。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ArgumentResolverConfigurer {

	/**
	 * 自定义解析器列表
	 */
	private final List<HandlerMethodArgumentResolver> customResolvers = new ArrayList<>(8);


	/**
	 * 配置自定义控制器方法参数的解析器。
	 *
	 * @param resolver 要添加的解析器（们）
	 */
	public void addCustomResolver(HandlerMethodArgumentResolver... resolver) {
		// 确保解析器不为 null
		Assert.notNull(resolver, "'resolvers' must not be null");
		// 将解析器添加到自定义解析器列表中
		this.customResolvers.addAll(Arrays.asList(resolver));
	}


	/**
	 * 获取自定义解析器列表的方法。
	 *
	 * @return 自定义解析器列表
	 */
	List<HandlerMethodArgumentResolver> getCustomResolvers() {
		return this.customResolvers;
	}

}
