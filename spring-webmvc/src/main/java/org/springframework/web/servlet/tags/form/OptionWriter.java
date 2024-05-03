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

package org.springframework.web.servlet.tags.form;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.support.BindStatus;

import javax.servlet.jsp.JspException;
import java.beans.PropertyEditor;
import java.util.Collection;
import java.util.Map;

/**
 * 提供支持功能以根据某个源对象渲染一组 '{@code option}' 标签。此对象可以是数组、{@link Collection} 或 {@link Map}。
 * <h3>使用数组或 {@link Collection}:</h3>
 * <p>
 * 如果将数组或 {@link Collection} 源对象提供给渲染内部 '{@code option}' 标签，可以可选地指定对象上对应于
 * 渲染 '{@code option}' 的<em>值</em>（即 {@code valueProperty}）的属性名称，以及对应于<em>标签</em>
 * （即 {@code labelProperty}）的属性名称。然后在将数组/{@link Collection} 的每个元素渲染为 '{@code option}' 时使用这些属性。
 * 如果省略任一属性名称，则使用相应数组/{@link Collection} 元素的 {@link Object#toString()} 的值。但是，
 * 如果该项是枚举，则将使用 {@link Enum#name()} 作为默认值。
 * </p>
 * <h3>使用 {@link Map}:</h3>
 * <p>
 * 您还可以选择通过提供 {@link Map} 作为源对象来渲染 '{@code option}' 标签。
 * </p>
 * <p>
 * 如果您<strong>省略</strong> <em>值</em> 和 <em>标签</em> 的属性名称：
 * </p>
 * <ul>
 * <li>每个 {@link Map} 条目的 {@code key} 将对应于渲染的 '{@code option}' 的<em>值</em>，并且</li>
 * <li>每个 {@link Map} 条目的 {@code value} 将对应于渲染的 '{@code option}' 的<em>标签</em>。</li>
 * </ul>
 * <p>
 * 如果您<strong>提供</strong> <em>值</em> 和 <em>标签</em> 的属性名称：
 * </p>
 * <ul>
 * <li>渲染的 '{@code option}' 的<em>值</em> 将从与每个 {@link Map} 条目的 {@code key} 对应的对象上的
 * {@code valueProperty} 中检索，并且</li>
 * <li>渲染的 '{@code option}' 的<em>标签</em> 将从与每个 {@link Map} 条目的 {@code value} 对应的对象上的
 * {@code labelProperty} 中检索。</li>
 * </ul>
 * <h3>在使用这些方法时：</h3>
 * <ul>
 * <li>为 <em>值</em> 和 <em>标签</em> 指定的属性名称作为参数传递给
 * {@link #OptionWriter(Object, BindStatus, String, String, boolean) 构造函数}。</li>
 * <li>如果其键 {@link #isOptionSelected 匹配} 绑定到标签实例的值，则将 '{@code option}' 标记为 'selected'。</li>
 * </ul>
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Scott Andrews
 * @since 2.0
 */
class OptionWriter {

	/**
	 * 选项源
	 */
	private final Object optionSource;

	/**
	 * 绑定状态
	 */
	private final BindStatus bindStatus;

	/**
	 * 值属性
	 */
	@Nullable
	private final String valueProperty;

	/**
	 * 标签属性
	 */
	@Nullable
	private final String labelProperty;

	/**
	 * 是否对HTML进行转义
	 */
	private final boolean htmlEscape;


	/**
	 * 为提供的 {@code objectSource} 创建一个新的 {@code OptionWriter}。
	 *
	 * @param optionSource  {@code options} 的源（永远不会为 {@code null}）
	 * @param bindStatus    绑定值的 {@link BindStatus}（永远不会为 {@code null}）
	 * @param valueProperty 用于渲染 {@code option} 值的属性的名称（可选）
	 * @param labelProperty 用于渲染 {@code option} 标签的属性的名称（可选）
	 */
	public OptionWriter(Object optionSource, BindStatus bindStatus,
						@Nullable String valueProperty, @Nullable String labelProperty, boolean htmlEscape) {

		Assert.notNull(optionSource, "'optionSource' must not be null");
		Assert.notNull(bindStatus, "'bindStatus' must not be null");
		this.optionSource = optionSource;
		this.bindStatus = bindStatus;
		this.valueProperty = valueProperty;
		this.labelProperty = labelProperty;
		this.htmlEscape = htmlEscape;
	}


