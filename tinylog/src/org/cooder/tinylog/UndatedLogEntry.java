/*
 * Copyright 2017 Martin Winandy
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.cooder.tinylog;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

/**
 * Log entry object without any date.
 */
public final class UndatedLogEntry extends LogEntry {

	/**
	 * @param processId
	 *            The ID of the process (pid)
	 * @param thread
	 *            The current thread
	 * @param context
	 *            Thread-based mapped diagnostic context
	 * @param className
	 *            The fully qualified class name
	 * @param methodName
	 *            The method name
	 * @param filename
	 *            The source filename
	 * @param lineNumber
	 *            The line number
	 * @param level
	 *            The severity level
	 * @param message
	 *            The message of the logging event
	 * @param exception
	 *            The exception of the log entry
	 */
	public UndatedLogEntry(final String processId, final Thread thread, final Map<String, String> context, final String className, final String methodName,
			final String filename, final int lineNumber, final Level level, final String message, final Throwable exception) {
		super(processId, thread, context, className, methodName, filename, lineNumber, level, message, exception);
	}

	@Override
	public Date getDate() {
		return null;
	}
	
	@Override
	public Timestamp getTimestamp() {
		return null;
	}

}
