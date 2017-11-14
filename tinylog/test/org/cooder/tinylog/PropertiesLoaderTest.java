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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.cooder.tinylog.hamcrest.ArrayMatchers.containsCollectionWithSizes;
import static org.cooder.tinylog.hamcrest.ArrayMatchers.distinctContentInArray;
import static org.cooder.tinylog.hamcrest.ArrayMatchers.typesInArray;
import static org.cooder.tinylog.hamcrest.ClassMatchers.type;
import static org.cooder.tinylog.hamcrest.CollectionMatchers.types;
import static org.cooder.tinylog.hamcrest.StringMatchers.matchesPattern;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import mockit.Mock;
import mockit.MockUp;

import org.junit.Test;
import org.cooder.tinylog.labelers.CountLabeler;
import org.cooder.tinylog.labelers.Labeler;
import org.cooder.tinylog.labelers.TimestampLabeler;
import org.cooder.tinylog.mocks.ClassLoaderMock;
import org.cooder.tinylog.policies.DailyPolicy;
import org.cooder.tinylog.policies.Policy;
import org.cooder.tinylog.policies.SizePolicy;
import org.cooder.tinylog.policies.StartupPolicy;
import org.cooder.tinylog.util.FileHelper;
import org.cooder.tinylog.util.NullWriter;
import org.cooder.tinylog.util.PropertiesBuilder;
import org.cooder.tinylog.writers.ConsoleWriter;
import org.cooder.tinylog.writers.FileWriter;
import org.cooder.tinylog.writers.PropertiesSupport;
import org.cooder.tinylog.writers.Property;
import org.cooder.tinylog.writers.Writer;

/**
 * Test properties loader.
 *
 * @see PropertiesLoader
 */
public class PropertiesLoaderTest extends AbstractTinylogTest {

	/**
	 * Test if the class is a valid utility class.
	 */
	@Test
	public final void testIfValidUtilityClass() {
		testIfValidUtilityClass(PropertiesLoader.class);
	}

	/**
	 * Test read a complete configuration.
	 */
	@Test
	public final void testReadProperties() {
		PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.level", "warning").set("tinylog.format", "{message}")
				.set("tinylog.locale", "de").set("tinylog.stacktrace", "42").set("tinylog.writer", "null").set("tinylog.writingthread", "true");

		Configurator configurator = PropertiesLoader.readProperties(propertiesBuilder.create());
		assertNotNull(configurator);

		Configuration configuration = configurator.create();
		assertEquals(Level.WARNING, configuration.getLevel());
		assertEquals("{message}", configuration.getFormatPattern());
		assertEquals(new Locale("de"), configuration.getLocale());
		assertNotSame(Locale.getDefault(), configuration.getLocale());
		assertEquals(42, configuration.getMaxStackTraceElements());
		assertThat(configuration.getWriters(), empty());
		assertNotNull(configuration.getWritingThread());
	}

	/**
	 * Test reading logging level.
	 */
	@Test
	public final void testReadLevel() {
		Level defaultLevel = Configurator.defaultConfig().create().getLevel();

		Configurator configurator = Configurator.defaultConfig();
		PropertiesLoader.readLevel(configurator, new PropertiesBuilder().create());
		assertEquals(defaultLevel, configurator.create().getLevel());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readLevel(configurator, new PropertiesBuilder().set("tinylog.level", "").create());
		assertEquals(defaultLevel, configurator.create().getLevel());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readLevel(configurator, new PropertiesBuilder().set("tinylog.level", "TRACE").create());
		assertEquals(Level.TRACE, configurator.create().getLevel());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readLevel(configurator, new PropertiesBuilder().set("tinylog.level", "warning").create());
		assertEquals(Level.WARNING, configurator.create().getLevel());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readLevel(configurator, new PropertiesBuilder().set("tinylog.level", "ErrOr").create());
		assertEquals(Level.ERROR, configurator.create().getLevel());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readLevel(configurator, new PropertiesBuilder().set("tinylog.level", "invalid").create());
		assertEquals(defaultLevel, configurator.create().getLevel());
		assertEquals("LOGGER WARNING: \"invalid\" is an invalid severity level", getErrorStream().nextLine());
	}

	/**
	 * Test reading custom logging levels for packages and classes.
	 */
	@Test
	public final void testReadCustomLevels() {
		Configurator configurator = Configurator.defaultConfig();
		PropertiesLoader.readLevel(configurator, new PropertiesBuilder().set("tinylog.level@a.b", "WARNING").create());
		assertEquals(Level.WARNING, configurator.create().getLevel("a.b"));

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readLevel(configurator, new PropertiesBuilder().set("tinylog.level@a.b.c", "trace").create());
		assertEquals(Level.TRACE, configurator.create().getLevel("a.b.c"));

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readLevel(configurator, new PropertiesBuilder().set("tinylog.level@org.cooder.tinylog", "ErrOr").create());
		assertEquals(Level.ERROR, configurator.create().getLevel("org.cooder.tinylog"));

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readLevel(configurator, new PropertiesBuilder().set("tinylog.level@org.cooder.tinylog", "nonsense").create());
		assertEquals(configurator.create().getLevel(), configurator.create().getLevel("org.cooder.tinylog"));
		assertEquals("LOGGER WARNING: \"nonsense\" is an invalid severity level", getErrorStream().nextLine());
	}

	/**
	 * Test reading format pattern.
	 */
	@Test
	public final void testReadFormatPattern() {
		String defaultFormatPattern = Configurator.defaultConfig().create().getFormatPattern();

		Configurator configurator = Configurator.defaultConfig();
		PropertiesLoader.readFormatPattern(configurator, new PropertiesBuilder().create());
		assertEquals(defaultFormatPattern, configurator.create().getFormatPattern());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readFormatPattern(configurator, new PropertiesBuilder().set("tinylog.format", "").create());
		assertEquals(defaultFormatPattern, configurator.create().getFormatPattern());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readFormatPattern(configurator, new PropertiesBuilder().set("tinylog.format", "My log entry: {message}").create());
		assertEquals("My log entry: {message}", configurator.create().getFormatPattern());
	}

