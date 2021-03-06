/*
 * Copyright 2013 Martin Winandy
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

import org.junit.Test;
import org.cooder.tinylog.EnvironmentHelper;
import org.cooder.tinylog.util.ConfigurationCreator;
import org.cooder.tinylog.util.FileHelper;

/**
 * Tests for process ID labeler.
 *
 * @see ProcessIdLabeler
 */
public class ProcessIdLabelerTest extends AbstractLabelerTest {

	/**
	 * Test if the labeler extract the process ID (pid).
	 */
	@Test
	public final void testProcessId() {
		assertEquals(EnvironmentHelper.getRuntimeDialect().getProcessId(), new ProcessIdLabeler().getProcessId());
	}

	/**
	 * Test labeling for log file with file extension.
	 *
	 * @throws IOException
	 *             Problem with the temporary file
	 */
	@Test
	public final void testLabelingWithFileExtension() throws IOException {
		File baseFile = FileHelper.createTemporaryFile("tmp");
		baseFile.delete();
		File realFile = getBackupFile(baseFile, "tmp", EnvironmentHelper.getRuntimeDialect().getProcessId());

		ProcessIdLabeler labeler = new ProcessIdLabeler();
		labeler.init(ConfigurationCreator.getDummyConfiguration());

		assertEquals(realFile, labeler.getLogFile(baseFile));

		assertEquals(realFile, labeler.roll(realFile, 0));

		realFile.createNewFile();
		assertEquals(realFile, labeler.roll(realFile, 0));
		assertFalse(realFile.exists());

		baseFile.delete();
	}

	/**
	 * Test labeling for log file without file extension.
	 *
	 * @throws IOException
	 *             Problem with the temporary file
	 */
	@Test
	public final void testLabelingWithoutFileExtension() throws IOException {
		File baseFile = FileHelper.createTemporaryFile(null);
		baseFile.delete();
		File realFile = getBackupFile(baseFile, null, EnvironmentHelper.getRuntimeDialect().getProcessId());

		ProcessIdLabeler labeler = new ProcessIdLabeler();
		labeler.init(ConfigurationCreator.getDummyConfiguration());

		assertEquals(realFile, labeler.getLogFile(baseFile));

		assertEquals(realFile, labeler.roll(realFile, 0));

		realFile.createNewFile();
		assertEquals(realFile, labeler.roll(realFile, 0));
		assertFalse(realFile.exists());

		baseFile.delete();
	}

	/**
	 * Test labeling without storing backups.
	 *
	 * @throws IOException
	 *             Problem with the temporary file
	 */
	@Test
	public final void testLabelingWithoutBackups() throws IOException {
		File baseFile = File.createTempFile("test", ".tmp");
		baseFile.delete();

		File targetFile = getBackupFile(baseFile, "tmp", EnvironmentHelper.getRuntimeDialect().getProcessId());
		targetFile.createNewFile();

		ProcessIdLabeler labeler = new ProcessIdLabeler();
		labeler.init(ConfigurationCreator.getDummyConfiguration());
		assertEquals(targetFile, labeler.getLogFile(baseFile));
		assertTrue(targetFile.exists());
		assertEquals(targetFile, labeler.roll(targetFile, 0));
		assertFalse(targetFile.exists());
	}

	/**
	 * Test if labeler deletes the right old files.
	 *
	 * @throws IOException
	 *             Problem with the temporary file
	 */
	@Test
	public final void testDeletingOldFiles() throws IOException {
		File baseFile = File.createTempFile("test", ".tmp");
		baseFile.delete();

		File backupFile1 = getBackupFile(baseFile, "tmp", "$OLD1$");
		backupFile1.createNewFile();
		backupFile1.setLastModified(1000L);
		File backupFile2 = getBackupFile(baseFile, "tmp", "$OLD2$");
		backupFile2.createNewFile();
		backupFile2.setLastModified(2000L);
		File backupFile3 = getBackupFile(baseFile, "tmp", "$OLD3$");
		backupFile3.createNewFile();
		backupFile3.setLastModified(0L);

		ProcessIdLabeler labeler = new ProcessIdLabeler();
		labeler.init(ConfigurationCreator.getDummyConfiguration());
		labeler.roll(labeler.getLogFile(baseFile), 1);

		assertFalse(backupFile1.exists());
		assertTrue(backupFile2.exists());
		assertFalse(backupFile3.exists());

		backupFile2.delete();
	}

	/**
	 * Test deleting if current file is in use.
	 *
	 * @throws IOException
	 *             Test failed
	 */
	@Test
	public final void testDeletingOfOpenLog() throws IOException {
		File baseFile = FileHelper.createTemporaryFile("tmp");

		ProcessIdLabeler labeler = new ProcessIdLabeler();
		labeler.init(ConfigurationCreator.getDummyConfiguration());
		File currentFile = labeler.getLogFile(baseFile);
		currentFile.createNewFile();
		FileInputStream stream = new FileInputStream(currentFile);

		labeler.roll(currentFile, 0); // Works or fails depending on OS

		if (getErrorStream().hasLines()) {
			assertEquals("LOGGER WARNING: Failed to delete \"" + currentFile + "\"", getErrorStream().nextLine());
		} else {
			assertFalse(currentFile.exists());
		}

		stream.close();
		currentFile.delete();
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

		File backupFile = getBackupFile(baseFile, "tmp", "$backup$");
		backupFile.createNewFile();
		FileInputStream stream = new FileInputStream(backupFile);

		ProcessIdLabeler labeler = new ProcessIdLabeler();
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

	/**
	 * Test reading process ID labeler from properties.
	 */
	@Test
	public final void testFromProperties() {
		Labeler labeler = createFromProperties("pid");
		assertThat(labeler, type(ProcessIdLabeler.class));
	}

}
