/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.cluster.impl.operations;

import com.hazelcast.cluster.ClusterState;
import com.hazelcast.cluster.impl.ClusterServiceImpl;
import com.hazelcast.cluster.impl.ClusterStateManager;
import com.hazelcast.nio.Address;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.spi.AbstractOperation;
import com.hazelcast.transaction.TransactionException;
import com.hazelcast.util.EmptyStatement;

import java.io.IOException;

public class LockClusterStateOperation extends AbstractOperation {

    private String stateName;
    private ClusterState newState;
    private Address initiator;
    private String txnId;
    private long leaseTime;

    public LockClusterStateOperation() {
    }

    public LockClusterStateOperation(ClusterState newState, Address initiator, String txnId, long leaseTime) {
        this.newState = newState;
        this.initiator = initiator;
        this.txnId = txnId;
        this.leaseTime = leaseTime;
    }

    @Override
    public void beforeRun() throws Exception {
        if (newState == null) {
            throw new IllegalArgumentException("Unknown cluster state: " + stateName);
        }
    }

    @Override
    public void run() throws Exception {
        ClusterServiceImpl service = getService();
        ClusterStateManager clusterStateManager = service.getClusterStateManager();
        ClusterState state = clusterStateManager.getState();
        if (state == ClusterState.IN_TRANSITION) {
            getLogger().info("Extending cluster state lock. Initiator: " + initiator
                    + ", lease-time: " + leaseTime);
        } else {
            getLogger().info("Locking cluster state. Initiator: " + initiator
                    + ", lease-time: " + leaseTime);
        }
        clusterStateManager.lockClusterState(newState, initiator, txnId, leaseTime);
    }

    @Override
    public void logError(Throwable e) {
        if (e instanceof TransactionException) {
            getLogger().severe(e.getMessage());
        } else {
            super.logError(e);
        }
    }

    @Override
    public String getServiceName() {
        return ClusterServiceImpl.SERVICE_NAME;
    }

    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        super.writeInternal(out);
        out.writeUTF(newState.toString());
        initiator.writeData(out);
        out.writeUTF(txnId);
        out.writeLong(leaseTime);
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        super.readInternal(in);
        stateName = in.readUTF();
        try {
            newState = ClusterState.valueOf(stateName);
        } catch (IllegalArgumentException ignored) {
            EmptyStatement.ignore(ignored);
        }
        initiator = new Address();
        initiator.readData(in);
        txnId = in.readUTF();
        leaseTime = in.readLong();
    }
}
