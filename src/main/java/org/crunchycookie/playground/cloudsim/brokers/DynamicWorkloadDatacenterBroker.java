/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.brokers;

import static org.crunchycookie.playground.cloudsim.constants.EC2Constants.EC2_INSTANCE_TYPES;
import static org.crunchycookie.playground.cloudsim.utils.FileOperationUtils.getTaskListFromWorkloadFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.crunchycookie.playground.cloudsim.models.EC2InstanceCharacteristics;
import org.crunchycookie.playground.cloudsim.models.EC2VMCandidate;
import org.crunchycookie.playground.cloudsim.models.Task;
import org.crunchycookie.playground.cloudsim.scenarios.DynamicWorkloadSubmission;

public class DynamicWorkloadDatacenterBroker extends DatacenterBroker {

  private final static String WORKLOAD_FILE_NAME = "workload-file.txt";

  private final static int CUSTOM_TAG_BASE = 55000;
  private final static int CUSTOM_TAG_HANDLE_NEXT_WORKLOAD = CUSTOM_TAG_BASE + 1;

  private List<Pair<Instant, Cloudlet>> tasksList;
  private final ThreadLocal<Boolean> INITIAL_CLOUDLET_SUBMISSION_TO_AVOID = new ThreadLocal<>();

  public DynamicWorkloadDatacenterBroker(String name) throws Exception {
    super(name);
  }

  @Override
  protected void processVmCreate(SimEvent ev) {

    // Upon calling the super method, a cloudlet submission is triggered but we need avoid it as the
    // Cloudlet submission is handled separately for the dynamic workload.
    INITIAL_CLOUDLET_SUBMISSION_TO_AVOID.set(true);
    try {
      super.processVmCreate(ev);
    } finally {
      INITIAL_CLOUDLET_SUBMISSION_TO_AVOID.set(false);
    }
  }

  /**
   * When VMs are created and ready to accept Cloudlets
   */
  @Override
  protected void submitCloudlets() {
    if (INITIAL_CLOUDLET_SUBMISSION_TO_AVOID.get()) {
      return;
    }

    super.submitCloudlets();
  }

  protected void handleWorkload(Object workLoad) {

    // Handle the workload. This should be a List containing Pair<Instant, Cloudlet>.
    if (!(workLoad instanceof List)) {
      Log.printLine("Error: Workload is not compatible");
      return;
    }

    // Let's trust that list only includes Cloudlets, and submit cloudlets.
    submitCloudletList((List<? extends Cloudlet>) workLoad);

    // Schedule next workload by referring to the workload tasks.
    scheduleNextWorkload();
  }

  private void scheduleNextWorkload() {

    Instant currentSimulationTime = CloudSim.getSimulationCalendar().toInstant();
    tasksList.stream()
        .filter(i -> i.getKey().isAfter(currentSimulationTime))
        .findFirst()
        .ifPresent(nextTask -> {
              // Get all cloudlets scheduled for the same submission time.
              List<Pair<Instant, Cloudlet>> nextWorkload = getAllTasksScheduledAtTheSameTime(nextTask);

              // Schedule identified workload to it's submission time.
              send(this.getId(), Duration.between(currentSimulationTime, getNextWorkloadStartTime(
                  nextWorkload)).toMillis(), CUSTOM_TAG_HANDLE_NEXT_WORKLOAD, nextWorkload.stream()
                  .map(Pair::getValue));

              // Remove already scheduled workload from the list.
              tasksList.removeIf(i -> i.getKey().equals(getNextWorkloadStartTime(nextWorkload)));
            }
        );
  }

  private Instant getNextWorkloadStartTime(List<Pair<Instant, Cloudlet>> nextWorkload) {
    return nextWorkload.get(0).getKey();
  }

  private List<Pair<Instant, Cloudlet>> getAllTasksScheduledAtTheSameTime(
      Pair<Instant, Cloudlet> immediatelyExecutableCloudlet) {

    return tasksList.stream()
        .filter(i -> i.getKey().equals(immediatelyExecutableCloudlet.getKey()))
        .collect(Collectors.toList());
  }

  /**
   * Handle custom events.
   *
   * @param ev
   */
  @Override
  protected void processOtherEvent(SimEvent ev) {

    if (ev.getTag() == CUSTOM_TAG_HANDLE_NEXT_WORKLOAD) {
      handleWorkload(ev.getData());
    } else {
      super.processOtherEvent(ev);
    }
  }

  /**
   * Read workload tasks and determine VMs to create in a way that cost is minimized. This allows
   * dynamically changing the workload file content between each simulations.
   */
  @Override
  public void startEntity() {

    // Read workload file and obtain all the tasks.
    Log.printLine("Reading workload tasks from " + WORKLOAD_FILE_NAME + "...");
    Optional<List<Task>> tasks = getTasksList();
    if (tasks.isEmpty()) {
      Log.printLine("Could not find the workload file thus cannot start the broker.");
      throw new RuntimeException("Unable to get the tasks");
    }

    // Convert tasks to Cloudlets and persist within the broker.
    Log.printLine("Deriving cloudlets...");
    this.tasksList = getCloudletsAgainstSortedSubmissionTime(tasks);

    // Trigger initial cloudlet submission. Cloudlet
    sendNow(this.getId(), CUSTOM_TAG_HANDLE_NEXT_WORKLOAD);

    // Derive the optimum list of VMs to create, and submit them to the broker.
    Log.printLine("Evaluating tasks and deriving the optimum VM list...");
    List<Vm> vmList = getOptimizedVmList(tasks);
    this.submitVmList(vmList);

    // Start the broker.
    super.startEntity();
  }

