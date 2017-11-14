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

package org.cooder.tinylog.labelers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.cooder.tinylog.hamcrest.ClassMatchers.type;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import mockit.Mock;
import mockit.MockUp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.cooder.tinylog.mocks.SystemTimeMock;
import org.cooder.tinylog.util.ConfigurationCreator;
import org.cooder.tinylog.util.FileHelper;

/**
 * Tests for timestamp labeler.
 *
 * @see TimestampLabeler
 */
public class TimestampLabelerTest extends AbstractLabelerTest {

	private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH-mm-ss";

	private SystemTimeMock systemTimeMock;

	/**
	 * Set time zone to UTC and set up the mock for {@link System} (to control time).
	 */
	@Before
	public final void init() {
		systemTimeMock = new SystemTimeMock();
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	/**
	 * Tear down mock and reset time zone.
	 */
	@After
	public final void dispose() {
		TimeZone.setDefault(null);
	}

	/**
	 * Test labeling for log file with file extension.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testLabelingWithFileExtension() throws IOException {
		File baseFile = FileHelper.createTemporaryFile("tmp");
		baseFile.delete();

		systemTimeMock.setCurrentTimeMillis(0L);
		File targetFile1 = getBackupFile(baseFile, "tmp", formatCurrentTime());

		TimestampLabeler labeler = new TimestampLabeler(TIMESTAMP_FORMAT);
		labeler.init(ConfigurationCreator.getDummyConfiguration());
		assertEquals(targetFile1, labeler.getLogFile(baseFile));
		targetFile1.createNewFile();
		targetFile1.setLastModified(systemTimeMock.currentTimeMillis());

		systemTimeMock.setCurrentTimeMillis(1000L);
		File targetFile2 = getBackupFile(baseFile, "tmp", formatCurrentTime());

		assertEquals(targetFile2, labeler.roll(targetFile1, 2));
		targetFile2.createNewFile();
		targetFile2.setLastModified(systemTimeMock.currentTimeMillis());
		assertTrue(targetFile1.exists());
		assertTrue(targetFile2.exists());

		systemTimeMock.setCurrentTimeMillis(2000L);
		File targetFile3 = getBackupFile(baseFile, "tmp", formatCurrentTime());

		assertEquals(targetFile3, labeler.roll(targetFile2, 2));
		targetFile3.createNewFile();
		targetFile3.setLastModified(systemTimeMock.currentTimeMillis());
		assertTrue(targetFile1.exists());
		assertTrue(targetFile2.exists());
		assertTrue(targetFile3.exists());

		systemTimeMock.setCurrentTimeMillis(3000L);
		File targetFile4 = getBackupFile(baseFile, "tmp", formatCurrentTime());

		assertEquals(targetFile4, labeler.roll(targetFile3, 2));
		targetFile4.createNewFile();
		targetFile4.setLastModified(systemTimeMock.currentTimeMillis());
		assertFalse(targetFile1.exists());
		assertTrue(targetFile2.exists());
		assertTrue(targetFile3.exists());
		assertTrue(targetFile4.exists());

		baseFile.delete();
		targetFile1.delete();
		targetFile2.delete();
		targetFile3.delete();
		targetFile4.delete();
	}

	/**
	 * Test labeling for log file without file extension.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testLabelingWithoutFileExtension() throws IOException {
		File baseFile = FileHelper.createTemporaryFile(null);
		baseFile.delete();

		systemTimeMock.setCurrentTimeMillis(0L);
		File targetFile1 = getBackupFile(baseFile, null, formatCurrentTime());

		TimestampLabeler labeler = new TimestampLabeler(TIMESTAMP_FORMAT);
		labeler.init(ConfigurationCreator.getDummyConfiguration());
		assertEquals(targetFile1, labeler.getLogFile(baseFile));
		targetFile1.createNewFile();
		targetFile1.setLastModified(systemTimeMock.currentTimeMillis());

		systemTimeMock.setCurrentTimeMillis(1000L);
		File targetFile2 = getBackupFile(baseFile, null, formatCurrentTime());

		assertEquals(targetFile2, labeler.roll(targetFile1, 1));
		targetFile2.createNewFile();
		targetFile2.setLastModified(systemTimeMock.currentTimeMillis());
		assertTrue(targetFile1.exists());
		assertTrue(targetFile2.exists());

		systemTimeMock.setCurrentTimeMillis(2000L);
		File targetFile3 = getBackupFile(baseFile, null, formatCurrentTime());

		assertEquals(targetFile3, labeler.roll(targetFile2, 1));
		targetFile3.createNewFile();
		targetFile3.setLastModified(systemTimeMock.currentTimeMillis());
		assertFalse(targetFile1.exists());
		assertTrue(targetFile2.exists());
		assertTrue(targetFile3.exists());

		baseFile.delete();
		targetFile1.delete();
		targetFile2.delete();
		targetFile3.delete();
	}

	/**
	 * Test labeling without storing backups.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testLabelingWithoutBackups() throws IOException {
		File baseFile = File.createTempFile("test", ".tmp");
		baseFile.delete();

		systemTimeMock.setCurrentTimeMillis(0L);
		File targetFile1 = getBackupFile(baseFile, "tmp", formatCurrentTime());
		targetFile1.deleteOnExit();

		TimestampLabeler labeler = new TimestampLabeler();
		labeler.init(ConfigurationCreator.getDummyConfiguration());
		assertEquals(targetFile1, labeler.getLogFile(baseFile));
		targetFile1.createNewFile();
		targetFile1.setLastModified(systemTimeMock.currentTimeMillis());

		systemTimeMock.setCurrentTimeMillis(1000L);
		File targetFile2 = getBackupFile(baseFile, "tmp", formatCurrentTime());
		targetFile2.setLastModified(systemTimeMock.currentTimeMillis());

		assertTrue(targetFile1.exists());
		assertEquals(targetFile2, labeler.roll(targetFile1, 0));
		assertFalse(targetFile1.exists());

		baseFile.delete();
		targetFile1.delete();
		targetFile2.delete();
	}

	/**
	 * Test deleting if backup file is in use.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testDeletingOfOpenBackup() throws IOException {
		File baseFile = FileHelper.createTemporaryFile("tmp");

		File backupFile = getBackupFile(baseFile, "tmp", formatCurrentTime());
		backupFile.createNewFile();
		FileInputStream stream = new FileInputStream(backupFile);

		TimestampLabeler labeler = new TimestampLabeler();
		labeler.init(ConfigurationCreator.getDummyConfiguration());
		File currentFile = labeler.getLogFile(baseFile);

		labeler.roll(currentFile, 0); // Works or fails depending on OS
		
		if (getErrorStream().hasLines()) {
			assertEquals("LOGGER WARNING: Failed to delete \"" + backupFile + "\"", getErrorStream().nextLine());
		} else {
			assertFalse(currentFile.exists());
		}

		stream.close();
		backupFile.delete();
	}

	@Test
	public final void testPreciseDate() throws IOException {
		File baseFile = FileHelper.createTemporaryFile("tmp");

		new MockUp<Instant>() {

			@Mock
			public Instant now() {
				return LocalDate.of(1970, 01,01).atTime(01, 02, 03,123456789).toInstant(ZoneOffset.UTC);
			}

		};

		TimestampLabeler labeler = new TimestampLabeler("HH-mm-ss.SSSSSS");
		labeler.init(ConfigurationCreator.getDummyConfiguration());

		File expectedFile = getBackupFile(baseFile, "tmp", "01-02-03.123456");
		File currentFile = labeler.getLogFile(baseFile);

		assertEquals(expectedFile, currentFile);
	}

	/**
	 * Test reading timestamp labeler with default timestamp format from properties.
	 */
	@Test
	public final void testDefaultFromProperties() {
		Labeler labeler = createFromProperties("timestamp");
		assertThat(labeler, type(TimestampLabeler.class));
	}

	/**
	 * Test reading timestamp labeler with defined timestamp format from properties.
	 */
	@Test
	public final void testDefinedProperties() {
		Labeler labeler = createFromProperties("timestamp: yyyy");
		assertThat(labeler, type(TimestampLabeler.class));
		labeler.init(ConfigurationCreator.getDummyConfiguration());
		assertEquals(new File(MessageFormat.format("test.{0,date,yyyy}.log", new Date())).getAbsoluteFile(), labeler.getLogFile(new File("test.log")));
	}

	private String formatCurrentTime() {
		return new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.ROOT).format(new Date());
	}

}
