/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.scenarios;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.crunchycookie.playground.cloudsim.models.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestDynamicWorkloadSubmissionScenario {

  @Test
  public void testDynamicWorkloadWithRealtimeData() throws IOException {

    // Create workload file.
    Instant currentTime = Instant.now();
    File workloadFile = new File("src/test/resources/workload-file.txt");
    FileUtils.writeLines(workloadFile, List.of(
        getTaskString(currentTime, 10000, 2000, 1000, 2),
        getTaskString(currentTime, 15000, 1500, 1500, 4),
        getTaskString(currentTime, 22000, 3200, 3400, 3)
    ));

    Assertions.assertTrue(DynamicWorkloadSubmissionScenario.start(workloadFile));
  }

  private String getTaskString(Instant currentTime, int mis, int minMemory, int minStorage,
      int wallclockTime) {
    Task task = new Task();
    task.setSubmissionTime(currentTime.toString());
    task.setMis(mis);
    task.setMinimumMemoryToExecute(minMemory);
    task.setMinimumStorageToExecute(minStorage);
    task.setWallClockTime((int) TimeUnit.MINUTES.toMillis(wallclockTime));
    return task.toString();
  }
}
