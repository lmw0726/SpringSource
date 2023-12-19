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

package org.springframework.web.reactive;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 定义请求和处理程序对象之间映射关系的对象应实现的接口。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public interface HandlerMapping {

	/**
	 * 包含最佳匹配模式的{@link ServerWebExchange#getAttributes()属性}名称。
	 */
	String BEST_MATCHING_HANDLER_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingHandler";

	/**
	 * 包含处理程序映射中最佳匹配模式的{@link ServerWebExchange#getAttributes()属性}名称。
	 */
	String BEST_MATCHING_PATTERN_ATTRIBUTE = HandlerMapping.class.getName() + ".bestMatchingPattern";

	/**
	 * 包含处理程序映射中路径的{@link ServerWebExchange#getAttributes()属性}名称，如果是模式匹配，例如{@code "/static/**"}，否则为相关URI的完整路径。
	 * <p>注意：并非所有HandlerMapping实现都需要支持此属性。
	 * 基于URL的HandlerMappings通常会支持它，但处理程序不一定要求在所有情况下存在此请求属性。
	 */
	String PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE = HandlerMapping.class.getName() + ".pathWithinHandlerMapping";

	/**
	 * 包含URI模板映射到值的{@link ServerWebExchange#getAttributes()属性}名称的映射。
	 * <p>注意：并非所有HandlerMapping实现都需要支持此属性。
	 * 基于URL的HandlerMappings通常会支持它，但处理程序不一定要求在所有情况下存在此请求属性。
	 */
	String URI_TEMPLATE_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".uriTemplateVariables";

	/**
	 * 包含具有URI变量名称和相应的MultiValueMap的映射的{@link ServerWebExchange#getAttributes()属性}名称。
	 * <p>注意：并非所有HandlerMapping实现都需要支持此属性，并且根据HandlerMapping是否配置为在请求URI中保留矩阵变量内容，此属性也可能不存在。
	 */
	String MATRIX_VARIABLES_ATTRIBUTE = HandlerMapping.class.getName() + ".matrixVariables";

	/**
	 * 包含适用于映射处理程序的可生产MediaType集合的{@link ServerWebExchange#getAttributes()属性}名称。
	 * <p>注意：并非所有HandlerMapping实现都需要支持此属性。
	 * 处理程序不一定要求在所有情况下存在此请求属性。
	 */
	String PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE = HandlerMapping.class.getName() + ".producibleMediaTypes";


	/**
	 * 返回此请求的处理程序。
	 * <p>在返回处理程序之前，实现方法应检查与处理程序关联的CORS配置，
	 * 根据其应用验证检查，并相应地更新响应。
	 * 对于预检请求，应根据处理程序匹配到预期的实际请求进行相同的操作。
	 *
	 * @param exchange 当前服务器交换信息
	 * @return 一个{@link Mono}，发出一个值或在无法解析为处理程序的情况下不发出任何值
	 */
	Mono<Object> getHandler(ServerWebExchange exchange);

}
