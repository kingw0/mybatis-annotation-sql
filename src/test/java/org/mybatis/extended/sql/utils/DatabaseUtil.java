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

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;

/**
 * @author kingw
 *
 */
public class DatabaseUtil
{
	public static void runScript(DataSource ds, String resource)
	                throws IOException, SQLException
	{
		Connection connection = ds.getConnection();
		try
		{
			ScriptRunner runner = new ScriptRunner(connection);
			runner.setAutoCommit(true);
			runner.setStopOnError(false);
			runner.setLogWriter(null);
			runner.setErrorLogWriter(null);
			runScript(runner, resource);
		}
		finally
		{
			connection.close();
		}
	}

	public static void runScript(ScriptRunner runner, String resource)
	                throws IOException, SQLException
	{
		Reader reader = Resources.getResourceAsReader(resource);
		try
		{
			runner.runScript(reader);
		}
		finally
		{
			reader.close();
		}
	}
}
