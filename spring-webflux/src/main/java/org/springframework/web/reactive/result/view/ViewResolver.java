/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * 用于将视图名称解析为{@link View}实例的契约。视图名称可能对应于HTML模板或动态生成的内容。
 * <p>
 * 视图解析的过程是通过基于ViewResolver的HandlerResultHandler实现驱动的，称为ViewResolutionResultHandler。
 *
 * @author Rossen Stoyanchev
 * @see ViewResolutionResultHandler
 * @since 5.0
 */
public interface ViewResolver {

	/**
	 * 将视图名称解析为View实例。
	 *
	 * @param viewName 视图的名称
	 * @param locale   请求的语言环境
	 * @return 解析后的视图或空的流
	 */
	Mono<View> resolveViewName(String viewName, Locale locale);

}
