/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;

import java.util.Map;

/**
 * 用于检查和可能修改请求数据值的合约，例如URL查询参数或表单字段值，在它们被视图呈现之前或重定向之前。
 * <p>
 * 实现可能会使用此合约作为解决方案的一部分，用于提供数据完整性、机密性、跨站请求伪造（CSRF）保护等，或者用于其他任务，例如自动向所有表单和URL添加隐藏字段。
 * <p>
 * 支持此合约的视图技术可以通过{@link RequestContext#getRequestDataValueProcessor()}获得一个实例来委托执行。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface RequestDataValueProcessor {

	/**
	 * 渲染新表单操作时调用。
	 *
	 * @param exchange   当前交换
	 * @param action     表单操作
	 * @param httpMethod 表单HTTP方法
	 * @return 要使用的操作，可能已修改
	 */
	String processAction(ServerWebExchange exchange, String action, String httpMethod);

	/**
	 * 渲染表单字段值时调用。
	 *
	 * @param exchange 当前交换
	 * @param name     表单字段名称
	 * @param value    表单字段值
	 * @param type     表单字段类型（"text"、"hidden"等）
	 * @return 要使用的表单字段值，可能已修改
	 */
	String processFormFieldValue(ServerWebExchange exchange, String name, String value, String type);

	/**
	 * 在所有表单字段都呈现后调用。
	 *
	 * @param exchange 当前交换
	 * @return 要添加的额外隐藏表单字段，或{@code null}
	 */
	@Nullable
	Map<String, String> getExtraHiddenFields(ServerWebExchange exchange);

	/**
	 * 渲染或重定向到URL时调用。
	 *
	 * @param exchange 当前交换
	 * @param url      URL值
	 * @return 要使用的URL，可能已修改
	 */
	String processUrl(ServerWebExchange exchange, String url);

}
