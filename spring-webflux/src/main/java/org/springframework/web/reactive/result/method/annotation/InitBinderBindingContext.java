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

package org.springframework.web.reactive.result.method.annotation;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.SimpleSessionStatus;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.SyncInvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;

import java.util.List;

/**
 * 扩展{@link BindingContext}，添加{@code @InitBinder}方法初始化。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class InitBinderBindingContext extends BindingContext {

	/**
	 * 存储 同步参数解析器 绑定方法的列表
	 */
	private final List<SyncInvocableHandlerMethod> binderMethods;

	/**
	 * 绑定方法上下文
	 */
	private final BindingContext binderMethodContext;

	/**
	 * 会话状态，使用SimpleSessionStatus实例化
	 */
	private final SessionStatus sessionStatus = new SimpleSessionStatus();

	/**
	 * Runnable实例，用于保存模型操作
	 */
	@Nullable
	private Runnable saveModelOperation;


	/**
	 * 构造函数，初始化{@code InitBinderBindingContext}。
	 *
	 * @param initializer   可能为null的WebBindingInitializer
	 * @param binderMethods {@code @InitBinder}方法的列表
	 */
	InitBinderBindingContext(@Nullable WebBindingInitializer initializer,
							 List<SyncInvocableHandlerMethod> binderMethods) {

		super(initializer);
		this.binderMethods = binderMethods;
		this.binderMethodContext = new BindingContext(initializer);
	}

	/**
	 * 返回用于信号会话处理完成的{@link SessionStatus}实例。
	 *
	 * @return SessionStatus实例
	 */
	public SessionStatus getSessionStatus() {
		return this.sessionStatus;
	}

	/**
	 * 重写的初始化数据绑定方法，根据{@code @InitBinder}注解的条件调用绑定方法。
	 *
	 * @param dataBinder 数据绑定器
	 * @param exchange   服务器WebExchange
	 * @return 初始化后的WebExchangeDataBinder
	 */
	@Override
	protected WebExchangeDataBinder initDataBinder(WebExchangeDataBinder dataBinder, ServerWebExchange exchange) {
		this.binderMethods.stream()
				.filter(binderMethod -> {
					// 获取绑定方法上的@InitBinder注解
					InitBinder ann = binderMethod.getMethodAnnotation(InitBinder.class);
					// 如果不存在@InitBinder注解，则抛出异常。
					Assert.state(ann != null, "No InitBinder annotation");
					// 获取注解的值
					String[] names = ann.value();
					// 如果注解值为空或者包含数据绑定器的对象名称
					return (ObjectUtils.isEmpty(names) ||
							ObjectUtils.containsElement(names, dataBinder.getObjectName()));
				})
				// 对每个方法调用绑定方法
				.forEach(method -> invokeBinderMethod(dataBinder, exchange, method));

		// 返回数据绑定器
		return dataBinder;
	}

	/**
	 * 调用绑定方法的私有辅助方法。
	 *
	 * @param dataBinder   数据绑定器
	 * @param exchange     服务器WebExchange
	 * @param binderMethod 绑定方法
	 */
	private void invokeBinderMethod(
			WebExchangeDataBinder dataBinder, ServerWebExchange exchange, SyncInvocableHandlerMethod binderMethod) {

		// 调用 绑定方法 来执行处理程序方法，并获取处理结果
		HandlerResult result = binderMethod.invokeForHandlerResult(exchange, this.binderMethodContext, dataBinder);

		// 如果结果不为 null 并且返回值不为 null，则抛出 IllegalStateException 异常
		if (result != null && result.getReturnValue() != null) {
			throw new IllegalStateException(
					"@InitBinder methods must not return a value (should be void): " + binderMethod);
		}

		// 如果模型中存在属性（通过 binderMethodContext.getModel() 检查），则抛出 IllegalStateException 异常
		// 不应发生（没有模型参数解析）...
		if (!this.binderMethodContext.getModel().asMap().isEmpty()) {
			throw new IllegalStateException(
					"@InitBinder methods are not allowed to add model attributes: " + binderMethod);
		}
	}

	/**
	 * 设置会话上下文以在调用控制器方法后应用{@link #saveModel()}。
	 *
	 * @param attributesHandler SessionAttributesHandler实例
	 * @param session           WebSession实例
	 */
	public void setSessionContext(SessionAttributesHandler attributesHandler, WebSession session) {
		// 创建一个操作（Operation），用于保存模型数据
		this.saveModelOperation = () -> {
			// 检查会话状态是否已完成
			if (getSessionStatus().isComplete()) {
				// 如果会话已完成，则清理属性
				attributesHandler.cleanupAttributes(session);
			} else {
				// 如果会话未完成，则将模型中的属性存储到会话中
				attributesHandler.storeAttributes(session, getModel().asMap());
			}
		};
	}

	/**
	 * 根据{@code @SessionAttributes}注解中的类型级声明，将模型属性保存到会话中。
	 */
	public void saveModel() {
		if (this.saveModelOperation != null) {
			this.saveModelOperation.run();
		}
	}

}
