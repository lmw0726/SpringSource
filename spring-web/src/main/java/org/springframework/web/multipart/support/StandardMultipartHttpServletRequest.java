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

package org.springframework.web.multipart.support;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Spring MultipartHttpServletRequest 适配器，包装了一个 Servlet 3.0 HttpServletRequest
 * 及其 Part 对象。参数通过原生请求的 getParameter 方法公开 - 我们不对其进行任何自定义处理。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @see StandardServletMultipartResolver
 * @since 3.1
 */
public class StandardMultipartHttpServletRequest extends AbstractMultipartHttpServletRequest {
	/**
	 * 多部分参数名称集合
	 */
	@Nullable
	private Set<String> multipartParameterNames;


	/**
	 * 创建一个新的 StandardMultipartHttpServletRequest 包装器，用于给定的请求，立即解析多部分内容。
	 *
	 * @param request 要包装的 servlet 请求
	 * @throws MultipartException 如果解析失败
	 */
	public StandardMultipartHttpServletRequest(HttpServletRequest request) throws MultipartException {
		this(request, false);
	}

	/**
	 * 为给定的请求创建一个新的 StandardMultipartHttpServletRequest 包装器。
	 *
	 * @param request     要包装的 servlet 请求
	 * @param lazyParsing 是否应该在首次访问多部分文件或参数时延迟触发多部分解析
	 * @throws MultipartException 如果立即解析尝试失败
	 * @since 3.2.9
	 */
	public StandardMultipartHttpServletRequest(HttpServletRequest request, boolean lazyParsing)
			throws MultipartException {

		super(request);
		if (!lazyParsing) {
			// 如果不是懒加载解析，则立即解析请求
			parseRequest(request);
		}
	}


	private void parseRequest(HttpServletRequest request) {
		try {
			// 尝试获取请求中的所有Part
			Collection<Part> parts = request.getParts();
			// 创建一个LinkedHashSet来存储多部分参数名
			this.multipartParameterNames = new LinkedHashSet<>(parts.size());
			// 创建一个LinkedMultiValueMap来存储多部分文件
			MultiValueMap<String, MultipartFile> files = new LinkedMultiValueMap<>(parts.size());
			// 遍历每一个Part
			for (Part part : parts) {
				// 获取Content-Disposition头部信息
				String headerValue = part.getHeader(HttpHeaders.CONTENT_DISPOSITION);
				// 解析Content-Disposition头部信息
				ContentDisposition disposition = ContentDisposition.parse(headerValue);
				// 获取文件名
				String filename = disposition.getFilename();
				if (filename != null) {
					// 如果文件名不为空
					if (filename.startsWith("=?") && filename.endsWith("?=")) {
						// 如果文件名包含MIME编码，则解码
						filename = MimeDelegate.decode(filename);
					}
					// 创建StandardMultipartFile对象并添加到文件列表中
					files.add(part.getName(), new StandardMultipartFile(part, filename));
				} else {
					// 如果文件名为空，则将Part的名称添加到多部分参数名列表中
					this.multipartParameterNames.add(part.getName());
				}
			}
			// 设置多部分文件列表
			setMultipartFiles(files);
		} catch (Throwable ex) {
			// 处理解析失败
			handleParseFailure(ex);
		}
	}

	protected void handleParseFailure(Throwable ex) {
		String msg = ex.getMessage();
		if (msg != null && msg.contains("size") && msg.contains("exceed")) {
			// 如果异常消息包含"size"和"exceed"关键词，则抛出MaxUploadSizeExceededException异常
			throw new MaxUploadSizeExceededException(-1, ex);
		}
		// 否则，抛出MultipartException异常
		throw new MultipartException("Failed to parse multipart servlet request", ex);
	}

	@Override
	protected void initializeMultipart() {
		parseRequest(getRequest());
	}

