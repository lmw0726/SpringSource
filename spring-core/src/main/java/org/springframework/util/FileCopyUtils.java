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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.io.*;
import java.nio.file.Files;

/**
 * 简单的文件和流复制工具方法。所有复制方法都使用 4096 字节的块大小，
 * 并在完成后关闭所有相关流。
 * 该类中有一组复制方法会关闭流，而 {@link StreamUtils} 中则提供了
 * 不关闭流的变体方法。
 *
 * <p>主要供框架内部使用，也适用于应用程序代码。
 *
 * @author Juergen Hoeller
 * @author Hyunjin Choi
 * @since 2003年10月6日
 * @see StreamUtils
 * @see FileSystemUtils
 */
public abstract class FileCopyUtils {

	/**
	 * 复制字节时使用的默认缓冲区大小。
	 */
	public static final int BUFFER_SIZE = StreamUtils.BUFFER_SIZE;


	//---------------------------------------------------------------------
	// 针对 java.io.File 的复制方法
	//---------------------------------------------------------------------

	/**
	 * 复制给定输入文件的内容到指定输出文件。
	 * @param in 要复制的输入文件
	 * @param out 目标输出文件
	 * @return 复制的字节数
	 * @throws IOException 发生 I/O 错误时抛出
	 */
	public static int copy(File in, File out) throws IOException {
		Assert.notNull(in, "No input File specified");
		Assert.notNull(out, "No output File specified");
		return copy(Files.newInputStream(in.toPath()), Files.newOutputStream(out.toPath()));
	}

	/**
	 * 将给定的字节数组内容复制到指定的输出文件。
	 * @param in 要复制的字节数组
	 * @param out 目标输出文件
	 * @throws IOException 发生 I/O 错误时抛出
	 */
	public static void copy(byte[] in, File out) throws IOException {
		Assert.notNull(in, "No input byte array specified");
		Assert.notNull(out, "No output File specified");
		copy(new ByteArrayInputStream(in), Files.newOutputStream(out.toPath()));
	}

	/**
	 * 将给定的输入文件内容复制到新的字节数组中。
	 * @param in 要复制的输入文件
	 * @return 复制得到的字节数组
	 * @throws IOException 发生 I/O 错误时抛出
	 */
	public static byte[] copyToByteArray(File in) throws IOException {
		Assert.notNull(in, "No input File specified");
		return copyToByteArray(Files.newInputStream(in.toPath()));
	}


	//---------------------------------------------------------------------
	// 针对 java.io.InputStream / java.io.OutputStream 的复制方法
	//---------------------------------------------------------------------

	/**
	 * 将给定的 InputStream 内容复制到指定的 OutputStream。
	 * 复制完成后关闭两个流。
	 * @param in 要复制的输入流
	 * @param out 目标输出流
	 * @return 复制的字节数
	 * @throws IOException 发生 I/O 错误时抛出
	 */
	public static int copy(InputStream in, OutputStream out) throws IOException {
		Assert.notNull(in, "No InputStream specified");
		Assert.notNull(out, "No OutputStream specified");

		try {
			return StreamUtils.copy(in, out);
		}
		finally {
			close(in);
			close(out);
		}
	}

	/**
	 * 将给定的字节数组内容复制到指定的 OutputStream。
	 * 复制完成后会关闭该流。
	 * @param in 要复制的字节数组
	 * @param out 要写入的 OutputStream
	 * @throws IOException 发生 I/O 错误时抛出
	 */
	public static void copy(byte[] in, OutputStream out) throws IOException {
		Assert.notNull(in, "No input byte array specified");
		Assert.notNull(out, "No OutputStream specified");

		try {
			out.write(in);
		}
		finally {
			close(out);
		}
	}

	/**
	 * 将给定的 InputStream 内容复制到一个新的字节数组中。
	 * 复制完成后会关闭该流。
	 * @param in 要复制的 InputStream（可以为 {@code null} 或空）
	 * @return 复制得到的新字节数组（可能为空）
	 * @throws IOException 发生 I/O 错误时抛出
	 */
	public static byte[] copyToByteArray(@Nullable InputStream in) throws IOException {
		if (in == null) {
			return new byte[0];
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_SIZE);
		copy(in, out);
		return out.toByteArray();
	}


	//---------------------------------------------------------------------
	// 针对 java.io.Reader / java.io.Writer 的复制方法
	//---------------------------------------------------------------------

	/**
	 * 将给定的 Reader 内容复制到指定的 Writer。
	 * 复制完成后会关闭 Reader 和 Writer。
	 * @param in 要复制的 Reader
	 * @param out 要写入的 Writer
	 * @return 复制的字符数量
	 * @throws IOException 发生 I/O 错误时抛出
	 */
	public static int copy(Reader in, Writer out) throws IOException {
		Assert.notNull(in, "No Reader specified");
		Assert.notNull(out, "No Writer specified");

		try {
			int charCount = 0;
			char[] buffer = new char[BUFFER_SIZE];
			int charsRead;
			while ((charsRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, charsRead);
				charCount += charsRead;
			}
			out.flush();
			return charCount;
		}
		finally {
			close(in);
			close(out);
		}
	}

	/**
	 * 将给定的字符串内容复制到给定的 Writer 中。
	 * 复制完成后会关闭该 Writer。
	 * @param in 要复制的字符串
	 * @param out 要写入的 Writer
	 * @throws IOException 发生 I/O 错误时抛出
	 */
	public static void copy(String in, Writer out) throws IOException {
		Assert.notNull(in, "No input String specified");
		Assert.notNull(out, "No Writer specified");

		try {
			out.write(in);
		}
		finally {
			close(out);
		}
	}

	/**
	 * 将给定的 Reader 内容复制成字符串。
	 * 复制完成后会关闭该 Reader。
	 * @param in 要复制的 Reader（可以为 {@code null} 或空）
	 * @return 复制得到的字符串（可能为空）
	 * @throws IOException 发生 I/O 错误时抛出
	 */
	public static String copyToString(@Nullable Reader in) throws IOException {
		if (in == null) {
			return "";
		}

		StringWriter out = new StringWriter(BUFFER_SIZE);
		copy(in, out);
		return out.toString();
	}

	/**
	 * 尝试关闭给定的 {@link Closeable}，如果发生异常则静默忽略。
	 * @param closeable 要关闭的 {@code Closeable}
	 */
	private static void close(Closeable closeable) {
		try {
			closeable.close();
		}
		catch (IOException ex) {
			// 忽略
		}
	}

}
