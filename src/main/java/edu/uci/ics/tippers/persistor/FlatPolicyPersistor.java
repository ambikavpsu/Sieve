package edu.uci.ics.tippers.persistor;

import edu.uci.ics.tippers.common.PolicyConstants;
import edu.uci.ics.tippers.dbms.mysql.MySQLConnectionManager;
import edu.uci.ics.tippers.dbms.postgresql.PGSQLConnectionManager;
import edu.uci.ics.tippers.model.policy.BEPolicy;

import java.sql.*;
import java.util.List;

public class FlatPolicyPersistor {

    private static FlatPolicyPersistor _instance = new FlatPolicyPersistor();

//    private static Connection connection = MySQLConnectionManager.getInstance().getConnection();
    private static Connection connection = PGSQLConnectionManager.getInstance().getConnection();

    public static FlatPolicyPersistor getInstance() {
        return _instance;
    }

    /**
     * @param bePolicyList
     */
    public void insertPolicies(List<BEPolicy> bePolicyList) {
        try {
            connection.setAutoCommit(true);

            String policyInsert =
                    "INSERT INTO FLAT_POLICY " +
                            "(id, querier, purpose, enforcement_action, inserted_at, ownerEq, profEq, groupEq, " +
                            " locEq, dateGe, dateLe, timeGe, timeLe, selectivity) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement policyStmt = connection.prepareStatement(policyInsert);

            for (BEPolicy bePolicy : bePolicyList) {
                policyStmt.setString(1, bePolicy.getId());

                // FIX: querier is INT in Postgres
                policyStmt.setInt(2, Integer.parseInt(bePolicy.fetchQuerier()));

                policyStmt.setString(3, bePolicy.getPurpose());
                policyStmt.setString(4, bePolicy.getAction());
                policyStmt.setTimestamp(5, bePolicy.getInserted_at());
                policyStmt.setInt(6, bePolicy.fetchOwner());
                policyStmt.setString(7, bePolicy.fetchProfile());
                policyStmt.setString(8, bePolicy.fetchGroup());
                policyStmt.setString(9, bePolicy.fetchLocation());

                List<java.sql.Date> dates = bePolicy.fetchDate();
                if (dates != null && dates.size() >= 2 && dates.get(0) != null && dates.get(1) != null) {
                    policyStmt.setDate(10, dates.get(0));
                    policyStmt.setDate(11, dates.get(1));
                } else {
                    policyStmt.setNull(10, java.sql.Types.DATE);
                    policyStmt.setNull(11, java.sql.Types.DATE);
                }

                List<java.sql.Time> times = bePolicy.fetchTime();
                if (times != null && times.size() >= 2 && times.get(0) != null && times.get(1) != null) {
                    policyStmt.setTime(12, times.get(0));
                    policyStmt.setTime(13, times.get(1));
                } else {
                    policyStmt.setNull(12, java.sql.Types.TIME);
                    policyStmt.setNull(13, java.sql.Types.TIME);
                }

                policyStmt.setFloat(14, bePolicy.computeL());
                policyStmt.addBatch();
            }

            policyStmt.executeBatch();
            policyStmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertCustomPolicies(List<BEPolicy> bePolicyList) {
        try {
            connection.setAutoCommit(true);

            String policyInsert =
                    "INSERT INTO CUSTOM_POLICY " +
                            "(id, querier, purpose, enforcement_action, inserted_at, ownerEq, profEq, groupEq, " +
                            " locEq, dateGe, dateLe, timeGe, timeLe, selectivity) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement policyStmt = connection.prepareStatement(policyInsert);

            for (BEPolicy bePolicy : bePolicyList) {
                policyStmt.setString(1, bePolicy.getId());

                // FIX: querier is INT in Postgres
                policyStmt.setInt(2, Integer.parseInt(bePolicy.fetchQuerier()));

                policyStmt.setString(3, bePolicy.getPurpose());
                policyStmt.setString(4, bePolicy.getAction());
                policyStmt.setTimestamp(5, bePolicy.getInserted_at());
                policyStmt.setInt(6, bePolicy.fetchOwner());
                policyStmt.setString(7, bePolicy.fetchProfile());
                policyStmt.setString(8, bePolicy.fetchGroup());
                policyStmt.setString(9, bePolicy.fetchLocation());

                List<java.sql.Date> dates = bePolicy.fetchDate();
                if (dates != null && dates.size() >= 2 && dates.get(0) != null && dates.get(1) != null) {
                    policyStmt.setDate(10, dates.get(0));
                    policyStmt.setDate(11, dates.get(1));
                } else {
                    policyStmt.setNull(10, java.sql.Types.DATE);
                    policyStmt.setNull(11, java.sql.Types.DATE);
                }

                List<java.sql.Time> times = bePolicy.fetchTime();
                if (times != null && times.size() >= 2 && times.get(0) != null && times.get(1) != null) {
                    policyStmt.setTime(12, times.get(0));
                    policyStmt.setTime(13, times.get(1));
                } else {
                    policyStmt.setNull(12, java.sql.Types.TIME);
                    policyStmt.setNull(13, java.sql.Types.TIME);
                }

                policyStmt.setFloat(14, bePolicy.computeL());
                policyStmt.addBatch();
            }

            policyStmt.executeBatch();
            policyStmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertPoliciesEnrollment(List<BEPolicy> bePolicyList) {
        try {
            connection.setAutoCommit(true);

            String policyInsert =
                    "INSERT INTO ENROLLMENT " +
                            "(user_id, faculty_id, course_name, loceq, start_date, end_date, start_time, end_time) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement policyStmt = connection.prepareStatement(policyInsert);

            for (BEPolicy bePolicy : bePolicyList) {
                policyStmt.setInt(1, bePolicy.fetchOwner());

                // FIX: querier is INT in Postgres
                policyStmt.setInt(2, Integer.parseInt(bePolicy.fetchQuerier()));
                policyStmt.setString(3, bePolicy.getPurpose());
                policyStmt.setString(4, bePolicy.fetchLocation());

                List<java.sql.Date> dates = bePolicy.fetchDate();
                if (dates != null && dates.size() >= 2 && dates.get(0) != null && dates.get(1) != null) {
                    policyStmt.setDate(5, dates.get(0));
                    policyStmt.setDate(6, dates.get(1));
                } else {
                    policyStmt.setNull(5, java.sql.Types.DATE);
                    policyStmt.setNull(6, java.sql.Types.DATE);
                }

                List<java.sql.Time> times = bePolicy.fetchTime();
                if (times != null && times.size() >= 2 && times.get(0) != null && times.get(1) != null) {
                    policyStmt.setTime(7, times.get(0));
                    policyStmt.setTime(8, times.get(1));
                } else {
                    policyStmt.setNull(7, java.sql.Types.TIME);
                    policyStmt.setNull(8, java.sql.Types.TIME);
                }

                policyStmt.addBatch();
            }

            policyStmt.executeBatch();
            policyStmt.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    public static void main(String [] args){
        PolicyPersistor polper = PolicyPersistor.getInstance();
        FlatPolicyPersistor flapolper = new FlatPolicyPersistor();
        List<BEPolicy> allowPolicies = polper.retrievePolicies(null,
                PolicyConstants.USER_INDIVIDUAL, PolicyConstants.ACTION_ALLOW);
        flapolper.insertPolicies(allowPolicies);
    }
}
