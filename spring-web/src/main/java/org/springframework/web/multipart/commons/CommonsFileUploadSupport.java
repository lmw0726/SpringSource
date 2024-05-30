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

package org.springframework.web.multipart.commons;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用 Apache Commons FileUpload 1.2 或更高版本的 multipart 解析器的基类。
 *
 * <p>提供 multipart 请求的常见配置属性和解析功能，使用 Spring 的 CommonsMultipartFile 实例的 Map
 * 作为上传文件的表示，并使用基于字符串的参数 Map 作为上传表单字段的表示。
 *
 * @author Juergen Hoeller
 * @see CommonsMultipartFile
 * @see CommonsMultipartResolver
 * @since 2.0
 */
public abstract class CommonsFileUploadSupport {
	/**
	 * 日志记录器
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 磁盘文件项目工厂
	 */
	private final DiskFileItemFactory fileItemFactory;

	/**
	 * 文件上传实例
	 */
	private final FileUpload fileUpload;

	/**
	 * 是否指定上传临时文件夹
	 */
	private boolean uploadTempDirSpecified = false;

	/**
	 * 是否保留原始文件名
	 */
	private boolean preserveFilename = false;


	/**
	 * 使用对应的 FileItemFactory 和 FileUpload 实例实例化一个新的 CommonsFileUploadSupport。
	 *
	 * @see #newFileItemFactory
	 * @see #newFileUpload
	 */
	public CommonsFileUploadSupport() {
		this.fileItemFactory = newFileItemFactory();
		this.fileUpload = newFileUpload(getFileItemFactory());
	}


	/**
	 * 返回底层的 {@code org.apache.commons.fileupload.disk.DiskFileItemFactory} 实例。几乎不需要访问此实例。
	 *
	 * @return 底层的 DiskFileItemFactory 实例
	 */
	public DiskFileItemFactory getFileItemFactory() {
		return this.fileItemFactory;
	}

	/**
	 * 返回底层的 {@code org.apache.commons.fileupload.FileUpload} 实例。几乎不需要访问此实例。
	 *
	 * @return 底层的 FileUpload 实例
	 */
	public FileUpload getFileUpload() {
		return this.fileUpload;
	}

	/**
	 * 设置上传被拒绝前允许的最大大小（以字节为单位）。-1 表示没有限制（默认值）。
	 *
	 * @param maxUploadSize 允许的最大上传大小
	 * @see org.apache.commons.fileupload.FileUploadBase#setSizeMax
	 */
	public void setMaxUploadSize(long maxUploadSize) {
		this.fileUpload.setSizeMax(maxUploadSize);
	}

	/**
	 * 设置每个文件在上传被拒绝前允许的最大大小（以字节为单位）。-1 表示没有限制（默认值）。
	 *
	 * @param maxUploadSizePerFile 每个文件的最大上传大小
	 * @see org.apache.commons.fileupload.FileUploadBase#setFileSizeMax
	 * @since 4.2
	 */
	public void setMaxUploadSizePerFile(long maxUploadSizePerFile) {
		this.fileUpload.setFileSizeMax(maxUploadSizePerFile);
	}

	/**
	 * 设置在上传被写入磁盘前允许的最大大小（以字节为单位）。上传的文件在超过此大小后仍然会被接收，但不会存储在内存中。
	 * 默认值为 10240，根据 Commons FileUpload 的设置。
	 *
	 * @param maxInMemorySize 允许的最大内存大小
	 * @see org.apache.commons.fileupload.disk.DiskFileItemFactory#setSizeThreshold
	 */
	public void setMaxInMemorySize(int maxInMemorySize) {
		this.fileItemFactory.setSizeThreshold(maxInMemorySize);
	}

	/**
	 * 设置用于解析请求的默认字符编码，应用于各个部分的头和表单字段。
	 * 默认值为 ISO-8859-1，根据 Servlet 规范。
	 * <p>如果请求本身指定了字符编码，请求编码将覆盖此设置。这也允许在调用
	 * {@code ServletRequest.setCharacterEncoding} 方法的过滤器中通用地覆盖字符编码。
	 *
	 * @param defaultEncoding 要使用的字符编码
	 * @see javax.servlet.ServletRequest#getCharacterEncoding
	 * @see javax.servlet.ServletRequest#setCharacterEncoding
	 * @see WebUtils#DEFAULT_CHARACTER_ENCODING
	 * @see org.apache.commons.fileupload.FileUploadBase#setHeaderEncoding
	 */
	public void setDefaultEncoding(String defaultEncoding) {
		this.fileUpload.setHeaderEncoding(defaultEncoding);
	}

	/**
	 * 确定用于解析请求的默认编码。
	 *
	 * @see #setDefaultEncoding
	 */
	protected String getDefaultEncoding() {
		// 获取请求头的编码信息
		String encoding = getFileUpload().getHeaderEncoding();
		if (encoding == null) {
			// 如果没有获取到编码信息，则使用默认的字符编码
			encoding = WebUtils.DEFAULT_CHARACTER_ENCODING;
		}
		return encoding;
	}

