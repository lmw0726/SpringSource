/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.beans;

import org.springframework.beans.propertyeditors.*;
import org.springframework.core.SpringProperties;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.xml.sax.InputSource;

import java.beans.PropertyEditor;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

/**
 * {@link PropertyEditorRegistry} 接口的基本实现。
 * 提供默认编辑器和自定义编辑器的管理。
 * 主要用作 {@link BeanWrapperImpl} 的基类。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sebastien Deleuze
 * @see java.beans.PropertyEditorManager
 * @see java.beans.PropertyEditorSupport#setAsText
 * @see java.beans.PropertyEditorSupport#setValue
 * @since 1.2.6
 */
public class PropertyEditorRegistrySupport implements PropertyEditorRegistry {

	/**
	 * 布尔标志，由 {@code spring.xml.ignore} 系统属性控制，指示 Spring 忽略 XML，即不初始化与 XML 相关的基础设施。
	 * <p>默认值为 "false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");

	/**
	 * 转换服务，用于转换属性值，作为JavaBeans PropertyEditors的替代。
	 */
	@Nullable
	private ConversionService conversionService;

	/**
	 * 是否激活默认编辑器。
	 */
	private boolean defaultEditorsActive = false;

	/**
	 * 是否激活配置值编辑器。
	 */
	private boolean configValueEditorsActive = false;

	/**
	 * 默认编辑器映射。
	 */
	@Nullable
	private Map<Class<?>, PropertyEditor> defaultEditors;

	/**
	 * 覆盖的默认编辑器映射。
	 */
	@Nullable
	private Map<Class<?>, PropertyEditor> overriddenDefaultEditors;

	/**
	 * 自定义编辑器映射。
	 */
	@Nullable
	private Map<Class<?>, PropertyEditor> customEditors;

	/**
	 * 用于路径的自定义编辑器映射。
	 */
	@Nullable
	private Map<String, CustomEditorHolder> customEditorsForPath;

	/**
	 * 自定义编辑器缓存映射。
	 */
	@Nullable
	private Map<Class<?>, PropertyEditor> customEditorCache;


	/**
	 * 指定 Spring 3.0 ConversionService 用于转换属性值，作为 JavaBeans PropertyEditors 的替代方案。
	 */
	public void setConversionService(@Nullable ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * 返回关联的 ConversionService（如果有）。
	 */
	@Nullable
	public ConversionService getConversionService() {
		return this.conversionService;
	}


	//---------------------------------------------------------------------
	// Management of default editors
	//---------------------------------------------------------------------

	/**
	 * 激活此注册表实例的默认编辑器，允许在需要时延迟注册默认编辑器。
	 */
	protected void registerDefaultEditors() {
		this.defaultEditorsActive = true;
	}

	/**
	 * 激活仅用于配置目的的配置值编辑器，例如{@link org.springframework.beans.propertyeditors.StringArrayPropertyEditor}。
	 * <p>这些编辑器通常不会默认注册，因为它们通常不适用于数据绑定目的。当然，您可以通过{@link #registerCustomEditor}在任何情况下单独注册它们。
	 */
	public void useConfigValueEditors() {
		this.configValueEditorsActive = true;
	}

	/**
	 * 使用给定的属性编辑器覆盖指定类型的默认编辑器。
	 * <p>请注意，这与注册自定义编辑器不同，因为编辑器在语义上仍然是默认编辑器。
	 * ConversionService将覆盖此类默认编辑器，而自定义编辑器通常会覆盖ConversionService。
	 *
	 * @param requiredType   属性的类型
	 * @param propertyEditor 要注册的编辑器
	 * @see #registerCustomEditor(Class, PropertyEditor)
	 */
	public void overrideDefaultEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
		if (this.overriddenDefaultEditors == null) {
			this.overriddenDefaultEditors = new HashMap<>();
		}
		this.overriddenDefaultEditors.put(requiredType, propertyEditor);
	}

