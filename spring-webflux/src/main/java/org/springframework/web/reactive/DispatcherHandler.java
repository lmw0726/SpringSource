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

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.cors.reactive.PreFlightRequestHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * HTTP请求处理程序/控制器的中央调度程序。将请求分派给注册的处理程序以处理请求，并提供便捷的映射功能。
 *
 * <p>{@code DispatcherHandler}从Spring配置中发现其需要的委托组件：
 * <ul>
 * <li>{@link HandlerMapping} -- 将请求映射到处理程序对象
 * <li>{@link HandlerAdapter} -- 用于使用任何处理程序接口
 * <li>{@link HandlerResultHandler} -- 处理处理程序的返回值
 * </ul>
 *
 * <p>{@code DispatcherHandler}也被设计为Spring bean，并实现了{@link ApplicationContextAware}以访问其运行的上下文。
 * 如果将{@code DispatcherHandler}声明为名为"webHandler"的bean，则由{@link WebHttpHandlerBuilder#applicationContext(ApplicationContext)}发现，
 * 与{@code WebFilter}、{@code WebExceptionHandler}等一起组成处理链。
 *
 * <p>{@link org.springframework.web.reactive.config.EnableWebFlux @EnableWebFlux}配置中包含了一个{@code DispatcherHandler} bean声明。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Juergen Hoeller
 * @see WebHttpHandlerBuilder#applicationContext(ApplicationContext)
 * @since 5.0
 */
public class DispatcherHandler implements WebHandler, PreFlightRequestHandler, ApplicationContextAware {

	/**
	 * 处理程序映射列表，可能为空
	 */
	@Nullable
	private List<HandlerMapping> handlerMappings;

	/**
	 * 处理程序适配器列表，可能为空
	 */
	@Nullable
	private List<HandlerAdapter> handlerAdapters;

	/**
	 * 处理程序结果处理器列表，可能为空
	 */
	@Nullable
	private List<HandlerResultHandler> resultHandlers;


	/**
	 * 创建一个新的{@code DispatcherHandler}，需要通过{@link #setApplicationContext}配置{@link ApplicationContext}。
	 */
	public DispatcherHandler() {
	}

	/**
	 * 为给定的{@link ApplicationContext}创建一个新的{@code DispatcherHandler}。
	 *
	 * @param applicationContext 在其中查找处理程序bean的应用程序上下文
	 */
	public DispatcherHandler(ApplicationContext applicationContext) {
		initStrategies(applicationContext);
	}


	/**
	 * 返回通过类型检测在{@link #setApplicationContext 注入的上下文}中检测到的所有{@link HandlerMapping} bean，
	 * 并进行{@link AnnotationAwareOrderComparator#sort(List) 排序}。
	 * <p><strong>注意：</strong>如果在{@link #setApplicationContext(ApplicationContext)}之前调用，此方法可能返回{@code null}。
	 *
	 * @return 不可变列表，其中包含配置的映射，可能为{@code null}
	 */
	@Nullable
	public final List<HandlerMapping> getHandlerMappings() {
		return this.handlerMappings;
	}


	/**
	 * 设置应用程序上下文。实现了{@link ApplicationContextAware}接口。
	 * 调用此方法将初始化策略。
	 *
	 * @param applicationContext 要设置的应用程序上下文
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		initStrategies(applicationContext);
	}

	/**
	 * 初始化处理策略，从应用程序上下文中检索处理程序映射、适配器和处理程序结果处理器。
	 *
	 * @param context 应用程序上下文，用于检索处理程序bean
	 */
	protected void initStrategies(ApplicationContext context) {
		// 从应用程序上下文中检索HandlerMapping类型的bean映射
		Map<String, HandlerMapping> mappingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerMapping.class, true, false);

		// 将映射中的值存入ArrayList
		ArrayList<HandlerMapping> mappings = new ArrayList<>(mappingBeans.values());

		// 对mappings进行排序
		AnnotationAwareOrderComparator.sort(mappings);

		// 将排序后的mappings列表设置为不可修改的handlerMappings
		this.handlerMappings = Collections.unmodifiableList(mappings);

		// 从应用程序上下文中检索HandlerAdapter类型的bean映射
		Map<String, HandlerAdapter> adapterBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerAdapter.class, true, false);

		// 将适配器映射中的值存入ArrayList
		this.handlerAdapters = new ArrayList<>(adapterBeans.values());

		// 对handlerAdapters进行排序
		AnnotationAwareOrderComparator.sort(this.handlerAdapters);

		// 从应用程序上下文中检索HandlerResultHandler类型的bean映射
		Map<String, HandlerResultHandler> beans = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				context, HandlerResultHandler.class, true, false);

		// 将HandlerResultHandler映射中的值存入ArrayList
		this.resultHandlers = new ArrayList<>(beans.values());

		// 对resultHandlers进行排序
		AnnotationAwareOrderComparator.sort(this.resultHandlers);

	}


	/**
	 * 处理HTTP请求的方法。
	 *
	 * @param exchange 服务器WebExchange对象，表示当前的HTTP请求和响应
	 * @return 一个{@link Mono}，发出一个值或在无法解析为处理程序的情况下不发出任何值
	 */
	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		// 如果handlerMappings为空，则创建NotFoundError
		if (this.handlerMappings == null) {
			return createNotFoundError();
		}
		// 如果是预检请求，处理预检请求
		if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
			return handlePreFlight(exchange);
		}
		// 从handlerMappings中获取处理程序并处理请求
		return Flux.fromIterable(this.handlerMappings)
				.concatMap(mapping -> mapping.getHandler(exchange))
				.next()
				.switchIfEmpty(createNotFoundError())
				.flatMap(handler -> invokeHandler(exchange, handler))
				.flatMap(result -> handleResult(exchange, result));
	}


	/**
	 * 创建一个NotFoundError的Mono。
	 *
	 * @param <R> 返回的Mono类型
	 * @return 一个{@link Mono}，发出NotFoundError异常
	 */
	private <R> Mono<R> createNotFoundError() {
		return Mono.defer(() -> {
			Exception ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "No matching handler");
			return Mono.error(ex);
		});
	}

	/**
	 * 调用处理程序方法。
	 *
	 * @param exchange 服务器WebExchange对象，表示当前的HTTP请求和响应
	 * @param handler  处理程序对象
	 * @return 一个{@link Mono}，发出处理结果或在无法处理的情况下发出错误
	 */
	private Mono<HandlerResult> invokeHandler(ServerWebExchange exchange, Object handler) {
		// 如果响应状态为FORBIDDEN，则返回空（CORS拒绝）
		if (ObjectUtils.nullSafeEquals(exchange.getResponse().getStatusCode(), HttpStatus.FORBIDDEN)) {
			return Mono.empty();
		}
		// 如果handlerAdapters不为null，则遍历handlerAdapters并调用相应的适配器处理方法
		if (this.handlerAdapters != null) {
			for (HandlerAdapter handlerAdapter : this.handlerAdapters) {
				if (handlerAdapter.supports(handler)) {
					//使用给定处理程序处理请求。
					return handlerAdapter.handle(exchange, handler);
				}
			}
		}
		// 没有对应的HandlerAdapter，则返回错误
		return Mono.error(new IllegalStateException("No HandlerAdapter: " + handler));
	}


	/**
	 * 处理处理程序结果。
	 *
	 * @param exchange 服务器WebExchange对象，表示当前的HTTP请求和响应
	 * @param result   处理程序结果
	 * @return 一个{@link Mono}，完成处理结果或在处理过程中发生错误时提供错误处理
	 */
	private Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		return getResultHandler(result).handleResult(exchange, result)
				.checkpoint("Handler " + result.getHandler() + " [DispatcherHandler]")
				.onErrorResume(ex ->
						// 如果发生错误，则应用异常处理器，并继续处理异常
						result.applyExceptionHandler(ex).flatMap(exResult -> {
							String text = "Exception handler " + exResult.getHandler() +
									", error=\"" + ex.getMessage() + "\" [DispatcherHandler]";
							return getResultHandler(exResult).handleResult(exchange, exResult).checkpoint(text);
						}));
	}

	/**
	 * 获取处理程序结果处理器。
	 *
	 * @param handlerResult 处理程序结果
	 * @return 匹配处理程序结果的{@link HandlerResultHandler}
	 * @throws IllegalStateException 如果未找到与处理程序结果匹配的HandlerResultHandler
	 */
	private HandlerResultHandler getResultHandler(HandlerResult handlerResult) {
		if (this.resultHandlers != null) {
			for (HandlerResultHandler resultHandler : this.resultHandlers) {
				if (resultHandler.supports(handlerResult)) {
					return resultHandler;
				}
			}
		}
		throw new IllegalStateException("No HandlerResultHandler for " + handlerResult.getReturnValue());
	}

	/**
	 * 处理预检请求。
	 *
	 * @param exchange 服务器WebExchange对象，表示当前的HTTP请求和响应
	 * @return 一个{@link Mono}，在处理预检请求完成后完成，如果无法找到处理程序，则设置响应状态为FORBIDDEN
	 */
	@Override
	public Mono<Void> handlePreFlight(ServerWebExchange exchange) {
		return Flux.fromIterable(this.handlerMappings != null ? this.handlerMappings : Collections.emptyList())
				.concatMap(mapping -> mapping.getHandler(exchange))
				.switchIfEmpty(Mono.fromRunnable(() -> exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN)))
				.next()
				.then();
	}

}
