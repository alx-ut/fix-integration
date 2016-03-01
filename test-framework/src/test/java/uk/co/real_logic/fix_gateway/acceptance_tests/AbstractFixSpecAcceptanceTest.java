package uk.co.real_logic.fix_gateway.acceptance_tests;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import uk.co.real_logic.aeron.driver.MediaDriver;
import uk.co.real_logic.fix_gateway.DebugLogger;
import uk.co.real_logic.fix_gateway.acceptance_tests.steps.TestStep;
import uk.co.real_logic.fix_gateway.decoder.Constants;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toList;
import static uk.co.real_logic.agrona.CloseHelper.quietClose;
import static uk.co.real_logic.fix_gateway.TestFixtures.launchMediaDriver;

public abstract class AbstractFixSpecAcceptanceTest
{
    private static final String FIX_TEST_TIMEOUT_PROP = "fix.test.timeout";
    private static final int FIX_TEST_TIMEOUT_DEFAULT = 25_000;

    protected static final String QUICKFIX_DEFINITIONS = "src/test/resources/quickfixj_definitions";
    protected static final String CUSTOM_ROOT_PATH = "src/test/resources/custom_definitions";

    @Rule
    public Timeout timeout = Timeout.millis(Long.getLong(FIX_TEST_TIMEOUT_PROP, FIX_TEST_TIMEOUT_DEFAULT));

    static
    {
        // Fake additional field in order to correctly test validation.
        Constants.ALL_FIELDS.add(55);
    }

    protected static List<Object[]> testsFor(
        final String rootPath, final List<String> files, final Supplier<Environment> environment)
    {
        return files.stream()
                    .map(file -> Paths.get(rootPath, file))
                    .map(path -> new Object[]{path, path.getFileName(), environment})
                    .collect(toList());
    }

    private final List<TestStep> steps;
    private final Environment environment;
    private final MediaDriver mediaDriver;

    public AbstractFixSpecAcceptanceTest(
        final Path path, final Path filename, final Supplier<Environment> environment)
    {
        steps = TestStep.load(path);
        mediaDriver = launchMediaDriver();
        this.environment = environment.get();
    }

    @Test
    public void shouldPassAcceptanceCriteria() throws Exception
    {
        steps.forEach(step ->
        {
            DebugLogger.log("Starting %s at %s\n", step, LocalTime.now());
            step.perform(environment);
        });
    }

    @After
    public void shutdown()
    {
        quietClose(environment);
        quietClose(mediaDriver);
    }

}