  private List<Pair<Instant, Cloudlet>> getCloudletsAgainstSortedSubmissionTime(
      Optional<List<Task>> tasks) {

    List<Pair<Instant, Cloudlet>> cloudletsWithSubmissionTime = new ArrayList<>();
    for (int id = 0; id < tasks.get().size(); id++) {
      Task task = tasks.get().get(id);

      // Cloudlet properties
      int cloudletId = id;
      long length = task.getMis();
      long fileSize = 300;
      long outputSize = 300;
      UtilizationModel utilizationModel = new UtilizationModelFull();

      Cloudlet cloudlet = new Cloudlet(cloudletId, length, 1, fileSize, outputSize,
          utilizationModel, utilizationModel, utilizationModel);

      // Set broker ID.
      cloudlet.setUserId(this.getId());

      // This will be orchestrated by the broker at a later stage.
      // cloudlet.setVmId(0);

      cloudletsWithSubmissionTime
          .add(new Pair<>(Instant.parse(task.getSubmissionTime()), cloudlet));
    }

    // Sort cloudlets according to the submission time ascending order.
    cloudletsWithSubmissionTime.sort(Comparator.comparing(Pair::getKey));

    return cloudletsWithSubmissionTime;
  }

  private List<Vm> getOptimizedVmList(Optional<List<Task>> tasks) {

    // Analyze the tasks list and derive number and types of VMs required.
    List<EC2VMCandidate> vmCandidateList = new ArrayList<>();
    for (Task task : tasks.get()) {
      ITERATING_OVER_TASKS:
      // First, analyze whether any of the already available VMs are capable of handling the task.
      for (EC2VMCandidate vmCandidate : vmCandidateList) {
        if (canVMHandleThisTask(task, vmCandidate)) {
          assignTaskToVMCandidate(task, vmCandidate);
          break ITERATING_OVER_TASKS;
        }
      }

      // If not, select a suitable EC2 candidate to execute the task.
      Optional<EC2InstanceCharacteristics> ec2InstanceCandidate = getEC2InstanceCandidate(task);
      if (ec2InstanceCandidate.isEmpty()) {
        Log.printLine("Skipping the task submitted at  " + task.getSubmissionTime() + ", because "
            + "none of the available EC2 VMs cannot execute this task");
        break;
      }
      EC2VMCandidate vmCandidate = new EC2VMCandidate(ec2InstanceCandidate.get().getId());
      assignTaskToVMCandidate(task, vmCandidate);
      vmCandidateList.add(vmCandidate);
    }

    // Convert vm candidates to CloudSim VMs and set them in the broker.
    List<Vm> vmList = new ArrayList<>();
    for (int id = 0; id < vmCandidateList.size(); id++) {
      EC2InstanceCharacteristics vmCharacteristics = EC2_INSTANCE_TYPES.get(vmCandidateList.get(id)
          .getType());
      vmList.add(getVm(id, vmCharacteristics));
    }

    return vmList;
  }

  private Vm getVm(int id, EC2InstanceCharacteristics vmCharacteristics) {

    // VM description.
    int vmId = id;
    int mips = vmCharacteristics.getMIPS();
    long size = 10000; // image size (MB)
    int ram = vmCharacteristics.getMemoryInGB() * 1024; // vm memory (MB)
    long bw = 1000;
    int pesNumber = vmCharacteristics.getNumberOfECU(); // number of cpus
    String vmm = "Xen"; // VMM name

    // Create VM.
    return new Vm(vmId, this.getId(), mips, pesNumber, ram, bw, size, vmm,
        new CloudletSchedulerSpaceShared());
  }

  private Optional<EC2InstanceCharacteristics> getEC2InstanceCandidate(Task task) {
    /*
    If an already selected VM is not available to handle this task, we need to find a suitable
    EC2 instance capable of handling the task.

    To do that,

    Iterate through instances in a cost ascending way, and grab the first instance which is able
    to meet the deadline of the task and also the memory requirements.

    Assumption: Each task will be occupied by a single ECU in space shared way.
     */
    Optional<EC2InstanceCharacteristics> ec2VMType = EC2_INSTANCE_TYPES.values().stream()
        .sorted((vmTypeA, vmTypeB) -> ((Double) (vmTypeA.getHourlyRateInUSD()))
            .compareTo(vmTypeB.getHourlyRateInUSD()))
        .filter(vmType -> (task.getMis() / vmType.getMIPS() <= task.getWallClockTime())
            && (task.getMinimumMemoryToExecute() <= (vmType.getMemoryInGB() * 1024)))
        .findFirst();

    return ec2VMType;
  }

  private Optional<List<Task>> getTasksList() {

    File workloadFile = new File(DynamicWorkloadSubmission.class.getClassLoader().getResource(
        WORKLOAD_FILE_NAME).getFile());

    List<Task> tasks = null;
    try {
      tasks = getTaskListFromWorkloadFile(workloadFile);
    } catch (FileNotFoundException e) {
      Log.printLine("Could not find the workload file!");
    }

    return Optional.ofNullable(tasks);
  }

  private void assignTaskToVMCandidate(Task task, EC2VMCandidate vmCandidate) {

    vmCandidate.setAvailableCores(vmCandidate.getAvailableCores() - 1);
    vmCandidate.setAvailableMemoryInMB(vmCandidate.getAvailableMemoryInMB() - task
        .getMinimumMemoryToExecute());
  }

  private boolean canVMHandleThisTask(Task task, EC2VMCandidate vmCandidate) {

    return vmCandidate.getAvailableCores() > 0
        && vmCandidate.getAvailableMemoryInMB() > task.getMinimumMemoryToExecute()
        && (task.getMis() / EC2_INSTANCE_TYPES.get(vmCandidate.getType()).getMIPS()
        <= task.getWallClockTime());
  }
}
