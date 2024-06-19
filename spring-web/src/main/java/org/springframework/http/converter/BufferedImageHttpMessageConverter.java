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

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * {@link HttpMessageConverter} 的实现类，可以读取和写入 {@link BufferedImage BufferedImages}。
 *
 * <p>默认情况下，此转换器可以读取由 {@linkplain ImageIO#getReaderMIMETypes() 注册的图像读取器}
 * 支持的所有媒体类型，并使用第一个可用的 {@linkplain javax.imageio.ImageIO#getWriterMIMETypes() 注册的图像写入器}
 * 的媒体类型进行写入。后者可以通过设置 {@link #setDefaultContentType defaultContentType} 属性来覆盖。
 *
 * <p>如果设置了 {@link #setCacheDir cacheDir} 属性，此转换器将缓存图像数据。
 *
 * <p>{@link #process(ImageReadParam)} 和 {@link #process(ImageWriteParam)} 模板方法
 * 允许子类覆盖图像 I/O 参数。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class BufferedImageHttpMessageConverter implements HttpMessageConverter<BufferedImage> {

	/**
	 * 可读媒体类型列表
	 */
	private final List<MediaType> readableMediaTypes = new ArrayList<>();

	/**
	 * 默认内容类型
	 */
	@Nullable
	private MediaType defaultContentType;

	/**
	 * 缓存的目录
	 */
	@Nullable
	private File cacheDir;


	public BufferedImageHttpMessageConverter() {
		// 获取所有注册的 图像IO 读取器支持的 MIME 类型
		String[] readerMediaTypes = ImageIO.getReaderMIMETypes();
		// 遍历每个 MIME 类型
		for (String mediaType : readerMediaTypes) {
			// 如果 MIME 类型不为空
			if (StringUtils.hasText(mediaType)) {
				// 将解析后的 媒体类型 添加到可读媒体类型集合中
				this.readableMediaTypes.add(MediaType.parseMediaType(mediaType));
			}
		}

		// 获取所有注册的 ImageIO 写入器支持的 MIME 类型
		String[] writerMediaTypes = ImageIO.getWriterMIMETypes();
		// 遍历每个 MIME 类型
		for (String mediaType : writerMediaTypes) {
			// 如果 MIME 类型不为空
			if (StringUtils.hasText(mediaType)) {
				// 解析 媒体类型 并设置为默认内容类型，并结束循环
				this.defaultContentType = MediaType.parseMediaType(mediaType);
				break;
			}
		}
	}


	/**
	 * 设置默认的 {@code Content-Type} 用于写入操作。
	 *
	 * @throws IllegalArgumentException 如果给定的内容类型不被 Java Image I/O API 支持
	 */
	public void setDefaultContentType(@Nullable MediaType defaultContentType) {
		// 如果提供了默认的内容类型
		if (defaultContentType != null) {
			// 获取指定 MIME 类型的 图像写入器 迭代器
			Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType(defaultContentType.toString());
			// 如果没有找到对应的 图像写入器
			if (!imageWriters.hasNext()) {
				// 抛出异常，说明 Java Image I/O API 不支持该 Content-Type
				throw new IllegalArgumentException(
						"Content-Type [" + defaultContentType + "] is not supported by the Java Image I/O API");
			}
		}

		// 设置默认的内容类型
		this.defaultContentType = defaultContentType;
	}

	/**
	 * 返回用于写入操作的默认 {@code Content-Type}。
	 * 当调用 {@link #write} 方法且未指定内容类型参数时调用此方法。
	 */
	@Nullable
	public MediaType getDefaultContentType() {
		return this.defaultContentType;
	}

	/**
	 * 设置缓存目录。如果此属性被设置为一个现有的目录，此转换器将缓存图像数据。
	 *
	 * @param cacheDir 缓存目录
	 */
	public void setCacheDir(File cacheDir) {
		Assert.notNull(cacheDir, "'cacheDir' must not be null");
		Assert.isTrue(cacheDir.isDirectory(), "'cacheDir' is not a directory");
		this.cacheDir = cacheDir;
	}


	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return (BufferedImage.class == clazz && isReadable(mediaType));
	}

	private boolean isReadable(@Nullable MediaType mediaType) {
		// 如果 媒体类型 为空
		if (mediaType == null) {
			// 返回 true
			return true;
		}
		// 获取指定 MIME 类型的 图像读取器 迭代器
		Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByMIMEType(mediaType.toString());
		// 返回是否存在适合的 图像读取器
		return imageReaders.hasNext();
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return (BufferedImage.class == clazz && isWritable(mediaType));
	}

	private boolean isWritable(@Nullable MediaType mediaType) {
		// 如果 媒体类型 为空，或者为通配符类型
		if (mediaType == null || MediaType.ALL.equalsTypeAndSubtype(mediaType)) {
			// 返回 true
			return true;
		}
		// 获取指定 MIME 类型的 图像写入器 迭代器
		Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType(mediaType.toString());
		// 返回是否存在适合的 图像写入器
		return imageWriters.hasNext();
	}

	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return Collections.unmodifiableList(this.readableMediaTypes);
	}

	@Override
	public BufferedImage read(@Nullable Class<? extends BufferedImage> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		ImageInputStream imageInputStream = null;
		ImageReader imageReader = null;
		// 由于我们在 finally 块中自定义了 close() 方法的处理，这里不能使用 try-with-resources
		try {
			// 创建 图像输入流 对象
			imageInputStream = createImageInputStream(inputMessage.getBody());
			// 获取请求头中的内容类型
			MediaType contentType = inputMessage.getHeaders().getContentType();
			// 如果内容类型为空，则抛出异常
			if (contentType == null) {
				throw new HttpMessageNotReadableException("No Content-Type header", inputMessage);
			}
			// 获取指定 MIME 类型的 图像读取器 迭代器
			Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByMIMEType(contentType.toString());
			// 如果还有 图像读取器
			if (imageReaders.hasNext()) {
				// 获取下一个 图像读取器
				imageReader = imageReaders.next();
				// 获取默认的读取参数
				ImageReadParam irp = imageReader.getDefaultReadParam();
				// 处理读取参数
				process(irp);
				// 设置 图像读取器 的输入流
				imageReader.setInput(imageInputStream, true);
				// 读取图像并返回
				return imageReader.read(0, irp);
			} else {
				// 如果没有找到合适的 图像读取器，抛出异常
				throw new HttpMessageNotReadableException(
						"Could not find javax.imageio.ImageReader for Content-Type [" + contentType + "]",
						inputMessage);
			}
		} finally {
			// 释放 图像读取器 资源
			if (imageReader != null) {
				imageReader.dispose();
			}
			// 关闭 图像输入流
			if (imageInputStream != null) {
				try {
					imageInputStream.close();
				} catch (IOException ex) {
					// 忽略异常
				}
			}
		}
	}

	private ImageInputStream createImageInputStream(InputStream is) throws IOException {
		// 将输入流转换为非关闭输入流
		is = StreamUtils.nonClosing(is);
		// 如果存在缓存的目录
		if (this.cacheDir != null) {
			// 返回一个使用文件缓存的 FileCacheImageInputStream 对象
			return new FileCacheImageInputStream(is, this.cacheDir);
		} else {
			// 否则返回一个使用内存缓存的 MemoryCacheImageInputStream 对象
			return new MemoryCacheImageInputStream(is);
		}
	}

	@Override
	public void write(final BufferedImage image, @Nullable final MediaType contentType,
					  final HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		// 获取选定的内容类型
		final MediaType selectedContentType = getContentType(contentType);
		// 设置输出消息的内容类型
		outputMessage.getHeaders().setContentType(selectedContentType);

		// 如果输出消息是 流式处理Http输出消息 的实例
		if (outputMessage instanceof StreamingHttpOutputMessage) {
			// 将输出消息转换为 流式处理Http输出消息
			StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
			// 设置流式输出消息的主体
			streamingOutputMessage.setBody(outputStream -> writeInternal(image, selectedContentType, outputStream));
		} else {
			// 否则，直接写入图像到输出消息的主体
			writeInternal(image, selectedContentType, outputMessage.getBody());
		}
	}

	private MediaType getContentType(@Nullable MediaType contentType) {
		// 如果 内容类型 为空，或是通配类型，或通配子类型
		if (contentType == null || contentType.isWildcardType() || contentType.isWildcardSubtype()) {
			// 获取默认的内容类型
			contentType = getDefaultContentType();
		}
		// 断言 内容类型 不为空，如果为空则抛出异常
		Assert.notNull(contentType, "Could not select Content-Type. " +
				"Please specify one through the 'defaultContentType' property.");
		// 返回 内容类型
		return contentType;
	}

	private void writeInternal(BufferedImage image, MediaType contentType, OutputStream body)
			throws IOException, HttpMessageNotWritableException {

		ImageOutputStream imageOutputStream = null;
		ImageWriter imageWriter = null;
		try {
			// 获取指定 MIME 类型的 图像写入器 迭代器
			Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType(contentType.toString());
			// 如果还有 图像写入器
			if (imageWriters.hasNext()) {
				// 获取下一个 图像写入器
				imageWriter = imageWriters.next();
				// 获取默认的写入参数
				ImageWriteParam iwp = imageWriter.getDefaultWriteParam();
				// 处理写入参数
				process(iwp);
				// 创建 图像输出流
				imageOutputStream = createImageOutputStream(body);
				// 设置 图像写入器 的输出流
				imageWriter.setOutput(imageOutputStream);
				// 写入图像数据
				imageWriter.write(null, new IIOImage(image, null, null), iwp);
			} else {
				// 如果没有找到合适的 图像写入器，抛出异常
				throw new HttpMessageNotWritableException(
						"Could not find javax.imageio.ImageWriter for Content-Type [" + contentType + "]");
			}
		} finally {
			// 释放 图像写入器 资源
			if (imageWriter != null) {
				imageWriter.dispose();
			}
			// 关闭 图像输出流
			if (imageOutputStream != null) {
				try {
					imageOutputStream.close();
				} catch (IOException ex) {
					// 忽略异常
				}
			}
		}
	}

	private ImageOutputStream createImageOutputStream(OutputStream os) throws IOException {
		// 如果存在缓存的目录
		if (this.cacheDir != null) {
			// 返回一个使用文件缓存的 FileCacheImageOutputStream 对象
			return new FileCacheImageOutputStream(os, this.cacheDir);
		} else {
			// 否则返回一个使用内存缓存的 MemoryCacheImageOutputStream 对象
			return new MemoryCacheImageOutputStream(os);
		}
	}


	/**
	 * 模板方法，允许在读取图像之前操作 {@link ImageReadParam}。
	 * <p>默认实现为空。
	 *
	 * @param irp 要处理的 {@link ImageReadParam} 对象
	 */
	protected void process(ImageReadParam irp) {
	}

	/**
	 * 模板方法，允许在写入图像之前操作 {@link ImageWriteParam}。
	 * <p>默认实现为空。
	 *
	 * @param iwp 要处理的 {@link ImageWriteParam} 对象
	 */
	protected void process(ImageWriteParam iwp) {
	}

}
