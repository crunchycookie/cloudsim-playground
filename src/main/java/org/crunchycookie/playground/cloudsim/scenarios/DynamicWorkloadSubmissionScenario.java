/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.scenarios;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.crunchycookie.playground.cloudsim.brokers.DynamicWorkloadDatacenterBroker;
import org.crunchycookie.playground.cloudsim.brokers.DynamicWorkloadDatacenterBroker.VmOptimizingMethod;
import org.crunchycookie.playground.cloudsim.builders.DatacenterBuilder;
import org.crunchycookie.playground.cloudsim.builders.DatacenterCharacteristicsBuilder;
import org.crunchycookie.playground.cloudsim.builders.HostBuilder;
import org.crunchycookie.playground.cloudsim.models.ExecutionStatistics;

public class DynamicWorkloadSubmissionScenario {

  private final static int NUMBER_OF_HOSTS = 500;

  private final static int NUMBER_OF_CORES_PER_HOST = 8;

  private final static int NUMBER_OF_MIPS_PER_HOST = 10000;

  private final static int AMOUNT_OF_RAM_PER_HOST_IN_GB = 64;

  private final static int AMOUNT_OF_STORAGE_PER_HOST_IN_TB = 10;

  private final static String WORKLOAD_FILE_NAME = "workload-file.txt";

  /**
   * Creates main() to run this example.
   *
   * @param args the args
   */
  public static void main(String[] args) {

    File workloadFile = getWorkloadFile();
    start(workloadFile, VmOptimizingMethod.VM_COSTS_FOCUSED, "simulation-results.txt");
  }

  private static File getWorkloadFile() {
    File workloadFile = new File(
        DynamicWorkloadSubmissionScenario.class.getClassLoader().getResource(
            WORKLOAD_FILE_NAME).getFile());
    return workloadFile;
  }

  public static boolean start(File workloadFile, Enum<VmOptimizingMethod> vmOptimizingMethod,
      String simulationResultsFileName) {
    // Number of users that are going to use the cloud.
    int numberOfUsers = 1;

    Calendar calender = Calendar.getInstance();

    // Initialize the CloudSim framework.
    CloudSim.init(numberOfUsers, calender, false);

    // Create the hosts.
    List<Host> hosts = getHosts();

    // Create the datacenter characteristics.
    DatacenterCharacteristics datacenterCharacteristics = getDatacenterCharacteristics(hosts);

    // Create the datacenter.
    Datacenter datacenter;
    try {
      datacenter = getDatacenter(hosts, datacenterCharacteristics);
      printStatusSuccessfulMessage(datacenter);
    } catch (Exception e) {
      System.out.println("Failed to create the datacenter");
      e.printStackTrace();
      return false;
    }

    // Create broker.
    DatacenterBroker broker = getDatacenterBroker(workloadFile, vmOptimizingMethod);

    // Start the simulation.
    CloudSim.startSimulation();

    CloudSim.stopSimulation();

    // Print results.
    printResults(broker, simulationResultsFileName);

    return true;
  }

  private static void printResults(DatacenterBroker broker, String simulationResultsFileName) {

    String outputFilePath = "src/test/resources/" + simulationResultsFileName;

    File simulationResultsFile =  new File(outputFilePath);
    if (simulationResultsFile.exists()) {
      simulationResultsFile.delete();
    }

    OutputStream existingOStream = Log.getOutput();
    try (FileOutputStream fout = new FileOutputStream(outputFilePath)) {
      Log.setOutput(fout);
      // Final step: Print results when simulation is over.
      List<Cloudlet> cloudletReceivedList = broker.getCloudletReceivedList();
      ExecutionStatistics executionStats = ((DynamicWorkloadDatacenterBroker) broker)
          .getExecutionStatistics();

//      printCloudletList(cloudletReceivedList);

      Map<Integer, Double> hostToExecutionTime = getHostToExecutionTime(cloudletReceivedList,
          executionStats);
      printHostToExecutionTime(hostToExecutionTime);

      Map<Integer, Double> vmToExecutionTime = getVmToExecutionTime(cloudletReceivedList);
      printVmToExecutionTime(vmToExecutionTime);

      Double totalCostInUSD = getTotalCostInUSD(executionStats, vmToExecutionTime);
      printTotalCost(totalCostInUSD);

      Double totalExecutionTime = getTotalExecutionTime(cloudletReceivedList);
      printTotalExecutionTime(totalExecutionTime);
    } catch (Exception e) {
      System.out.println("Failed to writing results to the file");
    } finally {
      Log.setOutput(existingOStream);
    }
  }