	/**
	 * 检索给定属性类型的默认编辑器（如果有）。
	 * <p>如果默认编辑器处于活动状态，则会延迟注册默认编辑器。
	 *
	 * @param requiredType 属性的类型
	 * @return 默认编辑器，如果找不到则为{@code null}
	 * @see #registerDefaultEditors
	 */
	@Nullable
	public PropertyEditor getDefaultEditor(Class<?> requiredType) {
		// 如果未激活默认编辑器，则返回 null
		if (!this.defaultEditorsActive) {
			return null;
		}

		// 如果存在覆盖的默认编辑器，则返回覆盖的编辑器
		if (this.overriddenDefaultEditors != null) {
			PropertyEditor editor = this.overriddenDefaultEditors.get(requiredType);
			if (editor != null) {
				return editor;
			}
		}

		// 如果默认编辑器集合为空，则创建默认编辑器集合
		if (this.defaultEditors == null) {
			createDefaultEditors();
		}

		// 返回默认编辑器集合中对应类型的编辑器
		return this.defaultEditors.get(requiredType);
	}

	/**
	 * 实际为此注册表实例注册默认编辑器。
	 */
	private void createDefaultEditors() {
		// 初始化默认编辑器集合
		this.defaultEditors = new HashMap<>(64);

		// 添加不带参数化能力的简单编辑器
		this.defaultEditors.put(Charset.class, new CharsetEditor());
		this.defaultEditors.put(Class.class, new ClassEditor());
		this.defaultEditors.put(Class[].class, new ClassArrayEditor());
		this.defaultEditors.put(Currency.class, new CurrencyEditor());
		this.defaultEditors.put(File.class, new FileEditor());
		this.defaultEditors.put(InputStream.class, new InputStreamEditor());
		if (!shouldIgnoreXml) {
			this.defaultEditors.put(InputSource.class, new InputSourceEditor());
		}
		this.defaultEditors.put(Locale.class, new LocaleEditor());
		this.defaultEditors.put(Path.class, new PathEditor());
		this.defaultEditors.put(Pattern.class, new PatternEditor());
		this.defaultEditors.put(Properties.class, new PropertiesEditor());
		this.defaultEditors.put(Reader.class, new ReaderEditor());
		this.defaultEditors.put(Resource[].class, new ResourceArrayPropertyEditor());
		this.defaultEditors.put(TimeZone.class, new TimeZoneEditor());
		this.defaultEditors.put(URI.class, new URIEditor());
		this.defaultEditors.put(URL.class, new URLEditor());
		this.defaultEditors.put(UUID.class, new UUIDEditor());
		this.defaultEditors.put(ZoneId.class, new ZoneIdEditor());

		// 添加集合编辑器的默认实例
		this.defaultEditors.put(Collection.class, new CustomCollectionEditor(Collection.class));
		this.defaultEditors.put(Set.class, new CustomCollectionEditor(Set.class));
		this.defaultEditors.put(SortedSet.class, new CustomCollectionEditor(SortedSet.class));
		this.defaultEditors.put(List.class, new CustomCollectionEditor(List.class));
		this.defaultEditors.put(SortedMap.class, new CustomMapEditor(SortedMap.class));

		// 添加原始数组的默认编辑器
		this.defaultEditors.put(byte[].class, new ByteArrayPropertyEditor());
		this.defaultEditors.put(char[].class, new CharArrayPropertyEditor());

		// JDK 不包含 char 类型的默认编辑器
		this.defaultEditors.put(char.class, new CharacterEditor(false));
		this.defaultEditors.put(Character.class, new CharacterEditor(true));

		// Spring 的 CustomBooleanEditor 接受比 JDK 默认编辑器更多的标志值
		this.defaultEditors.put(boolean.class, new CustomBooleanEditor(false));
		this.defaultEditors.put(Boolean.class, new CustomBooleanEditor(true));

		// JDK 不包含包装类型的默认编辑器
		this.defaultEditors.put(byte.class, new CustomNumberEditor(Byte.class, false));
		this.defaultEditors.put(Byte.class, new CustomNumberEditor(Byte.class, true));
		this.defaultEditors.put(short.class, new CustomNumberEditor(Short.class, false));
		this.defaultEditors.put(Short.class, new CustomNumberEditor(Short.class, true));
		this.defaultEditors.put(int.class, new CustomNumberEditor(Integer.class, false));
		this.defaultEditors.put(Integer.class, new CustomNumberEditor(Integer.class, true));
		this.defaultEditors.put(long.class, new CustomNumberEditor(Long.class, false));
		this.defaultEditors.put(Long.class, new CustomNumberEditor(Long.class, true));
		this.defaultEditors.put(float.class, new CustomNumberEditor(Float.class, false));
		this.defaultEditors.put(Float.class, new CustomNumberEditor(Float.class, true));
		this.defaultEditors.put(double.class, new CustomNumberEditor(Double.class, false));
		this.defaultEditors.put(Double.class, new CustomNumberEditor(Double.class, true));
		this.defaultEditors.put(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, true));
		this.defaultEditors.put(BigInteger.class, new CustomNumberEditor(BigInteger.class, true));

