package edu.uci.ics.tippers.template;

/*
INSERT INTO mv_flat_policy
(id, querier, purpose, ownerEq, locEq, dateGe, dateLe, timeGe, timeLe, enforcement_action)
SELECT
  CONCAT('ENR:', e.id)                    AS id,
  e.faculty_id                            AS querier,
  COALESCE(e.course_name, 'COURSE')       AS purpose,
  e.user_id                               AS ownerEq,
  e.locEq                                 AS locEq,
  e.start_date                            AS dateGe,
  e.end_date                              AS dateLe,
  e.start_time                            AS timeGe,
  e.end_time                              AS timeLe,
  'ALLOW'                                 AS enforcement_action
FROM enrollment e
WHERE e.faculty_id IS NOT NULL
  AND e.user_id IS NOT NULL;

CREATE UNIQUE INDEX mv_flat_policy_pk ON mv_flat_policy (id);
CREATE INDEX mv_flat_policy_querier_idx ON mv_flat_policy (querier);
CREATE INDEX mv_flat_policy_querier_loc_idx ON mv_flat_policy (querier, locEq);
CREATE INDEX mv_flat_policy_querier_owner_idx ON mv_flat_policy (querier, ownerEq);

REFRESH MATERIALIZED VIEW mv_flat_policy;
 */


import edu.uci.ics.tippers.common.PolicyConstants;
import edu.uci.ics.tippers.dbms.mysql.MySQLConnectionManager;
import edu.uci.ics.tippers.fileop.Writer;
import edu.uci.ics.tippers.generation.policy.WiFiDataSet.PolicyUtil;
import edu.uci.ics.tippers.model.policy.BEPolicy;
import edu.uci.ics.tippers.persistor.GuardPersistor;
import edu.uci.ics.tippers.persistor.PolicyPersistor;

import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class PRUsingMaterializedViews {
    PolicyPersistor polper;
    GuardPersistor guardPersistor;
    Connection connection;

    public PRUsingMaterializedViews(){
        this.polper = PolicyPersistor.getInstance();
        this.guardPersistor = new GuardPersistor();
        this.connection = MySQLConnectionManager.getInstance().getConnection();
    }

    public void runPlanComparison(List<Integer> queriers, boolean runPlanA, boolean runPlanB) {
        Writer writer = new Writer();
        StringBuilder row = new StringBuilder();
        String fileName = "testing.csv";

        System.out.println("Running Template Plan Comparison Experiment");

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

//        // Shared query runner & statements
//        QueryPerformance qp = new QueryPerformance();
//        List<QueryStatement> queries = qp.getQueries(1, 4);



        // ---------- Using filters ----------
        final List<String> locs = Arrays.asList(
                "3142-clwa-2039",  "3142-clwa-2065",  "3142-clwa-2051",  "3146-clwa-6217",  "3141-clwb-1100",
                "3143-clwa-3065",  "3142-clwa-2019",  "3142-clwa-2059",  "3144-clwa-4209",  "3142-clwa-2051",
                "3144-clwa-4039",  "3142-clwa-2051",  "3145-clwa-5065",  "3144-clwa-4019",  "3143-clwa-3039"
        );
//        final String qTimeStart = String.valueOf(LocalTime.parse("00:00:00"));
//        final String qTimeEnd   = String.valueOf(LocalTime.parse("23:59:00"));
//        final String qDateStart = String.valueOf(LocalDate.parse("2018-02-01"));
//        final String qDateEnd   = String.valueOf(LocalDate.parse("2018-02-05"));

        // ---------------------------------------------------------------

        double polRetSecondsT = 0;
        double polRetSecondsA = 0;

        for (int querier : queriers) {

            // -------- PLAN A (without template) ----------
            if (runPlanA) {
                System.out.println("Retrieving policies");
                Instant prStartA = Instant.now();
                List<BEPolicy> allowPoliciesA = polper.retrievePolicies(
                        String.valueOf(querier),
                        PolicyConstants.USER_INDIVIDUAL,
                        PolicyConstants.ACTION_ALLOW
                );
                System.out.println("Retrieving Done");
                Instant prEndA = Instant.now();
                if (allowPoliciesA != null) {
                    Duration polRetA = Duration.between(prStartA, prEndA);
                    polRetSecondsA = polRetA.toNanos()/1_000_000_000.0;

                }

//                Instant prStartB = Instant.now();
//                List<BEPolicy> allowPoliciesB = polper.retrievePolicies(
//                        String.valueOf(querier),
//                        PolicyConstants.USER_INDIVIDUAL,
//                        PolicyConstants.ACTION_ALLOW,
//                        locs,
//                        null, null,
//                        null, null
//                );
//                Instant prEndB = Instant.now();
//                if (allowPoliciesB != null) {
//                    Duration polRetB = Duration.between(prStartB, prEndB);
//                    double polRetSecondsB = polRetB.toNanos() / 1_000_000_000.0;
//                }
            }

            // -------- PLAN B (with template) ----------
            if (runPlanB) {
                Instant prStartT = Instant.now();

                List<BEPolicy> allowPoliciesT = polper.retrievePoliciesUsingViews(
                        querier// includeTimeRange
                );

                Instant prEndT = Instant.now();

                if (allowPoliciesT != null) {
                    Duration polRetT = Duration.between(prStartT, prEndT);
                    polRetSecondsT = polRetT.toNanos() / 1_000_000_000.0;
                }
            }
            row.append(querier).append(",")
                    .append(polRetSecondsA).append(",")
                    .append(polRetSecondsT).append(",")
                    .append("\n");
            writer.writeString(row.toString(), PolicyConstants.EXP_RESULTS_DIR, fileName);
        }
    }

    public void runExperiment(){
        PRUsingMaterializedViews tcp = new PRUsingMaterializedViews();
        PolicyUtil pg = new PolicyUtil();
//        List<Integer> users = pg.getAllUsers(true);
        List<Integer> queriers = Arrays.asList(1704);
        tcp.runPlanComparison(queriers, true,true);
    }
}
