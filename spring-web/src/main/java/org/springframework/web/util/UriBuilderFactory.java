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

package org.springframework.web.util;

/**
 * 工厂类，用于创建{@link UriBuilder}实例，通过工厂创建的所有URI构建器实例都共享配置，例如基本URI、编码模式策略等。
 *
 * @author Rossen Stoyanchev
 * @see DefaultUriBuilderFactory
 * @since 5.0
 */
public interface UriBuilderFactory extends UriTemplateHandler {

	/**
	 * 使用给定的URI模板初始化一个构建器。
	 *
	 * @param uriTemplate 要使用的URI模板
	 * @return 构建器实例
	 */
	UriBuilder uriString(String uriTemplate);

	/**
	 * 使用默认设置创建一个URI构建器。
	 *
	 * @return 构建器实例
	 */
	UriBuilder builder();

}
