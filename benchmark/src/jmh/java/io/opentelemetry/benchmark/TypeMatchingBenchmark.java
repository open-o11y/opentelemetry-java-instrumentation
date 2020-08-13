/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.benchmark;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.SingleShotTime)
@Fork(5)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
@OutputTimeUnit(MILLISECONDS)
public class TypeMatchingBenchmark {

  private static final Set<String> classNames;

  static {
    classNames = new HashSet<>();
    String classPath = System.getProperty("java.class.path");
    for (String path : classPath.split(File.pathSeparator)) {
      if (!path.endsWith(".jar")) {
        continue;
      }
      try (JarFile jarFile = new JarFile(path)) {
        Enumeration<JarEntry> e = jarFile.entries();
        while (e.hasMoreElements()) {
          JarEntry jarEntry = e.nextElement();
          String name = jarEntry.getName();
          if (name.endsWith(".class")) {
            name = name.replace('/', '.');
            name = name.substring(0, name.length() - ".class".length());
            classNames.add(name);
          }
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Benchmark
  public void loadLotsOfClasses() throws ClassNotFoundException {
    for (String className : classNames) {
      try {
        Class.forName(className, false, TypeMatchingBenchmark.class.getClassLoader());
      } catch (NoClassDefFoundError e) {
        // many classes in the jar files have optional dependencies which are not present
      }
    }
  }

  @Fork(
      jvmArgsAppend =
          "-javaagent:/path/to/opentelemetry-java-instrumentation"
              + "/javaagent/build/libs/opentelemetry-javaagent.jar")
  public static class WithAgent extends TypeMatchingBenchmark {}
}
