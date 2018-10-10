/**
 *    Copyright 2018 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.extended.sql.provider;

import java.lang.reflect.Method;

import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;

/**
 * @author kingw
 *
 */
public interface SqlSourceProvider
{
	static final String DEFAULT_SQLSOURCE_PROVIDER = "default";

	/**
	 * 
	 * @param configuration
	 * @param languageDriver
	 * @param mapper
	 * @param method
	 * @param parameterType
	 * @return
	 */
	SqlSource sqlSource(Configuration configuration,
	                LanguageDriver languageDriver, Class<?> mapper,
	                Method method, Class<?> parameterType);
}
