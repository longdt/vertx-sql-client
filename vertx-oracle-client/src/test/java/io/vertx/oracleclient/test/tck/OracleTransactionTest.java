/*
 * Copyright (c) 2011-2022 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package io.vertx.oracleclient.test.tck;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.oracleclient.OraclePool;
import io.vertx.oracleclient.test.junit.OracleRule;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.tck.TransactionTestBase;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class OracleTransactionTest extends TransactionTestBase {


  @ClassRule
  public static OracleRule rule = OracleRule.SHARED_INSTANCE;

  @Override
  protected Pool createPool() {
    return OraclePool.pool(vertx, rule.options(), new PoolOptions().setMaxSize(1));
  }

  @Override
  protected Pool nonTxPool() {
    return OraclePool.pool(vertx, rule.options(), new PoolOptions().setMaxSize(1));
  }

  @Override
  protected String statement(String... parts) {
    return String.join(" ?", parts);
  }

  @Test
  public void testTransactionsInConsecutiveConnectionAcquisitions(TestContext ctx) {
    Pool pool = getPool();
    pool.query("TRUNCATE TABLE mutable").execute().<Void>mapEmpty()
      .compose(v -> pool.withTransaction(client -> client.query("INSERT INTO mutable (id,val) VALUES (1,'bim')").execute().<Void>mapEmpty()))
      .compose(v -> pool.withConnection(client -> client.query("DELETE FROM mutable WHERE id = 1").execute().<Void>mapEmpty()))
      .compose(v -> pool.withTransaction(client -> client.query("SELECT 1 FROM DUAL").execute().<Void>mapEmpty()))
      .onComplete(ctx.asyncAssertSuccess());
  }
}