		// 仅在明确请求时注册配置值编辑器
		if (this.configValueEditorsActive) {
			// 如果激活了配置值编辑器。则添加字符串数组属性编辑器
			StringArrayPropertyEditor sae = new StringArrayPropertyEditor();
			this.defaultEditors.put(String[].class, sae);
			this.defaultEditors.put(short[].class, sae);
			this.defaultEditors.put(int[].class, sae);
			this.defaultEditors.put(long[].class, sae);
		}
	}

	/**
	 * 将此实例中注册的默认编辑器复制到给定的目标注册表。
	 *
	 * @param target 要复制到的目标注册表
	 */
	protected void copyDefaultEditorsTo(PropertyEditorRegistrySupport target) {
		target.defaultEditorsActive = this.defaultEditorsActive;
		target.configValueEditorsActive = this.configValueEditorsActive;
		target.defaultEditors = this.defaultEditors;
		target.overriddenDefaultEditors = this.overriddenDefaultEditors;
	}


	//---------------------------------------------------------------------
	// Management of custom editors
	//---------------------------------------------------------------------

	@Override
	public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
		registerCustomEditor(requiredType, null, propertyEditor);
	}

	@Override
	public void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath, PropertyEditor propertyEditor) {
		// 如果 属性类型 和 属性路径 都不存在，则抛出异常
		if (requiredType == null && propertyPath == null) {
			throw new IllegalArgumentException("Either requiredType or propertyPath is required");
		}

		// 如果存在 属性路径
		if (propertyPath != null) {
			// 如果 customEditorsForPath 为 null，则初始化为 LinkedHashMap
			if (this.customEditorsForPath == null) {
				this.customEditorsForPath = new LinkedHashMap<>(16);
			}
			// 将自定义编辑器和对应的 属性类型 放入 路径-自定义编辑器映射。
			this.customEditorsForPath.put(propertyPath, new CustomEditorHolder(propertyEditor, requiredType));
		} else {
			// 如果 customEditors 为 null，则初始化为 LinkedHashMap
			if (this.customEditors == null) {
				this.customEditors = new LinkedHashMap<>(16);
			}
			// 将自定义编辑器放入 customEditors，并清空 自定义编辑器缓存映射
			this.customEditors.put(requiredType, propertyEditor);
			this.customEditorCache = null;
		}
	}

	@Override
	@Nullable
	public PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath) {
		// 初始化要使用的 属性类型，默认为传入的 属性类型
		Class<?> requiredTypeToUse = requiredType;

		// 如果存在 属性路径
		if (propertyPath != null) {
			// 如果 customEditorsForPath 不为 null
			if (this.customEditorsForPath != null) {
				// 首先检查属性特定的编辑器
				PropertyEditor editor = getCustomEditor(propertyPath, requiredType);
				// 如果没有找到属性特定的编辑器，则尝试使用去除部分路径的方式查找
				if (editor == null) {
					// 创建一个存储去除部分路径的列表
					List<String> strippedPaths = new ArrayList<>();
					// 将去除部分路径添加到列表中
					addStrippedPropertyPaths(strippedPaths, "", propertyPath);

					// 遍历去除部分路径列表，直到找到编辑器或者列表遍历完毕为止
					for (Iterator<String> it = strippedPaths.iterator(); it.hasNext() && editor == null; ) {
						String strippedPath = it.next();
						// 获取属性特定的编辑器
						editor = getCustomEditor(strippedPath, requiredType);
					}
				}
				// 如果找到编辑器，则返回编辑器
				if (editor != null) {
					return editor;
				}
			}
			// 如果 属性类型 为 null，则尝试获取属性类型
			if (requiredType == null) {
				requiredTypeToUse = getPropertyType(propertyPath);
			}
		}
		// 如果不存在属性特定的编辑器，则检查类型特定的编辑器
		return getCustomEditor(requiredTypeToUse);
	}

	/**
	 * 确定此注册表是否包含指定数组/集合元素的自定义编辑器。
	 *
	 * @param elementType  元素的目标类型（如果未知，则可以为null）
	 * @param propertyPath 属性路径（通常是数组/集合的路径；如果未知，则可以为null）
	 * @return 是否找到匹配的自定义编辑器
	 */
	public boolean hasCustomEditorForElement(@Nullable Class<?> elementType, @Nullable String propertyPath) {
		// 如果存在 属性路径，并且 customEditorsForPath 不为 null
		if (propertyPath != null && this.customEditorsForPath != null) {
			// 遍历 customEditorsForPath 中的每个 entry
			for (Map.Entry<String, CustomEditorHolder> entry : this.customEditorsForPath.entrySet()) {
				// 如果 entry 的 key 与 属性路径 匹配，并且包含指定类型的编辑器，则返回 true
				if (PropertyAccessorUtils.matchesProperty(entry.getKey(), propertyPath) &&
						entry.getValue().getPropertyEditor(elementType) != null) {
					return true;
				}
			}
		}
		// 如果不存在属性特定的编辑器，则检查类型特定的编辑器
		return (elementType != null && this.customEditors != null && this.customEditors.containsKey(elementType));
	}

	/**
	 * 确定给定属性路径的属性类型。
	 * <p>如果未指定所需类型，则由{@link #findCustomEditor}调用，
	 * 以便能够查找特定于类型的编辑器，即使只给出属性路径。
	 * <p>默认实现始终返回{@code null}。
	 * BeanWrapperImpl使用BeanWrapper接口定义的标准{@code getPropertyType}方法重写此方法。
	 *
	 * @param propertyPath 要确定类型的属性路径
	 * @return 属性的类型，如果无法确定则为{@code null}
	 * @see BeanWrapper#getPropertyType(String)
	 */
	@Nullable
	protected Class<?> getPropertyType(String propertyPath) {
		return null;
	}

	/**
	 * 获取已为给定属性注册的自定义编辑器。
	 *
	 * @param propertyName 要查找的属性路径
	 * @param requiredType 要查找的类型
	 * @return 自定义编辑器，如果没有特定于此属性的编辑器则为{@code null}
	 */
	@Nullable
	private PropertyEditor getCustomEditor(String propertyName, @Nullable Class<?> requiredType) {
		CustomEditorHolder holder =
				(this.customEditorsForPath != null ? this.customEditorsForPath.get(propertyName) : null);
		return (holder != null ? holder.getPropertyEditor(requiredType) : null);
	}

	/**
	 * 获取给定类型的自定义编辑器。如果没有直接匹配，尝试超类的自定义编辑器
	 * （无论如何都可以通过{@code getAsText}将值呈现为字符串）。
	 *
	 * @param requiredType 要查找的类型
	 * @return 自定义编辑器，如果找不到此类型的编辑器则为{@code null}
	 * @see java.beans.PropertyEditor#getAsText()
	 */
	@Nullable
	private PropertyEditor getCustomEditor(@Nullable Class<?> requiredType) {
		// 如果 属性类型 为 null 或者 自定义编辑器映射 为 null，则返回 null
		if (requiredType == null || this.customEditors == null) {
			return null;
		}

		// 直接检查指定类型的注册编辑器
		PropertyEditor editor = this.customEditors.get(requiredType);

		// 如果找不到直接注册的编辑器
		if (editor == null) {
			// 检查缓存的编辑器，它可能是注册为超类或接口的
			if (this.customEditorCache != null) {
				editor = this.customEditorCache.get(requiredType);
			}
			// 如果缓存的编辑器仍然为空
			if (editor == null) {
				// 查找超类或接口的编辑器
				for (Map.Entry<Class<?>, PropertyEditor> entry : this.customEditors.entrySet()) {
					if (editor != null) {
						// 如果遍历过程中，其他线程获取到了自定义属性编辑器，则结束遍历
						break;
					}
					Class<?> key = entry.getKey();
					// 如果找到了与 属性类型 兼容的键
					if (key.isAssignableFrom(requiredType)) {
						editor = entry.getValue();
						// 缓存编辑器，以避免重复检查
						if (this.customEditorCache == null) {
							this.customEditorCache = new HashMap<>();
						}
						this.customEditorCache.put(requiredType, editor);
					}
				}
			}
		}

		// 返回找到的编辑器
		return editor;

	}

	/**
	 * 从注册的自定义编辑器中猜测指定属性的属性类型
	 * （前提是它们是针对特定类型进行注册的）。
	 *
	 * @param propertyName 属性的名称
	 * @return 属性类型；如果无法确定，则为{@code null}
	 */
	@Nullable
	protected Class<?> guessPropertyTypeFromEditors(String propertyName) {

		// 如果 customEditorsForPath 不为 null
		if (this.customEditorsForPath != null) {
			// 获取指定属性名对应的自定义编辑器持有者
			CustomEditorHolder editorHolder = this.customEditorsForPath.get(propertyName);
			// 如果自定义编辑器持有者为 null，则尝试获取其去除部分路径的编辑器持有者
			if (editorHolder == null) {
				// 创建一个存储去除部分路径的列表
				List<String> strippedPaths = new ArrayList<>();
				// 将去除部分路径添加到列表中
				addStrippedPropertyPaths(strippedPaths, "", propertyName);

				// 遍历去除部分路径列表，直到找到编辑器持有者或者列表遍历完毕为止
				for (Iterator<String> it = strippedPaths.iterator(); it.hasNext() && editorHolder == null; ) {
					String strippedName = it.next();
					// 获取去除部分路径对应的编辑器持有者
					editorHolder = this.customEditorsForPath.get(strippedName);
				}
			}
			// 如果找到编辑器持有者，则返回其注册类型
			if (editorHolder != null) {
				return editorHolder.getRegisteredType();
			}
		}
		// 如果没有找到自定义编辑器持有者，则返回 null
		return null;
	}

	/**
	 * 将此实例中注册的自定义编辑器复制到给定的目标注册表。
	 *
	 * @param target         目标注册表
	 * @param nestedProperty 目标注册表的嵌套属性路径（如果有）。
	 *                       如果此参数非空，则只复制注册在此嵌套属性下路径的编辑器。
	 *                       如果此参数为空，则将复制所有编辑器。
	 */
	protected void copyCustomEditorsTo(PropertyEditorRegistry target, @Nullable String nestedProperty) {
		// 获取实际属性名
		String actualPropertyName = (nestedProperty != null ? PropertyAccessorUtils.getPropertyName(nestedProperty) : null);

		// 注册自定义编辑器
		if (this.customEditors != null) {
			this.customEditors.forEach(target::registerCustomEditor);
		}

		// 遍历自定义编辑器路径及其持有者
		if (this.customEditorsForPath != null) {
			this.customEditorsForPath.forEach((editorPath, editorHolder) -> {
				// 如果存在嵌套属性
				if (nestedProperty != null) {
					// 获取路径中第一个嵌套属性的分隔符位置
					int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(editorPath);
					// 如果找到分隔符
					if (pos != -1) {
						// 获取嵌套属性名称和剩余路径
						String editorNestedProperty = editorPath.substring(0, pos);
						String editorNestedPath = editorPath.substring(pos + 1);
						// 如果嵌套属性名称与指定的嵌套属性名称或实际属性名称相匹配，则注册编辑器
						if (editorNestedProperty.equals(nestedProperty) || editorNestedProperty.equals(actualPropertyName)) {
							target.registerCustomEditor(
									editorHolder.getRegisteredType(), editorNestedPath, editorHolder.getPropertyEditor());
						}
					}
				} else {
					// 注册自定义编辑器
					target.registerCustomEditor(
							editorHolder.getRegisteredType(), editorPath, editorHolder.getPropertyEditor());
				}
			});
		}
	}


	/**
	 * 添加具有剥离键和/或索引的所有变体的属性路径。
	 * 使用嵌套路径递归调用自身。
	 *
	 * @param strippedPaths 要添加到的结果列表
	 * @param nestedPath    当前的嵌套路径
	 * @param propertyPath  要检查键/索引以剥离的属性路径
	 */
	private void addStrippedPropertyPaths(List<String> strippedPaths, String nestedPath, String propertyPath) {
		// 获取属性键前缀的起始索引
		int startIndex = propertyPath.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR);
		// 如果找到属性键前缀
		if (startIndex != -1) {
			// 获取属性键后缀的结束索引
			int endIndex = propertyPath.indexOf(PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR);
			// 如果找到属性键后缀
			if (endIndex != -1) {
				// 获取前缀、键和后缀
				String prefix = propertyPath.substring(0, startIndex);
				String key = propertyPath.substring(startIndex, endIndex + 1);
				String suffix = propertyPath.substring(endIndex + 1);
				// 去除第一个键，将剩余路径添加到列表中
				strippedPaths.add(nestedPath + prefix + suffix);
				// 递归去除剩余的键，保留第一个键
				addStrippedPropertyPaths(strippedPaths, nestedPath + prefix, suffix);
				// 递归去除剩余的键，去除第一个键
				addStrippedPropertyPaths(strippedPaths, nestedPath + prefix + key, suffix);
			}
		}
	}


	/**
	 * 带有属性名称的注册自定义编辑器的持有者。
	 * 保持PropertyEditor本身以及其注册的类型。
	 */
	private static final class CustomEditorHolder {
		/**
		 * 自定义属性编辑器
		 */
		private final PropertyEditor propertyEditor;

		/**
		 * 注册的类型
		 */
		@Nullable
		private final Class<?> registeredType;

		private CustomEditorHolder(PropertyEditor propertyEditor, @Nullable Class<?> registeredType) {
			this.propertyEditor = propertyEditor;
			this.registeredType = registeredType;
		}

		private PropertyEditor getPropertyEditor() {
			return this.propertyEditor;
		}

		@Nullable
		private Class<?> getRegisteredType() {
			return this.registeredType;
		}

		@Nullable
		private PropertyEditor getPropertyEditor(@Nullable Class<?> requiredType) {
			// 特殊情况：如果没有指定所需类型，通常只会发生在 Collection 元素中，
			// 或者所需类型不可分配给注册类型，通常只会发生在类型为 Object 的泛型属性中 -
			// 那么如果没有为 Collection 或数组类型注册编辑器，则返回 PropertyEditor。
			// （如果没有为 Collection 或数组注册，则假定其是为元素而注册的。）
			if (this.registeredType == null ||
					(requiredType != null &&
							(ClassUtils.isAssignable(this.registeredType, requiredType) ||
									ClassUtils.isAssignable(requiredType, this.registeredType))) ||
					(requiredType == null &&
							(!Collection.class.isAssignableFrom(this.registeredType) && !this.registeredType.isArray()))) {
				return this.propertyEditor;
			} else {
				return null;
			}
		}
	}

}