	/**
	 * 设置存储上传文件的临时目录。
	 * 默认是web应用的servlet容器的临时目录。
	 *
	 * @param uploadTempDir 要设置的临时目录
	 * @throws IOException 如果临时目录不存在且无法创建
	 * @see org.springframework.web.util.WebUtils#TEMP_DIR_CONTEXT_ATTRIBUTE
	 */
	public void setUploadTempDir(Resource uploadTempDir) throws IOException {
		if (!uploadTempDir.exists() && !uploadTempDir.getFile().mkdirs()) {
			// 临时目录不存在，并且临时目录无法创建，抛出异常
			throw new IllegalArgumentException("Given uploadTempDir [" + uploadTempDir + "] could not be created");
		}
		// 设置上传的文件
		this.fileItemFactory.setRepository(uploadTempDir.getFile());
		// 指定上传临时文件夹标志设置为true
		this.uploadTempDirSpecified = true;
	}

	/**
	 * 返回存储上传文件的临时目录。
	 *
	 * @return 是否指定了上传临时目录
	 * @see #setUploadTempDir
	 */
	protected boolean isUploadTempDirSpecified() {
		return this.uploadTempDirSpecified;
	}

	/**
	 * 设置是否保留客户端发送的文件名，在{@link CommonsMultipartFile#getOriginalFilename()}中不剥离路径信息。
	 * <p>默认值为"false"，剥离可能在文件名前缀的路径信息，例如来自Opera的路径信息。
	 * 将其切换为"true"可以保留客户端指定的文件名，包括可能的路径分隔符。
	 *
	 * @param preserveFilename 是否保留原始文件名
	 * @see MultipartFile#getOriginalFilename()
	 * @see CommonsMultipartFile#setPreserveFilename(boolean)
	 * @since 4.3.5
	 */
	public void setPreserveFilename(boolean preserveFilename) {
		this.preserveFilename = preserveFilename;
	}


	/**
	 * Commons DiskFileItemFactory实例的工厂方法。
	 * <p>默认实现返回一个标准的DiskFileItemFactory。
	 * 可以被覆盖以使用自定义子类，例如用于测试目的。
	 *
	 * @return 新的DiskFileItemFactory实例
	 */
	protected DiskFileItemFactory newFileItemFactory() {
		return new DiskFileItemFactory();
	}

	/**
	 * Commons FileUpload实例的工厂方法。
	 * <p><b>由子类实现。</b>
	 *
	 * @param fileItemFactory 用于构建的Commons FileItemFactory
	 * @return Commons FileUpload实例
	 */
	protected abstract FileUpload newFileUpload(FileItemFactory fileItemFactory);


	/**
	 * 为给定的编码确定合适的FileUpload实例。
	 * <p>默认实现返回共享的FileUpload实例，如果编码匹配，
	 * 否则创建一个具有相同配置但编码不同的新FileUpload实例。
	 *
	 * @param encoding 要使用的字符编码
	 * @return 合适的FileUpload实例
	 */
	protected FileUpload prepareFileUpload(@Nullable String encoding) {
		// 获取文件上传实例
		FileUpload fileUpload = getFileUpload();
		FileUpload actualFileUpload = fileUpload;

		// 如果请求指定了自己的编码且不匹配默认编码，则使用新的临时FileUpload实例
		if (encoding != null && !encoding.equals(fileUpload.getHeaderEncoding())) {
			// 创建并使用新的上传实例
			actualFileUpload = newFileUpload(getFileItemFactory());
			// 设置最大大小
			actualFileUpload.setSizeMax(fileUpload.getSizeMax());
			// 设置文件大小限制
			actualFileUpload.setFileSizeMax(fileUpload.getFileSizeMax());
			// 设置请求头编码
			actualFileUpload.setHeaderEncoding(encoding);
		}

		return actualFileUpload;
	}

