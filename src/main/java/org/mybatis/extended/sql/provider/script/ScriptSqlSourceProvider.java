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
package org.mybatis.extended.sql.provider.script;

import java.lang.reflect.Method;

import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.mybatis.extended.sql.provider.SqlSourceProvider;
import org.mybatis.extended.sql.source.ScriptSource;

/**
 * @author kingw
 *
 */
public class ScriptSqlSourceProvider implements SqlSourceProvider
{
	private ScriptSource scriptSource;

	/**
	 * @param scriptSource
	 *            the scriptSource to set
	 */
	public void setScriptSource(ScriptSource scriptSource)
	{
		this.scriptSource = scriptSource;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mybatis.annotation.sql.provider.SqlSourceProvider#sqlSource(org.
	 * apache.ibatis.session.Configuration,
	 * org.apache.ibatis.scripting.LanguageDriver, java.lang.Class,
	 * java.lang.reflect.Method, java.lang.Class)
	 */
	@Override
	public SqlSource sqlSource(Configuration configuration,
	                LanguageDriver languageDriver, Class<?> mapper,
	                Method method, Class<?> parameterType)
	{
		return languageDriver.createSqlSource(
		                configuration, scriptSource.script(configuration,
		                                mapper, method, parameterType),
		                parameterType);
	}

}
