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
package io.snappydata

import java.io.File

import io.snappydata.impl.LeadImpl
import org.apache.hadoop.conf.Configuration

import org.apache.spark.sql.catalyst.expressions.{Attribute, Expression}
import org.apache.spark.sql.catalyst.plans.physical.{OrderlessHashPartitioning, Partitioning}
import org.apache.spark.ui.{SnappyDashboardTab, SparkUI}
import org.apache.spark.util.SnappyUtils
import org.apache.spark.{SparkConf, SparkContext}

object ToolsCallbackImpl extends ToolsCallback {

  override def invokeLeadStartAddonService(sc: SparkContext): Unit = {
    LeadImpl.invokeLeadStartAddonService(sc)
  }

  def getOrderlessHashPartitioning(partitionColumns: Seq[Expression],
      partitionColumnAliases: Seq[Seq[Attribute]],
      numPartitions: Int, numBuckets: Int): Partitioning = {
    OrderlessHashPartitioning(partitionColumns, partitionColumnAliases,
      numPartitions, numBuckets)
  }

  override def checkOrderlessHashPartitioning(partitioning: Partitioning): Option[
      (Seq[Expression], Seq[Seq[Attribute]], Int, Int)] = partitioning match {
    case p: OrderlessHashPartitioning => Some(p.expressions, p.aliases,
      p.numPartitions, p.numBuckets)
    case _ => None
  }

  override def updateUI(scUI: Option[Any]): Unit = {
    scUI.foreach { ui =>
      new SnappyDashboardTab(ui.asInstanceOf[SparkUI])
    }
  }

  override def removeAddedJar(sc: SparkContext, jarName: String) : Unit =
    sc.removeAddedJar(jarName)

  /**
   * Callback to spark Utils to fetch file
   */
  override def doFetchFile(
      url: String,
      targetDir: File,
      filename: String): File = {
     SnappyUtils.doFetchFile(url, targetDir, filename)
  }

  override def setSessionDependencies(sparkContext: SparkContext, appName: String,
      classLoader: ClassLoader): Unit = {
    SnappyUtils.setSessionDependencies(sparkContext, appName, classLoader)
  }
}
