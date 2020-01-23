/*
 * Copyright (C) 2019,2020 IBM Corporation
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
package io.vertx.db2client.impl.codec;

import java.util.HashMap;
import java.util.Map;

import io.vertx.db2client.impl.drda.Cursor;
import io.vertx.db2client.impl.drda.Section;
import io.vertx.db2client.impl.drda.SectionManager;
import io.vertx.sqlclient.impl.ParamDesc;
import io.vertx.sqlclient.impl.PreparedStatement;
import io.vertx.sqlclient.impl.RowDesc;
import io.vertx.sqlclient.impl.TupleInternal;

class DB2PreparedStatement implements PreparedStatement {

    final String sql;
    final DB2ParamDesc paramDesc;
    final DB2RowDesc rowDesc;
    final Section section;
    
    private final Map<String,QueryInstance> activeQueries = new HashMap<>(4);
    
    public static class QueryInstance {
        final String cursorId;
        long queryInstanceId;
        Cursor cursor;
        QueryInstance(String cursorId) {
            this.cursorId = cursorId;
        }
    }

    DB2PreparedStatement(String sql, DB2ParamDesc paramDesc, DB2RowDesc rowDesc, Section section) {
        this.paramDesc = paramDesc;
        this.rowDesc = rowDesc;
        this.sql = sql;
        this.section = section;
    }

    @Override
    public ParamDesc paramDesc() {
        return paramDesc;
    }

    @Override
    public RowDesc rowDesc() {
        return rowDesc;
    }

    @Override
    public String sql() {
        return sql;
    }

    @Override
    public String prepare(TupleInternal values) {
        return paramDesc.prepare(values);
    }
    
    QueryInstance getQueryInstance(String cursorId) {
        cursorId = cursorId == null ? "NULLID" : cursorId;
        return activeQueries.computeIfAbsent(cursorId, c -> {
            System.out.println("@AGG creating new queryInstance with id=" + c);
            return new QueryInstance(c);
        });
    }
    
    void closeQuery(QueryInstance query) {
        activeQueries.remove(query.cursorId);
    }
    
    void close() {
        activeQueries.values().stream().forEach(this::closeQuery);
        section.release();
    }
}
