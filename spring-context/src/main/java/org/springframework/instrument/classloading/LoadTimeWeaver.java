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

package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;

/**
 * 定义向 {@link ClassLoader} 添加一个或多个
 * {@link ClassFileTransformer ClassFileTransformers} 的契约。
 *
 * <p>实现类可以操作当前上下文的 {@code ClassLoader}，
 * 也可以暴露自身的可检测 {@code ClassLoader}。
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @since 2.0
 * @see java.lang.instrument.ClassFileTransformer
 */
public interface LoadTimeWeaver {

	/**
	 * 添加一个 {@code ClassFileTransformer}，由本
	 * {@code LoadTimeWeaver} 应用。
	 * @param transformer 要添加的 {@code ClassFileTransformer}
	 */
	void addTransformer(ClassFileTransformer transformer);

	/**
	 * 返回一个支持基于用户自定义 {@link ClassFileTransformer ClassFileTransformers}
	 * 的 AspectJ 风格加载时织入（load-time weaving）的检测的 {@code ClassLoader}。
	 * <p>可以是当前的 {@code ClassLoader}，也可以是由本
	 * {@link LoadTimeWeaver} 实例创建的 {@code ClassLoader}。
	 * @return 根据已注册的转换器暴露已检测类的 {@code ClassLoader}
	 */
	ClassLoader getInstrumentableClassLoader();

	/**
	 * 返回一个临时的 {@code ClassLoader}，使类能够被加载和检查
	 * 而不影响父 {@code ClassLoader}。
	 * <p><i>不</i>应返回与 {@link #getInstrumentableClassLoader()} 调用
	 * 返回的同一实例。
	 * @return 一个临时的 {@code ClassLoader}；每次调用应返回新实例，
	 * 不带任何已有状态
	 */
	ClassLoader getThrowawayClassLoader();

}
