/*
 * Title:        CrunchyCookie source file.
 * Description:  CrunchyCookie source file for various tasks.
 * Licence:      MIT
 *
 * Copyright (c) 2021, CrunchyCookie.
 */

package org.crunchycookie.playground.cloudsim.builders;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;

import java.util.LinkedList;

/**
 * A builder class to build a datacenter in much friendly manner.
 */
public class DatacenterBuilder {

    String name;
    DatacenterCharacteristics datacenterCharacteristics;
    VmAllocationPolicy vmAllocationPolicy;
    LinkedList<Storage> storages = new LinkedList<>();
    double schedulingInterval = 0;

    public DatacenterBuilder(String name) {
        this.name = name;
    }

    public DatacenterBuilder withDatacenterCharacteristics(DatacenterCharacteristics datacenterCharacteristics) {
        this.datacenterCharacteristics = datacenterCharacteristics;
        return this;
    }

    public DatacenterBuilder withVmAllocationPolicy(VmAllocationPolicy vmAllocationPolicy) {
        this.vmAllocationPolicy = vmAllocationPolicy;
        return this;
    }

    public DatacenterBuilder withStorages(LinkedList<Storage> storages) {
        this.storages = storages;
        return this;
    }

    public DatacenterBuilder withSchedulingInterval(double schedulingInterval) {
        this.schedulingInterval = schedulingInterval;
        return this;
    }

    public Datacenter build() throws Exception {

        return new Datacenter(
                this.name,
                this.datacenterCharacteristics,
                this.vmAllocationPolicy,
                this.storages,
                this.schedulingInterval
        );
    }
}
