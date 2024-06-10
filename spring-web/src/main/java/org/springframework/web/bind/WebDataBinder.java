/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.bind;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.core.CollectionFactory;
import org.springframework.lang.Nullable;
import org.springframework.validation.DataBinder;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 专门用于将Web请求参数绑定到JavaBean对象的{@link DataBinder}。设计用于Web环境，但不依赖于Servlet API；
 * 用作更具体的DataBinder变体的基类，例如{@link org.springframework.web.bind.ServletRequestDataBinder}。
 *
 * <p><strong>警告</strong>：数据绑定可能会导致安全问题，通过公开不应由外部客户端访问或修改的对象图的部分。
 * 因此，应仔细考虑数据绑定的设计和使用，涉及到安全性。
 * 有关详细信息，请参阅专门针对Spring Web MVC和Spring WebFlux的数据绑定的章节，
 * 链接分别是<a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web.html#mvc-ann-initbinder-model-design">Spring Web MVC</a>
 * 和<a href="https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-ann-initbinder-model-design">Spring WebFlux</a>。
 *
 * <p>包括对字段标记的支持，这些字段标记解决了HTML复选框和选择选项的一个常见问题：
 * 检测到字段是表单的一部分，但由于为空而未生成请求参数。
 * 字段标记允许检测到该状态并相应地重置相应的bean属性。
 * 对于否则不存在的参数的默认值，可以为字段指定一个值，而不是空值。
 *
 * @author Juergen Hoeller
 * @author Scott Andrews
 * @author Brian Clozel
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 * @see #setFieldDefaultPrefix
 * @see ServletRequestDataBinder
 * @since 1.2
 */
public class WebDataBinder extends DataBinder {

	/**
	 * 字段标记参数的默认前缀，后跟字段名称：例如对于字段“subscribeToNewsletter”，前缀为“_subscribeToNewsletter”。
	 * <p>这样的标记参数表示字段是可见的，即存在于导致提交的表单中。
	 * 如果未找到相应的字段值参数，则将重置字段。
	 * 在这种情况下，字段标记参数的值无关紧要；可以使用任意值。
	 * 这对HTML复选框和选择选项特别有用。
	 *
	 * @see #setFieldMarkerPrefix
	 */
	public static final String DEFAULT_FIELD_MARKER_PREFIX = "_";

	/**
	 * 字段默认参数的默认前缀，后跟字段名称：例如对于字段“subscribeToNewsletter”，前缀为“!subscribeToNewsletter”。
	 * <p>默认参数与字段标记不同，它们提供默认值而不是空值。
	 *
	 * @see #setFieldDefaultPrefix
	 */
	public static final String DEFAULT_FIELD_DEFAULT_PREFIX = "!";

	/**
	 * 字段标记前缀，默认为 “_”。
	 */
	@Nullable
	private String fieldMarkerPrefix = DEFAULT_FIELD_MARKER_PREFIX;

	/**
	 * 字段默认前缀，默认为 “!”。
	 */
	@Nullable
	private String fieldDefaultPrefix = DEFAULT_FIELD_DEFAULT_PREFIX;

	/**
	 * 是否绑定空的多部分文件，默认为 true。
	 */
	private boolean bindEmptyMultipartFiles = true;


	/**
	 * 创建一个新的WebDataBinder实例，具有默认的对象名称。
	 *
	 * @param target 要绑定到的目标对象（如果仅将绑定器用于转换纯参数值，则为{@code null}）
	 * @see #DEFAULT_OBJECT_NAME
	 */
	public WebDataBinder(@Nullable Object target) {
		super(target);
	}

	/**
	 * 创建一个新的WebDataBinder实例。
	 *
	 * @param target     要绑定到的目标对象（如果仅将绑定器用于转换纯参数值，则为{@code null}）
	 * @param objectName 目标对象的名称
	 */
	public WebDataBinder(@Nullable Object target, String objectName) {
		super(target, objectName);
	}


	/**
	 * 指定可用于标记可能为空字段的参数的前缀，以“prefix + field”为名称。这样的标记参数通过存在性检查：您可以为其发送任何值，例如“visible”。
	 * 对于HTML复选框和选择选项，这特别有用。
	 * <p>默认值为“_”，用于“_FIELD”参数（例如“_subscribeToNewsletter”）。
	 * 如果要完全关闭空字段检查，请将其设置为null。
	 * <p>HTML复选框仅在选中时发送值，因此无法检测到以前已选中的框是否刚刚取消选中，至少不是通过标准HTML手段。
	 * <p>解决此问题的一种方法是，如果知道复选框已在表单中可见，则查找复选框参数值，如果找不到值，则重置复选框。在Spring Web MVC中，这通常在自定义的{@code onBind}实现中完成。
	 * <p>此自动重置机制解决了这个缺陷，只要为每个复选框字段发送一个标记参数，例如“subscribeToNewsletter”的“_subscribeToNewsletter”。
	 * 由于无论如何都会发送标记参数，因此数据绑定器可以检测到空字段并自动重置其值。
	 *
	 * @see #DEFAULT_FIELD_MARKER_PREFIX
	 */
	public void setFieldMarkerPrefix(@Nullable String fieldMarkerPrefix) {
		this.fieldMarkerPrefix = fieldMarkerPrefix;
	}