	/**
	 * 将配置的 {@link #optionSource} 的 '{@code option}' 标签写入提供的 {@link TagWriter}。
	 */
	public void writeOptions(TagWriter tagWriter) throws JspException {
		// 如果选项源是数组类型
		if (this.optionSource.getClass().isArray()) {
			// 从数组渲染选项
			renderFromArray(tagWriter);
		} else if (this.optionSource instanceof Collection) {
			// 如果选项源是集合类型
			// 从集合渲染选项
			renderFromCollection(tagWriter);
		} else if (this.optionSource instanceof Map) {
			// 如果选项源是映射类型
			// 从映射渲染选项
			renderFromMap(tagWriter);
		} else if (this.optionSource instanceof Class && ((Class<?>) this.optionSource).isEnum()) {
			// 如果选项源是枚举类型
			// 从枚举渲染选项
			renderFromEnum(tagWriter);
		} else {
			// 其他类型，抛出JspException异常
			throw new JspException(
					"Type [" + this.optionSource.getClass().getName() + "] is not valid for option items");
		}
	}

	/**
	 * 使用配置的 {@link #optionSource} 从数组中渲染 '{@code option}' 标签。
	 *
	 * @see #doRenderFromCollection(java.util.Collection, TagWriter)
	 */
	private void renderFromArray(TagWriter tagWriter) throws JspException {
		doRenderFromCollection(CollectionUtils.arrayToList(this.optionSource), tagWriter);
	}

	/**
	 * 使用提供的 {@link Map} 作为源对象渲染 '{@code option}' 标签。
	 *
	 * @see #renderOption(TagWriter, Object, Object, Object)
	 */
	private void renderFromMap(TagWriter tagWriter) throws JspException {
		Map<?, ?> optionMap = (Map<?, ?>) this.optionSource;
		// 遍历每一个选项
		for (Map.Entry<?, ?> entry : optionMap.entrySet()) {
			// 获取键
			Object mapKey = entry.getKey();
			// 获取值
			Object mapValue = entry.getValue();
			// 渲染值
			// 如果 值属性 存在，则使用PropertyAccessorFactory获取 键对应的属性值
			Object renderValue = (this.valueProperty != null ?
					PropertyAccessorFactory.forBeanPropertyAccess(mapKey).getPropertyValue(this.valueProperty) :
					// 否则，直接使用 键 作为渲染值
					mapKey);
			// 渲染标签
			Object renderLabel = (this.labelProperty != null ?
					// 如果标签属性存在，则使用PropertyAccessorFactory获取 值对应的属性值
					PropertyAccessorFactory.forBeanPropertyAccess(mapValue).getPropertyValue(this.labelProperty) :
					// 否则，直接使用 值 作为渲染标签
					mapValue);
			// 渲染选项
			renderOption(tagWriter, mapKey, renderValue, renderLabel);
		}
	}

	/**
	 * 使用配置的 {@link #optionSource} 从集合中渲染 '{@code option}' 标签。
	 *
	 * @see #doRenderFromCollection(java.util.Collection, TagWriter)
	 */
	private void renderFromCollection(TagWriter tagWriter) throws JspException {
		doRenderFromCollection((Collection<?>) this.optionSource, tagWriter);
	}

