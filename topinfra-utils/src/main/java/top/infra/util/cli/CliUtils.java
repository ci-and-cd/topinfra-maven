package top.infra.util.cli;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

public abstract class CliUtils {

    private CliUtils() {
    }

    public static Map.Entry<Integer, String> exec(final String command) {
        try {
            final Process proc = Runtime.getRuntime().exec(command);
            return execResult(proc);
        } catch (final IOException ex) {
            return newTuple(-1, "");
        }
    }

    private static Map.Entry<Integer, String> execResult(final Process proc) {
        try {
            final String result = new BufferedReader(new InputStreamReader(proc.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));
            return newTuple(proc.waitFor(), result);
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            return newTuple(-1, "");
        }
    }

    public static Map.Entry<Integer, String> exec(
        final Map<String, String> environment,
        final String stdIn,
        final List<String> command
    ) {
        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (environment != null) {
            processBuilder.environment().putAll(environment);
        }
        try {
            final Process proc = processBuilder.start();
            if (stdIn != null) {
                try (final PrintWriter writer = new PrintWriter(proc.getOutputStream())) {
                    writer.println(stdIn);
                    writer.flush();
                }
            }
            return execResult(proc);
        } catch (final IOException ex) {
            return newTuple(-1, "");
        }
    }

    public static Map.Entry<Integer, String> exec(
        final Map<String, String> environment,
        final String stdIn,
        final Commandline cl
    ) {
        try {
            if (environment != null) {
                environment.forEach(cl::addEnvironment);
            }

            final CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
            final CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
            final int exitCode;
            if (stdIn == null || stdIn.isEmpty()) {
                exitCode = CommandLineUtils.executeCommandLine(cl, out, err);
            } else {
                exitCode = CommandLineUtils.executeCommandLine(cl, new ByteArrayInputStream(stdIn.getBytes(UTF_8)), out, err);
            }
            final String output;
            if (exitCode == 0) {
                output = out.getOutput();
            } else {
                output = err.getOutput();
            }
            return new AbstractMap.SimpleEntry<>(exitCode, output);
        } catch (final CommandLineException ex) {
            throw new RuntimeException("Error executing command line", ex);
        }
    }

    public static <F, S> Map.Entry<F, S> newTuple(final F first, final S second) {
        return new AbstractMap.SimpleImmutableEntry<>(first, second);
    }
}
