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
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.crunchycookie.playground.cloudsim.models.EC2InstanceCharacteristics;
import org.crunchycookie.playground.cloudsim.models.EC2VMCandidate;
import org.crunchycookie.playground.cloudsim.models.EC2Vm;
import org.crunchycookie.playground.cloudsim.models.Task;
import org.crunchycookie.playground.cloudsim.schedulers.ExternalyManagedCloudletSchedulerSpaceShared;

public class DynamicWorkloadDatacenterBroker extends DatacenterBroker {

  private final static int CLOUDLET_ID_BASE = 5000;
  private int cloudletIdCount = CLOUDLET_ID_BASE;

  private final static int CUSTOM_TAG_BASE = 55000;
  private final static int CUSTOM_TAG_HANDLE_NEXT_WORKLOAD = CUSTOM_TAG_BASE + 1;

  private File workloadFile;
  private List<Pair<Instant, Task>> tasksList;
  private long initialTasksCountOnList = 0;
  private long remainingTasks = 0;

  // Each new VM must own a priority queue to hold the excess tasks allocated to itself.
  private Map<Integer, PriorityQueue<Pair<Instant, Cloudlet>>> vmTaskQueues = new HashMap<>();

  public DynamicWorkloadDatacenterBroker(String name, File workloadFile) throws Exception {
    super(name);
    this.workloadFile = workloadFile;
  }