	/**
	 * Test reading locale for format pattern.
	 */
	@Test
	public final void testReadLocale() {
		Locale defaultLocale = Configurator.defaultConfig().create().getLocale();

		Configurator configurator = Configurator.defaultConfig();
		PropertiesLoader.readLocale(configurator, new PropertiesBuilder().create());
		assertEquals(defaultLocale, configurator.create().getLocale());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readLocale(configurator, new PropertiesBuilder().set("tinylog.locale", "").create());
		assertEquals(defaultLocale, configurator.create().getLocale());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readLocale(configurator, new PropertiesBuilder().set("tinylog.locale", "de").create());
		assertEquals(Locale.GERMAN, configurator.create().getLocale());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readLocale(configurator, new PropertiesBuilder().set("tinylog.locale", "de_DE").create());
		assertEquals(Locale.GERMANY, configurator.create().getLocale());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readLocale(configurator, new PropertiesBuilder().set("tinylog.locale", "en").create());
		assertEquals(Locale.ENGLISH, configurator.create().getLocale());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readLocale(configurator, new PropertiesBuilder().set("tinylog.locale", "en_GB").create());
		assertEquals(Locale.UK, configurator.create().getLocale());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readLocale(configurator, new PropertiesBuilder().set("tinylog.locale", "en_US").create());
		assertEquals(Locale.US, configurator.create().getLocale());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readLocale(configurator, new PropertiesBuilder().set("tinylog.locale", "en_US_WIN").create());
		assertEquals(new Locale("en", "US", "WIN"), configurator.create().getLocale());
	}

	/**
	 * Test reading stack trace limitation.
	 */
	@Test
	public final void testReadMaxStackTraceElements() {
		int defaultMaxStackTraceElements = Configurator.defaultConfig().create().getMaxStackTraceElements();

		Configurator configurator = Configurator.defaultConfig();
		PropertiesLoader.readMaxStackTraceElements(configurator, new PropertiesBuilder().create());
		assertEquals(defaultMaxStackTraceElements, configurator.create().getMaxStackTraceElements());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readMaxStackTraceElements(configurator, new PropertiesBuilder().set("tinylog.stacktrace", "").create());
		assertEquals(defaultMaxStackTraceElements, configurator.create().getMaxStackTraceElements());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readMaxStackTraceElements(configurator, new PropertiesBuilder().set("tinylog.stacktrace", "0").create());
		assertEquals(0, configurator.create().getMaxStackTraceElements());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readMaxStackTraceElements(configurator, new PropertiesBuilder().set("tinylog.stacktrace", "1").create());
		assertEquals(1, configurator.create().getMaxStackTraceElements());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readMaxStackTraceElements(configurator, new PropertiesBuilder().set("tinylog.stacktrace", "42").create());
		assertEquals(42, configurator.create().getMaxStackTraceElements());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readMaxStackTraceElements(configurator, new PropertiesBuilder().set("tinylog.stacktrace", "-1").create());
		assertEquals(Integer.MAX_VALUE, configurator.create().getMaxStackTraceElements());

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readMaxStackTraceElements(configurator, new PropertiesBuilder().set("tinylog.stacktrace", "invalid").create());
		assertEquals(defaultMaxStackTraceElements, configurator.create().getMaxStackTraceElements());
		assertEquals("LOGGER WARNING: \"invalid\" is an invalid stack trace size", getErrorStream().nextLine());
	}

	/**
	 * Test reading <code>null</code> as writer (no writer).
	 */
	@Test
	public final void testReadNullWriter() {
		Configurator configurator = Configurator.defaultConfig();

		PropertiesLoader.readWriters(configurator, new PropertiesBuilder().set("tinylog.writer", "null").create());
		assertThat(configurator.create().getWriters(), empty());

		PropertiesLoader.readWriters(configurator, new PropertiesBuilder().set("tinylog.writer1", "null").create());
		assertThat(configurator.create().getWriters(), empty());

		PropertiesLoader.readWriters(configurator, new PropertiesBuilder().set("tinylog.writer1", "console").set("tinylog.writer2", "null").create());
		assertThat(configurator.create().getWriters(), types(ConsoleWriter.class));
	}

	/**
	 * Test reading multiple writers.
	 *
	 * @throws IOException
	 *             Failed to create temporary file
	 */
	@Test
	public final void testReadMultipleWriters() throws IOException {
		File logFile = FileHelper.createTemporaryFile("log");

		Configurator configurator = Configurator.defaultConfig().writer(null);

		PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
		propertiesBuilder.set("tinylog.writer1", "console");
		propertiesBuilder.set("tinylog.writer2", "file").set("tinylog.writer2.filename", logFile.getAbsolutePath());
		PropertiesLoader.readWriters(configurator, propertiesBuilder.create());

		List<Writer> writers = configurator.create().getWriters();
		assertThat(writers, types(ConsoleWriter.class, FileWriter.class));
		FileWriter fileWriter = (FileWriter) writers.get(1);
		assertEquals(logFile.getAbsolutePath(), fileWriter.getFilename());

		logFile.delete();
	}

	/**
	 * Test reading a writer without any properties.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterWithoutProperties() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());

			Configurator configurator = Configurator.defaultConfig();
			PropertiesLoader.readWriters(configurator, new PropertiesBuilder().set("tinylog.writer", "properties").create());
			List<Writer> writers = configurator.create().getWriters();
			assertThat(writers, types(PropertiesWriter.class));
		}
	}

	/**
	 * Test reading a writer with custom severity level.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterWithCustomLevel() throws IOException {
		/* One writer with custom severity level */

		Configurator configurator = Configurator.defaultConfig().writer(null);

		PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
		propertiesBuilder.set("tinylog.writer", "console").set("tinylog.writer.level", "info");
		PropertiesLoader.readWriters(configurator, propertiesBuilder.create());

		Configuration configuration = configurator.create();
		assertThat(configuration.getWriters(), types(ConsoleWriter.class));
		assertThat(configuration.getEffectiveWriters(Level.TRACE), emptyArray());
		assertThat(configuration.getEffectiveWriters(Level.DEBUG), emptyArray());
		assertThat(configuration.getEffectiveWriters(Level.INFO), typesInArray(ConsoleWriter.class));
		assertThat(configuration.getEffectiveWriters(Level.WARNING), typesInArray(ConsoleWriter.class));
		assertThat(configuration.getEffectiveWriters(Level.ERROR), typesInArray(ConsoleWriter.class));

		/* Two writers, one with custom severity level */

		configurator = Configurator.defaultConfig().writer(null);
		File logFile = FileHelper.createTemporaryFile("log");

		propertiesBuilder = new PropertiesBuilder();
		propertiesBuilder.set("tinylog.writer1", "console");
		propertiesBuilder.set("tinylog.writer2", "file").set("tinylog.writer2.filename", logFile.getAbsolutePath()).set("tinylog.writer2.level", "info");
		PropertiesLoader.readWriters(configurator, propertiesBuilder.create());

