/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.builders;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;

import java.util.List;

/**
 * A builder class to build datacenter characteristics in much friendly manner.
 */
public class DatacenterCharacteristicsBuilder {

    String systemArchitecture;
    String operatingSystem;
    String vmm;
    double timeZoneOfTheLocation;
    double costPerUsingProcessing;
    double costPerUsingMemory;
    double costPerUsingStorage;
    double costPerUsingBandwidth;
    List<Host> hostsList;

    public DatacenterCharacteristicsBuilder withSystemArchitecture(String systemArchitecture) {
        this.systemArchitecture = systemArchitecture;
        return this;
    }

    public DatacenterCharacteristicsBuilder withOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
        return this;
    }

    public DatacenterCharacteristicsBuilder withVmm(String vmm) {
        this.vmm = vmm;
        return this;
    }

    public DatacenterCharacteristicsBuilder withTimeZoneOfTheLocation(Double timeZoneOfTheLocation) {
        this.timeZoneOfTheLocation = timeZoneOfTheLocation;
        return this;
    }

    public DatacenterCharacteristicsBuilder withCostPerUsingProcessing(Double costPerUsingProcessing) {
        this.costPerUsingProcessing = costPerUsingProcessing;
        return this;
    }

    public DatacenterCharacteristicsBuilder withCostPerUsingMemory(Double costPerUsingMemory) {
        this.costPerUsingMemory = costPerUsingMemory;
        return this;
    }

    public DatacenterCharacteristicsBuilder withCostPerUsingStorage(Double costPerUsingStorage) {
        this.costPerUsingStorage = costPerUsingStorage;
        return this;
    }

    public DatacenterCharacteristicsBuilder withCostPerUsingBandwidth(Double costPerUsingBandwidth) {
        this.costPerUsingBandwidth = costPerUsingBandwidth;
        return this;
    }

    public DatacenterCharacteristicsBuilder withHosts(List<Host> hostsList) {
        this.hostsList = hostsList;
        return this;
    }

    public DatacenterCharacteristics build() {

        return new DatacenterCharacteristics(
                this.systemArchitecture,
                this.operatingSystem,
                this.vmm,
                this.hostsList,
                this.timeZoneOfTheLocation,
                this.costPerUsingProcessing,
                this.costPerUsingMemory,
                this.costPerUsingStorage,
                this.costPerUsingBandwidth
        );
    }
}
