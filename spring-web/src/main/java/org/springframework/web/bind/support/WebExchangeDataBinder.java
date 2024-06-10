/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.bind.support;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 专用于从URL查询参数或请求数据中的表单数据将数据绑定到Java对象的{@link org.springframework.validation.DataBinder}的特殊实现。
 *
 * <p><strong>警告</strong>：数据绑定可能会导致安全问题，因为它会暴露对象图中本不应由外部客户端访问或修改的部分。因此，设计和使用数据绑定时应该仔细考虑安全性。有关详细信息，请参阅参考手册中关于
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a> 和
 * <a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>
 * 上数据绑定的专用章节。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
public class WebExchangeDataBinder extends WebDataBinder {

	/**
	 * 创建一个新的实例，使用默认的对象名称。
	 *
	 * @param target 要绑定到的目标对象（或{@code null}，如果绑定器仅用于转换普通参数值）
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public WebExchangeDataBinder(@Nullable Object target) {
		super(target);
	}

	/**
	 * 创建一个新的实例。
	 *
	 * @param target     要绑定到的目标对象（或{@code null}，如果绑定器仅用于转换普通参数值）
	 * @param objectName 目标对象的名称
	 */
	public WebExchangeDataBinder(@Nullable Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * 将查询参数、表单数据或多部分表单数据绑定到绑定器目标。
	 *
	 * @param exchange 当前交换
	 * @return 当绑定完成时返回一个{@code Mono<Void>}
	 */
	public Mono<Void> bind(ServerWebExchange exchange) {
		// 返回要绑定的值，使用交换对象初始化
		return getValuesToBind(exchange)
				// 对每个值执行操作
				.doOnNext(values -> doBind(new MutablePropertyValues(values)))
				// 返回一个 Mono，表示异步操作的完成
				.then();
	}

	/**
	 * 通过提取要绑定的值进行数据绑定的受保护方法。默认情况下，此方法委托给{@link #extractValuesToBind(ServerWebExchange)}。
	 *
	 * @param exchange 当前交换
	 * @return 绑定值的映射
	 * @since 5.3
	 */
	public Mono<Map<String, Object>> getValuesToBind(ServerWebExchange exchange) {
		return extractValuesToBind(exchange);
	}


	/**
	 * 将查询参数和表单数据与请求主体中的多部分表单数据合并为用于数据绑定目的的{@code Map<String, Object>}值。
	 *
	 * @param exchange 当前交换
	 * @return 包含要绑定的值的{@code Mono}
	 */
	public static Mono<Map<String, Object>> extractValuesToBind(ServerWebExchange exchange) {
		// 获取请求的查询参数
		MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();

		// 获取表单数据
		Mono<MultiValueMap<String, String>> formData = exchange.getFormData();

		// 获取多部分数据
		Mono<MultiValueMap<String, Part>> multipartData = exchange.getMultipartData();

		// 使用 Mono.zip 将三个 Mono 合并为一个，并在完成后返回一个映射
		return Mono.zip(Mono.just(queryParams), formData, multipartData)
				.map(tuple -> {
					// 创建一个按键排序的结果映射
					Map<String, Object> result = new TreeMap<>();

					// 将查询参数的键值对添加到结果映射
					tuple.getT1().forEach((key, values) -> addBindValue(result, key, values));

					// 将表单数据的键值对添加到结果映射
					tuple.getT2().forEach((key, values) -> addBindValue(result, key, values));

					// 将多部分数据的键值对添加到结果映射
					tuple.getT3().forEach((key, values) -> addBindValue(result, key, values));

					// 返回结果映射
					return result;
				});
	}

	protected static void addBindValue(Map<String, Object> params, String key, List<?> values) {
		// 如果值列表不为空
		if (!CollectionUtils.isEmpty(values)) {
			// 将值列表中的每个值转换为字段部分的值，如果是字段部分，则取其值；
			// 否则保持原值不变，并收集为列表
			values = values.stream()
					.map(value -> value instanceof FormFieldPart ? ((FormFieldPart) value).value() : value)
					.collect(Collectors.toList());
			// 如果值列表的大小为1，则将该值作为参数的值；
			// 否则将整个值列表作为参数的值
			params.put(key, values.size() == 1 ? values.get(0) : values);
		}
	}

}
