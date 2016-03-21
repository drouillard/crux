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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

public class DefaultVagrantClient implements VagrantClient {
    
    static public class Builder extends VagrantClient.Builder<Builder> {
        
        @Override
        public VagrantClient build() throws VagrantException {
            return new DefaultVagrantClient(this.workingDirectory);
        }
        
    }
    
    private final Path workingDirectory;

    protected DefaultVagrantClient(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public Path workingDirectory() {
        return this.workingDirectory;
    }
    
    @Override
    public boolean areAllMachinesRunning() throws VagrantException {
        Map<String,MachineStatus> status = this.status();
        
        // at least 1 machine needs to be defined
        if (status.isEmpty()) {
            return false;
        }
        
        for (MachineStatus vs : status.values()) {
            if (!vs.isRunning()) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public boolean areAnyMachinesRunning() throws VagrantException {
        Map<String,MachineStatus> status = this.status();
        
        for (MachineStatus vs : status.values()) {
            if (vs.isRunning()) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public Set<String> machinesRunning() throws VagrantException {
        return this.status().values().stream()
            .filter((status) -> status.isRunning())
            .map((status) -> status.getName())
            .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
    }
    
    @Override
    public Map<String,MachineStatus> status() throws VagrantException {
        try {
            ProcessResult result
                = new ProcessExecutor()
                    .command("vagrant", "status", "--machine-readable")
                    .readOutput(true)
                    .exitValueNormal()
                    .execute();

            List<String> lines = VagrantUtil.parseLines(result);

            return VagrantUtil.parseStatus(lines);
        } catch (InvalidExitValueException e) {
            throw new VagrantException(e.getMessage());
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new VagrantException(e.getMessage(), e);
        }
    }
    
    @Override
    public Path sshConfig(String... machineNames) throws VagrantException {
        try {
            File tempFile = File.createTempFile("vagrant.", ".ssh-config");
            tempFile.deleteOnExit();
            Path sshConfigFile = tempFile.toPath();
            sshConfigWrite(sshConfigFile, machineNames);
            return sshConfigFile;
        } catch (IOException e) {
            throw new VagrantException(e.getMessage(), e);
        }
    }
    
    @Override
    public void sshConfigWrite(Path sshConfigFile, String... machineNames) throws VagrantException {
        try {
            // build command so we can append machine names if necessary
            List<String> commands = new ArrayList<>();
            commands.add("vagrant");
            commands.add("ssh-config");
            
            if (machineNames != null) {
                commands.addAll(Arrays.asList(machineNames));
            }
            
            ProcessResult result
                = new ProcessExecutor()
                    .command(commands)
                    .readOutput(true)
                    .exitValueNormal()
                    .execute();
            
            // save .ssh-config
            Files.write(sshConfigFile,
                        result.output(),
                        StandardOpenOption.TRUNCATE_EXISTING);
            
            // fix ssh-config for windows
            // remove UserKnownHostsFile line
            // identity file probably wrong too
            List<String> filteredConfig
                = Files.lines(sshConfigFile)
                    .filter((line) -> !line.contains("UserKnownHostsFile"))
                    .map((line) -> (!line.contains("IdentityFile") ? line : line.replace("\"", "")))
                    .collect(Collectors.toList());
        
            Files.write(sshConfigFile,
                        filteredConfig,
                        StandardOpenOption.TRUNCATE_EXISTING);
        } catch (InvalidExitValueException e) {
            throw new VagrantException(e.getMessage());
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new VagrantException(e.getMessage(), e);
        }
    }
    
}
