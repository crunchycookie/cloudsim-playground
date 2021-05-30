/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.crunchycookie.playground.cloudsim.models.Task;

public class FileOperationUtils {

  public static List<Task> getTaskListFromWorkloadFile(File workloadFile)
      throws FileNotFoundException {

    List<Task> tasks = new ArrayList<>();
    Scanner workloadFileScanner = new Scanner(workloadFile);
    while (workloadFileScanner.hasNextLine()) {
      String currentLine = workloadFileScanner.nextLine();
      String[] splitedCurrentLine = currentLine.trim().split(" ");

      Task currentTask = new Task();
      currentTask.setSubmissionTime(splitedCurrentLine[0]);
      currentTask.setMis(Integer.parseInt(splitedCurrentLine[1]));
      currentTask.setMinimumMemoryToExecute(Integer.parseInt(splitedCurrentLine[2]));
      currentTask.setMinimumStorageToExecute(Integer.parseInt(splitedCurrentLine[3]));
      currentTask.setWallClockTime(Integer.parseInt(splitedCurrentLine[4]));

      tasks.add(currentTask);
    }
    return tasks;
  }
}
