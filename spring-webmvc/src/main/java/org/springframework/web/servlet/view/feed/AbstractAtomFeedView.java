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

package org.springframework.web.servlet.view.feed;

import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.atom.Feed;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * Atom Feed视图的抽象超类，使用
 * <a href="https://github.com/rometools/rome">ROME</a>包。
 *
 * <p><b>注意：从Spring 4.1开始，这是基于ROME的{@code com.rometools}变体，
 * 版本为1.5。请升级您的构建依赖项。</b>
 *
 * <p>应用程序特定的视图类将扩展此类。
 * 视图将保存在子类本身中，而不是在模板中。
 * 主要入口点是{@link #buildFeedMetadata}和{@link #buildFeedEntries}。
 *
 * <p>感谢Jettro Coenradie和Sergio Bossa提供的原始Feed视图原型！
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @see #buildFeedMetadata
 * @see #buildFeedEntries
 * @see <a href="https://www.atomenabled.org/developers/syndication/">Atom Syndication Format</a>
 */
public abstract class AbstractAtomFeedView extends AbstractFeedView<Feed> {

	/**
	 * 默认使用的Feed类型。
	 */
	public static final String DEFAULT_FEED_TYPE = "atom_1.0";
	/**
	 * Feed类型
	 */
	private String feedType = DEFAULT_FEED_TYPE;


	public AbstractAtomFeedView() {
		setContentType("application/atom+xml");
	}

	/**
	 * 设置要使用的Rome Feed类型。
	 * <p>默认为Atom 1.0。
	 * @see Feed#setFeedType(String)
	 * @see #DEFAULT_FEED_TYPE
	 */
	public void setFeedType(String feedType) {
		this.feedType = feedType;
	}

	/**
	 * 创建一个新的Feed实例以容纳条目。
	 * <p>默认返回Atom 1.0 feed，但子类可以指定任何Feed。
	 * @see #setFeedType(String)
	 */
	@Override
	protected Feed newFeed() {
		return new Feed(this.feedType);
	}

	/**
	 * 调用{@link #buildFeedEntries(Map, HttpServletRequest, HttpServletResponse)}
	 * 获取一组Feed条目。
	 */
	@Override
	protected final void buildFeedEntries(Map<String, Object> model, Feed feed,
										  HttpServletRequest request, HttpServletResponse response) throws Exception {

		// 构建 Feed 条目列表
		List<Entry> entries = buildFeedEntries(model, request, response);
		// 设置 Feed 的条目列表
		feed.setEntries(entries);
	}

	/**
	 * 子类必须实现此方法以构建Feed条目，给定模型。
	 * <p>请注意，传入的HTTP响应仅应用于设置cookie或其他HTTP头。构建的Feed本身将在此方法返回后自动写入响应。
	 * @param model 模型Map
	 * @param request 如果我们需要区域设置等。不应查看属性。
	 * @param response 如果我们需要设置cookie。不应写入它。
	 * @return 要添加到Feed的Feed条目
	 * @throws Exception 文档构建过程中发生的任何异常
	 * @see Entry
	 */
	protected abstract List<Entry> buildFeedEntries(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception;

}
