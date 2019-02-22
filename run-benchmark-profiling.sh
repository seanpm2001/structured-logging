#!/bin/bash
#
# Copyright 2019 Jordan Zimmerman
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

echo compiling...
mvn -q -P benchmark clean compile
echo packaging...
mvn -q -P benchmark package
echo running benchmark...
java -Djmh.jfr.saveTo=structured-logger-benchmark/target -jar structured-logger-benchmark/target/benchmarks.jar -prof profilers.FlightRecordingProfiler -jvmArgsAppend "-XX:+UnlockCommercialFeatures" "$@"
