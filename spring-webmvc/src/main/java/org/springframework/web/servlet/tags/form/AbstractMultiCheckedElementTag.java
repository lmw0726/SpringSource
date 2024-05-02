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

package org.springframework.web.servlet.tags.form;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.servlet.jsp.JspException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * 抽象基类，提供用于实现数据绑定感知的 JSP 标签的通用方法，用于渲染<i>多个</i>带有 '{@code type}' 为 '{@code checkbox}' 或 '{@code radio}' 的 HTML '{@code input}' 元素。
 *
 * @author Juergen Hoeller
 * @author Scott Andrews
 * @since 2.5.2
 */
@SuppressWarnings("serial")
public abstract class AbstractMultiCheckedElementTag extends AbstractCheckedElementTag {

	/**
	 * HTML '{@code span}' 标签。
	 */
	private static final String SPAN_TAG = "span";


	/**
	 * 用于生成 '{@code input type="checkbox/radio"}' 标签的 {@link java.util.Collection}、{@link java.util.Map} 或对象数组。
	 */
	@Nullable
	private Object items;

	/**
	 * 映射到 '{@code input type="checkbox/radio"}' 标签的 '{@code value}' 属性的属性名称。
	 */
	@Nullable
	private String itemValue;

	/**
	 * 作为 '{@code input type="checkbox/radio"}' 标签的一部分显示的值。
	 */
	@Nullable
	private String itemLabel;

	/**
	 * 用于包裹 '{@code input type="checkbox/radio"}' 标签的 HTML 元素。
	 */
	private String element = SPAN_TAG;

	/**
	 * 用于在每个 '{@code input type="checkbox/radio"}' 标签之间使用的分隔符。
	 */
	@Nullable
	private String delimiter;


	/**
	 * 设置用于生成 '{@code input type="checkbox/radio"}' 标签的 {@link java.util.Collection}、{@link java.util.Map} 或对象数组。
	 * <p>通常是一个运行时表达式。
	 *
	 * @param items items
	 */
	public void setItems(Object items) {
		Assert.notNull(items, "'items' must not be null");
		this.items = items;
	}

	/**
	 * 获取用于生成 '{@code input type="checkbox/radio"}' 标签的 {@link java.util.Collection}、{@link java.util.Map} 或对象数组。
	 */
	@Nullable
	protected Object getItems() {
		return this.items;
	}

	/**
	 * 设置映射到 '{@code input type="checkbox/radio"}' 标签的 '{@code value}' 属性的属性名称。
	 * <p>可能是运行时表达式。
	 */
	public void setItemValue(String itemValue) {
		Assert.hasText(itemValue, "'itemValue' must not be empty");
		this.itemValue = itemValue;
	}

	/**
	 * 获取映射到 '{@code input type="checkbox/radio"}' 标签的 '{@code value}' 属性的属性名称。
	 */
	@Nullable
	protected String getItemValue() {
		return this.itemValue;
	}

	/**
	 * 设置作为 '{@code input type="checkbox/radio"}' 标签的一部分显示的值。
	 * <p>可能是运行时表达式。
	 */
	public void setItemLabel(String itemLabel) {
		Assert.hasText(itemLabel, "'itemLabel' must not be empty");
		this.itemLabel = itemLabel;
	}

	/**
	 * 获取作为 '{@code input type="checkbox/radio"}' 标签的一部分显示的值。
	 */
	@Nullable
	protected String getItemLabel() {
		return this.itemLabel;
	}

	/**
	 * 设置在每个 '{@code input type="checkbox/radio"}' 标签之间使用的分隔符。
	 * <p>默认情况下，没有分隔符。
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * 返回在每个 '{@code input type="radio"}' 标签之间使用的分隔符。
	 */
	@Nullable
	public String getDelimiter() {
		return this.delimiter;
	}

	/**
	 * 设置用于包裹 '{@code input type="checkbox/radio"}' 标签的 HTML 元素。
	 * <p>默认为 HTML '{@code <span/>}' 标签。
	 */
	public void setElement(String element) {
		Assert.hasText(element, "'element' cannot be null or blank");
		this.element = element;
	}

