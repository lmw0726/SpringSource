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

package org.springframework.core.io.buffer;

/**
 * {@link DataBuffer} 的扩展，支持共享内存池的缓冲区。
 * 引入了引用计数的方法。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface PooledDataBuffer extends DataBuffer {

	/**
	 * 如果该缓冲区已分配，返回 {@code true}；
	 * 如果已释放，返回 {@code false}。
	 * @since 5.1
	 */
	boolean isAllocated();

	/**
	 * 增加该缓冲区的引用计数1。
	 * @return 返回该缓冲区自身
	 */
	PooledDataBuffer retain();

	/**
	 * 为调试目的关联给定的提示信息到数据缓冲区。
	 * @return 返回该缓冲区自身
	 * @since 5.3.2
	 */
	PooledDataBuffer touch(Object hint);

	/**
	 * 减少该缓冲区的引用计数1，
	 * 当计数归零时释放缓冲区。
	 * @return 如果缓冲区已释放返回 {@code true}；
	 * 否则返回 {@code false}
	 */
	boolean release();

}
