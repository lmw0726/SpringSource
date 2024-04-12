/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.view.feed;

import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Item;
import org.springframework.http.MediaType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * RSS Feed视图的抽象超类，使用
 * <a href="https://github.com/rometools/rome">ROME</a>包。
 *
 * <p><b>注意：从Spring 4.1开始，这是基于ROME的{@code com.rometools}变体，
 * 版本为1.5。请升级您的构建依赖项。</b>
 *
 * <p>应用程序特定的视图类将扩展此类。
 * 视图将保存在子类本身中，而不是在模板中。
 * 主要入口点是{@link #buildFeedMetadata}和{@link #buildFeedItems}。
 *
 * <p>感谢Jettro Coenradie和Sergio Bossa提供的原始Feed视图原型！
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see #buildFeedMetadata
 * @see #buildFeedItems
 * @since 3.0
 */
public abstract class AbstractRssFeedView extends AbstractFeedView<Channel> {

	public AbstractRssFeedView() {
		setContentType(MediaType.APPLICATION_RSS_XML_VALUE);
	}


	/**
	 * 创建一个新的Channel实例以容纳条目。
	 * <p>默认返回RSS 2.0频道，但子类可以指定任何频道。
	 */
	@Override
	protected Channel newFeed() {
		return new Channel("rss_2.0");
	}

	/**
	 * 调用{@link #buildFeedItems(Map, HttpServletRequest, HttpServletResponse)}
	 * 获取一组Feed项。
	 */
	@Override
	protected final void buildFeedEntries(Map<String, Object> model, Channel channel,
										  HttpServletRequest request, HttpServletResponse response) throws Exception {

		// 构建频道的条目列表
		List<Item> items = buildFeedItems(model, request, response);
		// 设置频道的项目列表
		channel.setItems(items);
	}

	/**
	 * 子类必须实现此方法以构建Feed项，给定模型。
	 * <p>请注意，传入的HTTP响应仅应用于设置cookie或其他HTTP头。构建的Feed本身将在此方法返回后自动写入响应。
	 *
	 * @param model    模型Map
	 * @param request  如果我们需要区域设置等。不应查看属性。
	 * @param response 如果我们需要设置cookie。不应写入它。
	 * @return 要添加到Feed的Feed项
	 * @throws Exception 文档构建过程中发生的任何异常
	 * @see Item
	 */
	protected abstract List<Item> buildFeedItems(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
