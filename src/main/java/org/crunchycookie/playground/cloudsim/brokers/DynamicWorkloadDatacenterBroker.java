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
import org.crunchycookie.playground.cloudsim.models.ExecutionStatistics;
import org.crunchycookie.playground.cloudsim.models.Task;
import org.crunchycookie.playground.cloudsim.schedulers.ExternalyManagedCloudletSchedulerSpaceShared;

public class DynamicWorkloadDatacenterBroker extends DatacenterBroker {

  private final static int CLOUDLET_ID_BASE = 5000;
  private final static int CUSTOM_TAG_BASE = 55000;
  private final static int CUSTOM_TAG_HANDLE_NEXT_WORKLOAD = CUSTOM_TAG_BASE + 1;
  private final File workloadFile;
  private final ExecutionStatistics executionStatistics;
  private final Map<Integer, PriorityQueue<Pair<Instant, Cloudlet>>> vmTaskQueues = new HashMap<>();
  private int cloudletIdCount = CLOUDLET_ID_BASE;
  private List<Pair<Instant, Task>> tasksList;
  private long initialTasksCountOnList = 0;
  private long remainingTasks = 0;

  public DynamicWorkloadDatacenterBroker(String name, File workloadFile) throws Exception {
    super(name);
    this.workloadFile = workloadFile;
    this.executionStatistics = new ExecutionStatistics();
  }

  /**
   * This is the initial starting point of the broker. It is executed by the CloudSim framework once
   * simulation is started. In this extended broker, we are doing the followings.
   * <p>
   * 1. Read workload file, convert them into code-friendly task objects and persist within the
   * broker.
   * <p>
   * 2. Based on the tasks, derive the best virtual machines to create, and submit a list of
   * identified virtual machines to the broker.
   * <p>
   * 3. Initialize an empty queue to persist tasks for each VM. These queues automatically
   * prioratize tasks based on their submission time and most recent tasks gets the precedent.
   * <p>
   * 4. In the end, call super method to actually create submitted VMs in the datacenter.
   * <p>
   * <u>Next</u>: When VMs are created by the super method, {@link #submitCloudlets()} method is
   * invoked.
   */
  @Override
  public void startEntity() {

    // Read workload file and obtain all the tasks.
    Optional<List<Task>> tasks = getTasksList(workloadFile);
    if (tasks.isEmpty()) {
      throw new RuntimeException("Unable to get the tasks");
    }

    // Prepare tasks list.
    this.tasksList = getTasksAgainstSubmissionTime(tasks.get());
    updateTasksTrackers();

    // Derive the optimum VMs to create and submit them to the broker.
    List<EC2Vm> vmList = getOptimizedVmList(tasks);
    this.submitVmList(vmList);

    // Initialize priority queues for VMs.
    initVmTaskQueues(vmList);

    // Trigger creating Vms in the datacenter.
    super.startEntity();
  }

  /**
   * This method must be called after finishing the simulation. It provides an object containing
   * statistics for the completed simulation.
   *
   * @return Refer description.
   */
  public ExecutionStatistics getExecutionStatistics() {
    return executionStatistics;
  }

  /**
   * This method is called when parent broker created all the submitted Vms in the datacenter. Here
   * we are triggering a special custom event to indicate that tasks obtained from the workload file
   * can now be executed. This event is targetted for this broker itself.
   * <p>
   * <u>Next<u/>: Triggered event is captured by the parent broker and triggers
   * {@link #processOtherEvent(SimEvent)} method.
   */
  @Override
  protected void submitCloudlets() {

    // Persist execution details.
    addVmToHostMappingExecutionStats();

    // Trigger initial Cloudlet submission.
    sendNow(this.getId(), CUSTOM_TAG_HANDLE_NEXT_WORKLOAD);
  }

  /**
   * This method captures any custom event recieved to the broker. Once corresponding event is
   * captured, we trigger tasks submission by calling a specific method.
   * <p>
   * <u>Next</u>: Trigger {@link #handleWorkload(Object)} method to start tasks submission.
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
   * This method recieves a one or a set of tasks. This handles them by either submitting them to
   * the datacenter for execution, or persisting them in the Vm queues. After that, it analyses the
   * tasks list from the workload file and trigger {@link #handleWorkload(Object)} method again at
   * the next submission time. Therefore this method is responsible for iterating over tasks.
   *
   * @param workLoad
   */
  protected void handleWorkload(Object workLoad) {

    /*
    Current workload either submitted to the datacenter if vm has idle cores, or persisted in the
    vm queues.
     */
    handleTasks(workLoad);

    // Schedule next workload by referring to the tasks in the workload file.
    scheduleNextWorkload();
  }