  private static Double getTotalCostInUSD(ExecutionStatistics executionStats,
      Map<Integer, Double> vmToExecutionTime) {
    Double totalCostInUSD = 0D;
    for (Entry<Integer, Double> vmExec : vmToExecutionTime.entrySet()) {
      totalCostInUSD += executionStats.getVmToEC2Characteristics().get(vmExec.getKey())
          .getHourlyRateInUSD() * (vmExec.getValue() / 3600);
    }
    return totalCostInUSD;
  }

  private static double getTotalExecutionTime(List<Cloudlet> cloudletReceivedList) {
    Double initialCloudletExecTime = cloudletReceivedList
        .stream()
        .min((c1, c2) -> Double.valueOf(c1.getExecStartTime()).compareTo(c2.getExecStartTime()))
        .get().getExecStartTime();
    Double finalCloudletExecTime = cloudletReceivedList
        .stream()
        .max((c1, c2) -> Double.valueOf(c1.getFinishTime()).compareTo(c2.getFinishTime()))
        .get().getExecStartTime();
    double totalExecutionTime = finalCloudletExecTime - initialCloudletExecTime;
    return totalExecutionTime;
  }

  private static Map<Integer, Double> getHostToExecutionTime(List<Cloudlet> cloudletReceivedList,
      ExecutionStatistics executionStats) {
    Map<Integer, Double> hostToExecutionTime = new HashMap<>();
    for (Cloudlet cloudlet : cloudletReceivedList) {
      Integer hostId = executionStats.getMappedHost(cloudlet.getVmId());
      hostToExecutionTime
          .put(hostId, getUpdatedExecutionTime(hostToExecutionTime, cloudlet, hostId));
    }
    return hostToExecutionTime;
  }

  private static Map<Integer, Double> getVmToExecutionTime(List<Cloudlet> cloudletReceivedList) {
    Map<Integer, Double> vmToExecutionTime = new HashMap<>();
    for (Cloudlet c : cloudletReceivedList) {
      Double updatedExecutionTime = vmToExecutionTime.get(c.getVmId()) != null ? vmToExecutionTime
          .get(c.getVmId()) + c.getActualCPUTime() : c.getActualCPUTime();
      vmToExecutionTime.put(c.getVmId(), updatedExecutionTime);
    }
    return vmToExecutionTime;
  }

  private static double getUpdatedExecutionTime(Map<Integer, Double> hostToExecutionTime,
      Cloudlet cloudlet,
      Integer hostId) {
    return hostToExecutionTime.get(hostId) != null ? hostToExecutionTime.get(hostId) + cloudlet
        .getActualCPUTime() : cloudlet.getActualCPUTime();
  }

