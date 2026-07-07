package edu.uci.ics.tippers.execution.experiments.design;

import edu.uci.ics.tippers.common.PolicyConstants;
import edu.uci.ics.tippers.dbms.QueryResult;
import edu.uci.ics.tippers.dbms.mysql.MySQLConnectionManager;
import edu.uci.ics.tippers.execution.experiments.performance.QueryPerformance;
import edu.uci.ics.tippers.fileop.Writer;
import edu.uci.ics.tippers.generation.policy.WiFiDataSet.PolicyUtil;
import edu.uci.ics.tippers.model.guard.GuardExp;
import edu.uci.ics.tippers.model.query.QueryStatement;
import edu.uci.ics.tippers.persistor.GuardPersistor;
import edu.uci.ics.tippers.persistor.PolicyPersistor;
import edu.uci.ics.tippers.model.guard.SelectGuard;
import edu.uci.ics.tippers.model.policy.BEExpression;
import edu.uci.ics.tippers.model.policy.BEPolicy;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Experiment for measuring the time taken for policy retrieval, generating guards
 * and query execution belonging to queriers
 * for Plan A: running Sieve as it is
 * for Plan B: retrieving relevant policies depending on the query attributes
 */
public class PlanComparisonExp {
    PolicyPersistor polper;
    GuardPersistor guardPersistor;
    Connection connection;

    public PlanComparisonExp(){
        this.polper = PolicyPersistor.getInstance();
        this.guardPersistor = new GuardPersistor();
        this.connection = MySQLConnectionManager.getInstance().getConnection();
    }

