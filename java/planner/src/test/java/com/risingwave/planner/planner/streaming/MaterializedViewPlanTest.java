package com.risingwave.planner.planner.streaming;

import com.risingwave.common.config.LeaderServerConfigurations;
import com.risingwave.execution.handler.CreateMaterializedViewHandler;
import com.risingwave.planner.planner.batch.BatchPlanner;
import com.risingwave.planner.rel.physical.BatchPlan;
import com.risingwave.planner.rel.serialization.ExplainWriter;
import com.risingwave.planner.rel.streaming.StreamingPlan;
import com.risingwave.planner.util.PlanTestCaseLoader;
import com.risingwave.planner.util.PlannerTestCase;
import com.risingwave.planner.util.ToPlannerTestCase;
import com.risingwave.proto.streaming.plan.StreamNode;
import com.risingwave.rpc.Messages;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.ddl.SqlCreateMaterializedView;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/** Tests for streaming job */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MaterializedViewPlanTest extends StreamPlanTestBase {
  @BeforeAll
  public void initAll() {
    super.init();
    executionContext
        .getConf()
        .set(
            LeaderServerConfigurations.CLUSTER_MODE, LeaderServerConfigurations.ClusterMode.Single);
  }

  @ParameterizedTest(name = "{index} => {0}")
  @DisplayName("Streaming plan tests")
  @ArgumentsSource(PlanTestCaseLoader.class)
  @Order(0)
  public void testStreamingPlan(@ToPlannerTestCase PlannerTestCase testCase) {
    // Comment the line below if you want to make quick proto changes and this test block the
    // progress.
    runTestCase(testCase);
  }

  @Test
  @Order(1)
  void testCreateMaterializedView() {
    // A sample code for stream testing. Keep this for debugging.
    String sql = "create materialized view T_test as select sum(v1)+1 as V from t where v1>v2";
    SqlNode ast = parseSql(sql);
    StreamingPlan plan = streamPlanner.plan(ast, executionContext);
    String resultPlan = ExplainWriter.explainPlan(plan.getStreamingPlan());
    Assertions.assertEquals(
        "RwStreamMaterializedView(name=[t_test])\n"
            + "  RwStreamProject(v=[+($STREAM_NULL_BY_ROW_COUNT($1, $0), 1)])\n"
            + "    RwStreamAgg(group=[{}], agg#0=[SUM($0)], Row Count=[COUNT()])\n"
            + "      RwStreamFilter(condition=[>($0, $1)])\n"
            + "        RwStreamTableSource(table=[[test_schema, t]], columns=[v1,v2])",
        resultPlan);

    CreateMaterializedViewHandler handler = new CreateMaterializedViewHandler();
    SqlCreateMaterializedView createMaterializedView = (SqlCreateMaterializedView) ast;
    String tableName = createMaterializedView.name.getSimple();
    handler.convertPlanToCatalog(tableName, plan, executionContext);

    StreamNode serializedProto = plan.getStreamingPlan().serialize();
    String serializedJsonPlan = Messages.jsonFormat(serializedProto);
    // System.out.println(serializedJsonPlan);
  }

  @Test
  @Order(2)
  void testSelectFromMaterializedView() {
    String sql = "select * from T_test"; // T_test was created in previous case

    BatchPlanner batchPlanner = new BatchPlanner();
    BatchPlan plan = batchPlanner.plan(parseSql(sql), executionContext);
    String resultPlan = ExplainWriter.explainPlan(plan.getRoot());
    String expectedPlan =
        "RwBatchExchange(distribution=[RwDistributionTrait{type=SINGLETON, keys=[]}], collation=[[]])\n"
            + "  RwBatchMaterializedViewScan(table=[[test_schema, t_test]], columns=[v])";
    Assertions.assertEquals(expectedPlan, resultPlan);
  }

  @Test
  @Order(3)
  void testDistributedStreamingPlan() {
    executionContext
        .getConf()
        .set(
            LeaderServerConfigurations.CLUSTER_MODE,
            LeaderServerConfigurations.ClusterMode.Distributed);
    // A sample code for distributed streaming plan.
    String sql =
        "create materialized view T_distributed as select sum(v1)+1 as V from t where v1>v2";

    SqlNode ast = parseSql(sql);
    StreamingPlan plan = streamPlanner.plan(ast, executionContext);

    CreateMaterializedViewHandler handler = new CreateMaterializedViewHandler();
    SqlCreateMaterializedView createMaterializedView = (SqlCreateMaterializedView) ast;
    String tableName = createMaterializedView.name.getSimple();
    handler.convertPlanToCatalog(tableName, plan, executionContext);

    String explainExchangePlan = ExplainWriter.explainPlan(plan.getStreamingPlan());
    String expectedPlan =
        "RwStreamMaterializedView(name=[t_distributed])\n"
            + "  RwStreamProject(v=[+($STREAM_NULL_BY_ROW_COUNT($1, $0), 1)])\n"
            + "    RwStreamAgg(group=[{}], agg#0=[SUM($0)], Row Sum0=[$SUM0($1)])\n"
            + "      RwStreamExchange(distribution=[RwDistributionTrait{type=SINGLETON, keys=[]}], collation=[[]])\n"
            + "        RwStreamProject($f0=[$STREAM_NULL_BY_ROW_COUNT($1, $0)], Row Count=[$1])\n"
            + "          RwStreamAgg(group=[{}], agg#0=[SUM($0)], Row Count=[COUNT()])\n"
            + "            RwStreamFilter(condition=[>($0, $1)])\n"
            + "              RwStreamExchange(distribution=[RwDistributionTrait{type=HASH_DISTRIBUTED, keys=[0]}], collation=[[]])\n"
            + "                RwStreamTableSource(table=[[test_schema, t]], columns=[v1,v2])";
    System.out.println(explainExchangePlan);
    Assertions.assertEquals(expectedPlan, explainExchangePlan);
  }

  @Test
  @Order(4)
  void testDistributedAggPlan() {
    executionContext
        .getConf()
        .set(
            LeaderServerConfigurations.CLUSTER_MODE,
            LeaderServerConfigurations.ClusterMode.Distributed);
    String sql = "create materialized view T_agg as select v1, sum(v2) as V from t group by v1";

    SqlNode ast = parseSql(sql);
    StreamingPlan plan = streamPlanner.plan(ast, executionContext);

    CreateMaterializedViewHandler handler = new CreateMaterializedViewHandler();
    SqlCreateMaterializedView createMaterializedView = (SqlCreateMaterializedView) ast;
    String tableName = createMaterializedView.name.getSimple();
    handler.convertPlanToCatalog(tableName, plan, executionContext);

    String explainExchangePlan = ExplainWriter.explainPlan(plan.getStreamingPlan());
    String expectedPlan =
        "RwStreamMaterializedView(name=[t_agg])\n"
            + "  RwStreamProject(v1=[$0], v=[$1])\n"
            + "    RwStreamFilter(condition=[<>($2, 0)])\n"
            + "      RwStreamAgg(group=[{0}], v=[SUM($1)], Row Sum0=[$SUM0($2)])\n"
            + "        RwStreamExchange(distribution=[RwDistributionTrait{type=HASH_DISTRIBUTED, keys=[0]}], collation=[[]])\n"
            + "          RwStreamProject(v1=[$0], v=[$STREAM_NULL_BY_ROW_COUNT($2, $1)], Row Count=[$2])\n"
            + "            RwStreamAgg(group=[{0}], v=[SUM($1)], Row Count=[COUNT()])\n"
            + "              RwStreamExchange(distribution=[RwDistributionTrait{type=HASH_DISTRIBUTED, keys=[0]}], collation=[[]])\n"
            + "                RwStreamTableSource(table=[[test_schema, t]], columns=[v1,v2])";
    System.out.println(explainExchangePlan);
    Assertions.assertEquals(expectedPlan, explainExchangePlan);
  }

  @Test
  @Order(5)
  void testCreateMaterializedViewWithOrderAndLimit() {
    String sql =
        "create materialized view T_test_order_limit as select * from t order by v1, v2 LIMIT 100 OFFSET 10";
    SqlNode ast = parseSql(sql);
    StreamingPlan plan = streamPlanner.plan(ast, executionContext);
    String resultPlan = ExplainWriter.explainPlan(plan.getStreamingPlan());
    String expectedPlan =
        "RwStreamMaterializedView(name=[t_test_order_limit], collation=[[0, 1]], offset=[10], limit=[100])\n"
            + "  RwStreamExchange(distribution=[RwDistributionTrait{type=HASH_DISTRIBUTED, keys=[0]}], collation=[[]])\n"
            + "    RwStreamTableSource(table=[[test_schema, t]], columns=[v1,v2,v3,_row_id])";
    Assertions.assertEquals(expectedPlan, resultPlan);

    CreateMaterializedViewHandler handler = new CreateMaterializedViewHandler();
    SqlCreateMaterializedView createMaterializedView = (SqlCreateMaterializedView) ast;
    String tableName = createMaterializedView.name.getSimple();
    handler.convertPlanToCatalog(tableName, plan, executionContext);
  }

  @Test
  @Order(6)
  void testSelectMaterializedViewWithOrderAndLimit() {
    String sql = "select * from T_test_order_limit"; // T_test was created in previous case

    BatchPlanner batchPlanner = new BatchPlanner();
    BatchPlan plan = batchPlanner.plan(parseSql(sql), executionContext);
    String resultPlan = ExplainWriter.explainPlan(plan.getRoot());
    String expectedPlan =
        "RwBatchLimit(offset=[10], fetch=[100])\n"
            + "  RwBatchExchange(distribution=[RwDistributionTrait{type=SINGLETON, keys=[]}], collation=[[0, 1]])\n"
            + "    RwBatchLimit(offset=[0], fetch=[110])\n"
            + "      RwBatchMaterializedViewScan(table=[[test_schema, t_test_order_limit]], columns=[v1,v2,v3,_row_id])";
    Assertions.assertEquals(expectedPlan, resultPlan);
  }

  @Test
  @Order(7)
  void testSelectMaterializedViewWithOrderAndLimitBySameOrder() {
    String sql =
        "select * from T_test_order_limit order by v1, v2"; // T_test was created in previous case

    BatchPlanner batchPlanner = new BatchPlanner();
    BatchPlan plan = batchPlanner.plan(parseSql(sql), executionContext);
    String resultPlan = ExplainWriter.explainPlan(plan.getRoot());
    String expectedPlan =
        "RwBatchLimit(offset=[10], fetch=[100])\n"
            + "  RwBatchExchange(distribution=[RwDistributionTrait{type=SINGLETON, keys=[]}], collation=[[0, 1]])\n"
            + "    RwBatchLimit(offset=[0], fetch=[110])\n"
            + "      RwBatchMaterializedViewScan(table=[[test_schema, t_test_order_limit]], columns=[v1,v2,v3,_row_id])";
    Assertions.assertEquals(expectedPlan, resultPlan);
  }

  @Test
  @Order(7)
  void testSelectMaterializedViewWithOrderAndLimitByDifferentOrder() {
    String sql = "select * from T_test_order_limit order by v3, v2, v1";

    BatchPlanner batchPlanner = new BatchPlanner();
    BatchPlan plan = batchPlanner.plan(parseSql(sql), executionContext);
    String resultPlan = ExplainWriter.explainPlan(plan.getRoot());
    String expectedPlan =
        "RwBatchSort(sort0=[$2], sort1=[$1], sort2=[$0], dir0=[ASC], dir1=[ASC], dir2=[ASC])\n"
            + "  RwBatchLimit(offset=[10], fetch=[100])\n"
            + "    RwBatchExchange(distribution=[RwDistributionTrait{type=SINGLETON, keys=[]}], collation=[[0, 1]])\n"
            + "      RwBatchLimit(offset=[0], fetch=[110])\n"
            + "        RwBatchMaterializedViewScan(table=[[test_schema, t_test_order_limit]], columns=[v1,v2,v3,_row_id])";
    Assertions.assertEquals(expectedPlan, resultPlan);
  }

  @Test
  @Order(8)
  void testCreateMaterializedViewWithOrderAndZeroLimit() {
    String sql =
        "create materialized view T_test_order_zero_limit as select * from t order by v1, v2 OFFSET 10";
    SqlNode ast = parseSql(sql);
    StreamingPlan plan = streamPlanner.plan(ast, executionContext);
    String resultPlan = ExplainWriter.explainPlan(plan.getStreamingPlan());
    String expectedPlan =
        "RwStreamMaterializedView(name=[t_test_order_zero_limit], collation=[[0, 1]], offset=[10])\n"
            + "  RwStreamExchange(distribution=[RwDistributionTrait{type=HASH_DISTRIBUTED, keys=[0]}], collation=[[]])\n"
            + "    RwStreamTableSource(table=[[test_schema, t]], columns=[v1,v2,v3,_row_id])";
    Assertions.assertEquals(expectedPlan, resultPlan);

    CreateMaterializedViewHandler handler = new CreateMaterializedViewHandler();
    SqlCreateMaterializedView createMaterializedView = (SqlCreateMaterializedView) ast;
    String tableName = createMaterializedView.name.getSimple();
    handler.convertPlanToCatalog(tableName, plan, executionContext);
  }

  @Test
  @Order(9)
  void testSelectMaterializedViewWithOrderAndZeroLimit() {
    String sql = "select * from T_test_order_zero_limit"; // T_test was created in previous case

    BatchPlanner batchPlanner = new BatchPlanner();
    BatchPlan plan = batchPlanner.plan(parseSql(sql), executionContext);
    String resultPlan = ExplainWriter.explainPlan(plan.getRoot());
    String expectedPlan =
        "RwBatchLimit(offset=[10])\n"
            + "  RwBatchExchange(distribution=[RwDistributionTrait{type=SINGLETON, keys=[]}], collation=[[0, 1]])\n"
            + "    RwBatchMaterializedViewScan(table=[[test_schema, t_test_order_zero_limit]], columns=[v1,v2,v3,_row_id])";
    Assertions.assertEquals(expectedPlan, resultPlan);
  }
}
