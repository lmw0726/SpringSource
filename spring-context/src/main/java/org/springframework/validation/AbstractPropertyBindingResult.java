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

package org.springframework.validation;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConvertingPropertyEditorAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.beans.PropertyEditor;

/**
 * {@link BindingResult} 实现的抽象基类，适用于 Spring 的 {@link org.springframework.beans.PropertyAccessor} 机制。
 * 通过委托给相应的 PropertyAccessor 方法，预先实现了字段访问。
 *
 * @author Juergen Hoeller
 * @see #getPropertyAccessor()
 * @see org.springframework.beans.PropertyAccessor
 * @see org.springframework.beans.ConfigurablePropertyAccessor
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AbstractPropertyBindingResult extends AbstractBindingResult {
	/**
	 * 转换服务
	 */
	@Nullable
	private transient ConversionService conversionService;


	/**
	 * 创建一个新的 AbstractPropertyBindingResult 实例。
	 *
	 * @param objectName 目标对象的名称
	 * @see DefaultMessageCodesResolver
	 */
	protected AbstractPropertyBindingResult(String objectName) {
		super(objectName);
	}

	/**
	 * 初始化转换服务。
	 *
	 * @param conversionService 转换服务实例
	 * @throws IllegalArgumentException 如果 conversionService 为 {@code null}
	 */
	public void initConversion(ConversionService conversionService) {
		Assert.notNull(conversionService, "ConversionService must not be null");
		this.conversionService = conversionService;
		if (getTarget() != null) {
			// 如果目标对象不为空，则获取属性访问器后，设置它的转换服务
			getPropertyAccessor().setConversionService(conversionService);
		}
	}

	/**
	 * 返回底层的 PropertyAccessor。
	 *
	 * @see #getPropertyAccessor()
	 */
	@Override
	public PropertyEditorRegistry getPropertyEditorRegistry() {
		return (getTarget() != null ? getPropertyAccessor() : null);
	}

	/**
	 * 返回规范化的属性名称。
	 *
	 * @see org.springframework.beans.PropertyAccessorUtils#canonicalPropertyName
	 */
	@Override
	protected String canonicalFieldName(String field) {
		return PropertyAccessorUtils.canonicalPropertyName(field);
	}

	/**
	 * 从属性类型中确定字段类型。
	 *
	 * @see #getPropertyAccessor()
	 */
	@Override
	@Nullable
	public Class<?> getFieldType(@Nullable String field) {
		return (getTarget() != null ? getPropertyAccessor().getPropertyType(fixedField(field)) :
				super.getFieldType(field));
	}

	/**
	 * 从 PropertyAccessor 中获取字段值。
	 *
	 * @see #getPropertyAccessor()
	 */
	@Override
	@Nullable
	protected Object getActualFieldValue(String field) {
		return getPropertyAccessor().getPropertyValue(field);
	}

	/**
	 * 根据注册的 PropertyEditors 格式化字段值。
	 *
	 * @see #getCustomEditor
	 */
	@Override
	protected Object formatFieldValue(String field, @Nullable Object value) {
		// 获取修正字段名称
		String fixedField = fixedField(field);
		// 尝试使用自定义编辑器...
		PropertyEditor customEditor = getCustomEditor(fixedField);
		if (customEditor != null) {
			// 设置值
			customEditor.setValue(value);
			// 获取文本值
			String textValue = customEditor.getAsText();
			// 如果 PropertyEditor 返回 null，则没有适当的文本表示，仅在非空时使用。
			if (textValue != null) {
				return textValue;
			}
		}
		if (this.conversionService != null) {
			// 尝试使用自定义转换器...
			TypeDescriptor fieldDesc = getPropertyAccessor().getPropertyTypeDescriptor(fixedField);
			// 获取字符串类型描述符
			TypeDescriptor strDesc = TypeDescriptor.valueOf(String.class);
			if (fieldDesc != null && this.conversionService.canConvert(fieldDesc, strDesc)) {
				// 如果字符串类型描述符不为空，并且可以转换为字符串，将值转换为字符串。
				return this.conversionService.convert(value, fieldDesc, strDesc);
			}
		}
		return value;
	}

	/**
	 * 检索给定字段的自定义 PropertyEditor（如果有的话）。
	 *
	 * @param fixedField 完整的字段名称
	 * @return 自定义的 PropertyEditor，如果没有则返回 {@code null}
	 */
	@Nullable
	protected PropertyEditor getCustomEditor(String fixedField) {
		// 获取字段的目标类型
		Class<?> targetType = getPropertyAccessor().getPropertyType(fixedField);

		// 从属性访问器中查找与目标类型和字段名称匹配的自定义编辑器
		PropertyEditor editor = getPropertyAccessor().findCustomEditor(targetType, fixedField);

		// 如果未找到自定义编辑器，使用约定找到默认编辑器
		if (editor == null) {
			editor = BeanUtils.findEditorByConvention(targetType);
		}

		// 返回找到的编辑器
		return editor;
	}

	/**
	 * 该实现暴露了 PropertyEditor 适配器，以适应 Formatter（如果适用）。
	 */
	@Override
	@Nullable
	public PropertyEditor findEditor(@Nullable String field, @Nullable Class<?> valueType) {
		// 使用提供的值类型进行查找，如果值类型为 null，则获取字段类型
		Class<?> valueTypeForLookup = valueType;
		if (valueTypeForLookup == null) {
			valueTypeForLookup = getFieldType(field);
		}

		// 从父类中查找编辑器
		PropertyEditor editor = super.findEditor(field, valueTypeForLookup);

		// 如果未找到编辑器且存在转换服务
		if (editor == null && this.conversionService != null) {
			TypeDescriptor td = null;

			// 如果字段和目标对象都存在
			if (field != null && getTarget() != null) {
				// 获取字段的属性类型描述符
				TypeDescriptor ptd = getPropertyAccessor().getPropertyTypeDescriptor(fixedField(field));
				if (ptd != null && (valueType == null || valueType.isAssignableFrom(ptd.getType()))) {
					// 使用字段的类型描述符
					td = ptd;
				}
			}

			// 如果没有找到字段的类型描述符，则使用值类型进行描述
			if (td == null) {
				td = TypeDescriptor.valueOf(valueTypeForLookup);
			}

			// 如果转换服务可以将字符串类型转换为目标类型，则创建一个转换编辑器
			if (this.conversionService.canConvert(TypeDescriptor.valueOf(String.class), td)) {
				editor = new ConvertingPropertyEditorAdapter(this.conversionService, td);
			}
		}

		// 返回找到的编辑器
		return editor;
	}


	/**
	 * 提供要使用的 PropertyAccessor，依据具体的访问策略。
	 * <p>请注意，BindingResult 使用的 PropertyAccessor 应该
	 * 默认为“extractOldValueForEditor”标志设置为“true”，
	 * 因为这通常可以在不产生副作用的情况下进行，
	 * 适用于作为数据绑定目标的模型对象。
	 *
	 * @see ConfigurablePropertyAccessor#setExtractOldValueForEditor
	 */
	public abstract ConfigurablePropertyAccessor getPropertyAccessor();

}
