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

package org.pmw.benchmark.frameworks;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.cooder.tinylog.Configurator;
import org.cooder.tinylog.Level;
import org.cooder.tinylog.Logger;
import org.cooder.tinylog.writers.FileWriter;

/**
 * Test tinylog.
 */
public final class Tinylog implements Framework {

	private static final String NAME = "tinylog";
	private static final String NAME_ASYNC = NAME + " with writing thread";

	private final boolean location;
	private final boolean async;
	private FileWriter writer;

	public Tinylog(final boolean location, final boolean async) {
		this.location = location;
		this.async = async;
	}

	@Override
	public String getName() {
		return async ? NAME_ASYNC : NAME;
	}

	@Override
	public void init(final File file) {
		Configurator configurator = Configurator.defaultConfig();

		writer = new FileWriter(file.getAbsolutePath(), async);

		configurator.writer(writer);
		configurator.level(Level.INFO);

		if (location) {
			configurator.formatPattern("{date:yyyy-MM-dd HH:mm:ss} [{thread}] {class}.{method}(): {message}");
		} else {
			configurator.formatPattern("{date:yyyy-MM-dd HH:mm:ss}: {message}");
		}

		if (async) {
			configurator.writingThread(null);
		}

		configurator.activate();
	}

	@Override
	public void write(final long number) {
		Logger.trace("Trace: {}", number);
		Logger.debug("Debug: {}", number);
		Logger.info("Info: {}", number);
		Logger.warn("Warning: {}", number);
		Logger.error("Error: {}", number);
	}

	@Override
	public boolean calculate(final List<Long> primes, final long number) {
		for (Long prime : primes) {
			if (number % prime == 0L) {
				Logger.trace("{} is not prime", number);
				return false;
			}
		}
		Logger.info("{} is prime", number);
		return true;
	}

	@Override
	public void dispose() throws IOException {
		if (async) {
			Configurator.shutdownWritingThread(true);
		}

		writer.close();
	}

}
