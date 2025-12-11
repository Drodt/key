/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.key_project.rusty.control.KeYEnvironment;
import org.key_project.rusty.proof.io.ProblemLoaderException;
import org.key_project.rusty.proof.io.ProofSaver;

import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

public class CLI {
    @Option(names = { "-V", "--version" }, versionHelp = true, description = "display version info")
    boolean versionInfoRequested;

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @Parameters(paramLabel = "FILE", description = "the file to prove")
    File file;

    @Option(names = { "--output", "-o" }, description = "where to write the proof to")
    File outputFile;

    @Option(names = "--prove", negatable = true, defaultValue = "true", fallbackValue = "true",
        description = "whether to attempt to prove the file")
    boolean prove;

    @Option(names = "--replay", negatable = true, defaultValue = "true", fallbackValue = "true",
        description = "whether to replay the loaded proof")
    boolean replay;

    @Option(names = { "--verbose", "-v" },
        description = "whether to print additional information; implies `--print-stats`")
    boolean verbose;

    @Option(names = { "--print-stats", "-s" })
    boolean printStats;

    public static void main(String[] args) {
        CLI cli = new CLI();
        CommandLine cmd = new CommandLine(cli);
        cmd.parseArgs(args);
        if (cmd.isUsageHelpRequested()) {
            cmd.usage(System.out);
            return;
        } else if (cmd.isVersionHelpRequested()) {
            cmd.printVersionHelp(System.out);
            return;
        }
        if (cli.verbose)
            cli.printStats = true;
        boolean success = run(cli);
        System.out.flush();
        System.err.flush();
        System.exit(success ? 0 : 1);
    }

    private static boolean run(CLI cli) {
        try {
            if (cli.verbose)
                System.out.println("Loading...");
            File f = cli.file.getAbsoluteFile();
            var env = KeYEnvironment.load(f);
            var loadedProof = env.getLoadedProof();
            if (loadedProof.closed()) {
                if (cli.prove) {
                    System.err.println(
                        "Error: The loaded file already contains a proof.\nUse `--no-prove` to only load the proof.");
                    return false;
                }
                if (cli.replay) {
                    if (cli.verbose)
                        System.out.println("Replaying proof...");
                    var replayResult = env.getReplayResult();
                    if (replayResult.hasErrors()) {
                        System.err.println("Error(s) while loading!");
                        if (cli.verbose) {
                            List<Throwable> errors = replayResult.getErrorList();
                            for (int i = 0; i < errors.size(); i++) {
                                Throwable error = errors.get(i);
                                System.err.println("Error " + (i + 1) + ": " + error);
                            }
                        }
                        return false;
                    }
                    System.out.println("Loading and proof replay successful");
                    return true;
                }
                System.out.println("Loading successful");
                return true;
            } else {
                if (cli.prove) {
                    System.out.println("Proving...");
                    env.getProofControl().startAndWaitForAutoMode(loadedProof);
                    if (cli.printStats) {
                        System.out.println(loadedProof.getStatistics());
                    }
                    if (cli.outputFile != null) {
                        try {
                            ProofSaver.saveToFile(cli.outputFile.getAbsoluteFile(), loadedProof);
                        } catch (IOException e) {
                            System.err.println("Error saving proof to file: " + e.getMessage());
                            return false;
                        }
                    }
                    if (!loadedProof.closed()) {
                        System.err.println("Proof not closed. " + loadedProof.openGoals().size()
                            + " goals remaining");
                        return false;
                    } else {
                        System.out.println("Loading and proof successful");
                        return true;
                    }
                } else {
                    System.out.println("Loading successful");
                    return true;
                }
            }
        } catch (ProblemLoaderException e) {
            System.err.println("Error while loading: " + e.getMessage());
            if (cli.verbose) {
                System.err.println(e);
                System.err.println(Arrays.toString(e.getStackTrace()));
            }
            return false;
        }
    }
}
