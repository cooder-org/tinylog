/*
 * Copyright 2014 Martin Winandy
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

package org.cooder.tinylog.writers;

import java.util.Date;

import org.cooder.tinylog.Level;
import org.cooder.tinylog.LoggingContext;

/**
 * Supported log entry values in {@link org.cooder.tinylog.LogEntry LogEntry}.
 */
public enum LogEntryValue {

	/**
	 * The current date.
	 *
	 * @see Date
	 */
	DATE,

	/**
	 * The current date with nanosecond precision if supported.
	 */
	PRECISE_DATE,

	/**
	 * The ID of the process (pid).
	 */
	PROCESS_ID,

	/**
	 * The current thread.
	 *
	 * @see Thread
	 */
	THREAD,

	/**
	 * Thread-based mapped diagnostic context.
	 *
	 * @see LoggingContext
	 */
	CONTEXT,

	/**
	 * The fully qualified class name of the caller.
	 */
	CLASS,

	/**
	 * The method name of the caller.
	 */
	METHOD,

	/**
	 * The source filename of the caller.
	 */
	FILE,

	/**
	 * The line number of calling.
	 */
	LINE,

	/**
	 * The severity level.
	 *
	 * @see Level
	 */
	LEVEL,

	/**
	 * The message of the logging event.
	 */
	MESSAGE,

	/**
	 * The exception of the log entry.
	 *
	 * @see Throwable
	 */
	EXCEPTION,

	/**
	 * The rendered log entry.
	 */
	RENDERED_LOG_ENTRY;

}