	/**
	 * 获取用于包裹 '{@code input type="checkbox/radio"}' 标签的 HTML 元素。
	 */
	public String getElement() {
		return this.element;
	}


	/**
	 * 由于我们正在处理多个 HTML 元素，因此将计数器追加到指定的 id 中。
	 */
	@Override
	protected String resolveId() throws JspException {
		// 评估 ID 属性
		Object id = evaluate("id", getId());
		// 如果 ID 不为空
		if (id != null) {
			// 将 ID 转换为字符串
			String idString = id.toString();
			// 如果 ID 字符串有文本，则使用 TagIdGenerator 生成下一个 ID
			return (StringUtils.hasText(idString) ? TagIdGenerator.nextId(idString, this.pageContext) : null);
		}
		// 否则，自动生成 ID
		return autogenerateId();
	}

	/**
	 * 使用配置的 {@link #setItems(Object)} 值渲染 '{@code input type="radio"}' 元素。如果值与绑定值匹配，则将元素标记为已选中。
	 */
	@Override
	@SuppressWarnings("rawtypes")
	protected int writeTagContent(TagWriter tagWriter) throws JspException {
		// 获取 items 属性的值
		Object items = getItems();
		// 如果 items 是字符串类型，则评估其值
		Object itemsObject = (items instanceof String ? evaluate("items", items) : items);

		// 获取 itemValue 和 itemLabel 属性的值
		String itemValue = getItemValue();
		String itemLabel = getItemLabel();
		// 获取值属性和标签属性的显示字符串
		String valueProperty =
				(itemValue != null ? ObjectUtils.getDisplayString(evaluate("itemValue", itemValue)) : null);
		String labelProperty =
				(itemLabel != null ? ObjectUtils.getDisplayString(evaluate("itemLabel", itemLabel)) : null);

		// 获取绑定状态的值类型
		Class<?> boundType = getBindStatus().getValueType();
		if (itemsObject == null && boundType != null && boundType.isEnum()) {
			// 如果 itemsObject 为空且绑定类型是枚举类型，则将枚举类型转换为枚举常量数组
			itemsObject = boundType.getEnumConstants();
		}

		// 如果 itemsObject 为空，则抛出异常
		if (itemsObject == null) {
			throw new IllegalArgumentException("Attribute 'items' is required and must be a Collection, an Array or a Map");
		}

		// 根据 itemsObject 的类型分别处理
		if (itemsObject.getClass().isArray()) {
			// 如果 itemsObject 是数组类型，则处理数组中的每个元素
			Object[] itemsArray = (Object[]) itemsObject;
			for (int i = 0; i < itemsArray.length; i++) {
				// 遍历每一个数组元素
				Object item = itemsArray[i];
				// 写入实体条目中
				writeObjectEntry(tagWriter, valueProperty, labelProperty, item, i);
			}
		} else if (itemsObject instanceof Collection) {
			// 如果 itemsObject 是集合类型，则处理集合中的每个元素
			final Collection<?> optionCollection = (Collection<?>) itemsObject;
			int itemIndex = 0;
			for (Iterator<?> it = optionCollection.iterator(); it.hasNext(); itemIndex++) {
				// 遍历每一个集合元素
				Object item = it.next();
				// 写入实体条目中
				writeObjectEntry(tagWriter, valueProperty, labelProperty, item, itemIndex);
			}
		} else if (itemsObject instanceof Map) {
			// 如果 itemsObject 是映射类型，则处理映射中的每个条目
			final Map<?, ?> optionMap = (Map<?, ?>) itemsObject;
			int itemIndex = 0;
			for (Iterator it = optionMap.entrySet().iterator(); it.hasNext(); itemIndex++) {
				// 获取每一个映射条目
				Map.Entry entry = (Map.Entry) it.next();
				// 写入映射条目中
				writeMapEntry(tagWriter, valueProperty, labelProperty, entry, itemIndex);
			}
		} else {
			// 如果 itemsObject 类型不支持，则抛出异常
			throw new IllegalArgumentException("Attribute 'items' must be an array, a Collection or a Map");
		}

		// 跳过标签体
		return SKIP_BODY;
	}

