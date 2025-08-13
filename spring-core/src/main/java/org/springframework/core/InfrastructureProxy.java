/*
 * Copyright 2002-2012 the original author or authors.
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

/**
 * 透明资源代理需要实现的接口，用于在比较时被视为与底层资源相等，
 * 例如在一致性查找键比较中。注意，此接口并不意味着通用的混入功能，
 * 而是具有特定语义的。
 *
 * <p>此类包装器会在
 * {@link org.springframework.transaction.support.TransactionSynchronizationManager}
 * 中自动解包以用于键比较。
 *
 * <p>只有完全透明的代理，例如重定向或服务查找代理，才应实现此接口。
 * 装饰目标对象并添加新行为的代理（如AOP代理）不属于此类。
 *
 * @author Juergen Hoeller
 * @since 2.5.4
 * @see org.springframework.transaction.support.TransactionSynchronizationManager
 */
public interface InfrastructureProxy {

	/**
	 * 返回底层资源（绝不为 {@code null}）。
	 */
	Object getWrappedObject();

}
