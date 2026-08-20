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

package org.springframework.jmx.export.metadata;

import java.lang.reflect.Method;

import org.springframework.lang.Nullable;

/**
 * {@code MetadataMBeanInfoAssembler} 使用此接口从被管理资源的类中读取源级别的元数据。
 *
 * @author Rob Harrop
 * @author Jennifer Hickey
 * @since 1.2
 * @see org.springframework.jmx.export.assembler.MetadataMBeanInfoAssembler#setAttributeSource
 * @see org.springframework.jmx.export.MBeanExporter#setAssembler
 */
public interface JmxAttributeSource {

	/**
	 * 如果传入的 {@code Class} 包含相应的元数据，则实现应返回 {@code ManagedResource} 实例；否则应返回 {@code null}。
	 * @param clazz 要读取属性数据的类
	 * @return 属性，如果未找到则返回 {@code null}
	 * @throws InvalidMetadataException 当属性无效时抛出
	 */
	@Nullable
	ManagedResource getManagedResource(Class<?> clazz) throws InvalidMetadataException;

	/**
	 * 如果传入的 {@code Method} 包含相应的元数据，则实现应返回 {@code ManagedAttribute} 实例；否则应返回 {@code null}。
	 * @param method 要读取属性数据的方法
	 * @return 属性，如果未找到则返回 {@code null}
	 * @throws InvalidMetadataException 当属性无效时抛出
	 */
	@Nullable
	ManagedAttribute getManagedAttribute(Method method) throws InvalidMetadataException;

	/**
	 * 如果传入的 {@code Method} 包含相应的元数据，则实现应返回 {@code ManagedMetric} 实例；否则应返回 {@code null}。
	 * @param method 要读取属性数据的方法
	 * @return 指标，如果未找到则返回 {@code null}
	 * @throws InvalidMetadataException 当属性无效时抛出
	 */
	@Nullable
	ManagedMetric getManagedMetric(Method method) throws InvalidMetadataException;

	/**
	 * 如果传入的 {@code Method} 包含相应的元数据，则实现应返回 {@code ManagedOperation} 实例；否则应返回 {@code null}。
	 * @param method 要读取属性数据的方法
	 * @return 操作，如果未找到则返回 {@code null}
	 * @throws InvalidMetadataException 当属性无效时抛出
	 */
	@Nullable
	ManagedOperation getManagedOperation(Method method) throws InvalidMetadataException;

	/**
	 * 如果传入的 {@code Method} 包含相应的元数据，则实现应返回 {@code ManagedOperationParameter} 数组；否则如果未找到元数据则应返回空数组。
	 * @param method 要读取元数据的 {@code Method}
	 * @return 参数信息
	 * @throws InvalidMetadataException 当属性无效时抛出
	 */
	ManagedOperationParameter[] getManagedOperationParameters(Method method) throws InvalidMetadataException;

	/**
	 * 如果传入的 {@code Class} 包含相应的元数据，则实现应返回 {@link ManagedNotification ManagedNotifications} 数组；否则应返回空数组。
	 * @param clazz 要读取元数据的 {@code Class}
	 * @return 通知信息
	 * @throws InvalidMetadataException 当元数据无效时抛出
	 */
	ManagedNotification[] getManagedNotifications(Class<?> clazz) throws InvalidMetadataException;



}
