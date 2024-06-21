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

package org.springframework.http.converter;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * {@link HttpMessageConverter} 的实现类，可以写入单个 {@link ResourceRegion} 或 {@link ResourceRegion} 集合。
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 4.3
 */
public class ResourceRegionHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

	public ResourceRegionHttpMessageConverter() {
		super(MediaType.ALL);
	}


	@Override
	@SuppressWarnings("unchecked")
	protected MediaType getDefaultContentType(Object object) {
		Resource resource = null;

		// 如果对象是ResourceRegion类型，获取其内部的资源
		if (object instanceof ResourceRegion) {
			resource = ((ResourceRegion) object).getResource();
		} else {
			// 将对象转换为ResourceRegion的集合
			Collection<ResourceRegion> regions = (Collection<ResourceRegion>) object;
			// 如果集合不为空，获取集合中第一个ResourceRegion的资源
			if (!regions.isEmpty()) {
				resource = regions.iterator().next().getResource();
			}
		}

		// 根据资源获取媒体类型，如果无法确定媒体类型，返回默认的 application_octet_stream 类型
		return MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
	}

	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return false;
	}

	@Override
	public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
		return false;
	}

	@Override
	public Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		throw new UnsupportedOperationException();
	}

	@Override
	protected ResourceRegion readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return canWrite(clazz, null, mediaType);
	}

	@Override
	public boolean canWrite(@Nullable Type type, @Nullable Class<?> clazz, @Nullable MediaType mediaType) {
		// 如果 类型 不是ParameterizedType类型
		if (!(type instanceof ParameterizedType)) {
			// 检查 类型 是否是Class类型并且是否可以赋值给ResourceRegion类
			return (type instanceof Class && ResourceRegion.class.isAssignableFrom((Class<?>) type));
		}

		// 将 类型 转换为ParameterizedType
		ParameterizedType parameterizedType = (ParameterizedType) type;

		// 如果 参数化类型 的原始类型不是Class类型，返回false
		if (!(parameterizedType.getRawType() instanceof Class)) {
			return false;
		}

		// 获取原始类型的Class对象
		Class<?> rawType = (Class<?>) parameterizedType.getRawType();

		// 如果 原始类型 不是集合类型，返回false
		if (!(Collection.class.isAssignableFrom(rawType))) {
			return false;
		}

		// 如果 参数化类型 的实际类型参数数量不是1，返回false
		if (parameterizedType.getActualTypeArguments().length != 1) {
			return false;
		}

		// 获取第一个实际类型参数
		Type typeArgument = parameterizedType.getActualTypeArguments()[0];

		// 如果 类型参数 不是Class类型，返回false
		if (!(typeArgument instanceof Class)) {
			return false;
		}

		// 将 类型参数 转换为Class类型
		Class<?> typeArgumentClass = (Class<?>) typeArgument;

		// 检查 类型参数类 是否是ResourceRegion类或者它的子类
		return ResourceRegion.class.isAssignableFrom(typeArgumentClass);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		// 如果对象是ResourceRegion类型
		if (object instanceof ResourceRegion) {
			// 写入单个ResourceRegion到输出消息
			writeResourceRegion((ResourceRegion) object, outputMessage);
		} else {
			// 将对象转换为ResourceRegion的集合
			Collection<ResourceRegion> regions = (Collection<ResourceRegion>) object;
			// 如果集合大小为1
			if (regions.size() == 1) {
				// 写入集合中的第一个ResourceRegion到输出消息
				writeResourceRegion(regions.iterator().next(), outputMessage);
			} else {
				// 写入ResourceRegion集合到输出消息
				writeResourceRegionCollection((Collection<ResourceRegion>) object, outputMessage);
			}
		}
	}


	protected void writeResourceRegion(ResourceRegion region, HttpOutputMessage outputMessage) throws IOException {
		Assert.notNull(region, "ResourceRegion must not be null");
		// 获取输出消息的HttpHeaders
		HttpHeaders responseHeaders = outputMessage.getHeaders();

		// 获取资源区域的起始位置
		long start = region.getPosition();

		// 计算资源区域的结束位置
		long end = start + region.getCount() - 1;

		// 获取资源的总长度
		long resourceLength = region.getResource().contentLength();

		// 调整结束位置，确保不超过资源的总长度
		end = Math.min(end, resourceLength - 1);

		// 计算传输范围的长度
		long rangeLength = end - start + 1;

		// 添加Content-Range头信息
		responseHeaders.add("Content-Range", "bytes " + start + '-' + end + '/' + resourceLength);

		// 设置Content-Length头信息
		responseHeaders.setContentLength(rangeLength);

		// 获取资源的输入流
		InputStream in = region.getResource().getInputStream();
		// 我们不能在这里为InputStream使用try-with-resources，因为我们有
		// 自定义处理finally块中的close() 方法。
		try {
			// 将指定范围的内容从输入流复制到输出消息的主体
			StreamUtils.copyRange(in, outputMessage.getBody(), start, end);
		} finally {
			// 最后关闭输入流
			try {
				in.close();
			} catch (IOException ex) {
				// 忽略关闭异常
			}
		}
	}

	private void writeResourceRegionCollection(Collection<ResourceRegion> resourceRegions,
											   HttpOutputMessage outputMessage) throws IOException {

		Assert.notNull(resourceRegions, "Collection of ResourceRegion should not be null");
		// 获取输出消息的HttpHeaders
		HttpHeaders responseHeaders = outputMessage.getHeaders();

		// 获取内容类型
		MediaType contentType = responseHeaders.getContentType();

		// 生成多部分边界字符串
		String boundaryString = MimeTypeUtils.generateMultipartBoundaryString();

		// 设置ContentType为multipart/byteranges，并包含边界字符串
		responseHeaders.set(HttpHeaders.CONTENT_TYPE, "multipart/byteranges; boundary=" + boundaryString);

		// 获取输出流
		OutputStream out = outputMessage.getBody();

		Resource resource = null;
		InputStream in = null;
		long inputStreamPosition = 0;

		try {
			// 遍历资源区域
			for (ResourceRegion region : resourceRegions) {
				// 计算区域起始位置与当前输入流位置的差异
				long start = region.getPosition() - inputStreamPosition;

				// 如果起始位置小于0或资源不同，重新获取输入流
				if (start < 0 || resource != region.getResource()) {
					if (in != null) {
						// 关闭当前输入流
						in.close();
					}
					// 获取新资源
					resource = region.getResource();
					// 获取新输入流
					in = resource.getInputStream();
					// 重置输入流位置
					inputStreamPosition = 0;
					// 更新起始位置
					start = region.getPosition();
				}

				// 计算区域结束位置
				long end = start + region.getCount() - 1;

				// 写入MIME头信息
				println(out);
				print(out, "--" + boundaryString);
				println(out);

				// 如果内容类型不为null，写入Content-Type头信息
				if (contentType != null) {
					print(out, "Content-Type: " + contentType);
					println(out);
				}

				// 获取资源长度
				long resourceLength = region.getResource().contentLength();

				// 调整结束位置，确保不超过资源长度
				end = Math.min(end, resourceLength - inputStreamPosition - 1);

				// 写入Content-Range头信息
				print(out, "Content-Range: bytes " +
						region.getPosition() + '-' + (region.getPosition() + region.getCount() - 1) +
						'/' + resourceLength);
				println(out);
				println(out);

				// 将指定范围的内容从输入流复制到输出流
				StreamUtils.copyRange(in, out, start, end);

				// 更新输入流位置
				inputStreamPosition += (end + 1);
			}
		} finally {
			// 最后关闭输入流
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				// 忽略关闭异常
			}
		}

		// 写入多部分结束标记
		println(out);
		print(out, "--" + boundaryString + "--");
	}

	private static void println(OutputStream os) throws IOException {
		os.write('\r');
		os.write('\n');
	}

	private static void print(OutputStream os, String buf) throws IOException {
		os.write(buf.getBytes(StandardCharsets.US_ASCII));
	}

}