  /**
   * Prints the Cloudlet objects.
   *
   * @param list list of Cloudlets
   */
  private static void printCloudletList(List<Cloudlet> list) {
    int size = list.size();
    Cloudlet cloudlet;

    String indent = "    ";
    Log.printLine();
    Log.printLine("========== OUTPUT ==========");
    Log.printLine("Cloudlet ID" + indent + "STATUS" + indent
        + "Data center ID" + indent + "VM ID" + indent + "Time" + indent
        + "Start Time" + indent + "Finish Time");

    DecimalFormat dft = new DecimalFormat("###.##");

    for (int i = 0; i < size; i++) {
      cloudlet = list.get(i);
      Log.print(indent + cloudlet.getCloudletId() + indent + indent);

      if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
        Log.print("SUCCESS");

        Log.printLine(indent + indent + cloudlet.getResourceId()
            + indent + indent + indent + cloudlet.getVmId()
            + indent + indent
            + dft.format(cloudlet.getActualCPUTime()) + indent
            + indent + dft.format(cloudlet.getExecStartTime())
            + indent + indent
            + dft.format(cloudlet.getFinishTime()));
      }
    }
  }

  private static void printHostToExecutionTime(Map<Integer, Double> hostToExecutionTime) {

    String indent = "    ";
    Log.printLine();
    Log.printLine("========== Host to execution time ==========");
    Log.printLine("Host ID" + indent + "Execution Time");

    for (Entry<Integer, Double> mapping : hostToExecutionTime.entrySet().stream().sorted(
        Comparator.comparing(Entry::getKey)
    ).collect(Collectors.toList())) {
      Log.printLine(indent + indent + mapping.getKey()
          + indent + indent + indent + mapping.getValue());
    }
  }

  private static void printTotalExecutionTime(Double totalExecTime) {

    String indent = "    ";
    Log.printLine();
    Log.printLine("========== Total execution time ==========");
    Log.printLine("Total Execution Time = " + totalExecTime);
  }

  private static void printTotalCost(Double cost) {

    String indent = "    ";
    Log.printLine();
    Log.printLine("========== Total Cost in USD ==========");
    Log.printLine("Total Cost = " + cost);
  }

  private static void printVmToExecutionTime(Map<Integer, Double> vmToExecutionTime) {

    String indent = "    ";
    Log.printLine();
    Log.printLine("========== VM to execution time ==========");
    Log.printLine("Vm ID" + indent + "Execution Time");

    for (Entry<Integer, Double> mapping : vmToExecutionTime.entrySet().stream().sorted(
        Comparator.comparing(Entry::getKey)
    ).collect(Collectors.toList())) {
      Log.printLine(indent + indent + mapping.getKey()
          + indent + indent + indent + mapping.getValue());
    }
  }

  private static DatacenterBroker getDatacenterBroker(File workloadFile,
      Enum<VmOptimizingMethod> vmOptimizingMethodEnum) {
    DatacenterBroker broker = null;
    try {
      broker = new DynamicWorkloadDatacenterBroker("DynamicWorkloadBroker", workloadFile,
          vmOptimizingMethodEnum);
    } catch (Exception e) {
      Log.printLine("Failed to create the broker");
    }
    return broker;
  }

  private static void printStatusSuccessfulMessage(Datacenter datacenter) {

    String datacenterStatus = "Successfully created the datacenter. Here are some stats." + "\n" +
        "Number of Hosts: " + datacenter.getHostList().size() + ". Let's check stats of a host." +
        "\n" +
        "Number of cores: " + datacenter.getHostList().get(0).getNumberOfPes() +
        "\n" +
        "Amount of Ram(GB): " + datacenter.getHostList().get(0).getRam() +
        "\n" +
        "Amount of Storage(TB): " + datacenter.getHostList().get(0).getStorage();
    System.out.println(datacenterStatus);
  }

  private static Datacenter getDatacenter(List<Host> hosts,
      DatacenterCharacteristics datacenterCharacteristics) throws Exception {

    return new DatacenterBuilder("datacenter_0")
        .withDatacenterCharacteristics(datacenterCharacteristics)
        .withVmAllocationPolicy(new VmAllocationPolicySimple(hosts))
        .withSchedulingInterval(0)
        .build();
  }

  private static DatacenterCharacteristics getDatacenterCharacteristics(List<Host> hosts) {

    return new DatacenterCharacteristicsBuilder()
        .withSystemArchitecture("x86")
        .withOperatingSystem("Linux")
        .withVmm("Xen")
        .withTimeZoneOfTheLocation(10.0)
        .withCostPerUsingProcessing(3.0)
        .withCostPerUsingBandwidth(0.0)
        .withCostPerUsingMemory(0.05)
        .withCostPerUsingStorage(0.001)
        .withHosts(hosts)
        .build();
  }

  private static List<Host>  getHosts() {

    List<Host> hosts = new ArrayList<>();
    for (int index = 0; index < NUMBER_OF_HOSTS; index++) {
      hosts.add(
          new HostBuilder(index + 100)
              .withMipsPerCore(NUMBER_OF_MIPS_PER_HOST)
              .withNumberOfCores(NUMBER_OF_CORES_PER_HOST)
              .withAmountOfRamInGBs(AMOUNT_OF_RAM_PER_HOST_IN_GB)
              .withAmountOfStorageInGBs(AMOUNT_OF_STORAGE_PER_HOST_IN_TB)
              .withBandwidth(10000)
              .build()
      );
    }
    return hosts;
  }
}
