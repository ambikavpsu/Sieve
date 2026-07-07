package edu.uci.ics.tippers.template;

import edu.uci.ics.tippers.common.PolicyConstants;
import edu.uci.ics.tippers.dbms.QueryManager;
import edu.uci.ics.tippers.dbms.QueryResult;
import edu.uci.ics.tippers.dbms.mysql.MySQLConnectionManager;
import edu.uci.ics.tippers.dbms.postgresql.PGSQLConnectionManager;
import edu.uci.ics.tippers.execution.experiments.performance.QueryPerformance;
import edu.uci.ics.tippers.fileop.Writer;
import edu.uci.ics.tippers.generation.policy.WiFiDataSet.PolicyUtil;
import edu.uci.ics.tippers.model.guard.GuardExp;
import edu.uci.ics.tippers.model.guard.SelectGuard;
import edu.uci.ics.tippers.model.policy.BEExpression;
import edu.uci.ics.tippers.model.query.QueryStatement;
import edu.uci.ics.tippers.persistor.GuardPersistor;
import edu.uci.ics.tippers.persistor.PolicyPersistor;
import edu.uci.ics.tippers.model.policy.BEPolicy;

import java.util.Arrays;

import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
public class PRUsingViews {
    PolicyPersistor polper;
    GuardPersistor guardPersistor;
    Connection connection;
    private static QueryManager queryManager;

    public PRUsingViews(){
        this.polper = PolicyPersistor.getInstance();
        this.guardPersistor = new GuardPersistor();
//        this.connection = MySQLConnectionManager.getInstance().getConnection();
        this.connection = PGSQLConnectionManager.getInstance().getConnection();
    }