	/**
	 * 使用配置的 {@link #optionSource} 从枚举中渲染 '{@code option}' 标签。
	 *
	 * @see #doRenderFromCollection(java.util.Collection, TagWriter)
	 */
	private void renderFromEnum(TagWriter tagWriter) throws JspException {
		doRenderFromCollection(CollectionUtils.arrayToList(((Class<?>) this.optionSource).getEnumConstants()), tagWriter);
	}

	/**
	 * 使用提供的 {@link Collection} 对象作为源时，渲染 '{@code option}' 标签。
	 * 在渲染 '{@code option}' 的<em>值</em>时使用 {@link #valueProperty} 字段的值，
	 * 在渲染标签时使用 {@link #labelProperty} 属性的值。
	 */
	private void doRenderFromCollection(Collection<?> optionCollection, TagWriter tagWriter) throws JspException {
		// 遍历选项集合中的每个项
		for (Object item : optionCollection) {
			// 使用BeanWrapper访问项的属性
			BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(item);
			// 初始化值
			Object value;
			if (this.valueProperty != null) {
				// 如果指定了 值属性，则获取对应属性的值
				value = wrapper.getPropertyValue(this.valueProperty);
			} else if (item instanceof Enum) {
				// 如果项是枚举类型，则使用枚举常量的名称作为值
				value = ((Enum<?>) item).name();
			} else {
				// 否则，直接使用项作为值
				value = item;
			}
			// 初始化标签
			Object label = (this.labelProperty != null ? wrapper.getPropertyValue(this.labelProperty) : item);
			// 渲染选项
			renderOption(tagWriter, item, value, label);
		}
	}

	/**
	 * 渲染带有提供的值和标签的 HTML '{@code option}'。如果项目本身或其值与绑定的值匹配，则将值标记为 'selected'。
	 */
	private void renderOption(TagWriter tagWriter, Object item, @Nullable Object value, @Nullable Object label)
			throws JspException {

		// 开始<option>标签
		tagWriter.startTag("option");
		// 写入通用属性
		writeCommonAttributes(tagWriter);

		// 获取值的显示字符串和标签的显示字符串
		String valueDisplayString = getDisplayString(value);
		String labelDisplayString = getDisplayString(label);

		// 对值的显示字符串进行处理
		valueDisplayString = processOptionValue(valueDisplayString);

		// 将值的显示字符串写入"value"属性
		tagWriter.writeAttribute("value", valueDisplayString);

		// 如果选项被选中或者（值不等于项且项被选中）
		if (isOptionSelected(value) || (value != item && isOptionSelected(item))) {
			// 写入"selected"属性
			tagWriter.writeAttribute("selected", "selected");
		}

		// 如果选项被禁用
		if (isOptionDisabled()) {
			// 写入"disabled"属性
			tagWriter.writeAttribute("disabled", "disabled");
		}

		// 写入标签的显示字符串
		tagWriter.appendValue(labelDisplayString);

		// 结束<option>标签
		tagWriter.endTag();
	}

	/**
	 * 根据需要将提供的 {@code Object} 的显示值进行 HTML 转义。
	 */
	private String getDisplayString(@Nullable Object value) {
		PropertyEditor editor = (value != null ? this.bindStatus.findEditor(value.getClass()) : null);
		return ValueFormatter.getDisplayString(value, editor, this.htmlEscape);
	}

	/**
	 * 在写入之前处理选项值。
	 * <p>默认实现简单地返回相同的值不变。
	 */
	protected String processOptionValue(String resolvedValue) {
		return resolvedValue;
	}

	/**
	 * 确定提供的值是否与所选值匹配。
	 */
	private boolean isOptionSelected(@Nullable Object resolvedValue) {
		return SelectedValueComparator.isSelected(this.bindStatus, resolvedValue);
	}

	/**
	 * 确定是否应禁用选项字段。
	 */
	protected boolean isOptionDisabled() throws JspException {
		return false;
	}

	/**
	 * 向提供的 {@link TagWriter} 写入配置的默认属性。
	 */
	protected void writeCommonAttributes(TagWriter tagWriter) throws JspException {
	}

}
