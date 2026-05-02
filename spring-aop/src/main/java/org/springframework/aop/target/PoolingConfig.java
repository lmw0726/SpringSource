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

package org.springframework.aop.target;

/**
 * 池化目标源的配置接口。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface PoolingConfig {

	/**
	 * 返回池的最大大小。
	 */
	int getMaxSize();

	/**
	 * 返回池中活动对象的数量。
	 * @throws UnsupportedOperationException 如果池不支持此操作
	 */
	int getActiveCount() throws UnsupportedOperationException;

	/**
	 * 返回池中空闲对象的数量。
	 * @throws UnsupportedOperationException 如果池不支持此操作
	 */
	int getIdleCount() throws UnsupportedOperationException;

}