	private void writeObjectEntry(TagWriter tagWriter, @Nullable String valueProperty,
								  @Nullable String labelProperty, Object item, int itemIndex) throws JspException {

		// 使用 BeanWrapper 包装器获取属性值
		BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(item);
		Object renderValue;
		if (valueProperty != null) {
			// 如果值属性不为空，则获取该属性值
			renderValue = wrapper.getPropertyValue(valueProperty);
		} else if (item instanceof Enum) {
			// 如果 item 是枚举类型，则获取其枚举常量的名称作为值
			renderValue = ((Enum<?>) item).name();
		} else {
			// 否则直接使用 item 作为值
			renderValue = item;
		}
		// 如果标签属性不为空，则获取该属性值作为标签
		Object renderLabel = (labelProperty != null ? wrapper.getPropertyValue(labelProperty) : item);
		// 调用 writeElementTag 方法写入元素标签
		writeElementTag(tagWriter, item, renderValue, renderLabel, itemIndex);
	}

	private void writeMapEntry(TagWriter tagWriter, @Nullable String valueProperty,
							   @Nullable String labelProperty, Map.Entry<?, ?> entry, int itemIndex) throws JspException {

		// 获取 map 的键和值
		Object mapKey = entry.getKey();
		Object mapValue = entry.getValue();
		// 使用 BeanWrapper 包装器获取键和值的属性访问器
		BeanWrapper mapKeyWrapper = PropertyAccessorFactory.forBeanPropertyAccess(mapKey);
		BeanWrapper mapValueWrapper = PropertyAccessorFactory.forBeanPropertyAccess(mapValue);
		// 获取键的渲染值，如果值属性不为空，则获取该属性值，否则将键转换为字符串作为值
		Object renderValue = (valueProperty != null ?
				mapKeyWrapper.getPropertyValue(valueProperty) : mapKey.toString());
		// 获取值的渲染标签，如果标签属性不为空，则获取该属性值，否则将值转换为字符串作为标签
		Object renderLabel = (labelProperty != null ?
				mapValueWrapper.getPropertyValue(labelProperty) : mapValue.toString());
		// 调用 writeElementTag 方法写入元素标签
		writeElementTag(tagWriter, mapKey, renderValue, renderLabel, itemIndex);
	}

	private void writeElementTag(TagWriter tagWriter, Object item, @Nullable Object value,
								 @Nullable Object label, int itemIndex) throws JspException {

		// 开始标签
		tagWriter.startTag(getElement());
		if (itemIndex > 0) {
			// 如果索引大于 0，则添加分隔符
			Object resolvedDelimiter = evaluate("delimiter", getDelimiter());
			if (resolvedDelimiter != null) {
				tagWriter.appendValue(resolvedDelimiter.toString());
			}
		}
		// 开始输入标签
		tagWriter.startTag("input");
		// 解析并写入 id 属性
		String id = resolveId();
		Assert.state(id != null, "Attribute 'id' is required");
		writeOptionalAttribute(tagWriter, "id", id);
		// 写入 name 属性
		writeOptionalAttribute(tagWriter, "name", getName());
		// 写入可选属性
		writeOptionalAttributes(tagWriter);
		// 写入输入类型属性
		tagWriter.writeAttribute("type", getInputType());
		// 渲染值
		renderFromValue(item, value, tagWriter);
		// 结束输入标签
		tagWriter.endTag();
		// 开始标签
		tagWriter.startTag("label");
		// 写入关联的 for 属性
		tagWriter.writeAttribute("for", id);
		// 追加标签值
		tagWriter.appendValue(convertToDisplayString(label));
		// 结束标签
		tagWriter.endTag();
		// 结束标签
		tagWriter.endTag();
	}

}