    public void runPlanComparison(List<Integer> queriers, boolean runPlanA, boolean runPlanB, boolean runPlanC) {
        Writer writer = new Writer();
        StringBuilder row = new StringBuilder();
        String fileName = "sample_run.csv";

        System.out.println("Running Template Plan Comparison Experiment");

        String header = String.join(",",
                "Querier",
                "Plan A PR",
                "Plan A Planning",
                "Plan A QR",
                "RC: A",
                "Plan B PR",
                "Plan B Planning",
                "Plan B QR",
                "RC: B",
                "Plan C PR",
                "Plan C Planning",
                "Plan C QR",
                "RC: C"
        ) + "\n";

        writer.writeString(header, PolicyConstants.EXP_RESULTS_DIR, fileName);

        QueryPerformance qp = new QueryPerformance();
        queryManager = new QueryManager();

        int index = 5;

        while (index>0){
            for (int querier : queriers) {
                double polRetMillisT = 0;
                double polRetMillisA = 0;
                double polRetMillisMV = 0;
                double guardGenMillisA = 0;
                double queryExeMillisA = 0;
                double queryExeMillisV = 0;
                double queryExeMillisB = 0;

                QueryResult planA = new QueryResult();
                QueryResult planV = new QueryResult();
                QueryResult planB = new QueryResult();

                if (runPlanA) {
                    System.out.println("Retrieving policies");
                    Instant prStartA = Instant.now();
                    List<BEPolicy> allowPoliciesA = polper.retrievePoliciesForFlatPolicyTable(querier);
                    System.out.println("Retrieving Done");
                    Instant prEndA = Instant.now();

                    if (allowPoliciesA != null) {
                        polRetMillisA = Duration.between(prStartA, prEndA).toMillis();

                        BEExpression beA = new BEExpression(allowPoliciesA);
                        Instant ggStartA = Instant.now();
                        SelectGuard ghA = new SelectGuard(beA, true);
                        System.out.println("Number of policies: " + beA.getPolicies().size() + " Number of Guards: " + ghA.numberOfGuards());
                        Instant ggEndA = Instant.now();

                        guardGenMillisA = Duration.between(ggStartA, ggEndA).toMillis();
                        GuardExp guardA = ghA.create();

                        String guard_query_union = guardA.queryRewrite(true, true);
                        Instant qeStartA = Instant.now();
                        planA = queryManager.runTimedQueryExp(guard_query_union, 1);
                        Instant qeEndA = Instant.now();

                        queryExeMillisA = Duration.between(qeStartA, qeEndA).toMillis();

                        long noOfPredA = guardA.countNoOfPredicate();
                        System.out.println("Query Executed: " + noOfPredA);
                    }
                }

                if (runPlanB) {
                    Instant prStartT = Instant.now();
                    List<BEPolicy> allowPoliciesT = polper.retrievePoliciesUsingViews(querier);
                    Instant prEndT = Instant.now();
                    System.out.println("Retrieving Done for Template Views");

                    if (allowPoliciesT != null) {
                        polRetMillisT = Duration.between(prStartT, prEndT).toMillis();

                        String rewrittenQuery =
                                "SELECT p.* " +
                                        "FROM presence p " +
                                        "WHERE EXISTS ( " +
                                        "    SELECT 1 " +
                                        "    FROM v_pr_attendance_policy ap " +
                                        "    WHERE ap.querier = " + querier + " " +
                                        "      AND ap.owner_id = p.user_id " +
                                        "      AND (ap.location_id IS NULL OR ap.location_id = p.location_id) " +
                                        "      AND (ap.start_date IS NULL OR p.start_date >= ap.start_date) " +
                                        "      AND (ap.end_date IS NULL OR p.start_date <= ap.end_date) " +
                                        "      AND (ap.start_time IS NULL OR p.start_time >= ap.start_time) " +
                                        "      AND (ap.end_time IS NULL OR p.start_time <= ap.end_time) " +
                                        ")";

//                        String rewrittenQuery =
//                                "SELECT p.* " +
//                                        "FROM presence p " +
//                                        "JOIN v_pr_attendance_policy ap " +
//                                        "  ON ap.owner_id = p.user_id " +
//                                        " AND ap.location_id = p.location_id " +
//                                        " AND (ap.start_date IS NULL OR p.start_date >= ap.start_date) " +
//                                        " AND (ap.end_date IS NULL OR p.start_date <= ap.end_date) " +
//                                        " AND (ap.start_time IS NULL OR p.start_time >= ap.start_time) " +
//                                        " AND (ap.end_time IS NULL OR p.start_time <= ap.end_time) " +
//                                        "WHERE ap.querier = " + querier + " " +
//                                        "UNION " +
//                                        "SELECT p.* " +
//                                        "FROM presence p " +
//                                        "JOIN custom_policy cp " +
//                                        "  ON cp.ownerEq = p.user_id " +
//                                        " AND cp.locEq = p.location_id " +
//                                        " AND (cp.dateGe IS NULL OR p.start_date >= cp.dateGe) " +
//                                        " AND (cp.dateLe IS NULL OR p.start_date <= cp.dateLe) " +
//                                        " AND (cp.timeGe IS NULL OR p.start_time >= cp.timeGe) " +
//                                        " AND (cp.timeLe IS NULL OR p.start_time <= cp.timeLe) " +
//                                        "WHERE cp.querier = " + querier + " " +
//                                        "  AND cp.purpose = 'attendance-control' " +
//                                        "  AND cp.enforcement_action = 'allow'";

                        Instant qeStartV = Instant.now();
                        planV = queryManager.runTimedQueryExp(rewrittenQuery, 1);
                        Instant qeEndV = Instant.now();

                        queryExeMillisV = Duration.between(qeStartV, qeEndV).toMillis();

                        System.out.println("Query Executed: " + planV.getResultCount());
                    }
                }

                if (runPlanC) {
                    System.out.println("Retrieving policies");
                    Instant prStartA = Instant.now();
                    List<BEPolicy> allowPoliciesA = polper.retrievePoliciesForFlatPolicyTable(querier);
                    System.out.println("Retrieving Done");
                    Instant prEndA = Instant.now();

                    if (allowPoliciesA != null) {

                        System.out.println("Inside Baseline P");
                        BEExpression beExpression = new BEExpression(allowPoliciesA);
                        String polEvalQuery = "With polEval as ( Select * from PRESENCE where "
                                + beExpression.createQueryFromPolices() + "  )";
                        Instant qeStartA = Instant.now();
                        planB = queryManager.runTimedQueryExp(polEvalQuery + "SELECT * from polEval ", 1);
                        Instant qeEndA = Instant.now();

                        queryExeMillisB = Duration.between(qeStartA, qeEndA).toMillis();

                        System.out.println("Query Executed with baseline");
                    }
                }

                row.setLength(0);
                row.append(querier).append(",")
                        .append(polRetMillisA).append(",")
                        .append(guardGenMillisA).append(",")
                        .append(queryExeMillisA).append(",")
                        .append(planA.getResultCount()).append(",")
                        .append(polRetMillisT).append(",")
                        .append(0).append(",")
                        .append(queryExeMillisV).append(",")
                        .append(planV.getResultCount()).append(",")
                        .append(0).append(",")
                        .append(0).append(",")
                        .append(queryExeMillisB).append(",")
                        .append(planB.getResultCount()).append("\n");

                writer.writeString(row.toString(), PolicyConstants.EXP_RESULTS_DIR, fileName);

                Boolean sanityCheck = planA.checkResults(planV);
                System.out.println("Querier: " + querier + " Sanity Check: " + sanityCheck);
                Boolean sanityCheck2 = planA.checkResults(planB);
                System.out.println("Querier: " + querier + " Sanity Check: " + sanityCheck2);
            }
            index --;
        }

    }

    public void runExperiment(){
        PRUsingViews tcp = new PRUsingViews();
        PolicyUtil pg = new PolicyUtil();
//        List<Integer> users = pg.getAllUsers(true);
        List<Integer> queriers = Arrays.asList(177);
        tcp.runPlanComparison(queriers, true,true,true);
    }

}
