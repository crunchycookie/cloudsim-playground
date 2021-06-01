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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
        getTaskString(currentTime.plus(4, ChronoUnit.SECONDS), 10000, 2000,
            1000, 2),
        getTaskString(currentTime.plus(6, ChronoUnit.SECONDS), 15000, 1500,
            1500, 4),
        getTaskString(currentTime.plus(8, ChronoUnit.SECONDS), 22000, 3200,
            3400, 3)
    ));

    Assertions.assertTrue(DynamicWorkloadSubmissionScenario.start(workloadFile));
  }

  @Test
  public void testDynamicWorkloadWithHeavyLoad() throws IOException {

    List<String> heavyLoad = getHeavyLoad();
    File workloadFile = new File("src/test/resources/workload-file.txt");
    FileUtils.writeLines(workloadFile, heavyLoad);

    Assertions.assertTrue(DynamicWorkloadSubmissionScenario.start(workloadFile));
  }

  private List<String> getHeavyLoad() {
    Instant currentTime = Instant.now();
    List<String> heavyLoad = new ArrayList<>();
    Random random = new Random();
    int numberOfTasks = 1000;
    for (int i = 0; i < numberOfTasks; i++) {
      heavyLoad.add(getTaskString(
          currentTime.plus(5 + 2 * i, ChronoUnit.SECONDS),
          random.longs(1000000000L, 10000000000L).findFirst().getAsLong(),
          random.ints(800, 15000).findFirst().getAsInt(),
          random.ints(1500, 5000).findFirst().getAsInt(),
          random.ints(100, 200).findFirst().getAsInt()
      ));
    }
    return heavyLoad;
  }

  private String getTaskString(Instant currentTime, long mis, int minMemory, int minStorage,
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
