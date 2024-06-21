/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.http.converter.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

import java.text.SimpleDateFormat;

/**
 * 用于创建 Google Gson 2.x {@link Gson} 实例的 {@link FactoryBean}。
 *
 * <p>此工厂bean可以根据配置创建 {@code Gson} 实例，支持以下配置选项：
 * <ul>
 * <li>{@link #setBase64EncodeByteArrays(boolean)}：是否对 {@code byte[]} 属性进行 Base64 编码。</li>
 * <li>{@link #setSerializeNulls(boolean)}：是否在写入 JSON 时序列化空值。</li>
 * <li>{@link #setPrettyPrinting(boolean)}：是否在写入 JSON 时进行格式化输出。</li>
 * <li>{@link #setDisableHtmlEscaping(boolean)}：是否在写入 JSON 时禁用 HTML 转义。</li>
 * <li>{@link #setDateFormatPattern(String)}：定义日期/时间格式的模式。</li>
 * </ul>
 *
 * <p>通过配置这些选项，可以快速创建自定义的 {@code Gson} 实例。
 *
 * @author Roy Clarkson
 * @author Juergen Hoeller
 * @since 4.1
 */
public class GsonFactoryBean implements FactoryBean<Gson>, InitializingBean {

	/**
	 * 是否对字节数组进行 Base64 编码
	 */
	private boolean base64EncodeByteArrays = false;

	/**
	 * 是否在写入 JSON 时序列化空值
	 */
	private boolean serializeNulls = false;

	/**
	 * 是否美化打印
	 */
	private boolean prettyPrinting = false;

	/**
	 * 是否禁用 HTML 转义
	 */
	private boolean disableHtmlEscaping = false;

	/**
	 * 日期/时间格式的模式
	 */
	@Nullable
	private String dateFormatPattern;

	/**
	 * Gson 实例
	 */
	@Nullable
	private Gson gson;


	/**
	 * 设置是否对 {@code byte[]} 属性进行 Base64 编码。
	 * <p>当设置为 {@code true} 时，将通过 {@link GsonBuilder#registerTypeHierarchyAdapter(Class, Object)} 注册
	 * 自定义的 {@link com.google.gson.TypeAdapter}，将 {@code byte[]} 属性序列化为 Base64 编码的字符串，而不是 JSON 数组。
	 *
	 * @param base64EncodeByteArrays 是否对 {@code byte[]} 属性进行 Base64 编码
	 * @see GsonBuilderUtils#gsonBuilderWithBase64EncodedByteArrays()
	 */
	public void setBase64EncodeByteArrays(boolean base64EncodeByteArrays) {
		this.base64EncodeByteArrays = base64EncodeByteArrays;
	}

	/**
	 * 设置是否在写入 JSON 时序列化空值。
	 * <p>这是一个快捷方式，相当于通过以下方式设置一个 {@code Gson} 实例：
	 * <pre class="code">
	 * new GsonBuilder().serializeNulls().create();
	 * </pre>
	 *
	 * @param serializeNulls 是否序列化空值
	 */
	public void setSerializeNulls(boolean serializeNulls) {
		this.serializeNulls = serializeNulls;
	}

	/**
	 * 设置是否在写入 JSON 时进行格式化输出。
	 * <p>这是一个快捷方式，相当于通过以下方式设置一个 {@code Gson} 实例：
	 * <pre class="code">
	 * new GsonBuilder().setPrettyPrinting().create();
	 * </pre>
	 *
	 * @param prettyPrinting 是否进行格式化输出
	 */
	public void setPrettyPrinting(boolean prettyPrinting) {
		this.prettyPrinting = prettyPrinting;
	}

	/**
	 * 设置是否在写入 JSON 时禁用 HTML 转义。
	 * <p>设置为 {@code true} 表示禁用 JSON 中的 HTML 转义。这是一个快捷方式，相当于通过以下方式设置一个 {@code Gson} 实例：
	 * <pre class="code">
	 * new GsonBuilder().disableHtmlEscaping().create();
	 * </pre>
	 *
	 * @param disableHtmlEscaping 是否禁用 HTML 转义
	 */
	public void setDisableHtmlEscaping(boolean disableHtmlEscaping) {
		this.disableHtmlEscaping = disableHtmlEscaping;
	}

	/**
	 * 定义日期/时间格式的模式，使用 {@link SimpleDateFormat} 样式的模式。
	 * <p>这是一个快捷方式，相当于通过以下方式设置一个 {@code Gson} 实例：
	 * <pre class="code">
	 * new GsonBuilder().setDateFormat(dateFormatPattern).create();
	 * </pre>
	 *
	 * @param dateFormatPattern 日期/时间格式的模式
	 */
	public void setDateFormatPattern(String dateFormatPattern) {
		this.dateFormatPattern = dateFormatPattern;
	}

	/**
	 * 在设置属性后执行初始化操作，创建 {@code Gson} 实例。
	 */
	@Override
	public void afterPropertiesSet() {
		// 如果需要对字节数组进行 Base64 编码，则使用GsonBuilderUtils提供的方法创建 Gson构建器；
		// 否则直接创建新的Gson构建器
		GsonBuilder builder = (this.base64EncodeByteArrays ?
				GsonBuilderUtils.gsonBuilderWithBase64EncodedByteArrays() : new GsonBuilder());

		// 如果设置了序列化空值，则开启序列化null值
		if (this.serializeNulls) {
			builder.serializeNulls();
		}

		// 如果设置了美化打印，则开启美化输出
		if (this.prettyPrinting) {
			builder.setPrettyPrinting();
		}

		// 如果设置了禁用 HTML 转义 ，则禁用HTML转义
		if (this.disableHtmlEscaping) {
			builder.disableHtmlEscaping();
		}

		// 如果设置了 日期/时间格式的模式，则设置日期格式
		if (this.dateFormatPattern != null) {
			builder.setDateFormat(this.dateFormatPattern);
		}

		// 使用配置好的 Gson构建器 创建Gson实例
		this.gson = builder.create();
	}


	/**
	 * 返回创建的 Gson 实例。
	 *
	 * @return Gson 实例
	 */
	@Override
	@Nullable
	public Gson getObject() {
		return this.gson;
	}

	@Override
	public Class<?> getObjectType() {
		return Gson.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
