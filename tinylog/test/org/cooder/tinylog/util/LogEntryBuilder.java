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

package org.cooder.tinylog.util;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.cooder.tinylog.Level;
import org.cooder.tinylog.LogEntry;
import org.cooder.tinylog.PreciseLogEntry;

/**
 * Fluent API to create and fill {@link LogEntry}.
 */
public final class LogEntryBuilder {

	private Instant instant = null;
	private String processId = null;
	private Thread thread = null;
	private Map<String, String> context = new HashMap<String, String>();
	private String className = null;
	private String method = null;
	private String file = null;
	private int lineNumber = -1;
	private Level level = null;
	private String message = null;
	private Throwable exception = null;
	private String renderedLogEntry = null;

	/**
	 * Set the current date.
	 *
	 * @param date
	 *            Current date
	 * @return The current log entry builder
	 */
	public LogEntryBuilder date(final Date date) {
		return date(date == null ? null : date.toInstant());
	}

	/**
	 * Set the current date as instant.
	 *
	 * @param date
	 *            Current date
	 * @return The current log entry builder
	 */
	public LogEntryBuilder date(final Instant instant) {
		this.instant = instant;
		return this;
	}

	/**
	 * Set the ID of the process (pid).
	 *
	 * @param processId
	 *            ID of the process
	 * @return The current log entry builder
	 */
	public LogEntryBuilder processId(final String processId) {
		this.processId = processId;
		return this;
	}

	/**
	 * Set the current thread.
	 *
	 * @param thread
	 *            Current thread
	 * @return The current log entry builder
	 */
	public LogEntryBuilder thread(final Thread thread) {
		this.thread = thread;
		return this;
	}

	/**
	 * Add a mapping to the logging context.
	 *
	 * @param key
	 *            Key of mapping
	 * @param value
	 *            Value of mapping
	 * @return The current log entry builder instance
	 */
	public LogEntryBuilder context(final String key, final String value) {
		context.put(key, value);
		return this;
	}


	/**
	 * Set the fully qualified class name of the caller.
	 *
	 * @param className
	 *            Fully qualified class name of the caller
	 * @return The current log entry builder
	 */
	public LogEntryBuilder className(final String className) {
		this.className = className;
		return this;
	}

	/**
	 * Set the method name of the caller.
	 *
	 * @param method
	 *            Method name of the caller
	 * @return The current log entry builder
	 */
	public LogEntryBuilder method(final String method) {
		this.method = method;
		return this;
	}

	/**
	 * Set the source filename of the caller.
	 *
	 * @param file
	 *            Source filename of the caller
	 * @return The current log entry builder
	 */
	public LogEntryBuilder file(final String file) {
		this.file = file;
		return this;
	}

	/**
	 * Set the line number of calling.
	 *
	 * @param lineNumber
	 *            Line number of calling
	 * @return The current log entry builder
	 */
	public LogEntryBuilder lineNumber(final int lineNumber) {
		this.lineNumber = lineNumber;
		return this;
	}

	/**
	 * Set the logging level.
	 *
	 * @param level
	 *            Logging level
	 * @return The current log entry builder
	 */
	public LogEntryBuilder level(final Level level) {
		this.level = level;
		return this;
	}

	/**
	 * Set the message of the logging event.
	 *
	 * @param message
	 *            Message of the logging event
	 * @return The current log entry builder
	 */
	public LogEntryBuilder message(final String message) {
		this.message = message;
		return this;
	}

	/**
	 * Set the exception of the log entry.
	 *
	 * @param exception
	 *            Exception of the log entry
	 * @return The current log entry builder
	 */
	public LogEntryBuilder exception(final Throwable exception) {
		this.exception = exception;
		return this;
	}

	/**
	 * Set the rendered log entry.
	 *
	 * @param renderedLogEntry
	 *            Rendered log entry
	 * @return The current log entry builder
	 */
	public LogEntryBuilder renderedLogEntry(final String renderedLogEntry) {
		this.renderedLogEntry = renderedLogEntry;
		return this;
	}

	/**
	 * Get the created log entry.
	 *
	 * @return Created log entry
	 */
	public LogEntry create() {
		LogEntry logEntry = new PreciseLogEntry(instant, processId, thread, context, className, method, file, lineNumber, level, message, exception);
		try {
			Method setter = LogEntry.class.getDeclaredMethod("setRenderedLogEntry", String.class);
			setter.setAccessible(true);
			setter.invoke(logEntry, renderedLogEntry);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		} catch (SecurityException ex) {
			throw new RuntimeException(ex);
		}
		return logEntry;
	}

}