	/**
	 * 返回标记可能为空字段的参数的前缀。
	 */
	@Nullable
	public String getFieldMarkerPrefix() {
		return this.fieldMarkerPrefix;
	}

	/**
	 * 指定可用于指示默认值字段的参数的前缀，以“prefix + field”为名称。
	 * 如果字段未提供，则使用默认字段的值。
	 * <p>默认值为“!”，用于“!FIELD”参数（例如“!subscribeToNewsletter”）。
	 * 如果要完全关闭字段默认值，请将其设置为null。
	 * <p>HTML复选框仅在选中时发送值，因此无法检测到以前已选中的框是否刚刚取消选中，至少不是通过标准HTML手段。
	 * 当复选框表示非布尔值时，默认字段特别有用。
	 * <p>默认参数的存在会优先于给定字段的字段标记的行为。
	 *
	 * @see #DEFAULT_FIELD_DEFAULT_PREFIX
	 */
	public void setFieldDefaultPrefix(@Nullable String fieldDefaultPrefix) {
		this.fieldDefaultPrefix = fieldDefaultPrefix;
	}

	/**
	 * 返回指示默认字段的参数的前缀。
	 */
	@Nullable
	public String getFieldDefaultPrefix() {
		return this.fieldDefaultPrefix;
	}

	/**
	 * 设置是否绑定空的MultipartFile参数。默认值为“true”。
	 * <p>如果希望在用户重新提交表单而不选择不同文件时保留已绑定的MultipartFile，则关闭此选项。
	 * 否则，已绑定的MultipartFile将被空的MultipartFile持有者替换。
	 *
	 * @see org.springframework.web.multipart.MultipartFile
	 */
	public void setBindEmptyMultipartFiles(boolean bindEmptyMultipartFiles) {
		this.bindEmptyMultipartFiles = bindEmptyMultipartFiles;
	}

	/**
	 * 返回是否绑定空的MultipartFile参数。
	 */
	public boolean isBindEmptyMultipartFiles() {
		return this.bindEmptyMultipartFiles;
	}


	/**
	 * 在委托给超类绑定处理之前，此实现执行字段默认和标记检查。
	 *
	 * @see #checkFieldDefaults
	 * @see #checkFieldMarkers
	 */
	@Override
	protected void doBind(MutablePropertyValues mpvs) {
		// 检查字段的默认值
		checkFieldDefaults(mpvs);

		// 检查字段标记
		checkFieldMarkers(mpvs);

		// 调整空数组的索引
		adaptEmptyArrayIndices(mpvs);

		// 调用父类的 doBind 方法，执行绑定操作
		super.doBind(mpvs);
	}

	/**
	 * 检查给定的属性值以查找字段默认值，即以字段默认前缀开头的字段。
	 * <p>字段默认值的存在表示如果字段否则不存在，则应使用指定的值。
	 *
	 * @param mpvs 要绑定的属性值（可以修改）
	 * @see #getFieldDefaultPrefix
	 */
	protected void checkFieldDefaults(MutablePropertyValues mpvs) {
		// 获取字段的默认前缀
		String fieldDefaultPrefix = getFieldDefaultPrefix();

		// 如果字段的默认前缀不为空
		if (fieldDefaultPrefix != null) {
			// 获取属性值数组
			PropertyValue[] pvArray = mpvs.getPropertyValues();

			// 遍历属性值数组
			for (PropertyValue pv : pvArray) {
				// 如果属性值的名称以默认前缀开头
				if (pv.getName().startsWith(fieldDefaultPrefix)) {
					// 获取字段名称
					String field = pv.getName().substring(fieldDefaultPrefix.length());

					// 如果属性访问器可写入属性，并且属性值集合不包含字段
					if (getPropertyAccessor().isWritableProperty(field) && !mpvs.contains(field)) {
						// 将字段添加到属性值集合
						mpvs.add(field, pv.getValue());
					}

					// 移除原始属性值。
					mpvs.removePropertyValue(pv);
				}
			}
		}
	}

	/**
	 * 检查给定的属性值是否包含字段标记，即以字段标记前缀开头的字段。
	 * <p>字段标记的存在表示该字段存在于表单中。如果属性值中不包含相应的字段值，则该字段将被视为空，并将相应地进行重置。
	 *
	 * @param mpvs 要绑定的属性值（可修改）
	 * @see #getFieldMarkerPrefix()
	 * @see #getEmptyValue(String, Class)
	 */
	protected void checkFieldMarkers(MutablePropertyValues mpvs) {
		// 获取字段标记前缀
		String fieldMarkerPrefix = getFieldMarkerPrefix();

		// 如果字段标记前缀不为空
		if (fieldMarkerPrefix != null) {
			// 获取属性值数组
			PropertyValue[] pvArray = mpvs.getPropertyValues();

			// 遍历属性值数组
			for (PropertyValue pv : pvArray) {
				// 如果属性名以字段标记前缀开头
				if (pv.getName().startsWith(fieldMarkerPrefix)) {
					// 提取字段名
					String field = pv.getName().substring(fieldMarkerPrefix.length());

					// 如果字段可写，且尚未添加到属性值集合中
					if (getPropertyAccessor().isWritableProperty(field) && !mpvs.contains(field)) {
						// 获取字段类型
						Class<?> fieldType = getPropertyAccessor().getPropertyType(field);

						// 添加字段及其对应的默认值。
						mpvs.add(field, getEmptyValue(field, fieldType));
					}

					// 移除原始属性值。
					mpvs.removePropertyValue(pv);
				}
			}
		}
	}

