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

package org.springframework.instrument.classloading;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 基于 ClassFileTransformer 的织入器，允许将一系列转换器应用于类的字节数组。
 * 通常在类加载器内部使用。
 *
 * <p>注意：此类特意设计为尽量减少外部依赖，因为它会被包含在织入器 jar 中
 * （需要部署到应用服务器中）。
 *
 * @author Rod Johnson
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 2.0
 */
public class WeavingTransformer {


	@Nullable
	private final ClassLoader classLoader;

	private final List<ClassFileTransformer> transformers = new ArrayList<>();


	/**
	 * 为指定的类加载器创建一个新的 WeavingTransformer。
	 * @param classLoader 要为其构建转换器的 ClassLoader
	 */
	public WeavingTransformer(@Nullable ClassLoader classLoader) {
		this.classLoader = classLoader;
	}


	/**
	 * 添加一个类文件转换器，供此织入器应用。
	 * @param transformer 要注册的类文件转换器
	 */
	public void addTransformer(ClassFileTransformer transformer) {
		Assert.notNull(transformer, "Transformer must not be null");
		this.transformers.add(transformer);
	}


	/**
	 * 对给定的类字节定义应用转换。
	 * 该方法始终返回一个非空的字节数组（如果未发生转换，数组内容将与原始内容相同）。
	 * @param className 类的完全限定名，采用点分隔格式（例如 some.package.SomeClass）
	 * @param bytes 类的字节定义
	 * @return （可能经过转换的）类字节定义
	 */
	public byte[] transformIfNecessary(String className, byte[] bytes) {
		String internalName = StringUtils.replace(className, ".", "/");
		return transformIfNecessary(className, internalName, bytes, null);
	}

	/**
	 * 对给定的类字节定义应用转换。
	 * 该方法始终返回一个非空的字节数组（如果未发生转换，数组内容将与原始内容相同）。
	 * @param className 类的完全限定名，采用点分隔格式（例如 some.package.SomeClass）
	 * @param internalName 类的内部名称，采用 / 分隔格式（例如 some/package/SomeClass）
	 * @param bytes 类的字节定义
	 * @param pd 要使用的保护域（可以为 null）
	 * @return （可能经过转换的）类字节定义
	 */
	public byte[] transformIfNecessary(String className, String internalName, byte[] bytes, @Nullable ProtectionDomain pd) {
		byte[] result = bytes;
		for (ClassFileTransformer cft : this.transformers) {
			try {
				byte[] transformed = cft.transform(this.classLoader, internalName, null, pd, result);
				if (transformed != null) {
					result = transformed;
				}
			}
			catch (IllegalClassFormatException ex) {
				throw new IllegalStateException("Class file transformation failed", ex);
			}
		}
		return result;
	}

}
