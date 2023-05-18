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

package org.springframework.util.xml;

import java.io.BufferedReader;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Detects whether an XML stream is using DTD- or XSD-based validation.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.0
 */
public class XmlValidationModeDetector {

	/**
	 * Indicates that the validation should be disabled.
	 */
	public static final int VALIDATION_NONE = 0;

	/**
	 * Indicates that the validation mode should be auto-guessed, since we cannot find
	 * a clear indication (probably choked on some special characters, or the like).
	 */
	public static final int VALIDATION_AUTO = 1;

	/**
	 * Indicates that DTD validation should be used (we found a "DOCTYPE" declaration).
	 */
	public static final int VALIDATION_DTD = 2;

	/**
	 * Indicates that XSD validation should be used (found no "DOCTYPE" declaration).
	 */
	public static final int VALIDATION_XSD = 3;


	/**
	 * XML文档中的令牌，该令牌声明要用于验证的DTD，因此正在使用DTD验证。
	 */
	private static final String DOCTYPE = "DOCTYPE";

	/**
	 * XML注释开始字符串
	 */
	private static final String START_COMMENT = "<!--";

	/**
	 * XML注释结束字符串
	 */
	private static final String END_COMMENT = "-->";


	/**
	 * 指示当前解析位置是否在XML注释内。
	 */
	private boolean inComment;


	/**
	 * 检测提供的 {@link InputStream} 中XML文档的验证模式。
	 * <p> 请注意，提供的 {@link InputStream} 在返回之前已通过此方法关闭。
	 *
	 * @param inputStream 要解析的InputStream
	 * @throws IOException 在IO失败的情况下
	 * @see #VALIDATION_DTD
	 * @see #VALIDATION_XSD
	 */
	public int detectValidationMode(InputStream inputStream) throws IOException {
		//当前解析位置不在注释内

		this.inComment = false;

		//在文件中查找DOCTYPE。
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			boolean isDtdValidated = false;
			String content;
			while ((content = reader.readLine()) != null) {
				//消费注释符号
				content = consumeCommentTokens(content);
				if (!StringUtils.hasText(content)) {
					//如果是注释，则忽略
					continue;
				}
				if (hasDoctype(content)) {
					//如果内容中含有DOCTYPE声明，则isDtdValidated 返回true，并终结循环。
					isDtdValidated = true;
					break;
				}
				// hasOpeningTag 方法会校验，如果这一行有 < ，并且 < 后面跟着的是字母，则返回 true 。
				if (hasOpeningTag(content)) {
					break;
				}
			}
			return (isDtdValidated ? VALIDATION_DTD : VALIDATION_XSD);
		} catch (CharConversionException ex) {
			//返回 VALIDATION_AUTO 模式
			return VALIDATION_AUTO;
		}
	}


	/**
	 * 内容是否包含DTD DOCTYPE声明？
	 */
	private boolean hasDoctype(String content) {
		return content.contains(DOCTYPE);
	}

	/**
	 * 确定提供的内容是否包含XML打开标记。
	 * <p> 在将其余部分传递给此方法之前，预计已为所提供的内容消耗了所有注释令牌。（已经处理了注释字符串）
	 * 但是，作为健全性检查，如果解析状态当前处于XML注释中，则此方法始终返回 {@code false}。
	 */
	private boolean hasOpeningTag(String content) {
		if (this.inComment) {
			return false;
		}
		int openTagIndex = content.indexOf('<');
		return (openTagIndex > -1 &&
				//判断<后不仅有一个字符
				(content.length() > openTagIndex + 1) &&
				//判断<后一位的字符是否为字母
				Character.isLetter(content.charAt(openTagIndex + 1)));
	}

	/**
	 * 消耗给定字符串中的所有注释并返回剩余内容，该内容可能为空，因为提供的内容可能是所有注释数据。<p> 此方法考虑了当前的 “注释中” 解析状态。
	 *
	 * @param line 字符串
	 * @return 非注释内容
	 */
	private String consumeCommentTokens(String line) {
		int indexOfStartComment = line.indexOf(START_COMMENT);
		if (indexOfStartComment == -1 && !line.contains(END_COMMENT)) {
			//如果没有<!--且没有-->，返回当前的字符串
			return line;
		}

		String result = "";
		String currLine = line;
		if (!this.inComment && (indexOfStartComment >= 0)) {
			//当前解析位置不在注释内，且有<!--字符串
			//截取<!--之前的字符
			result = line.substring(0, indexOfStartComment);
			//截取<!--之后的字符
			currLine = line.substring(indexOfStartComment);
		}

		if ((currLine = consume(currLine)) != null) {
			result += consumeCommentTokens(currLine);
		}
		return result;
	}

	/**
	 * 消费下一个注释令牌，更新 “inComment” 标志，并返回剩余内容。
	 */
	@Nullable
	private String consume(String line) {
		//如果当前解析位置不在注释内，则返回null，否则返回index后的字符串
		int index = (this.inComment ? endComment(line) : startComment(line));
		return (index == -1 ? null : line.substring(index));
	}

	/**
	 * 尝试使用 {@link #START_COMMENT} 令牌。
	 *
	 * @see #commentToken(String, String, boolean)
	 */
	private int startComment(String line) {
		return commentToken(line, START_COMMENT, true);
	}

	/**
	 * 尝试使用 {@link #END_COMMENT} 令牌。
	 *
	 * @see #commentToken(String, String, boolean)
	 */
	private int endComment(String line) {
		return commentToken(line, END_COMMENT, false);
	}

	/**
	 * 尝试根据所提供的内容使用所提供的令牌，并将“注释内”解析状态更新为所提供的值。
	 * <p> 将索引返回到令牌之后的内容中，如果找不到令牌，则返回-1。
	 */
	private int commentToken(String line, String token, boolean inCommentIfPresent) {
		int index = line.indexOf(token);
		if (index > -1) {
			//如果有令牌，，则将注释内的标记设置为inCommentIfPresent参数值
			this.inComment = inCommentIfPresent;
		}
		//如果没有找到则返回-1，找到了则返回index位置+token长度
		return (index == -1 ? index : index + token.length());
	}

}
