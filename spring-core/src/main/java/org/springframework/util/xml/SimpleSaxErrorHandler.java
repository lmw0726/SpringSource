/*
 * Copyright 2002-2012 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * 简单的{@code org.xml.sax.ErrorHandler}实现：
 * 使用给定的Commons Logging日志记录器记录警告信息，
 * 并重新抛出错误以终止XML处理过程。
 *
 * @author Juergen Hoeller
 * @since 1.2
 */
public class SimpleSaxErrorHandler implements ErrorHandler {

	private final Log logger;


	/**
	 * 为指定的Commons Logging日志记录器创建新的SimpleSaxErrorHandler。
	 */
	public SimpleSaxErrorHandler(Log logger) {
		this.logger = logger;
	}


	@Override
	public void warning(SAXParseException ex) throws SAXException {
		logger.warn("Ignored XML validation warning", ex);
	}

	@Override
	public void error(SAXParseException ex) throws SAXException {
		throw ex;
	}

	@Override
	public void fatalError(SAXParseException ex) throws SAXException {
		throw ex;
	}

}
