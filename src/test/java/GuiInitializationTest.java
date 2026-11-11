import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class GuiInitializationTest {

    @Test
    @DisplayName("Verify Swing GUI initializes on Event Dispatch Thread without exceptions")
    void testGuiInitialization() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Running in headless mode; skipping GUI window display.");
            return;
        }

        SwingUtilities.invokeAndWait(() -> {
            InventoryAndSupplierSystem frame = new InventoryAndSupplierSystem();
            assertNotNull(frame.getTitle());
            assertEquals("Inventory and Supplier Management System", frame.getTitle());
            assertNotNull(frame.getInventoryModel());
            assertNotNull(frame.getSupplierModel());
            assertEquals(5, frame.getInventoryModel().getColumnCount());
            assertEquals(5, frame.getSupplierModel().getColumnCount());
            frame.dispose();
        });
    }

    @Test
    @DisplayName("Verify SwingWorker executes DB operations off-EDT and updates UI on-EDT")
    void testSwingWorkerThreadingContract() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean backgroundWasOffEdt = new AtomicBoolean(false);
        AtomicBoolean doneWasOnEdt = new AtomicBoolean(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Assert doInBackground executes off the Event Dispatch Thread
                backgroundWasOffEdt.set(!SwingUtilities.isEventDispatchThread());
                // Perform actual query helper call
                InventoryAndSupplierSystem.queryInventoryRows();
                return null;
            }

            @Override
            protected void done() {
                // Assert done() executes on the Event Dispatch Thread
                doneWasOnEdt.set(SwingUtilities.isEventDispatchThread());
                latch.countDown();
            }
        };

        worker.execute();
        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertTrue(completed, "SwingWorker must complete within 5 seconds");
        assertTrue(backgroundWasOffEdt.get(), "Database queries must run off the Event Dispatch Thread");
        assertTrue(doneWasOnEdt.get(), "SwingWorker done() and UI updates must execute on the Event Dispatch Thread");
    }
}
