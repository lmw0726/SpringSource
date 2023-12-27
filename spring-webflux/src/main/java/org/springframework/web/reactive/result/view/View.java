/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 渲染{@link HandlerResult}到HTTP响应的接口。
 *
 * <p>与{@link org.springframework.core.codec.Encoder Encoder}不同，后者是单例的，
 * 可以对给定类型的任何对象进行编码，{@code View}通常是通过名称选择并使用{@link ViewResolver}解析的，
 * 例如将其匹配到HTML模板。此外，{@code View}可以基于模型中包含的多个属性进行渲染。
 *
 * <p>{@code View}也可以选择从模型中选择属性，并使用任何现有的{@code Encoder}来渲染替代的媒体类型。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface View {

	/**
	 * 包含请求的{@link org.springframework.web.reactive.BindingContext BindingContext}的交换属性的名称，
	 * 可用于为模型中的对象创建{@link org.springframework.validation.BindingResult BindingResult}实例。
	 * <p>注意：此属性不是必需的，可能不存在。
	 *
	 * @since 5.1.8
	 */
	String BINDING_CONTEXT_ATTRIBUTE = View.class.getName() + ".bindingContext";


	/**
	 * 返回此View支持的媒体类型列表，或者返回空列表。
	 *
	 * @return 此视图支持的媒体类型列表
	 */
	default List<MediaType> getSupportedMediaTypes() {
		return Collections.emptyList();
	}

	/**
	 * 此View是否通过执行重定向来进行渲染。
	 *
	 * @return true表示当前视图时通过执行重定向进行渲染
	 */
	default boolean isRedirectView() {
		return false;
	}

	/**
	 * 根据给定的{@link HandlerResult}渲染视图。
	 * 实现可以访问和使用模型，或者仅使用其中的特定属性。
	 *
	 * @param model       一个Map，以名称字符串为键，以对应模型对象为值（在空模型的情况下，Map也可以是{@code null}）
	 * @param contentType 选定的内容类型，应该与{@link #getSupportedMediaTypes() 支持的媒体类型}之一匹配
	 * @param exchange    当前交换
	 * @return 表示渲染成功与否的{@code Mono}
	 */
	Mono<Void> render(@Nullable Map<String, ?> model, @Nullable MediaType contentType, ServerWebExchange exchange);

}
