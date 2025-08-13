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

package org.springframework.core;

import org.reactivestreams.Publisher;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.function.Function;

/**
 * Reactive Streams {@link Publisher} 与异步/响应式类型（如 {@code CompletableFuture}、RxJava {@code Observable} 等）之间的适配器。
 *
 * <p>通常通过 {@link ReactiveAdapterRegistry} 获取适配器实例。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ReactiveAdapter {

	private final ReactiveTypeDescriptor descriptor;

	private final Function<Object, Publisher<?>> toPublisherFunction;

	private final Function<Publisher<?>, Object> fromPublisherFunction;


	/**
	 * 构造器，接收用于将目标响应式或异步类型转换为 Reactive Streams Publisher 及反向转换的函数。
	 *
	 * @param descriptor            响应式类型描述符
	 * @param toPublisherFunction   转换为 Publisher 的函数
	 * @param fromPublisherFunction 从 Publisher 转换的函数
	 */
	public ReactiveAdapter(ReactiveTypeDescriptor descriptor,
						   Function<Object, Publisher<?>> toPublisherFunction,
						   Function<Publisher<?>, Object> fromPublisherFunction) {

		Assert.notNull(descriptor, "'descriptor' is required");
		Assert.notNull(toPublisherFunction, "'toPublisherFunction' is required");
		Assert.notNull(fromPublisherFunction, "'fromPublisherFunction' is required");

		this.descriptor = descriptor;
		this.toPublisherFunction = toPublisherFunction;
		this.fromPublisherFunction = fromPublisherFunction;
	}


	/**
	 * 返回该适配器对应的响应式类型描述符。
	 */
	public ReactiveTypeDescriptor getDescriptor() {
		return this.descriptor;
	}

	/**
	 * {@code getDescriptor().getReactiveType()} 的快捷方法。
	 */
	public Class<?> getReactiveType() {
		return getDescriptor().getReactiveType();
	}

	/**
	 * {@code getDescriptor().isMultiValue()} 的快捷方法。
	 */
	public boolean isMultiValue() {
		return getDescriptor().isMultiValue();
	}

	/**
	 * {@code getDescriptor().isNoValue()} 的快捷方法。
	 */
	public boolean isNoValue() {
		return getDescriptor().isNoValue();
	}

	/**
	 * {@code getDescriptor().supportsEmpty()} 的快捷方法。
	 */
	public boolean supportsEmpty() {
		return getDescriptor().supportsEmpty();
	}


	/**
	 * 使给定实例适应反应流 {@code Publisher}。
	 *
	 * @param source 要适应的源对象; 如果给定对象为 {@code null}，则使用 {@link ReactiveTypeDescriptor#getEmptyValue()}。
	 * @return 代表适配了的Publisher
	 */
	@SuppressWarnings("unchecked")
	public <T> Publisher<T> toPublisher(@Nullable Object source) {
		if (source == null) {
			source = getDescriptor().getEmptyValue();
		}
		return (Publisher<T>) this.toPublisherFunction.apply(source);
	}

	/**
	 * 适配给定的反应式流 Publisher
	 *
	 * @param publisher 要适配的publisher
	 * @return 表示适配的发布者的响应类型实例
	 */
	public Object fromPublisher(Publisher<?> publisher) {
		return this.fromPublisherFunction.apply(publisher);
	}

}
