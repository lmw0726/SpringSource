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

package org.springframework.web.reactive.function.client;

import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * {@link ExchangeStrategies.Builder} 的默认实现。
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
final class DefaultExchangeStrategiesBuilder implements ExchangeStrategies.Builder {

	/**
	 * 交换策略
	 */
	final static ExchangeStrategies DEFAULT_EXCHANGE_STRATEGIES;

	static {
		// 创建默认交换策略的构建器
		DefaultExchangeStrategiesBuilder builder = new DefaultExchangeStrategiesBuilder();

		// 应用默认配置
		builder.defaultConfiguration();

		// 构建默认交换策略
		DEFAULT_EXCHANGE_STRATEGIES = builder.build();
	}


	/**
	 * 客户端加解码配置
	 */
	private final ClientCodecConfigurer codecConfigurer;


	public DefaultExchangeStrategiesBuilder() {
		this.codecConfigurer = ClientCodecConfigurer.create();
		this.codecConfigurer.registerDefaults(false);
	}

	private DefaultExchangeStrategiesBuilder(DefaultExchangeStrategies other) {
		this.codecConfigurer = other.codecConfigurer.clone();
	}


	public void defaultConfiguration() {
		this.codecConfigurer.registerDefaults(true);
	}

	@Override
	public ExchangeStrategies.Builder codecs(Consumer<ClientCodecConfigurer> consumer) {
		consumer.accept(this.codecConfigurer);
		return this;
	}

	@Override
	public ExchangeStrategies build() {
		return new DefaultExchangeStrategies(this.codecConfigurer);
	}


	/**
	 * 默认的 ExchangeStrategies 实现。
	 */
	private static class DefaultExchangeStrategies implements ExchangeStrategies {

		/**
		 * ClientCodecConfigurer 实例，客户端加解码配置。
		 */
		private final ClientCodecConfigurer codecConfigurer;

		/**
		 * 用于读取消息的读取器列表。
		 */
		private final List<HttpMessageReader<?>> readers;

		/**
		 * 用于写入消息的写入器列表。
		 */
		private final List<HttpMessageWriter<?>> writers;


		/**
		 * 构造一个 DefaultExchangeStrategies 实例。
		 *
		 * @param codecConfigurer ClientCodecConfigurer 实例
		 */
		public DefaultExchangeStrategies(ClientCodecConfigurer codecConfigurer) {
			this.codecConfigurer = codecConfigurer;
			this.readers = unmodifiableCopy(this.codecConfigurer.getReaders());
			this.writers = unmodifiableCopy(this.codecConfigurer.getWriters());
		}

		/**
		 * 创建一个不可修改的列表副本。
		 *
		 * @param list 要复制的列表
		 * @param <T>  列表中元素的类型
		 * @return 不可修改的列表副本
		 */
		private static <T> List<T> unmodifiableCopy(List<? extends T> list) {
			return Collections.unmodifiableList(new ArrayList<>(list));
		}

		/**
		 * 获取用于读取消息的读取器列表。
		 *
		 * @return 消息读取器列表
		 */
		@Override
		public List<HttpMessageReader<?>> messageReaders() {
			return this.readers;
		}

		/**
		 * 获取用于写入消息的写入器列表。
		 *
		 * @return 消息写入器列表
		 */
		@Override
		public List<HttpMessageWriter<?>> messageWriters() {
			return this.writers;
		}

		/**
		 * 创建一个新的 ExchangeStrategies 实例以进行修改。
		 *
		 * @return ExchangeStrategies.Builder 实例
		 */
		@Override
		public Builder mutate() {
			return new DefaultExchangeStrategiesBuilder(this);
		}
	}

}
