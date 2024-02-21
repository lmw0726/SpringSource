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

package org.springframework.core.convert.support;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;

import java.util.*;

/**
 * 将一个 Map 转换为另一个 Map。
 *
 * <p>首先，创建一个大小等于源 Map 大小的请求 targetType 的新 Map。然后将源 Map 中的每个元素复制到目标 Map 中。
 * 如果需要，将从源 Map 的参数化 K、V 类型转换为目标 Map 的参数化类型 K、V。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
final class MapToMapConverter implements ConditionalGenericConverter {

	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;


	public MapToMapConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Map.class, Map.class));
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return canConvertKey(sourceType, targetType) && canConvertValue(sourceType, targetType);
	}

	@Override
	@Nullable
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		// 如果源对象为null，则返回null
		if (source == null) {
			return null;
		}

		// 将源对象转换为Map类型
		@SuppressWarnings("unchecked")
		Map<Object, Object> sourceMap = (Map<Object, Object>) source;

		// 如果目标类型不是源对象的实例并且源Map为空，则无需复制，直接返回源Map
		boolean copyRequired = !targetType.getType().isInstance(source);
		if (!copyRequired && sourceMap.isEmpty()) {
			return sourceMap;
		}

		// 获取目标Map的键和值的类型描述符
		TypeDescriptor keyDesc = targetType.getMapKeyTypeDescriptor();
		TypeDescriptor valueDesc = targetType.getMapValueTypeDescriptor();

		// 存储转换后的键值对
		List<MapEntry> targetEntries = new ArrayList<>(sourceMap.size());

		// 遍历源Map中的键值对，进行转换
		for (Map.Entry<Object, Object> entry : sourceMap.entrySet()) {
			Object sourceKey = entry.getKey();
			Object sourceValue = entry.getValue();
			// 转换键和值
			Object targetKey = convertKey(sourceKey, sourceType, keyDesc);
			Object targetValue = convertValue(sourceValue, sourceType, valueDesc);
			// 将转换后的键值对添加到目标条目列表中
			targetEntries.add(new MapEntry(targetKey, targetValue));
			// 如果键或值发生了变化，则需要复制
			if (sourceKey != targetKey || sourceValue != targetValue) {
				copyRequired = true;
			}
		}

		// 如果不需要复制，则直接返回源Map
		if (!copyRequired) {
			return sourceMap;
		}

		// 创建目标Map对象
		Map<Object, Object> targetMap = CollectionFactory.createMap(targetType.getType(),
				(keyDesc != null ? keyDesc.getType() : null), sourceMap.size());

		// 将转换后的键值对添加到目标Map中
		for (MapEntry entry : targetEntries) {
			entry.addToMap(targetMap);
		}
		return targetMap;
	}


	// internal helpers

	private boolean canConvertKey(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(sourceType.getMapKeyTypeDescriptor(),
				targetType.getMapKeyTypeDescriptor(), this.conversionService);
	}

	private boolean canConvertValue(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return ConversionUtils.canConvertElements(sourceType.getMapValueTypeDescriptor(),
				targetType.getMapValueTypeDescriptor(), this.conversionService);
	}

	@Nullable
	private Object convertKey(Object sourceKey, TypeDescriptor sourceType, @Nullable TypeDescriptor targetType) {
		if (targetType == null) {
			return sourceKey;
		}
		return this.conversionService.convert(sourceKey, sourceType.getMapKeyTypeDescriptor(sourceKey), targetType);
	}

	@Nullable
	private Object convertValue(Object sourceValue, TypeDescriptor sourceType, @Nullable TypeDescriptor targetType) {
		if (targetType == null) {
			return sourceValue;
		}
		return this.conversionService.convert(sourceValue, sourceType.getMapValueTypeDescriptor(sourceValue), targetType);
	}


	private static class MapEntry {

		@Nullable
		private final Object key;

		@Nullable
		private final Object value;

		public MapEntry(@Nullable Object key, @Nullable Object value) {
			this.key = key;
			this.value = value;
		}

		public void addToMap(Map<Object, Object> map) {
			map.put(this.key, this.value);
		}
	}

}
