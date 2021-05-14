/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.scenarios;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.crunchycookie.playground.cloudsim.builders.DatacenterBuilder;
import org.crunchycookie.playground.cloudsim.builders.DatacenterCharacteristicsBuilder;
import org.crunchycookie.playground.cloudsim.builders.HostBuilder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Simulation of one datacenter with 500 hosts. Each host has 8 cores(10000 MIPS each), 65 GB RAM, 10 TB of storage.
 */
public class CreateDatacenter {

    private final static int NUMBER_OF_HOSTS = 500;
    private final static int NUMBER_OF_CORES_PER_HOST = 8;
    private final static int NUMBER_OF_MIPS_PER_HOST = 10000;
    private final static int AMOUNT_OF_RAM_PER_HOST_IN_GB = 64;
    private final static int AMOUNT_OF_STORAGE_PER_HOST_IN_TB = 10;

    /**
     * Creates main() to run this example.
     *
     * @param args the args
     */
    public static void main(String[] args) {

        // Number of users that are going to use the cloud.
        int numberOfUsers = 1;

        // Initialize the CloudSim framework.
        CloudSim.init(numberOfUsers, Calendar.getInstance(), false);

        // Create the hosts.
        List<Host> hosts = getHosts();

        // Create the datacenter characteristics.
        DatacenterCharacteristics datacenterCharacteristics = getDatacenterCharacteristics(hosts);

        // Create the datacenter.
        Datacenter datacenter = null;
        try {
            datacenter = getDatacenter(hosts, datacenterCharacteristics);
        } catch (Exception e) {
            System.out.println("Failed to create the datacenter");
            e.printStackTrace();
        }

        printStatusSuccessfulMessage(datacenter);
    }

    private static void printStatusSuccessfulMessage(Datacenter datacenter) {

        StringBuilder datacenterStatus = new StringBuilder("Successfully created the datacenter. Here are some stats.");
        datacenterStatus.append("\n");
        datacenterStatus.append("Number of Hosts: " + datacenter.getHostList().size() + ". Let's check stats of a host.");
        datacenterStatus.append("\n");
        datacenterStatus.append("Number of cores: " + datacenter.getHostList().get(0).getNumberOfPes());
        datacenterStatus.append("\n");
        datacenterStatus.append("Amount of Ram(GB): " + datacenter.getHostList().get(0).getRam());
        datacenterStatus.append("\n");
        datacenterStatus.append("Amount of Storage(TB): " + datacenter.getHostList().get(0).getStorage());

        System.out.println(datacenterStatus);
    }

    private static Datacenter getDatacenter(List<Host> hosts, DatacenterCharacteristics datacenterCharacteristics) throws Exception {

        return new DatacenterBuilder("datacenter_0")
                .withDatacenterCharacteristics(datacenterCharacteristics)
                .withVmAllocationPolicy(new VmAllocationPolicySimple(hosts))
                .withSchedulingInterval(0)
                .build();
    }

    private static DatacenterCharacteristics getDatacenterCharacteristics(List<Host> hosts) {

        DatacenterCharacteristics datacenterCharacteristics = new DatacenterCharacteristicsBuilder()
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
        return datacenterCharacteristics;
    }

    private static List<Host> getHosts() {

        List<Host> hosts = new ArrayList<Host>();
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