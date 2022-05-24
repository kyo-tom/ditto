/*
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
package com.kyotom.ditto.client;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSet;
import com.kyotom.ditto.client.cli.ClientOptions;
import com.kyotom.ditto.client.cli.InputReader;
import com.kyotom.ditto.client.cli.ThreadInterruptor;
import com.kyotom.ditto.parser.Main;
import org.jline.reader.EndOfFileException;
import org.jline.reader.History;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.InfoCmp;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;


import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.StandardSystemProperty.USER_HOME;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.io.Files.asCharSource;
import static com.google.common.util.concurrent.Uninterruptibles.awaitUninterruptibly;
import static com.kyotom.ditto.client.StatementSplitter.isEmptyStatement;
import static com.kyotom.ditto.client.cli.Completion.commandCompleter;
import static com.kyotom.ditto.client.cli.Help.getHelpText;
import static com.kyotom.ditto.client.cli.TerminalUtils.closeTerminal;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.ENGLISH;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.jline.utils.AttributedStyle.CYAN;
import static org.jline.utils.AttributedStyle.DEFAULT;

@Command(
        name = "trino",
        header = "Trino command line interface",
        synopsisHeading = "%nUSAGE:%n%n",
        optionListHeading = "%nOPTIONS:%n",
        usageHelpAutoWidth = true)
public class Console
        implements Callable<Integer>
{

    private static final String PROMPT_NAME = "trino";
    public static final Set<String> STATEMENT_DELIMITERS = ImmutableSet.of(";", "\\G");

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
    public boolean usageHelpRequested;

    @Mixin
    public ClientOptions clientOptions;

    @Override
    public Integer call()
    {
        return run() ? 0 : 1;
    }

    public boolean run()
    {
        Main.init();;
        boolean hasQuery = false;
        String query = "";
        boolean isFromFile = !isNullOrEmpty(clientOptions.file);

        if (isFromFile) {
            try {
                query = asCharSource(new File(clientOptions.file), UTF_8).read();
                hasQuery = true;
            }
            catch (IOException e) {
                throw new RuntimeException(String.format("Error reading from file %s: %s", clientOptions.file, e.getMessage()));
            }
        }

        // abort any running query if the CLI is terminated
        AtomicBoolean exiting = new AtomicBoolean();
        ThreadInterruptor interruptor = new ThreadInterruptor();
        CountDownLatch exited = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            exiting.set(true);
            interruptor.interrupt();
            awaitUninterruptibly(exited, 3000, MILLISECONDS);
            // Terminal closing restores terminal settings and releases underlying system resources
            closeTerminal();
        }));
        if (hasQuery) {
            return executeCommand(
                    exiting,
                    query,
                    false);
        }

        runConsole(exiting);
        return true;
    }

    private static void runConsole(AtomicBoolean exiting)
    {
        try (InputReader reader = new InputReader(getHistoryFile(), commandCompleter())) {
            String remaining = "";
            while (!exiting.get()) {
                // setup prompt
                String prompt = PROMPT_NAME;
                String commandPrompt = prompt + "> ";

                // read a line of input from user
                String line;
                try {
                    line = reader.readLine(commandPrompt, remaining);
                }
                catch (UserInterruptException e) {
                    if (!e.getPartialLine().isEmpty()) {
                        reader.getHistory().add(e.getPartialLine());
                    }
                    remaining = "";
                    continue;
                }
                catch (EndOfFileException e) {
                    System.out.println();
                    return;
                }

                // check for special commands -- must match InputParser
                String command = CharMatcher.is(';').or(whitespace()).trimTrailingFrom(line);
                switch (command.toLowerCase(ENGLISH)) {
                    case "exit":
                    case "quit":
                        return;
                    case "clear":
                        Terminal terminal = reader.getTerminal();
                        terminal.puts(InfoCmp.Capability.clear_screen);
                        terminal.flush();
                        continue;
                    case "history":
                        for (History.Entry entry : reader.getHistory()) {
                            System.out.println(new AttributedStringBuilder()
                                    .style(DEFAULT.foreground(CYAN))
                                    .append(format("%5d", entry.index() + 1))
                                    .style(DEFAULT)
                                    .append("  ")
                                    .append(entry.line())
                                    .toAnsi(reader.getTerminal()));
                        }
                        continue;
                    case "help":
                        System.out.println();
                        System.out.println(getHelpText());
                        continue;
                }

                // execute any complete statements
                StatementSplitter splitter = new StatementSplitter(line, STATEMENT_DELIMITERS);
                for (StatementSplitter.Statement split : splitter.getCompleteStatements()) {
                    process(split.statement(), System.out, System.out);
                }

                // replace remaining with trailing partial statement
                remaining = whitespace().trimTrailingFrom(splitter.getPartialStatement());
            }
        }
        catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    private static boolean process(
            String sql,
            PrintStream out,
            PrintStream errorChannel)
    {
        String finalSql;
        try {
            finalSql = Main.transformSingle(sql);
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(out, UTF_8), 16384);
            bufferedWriter.write(finalSql);
            bufferedWriter.write('\n');
            bufferedWriter.flush();
            return true;
        }
        catch (Exception e) {
            errorChannel.println(e.getMessage());
            return false;
        }
    }

    private static boolean executeCommand(
            AtomicBoolean exiting,
            String query,
            boolean ignoreErrors)
    {
        boolean success = true;
        StatementSplitter splitter = new StatementSplitter(query);
        for (StatementSplitter.Statement split : splitter.getCompleteStatements()) {
            if (!isEmptyStatement(split.statement())) {
                if (!process(split.statement(), System.out, System.err)) {
                    if (!ignoreErrors) {
                        return false;
                    }
                    success = false;
                }
            }
            if (exiting.get()) {
                return success;
            }
        }
        if (!isEmptyStatement(splitter.getPartialStatement())) {
            System.err.println("Non-terminated statement: " + splitter.getPartialStatement());
            return false;
        }
        return success;
    }

    private static Path getHistoryFile()
    {
        String path = System.getenv("TRINO_HISTORY_FILE");
        if (!isNullOrEmpty(path)) {
            return Paths.get(path);
        }
        return Paths.get(nullToEmpty(USER_HOME.value()), ".trino_history");
    }
}
