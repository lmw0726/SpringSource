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

package org.springframework.web.context.request;

import org.springframework.util.Assert;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * RequestAttributes 实现的抽象支持类，为请求特定的销毁回调和更新已访问的会话属性提供请求完成机制。
 *
 * @author Juergen Hoeller
 * @see #requestCompleted()
 * @since 2.0
 */
public abstract class AbstractRequestAttributes implements RequestAttributes {

	/**
	 * 属性名称字符串到销毁回调 Runnable 的映射。
	 */
	protected final Map<String, Runnable> requestDestructionCallbacks = new LinkedHashMap<>(8);

	/**
	 * 请求是否存活
	 */
	private volatile boolean requestActive = true;


	/**
	 * 表示请求已完成。
	 * <p>执行所有请求销毁回调并更新在请求处理期间已访问的会话属性。
	 */
	public void requestCompleted() {
		// 执行请求销毁回调
		executeRequestDestructionCallbacks();
		// 更新访问过的会话属性
		updateAccessedSessionAttributes();
		// 设置请求不再活跃
		this.requestActive = false;
	}

	/**
	 * 确定原始请求是否仍然活动。
	 *
	 * @see #requestCompleted()
	 */
	protected final boolean isRequestActive() {
		return this.requestActive;
	}

	/**
	 * 注册给定的回调以在请求完成后执行。
	 *
	 * @param name     要为其注册回调的属性的名称
	 * @param callback 要在销毁时执行的回调
	 */
	protected final void registerRequestDestructionCallback(String name, Runnable callback) {
		Assert.notNull(name, "Name must not be null");
		Assert.notNull(callback, "Callback must not be null");
		synchronized (this.requestDestructionCallbacks) {
			this.requestDestructionCallbacks.put(name, callback);
		}
	}

	/**
	 * 删除指定属性的请求销毁回调（如果有）。
	 *
	 * @param name 要移除回调的属性的名称
	 */
	protected final void removeRequestDestructionCallback(String name) {
		Assert.notNull(name, "Name must not be null");
		synchronized (this.requestDestructionCallbacks) {
			this.requestDestructionCallbacks.remove(name);
		}
	}

	/**
	 * 执行已注册的所有在请求完成后执行的回调。
	 */
	private void executeRequestDestructionCallbacks() {
		// 使用请求销毁回调列表进行同步
		synchronized (this.requestDestructionCallbacks) {
			// 遍历请求销毁回调列表中的每个回调对象
			for (Runnable runnable : this.requestDestructionCallbacks.values()) {
				// 执行其run()方法
				runnable.run();
			}
			// 清空请求销毁回调列表
			this.requestDestructionCallbacks.clear();
		}
	}

	/**
	 * 更新在请求处理期间已访问的所有会话属性，以将其潜在更新的状态暴露给底层会话管理器。
	 */
	protected abstract void updateAccessedSessionAttributes();

}
