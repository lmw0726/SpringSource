/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.aop.framework;

/**
 * 要注册到 {@link ProxyCreatorSupport} 对象的监听器。
 * 允许接收有关激活和通知更改的回调。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see ProxyCreatorSupport#addListener
 */
public interface AdvisedSupportListener {

	/**
	 * 在创建第一个代理时调用。
	 * @param advised AdvisedSupport 对象
	 */
	void activated(AdvisedSupport advised);

	/**
	 * 在创建代理后更改通知时调用。
	 * @param advised AdvisedSupport 对象
	 */
	void adviceChanged(AdvisedSupport advised);

}
