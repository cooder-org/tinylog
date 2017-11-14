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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Properties;

import org.cooder.tinylog.AbstractTinylogTest;
import org.cooder.tinylog.Configuration;
import org.cooder.tinylog.Configurator;
import org.cooder.tinylog.util.ConfigurationCreator;

/**
 * Basis class for all writer tests.
 * 
 * @see Writer
 */
public abstract class AbstractWriterTest extends AbstractTinylogTest {

	/**
	 * Create a writer from properties.
	 * 
	 * @param properties
	 *            Properties with writer definition
	 * @return Created writer
	 */
	protected final List<Writer> createFromProperties(final Properties properties) {
		try {
			Configurator configurator = ConfigurationCreator.getDummyConfigurator();

			Class<?> propertiesLoaderClass = Class.forName("org.cooder.tinylog.PropertiesLoader");
			Method readWriterMethod = propertiesLoaderClass.getDeclaredMethod("readWriters", Configurator.class, Properties.class);
			readWriterMethod.setAccessible(true);
			readWriterMethod.invoke(null, configurator, properties);

			Method createMethod = Configurator.class.getDeclaredMethod("create");
			createMethod.setAccessible(true);
			Configuration configuration = (Configuration) createMethod.invoke(configurator);
			return configuration.getWriters();
		} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
			throw new RuntimeException(ex);
		}
	}

}