		configuration = configurator.create();
		assertThat(configuration.getWriters(), types(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveWriters(Level.TRACE), typesInArray(ConsoleWriter.class));
		assertThat(configuration.getEffectiveWriters(Level.DEBUG), typesInArray(ConsoleWriter.class));
		assertThat(configuration.getEffectiveWriters(Level.INFO), typesInArray(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveWriters(Level.WARNING), typesInArray(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveWriters(Level.ERROR), typesInArray(ConsoleWriter.class, FileWriter.class));

		logFile.delete();

		/* Two writers, both with custom severity level */

		configurator = Configurator.defaultConfig().writer(null);
		logFile = FileHelper.createTemporaryFile("log");

		propertiesBuilder = new PropertiesBuilder();
		propertiesBuilder.set("tinylog.writer1", "console").set("tinylog.writer1.level", "debug");
		propertiesBuilder.set("tinylog.writer2", "file").set("tinylog.writer2.filename", logFile.getAbsolutePath()).set("tinylog.writer2.level", "info");
		PropertiesLoader.readWriters(configurator, propertiesBuilder.create());

		configuration = configurator.create();
		assertThat(configuration.getWriters(), types(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveWriters(Level.TRACE), emptyArray());
		assertThat(configuration.getEffectiveWriters(Level.DEBUG), typesInArray(ConsoleWriter.class));
		assertThat(configuration.getEffectiveWriters(Level.INFO), typesInArray(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveWriters(Level.WARNING), typesInArray(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveWriters(Level.ERROR), typesInArray(ConsoleWriter.class, FileWriter.class));

		logFile.delete();
	}

	/**
	 * Test reading a writer with custom format pattern.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterWithCustomFormatPattern() throws IOException {
		/* One writer with custom format pattern */

		Configurator configurator = Configurator.defaultConfig().writer(null);

		PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
		propertiesBuilder.set("tinylog.writer", "console").set("tinylog.writer.format", "123");
		PropertiesLoader.readWriters(configurator, propertiesBuilder.create());

		Configuration configuration = configurator.create();
		assertThat(configuration.getWriters(), types(ConsoleWriter.class));
		assertThat(configuration.getEffectiveFormatTokens(Level.TRACE), containsCollectionWithSizes(1));
		assertThat(configuration.getEffectiveFormatTokens(Level.DEBUG), containsCollectionWithSizes(1));
		assertThat(configuration.getEffectiveFormatTokens(Level.INFO), containsCollectionWithSizes(1));
		assertThat(configuration.getEffectiveFormatTokens(Level.WARNING), containsCollectionWithSizes(1));
		assertThat(configuration.getEffectiveFormatTokens(Level.ERROR), containsCollectionWithSizes(1));

		/* Two writers, one with custom format pattern */

		configurator = Configurator.defaultConfig().writer(null).formatPattern("abc");
		File logFile = FileHelper.createTemporaryFile("log");

		propertiesBuilder = new PropertiesBuilder();
		propertiesBuilder.set("tinylog.writer1", "console");
		propertiesBuilder.set("tinylog.writer2", "file").set("tinylog.writer2.filename", logFile.getAbsolutePath()).set("tinylog.writer2.format", "xyz");
		PropertiesLoader.readWriters(configurator, propertiesBuilder.create());

		configuration = configurator.create();
		assertThat(configuration.getWriters(), types(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveFormatTokens(Level.TRACE), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));
		assertThat(configuration.getEffectiveFormatTokens(Level.DEBUG), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));
		assertThat(configuration.getEffectiveFormatTokens(Level.INFO), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));
		assertThat(configuration.getEffectiveFormatTokens(Level.WARNING), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));
		assertThat(configuration.getEffectiveFormatTokens(Level.ERROR), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));

		logFile.delete();

		/* Two writers, both with custom format pattern */

		configurator = Configurator.defaultConfig().writer(null);
		logFile = FileHelper.createTemporaryFile("log");

		propertiesBuilder = new PropertiesBuilder();
		propertiesBuilder.set("tinylog.writer1", "console").set("tinylog.writer1.format", "abc");
		propertiesBuilder.set("tinylog.writer2", "file").set("tinylog.writer2.filename", logFile.getAbsolutePath()).set("tinylog.writer2.format", "xyz");
		PropertiesLoader.readWriters(configurator, propertiesBuilder.create());

		configuration = configurator.create();
		assertThat(configuration.getWriters(), types(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveFormatTokens(Level.TRACE), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));
		assertThat(configuration.getEffectiveFormatTokens(Level.DEBUG), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));
		assertThat(configuration.getEffectiveFormatTokens(Level.INFO), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));
		assertThat(configuration.getEffectiveFormatTokens(Level.WARNING), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));
		assertThat(configuration.getEffectiveFormatTokens(Level.ERROR), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));

		logFile.delete();
	}

	/**
	 * Test reading a writer with custom severity level and format pattern.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterWithCustomLevelAndFormatPattern() throws IOException {
		/* One writer with custom severity level and format pattern */

		Configurator configurator = Configurator.defaultConfig().writer(null);

		PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
		propertiesBuilder.set("tinylog.writer", "console").set("tinylog.writer.level", "info").set("tinylog.writer.format", "123");
		PropertiesLoader.readWriters(configurator, propertiesBuilder.create());

		Configuration configuration = configurator.create();
		assertThat(configuration.getWriters(), types(ConsoleWriter.class));
		assertThat(configuration.getEffectiveWriters(Level.TRACE), emptyArray());
		assertThat(configuration.getEffectiveFormatTokens(Level.TRACE), emptyArray());
		assertThat(configuration.getEffectiveWriters(Level.DEBUG), emptyArray());
		assertThat(configuration.getEffectiveFormatTokens(Level.DEBUG), emptyArray());
		assertThat(configuration.getEffectiveWriters(Level.INFO), typesInArray(ConsoleWriter.class));
		assertThat(configuration.getEffectiveFormatTokens(Level.INFO), containsCollectionWithSizes(1));
		assertThat(configuration.getEffectiveWriters(Level.WARNING), typesInArray(ConsoleWriter.class));
		assertThat(configuration.getEffectiveFormatTokens(Level.WARNING), containsCollectionWithSizes(1));
		assertThat(configuration.getEffectiveWriters(Level.ERROR), typesInArray(ConsoleWriter.class));
		assertThat(configuration.getEffectiveFormatTokens(Level.ERROR), containsCollectionWithSizes(1));

		/* Two writers, one with custom severity level and format pattern */

		configurator = Configurator.defaultConfig().writer(null).formatPattern("abc");
		File logFile = FileHelper.createTemporaryFile("log");

		propertiesBuilder = new PropertiesBuilder();
		propertiesBuilder.set("tinylog.writer1", "console");
		propertiesBuilder.set("tinylog.writer2", "file").set("tinylog.writer2.filename", logFile.getAbsolutePath());
		propertiesBuilder.set("tinylog.writer2.level", "info").set("tinylog.writer2.format", "xyz");
		PropertiesLoader.readWriters(configurator, propertiesBuilder.create());

		configuration = configurator.create();
		assertThat(configuration.getWriters(), types(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveWriters(Level.TRACE), typesInArray(ConsoleWriter.class));
		assertThat(configuration.getEffectiveFormatTokens(Level.TRACE), containsCollectionWithSizes(1));
		assertThat(configuration.getEffectiveWriters(Level.DEBUG), typesInArray(ConsoleWriter.class));
		assertThat(configuration.getEffectiveFormatTokens(Level.DEBUG), containsCollectionWithSizes(1));
		assertThat(configuration.getEffectiveWriters(Level.INFO), typesInArray(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveFormatTokens(Level.INFO), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));
		assertThat(configuration.getEffectiveWriters(Level.WARNING), typesInArray(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveFormatTokens(Level.WARNING), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));
		assertThat(configuration.getEffectiveWriters(Level.ERROR), typesInArray(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveFormatTokens(Level.ERROR), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));

		logFile.delete();

		/* Two writers, both with custom severity level and format pattern */

		configurator = Configurator.defaultConfig().writer(null);
		logFile = FileHelper.createTemporaryFile("log");

		propertiesBuilder = new PropertiesBuilder();
		propertiesBuilder.set("tinylog.writer1", "console");
		propertiesBuilder.set("tinylog.writer1.level", "debug").set("tinylog.writer1.format", "abc");
		propertiesBuilder.set("tinylog.writer2", "file").set("tinylog.writer2.filename", logFile.getAbsolutePath());
		propertiesBuilder.set("tinylog.writer2.level", "info").set("tinylog.writer2.format", "xyz");
		PropertiesLoader.readWriters(configurator, propertiesBuilder.create());

		configuration = configurator.create();
		assertThat(configuration.getWriters(), types(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveWriters(Level.TRACE), emptyArray());
		assertThat(configuration.getEffectiveFormatTokens(Level.TRACE), emptyArray());
		assertThat(configuration.getEffectiveWriters(Level.DEBUG), typesInArray(ConsoleWriter.class));
		assertThat(configuration.getEffectiveFormatTokens(Level.DEBUG), containsCollectionWithSizes(1));
		assertThat(configuration.getEffectiveWriters(Level.INFO), typesInArray(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveFormatTokens(Level.INFO), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));
		assertThat(configuration.getEffectiveWriters(Level.WARNING), typesInArray(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveFormatTokens(Level.WARNING), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));
		assertThat(configuration.getEffectiveWriters(Level.ERROR), typesInArray(ConsoleWriter.class, FileWriter.class));
		assertThat(configuration.getEffectiveFormatTokens(Level.ERROR), allOf(containsCollectionWithSizes(1, 1), distinctContentInArray()));

		logFile.delete();
	}

	/**
	 * Test reading a writer with boolean properties.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterWithBooleanProperties() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties");

			Configurator configurator = Configurator.defaultConfig();
			propertiesBuilder.set("tinylog.writer.boolean", "true");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			List<Writer> writers = configurator.create().getWriters();
			assertThat(writers, types(PropertiesWriter.class));
			PropertiesWriter propertiesWriter = (PropertiesWriter) writers.get(0);
			assertEquals(Boolean.TRUE, propertiesWriter.booleanValue);

			configurator = Configurator.defaultConfig();
			propertiesBuilder.set("tinylog.writer.boolean", "false");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			writers = configurator.create().getWriters();
			assertThat(writers, types(PropertiesWriter.class));
			propertiesWriter = (PropertiesWriter) writers.get(0);
			assertEquals(Boolean.FALSE, propertiesWriter.booleanValue);

			configurator = Configurator.defaultConfig();
			propertiesBuilder.set("tinylog.writer.boolean", "abc");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			writers = configurator.create().getWriters();
			assertThat(writers, types(ConsoleWriter.class));
			assertEquals("LOGGER ERROR: \"abc\" for \"tinylog.writer.boolean\" is an invalid boolean", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());
		}
	}

	/**
	 * Test reading a writer with integer properties.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterWithIntegerProperties() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties");

			Configurator configurator = Configurator.defaultConfig();
			propertiesBuilder = propertiesBuilder.set("tinylog.writer.int", "42");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			List<Writer> writers = configurator.create().getWriters();
			assertThat(writers, types(PropertiesWriter.class));
			PropertiesWriter propertiesWriter = (PropertiesWriter) writers.get(0);
			assertEquals(Integer.valueOf(42), propertiesWriter.intValue);

			configurator = Configurator.defaultConfig();
			propertiesBuilder = propertiesBuilder.set("tinylog.writer.int", "abc");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			writers = configurator.create().getWriters();
			assertThat(writers, types(ConsoleWriter.class));
			assertEquals("LOGGER ERROR: \"abc\" for \"tinylog.writer.int\" is an invalid number", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());
		}
	}

	/**
	 * Test reading a writer with string properties.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterWithStringProperties() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());

			Configurator configurator = Configurator.defaultConfig();
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.string", "abc");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			List<Writer> writers = configurator.create().getWriters();
			assertThat(writers, types(PropertiesWriter.class));
			PropertiesWriter propertiesWriter = (PropertiesWriter) writers.get(0);
			assertEquals("abc", propertiesWriter.stringValue);
		}
	}

	/**
	 * Test reading a writer with string array properties.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterWithStringArrayProperties() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties");

			Configurator configurator = Configurator.defaultConfig();
			propertiesBuilder.set("tinylog.writer.strings", "abc");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			List<Writer> writers = configurator.create().getWriters();
			assertThat(writers, types(PropertiesWriter.class));
			PropertiesWriter propertiesWriter = (PropertiesWriter) writers.get(0);
			assertArrayEquals(new String[] { "abc" }, propertiesWriter.stringsValue);

			configurator = Configurator.defaultConfig();
			propertiesBuilder.set("tinylog.writer.strings", "abc, test");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			writers = configurator.create().getWriters();
			assertThat(writers, types(PropertiesWriter.class));
			propertiesWriter = (PropertiesWriter) writers.get(0);
			assertArrayEquals(new String[] { "abc", "test" }, propertiesWriter.stringsValue);

			configurator = Configurator.defaultConfig();
			propertiesBuilder.set("tinylog.writer.strings", "");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			writers = configurator.create().getWriters();
			assertThat(writers, types(PropertiesWriter.class));
			propertiesWriter = (PropertiesWriter) writers.get(0);
			assertArrayEquals(new String[] { "" }, propertiesWriter.stringsValue);

			configurator = Configurator.defaultConfig();
			propertiesBuilder.set("tinylog.writer.strings", ",,");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			writers = configurator.create().getWriters();
			assertThat(writers, types(PropertiesWriter.class));
			propertiesWriter = (PropertiesWriter) writers.get(0);
			assertArrayEquals(new String[] { "", "", "" }, propertiesWriter.stringsValue);
		}
	}

	/**
	 * Test reading a writer with labeler properties.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterWithLabelerProperties() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties");

			Configurator configurator = Configurator.defaultConfig();
			propertiesBuilder.set("tinylog.writer.labeler", "count");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			List<Writer> writers = configurator.create().getWriters();
			assertThat(writers, types(PropertiesWriter.class));
			PropertiesWriter propertiesWriter = (PropertiesWriter) writers.get(0);
			assertThat(propertiesWriter.labeler, type(CountLabeler.class));

			configurator = Configurator.defaultConfig();
			propertiesBuilder.set("tinylog.writer.labeler", "timestamp: yyyy");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			writers = configurator.create().getWriters();
			assertThat(writers, types(PropertiesWriter.class));
			propertiesWriter = (PropertiesWriter) writers.get(0);
			Labeler labeler = propertiesWriter.labeler;
			assertThat(labeler, type(TimestampLabeler.class));
			labeler.init(configurator.create());
			assertEquals(new File(MessageFormat.format("test.{0,date,yyyy}.log", new Date())).getAbsoluteFile(), labeler.getLogFile(new File("test.log")));
		}
	}

	/**
	 * Test reading a writer with policy properties.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterWithPolicyProperties() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties");

			Configurator configurator = Configurator.defaultConfig();
			propertiesBuilder.set("tinylog.writer.policy", "startup");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			List<Writer> writers = configurator.create().getWriters();
			assertThat(writers, types(PropertiesWriter.class));
			PropertiesWriter propertiesWriter = (PropertiesWriter) writers.get(0);
			assertThat(propertiesWriter.policy, type(StartupPolicy.class));

			configurator = Configurator.defaultConfig();
			propertiesBuilder.set("tinylog.writer.policy", "size: 10");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			writers = configurator.create().getWriters();
			assertThat(writers, types(PropertiesWriter.class));
			propertiesWriter = (PropertiesWriter) writers.get(0);
			assertThat(propertiesWriter.policy, type(SizePolicy.class));
		}
	}

	/**
	 * Test reading a writer with policy array properties.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterWithPolicyArrayProperties() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties");

			Configurator configurator = Configurator.defaultConfig();
			propertiesBuilder.set("tinylog.writer.policies", "startup");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			List<Writer> writers = configurator.create().getWriters();
			assertThat(writers, types(PropertiesWriter.class));
			PropertiesWriter propertiesWriter = (PropertiesWriter) writers.get(0);
			assertThat(propertiesWriter.policies, typesInArray(StartupPolicy.class));

			configurator = Configurator.defaultConfig();
			propertiesBuilder.set("tinylog.writer.policies", "startup, daily");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			writers = configurator.create().getWriters();
			assertThat(writers, types(PropertiesWriter.class));
			propertiesWriter = (PropertiesWriter) writers.get(0);
			assertThat(propertiesWriter.policies, typesInArray(StartupPolicy.class, DailyPolicy.class));
		}
	}

	/**
	 * Test reading a writer with missing required property.
	 */
	@Test
	public final void testReadWriterWithMissingProperty() {
		Configurator configurator = Configurator.defaultConfig();
		PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "file");
		PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
		List<Writer> writers = configurator.create().getWriters();
		assertThat(writers, types(ConsoleWriter.class));
		assertEquals("LOGGER ERROR: Missing required property \"tinylog.writer.filename\"", getErrorStream().nextLine());
		assertEquals("LOGGER ERROR: Failed to initialize file writer", getErrorStream().nextLine());
	}

	/**
	 * Test reading a writer with unsupported property type.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterWithUnsupportedProperties() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), ClassPropertyWriter.class.getName());

			Configurator configurator = Configurator.defaultConfig();
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.class", "MyClass");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			List<Writer> writers = configurator.create().getWriters();
			assertThat(writers, types(ConsoleWriter.class));
			assertThat(getErrorStream().nextLine(), matchesPattern("LOGGER ERROR\\: \"" + Pattern.quote(Class.class.getName())
					+ "\" for \"tinylog\\.writer\\.class\" is an unsupported type \\(.+ are supported\\)"));
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());
		}
	}

	/**
	 * Test reading a nonexistent writer.
	 */
	@Test
	public final void testReadInvalidWriter() {
		Configurator configurator = Configurator.defaultConfig();
		PropertiesLoader.readWriters(configurator, new PropertiesBuilder().set("tinylog.writer", "invalid").create());
		List<Writer> writers = configurator.create().getWriters();
		assertThat(writers, types(ConsoleWriter.class));
		assertEquals("LOGGER ERROR: Cannot find a writer for the name \"invalid\"", getErrorStream().nextLine());
	}

	/**
	 * Test reading a nonexistent labeler.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadInvalidLabeler() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());

			Configurator configurator = Configurator.defaultConfig();
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.labeler", "invalid");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER ERROR: Cannot find a labeler for the name \"invalid\"", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());
		}
	}

	/**
	 * Test reading a nonexistent policy.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadInvalidPolicy() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());

			Configurator configurator = Configurator.defaultConfig();
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.policy", "invalid");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER ERROR: Cannot find a policy for the name \"invalid\"", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());

			configurator = Configurator.defaultConfig();
			propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.policies", "invalid");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER ERROR: Cannot find a policy for the name \"invalid\"", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());
		}
	}

	/**
	 * Test reading a writer if there is no file with registered writers.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterIfNoRegistered() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), (String) null);

			Configurator configurator = Configurator.defaultConfig();
			PropertiesLoader.readWriters(configurator, new PropertiesBuilder().set("tinylog.writer", "console").create());
			List<Writer> writers = configurator.create().getWriters();
			assertThat(writers, types(ConsoleWriter.class));
			assertEquals("LOGGER ERROR: Cannot find a writer for the name \"console\"", getErrorStream().nextLine());
		}
	}

	/**
	 * Test reading a labeler if there is no file with registered labelers.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadLabelerIfNoRegistered() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());
			mock.set("META-INF/services/" + Labeler.class.getPackage().getName(), (String) null);

			Configurator configurator = Configurator.defaultConfig();
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.labeler", "count");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER ERROR: Cannot find a labeler for the name \"count\"", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());
		}
	}

	/**
	 * Test reading a policy if there is no file with registered policies.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadPolicyIfNoRegistered() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());
			mock.set("META-INF/services/" + Policy.class.getPackage().getName(), (String) null);

			Configurator configurator = Configurator.defaultConfig();
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.policy", "startup");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER ERROR: Cannot find a policy for the name \"startup\"", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());

			configurator = Configurator.defaultConfig();
			propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.policies", "startup");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER ERROR: Cannot find a policy for the name \"startup\"", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());
		}
	}

	/**
	 * Test reading a writer if failed to open and read the file with registered writers.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterIfFailedReadingServiceFile() throws IOException {
		new MockUp<BufferedReader>() {
			@Mock
			public String readLine() throws IOException {
				throw new IOException();
			}
		};

		Configurator configurator = Configurator.defaultConfig();
		PropertiesLoader.readWriters(configurator, new PropertiesBuilder().set("tinylog.writer", "console").create());
		List<Writer> writers = configurator.create().getWriters();
		assertThat(writers, types(ConsoleWriter.class));
		assertEquals("LOGGER ERROR: Failed to read services from \"META-INF/services/org.cooder.tinylog.writers\" (" + IOException.class.getName() + ")",
				getErrorStream().nextLine());
		assertEquals("LOGGER ERROR: Cannot find a writer for the name \"console\"", getErrorStream().nextLine());
	}

	/**
	 * Test reading a registered writer but the class is missing.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterWithMissingClass() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), "a.b.c.MyWriter");

			Configurator configurator = Configurator.defaultConfig();
			PropertiesLoader.readWriters(configurator, new PropertiesBuilder().set("tinylog.writer", "mywriter").create());
			List<Writer> writers = configurator.create().getWriters();
			assertThat(writers, types(ConsoleWriter.class));
			assertEquals("LOGGER WARNING: Cannot find class \"a.b.c.MyWriter\"", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Cannot find a writer for the name \"mywriter\"", getErrorStream().nextLine());
		}
	}

	/**
	 * Test reading a registered labeler but the class is missing.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadLabelerWithMissingClass() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());
			mock.set("META-INF/services/" + Labeler.class.getPackage().getName(), "a.b.c.MyLabeler");

			Configurator configurator = Configurator.defaultConfig();
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.labeler", "mylabeler");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER WARNING: Cannot find class \"a.b.c.MyLabeler\"", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Cannot find a labeler for the name \"mylabeler\"", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());
		}
	}

	/**
	 * Test reading a registered policy but the class is missing.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadPolicyWithMissingClass() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());
			mock.set("META-INF/services/" + Policy.class.getPackage().getName(), "a.b.c.MyPolicy");

			Configurator configurator = Configurator.defaultConfig();
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.policy", "mypolicy");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER WARNING: Cannot find class \"a.b.c.MyPolicy\"", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Cannot find a policy for the name \"mypolicy\"", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());

			configurator = Configurator.defaultConfig();
			propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.policies", "mypolicy");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER WARNING: Cannot find class \"a.b.c.MyPolicy\"", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Cannot find a policy for the name \"mypolicy\"", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());
		}
	}

	/**
	 * Test potential exception while instantiation of writers.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadWriterIfInstantiationFailed() throws IOException {
		try (ClassLoaderMock classLoaderMock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			String writerClassName = EvilWriter.class.getName();
			classLoaderMock.set("META-INF/services/" + Writer.class.getPackage().getName(), writerClassName);

			Configurator configurator = Configurator.defaultConfig();
			PropertiesLoader.readWriters(configurator, new PropertiesBuilder().set("tinylog.writer", "evil").create());
			List<Writer> writers = configurator.create().getWriters();
			assertThat(writers, types(ConsoleWriter.class));
			assertEquals("LOGGER ERROR: Failed to create an instance of \"" + writerClassName + "\" (" + UnsupportedOperationException.class.getName() + ")",
					getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize evil writer", getErrorStream().nextLine());
	
			ExceptionThrowingMockUp<Constructor<?>> mock = new ExceptionThrowingMockUp<Constructor<?>>() {
				@Mock
				public Object newInstance(final Object... arguments) throws Exception {
					throw getException();
				}
			};

			for (Exception exception : Arrays.asList(new IllegalArgumentException(), new InstantiationException(), new IllegalAccessException())) {
				mock.setException(exception);
				configurator = Configurator.defaultConfig();
				PropertiesLoader.readWriters(configurator, new PropertiesBuilder().set("tinylog.writer", "evil").create());
				writers = configurator.create().getWriters();
				assertThat(writers, types(ConsoleWriter.class));
				assertEquals("LOGGER ERROR: Failed to create an instance of \"" + writerClassName + "\" (" + exception.getClass().getName() + ")",
						getErrorStream().nextLine());
				assertEquals("LOGGER ERROR: Failed to initialize evil writer", getErrorStream().nextLine());
			}
		}
	}

	/**
	 * Test potential exception while instantiation of labelers.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadLabelerIfInstantiationFailed() throws IOException {
		try (ClassLoaderMock classLoaderMock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			String labelerClassName = EvilLabeler.class.getName();
			classLoaderMock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());
			classLoaderMock.set("META-INF/services/" + Labeler.class.getPackage().getName(), labelerClassName);

			Configurator configurator = Configurator.defaultConfig();
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.labeler", "evil");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER ERROR: Failed to create an instance of \"" + labelerClassName + "\" (" + UnsupportedOperationException.class.getName() + ")",
					getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize evil labeler", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());

			configurator = Configurator.defaultConfig();
			propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.labeler", "evil: abc");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER WARNING: evil does not support parameters", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to create an instance of \"" + labelerClassName + "\" (" + UnsupportedOperationException.class.getName() + ")",
					getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize evil labeler", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());

			ExceptionThrowingMockUp<Class<?>> mock = new ExceptionThrowingMockUp<Class<?>>() {
				@Mock
				public Constructor<?> getDeclaredConstructor(final Class<?>... parameterTypes) throws Exception {
					throw getException();
				}
			};

			for (Exception exception : Arrays.asList(new InstantiationException(), new IllegalAccessException(), new IllegalArgumentException())) {
				mock.setException(exception);
				configurator = Configurator.defaultConfig();
				propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.labeler", "evil");
				PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
				assertEquals("LOGGER ERROR: Failed to create an instance of \"" + labelerClassName + "\" (" + exception.getClass().getName() + ")",
						getErrorStream().nextLine());
				assertEquals("LOGGER ERROR: Failed to initialize evil labeler", getErrorStream().nextLine());
				assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());
			}
		}
	}

	/**
	 * Test potential exception while instantiation of policies.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadPolicyIfInstantiationFailed() throws IOException {
		try (ClassLoaderMock classLoaderMock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			String policyClassName = EvilPolicy.class.getName();
			classLoaderMock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());
			classLoaderMock.set("META-INF/services/" + Policy.class.getPackage().getName(), policyClassName);

			Configurator configurator = Configurator.defaultConfig();
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.policy", "evil");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER ERROR: Failed to create an instance of \"" + policyClassName + "\" (" + UnsupportedOperationException.class.getName() + ")",
					getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize evil policy", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());

			configurator = Configurator.defaultConfig();
			propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.policy", "evil: abc");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER WARNING: evil does not support parameters", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to create an instance of \"" + policyClassName + "\" (" + UnsupportedOperationException.class.getName() + ")",
					getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize evil policy", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());

			ExceptionThrowingMockUp<Class<?>> mock = new ExceptionThrowingMockUp<Class<?>>() {
				@Mock
				public Constructor<?> getDeclaredConstructor(final Class<?>... parameterTypes) throws Exception {
					throw getException();
				}
			};

			for (Exception exception : Arrays.asList(new InstantiationException(), new IllegalAccessException(), new IllegalArgumentException())) {
				mock.setException(exception);
				configurator = Configurator.defaultConfig();
				propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.policy", "evil");
				PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
				assertEquals("LOGGER ERROR: Failed to create an instance of \"" + policyClassName + "\" (" + exception.getClass().getName() + ")",
						getErrorStream().nextLine());
				assertEquals("LOGGER ERROR: Failed to initialize evil policy", getErrorStream().nextLine());
				assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());
			}
		}
	}

	/**
	 * Test instantiation of a labeler without default constructor.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadLabelerWithoutDefaultConstructor() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			String labelerClassName = LabelerWithoutDefaultConstructor.class.getName();
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());
			mock.set("META-INF/services/" + Labeler.class.getPackage().getName(), labelerClassName);

			Configurator configurator = Configurator.defaultConfig();
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.labeler", "nodefault");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER ERROR: \"" + labelerClassName + "\" does not have a default constructor", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize nodefault labeler", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());
		}
	}

	/**
	 * Test instantiation of a policy without default constructor.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadPolicyWithoutDefaultConstructor() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			String policyClassName = PolicyWithoutDefaultConstructor.class.getName();
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());
			mock.set("META-INF/services/" + Policy.class.getPackage().getName(), policyClassName);

			Configurator configurator = Configurator.defaultConfig();
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.policy", "nodefault");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER ERROR: \"" + policyClassName + "\" does not have a default constructor", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize nodefault policy", getErrorStream().nextLine());
			assertEquals("LOGGER ERROR: Failed to initialize properties writer", getErrorStream().nextLine());
		}
	}

	/**
	 * Test instantiation a labeler that doesn't support parameters with a parameter.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadLabelerWithUnsupportedParameters() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());

			Configurator configurator = Configurator.defaultConfig();
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.labeler", "count: abc");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER WARNING: count does not support parameters", getErrorStream().nextLine());
		}
	}

	/**
	 * Test instantiation a policy that doesn't support parameters with a parameter.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testReadPolicyWithUnsupportedParameters() throws IOException {
		try (ClassLoaderMock mock = new ClassLoaderMock(PropertiesLoader.class.getClassLoader())) {
			mock.set("META-INF/services/" + Writer.class.getPackage().getName(), PropertiesWriter.class.getName());

			Configurator configurator = Configurator.defaultConfig();
			PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writer", "properties").set("tinylog.writer.policy", "startup: abc");
			PropertiesLoader.readWriters(configurator, propertiesBuilder.create());
			assertEquals("LOGGER WARNING: startup does not support parameters", getErrorStream().nextLine());
		}
	}

	/**
	 * Test reading writing thread.
	 */
	@Test
	public final void testReadWritingThread() {
		Configurator configurator = Configurator.defaultConfig();
		PropertiesBuilder propertiesBuilder = new PropertiesBuilder().set("tinylog.writingthread", "true");
		PropertiesLoader.readWritingThread(configurator, propertiesBuilder.create());
		Configuration configuration = configurator.create();
		assertNotNull(configuration.getWritingThread());
		assertEquals("main", configuration.getWritingThread().getNameOfThreadToObserve());
		assertThat(configuration.getWritingThread().getPriority(), lessThan(Thread.NORM_PRIORITY));

		configurator = Configurator.defaultConfig();
		propertiesBuilder = new PropertiesBuilder().set("tinylog.writingthread", "TRUE");
		PropertiesLoader.readWritingThread(configurator, propertiesBuilder.create());
		configuration = configurator.create();
		assertNotNull(configuration.getWritingThread());
		assertEquals("main", configuration.getWritingThread().getNameOfThreadToObserve());
		assertThat(configuration.getWritingThread().getPriority(), lessThan(Thread.NORM_PRIORITY));

		configurator = Configurator.defaultConfig();
		PropertiesLoader.readWritingThread(configurator, new PropertiesBuilder().set("tinylog.writingthread", "false").create());
		configuration = configurator.create();
		assertNull(configuration.getWritingThread());

		configurator = Configurator.defaultConfig();
		propertiesBuilder = new PropertiesBuilder().set("tinylog.writingthread", "true").set("tinylog.writingthread.priority", "1");
		PropertiesLoader.readWritingThread(configurator, propertiesBuilder.create());
		configuration = configurator.create();
		assertNotNull(configuration.getWritingThread());
		assertEquals("main", configuration.getWritingThread().getNameOfThreadToObserve());
		assertEquals(1, configuration.getWritingThread().getPriority());

		configurator = Configurator.defaultConfig();
		propertiesBuilder = new PropertiesBuilder().set("tinylog.writingthread", "true").set("tinylog.writingthread.priority", "9");
		PropertiesLoader.readWritingThread(configurator, propertiesBuilder.create());
		configuration = configurator.create();
		assertNotNull(configuration.getWritingThread());
		assertEquals("main", configuration.getWritingThread().getNameOfThreadToObserve());
		assertEquals(9, configuration.getWritingThread().getPriority());

		configurator = Configurator.defaultConfig();
		propertiesBuilder = new PropertiesBuilder().set("tinylog.writingthread", "true").set("tinylog.writingthread.priority", "invalid");
		PropertiesLoader.readWritingThread(configurator, propertiesBuilder.create());
		configuration = configurator.create();
		assertNotNull(configuration.getWritingThread());
		assertEquals("main", configuration.getWritingThread().getNameOfThreadToObserve());
		assertThat(configuration.getWritingThread().getPriority(), lessThan(Thread.NORM_PRIORITY));
		assertEquals("LOGGER WARNING: \"invalid\" is an invalid thread priority", getErrorStream().nextLine());

		configurator = Configurator.defaultConfig();
		propertiesBuilder = new PropertiesBuilder().set("tinylog.writingthread", "true").set("tinylog.writingthread.observe", "null");
		PropertiesLoader.readWritingThread(configurator, propertiesBuilder.create());
		configuration = configurator.create();
		assertNotNull(configuration.getWritingThread());
		assertNull(configuration.getWritingThread().getNameOfThreadToObserve());
		assertThat(configuration.getWritingThread().getPriority(), lessThan(Thread.NORM_PRIORITY));

		configurator = Configurator.defaultConfig();
		String threadName = Thread.currentThread().getName();
		propertiesBuilder = new PropertiesBuilder().set("tinylog.writingthread", "true").set("tinylog.writingthread.observe", threadName);
		PropertiesLoader.readWritingThread(configurator, propertiesBuilder.create());
		configuration = configurator.create();
		assertNotNull(configuration.getWritingThread());
		assertEquals(threadName, configuration.getWritingThread().getNameOfThreadToObserve());
		assertThat(configuration.getWritingThread().getPriority(), lessThan(Thread.NORM_PRIORITY));

		configurator = Configurator.defaultConfig();
		propertiesBuilder = new PropertiesBuilder().set("tinylog.writingthread", "true").set("tinylog.writingthread.observe", "null")
				.set("tinylog.writingthread.priority", "1");
		PropertiesLoader.readWritingThread(configurator, propertiesBuilder.create());
		configuration = configurator.create();
		assertNotNull(configuration.getWritingThread());
		assertNull(configuration.getWritingThread().getNameOfThreadToObserve());
		assertEquals(1, configuration.getWritingThread().getPriority());
	}

	@PropertiesSupport(name = "properties", properties = { @Property(name = "boolean", type = boolean.class, optional = true),
			@Property(name = "int", type = int.class, optional = true), @Property(name = "string", type = String.class, optional = true),
			@Property(name = "strings", type = String[].class, optional = true), @Property(name = "labeler", type = Labeler.class, optional = true),
			@Property(name = "policy", type = Policy.class, optional = true), @Property(name = "policies", type = Policy[].class, optional = true) })
	private static final class PropertiesWriter extends NullWriter {

		private final Boolean booleanValue;
		private final Integer intValue;
		private final String stringValue;
		private final String[] stringsValue;
		private final Labeler labeler;
		private final Policy policy;
		private final Policy[] policies;

		@SuppressWarnings("unused")
		public PropertiesWriter(final String stringValue, final String[] stringsValue, final Labeler labeler, final Policy policy, final Policy[] policies) {
			this.booleanValue = null;
			this.intValue = null;
			this.stringValue = stringValue;
			this.stringsValue = stringsValue;
			this.labeler = labeler;
			this.policy = policy;
			this.policies = policies;
		}

		@SuppressWarnings("unused")
		public PropertiesWriter(final int intValue, final String stringValue, final String[] stringsValue, final Labeler labeler, final Policy policy,
				final Policy[] policies) {
			this.booleanValue = null;
			this.intValue = intValue;
			this.stringValue = stringValue;
			this.stringsValue = stringsValue;
			this.labeler = labeler;
			this.policy = policy;
			this.policies = policies;
		}

		@SuppressWarnings("unused")
		public PropertiesWriter(final boolean booleanValue, final String stringValue, final String[] stringsValue, final Labeler labeler, final Policy policy,
				final Policy[] policies) {
			this.booleanValue = booleanValue;
			this.intValue = null;
			this.stringValue = stringValue;
			this.stringsValue = stringsValue;
			this.labeler = labeler;
			this.policy = policy;
			this.policies = policies;
		}

		@SuppressWarnings("unused")
		public PropertiesWriter(final boolean booleanValue, final int intValue, final String stringValue, final String[] stringsValue, final Labeler labeler,
				final Policy policy, final Policy[] policies) {
			this.booleanValue = booleanValue;
			this.intValue = intValue;
			this.stringValue = stringValue;
			this.stringsValue = stringsValue;
			this.labeler = labeler;
			this.policy = policy;
			this.policies = policies;
		}

	}

	@PropertiesSupport(name = "properties", properties = { @Property(name = "class", type = Class.class, optional = true) })
	private static final class ClassPropertyWriter extends NullWriter {

		@SuppressWarnings("unused")
		public ClassPropertyWriter(final Class<?> clazz) {
		}

	}

	@PropertiesSupport(name = "evil", properties = { })
	private static final class EvilWriter extends NullWriter {

		@SuppressWarnings("unused")
		public EvilWriter() throws Exception {
			throw new UnsupportedOperationException();
		}

	}

	@org.cooder.tinylog.labelers.PropertiesSupport(name = "evil")
	private static class EvilLabeler implements Labeler {

		public EvilLabeler() throws Exception {
			throw new UnsupportedOperationException();
		}

		@Override
		public void init(final Configuration configuration) {
			throw new UnsupportedOperationException();
		}

		@Override
		public File getLogFile(final File baseFile) {
			throw new UnsupportedOperationException();
		}

		@Override
		public File roll(final File file, final int maxBackups) {
			throw new UnsupportedOperationException();
		}

	}

	@org.cooder.tinylog.labelers.PropertiesSupport(name = "nodefault")
	private static final class LabelerWithoutDefaultConstructor extends EvilLabeler {

		@SuppressWarnings("unused")
		public LabelerWithoutDefaultConstructor(final boolean flag) throws Exception {
			super();
		}

	}

	@org.cooder.tinylog.policies.PropertiesSupport(name = "evil")
	private static class EvilPolicy implements Policy {

		public EvilPolicy() throws Exception {
			throw new UnsupportedOperationException();
		}

		@Override
		public void init(final Configuration configuration) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean check(final File logFile) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean check(final String logEntry) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void reset() {
			throw new UnsupportedOperationException();
		}

	}

	@org.cooder.tinylog.policies.PropertiesSupport(name = "nodefault")
	private static final class PolicyWithoutDefaultConstructor extends EvilPolicy {

		@SuppressWarnings("unused")
		public PolicyWithoutDefaultConstructor(final boolean flag) throws Exception {
			super();
		}

	}
	
	private abstract static class ExceptionThrowingMockUp<T> extends MockUp<T> {
		
		private Exception exception;
		
		public Exception getException() {
			return exception;
		}
		
		public void setException(final Exception exception) {
			this.exception = exception;
		}
		
	}

}
