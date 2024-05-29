/*
 * Copyright 2002-2020 the original author or authors.
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
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Apache Commons FileUpload 的 {@link MultipartFile} 实现。
 *
 * @author Trevor D. Cook
 * @author Juergen Hoeller
 * @see CommonsMultipartResolver
 * @since 29.09.2003
 */
@SuppressWarnings("serial")
public class CommonsMultipartFile implements MultipartFile, Serializable {

	/**
	 * 日志记录器
	 */
	protected static final Log logger = LogFactory.getLog(CommonsMultipartFile.class);
	/**
	 * 文件项目
	 */
	private final FileItem fileItem;

	/**
	 * 文件大小
	 */
	private final long size;

	/**
	 * 是否保留文件名
	 */
	private boolean preserveFilename = false;


	/**
	 * 创建一个包装给定 FileItem 的实例。
	 *
	 * @param fileItem 要包装的 FileItem
	 */
	public CommonsMultipartFile(FileItem fileItem) {
		this.fileItem = fileItem;
		this.size = this.fileItem.getSize();
	}


	/**
	 * 返回底层的 {@code org.apache.commons.fileupload.FileItem} 实例。
	 * 几乎没有必要访问此实例。
	 */
	public final FileItem getFileItem() {
		return this.fileItem;
	}

	/**
	 * 设置是否保留由客户端发送的文件名，不会在 {@link CommonsMultipartFile#getOriginalFilename()} 中剥离路径信息。
	 * <p>默认为 "false"，剥离可能在实际文件名之前的路径信息，例如来自 Opera 的信息。
	 * 将此值设置为 "true" 可以保留客户端指定的文件名，包括潜在的路径分隔符。
	 *
	 * @see #getOriginalFilename()
	 * @see CommonsMultipartResolver#setPreserveFilename(boolean)
	 * @since 4.3.5
	 */
	public void setPreserveFilename(boolean preserveFilename) {
		this.preserveFilename = preserveFilename;
	}


	@Override
	public String getName() {
		return this.fileItem.getFieldName();
	}

	@Override
	public String getOriginalFilename() {
		String filename = this.fileItem.getName();
		if (filename == null) {
			// 不应该发生这种情况。
			return "";
		}
		if (this.preserveFilename) {
			// 不要尝试去掉路径...
			return filename;
		}

		// 检查Unix风格路径
		int unixSep = filename.lastIndexOf('/');
		// 检查Windows风格路径
		int winSep = filename.lastIndexOf('\\');
		// 在最新的可能点处截断
		int pos = Math.max(winSep, unixSep);
		if (pos != -1) {
			// 发现任何类型的路径分隔符...
			return filename.substring(pos + 1);
		} else {
			// 一个简单的名称
			return filename;
		}
	}

	@Override
	public String getContentType() {
		return this.fileItem.getContentType();
	}

	@Override
	public boolean isEmpty() {
		return (this.size == 0);
	}

	@Override
	public long getSize() {
		return this.size;
	}

	@Override
	public byte[] getBytes() {
		if (!isAvailable()) {
			// 如果文件已移动，则无法再次读取
			throw new IllegalStateException("File has been moved - cannot be read again");
		}
		// 获取文件字节数组
		byte[] bytes = this.fileItem.get();
		// 返回字节数组，如果为null，则返回一个空字节数组
		return (bytes != null ? bytes : new byte[0]);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (!isAvailable()) {
			// 如果文件已移动，则无法再次读取
			throw new IllegalStateException("File has been moved - cannot be read again");
		}
		// 获取文件的输入流
		InputStream inputStream = this.fileItem.getInputStream();
		// 返回输入流，如果为null，则返回一个空输入流
		return (inputStream != null ? inputStream : StreamUtils.emptyInput());
	}

	@Override
	public void transferTo(File dest) throws IOException, IllegalStateException {
		if (!isAvailable()) {
			// 如果文件已移动，则无法再次传输
			throw new IllegalStateException("File has already been moved - cannot be transferred again");
		}

		if (dest.exists() && !dest.delete()) {
			// 如果目标文件已存在且无法删除，则抛出IOException异常
			throw new IOException(
					"Destination file [" + dest.getAbsolutePath() + "] already exists and could not be deleted");
		}

		try {
			// 尝试将文件写入目标位置
			this.fileItem.write(dest);
			LogFormatUtils.traceDebug(logger, traceOn -> {
				String action = "transferred";
				// 根据文件存储位置确定操作类型
				if (!this.fileItem.isInMemory()) {
					action = (isAvailable() ? "copied" : "moved");
				}
				// 构建日志信息
				return "Part '" + getName() + "',  filename '" + getOriginalFilename() + "'" +
						(traceOn ? ", stored " + getStorageDescription() : "") +
						": " + action + " to [" + dest.getAbsolutePath() + "]";
			});
		} catch (FileUploadException ex) {
			// 抛出IllegalStateException异常并传递FileUploadException的消息
			throw new IllegalStateException(ex.getMessage(), ex);
		} catch (IllegalStateException | IOException ex) {
			// 当来自FileItem的IllegalStateException时，或者在FileItem.write中的I/O操作引发异常时，
			// 传递IllegalStateException，或者传递IOException异常
			throw ex;
		} catch (Exception ex) {
			// 抛出IOException异常并传递"File transfer failed"消息和异常对象
			throw new IOException("File transfer failed", ex);
		}
	}

	@Override
	public void transferTo(Path dest) throws IOException, IllegalStateException {
		if (!isAvailable()) {
			// 如果文件已移动，则无法再次传输
			throw new IllegalStateException("File has already been moved - cannot be transferred again");
		}

		// 将文件的输入流复制到目标文件的输出流中
		FileCopyUtils.copy(this.fileItem.getInputStream(), Files.newOutputStream(dest));
	}

	/**
	 * 确定多部分内容是否仍然可用。
	 * 如果临时文件已移动，则内容不再可用。
	 */
	protected boolean isAvailable() {
		// 如果在内存中，则可用。
		if (this.fileItem.isInMemory()) {
			return true;
		}
		// 检查临时文件的实际存在性。
		if (this.fileItem instanceof DiskFileItem) {
			return ((DiskFileItem) this.fileItem).getStoreLocation().exists();
		}
		// 检查当前文件大小是否与原始大小不同。
		return (this.fileItem.getSize() == this.size);
	}

	/**
	 * 返回多部分内容的存储位置描述。
	 * 尝试尽可能具体：在临时文件的情况下提及文件位置。
	 */
	public String getStorageDescription() {
		if (this.fileItem.isInMemory()) {
			return "in memory";
		} else if (this.fileItem instanceof DiskFileItem) {
			return "at [" + ((DiskFileItem) this.fileItem).getStoreLocation().getAbsolutePath() + "]";
		} else {
			return "on disk";
		}
	}

	@Override
	public String toString() {
		return "MultipartFile[field=\"" + this.fileItem.getFieldName() + "\"" +
				(this.fileItem.getName() != null ? ", filename=" + this.fileItem.getName() : "") +
				(this.fileItem.getContentType() != null ? ", contentType=" + this.fileItem.getContentType() : "") +
				", size=" + this.fileItem.getSize() + "]";
	}
}