	/**
	 * 将给定的Commons FileItems列表解析为Spring的MultipartParsingResult，
	 * 包含Spring的MultipartFile实例和多部分参数的Map。
	 *
	 * @param fileItems 要解析的Commons FileItems
	 * @param encoding  用于表单字段的编码
	 * @return Spring的MultipartParsingResult
	 * @see CommonsMultipartFile#CommonsMultipartFile(org.apache.commons.fileupload.FileItem)
	 */
	protected MultipartParsingResult parseFileItems(List<FileItem> fileItems, String encoding) {
		MultiValueMap<String, MultipartFile> multipartFiles = new LinkedMultiValueMap<>();
		Map<String, String[]> multipartParameters = new HashMap<>();
		Map<String, String> multipartParameterContentTypes = new HashMap<>();

		// 提取多部分文件和多部分参数。
		// 遍历文件项目列表
		for (FileItem fileItem : fileItems) {
			if (fileItem.isFormField()) {
				// 如果文件项目是一个表单域
				String value;
				// 推断部分文件的编码格式
				String partEncoding = determineEncoding(fileItem.getContentType(), encoding);
				try {
					// 尝试获取表单域的值
					value = fileItem.getString(partEncoding);
				} catch (UnsupportedEncodingException ex) {
					if (logger.isWarnEnabled()) {
						logger.warn("Could not decode multipart item '" + fileItem.getFieldName() +
								"' with encoding '" + partEncoding + "': using platform default");
					}
					// 如果获取不到，则将值设置为文件项目
					value = fileItem.getString();
				}
				// 获取字段名称对应的参数数组
				String[] curParam = multipartParameters.get(fileItem.getFieldName());
				if (curParam == null) {
					// 简单表单字段
					multipartParameters.put(fileItem.getFieldName(), new String[]{value});
				} else {
					// 简单表单字段的数组
					String[] newParam = StringUtils.addStringToArray(curParam, value);
					multipartParameters.put(fileItem.getFieldName(), newParam);
				}
				// 设置字段名称与内容类型映射
				multipartParameterContentTypes.put(fileItem.getFieldName(), fileItem.getContentType());
			} else {
				// 多部分文件字段
				CommonsMultipartFile file = createMultipartFile(fileItem);
				multipartFiles.add(file.getName(), file);
				LogFormatUtils.traceDebug(logger, traceOn ->
						"Part '" + file.getName() + "', size " + file.getSize() +
								" bytes, filename='" + file.getOriginalFilename() + "'" +
								(traceOn ? ", storage=" + file.getStorageDescription() : "")
				);
			}
		}
		return new MultipartParsingResult(multipartFiles, multipartParameters, multipartParameterContentTypes);
	}

	/**
	 * 为给定的Commons FileItem创建一个{@link CommonsMultipartFile}包装器。
	 *
	 * @param fileItem 要包装的Commons FileItem
	 * @return 对应的CommonsMultipartFile（可能是自定义子类）
	 * @see #setPreserveFilename(boolean)
	 * @see CommonsMultipartFile#setPreserveFilename(boolean)
	 * @since 4.3.5
	 */
	protected CommonsMultipartFile createMultipartFile(FileItem fileItem) {
		// 创建通用的多部分文件
		CommonsMultipartFile multipartFile = new CommonsMultipartFile(fileItem);
		// 设置是否保留原始名称
		multipartFile.setPreserveFilename(this.preserveFilename);
		return multipartFile;
	}

	/**
	 * 清理在多部分解析期间创建的Spring MultipartFiles，
	 * 这些文件可能在磁盘上保存了临时数据。
	 * <p>删除底层的Commons FileItem实例。
	 *
	 * @param multipartFiles MultipartFile实例的集合
	 * @see org.apache.commons.fileupload.FileItem#delete()
	 */
	protected void cleanupFileItems(MultiValueMap<String, MultipartFile> multipartFiles) {
		// 遍历多部分文件列表
		for (List<MultipartFile> files : multipartFiles.values()) {
			// 遍历多部分文件
			for (MultipartFile file : files) {
				if (file instanceof CommonsMultipartFile) {
					// 如果是通用多部分文件
					CommonsMultipartFile cmf = (CommonsMultipartFile) file;
					// 删除文件项
					cmf.getFileItem().delete();
					LogFormatUtils.traceDebug(logger, traceOn ->
							"Cleaning up part '" + cmf.getName() +
									"', filename '" + cmf.getOriginalFilename() + "'" +
									(traceOn ? ", stored " + cmf.getStorageDescription() : ""));
				}
			}
		}
	}

	private String determineEncoding(String contentTypeHeader, String defaultEncoding) {
		if (!StringUtils.hasText(contentTypeHeader)) {
			// 如果没有内容类型请求头，返回默认编码
			return defaultEncoding;
		}
		// 根据内容类型请求头获取内容编码对应的媒体类型。
		MediaType contentType = MediaType.parseMediaType(contentTypeHeader);
		// 获取字符集
		Charset charset = contentType.getCharset();
		// 字符集不为空，则返回其字符串形式。否则返回默认编码。
		return (charset != null ? charset.name() : defaultEncoding);
	}


	/**
	 * 持有Spring MultipartFiles的Map和多部分参数的Map的类。
	 */
	protected static class MultipartParsingResult {
		/**
		 * 文件名 —— 多部分文件映射
		 */
		private final MultiValueMap<String, MultipartFile> multipartFiles;

		/**
		 * 文件名 —— 多部分参数映射
		 */
		private final Map<String, String[]> multipartParameters;

		/**
		 * 多部分参数 —— 内容类型映射
		 */
		private final Map<String, String> multipartParameterContentTypes;

		public MultipartParsingResult(MultiValueMap<String, MultipartFile> mpFiles,
									  Map<String, String[]> mpParams, Map<String, String> mpParamContentTypes) {

			this.multipartFiles = mpFiles;
			this.multipartParameters = mpParams;
			this.multipartParameterContentTypes = mpParamContentTypes;
		}

		public MultiValueMap<String, MultipartFile> getMultipartFiles() {
			return this.multipartFiles;
		}

		public Map<String, String[]> getMultipartParameters() {
			return this.multipartParameters;
		}

		public Map<String, String> getMultipartParameterContentTypes() {
			return this.multipartParameterContentTypes;
		}
	}

}