	@Override
	public Enumeration<String> getParameterNames() {
		if (this.multipartParameterNames == null) {
			// 如果多部分参数名列表为空，则初始化多部分内容
			initializeMultipart();
		}
		if (this.multipartParameterNames.isEmpty()) {
			// 如果多部分参数名列表为空，则调用父类的getParameterNames方法
			return super.getParameterNames();
		}

		// Servlet 3.0的getParameterNames()方法不能保证包含多部分表单项
		// (例如在WebLogic 12上) -> 为了保险起见，需要在这里合并它们
		Set<String> paramNames = new LinkedHashSet<>();
		Enumeration<String> paramEnum = super.getParameterNames();
		while (paramEnum.hasMoreElements()) {
			// 将父类的参数名添加到paramNames中
			paramNames.add(paramEnum.nextElement());
		}
		// 将多部分参数名列表中的参数名也添加到paramNames中
		paramNames.addAll(this.multipartParameterNames);
		// 返回合并后的参数名集合的枚举
		return Collections.enumeration(paramNames);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		if (this.multipartParameterNames == null) {
			// 如果多部分参数名列表为空，则初始化多部分内容
			initializeMultipart();
		}
		if (this.multipartParameterNames.isEmpty()) {
			// 如果多部分参数名列表为空，则调用父类的getParameterMap方法
			return super.getParameterMap();
		}

		// Servlet 3.0的getParameterMap()方法不能保证包含多部分表单项
		// (例如在WebLogic 12上) -> 为了保险起见，需要在这里合并它们
		Map<String, String[]> paramMap = new LinkedHashMap<>(super.getParameterMap());
		// 遍历多部分参数名列表
		for (String paramName : this.multipartParameterNames) {
			// 如果参数名不包含在paramMap中，则将其添加到paramMap中
			if (!paramMap.containsKey(paramName)) {
				paramMap.put(paramName, getParameterValues(paramName));
			}
		}
		return paramMap;
	}

	@Override
	public String getMultipartContentType(String paramOrFileName) {
		try {
			// 尝试获取Part
			Part part = getPart(paramOrFileName);
			// 返回Part的内容类型，如果Part为空，则返回null
			return (part != null ? part.getContentType() : null);
		} catch (Throwable ex) {
			// 捕获并抛出MultipartException异常
			throw new MultipartException("Could not access multipart servlet request", ex);
		}
	}

	@Override
	public HttpHeaders getMultipartHeaders(String paramOrFileName) {
		try {
			// 尝试获取Part
			Part part = getPart(paramOrFileName);
			if (part != null) {
				// 如果Part不为空，则创建HttpHeaders对象并填充头部信息
				HttpHeaders headers = new HttpHeaders();
				// 遍历Part的所有头部名称
				for (String headerName : part.getHeaderNames()) {
					// 将头部名称和对应的头部值添加到HttpHeaders
					headers.put(headerName, new ArrayList<>(part.getHeaders(headerName)));
				}
				return headers;
			} else {
				// 如果Part为空，则返回null
				return null;
			}
		} catch (Throwable ex) {
			// 捕获并抛出MultipartException异常
			throw new MultipartException("Could not access multipart servlet request", ex);
		}
	}


	/**
	 * Spring 的 MultipartFile 适配器，包装了一个 Servlet 3.0 Part 对象。
	 */
	@SuppressWarnings("serial")
	private static class StandardMultipartFile implements MultipartFile, Serializable {
		/**
		 * 上传的文件部分
		 */
		private final Part part;

		/**
		 * 文件名
		 */
		private final String filename;

		public StandardMultipartFile(Part part, String filename) {
			this.part = part;
			this.filename = filename;
		}

		@Override
		public String getName() {
			return this.part.getName();
		}

		@Override
		public String getOriginalFilename() {
			return this.filename;
		}

		@Override
		public String getContentType() {
			return this.part.getContentType();
		}

		@Override
		public boolean isEmpty() {
			return (this.part.getSize() == 0);
		}

		@Override
		public long getSize() {
			return this.part.getSize();
		}

		@Override
		public byte[] getBytes() throws IOException {
			// 将Part的输入流复制到字节数组中并返回
			return FileCopyUtils.copyToByteArray(this.part.getInputStream());
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return this.part.getInputStream();
		}

		@Override
		public void transferTo(File dest) throws IOException, IllegalStateException {
			// 将Part写入到目标路径
			this.part.write(dest.getPath());
			if (dest.isAbsolute() && !dest.exists()) {
				// 如果目标路径是绝对路径且不存在
				// Servlet 3.0 Part.write 不保证支持绝对文件路径:
				// 可能会将给定的路径翻译为临时目录中的相对位置
				// （例如在Jetty中，而Tomcat和Undertow会检测绝对路径）。
				// 至少我们将文件从内存存储中卸载了；它最终会从临时目录中被删除。
				// 而对于我们用户的目的，我们可以手动将其复制到请求的位置作为备用方案。
				FileCopyUtils.copy(this.part.getInputStream(), Files.newOutputStream(dest.toPath()));
			}
		}

		@Override
		public void transferTo(Path dest) throws IOException, IllegalStateException {
			FileCopyUtils.copy(this.part.getInputStream(), Files.newOutputStream(dest));
		}
	}


	/**
	 * 内部类，避免对 JavaMail API 的硬依赖。
	 */
	private static class MimeDelegate {

		public static String decode(String value) {
			try {
				// 尝试解码文本
				return MimeUtility.decodeText(value);
			} catch (UnsupportedEncodingException ex) {
				// 捕获不支持的编码异常，并抛出IllegalStateException异常
				throw new IllegalStateException(ex);
			}
		}
	}

}
