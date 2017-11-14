tinylog
=======
tinylog is forked from [pmwmedia/tinylog](https://github.com/pmwmedia/tinylog)

Example
-------

```java
import org.cooder.tinylog.Logger;

public class Application {

    public static void main(String[] args) {
        Logger.info("Hello World!");
    }

}
```

Projects
--------

* benchmark
  * Contains a benchmark for comparing logging frameworks
* tinylog-core
  * Contains shared basis for tinylog and compatible server replacements
* tinylog
  * Contains tinylog itself

All projects can be imported as Maven projects.

Other folders
-------------
	
* configuration
  * Contains configuration files for Java formatter, Checkstyle and FindBugs

Support
-------

A detailed user manual and the Javadoc documentation can be found on http://www.tinylog.org/. Bug reports and feature requests are welcome and can be created via [GitHub issues](https://github.com/pmwmedia/tinylog/issues).

Build tinylog
-------------

tinylog requires at least Maven 3.5 and JDK 9 for building. The generated JARs are compatible with Java 6 and higher.

Build command:

	mvn clean install -P release

A new folder "target" with Javadoc documentation and all JARs will be created in the root directory.

License
-------

Copyright 2012 Martin Winandy  
Copyright 2017 [yanxiyue](wuling@cooder.org)

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
