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

package org.springframework.util.xml;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.w3c.dom.CharacterData;
import org.w3c.dom.*;
import org.xml.sax.ContentHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 提供操作DOM API的便捷方法，
 * 特别是用于操作DOM节点和DOM元素。
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Costin Leau
 * @author Arjen Poutsma
 * @author Luke Taylor
 * @see org.w3c.dom.Node
 * @see org.w3c.dom.Element
 * @since 1.2
 */
public abstract class DomUtils {

	/**
	 * 检索与给定元素名称匹配的给定DOM元素的所有子元素。
	 * 只看给定元素的直接子级别; 不要深入 (与DOM API的 {@code getElementsByTagName}  方法相反)。
	 *
	 * @param ele           要分析的DOM元素
	 * @param childEleNames 要查找的子元素名称
	 * @return 一个{@code org.w3c.dom.Element}实例的子节点列表
	 * @see org.w3c.dom.Element
	 * @see org.w3c.dom.Element#getElementsByTagName
	 */
	public static List<Element> getChildElementsByTagName(Element ele, String... childEleNames) {
		Assert.notNull(ele, "Element must not be null");
		Assert.notNull(childEleNames, "Element names collection must not be null");
		List<String> childEleNameList = Arrays.asList(childEleNames);
		NodeList nl = ele.getChildNodes();
		List<Element> childEles = new ArrayList<>();
		for (int i = 0; i < nl.getLength(); i++) {
			//遍历子节点
			Node node = nl.item(i);
			if (node instanceof Element && nodeNameMatch(node, childEleNameList)) {
				//如果子节点是Element类型，并且节点名称中含有当前子节点名称，添加到子元素列表。
				childEles.add((Element) node);
			}
		}
		return childEles;
	}

	/**
	 * 检索与给定元素名称匹配的给定DOM元素的所有子元素。
	 * 只看给定元素的直接子级别; 不要深入 (与DOM API的 {@code getElementsByTagName}  方法相反)。
	 *
	 * @param ele          要分析的DOM元素
	 * @param childEleName 要查找的子元素名称
	 * @return 一个{@code org.w3c.dom.Element}实例的子节点列表
	 * @see org.w3c.dom.Element
	 * @see org.w3c.dom.Element#getElementsByTagName
	 */
	public static List<Element> getChildElementsByTagName(Element ele, String childEleName) {
		return getChildElementsByTagName(ele, new String[]{childEleName});
	}

	/**
	 * 返回由其名称标识的第一个子元素的实用程序方法。
	 *
	 * @param ele          要分析的DOM元素
	 * @param childEleName 要查找的子元素名称
	 * @return {@code org.w3c.dom.Element} 实例, 如果没有找到返回 {@code null}
	 */
	@Nullable
	public static Element getChildElementByTagName(Element ele, String childEleName) {
		Assert.notNull(ele, "Element must not be null");
		Assert.notNull(childEleName, "Element name must not be null");
		//获取子节点列表
		NodeList nl = ele.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			//遍历子节点
			Node node = nl.item(i);
			if (node instanceof Element && nodeNameMatch(node, childEleName)) {
				//如果子节点是元素类型，且元素名称匹配，则返回子元素
				return (Element) node;
			}
		}
		return null;
	}

	/**
	 * 返回由其名称标识的第一个子元素值的实用程序方法。
	 *
	 * @param ele          要分析的DOM元素
	 * @param childEleName 要查找的子元素名称
	 * @return 提取的文本值，如果找不到子元素，则为 {@code null}
	 */
	@Nullable
	public static String getChildElementValueByTagName(Element ele, String childEleName) {
		Element child = getChildElementByTagName(ele, childEleName);
		//如果子元素不为空，获取子元素的文本值，否则返回null
		return (child != null ? getTextValue(child) : null);
	}

	/**
	 * 获取给定DOM元素的所有子元素。
	 *
	 * @param ele 要分析的DOM元素
	 * @return 包含所有子元素{@code org.w3c.dom.Element}实例的列表
	 * @throws IllegalArgumentException 如果元素为null
	 */
	public static List<Element> getChildElements(Element ele) {
		Assert.notNull(ele, "Element must not be null");
		NodeList nl = ele.getChildNodes();
		List<Element> childEles = new ArrayList<>();
		for (int i = 0; i < nl.getLength(); i++) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				childEles.add((Element) node);
			}
		}
		return childEles;
	}

	/**
	 * 从给定的DOM元素中提取文本值，忽略XML注释。
	 * <p> 将所有 CharacterData节点 和 EntityReference节点 附加到单个字符串值中，不包括注释节点。
	 * 仅公开实际的用户指定文本，没有任何类型的默认值。
	 *
	 * @see CharacterData
	 * @see EntityReference
	 * @see Comment
	 */
	public static String getTextValue(Element valueEle) {
		Assert.notNull(valueEle, "Element must not be null");
		StringBuilder sb = new StringBuilder();
		//获取子节点
		NodeList nl = valueEle.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node item = nl.item(i);
			if ((item instanceof CharacterData && !(item instanceof Comment)) || item instanceof EntityReference) {
				//如果子节点是CharacterData节点并且子节点不是注释节点或者EntityReference节点，则添加到字符串中
				sb.append(item.getNodeValue());
			}
		}
		//返回该字符串
		return sb.toString();
	}

	/**
	 * 支持命名空间的节点名称比较。如果{@link Node#getLocalName}或
	 * {@link Node#getNodeName}等于{@code desiredName}则返回{@code true}，
	 * 否则返回{@code false}。
	 *
	 * @param node 要比较的节点
	 * @param desiredName 期望匹配的名称
	 * @return 名称是否匹配
	 * @throws IllegalArgumentException 如果节点或名称为null
	 */
	public static boolean nodeNameEquals(Node node, String desiredName) {
		Assert.notNull(node, "Node must not be null");
		Assert.notNull(desiredName, "Desired name must not be null");
		return nodeNameMatch(node, desiredName);
	}

	/**
	 * 创建一个将SAX回调事件转换为DOM {@code Node}的SAX {@code ContentHandler}。
	 *
	 * @param node 要接收事件的DOM节点
	 * @return 内容处理器实例
	 */
	public static ContentHandler createContentHandler(Node node) {
		return new DomContentHandler(node);
	}

	/**
	 * 将给定节点的名称和本地名称与给定的所需名称匹配。
	 */
	private static boolean nodeNameMatch(Node node, String desiredName) {
		//节点名称或者本地名称和指定名称相等
		return (desiredName.equals(node.getNodeName()) || desiredName.equals(node.getLocalName()));
	}

	/**
	 * 将给定节点的名称和本地名称与给定的所需名称匹配。
	 */
	private static boolean nodeNameMatch(Node node, Collection<?> desiredNames) {
		return (desiredNames.contains(node.getNodeName()) || desiredNames.contains(node.getLocalName()));
	}

}
