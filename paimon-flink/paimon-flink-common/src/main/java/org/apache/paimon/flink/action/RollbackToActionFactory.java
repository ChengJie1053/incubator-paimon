/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.paimon.flink.action;

import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.MultipleParameterTool;

import java.util.Map;
import java.util.Optional;

/** Factory to create {@link RollbackToAction}. */
public class RollbackToActionFactory implements ActionFactory {

    public static final String IDENTIFIER = "rollback-to";

    @Override
    public String identifier() {
        return IDENTIFIER;
    }

    @Override
    public Optional<Action> create(MultipleParameterTool params) {
        Tuple3<String, String, String> tablePath = getTablePath(params);

        checkRequiredArgument(params, "version");
        String version = params.get("version");

        Map<String, String> catalogConfig = optionalConfigMap(params, "catalog-conf");

        RollbackToAction action =
                new RollbackToAction(
                        tablePath.f0, tablePath.f1, tablePath.f2, version, catalogConfig);

        return Optional.of(action);
    }

    @Override
    public void printHelp() {
        System.out.println(
                "Action \"rollback-to\" roll back a table to a specific snapshot ID or tag.");
        System.out.println();

        System.out.println("Syntax:");
        System.out.println(
                "  rollback-to --warehouse <warehouse-path> --database <database-name> "
                        + "--table <table-name> --version <version-string>");
        System.out.println(
                "  'version-string can be a long value representing a snapshot ID or a tag name.'");
        System.out.println();
    }
}
