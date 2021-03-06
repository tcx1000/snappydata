/*
 * Copyright (c) 2016 SnappyData, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */
package org.apache.spark.sql.store

import scala.collection.mutable.ArrayBuffer

import io.snappydata.app.Data1
import io.snappydata.{SnappyFunSuite, SnappyTableStatsProviderService}
import io.snappydata.core.Data
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

import org.apache.spark.Logging
import org.apache.spark.sql.SnappySession.CachedKey
import org.apache.spark.sql._

/**
  * Tests for column tables in GFXD.
  */
class DisableTokenizationTest
    extends SnappyFunSuite
        with Logging
        with BeforeAndAfter
        with BeforeAndAfterAll {

  val table = "my_table"

  override def beforeAll(): Unit = {
    System.setProperty("DISABLE_TOKENIZATION", "true")
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    System.setProperty("DISABLE_TOKENIZATION", "false")
    super.afterAll()
  }

  after {
    SnappyTableStatsProviderService.suspendCacheInvalidation = false
    SnappySession.clearAllCache()
    snc.dropTable(s"$table", ifExists = true)
  }

  test("test disable property") {
    SnappyTableStatsProviderService.suspendCacheInvalidation = true
    val numRows = 100
    createSimpleTableAndPoupulateData(numRows, s"$table", true)

    try {
      (0 to 10).foreach(i => {
        val q = s"select b from $table where a = $i"
        var result = snc.sql(q).collect()
        assert(result(0).get(0) == i)
      })

      val cacheMap = SnappySession.getPlanCache.asMap()
      assert(cacheMap.size() == 11)

      SnappyTableStatsProviderService.suspendCacheInvalidation = false
    } finally {
      System.setProperty("DISABLE_TOKENIZATION", "false")
    }
  }

  private def createSimpleTableAndPoupulateData(numRows: Int, name: String,
      dosleep: Boolean = false) = {
    val data = ((0 to numRows), (0 to numRows), (0 to numRows)).zipped.toArray
    val rdd = sc.parallelize(data, data.length)
      .map(s => Data(s._1, s._2, s._3))
    val dataDF = snc.createDataFrame(rdd)

    snc.sql(s"Drop Table if exists $name")
    snc.sql(s"Create Table $name (a INT, b INT, c INT) " +
      "using column options()")
    dataDF.write.insertInto(s"$name")
    // This sleep was necessary as it has some dependency on the region size
    // collector thread frequency. Can't remember right now.
    if (dosleep) Thread.sleep(5000)
  }
}
