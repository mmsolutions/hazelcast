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

package com.hazelcast.cluster.impl;

import com.hazelcast.instance.MemberImpl;
import com.hazelcast.instance.Node;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.discovery.DiscoveredNode;
import com.hazelcast.spi.discovery.integration.DiscoveryService;

import java.util.ArrayList;
import java.util.Collection;

public class DiscoveryJoiner
        extends TcpIpJoiner {

    private final DiscoveryService discoveryService;

    public DiscoveryJoiner(Node node, DiscoveryService discoveryService) {
        super(node);
        this.discoveryService = discoveryService;
    }

    @Override
    protected Collection<Address> getPossibleAddresses() {
        Iterable<DiscoveredNode> discoveredNodes = discoveryService.discoverNodes();

        MemberImpl localMember = node.nodeEngine.getLocalMember();
        Address localAddress = localMember.getAddress();

        Collection<Address> possibleMembers = new ArrayList<Address>();
        for (DiscoveredNode discoveredNode : discoveredNodes) {
            Address discoveredAddress = discoveredNode.getPrivateAddress();
            if (localAddress.equals(discoveredAddress)) {
                continue;
            }
            possibleMembers.add(discoveredAddress);
        }
        return possibleMembers;
    }
}
