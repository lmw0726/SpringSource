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

package org.springframework.web.servlet.view.xml;

import org.springframework.lang.Nullable;
import org.springframework.oxm.Marshaller;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBElement;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * 允许响应上下文根据{@link Marshaller}进行编组的Spring-MVC {@link View}。
 *
 * <p>要编组的对象作为模型中的参数提供，然后在响应渲染期间进行{@linkplain #locateToBeMarshalled(Map) 检测}。用户可以通过{@link #setModelKey(String) sourceKey}属性指定模型中的特定条目，也可以让Spring定位源对象。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 */
public class MarshallingView extends AbstractView {

	/**
	 * 默认内容类型。可作为bean属性进行覆盖。
	 */
	public static final String DEFAULT_CONTENT_TYPE = "application/xml";

	/**
	 * 编组器
	 */
	@Nullable
	private Marshaller marshaller;

	/**
	 * 此模型中渲染的属性
	 */
	@Nullable
	private String modelKey;


	/**
	 * 构造一个没有设置{@link Marshaller}的新{@code MarshallingView}。
	 * 必须在构造之后通过调用{@link #setMarshaller}设置marshaller。
	 */
	public MarshallingView() {
		setContentType(DEFAULT_CONTENT_TYPE);
		// 不暴露路径变量
		setExposePathVariables(false);
	}

	/**
	 * 构造一个设置了给定{@link Marshaller}的新{@code MarshallingView}。
	 */
	public MarshallingView(Marshaller marshaller) {
		this();
		Assert.notNull(marshaller, "Marshaller must not be null");
		this.marshaller = marshaller;
	}


	/**
	 * 设置此视图要使用的{@link Marshaller}。
	 */
	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	/**
	 * 设置表示要编组的对象的模型键的名称。如果未指定，则将在模型映射中搜索受支持的值类型。
	 *
	 * @see Marshaller#supports(Class)
	 */
	public void setModelKey(String modelKey) {
		this.modelKey = modelKey;
	}

	@Override
	protected void initApplicationContext() {
		Assert.notNull(this.marshaller, "Property 'marshaller' is required");
	}


	@Override
	protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
										   HttpServletResponse response) throws Exception {

		// 定位被编组的对象
		Object toBeMarshalled = locateToBeMarshalled(model);
		if (toBeMarshalled == null) {
			// 如果无法定位到要编组的对象，则抛出异常
			throw new IllegalStateException("Unable to locate object to be marshalled in model: " + model);
		}

		Assert.state(this.marshaller != null, "No Marshaller set");

		// 使用 Marshaller 将对象编组到 ByteArrayOutputStream 中
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		this.marshaller.marshal(toBeMarshalled, new StreamResult(baos));

		// 设置响应的内容类型和内容长度，并将 ByteArrayOutputStream 写入到响应的输出流中
		setResponseContentType(request, response);
		response.setContentLength(baos.size());
		baos.writeTo(response.getOutputStream());
	}

	/**
	 * 定位要编组的对象。
	 * <p>默认实现首先尝试在配置的{@linkplain #setModelKey(String) 模型键}下查找（如果有的话），然后尝试定位{@linkplain Marshaller#supports(Class) 支持的类型}的对象。
	 *
	 * @param model 模型Map
	 * @return 要编组的对象（如果没有找到则为{@code null}）
	 * @throws IllegalStateException 如果模型对象由{@linkplain #setModelKey(String) 模型键}指定的不受marshaller支持
	 * @see #setModelKey(String)
	 */
	@Nullable
	protected Object locateToBeMarshalled(Map<String, Object> model) throws IllegalStateException {
		if (this.modelKey != null) {
			// 如果模型中存在指定的模型键，则获取该键对应的对象
			Object value = model.get(this.modelKey);
			if (value == null) {
				// 如果找不到指定键对应的对象，则抛出异常
				throw new IllegalStateException("Model contains no object with key [" + this.modelKey + "]");
			}
			if (!isEligibleForMarshalling(this.modelKey, value)) {
				// 如果获取的对象不支持编组，则抛出异常
				throw new IllegalStateException("Model object [" + value + "] retrieved via key [" +
						this.modelKey + "] is not supported by the Marshaller");
			}
			return value;
		}

		// 如果没有指定的模型键，则遍历模型中的条目，寻找适合编组的对象
		for (Map.Entry<String, Object> entry : model.entrySet()) {
			Object value = entry.getValue();
			if (value != null && (model.size() == 1 || !(value instanceof BindingResult)) &&
					isEligibleForMarshalling(entry.getKey(), value)) {
				return value;
			}
		}
		// 如果没有找到适合编组的对象，则返回 null
		return null;
	}

	/**
	 * 检查当前视图模型中给定值是否有资格通过配置的{@link Marshaller}进行编组。
	 * <p>默认实现调用{@link Marshaller#supports(Class)}，首先适用于给定的{@link JAXBElement}。
	 *
	 * @param modelKey 模型中值的键（永远不会是{@code null}）
	 * @param value    要检查的值（永远不会是{@code null}）
	 * @return 给定值是否应视为合格
	 * @see Marshaller#supports(Class)
	 */
	protected boolean isEligibleForMarshalling(String modelKey, Object value) {
		Assert.state(this.marshaller != null, "No Marshaller set");

		// 获取要检查的对象的类
		Class<?> classToCheck = value.getClass();

		// 如果对象是 JAXBElement 类型，则使用其声明的类型进行检查
		if (value instanceof JAXBElement) {
			classToCheck = ((JAXBElement<?>) value).getDeclaredType();
		}

		// 返回 Marshaller 是否支持该类
		return this.marshaller.supports(classToCheck);
	}

}
