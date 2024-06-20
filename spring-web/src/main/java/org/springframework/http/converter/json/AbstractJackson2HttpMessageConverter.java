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

package org.springframework.http.converter.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import org.springframework.core.GenericTypeResolver;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.TypeUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 抽象基类，用于基于 Jackson 的、与内容类型无关的 {@link HttpMessageConverter} 实现。
 *
 * <p>从 Spring 5.3 开始兼容 Jackson 2.9 到 2.12。
 *
 * @author Arjen Poutsma
 * @author Keith Donald
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @see MappingJackson2HttpMessageConverter
 * @since 4.1
 */
public abstract class AbstractJackson2HttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {
	/**
	 * 编码名称——编码映射
	 */
	private static final Map<String, JsonEncoding> ENCODINGS;

	static {
		// 创建一个空的HashMap，用于存储编码名称到JsonEncoding枚举类型的映射关系
		ENCODINGS = CollectionUtils.newHashMap(JsonEncoding.values().length);

		// 遍历所有的JsonEncoding枚举值
		for (JsonEncoding encoding : JsonEncoding.values()) {
			// 将编码名称和对应的JsonEncoding枚举值放入映射中
			ENCODINGS.put(encoding.getJavaName(), encoding);
		}

		// 特殊处理US-ASCII编码，将其映射为JsonEncoding.UTF8
		ENCODINGS.put("US-ASCII", JsonEncoding.UTF8);
	}


	/**
	 * 转换器使用的默认字符集。
	 */
	@Nullable
	@Deprecated
	public static final Charset DEFAULT_CHARSET = null;

	/**
	 * 默认对象映射
	 */
	protected ObjectMapper defaultObjectMapper;

	/**
	 * 对象映射注册器
	 */
	@Nullable
	private Map<Class<?>, Map<MediaType, ObjectMapper>> objectMapperRegistrations;

	/**
	 * 是否美化打印
	 */
	@Nullable
	private Boolean prettyPrint;

	/**
	 * SSE美化打印器
	 */
	@Nullable
	private PrettyPrinter ssePrettyPrinter;


	protected AbstractJackson2HttpMessageConverter(ObjectMapper objectMapper) {
		// 设置默认的对象映射器
		this.defaultObjectMapper = objectMapper;

		// 创建默认的美化打印格式
		DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();

		// 使用两个空格缩进对象，并用"\ndata:"分隔对象
		prettyPrinter.indentObjectsWith(new DefaultIndenter("  ", "\ndata:"));

		// 设置为服务器发送事件的美化打印格式
		this.ssePrettyPrinter = prettyPrinter;
	}

	protected AbstractJackson2HttpMessageConverter(ObjectMapper objectMapper, MediaType supportedMediaType) {
		this(objectMapper);
		setSupportedMediaTypes(Collections.singletonList(supportedMediaType));
	}

	protected AbstractJackson2HttpMessageConverter(ObjectMapper objectMapper, MediaType... supportedMediaTypes) {
		this(objectMapper);
		setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
	}