    // Run Plan A and/or Plan B in one go and write a single CSV with a "Plan" column.
    public void runPlanComparison(List<Integer> queriers, boolean runPlanA, boolean runPlanB) {
        Writer writer = new Writer();
        StringBuilder row = new StringBuilder();
        String fileName = "Plans_Comparison_store_resultSize2.csv";

        System.out.println("Running Plan Comparison Experiment");

        // Write header once
        String header = String.join(",",
                "Querier",
                "Plan",
                "Allow Policies Size",
                "Number Of Guards",
                "No Of Predicates",
                "No Of Candidate Guards",
                "Guard Gen",
                "Policy Retrieval",
                "Query Execution"
        ) + "\n";
        writer.writeString(header, PolicyConstants.EXP_RESULTS_DIR, fileName);

        // Shared query runner & statements
        QueryPerformance qp = new QueryPerformance();
        List<QueryStatement> queries = qp.getQueries(1, 4);

//        start_date >= "2018-02-01" AND start_date <= "2018-02-05"
//        and start_time >= "00:00" AND start_time <= "23:59"
//        AND location_id in ("3142-clwa-2039",  "3142-clwa-2065",  "3142-clwa-2051",  "3146-clwa-6217",
//        "3141-clwb-1100",  "3143-clwa-3065",  "3142-clwa-2019",  "3142-clwa-2059",  "3144-clwa-4209",
//        "3142-clwa-2051",  "3144-clwa-4039",  "3142-clwa-2051",  "3145-clwa-5065",  "3144-clwa-4019",
//        "3143-clwa-3039")

        // ---------- Plan B fixed filters (same as your snippet) ----------
        final List<String> locs = Arrays.asList(
                "3142-clwa-2039",  "3142-clwa-2065",  "3142-clwa-2051",  "3146-clwa-6217",  "3141-clwb-1100",
                "3143-clwa-3065",  "3142-clwa-2019",  "3142-clwa-2059",  "3144-clwa-4209",  "3142-clwa-2051",
                "3144-clwa-4039",  "3142-clwa-2051",  "3145-clwa-5065",  "3144-clwa-4019",  "3143-clwa-3039"
        );

        final List<String> locs2 = Arrays.asList(
                "3143-clwa-3219",  "3146-clwa-6131",  "3146-clwa-6029",  "3144-clwa-4019",  "3142-clwa-2059",
                "3145-clwa-5019",  "3146-clwa-6049",  "3146-clwa-6049",  "3145-clwa-5039",  "3142-clwa-2065",
                "3144-clwa-4099",  "3142-clwa-2039",  "3144-clwa-4059",  "3141-clwe-1100",  "3146-clwa-6029",
                "3141-clwc-1100",  "3141-clwa-1433",  "3146-clwa-6029",  "3146-clwa-6217",  "3144-clwa-4219",
                "3142-clwa-2019",  "3141-clwb-1100",  "3146-clwa-6219",  "3142-clwa-2039",  "3142-clwa-2065",
                "3141-clwb-1100",  "3144-clwa-4065",  "3144-clwa-4099",  "3142-clwa-2051",  "3145-clwa-5099",
                "3146-clwa-6049",  "3143-clwa-3065",  "3141-clwa-1100",  "3141-clwa-1433",  "3145-clwa-5059",
                "3142-clwa-2231",  "3141-clwe-1100",  "3146-clwa-6011",  "3145-clwa-5219",  "3142-clwa-2209",
                "3142-clwa-2019",  "3145-clwa-5065",  "3145-clwa-5231",  "3144-clwa-4039",  "3143-clwa-3231",
                "3144-clwa-4231",  "3146-clwa-6131",  "3145-clwa-5209",  "3144-clwa-4039",  "3145-clwa-5065"
        );
        final String qTimeStart = String.valueOf(LocalTime.parse("00:00:00"));
        final String qTimeEnd   = String.valueOf(LocalTime.parse("23:59:00"));
        final String qDateStart = String.valueOf(LocalDate.parse("2018-02-01"));
        final String qDateEnd   = String.valueOf(LocalDate.parse("2018-02-05"));
        // ---------------------------------------------------------------

        for (int querier : queriers) {

            QueryResult planA = new QueryResult();
            QueryResult planB = new QueryResult();

            // -------- PLAN A ----------
            if (runPlanA) {
                Instant prStartA = Instant.now();
                List<BEPolicy> allowPoliciesA = polper.retrievePolicies(
                        String.valueOf(querier),
                        PolicyConstants.USER_INDIVIDUAL,
                        PolicyConstants.ACTION_ALLOW
                );
                Instant prEndA = Instant.now();
                if (allowPoliciesA != null) {
                    Duration polRetA = Duration.between(prStartA, prEndA);
                    double polRetSecondsA = polRetA.toNanos()/1_000_000_000.0;

                    BEExpression beA = new BEExpression(allowPoliciesA);
                    Instant ggStartA = Instant.now();
                    SelectGuard ghA = new SelectGuard(beA, true);
                    Instant ggEndA = Instant.now();
                    Duration guardGenA = Duration.between(ggStartA, ggEndA);
                    double guardGenSecondsA = guardGenA.toNanos()/1_000_000_000.0;

                    GuardExp guardA = ghA.create(String.valueOf(querier), "user");
                    Instant qeStartA = Instant.now();
                    planA = qp.queryExecution(String.valueOf(querier), queries.get(2), guardA);
                    Instant qeEndA = Instant.now();
                    Duration queryExeA = Duration.between(qeStartA, qeEndA);
                    double queryExeSecondsA = queryExeA.toNanos()/1_000_000_000.0;

                    long noOfPredA = ghA.create().countNoOfPredicate();

                    row.setLength(0);
                    row.append(querier).append(",")
                            .append("A").append(",")
                            .append(allowPoliciesA.size()).append(",")
                            .append(ghA.numberOfGuards()).append(",")
                            .append(noOfPredA).append(",")
                            .append(ghA.getTotalCandidateGuards()).append(",")
                            .append(guardGenSecondsA).append(",")
                            .append(polRetSecondsA).append(",")
                            .append(queryExeSecondsA).append(",")
                            .append(planA.getResultCount())
                            .append("\n");
                    writer.writeString(row.toString(), PolicyConstants.EXP_RESULTS_DIR, fileName);
                }
            }

            // -------- PLAN B ----------
            if (runPlanB) {
                Instant prStartB = Instant.now();
                List<BEPolicy> allowPoliciesB = polper.retrievePolicies(
                        String.valueOf(querier),
                        PolicyConstants.USER_INDIVIDUAL,
                        PolicyConstants.ACTION_ALLOW,
                        locs,
                        qTimeStart, qTimeEnd,
                        qDateStart, qDateEnd
                );
                Instant prEndB = Instant.now();
                if (allowPoliciesB != null) {
                    Duration polRetB = Duration.between(prStartB, prEndB);
                    double polRetSecondsB = polRetB.toNanos()/1_000_000_000.0;

                    BEExpression beB = new BEExpression(allowPoliciesB);
                    Instant ggStartB = Instant.now();
                    SelectGuard ghB = new SelectGuard(beB, true);
                    Instant ggEndB = Instant.now();
                    Duration guardGenB = Duration.between(ggStartB, ggEndB);
                    double guardGenSecondsB = guardGenB.toNanos()/1_000_000_000.0;

                    GuardExp guardB = ghB.create(String.valueOf(querier), "user");
                    Instant qeStartB = Instant.now();
                    planB = qp.queryExecution(String.valueOf(querier), queries.get(2), guardB);
                    Instant qeEndB = Instant.now();
                    Duration queryExeB = Duration.between(qeStartB, qeEndB);
                    double queryExeSecondsB = queryExeB.toNanos()/1_000_000_000.0;

                    long noOfPredB = ghB.create().countNoOfPredicate();

                    row.setLength(0);
                    row.append(querier).append(",")
                            .append("B").append(",")
                            .append(allowPoliciesB.size()).append(",")
                            .append(ghB.numberOfGuards()).append(",")
                            .append(noOfPredB).append(",")
                            .append(ghB.getTotalCandidateGuards()).append(",")
                            .append(guardGenSecondsB).append(",")
                            .append(polRetSecondsB).append(",")
                            .append(queryExeSecondsB).append(",")
                            .append(planB.getResultCount())
                            .append("\n");
                    writer.writeString(row.toString(), PolicyConstants.EXP_RESULTS_DIR, fileName);
                }
            }

            Boolean sanityCheck = planA.checkResults(planB);
            System.out.println("Querier: " + querier + " Sanity Check: " + sanityCheck);
        }
    }

    public void runExperiment(){
        PlanComparisonExp pce = new PlanComparisonExp();
        PolicyUtil pg = new PolicyUtil();
        List<Integer> users = pg.getAllUsers(true);
        pce.runPlanComparison(users, true,true);
    }
}
