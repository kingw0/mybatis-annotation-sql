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
package org.mybatis.extended.sql.utils;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Results;

/**
 * @author kingw
 *
 */
public class ExtendedSqlUtil
{
	/**
	 * 
	 * @param mapper
	 * @param method
	 * @return
	 */
	public static String generateResultMapName(Class<?> mapper, Method method)
	{
		Results results = method.getAnnotation(Results.class);
		if (results != null && !results.id().isEmpty())
		{
			return mapper.getName() + "." + results.id();
		}
		StringBuilder suffix = new StringBuilder();
		for (Class<?> c : method.getParameterTypes())
		{
			suffix.append("-");
			suffix.append(c.getSimpleName());
		}
		if (suffix.length() < 1)
		{
			suffix.append("-void");
		}
		return mapper.getName() + "." + method.getName() + suffix;
	}
}