	/**
	 * 检查属性值是否以{@code "[]"}结尾的名称。某些客户端使用此类语法来表示数组，而不带有显式的索引值。
	 * 如果发现此类值，则删除方括号，以适应数据绑定目的的期望表示方式。
	 *
	 * @param mpvs 要绑定的属性值（可修改）
	 * @since 5.3
	 */
	protected void adaptEmptyArrayIndices(MutablePropertyValues mpvs) {
		// 遍历属性值
		for (PropertyValue pv : mpvs.getPropertyValues()) {
			String name = pv.getName();
			if (name.endsWith("[]")) {
				// 获取数组字段名。
				String field = name.substring(0, name.length() - 2);
				// 如果字段可写，且尚未添加到属性值集合中
				if (getPropertyAccessor().isWritableProperty(field) && !mpvs.contains(field)) {
					// 添加字段及其对应的值。
					mpvs.add(field, pv.getValue());
				}
				// 移除原始属性值。
				mpvs.removePropertyValue(pv);
			}
		}
	}

	/**
	 * 确定指定字段的空值。
	 * <p>如果已知字段类型，则默认实现将委托给 {@link #getEmptyValue(Class)}；否则，回退为 {@code null}。
	 *
	 * @param field     字段名称
	 * @param fieldType 字段类型
	 * @return 空值（对于大多数字段：{@code null}）
	 */
	@Nullable
	protected Object getEmptyValue(String field, @Nullable Class<?> fieldType) {
		return (fieldType != null ? getEmptyValue(fieldType) : null);
	}

	/**
	 * 确定指定字段的空值。
	 * <p>默认实现返回：
	 * <ul>
	 * <li>对于布尔字段，返回 {@code Boolean.FALSE}
	 * <li>对于数组类型，返回一个空数组
	 * <li>对于 Collection 类型，返回 Collection 实现
	 * <li>对于 Map 类型，返回 Map 实现
	 * <li>否则，默认使用 {@code null}
	 * </ul>
	 *
	 * @param fieldType 字段类型
	 * @return 空值（对于大多数字段：{@code null}）
	 * @since 5.0
	 */
	@Nullable
	public Object getEmptyValue(Class<?> fieldType) {
		try {
			if (boolean.class == fieldType || Boolean.class == fieldType) {
				// 对布尔属性的特殊处理。
				return Boolean.FALSE;
			} else if (fieldType.isArray()) {
				// 对数组属性的特殊处理。
				return Array.newInstance(fieldType.getComponentType(), 0);
			} else if (Collection.class.isAssignableFrom(fieldType)) {
				// 对集合属性的特殊处理。
				return CollectionFactory.createCollection(fieldType, 0);
			} else if (Map.class.isAssignableFrom(fieldType)) {
				// 对映射属性的特殊处理。
				return CollectionFactory.createMap(fieldType, 0);
			}
		} catch (IllegalArgumentException ex) {
			if (logger.isDebugEnabled()) {
				// 如果无法创建默认值，则记录异常信息。
				logger.debug("Failed to create default value - falling back to null: " + ex.getMessage());
			}
		}
		// 默认值：null。
		return null;
	}


	/**
	 * 如果有的话（在多部分请求的情况下），绑定给定请求中包含的所有多部分文件。由子类调用。
	 * <p>仅当多部分文件不为空或者我们配置为也绑定空的多部分文件时，才将多部分文件添加到属性值中。
	 *
	 * @param multipartFiles 字段名称字符串到 MultipartFile 对象的映射
	 * @param mpvs           要绑定的属性值（可修改）
	 * @see org.springframework.web.multipart.MultipartFile
	 * @see #setBindEmptyMultipartFiles
	 */
	protected void bindMultipart(Map<String, List<MultipartFile>> multipartFiles, MutablePropertyValues mpvs) {
		// 迭代处理多部分文件
		multipartFiles.forEach((key, values) -> {
			// 如果只有一个文件
			if (values.size() == 1) {
				// 获取第一个文件
				MultipartFile value = values.get(0);
				// 如果允许绑定空的多部分文件，或者文件不为空
				if (isBindEmptyMultipartFiles() || !value.isEmpty()) {
					// 将文件添加到mpvs中
					mpvs.add(key, value);
				}
			} else {
				// 如果有多个文件，将它们作为列表添加到mpvs中
				mpvs.add(key, values);
			}
		});
	}

}