  /**
   * When cloudlet is returned, vm core become available. Now we need to check and allocate any
   * waiting tasks in the vm queue.
   *
   * @param ev
   */
  @Override
  protected void processCloudletReturn(SimEvent ev) {
    Cloudlet cloudlet = (Cloudlet) ev.getData();

    /*
    Since a cloudlet has been freed from the VM, pending tasks in the corresponding VM maybe
    executed.
     */
    handleWaitingTasksInVMQueue(cloudlet);

    getCloudletReceivedList().add(cloudlet);
    Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId()
        + " received");
    cloudletsSubmitted--;
    if (remainingTasks == 0 && isAllVMQueuesEmpty() && cloudletsSubmitted == 0) {
      // All cloudlets executed.
      Log.printLine(CloudSim.clock() + ": " + getName() + ": All Cloudlets executed. Finishing...");
      clearDatacenters();
      finishExecution();
    }
  }

  private void handleWaitingTasksInVMQueue(Cloudlet cloudlet) {
    int vmId = cloudlet.getVmId();
    PriorityQueue<Pair<Instant, Cloudlet>> queue = vmTaskQueues.get(vmId);
    ExternalyManagedCloudletSchedulerSpaceShared scheduler
        = (ExternalyManagedCloudletSchedulerSpaceShared) getVmsCreatedList().get(vmId)
        .getCloudletScheduler();
    if (!queue.isEmpty() && isEnoughIdleCoresAreAvailable(queue, scheduler)) {
      submitCloudletToDatacenter(getVmsCreatedList().get(vmId), queue.remove().getValue());
    }
  }

  private boolean isEnoughIdleCoresAreAvailable(PriorityQueue<Pair<Instant, Cloudlet>> queue,
      ExternalyManagedCloudletSchedulerSpaceShared scheduler) {
    return scheduler.getIdleCoresCount() >= queue.peek().getValue().getNumberOfPes();
  }

  private boolean isAllVMQueuesEmpty() {
    return vmTaskQueues.values().stream().allMatch(AbstractCollection::isEmpty);
  }

  /**
   * When VMs are created and ready to accept Cloudlets
   */
  @Override
  protected void submitCloudlets() {

    // Vms are created and ready. Trigger initial cloudlet submission.
    sendNow(this.getId(), CUSTOM_TAG_HANDLE_NEXT_WORKLOAD);
  }

  /**
   * This method is called when VMs are created and ready to execute tasks.
   *
   * @param workLoad
   */
  protected void handleWorkload(Object workLoad) {

    /*
    Current workload either submitted to the datacenter if vm has idle cores, or persisted in the
    vm queues.
     */
    if (workLoad != null) {
      for (Pair<Instant, Task> task : (List<Pair<Instant, Task>>) workLoad) {
        handleTask(task);
        remainingTasks = initialTasksCountOnList--;
      }
    }

    // Schedule next workload by referring to the workload tasks.
    scheduleNextWorkload();
  }

  private void handleTask(Pair<Instant, Task> task) {
    for (Vm vm : getVmsCreatedList()) {
      if (isVMMemoryEnoughToRun(task, vm)) {
        allocateTask(task, vm);
        break;
      }
    }
  }

  private void allocateTask(Pair<Instant, Task> task, Vm vm) {
    CloudletScheduler cloudletScheduler = vm.getCloudletScheduler();
    if (cloudletScheduler instanceof ExternalyManagedCloudletSchedulerSpaceShared) {
      int idleCoresCount = ((ExternalyManagedCloudletSchedulerSpaceShared) cloudletScheduler)
          .getIdleCoresCount();
      Cloudlet cloudlet = getCloudlet(task, vm);
      if (idleCoresCount < 1) {
        addTaskToVMQueue(task, vm, cloudlet);
      } else {
        submitCloudletToDatacenter(vm, cloudlet);
      }
    }
  }

  private void submitCloudletToDatacenter(Vm vm, Cloudlet cloudlet) {
    sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
    cloudletsSubmitted++;
  }

  private Cloudlet getCloudlet(Pair<Instant, Task> task, Vm vm) {
    Cloudlet cloudlet = task.getValue().getCloudletForTheTargetVm(cloudletIdCount++, vm);
    cloudlet.setUserId(this.getId());
    cloudlet.setVmId(vm.getId());
    return cloudlet;
  }

  private void addTaskToVMQueue(Pair<Instant, Task> task, Vm vm, Cloudlet cloudlet) {
    vmTaskQueues.get(vm.getId()).add(new Pair<>(
        task.getKey(),
        cloudlet
    ));
  }

  private boolean isVMMemoryEnoughToRun(Pair<Instant, Task> task, Vm vm) {
    return task.getValue().getMinimumMemoryToExecute() <= vm.getRam();
  }

  private void scheduleNextWorkload() {

    Instant currentSimulationTime = CloudSim.getSimulationCalendar().toInstant();
    tasksList.stream()
        .filter(i -> i.getKey().isAfter(currentSimulationTime))
        .findFirst()
        .ifPresent(nextTask -> {
              // Get all cloudlets scheduled for the same submission time.
              List<Pair<Instant, Task>> nextWorkload = getAllTasksScheduledAtTheSameTime(nextTask);

              // Schedule identified workload to it's submission time.
              send(this.getId(), Duration.between(currentSimulationTime, getNextWorkloadStartTime(
                  nextWorkload)).toMillis(), CUSTOM_TAG_HANDLE_NEXT_WORKLOAD, nextWorkload);

              // Remove already scheduled workload from the list.
              tasksList.removeIf(i -> i.getKey().equals(getNextWorkloadStartTime(nextWorkload)));
            }
        );
  }

  private Instant getNextWorkloadStartTime(List<Pair<Instant, Task>> nextWorkload) {
    return nextWorkload.get(0).getKey();
  }

  private List<Pair<Instant, Task>> getAllTasksScheduledAtTheSameTime(
      Pair<Instant, Task> immediateTask) {

    return tasksList.stream()
        .filter(i -> i.getKey().equals(immediateTask.getKey()))
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
    Optional<List<Task>> tasks = getTasksList(workloadFile);
    if (tasks.isEmpty()) {
      Log.printLine("Could not find the workload file thus cannot start the broker.");
      throw new RuntimeException("Unable to get the tasks");
    }

    // Prepare tasks list.
    this.tasksList = getTasksAgainstSubmissionTime(tasks.get());
    updateTasksTrackers();

    // Derive the optimum list of VMs to create, and submit them to the broker.
    Log.printLine("Evaluating tasks and deriving the optimum VM list...");
    List<EC2Vm> vmList = getOptimizedVmList(tasks);
    initVmTaskQueues(vmList);

    this.submitVmList(vmList);

    // Start the broker.
    super.startEntity();
  }

  private void updateTasksTrackers() {
    initialTasksCountOnList = this.tasksList.size();
    remainingTasks = initialTasksCountOnList;
  }

  private void initVmTaskQueues(List<EC2Vm> vmList) {

    for (EC2Vm vm : vmList) {
      vmTaskQueues.put(vm.getId(), new PriorityQueue<>((taskA, taskB) -> {
        if (taskA.getKey().equals(taskB.getKey())) {
          return 0;
        }
        return taskA.getKey().isBefore(taskB.getKey()) ? 1 : -1;
      }));
    }
  }

  private List<Pair<Instant, Task>> getTasksAgainstSubmissionTime(List<Task> tasks) {

    return tasks.stream()
        .map(t -> new Pair<>(Instant.parse(t.getSubmissionTime()), t))
        .sorted(Comparator.comparing(Pair::getKey))
        .collect(Collectors.toList());
  }

  private List<EC2Vm> getOptimizedVmList(Optional<List<Task>> tasks) {

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
    List<EC2Vm> vmList = new ArrayList<>();
    for (int id = 0; id < vmCandidateList.size(); id++) {
      EC2InstanceCharacteristics vmCharacteristics = EC2_INSTANCE_TYPES.get(vmCandidateList.get(id)
          .getType());
      vmList.add(getVm(id, vmCharacteristics));
    }

    return vmList;
  }

  private EC2Vm getVm(int id, EC2InstanceCharacteristics vmCharacteristics) {

    // VM description.
    int vmId = id;
    int mips = vmCharacteristics.getMIPS();
    int ram = vmCharacteristics.getMemoryInGB() * 1024; // vm memory (MB)
    int pesNumber = vmCharacteristics.getNumberOfECU(); // number of cpus

    // Create VM.
    return new EC2Vm(vmId, this.getId(), mips, pesNumber, ram,
        vmCharacteristics.getHourlyRateInUSD(), new ExternalyManagedCloudletSchedulerSpaceShared());
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

  private Optional<List<Task>> getTasksList(File workloadFile) {

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