  /**
   * This method is executed once a cloudlet completes its execution in the datacenter. It will,
   * <p>
   * - Finish simulations of no other tasks are pending, or
   * <p>
   * - Execute a pending task from the VM queue, since a cloudlet has just finished it's work.
   */
  @Override
  protected void processCloudletReturn(SimEvent ev) {

    Cloudlet cloudlet = (Cloudlet) ev.getData();

    // Collect finished cloudlet to the recieved cloudlets list.
    collectFinishedCloudlet(cloudlet);

    /*
    Since a cloudlet has been freed from the VM, pending tasks in the corresponding VM maybe
    executed.
     */
    handleWaitingTasksInVMQueue(cloudlet);

    // If all the tasks are handled, stop the simulation.
    finishSimulationIfAllPendingTasksCompleted();
  }

  private void handleTasks(Object workLoad) {
    if (workLoad != null) {
      for (Pair<Instant, Task> task : (List<Pair<Instant, Task>>) workLoad) {
        handleTask(task);
        remainingTasks = initialTasksCountOnList--;
      }
    }
  }

  private void collectFinishedCloudlet(Cloudlet cloudlet) {
    getCloudletReceivedList().add(cloudlet);
    cloudletsSubmitted--;
    Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId()
        + " received");
  }

  private void finishSimulationIfAllPendingTasksCompleted() {
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
        = (ExternalyManagedCloudletSchedulerSpaceShared) getSchedulerForCreatedVM(vmId);

    if (!queue.isEmpty() && isEnoughIdleCoresAreAvailable(queue, scheduler)) {
      submitCloudletToDatacenter(getCreatedVM(vmId), queue.remove().getValue());
    }
  }

  private Vm getCreatedVM(int vmId) {
    return getVmsCreatedList()
        .stream()
        .filter(v -> v.getId() == vmId)
        .findFirst()
        .get();
  }

  private CloudletScheduler getSchedulerForCreatedVM(int vmId) {
    return getCreatedVM(vmId).getCloudletScheduler();
  }

  private boolean isEnoughIdleCoresAreAvailable(PriorityQueue<Pair<Instant, Cloudlet>> queue,
      ExternalyManagedCloudletSchedulerSpaceShared scheduler) {
    return scheduler.getIdleCoresCount() >= queue.peek().getValue().getNumberOfPes();
  }

  private boolean isAllVMQueuesEmpty() {
    return vmTaskQueues.values().stream().allMatch(AbstractCollection::isEmpty);
  }

  private void addVmToHostMappingExecutionStats() {
    for (Vm vm : getVmsCreatedList()) {
      executionStatistics.setVmToHostMapping(vm.getId(), vm.getHost().getId());
    }
  }

  private void handleTask(Pair<Instant, Task> task) {

    // Optimum Vm should have enough memory and should have the highest idle core count.
    Optional<Vm> optimumVm = getVmsCreatedList()
        .stream()
        .max((vm1, vm2) -> {
          ExternalyManagedCloudletSchedulerSpaceShared scheduler1
              = (ExternalyManagedCloudletSchedulerSpaceShared) vm1.getCloudletScheduler();
          ExternalyManagedCloudletSchedulerSpaceShared scheduler2
              = (ExternalyManagedCloudletSchedulerSpaceShared) vm2.getCloudletScheduler();
          return Integer.compare(scheduler1.getIdleCoresCount(), scheduler2.getIdleCoresCount());
        });
    optimumVm.ifPresent(vm -> allocateTask(task, vm));
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
                  nextWorkload)).toSeconds(), CUSTOM_TAG_HANDLE_NEXT_WORKLOAD, nextWorkload);

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
    int vmId = id + 200;
    int mips = vmCharacteristics.getMIPS();
    int ram = vmCharacteristics.getMemoryInGB() * 1024; // vm memory (MB)
    int pesNumber = vmCharacteristics.getNumberOfECU(); // number of cpus

    executionStatistics.setVmToEC2Characteristics(vmId, vmCharacteristics);

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
