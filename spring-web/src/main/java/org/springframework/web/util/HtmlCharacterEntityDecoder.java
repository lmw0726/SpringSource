/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.util;

/**
 * 辅助类，用于解码HTML字符串，将字符实体引用替换为引用的字符。
 *
 * @author Juergen Hoeller
 * @author Martin Kersten
 * @since 1.2.1
 */
class HtmlCharacterEntityDecoder {

	/**
	 * 引用的最大大小。
	 */
	private static final int MAX_REFERENCE_SIZE = 10;

	/**
	 * HTML字符实体引用。
	 */
	private final HtmlCharacterEntityReferences characterEntityReferences;

	/**
	 * 原始消息字符串。
	 */
	private final String originalMessage;

	/**
	 * 解码后的消息字符串。
	 */
	private final StringBuilder decodedMessage;

	/**
	 * 当前处理位置。
	 */
	private int currentPosition = 0;

	/**
	 * 下一个潜在引用位置。
	 */
	private int nextPotentialReferencePosition = -1;

	/**
	 * 下一个分号位置。
	 */
	private int nextSemicolonPosition = -2;

	/**
	 * 构造一个新的HtmlCharacterEntityDecoder实例。
	 *
	 * @param characterEntityReferences HTML字符实体引用
	 * @param original                  原始消息字符串
	 */
	public HtmlCharacterEntityDecoder(HtmlCharacterEntityReferences characterEntityReferences, String original) {
		this.characterEntityReferences = characterEntityReferences;
		this.originalMessage = original;
		this.decodedMessage = new StringBuilder(original.length());
	}

	/**
	 * 解码HTML字符串。
	 *
	 * @return 解码后的字符串
	 */
	public String decode() {
		// 在当前位置小于原始消息长度时执行循环
		while (this.currentPosition < this.originalMessage.length()) {
			// 查找下一个潜在的引用
			findNextPotentialReference(this.currentPosition);
			// 复制字符直到潜在引用
			copyCharactersTillPotentialReference();
			// 处理可能的引用
			processPossibleReference();
		}
		// 返回解码后的消息字符串
		return this.decodedMessage.toString();
	}

	/**
	 * 查找下一个潜在的引用位置。
	 *
	 * @param startPosition 起始位置
	 */
	private void findNextPotentialReference(int startPosition) {
		// 获取下一个潜在的引用位置
		this.nextPotentialReferencePosition = Math.max(startPosition, this.nextSemicolonPosition - MAX_REFERENCE_SIZE);

		do {
			// 在原始消息中查找下一个 '&'
			this.nextPotentialReferencePosition =
					this.originalMessage.indexOf('&', this.nextPotentialReferencePosition);

			// 如果找到 '&' 且 ';' 位于 '&' 之后
			if (this.nextSemicolonPosition != -1 &&
					this.nextSemicolonPosition < this.nextPotentialReferencePosition) {
				// 更新下一个 ';' 的位置
				this.nextSemicolonPosition = this.originalMessage.indexOf(';', this.nextPotentialReferencePosition + 1);
			}

			// 检查是否存在潜在的引用
			boolean isPotentialReference = (this.nextPotentialReferencePosition != -1 &&
					this.nextSemicolonPosition != -1 &&
					this.nextSemicolonPosition - this.nextPotentialReferencePosition < MAX_REFERENCE_SIZE);

			// 如果找到潜在的引用，则退出循环
			if (isPotentialReference) {
				break;
			}
			// 如果没有找到 '&'，则退出循环
			if (this.nextPotentialReferencePosition == -1) {
				break;
			}
			// 如果没有找到 ';'，则更新下一个潜在引用的位置
			if (this.nextSemicolonPosition == -1) {
				this.nextPotentialReferencePosition = -1;
				break;
			}

			// 更新下一个潜在引用的位置
			this.nextPotentialReferencePosition = this.nextPotentialReferencePosition + 1;
		}
		while (this.nextPotentialReferencePosition != -1);
	}

	/**
	 * 复制直到潜在引用位置的字符。
	 */
	private void copyCharactersTillPotentialReference() {
		// 如果下一个潜在引用位置不等于当前位置
		if (this.nextPotentialReferencePosition != this.currentPosition) {
			// 计算跳过的索引位置，直到下一个潜在引用位置，或者直到消息的末尾
			int skipUntilIndex = (this.nextPotentialReferencePosition != -1 ?
					this.nextPotentialReferencePosition : this.originalMessage.length());
			if (skipUntilIndex - this.currentPosition > 3) {
				// 如果跳过的字符数大于3，则直接复制字符串片段
				this.decodedMessage.append(this.originalMessage, this.currentPosition, skipUntilIndex);
				this.currentPosition = skipUntilIndex;
			} else {
				// 否则，逐个字符复制直到跳过的位置
				while (this.currentPosition < skipUntilIndex) {
					this.decodedMessage.append(this.originalMessage.charAt(this.currentPosition++));
				}
			}
		}
	}

	/**
	 * 处理可能的引用。
	 */
	private void processPossibleReference() {
		// 如果下一个潜在引用位置不为 -1
		if (this.nextPotentialReferencePosition != -1) {
			// 检查是否是数字引用
			boolean isNumberedReference = (this.originalMessage.charAt(this.currentPosition + 1) == '#');
			// 处理引用并返回是否可处理
			boolean wasProcessable = isNumberedReference ? processNumberedReference() : processNamedReference();
			// 如果引用可处理，则更新当前位置
			if (wasProcessable) {
				this.currentPosition = this.nextSemicolonPosition + 1;
			} else {
				// 否则将当前字符追加到解码消息中，并更新当前位置
				char currentChar = this.originalMessage.charAt(this.currentPosition);
				this.decodedMessage.append(currentChar);
				this.currentPosition++;
			}
		}
	}

	/**
	 * 处理编号的引用。
	 *
	 * @return 如果引用是可处理的则返回true，否则返回false
	 */
	private boolean processNumberedReference() {
		// 获取引用字符
		char referenceChar = this.originalMessage.charAt(this.nextPotentialReferencePosition + 2);
		// 检查是否是十六进制数字引用
		boolean isHexNumberedReference = (referenceChar == 'x' || referenceChar == 'X');
		try {
			// 解析引用值
			int value = (!isHexNumberedReference ?
					Integer.parseInt(getReferenceSubstring(2)) :
					Integer.parseInt(getReferenceSubstring(3), 16));
			// 将解析的值追加到解码消息中，并返回可处理
			this.decodedMessage.append((char) value);
			return true;
		} catch (NumberFormatException ex) {
			// 如果解析失败，则返回不可处理
			return false;
		}
	}

	/**
	 * 处理命名引用。
	 *
	 * @return 如果引用是可处理的则返回true，否则返回false
	 */
	private boolean processNamedReference() {
		// 获取引用名称
		String referenceName = getReferenceSubstring(1);
		// 将引用名称转换为字符
		char mappedCharacter = this.characterEntityReferences.convertToCharacter(referenceName);
		// 如果映射字符不为空字符，则将其追加到解码消息中，并返回可处理
		if (mappedCharacter != HtmlCharacterEntityReferences.CHAR_NULL) {
			this.decodedMessage.append(mappedCharacter);
			return true;
		}
		// 否则返回不可处理
		return false;
	}

	/**
	 * 获取引用的子字符串。
	 *
	 * @param referenceOffset 引用偏移量
	 * @return 引用的子字符串
	 */
	private String getReferenceSubstring(int referenceOffset) {
		return this.originalMessage.substring(
				this.nextPotentialReferencePosition + referenceOffset, this.nextSemicolonPosition);
	}

}
