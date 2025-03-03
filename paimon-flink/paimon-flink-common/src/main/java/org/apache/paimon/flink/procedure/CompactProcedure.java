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

package org.apache.paimon.flink.procedure;

import org.apache.paimon.catalog.AbstractCatalog;
import org.apache.paimon.catalog.Identifier;
import org.apache.paimon.flink.action.ActionFactory;
import org.apache.paimon.flink.action.CompactAction;
import org.apache.paimon.flink.action.SortCompactAction;
import org.apache.paimon.utils.StringUtils;

import org.apache.flink.table.procedure.ProcedureContext;

import java.util.Collections;
import java.util.Map;

/**
 * Compact procedure. Usage:
 *
 * <pre><code>
 *  -- NOTE: use '' as placeholder for optional arguments
 *
 *  -- compact a table (tableId should be 'database_name.table_name')
 *  CALL sys.compact('tableId')
 *
 *  -- compact specific partitions ('pt1=A,pt2=a;pt1=B,pt2=b', ...)
 *  CALL sys.compact('tableId', 'pt1=A,pt2=a;pt1=B,pt2=b')
 *
 *  -- compact a table with sorting
 *  CALL sys.compact('tableId', 'partitions', 'ORDER/ZORDER', 'col1,col2', 'sink.parallelism=6')
 *
 * </code></pre>
 */
public class CompactProcedure extends ProcedureBase {

    public static final String IDENTIFIER = "compact";

    public String[] call(ProcedureContext procedureContext, String tableId) throws Exception {
        return call(procedureContext, tableId, "");
    }

    public String[] call(ProcedureContext procedureContext, String tableId, String partitions)
            throws Exception {
        return call(procedureContext, tableId, partitions, "", "", "");
    }

    public String[] call(
            ProcedureContext procedureContext,
            String tableId,
            String partitions,
            String orderStrategy,
            String orderByColumns)
            throws Exception {
        return call(procedureContext, tableId, partitions, orderStrategy, orderByColumns, "");
    }

    public String[] call(
            ProcedureContext procedureContext,
            String tableId,
            String partitions,
            String orderStrategy,
            String orderByColumns,
            String tableOptions)
            throws Exception {
        String warehouse = ((AbstractCatalog) catalog).warehouse();
        Map<String, String> catalogOptions = ((AbstractCatalog) catalog).options();
        Map<String, String> tableConf =
                StringUtils.isBlank(tableOptions)
                        ? Collections.emptyMap()
                        : ActionFactory.parseCommaSeparatedKeyValues(tableOptions);
        Identifier identifier = Identifier.fromString(tableId);
        CompactAction action;
        String jobName;
        if (orderStrategy.isEmpty() && orderByColumns.isEmpty()) {
            action =
                    new CompactAction(
                            warehouse,
                            identifier.getDatabaseName(),
                            identifier.getObjectName(),
                            catalogOptions,
                            tableConf);
            jobName = "Compact Job";
        } else if (!orderStrategy.isEmpty() && !orderByColumns.isEmpty()) {
            action =
                    new SortCompactAction(
                                    warehouse,
                                    identifier.getDatabaseName(),
                                    identifier.getObjectName(),
                                    catalogOptions,
                                    tableConf)
                            .withOrderStrategy(orderStrategy)
                            .withOrderColumns(orderByColumns.split(","));
            jobName = "Sort Compact Job";
        } else {
            throw new IllegalArgumentException(
                    "You must specify 'order strategy' and 'order by columns' both.");
        }

        if (!(StringUtils.isBlank(partitions) || "ALL".equals(partitions))) {
            action.withPartitions(getPartitions(partitions.split(";")));
        }

        return execute(procedureContext, action, jobName);
    }

    @Override
    public String identifier() {
        return IDENTIFIER;
    }
}
