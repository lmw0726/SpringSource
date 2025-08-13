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

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

/**
 * 用于操作文件系统的工具方法。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.5.3
 * @see java.io.File
 * @see java.nio.file.Path
 * @see java.nio.file.Files
 */
public abstract class FileSystemUtils {

	/**
	 * 删除指定的 {@link File} —— 对于目录，也会递归删除其中嵌套的目录或文件。
	 * <p>注意：类似于 {@link File#delete()}，此方法不会抛出异常，
	 * 而是在发生 I/O 错误时静默返回 {@code false}。如果需要使用 NIO 风格处理 I/O 错误，
	 * 并清晰区分文件不存在和删除失败，建议使用 {@link #deleteRecursively(Path)}。
	 * @param root 要删除的根 {@code File}
	 * @return 如果文件成功删除，返回 {@code true}；否则返回 {@code false}
	 */
	public static boolean deleteRecursively(@Nullable File root) {
		if (root == null) {
			return false;
		}

		try {
			return deleteRecursively(root.toPath());
		}
		catch (IOException ex) {
			return false;
		}
	}

	/**
	 * 删除指定的 {@link File} —— 对于目录，也会递归删除其中嵌套的目录或文件。
	 * @param root 要删除的根 {@code File}
	 * @return 如果文件存在且被删除，返回 {@code true}；如果文件不存在，返回 {@code false}
	 * @throws IOException 如果发生 I/O 错误
	 * @since 5.0
	 */
	public static boolean deleteRecursively(@Nullable Path root) throws IOException {
		if (root == null) {
			return false;
		}
		if (!Files.exists(root)) {
			return false;
		}

		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
		return true;
	}

	/**
	 * 递归复制 {@code src} 文件/目录的内容到 {@code dest} 文件/目录。
	 * @param src 源目录
	 * @param dest 目标目录
	 * @throws IOException 如果发生 I/O 错误
	 */
	public static void copyRecursively(File src, File dest) throws IOException {
		Assert.notNull(src, "Source File must not be null");
		Assert.notNull(dest, "Destination File must not be null");
		copyRecursively(src.toPath(), dest.toPath());
	}

	/**
	 * 递归复制 {@code src} 文件/目录的内容到 {@code dest} 文件/目录。
	 * @param src 源目录
	 * @param dest 目标目录
	 * @throws IOException 如果发生 I/O 错误
	 * @since 5.0
	 */
	public static void copyRecursively(Path src, Path dest) throws IOException {
		Assert.notNull(src, "Source Path must not be null");
		Assert.notNull(dest, "Destination Path must not be null");
		BasicFileAttributes srcAttr = Files.readAttributes(src, BasicFileAttributes.class);

		if (srcAttr.isDirectory()) {
			Files.walkFileTree(src, EnumSet.of(FOLLOW_LINKS), Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					Files.createDirectories(dest.resolve(src.relativize(dir)));
					return FileVisitResult.CONTINUE;
				}
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.copy(file, dest.resolve(src.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
					return FileVisitResult.CONTINUE;
				}
			});
		}
		else if (srcAttr.isRegularFile()) {
			Files.copy(src, dest);
		}
		else {
			throw new IllegalArgumentException("Source File must denote a directory or file");
		}
	}

}
