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

package org.springframework.http.codec.json;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.http.HttpLogging;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import org.springframework.util.ObjectUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Consumer;

/**
 * 提供对 Jackson 2.9 编码和解码的支持方法的基类。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class Jackson2CodecSupport {

	/**
	 * 用于指定编码或解码的 "JSON View" 的提示键，其值应为 {@link Class} 类型。
	 *
	 * @see <a href="https://www.baeldung.com/jackson-json-view-annotation">Jackson JSON Views</a>
	 */
	public static final String JSON_VIEW_HINT = Jackson2CodecSupport.class.getName() + ".jsonView";

	/**
	 * 用于访问传递给 {@link org.springframework.http.codec.HttpMessageReader#read(ResolvableType, ResolvableType, ServerHttpRequest, ServerHttpResponse, Map)} 的实际 ResolvableType 的提示键
	 * （仅服务器端）。目前在方法参数具有泛型时设置，因为在响应式类型的情况下，使用 {@code ResolvableType.getGeneric()} 意味着没有 MethodParameter 源，并且不知道包含的类。
	 */
	static final String ACTUAL_TYPE_HINT = Jackson2CodecSupport.class.getName() + ".actualType";

	/**
	 * JSON视图提示错误
	 */
	private static final String JSON_VIEW_HINT_ERROR =
			"@JsonView only supported for write hints with exactly 1 class argument: ";

	/**
	 * 默认MIME类型列表
	 */
	private static final List<MimeType> DEFAULT_MIME_TYPES = Collections.unmodifiableList(
			Arrays.asList(
					MediaType.APPLICATION_JSON,
					new MediaType("application", "*+json"),
					MediaType.APPLICATION_NDJSON));

	/**
	 * 日志记录器
	 */
	protected final Log logger = HttpLogging.forLogName(getClass());

	/**
	 * 默认对象映射器
	 */
	private ObjectMapper defaultObjectMapper;

	/**
	 * 对象映射器注册表
	 */
	@Nullable
	private Map<Class<?>, Map<MimeType, ObjectMapper>> objectMapperRegistrations;

	/**
	 * MIME类型列表
	 */
	private final List<MimeType> mimeTypes;


	/**
	 * 使用指定的 Jackson {@link ObjectMapper} 构造一个实例。
	 *
	 * @param objectMapper 要使用的 ObjectMapper 实例
	 * @param mimeTypes    支持的 MIME 类型
	 */
	protected Jackson2CodecSupport(ObjectMapper objectMapper, MimeType... mimeTypes) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.defaultObjectMapper = objectMapper;
		// 如果Mime类型不为空，则返回不可更改的Mime类型列表；否则返回默认的Mime类型列表
		this.mimeTypes = !ObjectUtils.isEmpty(mimeTypes) ?
				Collections.unmodifiableList(Arrays.asList(mimeTypes)) : DEFAULT_MIME_TYPES;
	}


	/**
	 * 配置默认使用的 ObjectMapper 实例。
	 *
	 * @param objectMapper 要使用的 ObjectMapper 实例
	 * @since 5.3.4
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.defaultObjectMapper = objectMapper;
	}

	/**
	 * 返回 {@link #setObjectMapper(ObjectMapper) 配置的} 默认 ObjectMapper。
	 *
	 * @return 默认的 ObjectMapper 实例
	 */
	public ObjectMapper getObjectMapper() {
		return this.defaultObjectMapper;
	}

	/**
	 * 配置用于特定 {@link Class} 的 {@link ObjectMapper} 实例。这在需要偏离默认的
	 * {@link #getObjectMapper() ObjectMapper} 或者根据 {@code MediaType} 变化的
	 * {@code ObjectMapper} 时非常有用。
	 * <p><strong>注意:</strong> 使用此方法实际上会关闭给定类的默认 {@link #getObjectMapper() ObjectMapper}
	 * 和支持的 {@link #getMimeTypes() MimeTypes}。因此，这里配置的映射需要 {@link MediaType#includes(MediaType)}
	 * 包括所有必须支持的给定类的 MediaType。
	 *
	 * @param clazz     要注册 ObjectMapper 实例的对象类型
	 * @param registrar 用于填充或更新给定类的 MediaType-to-ObjectMapper 关联的消费者
	 * @since 5.3.4
	 */
	public void registerObjectMappersForType(Class<?> clazz, Consumer<Map<MimeType, ObjectMapper>> registrar) {
		// 如果 对象映射器注册表 为 null
		if (this.objectMapperRegistrations == null) {
			// 初始化 对象映射器注册表 为一个新的 LinkedHashMap
			this.objectMapperRegistrations = new LinkedHashMap<>();
		}

		// 从 对象映射器注册表 中获取 指定类型 的注册表，如果不存在则创建一个新的 LinkedHashMap
		Map<MimeType, ObjectMapper> registrations =
				this.objectMapperRegistrations.computeIfAbsent(clazz, c -> new LinkedHashMap<>());

		// 调用 注册消费函数 的 accept 方法，消费注册表
		registrar.accept(registrations);
	}

	/**
	 * 返回给定类的 ObjectMapper 注册表（如果有）。
	 *
	 * @param clazz 要查找注册表的类
	 * @return 注册的 MediaType-to-ObjectMapper 映射的地图，如果没有注册表，则为空
	 * @since 5.3.4
	 */
	@Nullable
	public Map<MimeType, ObjectMapper> getObjectMappersForType(Class<?> clazz) {
		// 遍历 对象映射器注册表 的条目集合
		for (Map.Entry<Class<?>, Map<MimeType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
			// 如果 条目 的键（Class 类型）是 目标类型 的父类或同类
			if (entry.getKey().isAssignableFrom(clazz)) {
				// 返回 条目 的值（对应的注册表）
				return entry.getValue();
			}
		}
		// 如果没有找到匹配的条目，返回一个空的 Map
		return Collections.emptyMap();
	}

	protected Map<Class<?>, Map<MimeType, ObjectMapper>> getObjectMapperRegistrations() {
		return (this.objectMapperRegistrations != null ? this.objectMapperRegistrations : Collections.emptyMap());
	}

	/**
	 * 子类应该将此方法公开为 "decodable" 或 "encodable" 的 MIME 类型。
	 *
	 * @return 支持的 MIME 类型列表
	 */
	protected List<MimeType> getMimeTypes() {
		return this.mimeTypes;
	}

	/**
	 * 根据给定的元素类型获取 MIME 类型。
	 * <p>如果为特定类型注册了特定的 ObjectMapper 实例，那么将返回这些 MIME 类型。
	 * <p>否则，返回默认的 MIME 类型。
	 *
	 * @param elementType 元素类型
	 * @return 支持的 MIME 类型列表
	 */
	protected List<MimeType> getMimeTypes(ResolvableType elementType) {
		// 获取 元素类型 的类对象
		Class<?> elementClass = elementType.toClass();
		List<MimeType> result = null;
		// 遍历 对象映射器注册表 的条目集合
		for (Map.Entry<Class<?>, Map<MimeType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
			// 如果 条目 的键（Class 类型）是 元素类 的父类或同类
			if (entry.getKey().isAssignableFrom(elementClass)) {
				// 如果 结果列表 为 null，则初始化 结果列表
				result = (result != null ? result : new ArrayList<>(entry.getValue().size()));
				// 将 条目 中值的所有键（MimeType）添加到 结果列表 中
				result.addAll(entry.getValue().keySet());
			}
		}
		// 如果 结果列表 为空，则返回默认的 MIME 类型集合；否则返回 结果列表
		return (CollectionUtils.isEmpty(result) ? getMimeTypes() : result);
	}

	protected boolean supportsMimeType(@Nullable MimeType mimeType) {
		// 如果 MIME类型 为空，则返回 true
		if (mimeType == null) {
			return true;
		}

		// 遍历支持的 MIME 类型集合
		for (MimeType supportedMimeType : this.mimeTypes) {
			// 如果支持的 MIME类型 兼容传入的 MIME类型 ，则返回 true
			if (supportedMimeType.isCompatibleWith(mimeType)) {
				return true;
			}
		}

		// 如果没有找到兼容的 MIME 类型，则返回 false
		return false;
	}

	/**
	 * 确定是否记录来自 {@link ObjectMapper#canDeserialize} / {@link ObjectMapper#canSerialize} 检查的异常。
	 *
	 * @param type  Jackson 测试的（反）序列化类
	 * @param cause 需要评估的 Jackson 抛出的异常（通常是 {@link JsonMappingException}）
	 * @since 5.3.1
	 */
	protected void logWarningIfNecessary(Type type, @Nullable Throwable cause) {
		// 如果 错误 为空，则返回
		if (cause == null) {
			return;
		}

		// 如果 日志记录器 处于调试模式
		if (logger.isDebugEnabled()) {
			// 构建调试信息消息
			String msg = "Failed to evaluate Jackson " + (type instanceof JavaType ? "de" : "") +
					"serialization for type [" + type + "]";
			// 记录调试信息和异常原因
			logger.debug(msg, cause);
		}
	}

	protected JavaType getJavaType(Type type, @Nullable Class<?> contextClass) {
		return this.defaultObjectMapper.constructType(GenericTypeResolver.resolveType(type, contextClass));
	}

	protected Map<String, Object> getHints(ResolvableType resolvableType) {
		// 获取方法参数
		MethodParameter param = getParameter(resolvableType);

		// 如果参数不为空
		if (param != null) {
			Map<String, Object> hints = null;

			// 如果 可解析类型 有泛型
			if (resolvableType.hasGenerics()) {
				// 初始化 提示信息，容量为 2
				hints = new HashMap<>(2);
				// 添加实际类型提示信息
				hints.put(ACTUAL_TYPE_HINT, resolvableType);
			}

			// 获取参数上的 JsonView 注解
			JsonView annotation = getAnnotation(param, JsonView.class);

			// 如果注解不为空
			if (annotation != null) {
				// 获取注解的值
				Class<?>[] classes = annotation.value();
				// 断言注解值的长度为 1，否则抛出异常
				Assert.isTrue(classes.length == 1, JSON_VIEW_HINT_ERROR + param);

				// 初始化 提示信息，如果未初始化，则容量为 1
				hints = (hints != null ? hints : new HashMap<>(1));
				// 添加 JSON 视图提示信息
				hints.put(JSON_VIEW_HINT, classes[0]);
			}

			// 如果 提示信息 不为空，则返回 提示信息
			if (hints != null) {
				return hints;
			}
		}

		// 如果没有任何提示信息，则返回空的提示信息
		return Hints.none();
	}

	@Nullable
	protected MethodParameter getParameter(ResolvableType type) {
		return (type.getSource() instanceof MethodParameter ? (MethodParameter) type.getSource() : null);
	}

	@Nullable
	protected abstract <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType);

	/**
	 * 选择一个 ObjectMapper 使用，可以是主 ObjectMapper 或者是另一个
	 * 如果通过 {@link #registerObjectMappersForType(Class, Consumer)} 为给定类的处理进行了定制。
	 *
	 * @since 5.3.4
	 */
	@Nullable
	protected ObjectMapper selectObjectMapper(ResolvableType targetType, @Nullable MimeType targetMimeType) {
		// 如果目标媒体类型为空，或者 对象映射器注册表 为空
		if (targetMimeType == null || CollectionUtils.isEmpty(this.objectMapperRegistrations)) {
			// 返回默认的对象映射器
			return this.defaultObjectMapper;
		}

		// 获取目标类型的 类
		Class<?> targetClass = targetType.toClass();

		// 遍历 对象映射器注册表 的条目
		for (Map.Entry<Class<?>, Map<MimeType, ObjectMapper>> typeEntry : getObjectMapperRegistrations().entrySet()) {
			// 如果 类型条目 的键是 目标类型 的父类或接口
			if (typeEntry.getKey().isAssignableFrom(targetClass)) {
				// 遍历 类型条目 的值，即 MimeType 和 对象映射器 的映射关系
				for (Map.Entry<MimeType, ObjectMapper> objectMapperEntry : typeEntry.getValue().entrySet()) {
					// 如果 对象映射器条目 的键包含目标媒体类型
					if (objectMapperEntry.getKey().includes(targetMimeType)) {
						// 返回匹配的 对象映射器
						return objectMapperEntry.getValue();
					}
				}
				// 没有匹配的注册项，返回 null
				return null;
			}
		}

		// 没有注册项匹配目标类型，返回默认的对象映射器
		return this.defaultObjectMapper;
	}

}
