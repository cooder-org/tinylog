/*
 * Copyright 2012 Martin Winandy
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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.cooder.tinylog.util.StoreWriter;
import org.cooder.tinylog.writers.LogEntryValue;

/**
 * Tests for the logging facade API.
 *
 * @see LogEntryForwarder
 */
public class LogEntryForwarderTest extends AbstractTinylogTest {

	/**
	 * Test if the class is a valid utility class.
	 */
	@Test
	public final void testIfValidUtilityClass() {
		testIfValidUtilityClass(LogEntryForwarder.class);
	}

	/**
	 * Test the default forward methods.
	 */
	@Test
	public final void testLogging() {
		StoreWriter writer = new StoreWriter(LogEntryValue.LEVEL, LogEntryValue.FILE, LogEntryValue.MESSAGE);
		Configurator.defaultConfig().writer(writer).level(Level.TRACE).activate();

		LogEntryForwarder.forward(0, Level.INFO, "Hello!");
		LogEntry logEntry = writer.consumeLogEntry();
		assertEquals(Level.INFO, logEntry.getLevel());
		assertEquals("LogEntryForwarderTest.java", logEntry.getFilename());
		assertEquals("Hello!", logEntry.getMessage());

		LogEntryForwarder.forward(0, Level.INFO, "Hello {}!", "World");
		logEntry = writer.consumeLogEntry();
		assertEquals(Level.INFO, logEntry.getLevel());
		assertEquals("LogEntryForwarderTest.java", logEntry.getFilename());
		assertEquals("Hello World!", logEntry.getMessage());

		Exception exception = new Exception();
		LogEntryForwarder.forward(0, Level.ERROR, exception, "Test");
		logEntry = writer.consumeLogEntry();
		assertEquals(Level.ERROR, logEntry.getLevel());
		assertEquals("LogEntryForwarderTest.java", logEntry.getFilename());
		assertEquals("Test", logEntry.getMessage());
		assertEquals(exception, logEntry.getException());
	}

	/**
	 * Test the the advanced forward methods with given stack trace elements.
	 */
	@Test
	public final void testLoggingWithStackTraceElement() {
		StackTraceElement stackTraceElement = new StackTraceElement("MyClass", "?", "?", -1);
		StoreWriter writer = new StoreWriter(LogEntryValue.LEVEL, LogEntryValue.CLASS, LogEntryValue.MESSAGE);
		Configurator.defaultConfig().writer(writer).level(Level.TRACE).activate();

		LogEntryForwarder.forward(stackTraceElement, Level.INFO, "Hello!");
		LogEntry logEntry = writer.consumeLogEntry();
		assertEquals(Level.INFO, logEntry.getLevel());
		assertEquals("MyClass", logEntry.getClassName());
		assertEquals("Hello!", logEntry.getMessage());

		LogEntryForwarder.forward(stackTraceElement, Level.INFO, "Hello {}!", "World");
		logEntry = writer.consumeLogEntry();
		assertEquals(Level.INFO, logEntry.getLevel());
		assertEquals("MyClass", logEntry.getClassName());
		assertEquals("Hello World!", logEntry.getMessage());

		Exception exception = new Exception();
		LogEntryForwarder.forward(stackTraceElement, Level.ERROR, exception, "Test");
		logEntry = writer.consumeLogEntry();
		assertEquals(Level.ERROR, logEntry.getLevel());
		assertEquals("MyClass", logEntry.getClassName());
		assertEquals("Test", logEntry.getMessage());
		assertEquals(exception, logEntry.getException());
	}

}
