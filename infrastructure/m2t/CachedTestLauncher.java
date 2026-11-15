import java.io.PrintWriter;
import java.nio.file.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** Solo ejecuta JUnit con dependencias disponibles; no sustituye la compilacion Maven del POM. */
public final class CachedTestLauncher {
    public static void main(String[] args) throws Exception {
        var request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(args[0])).build();
        var summary = new SummaryGeneratingListener();
        var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(summary);launcher.execute(request);
        summary.getSummary().printTo(new PrintWriter(System.out,true));
        summary.getSummary().printFailuresTo(new PrintWriter(System.out,true));
        try (var report = new PrintWriter(Files.newBufferedWriter(Path.of(args[1])))) {
            summary.getSummary().printTo(report);summary.getSummary().printFailuresTo(report);
        }
        if (summary.getSummary().getTestsFoundCount() != 12 || summary.getSummary().getTestsFailedCount() != 0 || summary.getSummary().getTestsAbortedCount() != 0) throw new IllegalStateException("La suite de 12 pruebas no termino correctamente");
    }
}
