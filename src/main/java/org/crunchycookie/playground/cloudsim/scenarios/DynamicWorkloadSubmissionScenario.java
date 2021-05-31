/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.scenarios;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.crunchycookie.playground.cloudsim.brokers.DynamicWorkloadDatacenterBroker;
import org.crunchycookie.playground.cloudsim.builders.DatacenterBuilder;
import org.crunchycookie.playground.cloudsim.builders.DatacenterCharacteristicsBuilder;
import org.crunchycookie.playground.cloudsim.builders.HostBuilder;

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
    start(workloadFile);
  }

  private static File getWorkloadFile() {
    File workloadFile = new File(DynamicWorkloadSubmissionScenario.class.getClassLoader().getResource(
        WORKLOAD_FILE_NAME).getFile());
    return workloadFile;
  }

  public static boolean start(File workloadFile) {
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
    try {
      Datacenter datacenter = getDatacenter(hosts, datacenterCharacteristics);
      printStatusSuccessfulMessage(datacenter);
    } catch (Exception e) {
      System.out.println("Failed to create the datacenter");
      e.printStackTrace();
      return false;
    }

    // Create broker.
    DatacenterBroker broker = getDatacenterBroker(workloadFile);

    // Start the simulation.
    CloudSim.startSimulation();

    CloudSim.stopSimulation();

    // Final step: Print results when simulation is over.
    List<Cloudlet> newList = broker.getCloudletReceivedList();
    printCloudletList(newList);

    return true;
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

  private static DatacenterBroker getDatacenterBroker(File workloadFile) {
    DatacenterBroker broker = null;
    try {
      broker = new DynamicWorkloadDatacenterBroker("DynamicWorkloadBroker", workloadFile);
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

  private static List<Host> getHosts() {

    List<Host> hosts = new ArrayList<>();
    for (int index = 0; index < NUMBER_OF_HOSTS; index++) {
      hosts.add(
          new HostBuilder(index)
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
