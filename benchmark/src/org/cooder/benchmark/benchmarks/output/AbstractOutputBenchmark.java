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

package org.pmw.benchmark.benchmarks.output;

import org.pmw.benchmark.benchmarks.AbstractBenchmark;
import org.pmw.benchmark.frameworks.Framework;

public abstract class AbstractOutputBenchmark extends AbstractBenchmark {

	protected AbstractOutputBenchmark(final Framework framework, final boolean locationInformation, final int depth) {
		super(framework, locationInformation, depth);
	}

	@Override
	public boolean isValidLogEntry(final String line) {
		if (super.isValidLogEntry(line) && (!locationInformation || line.contains("write()"))) {
			return line.contains("Trace") ^ line.contains("Debug") ^ line.contains("Info") ^ line.contains("Warning") ^ line.contains("Error");
		} else {
			return false;
		}
	}

	protected final void write(final int stackTraceDepth, final long iterations) throws Exception {
		if (stackTraceDepth <= 0) {
			for (long i = 0; i < iterations; ++i) {
				framework.write(i + 1);
			}
		} else {
			write(stackTraceDepth - 1, iterations);
		}
	}

}