	/**
	 * 配置用于对象转换的主 {@code ObjectMapper}。
	 * 如果未设置，则会创建一个默认的 {@link ObjectMapper} 实例。
	 * <p>设置自定义配置的 {@code ObjectMapper} 是控制 JSON 序列化过程的一种方法。
	 * 例如，可以配置一个扩展的 {@link com.fasterxml.jackson.databind.ser.SerializerFactory}，
	 * 提供特定类型的自定义序列化器。另一种调整序列化过程的选项是在要序列化的类型上使用 Jackson 提供的注解，
	 * 这种情况下不需要自定义配置的 ObjectMapper。
	 *
	 * @see #registerObjectMappersForType(Class, Consumer)
	 */
	public void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper must not be null");
		this.defaultObjectMapper = objectMapper;
		// 配置默认的美化打印格式
		configurePrettyPrint();
	}

	/**
	 * 返回当前使用的主 {@code ObjectMapper}。
	 */
	public ObjectMapper getObjectMapper() {
		return this.defaultObjectMapper;
	}

	/**
	 * 为给定的 {@link Class} 配置要使用的 {@link ObjectMapper} 实例。
	 * 当希望偏离 {@link #getObjectMapper() 默认} ObjectMapper 或者希望根据 {@code MediaType} 变化时，
	 * 可以使用此方法。
	 * <p><strong>注意:</strong> 使用此方法会禁用默认的 {@link #getObjectMapper() ObjectMapper}
	 * 和给定类的 {@link #setSupportedMediaTypes(List) supportedMediaTypes}。因此，
	 * 在此处配置的映射必须 {@link MediaType#includes(MediaType) 包含} 给定类必须支持的每个 MediaType。
	 *
	 * @param clazz     要为其注册 ObjectMapper 实例的对象类型
	 * @param registrar 用于填充或更新给定类的 MediaType-to-ObjectMapper 关联的 Consumer
	 * @since 5.3.4
	 */
	public void registerObjectMappersForType(Class<?> clazz, Consumer<Map<MediaType, ObjectMapper>> registrar) {
		// 如果 对象映射注册器 为空，则初始化为一个空的LinkedHashMap
		if (this.objectMapperRegistrations == null) {
			this.objectMapperRegistrations = new LinkedHashMap<>();
		}

		// 获取指定类型对应的 对应映射器，如果不存在则初始化为一个空的LinkedHashMap
		Map<MediaType, ObjectMapper> registrations =
				this.objectMapperRegistrations.computeIfAbsent(clazz, c -> new LinkedHashMap<>());

		// 将注册消费函数消费 对象映射注册器
		registrar.accept(registrations);
	}

	/**
	 * 返回给定类的 ObjectMapper 注册，如果有的话。
	 *
	 * @param clazz 要查找其注册的类
	 * @return 注册的 MediaType-to-ObjectMapper 注册的映射，
	 * 如果给定类没有注册，则返回空的映射。
	 * @since 5.3.4
	 */
	@Nullable
	public Map<MediaType, ObjectMapper> getObjectMappersForType(Class<?> clazz) {
		// 遍历 对象映射注册器 中的每个条目
		for (Map.Entry<Class<?>, Map<MediaType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
			// 如果当前条目的键可以赋值给给定类型
			if (entry.getKey().isAssignableFrom(clazz)) {
				// 返回当前条目的值
				return entry.getValue();
			}
		}
		// 如果没有找到匹配的条目，则返回一个空的不可变Map
		return Collections.emptyMap();
	}

	@Override
	public List<MediaType> getSupportedMediaTypes(Class<?> clazz) {
		List<MediaType> result = null;

		// 遍历 对象映射注册器中的每个条目
		for (Map.Entry<Class<?>, Map<MediaType, ObjectMapper>> entry : getObjectMapperRegistrations().entrySet()) {
			// 如果当前条目的键可以赋值给给定类型
			if (entry.getKey().isAssignableFrom(clazz)) {
				// 如果 结果列表 不为空，则使用当前 结果列表；
				// 否则设置为空的ArrayList
				result = (result != null ? result : new ArrayList<>(entry.getValue().size()));
				// 将当前条目的所有 媒体类型的键 添加到 结果集 中
				result.addAll(entry.getValue().keySet());
			}
		}

		// 如果 结果列表 为空，则返回支持的媒体类型集合；否则返回 结果列表
		return (CollectionUtils.isEmpty(result) ? getSupportedMediaTypes() : result);
	}

	private Map<Class<?>, Map<MediaType, ObjectMapper>> getObjectMapperRegistrations() {
		return (this.objectMapperRegistrations != null ? this.objectMapperRegistrations : Collections.emptyMap());
	}

	/**
	 * 设置在写入 JSON 时是否使用 {@link DefaultPrettyPrinter}。
	 * 这是设置 {@code ObjectMapper} 的快捷方式，如下所示：
	 * <pre class="code">
	 * ObjectMapper mapper = new ObjectMapper();
	 * mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	 * converter.setObjectMapper(mapper);
	 * </pre>
	 */
	public void setPrettyPrint(boolean prettyPrint) {
		this.prettyPrint = prettyPrint;
		// 配置默认的美化打印格式
		configurePrettyPrint();
	}

	private void configurePrettyPrint() {
		if (this.prettyPrint != null) {
			// 如要要美化打印，设置默认的美化打印格式
			this.defaultObjectMapper.configure(SerializationFeature.INDENT_OUTPUT, this.prettyPrint);
		}
	}


	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return canRead(clazz, null, mediaType);
	}

	@Override
	public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
		// 如果不能读取指定的媒体类型，则返回false
		if (!canRead(mediaType)) {
			return false;
		}

		// 获取JavaType对象
		JavaType javaType = getJavaType(type, contextClass);

		// 根据原始类和媒体类型选择ObjectMapper
		ObjectMapper objectMapper = selectObjectMapper(javaType.getRawClass(), mediaType);

		// 如果未选择到合适的ObjectMapper，则返回false
		if (objectMapper == null) {
			return false;
		}

		// 创建一个AtomicReference来存储异常信息
		AtomicReference<Throwable> causeRef = new AtomicReference<>();

		// 如果ObjectMapper可以反序列化指定的JavaType，则返回true
		if (objectMapper.canDeserialize(javaType, causeRef)) {
			return true;
		}

		// 如果不能反序列化，记录警告信息（如果有异常）
		logWarningIfNecessary(javaType, causeRef.get());

		// 最终返回false
		return false;
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		// 如果不能写入指定的媒体类型，则返回false
		if (!canWrite(mediaType)) {
			return false;
		}

		// 检查媒体类型是否不为null，并且具有字符集
		if (mediaType != null && mediaType.getCharset() != null) {
			// 获取媒体类型的字符集
			Charset charset = mediaType.getCharset();
			// 如果字符集不在已知编码集合中，则返回false
			if (!ENCODINGS.containsKey(charset.name())) {
				return false;
			}
		}

		// 根据类和媒体类型选择ObjectMapper
		ObjectMapper objectMapper = selectObjectMapper(clazz, mediaType);

		// 如果未选择到合适的ObjectMapper，则返回false
		if (objectMapper == null) {
			return false;
		}

		// 创建一个AtomicReference来存储异常信息
		AtomicReference<Throwable> causeRef = new AtomicReference<>();

		// 如果ObjectMapper可以序列化指定的类，则返回true
		if (objectMapper.canSerialize(clazz, causeRef)) {
			return true;
		}

		// 如果不能序列化，记录警告信息（如果有异常）
		logWarningIfNecessary(clazz, causeRef.get());

		// 最终返回false
		return false;
	}

	/**
	 * 选择要使用的 ObjectMapper，可以是主要的 ObjectMapper，也可以是通过
	 * {@link #registerObjectMappersForType(Class, Consumer)} 自定义处理的其他 ObjectMapper。
	 *
	 * @param targetType      目标类型
	 * @param targetMediaType 目标的 MediaType，可能为 null
	 * @return 选择的 ObjectMapper 实例，如果没有匹配的注册，则返回 null
	 */
	@Nullable
	private ObjectMapper selectObjectMapper(Class<?> targetType, @Nullable MediaType targetMediaType) {
		// 如果目标媒体类型为null，或者 对象映射注册器 为空，则返回默认的ObjectMapper
		if (targetMediaType == null || CollectionUtils.isEmpty(this.objectMapperRegistrations)) {
			return this.defaultObjectMapper;
		}

		// 遍历 对象映射注册器 中的每个条目
		for (Map.Entry<Class<?>, Map<MediaType, ObjectMapper>> typeEntry : getObjectMapperRegistrations().entrySet()) {
			// 如果当前条目的键可以赋值给目标类型
			if (typeEntry.getKey().isAssignableFrom(targetType)) {
				// 遍历当前条目中的每个ObjectMapper条目
				for (Map.Entry<MediaType, ObjectMapper> objectMapperEntry : typeEntry.getValue().entrySet()) {
					// 如果当前ObjectMapper条目的媒体类型包含目标媒体类型
					if (objectMapperEntry.getKey().includes(targetMediaType)) {
						// 返回当前ObjectMapper
						return objectMapperEntry.getValue();
					}
				}
				// 如果没有匹配的注册信息，则返回null
				return null;
			}
		}

		// 如果没有找到匹配的注册信息，则返回默认的ObjectMapper
		return this.defaultObjectMapper;
	}

	/**
	 * 根据 {@link ObjectMapper#canDeserialize} / {@link ObjectMapper#canSerialize} 的检查结果，
	 * 决定是否记录来自 Jackson 的异常日志。
	 *
	 * @param type  Jackson 进行序列化或反序列化检查的类型
	 * @param cause Jackson 抛出的异常，通常是 {@link JsonMappingException}
	 * @since 4.3
	 */
	protected void logWarningIfNecessary(Type type, @Nullable Throwable cause) {
		// 如果异常cause为null，则直接返回，不进行任何处理
		if (cause == null) {
			return;
		}

		// 判断是否为调试级别日志（对于JsonMappingException并且消息以"Cannot find"开头的异常）
		boolean debugLevel = (cause instanceof JsonMappingException && cause.getMessage().startsWith("Cannot find"));

		// 根据日志级别输出相应的警告信息
		if (debugLevel ? logger.isDebugEnabled() : logger.isWarnEnabled()) {
			// 构造日志消息
			String msg = "Failed to evaluate Jackson " + (type instanceof JavaType ? "de" : "") +
					"serialization for type [" + type + "]";
			// 如果是调试级别，则使用debug输出日志
			if (debugLevel) {
				logger.debug(msg, cause);
			} else if (logger.isDebugEnabled()) {
				// 如果是普通调试级别，则使用warn输出日志
				logger.warn(msg, cause);
			} else {
				// 否则，使用warn输出简化的日志消息
				logger.warn(msg + ": " + cause);
			}
		}
	}

	@Override
	public Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		// 获取Java类型
		JavaType javaType = getJavaType(type, contextClass);
		// 根据输入消息和Java类型反序列化
		return readJavaType(javaType, inputMessage);
	}

	@Override
	protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {
		// 获取Java类型
		JavaType javaType = getJavaType(clazz, null);
		// 反序列化
		return readJavaType(javaType, inputMessage);
	}

	private Object readJavaType(JavaType javaType, HttpInputMessage inputMessage) throws IOException {
		// 获取输入消息的内容类型
		MediaType contentType = inputMessage.getHeaders().getContentType();

		// 获取字符集
		Charset charset = getCharset(contentType);

		// 根据Java类型和 内容类型 选择ObjectMapper
		ObjectMapper objectMapper = selectObjectMapper(javaType.getRawClass(), contentType);

		// 断言确保找到了合适的ObjectMapper
		Assert.state(objectMapper != null, "No ObjectMapper for " + javaType);

		// 判断字符集是否为Unicode编码
		boolean isUnicode = ENCODINGS.containsKey(charset.name()) ||
				"UTF-16".equals(charset.name()) ||
				"UTF-32".equals(charset.name());

		try {
			// 获取输入流，确保不关闭
			InputStream inputStream = StreamUtils.nonClosing(inputMessage.getBody());

			// 如果输入消息是MappingJacksonInputMessage类型
			if (inputMessage instanceof MappingJacksonInputMessage) {
				// 获取反序列化视图
				Class<?> deserializationView = ((MappingJacksonInputMessage) inputMessage).getDeserializationView();
				if (deserializationView != null) {
					// 根据视图和Java类型创建ObjectReader
					ObjectReader objectReader = objectMapper.readerWithView(deserializationView).forType(javaType);
					if (isUnicode) {
						// 如果是Unicode编码，则通过输入流读取
						return objectReader.readValue(inputStream);
					} else {
						// 否则，将输入流和字符集包装成读取器
						Reader reader = new InputStreamReader(inputStream, charset);
						// 通过读取器读取数据
						return objectReader.readValue(reader);
					}
				}
			}

			if (isUnicode) {
				// 如果是Unicode编码，则通过 输入流 和 Java类型 读取
				return objectMapper.readValue(inputStream, javaType);
			} else {
				// 否则，将输入流和字符集包装成读取器
				Reader reader = new InputStreamReader(inputStream, charset);
				// 通过 读取器 和 Java类型 读取数据
				return objectMapper.readValue(reader, javaType);
			}
		} catch (InvalidDefinitionException ex) {
			// 处理类型定义错误异常
			throw new HttpMessageConversionException("Type definition error: " + ex.getType(), ex);
		} catch (JsonProcessingException ex) {
			// 处理JSON解析错误异常
			throw new HttpMessageNotReadableException("JSON parse error: " + ex.getOriginalMessage(), ex, inputMessage);
		}
	}

	/**
	 * 确定用于 JSON 输入的字符集。
	 * <p>默认情况下，这是从输入的 {@code MediaType} 中获取的字符集，如果没有则回退到 {@code UTF-8}。
	 * 可以在子类中进行重写。
	 *
	 * @param contentType HTTP 输入消息的内容类型
	 * @return 要使用的字符集
	 * @since 5.1.18
	 */
	protected Charset getCharset(@Nullable MediaType contentType) {
		// 如果 内容类型 不为null，并且具有字符集
		if (contentType != null && contentType.getCharset() != null) {
			// 返回 内容类型 中定义的字符集
			return contentType.getCharset();
		} else {
			// 否则返回默认的UTF-8字符集
			return StandardCharsets.UTF_8;
		}
	}

	@Override
	protected void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		// 获取输出消息的 内容类型
		MediaType contentType = outputMessage.getHeaders().getContentType();

		// 获取JSON编码方式
		JsonEncoding encoding = getJsonEncoding(contentType);

		// 确定对象的实际类型
		Class<?> clazz = (object instanceof MappingJacksonValue ?
				((MappingJacksonValue) object).getValue().getClass() : object.getClass());

		// 根据类和 内容类型 选择ObjectMapper
		ObjectMapper objectMapper = selectObjectMapper(clazz, contentType);

		// 断言确保找到合适的ObjectMapper
		Assert.state(objectMapper != null, "No ObjectMapper for " + clazz.getName());

		// 获取输出流，确保不关闭
		OutputStream outputStream = StreamUtils.nonClosing(outputMessage.getBody());

		try (JsonGenerator generator = objectMapper.getFactory().createGenerator(outputStream, encoding)) {
			// 写入前缀
			writePrefix(generator, object);

			// 初始化要序列化的值和相关属性
			Object value = object;
			Class<?> serializationView = null;
			FilterProvider filters = null;
			JavaType javaType = null;

			// 如果对象是MappingJacksonValue类型，获取其值、序列化视图和过滤器
			if (object instanceof MappingJacksonValue) {
				MappingJacksonValue container = (MappingJacksonValue) object;
				value = container.getValue();
				serializationView = container.getSerializationView();
				filters = container.getFilters();
			}

			// 如果类型不为null，且可以赋值给指定值的类，获取Java类型
			if (type != null && TypeUtils.isAssignable(type, value.getClass())) {
				javaType = getJavaType(type, null);
			}

			// 根据是否有序列化视图选择ObjectWriter
			ObjectWriter objectWriter = (serializationView != null ?
					objectMapper.writerWithView(serializationView) : objectMapper.writer());

			// 如果有过滤器，应用过滤器
			if (filters != null) {
				objectWriter = objectWriter.with(filters);
			}

			// 如果Java类型不为null，且是容器类型，应用Java类型
			if (javaType != null && javaType.isContainerType()) {
				objectWriter = objectWriter.forType(javaType);
			}

			// 获取序列化配置
			SerializationConfig config = objectWriter.getConfig();

			// 如果内容类型兼容 文本事件流，且启用了缩进输出，应用 SSE美化打印器
			if (contentType != null && contentType.isCompatibleWith(MediaType.TEXT_EVENT_STREAM) &&
					config.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
				objectWriter = objectWriter.with(this.ssePrettyPrinter);
			}

			// 写入值
			objectWriter.writeValue(generator, value);

			// 写入后缀
			writeSuffix(generator, object);

			// 刷新生成器
			generator.flush();
		} catch (InvalidDefinitionException ex) {
			// 处理类型定义错误异常
			throw new HttpMessageConversionException("Type definition error: " + ex.getType(), ex);
		} catch (JsonProcessingException ex) {
			// 处理JSON处理异常
			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getOriginalMessage(), ex);
		}
	}

	/**
	 * 在主内容之前写入前缀。
	 *
	 * @param generator 用于写入内容的生成器。
	 * @param object    要写入输出消息的对象。
	 */
	protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
	}

	/**
	 * 在主内容之后写入后缀。
	 *
	 * @param generator 用于写入内容的生成器。
	 * @param object    要写入输出消息的对象。
	 */
	protected void writeSuffix(JsonGenerator generator, Object object) throws IOException {
	}

	/**
	 * 返回指定类型和上下文类的 Jackson {@link JavaType}。
	 *
	 * @param type         要返回 Jackson JavaType 的泛型类型
	 * @param contextClass 目标类型的上下文类，例如目标类型出现在方法签名中的类（可以为 {@code null}）
	 * @return Jackson JavaType
	 */
	protected JavaType getJavaType(Type type, @Nullable Class<?> contextClass) {
		return this.defaultObjectMapper.constructType(GenericTypeResolver.resolveType(type, contextClass));
	}

	/**
	 * 确定要用于给定内容类型的 JSON 编码。
	 *
	 * @param contentType 调用方请求的媒体类型
	 * @return 要使用的 JSON 编码（永远不为 {@code null}）
	 */
	protected JsonEncoding getJsonEncoding(@Nullable MediaType contentType) {
		// 检查 内容类型 是否不为null，并且具有字符集
		if (contentType != null && contentType.getCharset() != null) {
			// 获取字符集
			Charset charset = contentType.getCharset();
			// 根据字符集名称获取对应的JsonEncoding编码方式
			JsonEncoding encoding = ENCODINGS.get(charset.name());
			// 如果找到对应的编码方式，则返回它
			if (encoding != null) {
				return encoding;
			}
		}

		// 否则返回默认的UTF-8编码方式
		return JsonEncoding.UTF8;
	}

	@Override
	@Nullable
	protected MediaType getDefaultContentType(Object object) throws IOException {
		// 如果对象是MappingJacksonValue类型，获取其内部的实际值
		if (object instanceof MappingJacksonValue) {
			object = ((MappingJacksonValue) object).getValue();
		}
		// 调用父类的getDefaultContentType方法，传入实际值作为参数，并返回其结果
		return super.getDefaultContentType(object);
	}

	@Override
	protected Long getContentLength(Object object, @Nullable MediaType contentType) throws IOException {
		// 如果对象是MappingJacksonValue类型，获取其内部的实际值
		if (object instanceof MappingJacksonValue) {
			object = ((MappingJacksonValue) object).getValue();
		}
		// 调用父类的getContentLength方法，传入实际值和ContentType作为参数，并返回其结果
		return super.getContentLength(object, contentType);
	}

}
