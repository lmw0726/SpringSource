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

package org.springframework.context.annotation;

/**
 * 一个 {@link Condition}，当与 {@code @Configuration} 一起使用时提供更细粒度的控制。
 * 允许某些条件根据配置阶段（configuration phase）调整其匹配行为。
 * 例如，一个检查 bean 是否已注册的条件，可能选择仅在
 * {@link ConfigurationPhase#REGISTER_BEAN REGISTER_BEAN} {@link ConfigurationPhase} 阶段进行求值。
 *
 * @author Phillip Webb
 * @since 4.0
 * @see Configuration
 */
public interface ConfigurationCondition extends Condition {

	/**
	 * 返回该条件应被求值的 {@link ConfigurationPhase}。
	 */
	ConfigurationPhase getConfigurationPhase();


	/**
	 * 条件可能被求值的各种配置阶段。
	 */
	enum ConfigurationPhase {

		/**
		 * 当 {@code @Configuration} 类正在被解析时，应对 {@link Condition} 进行求值。
		 * <p>如果此时条件不匹配，则不会添加该 {@code @Configuration} 类。
		 */
		PARSE_CONFIGURATION,

		/**
		 * 当添加普通（非 {@code @Configuration}）bean 时，应对 {@link Condition} 进行求值。
		 * 该条件不会阻止 {@code @Configuration} 类被添加。
		 * <p>在条件被求值时，所有 {@code @Configuration} 类都将已被解析。
		 */
		REGISTER_BEAN
	}

}
