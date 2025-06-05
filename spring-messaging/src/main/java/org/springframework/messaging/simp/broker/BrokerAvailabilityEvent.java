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

package org.springframework.messaging.simp.broker;

import org.springframework.context.ApplicationEvent;

/**
 * 当消息代理的可用性发生变化时，发布的事件。
 *
 * @author Andy Wilkinson
 */
public class BrokerAvailabilityEvent extends ApplicationEvent {

	private static final long serialVersionUID = -8156742505179181002L;

	private final boolean brokerAvailable;


	/**
	 * 创建一个新的 {@code BrokerAvailabilityEvent}。
	 *
	 * @param brokerAvailable {@code true} 表示代理可用，{@code false} 表示代理不可用
	 * @param source 发生变化的组件，通常是充当代理的组件，或作为外部代理的中继的组件。不能为空。
	 */
	public BrokerAvailabilityEvent(boolean brokerAvailable, Object source) {
		super(source);
		this.brokerAvailable = brokerAvailable;
	}

	public boolean isBrokerAvailable() {
		return this.brokerAvailable;
	}

	@Override
	public String toString() {
		return "BrokerAvailabilityEvent[available=" + this.brokerAvailable + ", " + getSource() + "]";
	}

}
