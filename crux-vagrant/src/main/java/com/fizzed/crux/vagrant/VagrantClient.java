/*
 * Copyright 2016 Fizzed, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fizzed.crux.vagrant;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public interface VagrantClient {

    boolean areAllMachinesRunning() throws VagrantException;

    boolean areAllMachinesRunning(boolean refresh) throws VagrantException;

    boolean areAnyMachinesRunning() throws VagrantException;

    boolean areAnyMachinesRunning(boolean refresh) throws VagrantException;
    
    Set<String> fetchMachinesRunning() throws VagrantException;
    
    Set<String> fetchMachinesRunning(boolean refresh) throws VagrantException;

    Path fetchSshConfig() throws VagrantException;

    Path fetchSshConfig(boolean refresh) throws VagrantException;

    void fetchSshConfig(Path sshConfigFile) throws VagrantException;

    Map<String,VagrantStatus> fetchStatus() throws VagrantException;

    Map<String,VagrantStatus> fetchStatus(boolean refresh) throws VagrantException;

    Path workingDirectory();
    
    static public class Builder {
        
        private Path workingDirectory;

        public Builder() {
        }

        public Builder workingDirectory(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
            return this;
        }
        
        public VagrantClient build() {
            return new DefaultVagrantClient(workingDirectory);
        }
        
        /**
         * Builds a client then attempts to fetch both the status and ssh-config
         * but if those fail then returns an empty client.  Helpful for use
         * as a static instance in junit tests, etc.
         * @return Either a load client or an empty one
         */
        public VagrantClient safeLoad() {
            try {
                VagrantClient client = build();
                client.fetchStatus();
                client.fetchSshConfig();
                return client;
            } catch (Exception e) {
                return new EmptyVagrantClient(workingDirectory);
            }
        }
    }
    
}