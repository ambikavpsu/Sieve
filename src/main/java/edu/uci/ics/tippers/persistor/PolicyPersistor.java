package edu.uci.ics.tippers.persistor;

import edu.uci.ics.tippers.common.AttributeType;
import edu.uci.ics.tippers.common.PolicyConstants;
import edu.uci.ics.tippers.model.policy.*;
import edu.uci.ics.tippers.template.PolicyTemplate;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.*;

public class PolicyPersistor {

    private static final PolicyPersistor _instance = new PolicyPersistor();
    //TODO: Generalize this database connection
    private static Connection connection;

    private PolicyPersistor(){

    }

    public static PolicyPersistor getInstance() {
        connection = PolicyConstants.getDBMSConnection();
        return _instance;
    }

    /**
     * Inserts a list of policies into a relational table based on whether it's a user policy or a group policy
     *
     * @param bePolicies
     */
    public void insertPolicy(List<BEPolicy> bePolicies) {
        String userPolicyInsert = "INSERT INTO USER_POLICY " +  //
                "(id, querier, purpose, enforcement_action, inserted_at) VALUES (?, ?, ?, ?, ?)";
        String userobjectConditionInsert = "INSERT INTO USER_POLICY_OBJECT_CONDITION " +
                "(policy_id, attribute, attribute_type, operator, comp_value) VALUES (?, ?, ?, ?, ?)";
        String groupPolicyInsert = "INSERT INTO GROUP_POLICY " +
                "(id, querier, purpose, enforcement_action, inserted_at) VALUES (?, ?, ?, ?, ?)";
        String groupObjectConditionInsert = "INSERT INTO GROUP_POLICY_OBJECT_CONDITION " +
                "(policy_id, attribute, attribute_type, operator, comp_value) VALUES (?, ?, ?, ?, ?)";

        boolean USER_POLICY = true;

        try {
            PreparedStatement userPolicyStmt = connection.prepareStatement(userPolicyInsert);
            PreparedStatement userOcStmt = connection.prepareStatement(userobjectConditionInsert);

            PreparedStatement groupPolicyStmt = connection.prepareStatement(groupPolicyInsert);
            PreparedStatement groupOcStmt = connection.prepareStatement(groupObjectConditionInsert);
            int policyCount = 0;

            for (BEPolicy bePolicy : bePolicies) {
                if (bePolicy.typeOfPolicy()) { //User Policy
                    userPolicyStmt.setString(1, bePolicy.getId());
                    userPolicyStmt.setInt(2, Integer.parseInt(bePolicy.fetchQuerier()));
                    userPolicyStmt.setString(3, bePolicy.getPurpose());
                    userPolicyStmt.setString(4, bePolicy.getAction());
                    userPolicyStmt.setTimestamp(5, bePolicy.getInserted_at());
                    userPolicyStmt.addBatch();

                    for (ObjectCondition oc : bePolicy.getObject_conditions()) {
                        for (BooleanPredicate bp : oc.getBooleanPredicates()) {
                            userOcStmt.setString(1, bePolicy.getId());
                            userOcStmt.setString(2, oc.getAttribute());
                            userOcStmt.setString(3, oc.getType().toString());
                            userOcStmt.setString(4, bp.getOperator().toString());
                            userOcStmt.setString(5, bp.getValue());
                            userOcStmt.addBatch();
                        }
                    }
                    policyCount++;

                } else { //Group Policy
                    USER_POLICY = false;

                    groupPolicyStmt.setString(1, bePolicy.getId());
                    groupPolicyStmt.setInt(2, Integer.parseInt(bePolicy.fetchQuerier()));
                    groupPolicyStmt.setString(3, bePolicy.getPurpose());
                    groupPolicyStmt.setString(4, bePolicy.getAction());
                    groupPolicyStmt.setTimestamp(5, bePolicy.getInserted_at());
                    groupPolicyStmt.addBatch();
                    groupPolicyStmt.close();

                    for (ObjectCondition oc : bePolicy.getObject_conditions()) {
                        for (BooleanPredicate bp : oc.getBooleanPredicates()) {
                            groupOcStmt.setString(1, bePolicy.getId());
                            groupOcStmt.setString(2, oc.getAttribute());
                            groupOcStmt.setString(3, oc.getType().toString());
                            groupOcStmt.setString(4, bp.getOperator().toString());
                            groupOcStmt.setString(5, bp.getValue());
                            groupOcStmt.addBatch();
                        }
                    }
                }
                if (USER_POLICY) {
//                    if (policyCount % 100 == 0) {
                        userPolicyStmt.executeBatch();
                        userOcStmt.executeBatch();
//                        System.out.println("# " + policyCount + " inserted");
//                    }
                } else {
                    groupPolicyStmt.executeBatch();
                    groupOcStmt.executeBatch();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertPolicy(List<BEPolicy> bePolicies, String tablename) {
        String userPolicyInsert = "INSERT INTO " + tablename + " " +  //
                "(id, querier, purpose, enforcement_action, inserted_at) VALUES (?, ?, ?, ?, ?)";
        String userobjectConditionInsert = "INSERT INTO " + tablename + "_OBJECT_CONDITION " +
                "(policy_id, attribute, attribute_type, operator, comp_value) VALUES (?, ?, ?, ?, ?)";
        String groupPolicyInsert = "INSERT INTO " + tablename + "_GROUP_POLICY " +
                "(id, querier, purpose, enforcement_action, inserted_at) VALUES (?, ?, ?, ?, ?)";
        String groupObjectConditionInsert = "INSERT INTO " + tablename + "_GROUP_POLICY_OBJECT_CONDITION " +
                "(policy_id, attribute, attribute_type, operator, comp_value) VALUES (?, ?, ?, ?, ?)";

        boolean USER_POLICY = true;

        try {
            PreparedStatement userPolicyStmt = connection.prepareStatement(userPolicyInsert);
            PreparedStatement userOcStmt = connection.prepareStatement(userobjectConditionInsert);

            PreparedStatement groupPolicyStmt = connection.prepareStatement(groupPolicyInsert);
            PreparedStatement groupOcStmt = connection.prepareStatement(groupObjectConditionInsert);

            for (BEPolicy bePolicy : bePolicies) {
                if (bePolicy.typeOfPolicy()) { //User Policy
                    userPolicyStmt.setString(1, bePolicy.getId());
                    userPolicyStmt.setInt(2, Integer.parseInt(bePolicy.fetchQuerier()));
                    userPolicyStmt.setString(3, bePolicy.getPurpose());
                    userPolicyStmt.setString(4, bePolicy.getAction());
                    userPolicyStmt.setTimestamp(5, bePolicy.getInserted_at());
                    userPolicyStmt.addBatch();

                    for (ObjectCondition oc : bePolicy.getObject_conditions()) {
                        for (BooleanPredicate bp : oc.getBooleanPredicates()) {
                            userOcStmt.setString(1, bePolicy.getId());
                            userOcStmt.setString(2, oc.getAttribute());
                            userOcStmt.setString(3, oc.getType().toString());
                            userOcStmt.setString(4, bp.getOperator().toString());
                            userOcStmt.setString(5, bp.getValue());
                            userOcStmt.addBatch();
                        }
                    }
                } else { //Group Policy
                    USER_POLICY = false;

                    groupPolicyStmt.setString(1, bePolicy.getId());
                    groupPolicyStmt.setInt(2, Integer.parseInt(bePolicy.fetchQuerier()));
                    groupPolicyStmt.setString(3, bePolicy.getPurpose());
                    groupPolicyStmt.setString(4, bePolicy.getAction());
                    groupPolicyStmt.setTimestamp(5, bePolicy.getInserted_at());
                    groupPolicyStmt.addBatch();
                    groupPolicyStmt.close();

                    for (ObjectCondition oc : bePolicy.getObject_conditions()) {
                        for (BooleanPredicate bp : oc.getBooleanPredicates()) {
                            groupOcStmt.setString(1, bePolicy.getId());
                            groupOcStmt.setString(2, oc.getAttribute());
                            groupOcStmt.setString(3, oc.getType().toString());
                            groupOcStmt.setString(4, bp.getOperator().toString());
                            groupOcStmt.setString(5, bp.getValue());
                            groupOcStmt.addBatch();
                        }
                    }
                }
                if (USER_POLICY) {
                    userPolicyStmt.executeBatch();
                    userOcStmt.executeBatch();
                } else {
                    groupPolicyStmt.executeBatch();
                    groupOcStmt.executeBatch();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Operation convertOperator(String operator) {
        if (operator.equalsIgnoreCase("=")) return Operation.EQ;
        else if (operator.equalsIgnoreCase(">=")) return Operation.GTE;
        else if (operator.equalsIgnoreCase("<=")) return Operation.LTE;
        else if (operator.equalsIgnoreCase("<")) return Operation.LT;
        else return Operation.GT;
    }

    public List<BEPolicy> retrievePolicies(String querier, String querier_type, String enforcement_action) {
        List<BEPolicy> bePolicies = new ArrayList<>();
        String id = null, purpose = null, action = null;
        Timestamp inserted_at = null;

        String policy_table = null, oc_table = null;
        if (querier_type.equalsIgnoreCase("user")) {
            policy_table = "USER_POLICY";
            oc_table = "USER_POLICY_OBJECT_CONDITION";
        } else if (querier_type.equalsIgnoreCase("group")) {
            policy_table = "GROUP_POLICY";
            oc_table = "GROUP_POLICY_OBJECT_CONDITION";
        }
        PreparedStatement queryStm = null;
        try {
            if (querier != null) {
                queryStm = connection.prepareStatement("SELECT " + policy_table + ".id as \"" + policy_table + ".id\"," +
                        policy_table + ".querier as \"" + policy_table + ".querier\"," +
                        policy_table + ".purpose as \"" + policy_table + ".purpose\", " +
                        policy_table + ".enforcement_action as \"" + policy_table + ".enforcement_action\"," +
                        policy_table + ".inserted_at as \"" + policy_table + ".inserted_at\"," +
                        oc_table + ".id as \"" + oc_table + ".id\", " +
                        oc_table + ".policy_id as \"" + oc_table + ".policy_id\"," +
                        oc_table + ".attribute as \"" + oc_table + ".attribute\", " +
                        oc_table + ".attribute_type as \"" + oc_table + ".attribute_type\", " +
                        oc_table + ".operator as \"" + oc_table + ".operator\"," +
                        oc_table + ".comp_value as \"" + oc_table + ".comp_value\" " +
                        "FROM " + policy_table + ", " + oc_table +
                        " WHERE " + policy_table + ".querier=? AND " + policy_table + ".id = " + oc_table + ".policy_id " +
                        "AND " + policy_table + ".enforcement_action=? " +
                        " order by " + policy_table + ".id, " + oc_table + ".attribute, " + oc_table + ".comp_value");
                queryStm.setString(1, querier);
                queryStm.setString(2, enforcement_action);
            } else {
                queryStm = connection.prepareStatement("SELECT " + policy_table + ".id, " + policy_table + ".querier, " + policy_table + ".purpose, " +
                        policy_table + ".enforcement_action," + policy_table + ".inserted_at," + oc_table + ".id, " + oc_table + " .policy_id," + oc_table + ".attribute, " +
                        oc_table + ".attribute_type, " + oc_table + ".operator," + oc_table + ".comp_value " +
                        "FROM " + policy_table + ", " + oc_table +
                        " WHERE " + policy_table + ".id = " + oc_table + ".policy_id " +
                        "AND " + policy_table + ".enforcement_action=? " +
                        "order by " + policy_table + ".id, " + oc_table + ".attribute, " + oc_table + ".comp_value");
                queryStm.setString(1, enforcement_action);
            }
            ResultSet rs = queryStm.executeQuery();
            if (!rs.next()) return null;
            String next = null;
            boolean skip = false;
            List<QuerierCondition> querierConditions = new ArrayList<>();
            List<ObjectCondition> objectConditions = new ArrayList<>();
            while (true) {
                if (!skip) {
                    id = rs.getString(policy_table + ".id");
                    purpose = rs.getString(policy_table + ".purpose");
                    action = rs.getString(policy_table + ".enforcement_action");
                    inserted_at = rs.getTimestamp(policy_table + ".inserted_at");
                    querier = rs.getString(policy_table + ".querier");

                    querierConditions = new ArrayList<>();
                    QuerierCondition qc1 = new QuerierCondition();
                    qc1.setPolicy_id(id);
                    qc1.setAttribute("policy_type");
                    qc1.setType(AttributeType.STRING);
                    List<BooleanPredicate> qbps1 = new ArrayList<>();
                    BooleanPredicate qbp1 = new BooleanPredicate();
                    qbp1.setOperator(Operation.EQ);
                    qbp1.setValue(querier_type);
                    qbps1.add(qbp1);
                    qc1.setBooleanPredicates(qbps1);
                    querierConditions.add(qc1);
                    QuerierCondition qc2 = new QuerierCondition();
                    qc2.setPolicy_id(id);
                    qc2.setAttribute("querier");
                    qc2.setType(AttributeType.STRING);
                    List<BooleanPredicate> qbps2 = new ArrayList<>();
                    BooleanPredicate qbp2 = new BooleanPredicate();
                    qbp2.setOperator(Operation.EQ);
                    qbp2.setValue(querier);
                    qbps2.add(qbp2);
                    qc2.setBooleanPredicates(qbps2);
                    querierConditions.add(qc2);
                    objectConditions = new ArrayList<>();
                }
                ObjectCondition oc = new ObjectCondition();
                oc.setAttribute(rs.getString(oc_table + ".attribute"));
                oc.setPolicy_id(rs.getString(oc_table + ".policy_id"));
                oc.setType(AttributeType.valueOf(rs.getString(oc_table + ".attribute_type")));
                List<BooleanPredicate> booleanPredicates = new ArrayList<>();
                BooleanPredicate bp1 = new BooleanPredicate();
                bp1.setOperator(convertOperator(rs.getString(oc_table + ".operator")));
                bp1.setValue(rs.getString(oc_table + ".comp_value"));
                rs.next();
                BooleanPredicate bp2 = new BooleanPredicate();
                bp2.setOperator(convertOperator(rs.getString(oc_table + ".operator")));
                bp2.setValue(rs.getString(oc_table + ".comp_value"));
                booleanPredicates.add(bp1);
                booleanPredicates.add(bp2);
                oc.setBooleanPredicates(booleanPredicates);
                objectConditions.add(oc);

                if (!rs.next()) {
                    BEPolicy bePolicy = new BEPolicy(id, objectConditions, querierConditions, purpose, action, inserted_at);
                    bePolicies.add(bePolicy);
                    break;
                }

                next = rs.getString(policy_table + ".id");
                if (!id.equalsIgnoreCase(next)) {
                    BEPolicy bePolicy = new BEPolicy(id, objectConditions, querierConditions, purpose, action, inserted_at);
                    bePolicies.add(bePolicy);
                    skip = false;
                } else skip = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bePolicies;
    }



    /**
     * Retrieves policies with their object conditions, applying optional filters
     * (querier, action, location, time, date) using EXISTS function. Builds BEPolicy objects by grouping
     * query results into querier and object conditions per policy.
     */
//    public List<BEPolicy> retrievePolicies(
//            String querier,
//            String querier_type,
//            String enforcement_action,
//            List<String> locations,      // e.g., ["3144-clwa-4051","3142-clwa-2051"]; null/empty = no filter
//            String timeStart, String timeEnd,   // e.g., "08:00:00" .. "17:00:00"; both non-null to enable
//            String dateStart, String dateEnd    // e.g., "2018-02-01" .. "2018-03-10"; both non-null to enable
//    ) {
//        List<BEPolicy> bePolicies = new ArrayList<>();
//        String id = null, purpose = null, action = null;
//        Timestamp inserted_at = null;
//
//        String policy_table = null, oc_table = null;
//        if ("user".equalsIgnoreCase(querier_type)) {
//            policy_table = "USER_POLICY";
//            oc_table = "USER_POLICY_OBJECT_CONDITION";
//        } else if ("group".equalsIgnoreCase(querier_type)) {
//            policy_table = "GROUP_POLICY";
//            oc_table = "GROUP_POLICY_OBJECT_CONDITION";
//        } else {
//            // fallback to user tables if not specified
//            policy_table = "USER_POLICY";
//            oc_table = "USER_POLICY_OBJECT_CONDITION";
//        }
//
//        PreparedStatement queryStm = null;
//        try {
//            // ---------- Build SELECT + WHERE ----------
//            StringBuilder sql = new StringBuilder();
//            sql.append("SELECT ")
//                    .append(policy_table).append(".id as \"").append(policy_table).append(".id\",")
//                    .append(policy_table).append(".querier as \"").append(policy_table).append(".querier\",")
//                    .append(policy_table).append(".purpose as \"").append(policy_table).append(".purpose\", ")
//                    .append(policy_table).append(".enforcement_action as \"").append(policy_table).append(".enforcement_action\",")
//                    .append(policy_table).append(".inserted_at as \"").append(policy_table).append(".inserted_at\",")
//                    .append(oc_table).append(".id as \"").append(oc_table).append(".id\", ")
//                    .append(oc_table).append(".policy_id as \"").append(oc_table).append(".policy_id\",")
//                    .append(oc_table).append(".attribute as \"").append(oc_table).append(".attribute\", ")
//                    .append(oc_table).append(".attribute_type as \"").append(oc_table).append(".attribute_type\", ")
//                    .append(oc_table).append(".operator as \"").append(oc_table).append(".operator\",")
//                    .append(oc_table).append(".comp_value as \"").append(oc_table).append(".comp_value\" ")
//                    .append("FROM ").append(policy_table).append(", ").append(oc_table).append(" ")
//                    .append("WHERE ").append(policy_table).append(".id = ").append(oc_table).append(".policy_id ");
//
//            List<Object> params = new ArrayList<>();
//
//            // (a) Querier filter
//            if (querier != null) {
//                sql.append("AND ").append(policy_table).append(".querier = ? ");
//                params.add(querier);
//            }
//
//            // (b) Enforcement action
//            if (enforcement_action != null) {
//                sql.append("AND ").append(policy_table).append(".enforcement_action = ? ");
//                params.add(enforcement_action);
//            }
//
//            // ---------- OPTIONAL FILTERS ----------
//            // 1) Location IN (...) OR no location condition (wildcard)
//            if (locations != null && !locations.isEmpty()) {
//                sql.append("AND (")
//                        .append(" EXISTS (")
//                        .append("   SELECT 1 FROM ").append(oc_table).append(" loc ")
//                        .append("   WHERE loc.policy_id = ").append(policy_table).append(".id ")
//                        .append("     AND loc.attribute = ? ")
//                        .append("     AND loc.operator = '=' ")
//                        .append("     AND loc.comp_value IN (");
//                params.add("location_id");
//
//                for (int i = 0; i < locations.size(); i++) {
//                    if (i > 0) sql.append(",");
//                    sql.append("?");
//                    params.add(locations.get(i));
//                }
//                sql.append(")) ")
//                        .append(" OR NOT EXISTS (")
//                        .append("   SELECT 1 FROM ").append(oc_table).append(" noloc ")
//                        .append("   WHERE noloc.policy_id = ").append(policy_table).append(".id ")
//                        .append("     AND noloc.attribute = 'location_id'")
//                        .append(") ")
//                        .append(") ");
//            }
//
//            // 2) TIME overlap (>= lower AND <= upper) OR no time condition (wildcard)
//            if (timeStart != null && timeEnd != null) {
//                sql.append("AND ((")
//                        .append(" EXISTS (SELECT 1 FROM ").append(oc_table).append(" tl ")
//                        .append("   WHERE tl.policy_id = ").append(policy_table).append(".id ")
//                        .append("     AND tl.attribute = ? ")
//                        .append("     AND tl.operator = '>=' ")
//                        .append("     AND tl.comp_value <= ?) ")
//                        .append(" AND EXISTS (SELECT 1 FROM ").append(oc_table).append(" tu ")
//                        .append("   WHERE tu.policy_id = ").append(policy_table).append(".id ")
//                        .append("     AND tu.attribute = ? ")
//                        .append("     AND tu.operator = '<=' ")
//                        .append("     AND tu.comp_value >= ?)) ")
//                        .append(" OR NOT EXISTS (SELECT 1 FROM ").append(oc_table).append(" notime ")
//                        .append("   WHERE notime.policy_id = ").append(policy_table).append(".id ")
//                        .append("     AND notime.attribute = 'start_time') ")
//                        .append(") ");
//                params.add("start_time");
//                params.add(timeEnd);   // "HH:mm:ss"
//                params.add("start_time");
//                params.add(timeStart); // "HH:mm:ss"
//            }
//
//            // 3) DATE overlap (>= lower AND <= upper) OR no date condition (wildcard)
//            if (dateStart != null && dateEnd != null) {
//                sql.append("AND ((")
//                        .append(" EXISTS (SELECT 1 FROM ").append(oc_table).append(" dl ")
//                        .append("   WHERE dl.policy_id = ").append(policy_table).append(".id ")
//                        .append("     AND dl.attribute = ? ")
//                        .append("     AND dl.operator = '>=' ")
//                        .append("     AND dl.comp_value <= ?) ")
//                        .append(" AND EXISTS (SELECT 1 FROM ").append(oc_table).append(" du ")
//                        .append("   WHERE du.policy_id = ").append(policy_table).append(".id ")
//                        .append("     AND du.attribute = ? ")
//                        .append("     AND du.operator = '<=' ")
//                        .append("     AND du.comp_value >= ?)) ")
//                        .append(" OR NOT EXISTS (SELECT 1 FROM ").append(oc_table).append(" nodate ")
//                        .append("   WHERE nodate.policy_id = ").append(policy_table).append(".id ")
//                        .append("     AND nodate.attribute = 'start_date') ")
//                        .append(") ");
//                params.add("start_date");
//                params.add(dateEnd);   // "YYYY-MM-DD"
//                params.add("start_date");
//                params.add(dateStart); // "YYYY-MM-DD"
//            }
//
//            sql.append(" ORDER BY ").append(policy_table).append(".id, ")
//                    .append(oc_table).append(".attribute, ")
//                    .append(oc_table).append(".comp_value");
//
//            queryStm = connection.prepareStatement(sql.toString());
//
//            // Bind params in order
//            int idx = 1;
//            for (Object o : params) {
//                queryStm.setString(idx++, String.valueOf(o));
//            }
//
//            ResultSet rs = queryStm.executeQuery();
//            if (!rs.next()) return null;
//
//            String next = null;
//            boolean skip = false;
//            List<QuerierCondition> querierConditions = new ArrayList<>();
//            List<ObjectCondition> objectConditions = new ArrayList<>();
//
//            // ---------- Assembly logic (as in your original) ----------
//            while (true) {
//                if (!skip) {
//                    id = rs.getString(policy_table + ".id");
//                    purpose = rs.getString(policy_table + ".purpose");
//                    action = rs.getString(policy_table + ".enforcement_action");
//                    inserted_at = rs.getTimestamp(policy_table + ".inserted_at");
//                    querier = rs.getString(policy_table + ".querier");
//
//                    querierConditions = new ArrayList<>();
//                    QuerierCondition qc1 = new QuerierCondition();
//                    qc1.setPolicy_id(id);
//                    qc1.setAttribute("policy_type");
//                    qc1.setType(AttributeType.STRING);
//                    List<BooleanPredicate> qbps1 = new ArrayList<>();
//                    BooleanPredicate qbp1 = new BooleanPredicate();
//                    qbp1.setOperator(Operation.EQ);
//                    qbp1.setValue(querier_type);
//                    qbps1.add(qbp1);
//                    qc1.setBooleanPredicates(qbps1);
//                    querierConditions.add(qc1);
//
//                    QuerierCondition qc2 = new QuerierCondition();
//                    qc2.setPolicy_id(id);
//                    qc2.setAttribute("querier");
//                    qc2.setType(AttributeType.STRING);
//                    List<BooleanPredicate> qbps2 = new ArrayList<>();
//                    BooleanPredicate qbp2 = new BooleanPredicate();
//                    qbp2.setOperator(Operation.EQ);
//                    qbp2.setValue(querier);
//                    qbps2.add(qbp2);
//                    qc2.setBooleanPredicates(qbps2);
//                    querierConditions.add(qc2);
//
//                    objectConditions = new ArrayList<>();
//                }
//
//                ObjectCondition oc = new ObjectCondition();
//                oc.setAttribute(rs.getString(oc_table + ".attribute"));
//                oc.setPolicy_id(rs.getString(oc_table + ".policy_id"));
//                oc.setType(AttributeType.valueOf(rs.getString(oc_table + ".attribute_type")));
//                List<BooleanPredicate> booleanPredicates = new ArrayList<>();
//                BooleanPredicate bp1 = new BooleanPredicate();
//                bp1.setOperator(convertOperator(rs.getString(oc_table + ".operator")));
//                bp1.setValue(rs.getString(oc_table + ".comp_value"));
//                rs.next();
//                BooleanPredicate bp2 = new BooleanPredicate();
//                bp2.setOperator(convertOperator(rs.getString(oc_table + ".operator")));
//                bp2.setValue(rs.getString(oc_table + ".comp_value"));
//                booleanPredicates.add(bp1);
//                booleanPredicates.add(bp2);
//                oc.setBooleanPredicates(booleanPredicates);
//                objectConditions.add(oc);
//
//                if (!rs.next()) {
//                    BEPolicy bePolicy = new BEPolicy(id, objectConditions, querierConditions, purpose, action, inserted_at);
//                    bePolicies.add(bePolicy);
//                    break;
//                }
//
//                next = rs.getString(policy_table + ".id");
//                if (!id.equalsIgnoreCase(next)) {
//                    BEPolicy bePolicy = new BEPolicy(id, objectConditions, querierConditions, purpose, action, inserted_at);
//                    bePolicies.add(bePolicy);
//                    skip = false;
//                } else {
//                    skip = true;
//                }
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//
//        return bePolicies;
//    }



    /**
     * Retrieves policies with optional filters (querier, action, locations, time, date) using a
     * WITH + SUM/CASE + HAVING query, then assembles BEPolicy objects exactly like the original.
     */
    public List<BEPolicy> retrievePolicies(
            String querier,
            String querier_type,
            String enforcement_action,
            List<String> locations,      // e.g., ["3144-clwa-4051","3142-clwa-2051"]; null/empty = no filter
            String timeStart, String timeEnd,   // e.g., "08:00:00" .. "17:00:00"; both non-null to enable
            String dateStart, String dateEnd    // e.g., "2018-02-01" .. "2018-03-10"; both non-null to enable
    ) {
        List<BEPolicy> bePolicies = new ArrayList<>();
        String id = null, purpose = null, action = null;
        Timestamp inserted_at = null;

        String policy_table = null, oc_table = null;
        if ("user".equalsIgnoreCase(querier_type)) {
            policy_table = "USER_POLICY";
            oc_table = "USER_POLICY_OBJECT_CONDITION";
        } else if ("group".equalsIgnoreCase(querier_type)) {
            policy_table = "GROUP_POLICY";
            oc_table = "GROUP_POLICY_OBJECT_CONDITION";
        } else {
            // fallback to user tables if not specified
            policy_table = "USER_POLICY";
            oc_table = "USER_POLICY_OBJECT_CONDITION";
        }

        boolean hasLocations = (locations != null && !locations.isEmpty());
        boolean hasTime = (timeStart != null && timeEnd != null);
        boolean hasDate = (dateStart != null && dateEnd != null);

        PreparedStatement queryStm = null;
        try {
            // ---------- Build SELECT with WITH + HAVING ----------
            StringBuilder sql = new StringBuilder();

            // CTE #1: filtered_policies
            sql.append("WITH filtered_policies AS (")
                    .append("  SELECT up.id")
                    .append("  FROM ").append(policy_table).append(" up")
                    .append("  WHERE 1=1 ");

            List<Object> params = new ArrayList<>();

            // Querier filter
            if (querier != null) {
                sql.append(" AND up.querier = ? ");
                params.add(querier);
            }
            // Enforcement action
            if (enforcement_action != null) {
                sql.append(" AND up.enforcement_action = ? ");
                params.add(enforcement_action);
            }
            sql.append(")");

            // -------- Build dynamic SUM/CASE block to keep SELECT shape stable --------
            StringBuilder sums = new StringBuilder();
            // Presence flags
            sums.append("    SUM(CASE WHEN c.attribute='location_id' THEN 1 ELSE 0 END) AS loc_has_any,\n")
                    .append("    SUM(CASE WHEN c.attribute='start_time' THEN 1 ELSE 0 END)  AS time_has_any,\n")
                    .append("    SUM(CASE WHEN c.attribute='start_date' THEN 1 ELSE 0 END)  AS date_has_any,\n");

            // Location OK counter (only used in HAVING if locations provided)
            if (hasLocations) {
                sums.append("    SUM(CASE WHEN c.attribute='location_id' AND (")
                        .append(" (c.operator='=' AND c.comp_value IN (");
                for (int i = 0; i < locations.size(); i++) {
                    if (i > 0) sums.append(",");
                    sums.append("?");
                    params.add(locations.get(i));
                }
                sums.append(")) OR (c.operator='*' OR c.comp_value='*')")
                        .append(" ) THEN 1 ELSE 0 END) AS loc_ok,\n");
            } else {
                // Keep column shape consistent when no locations are passed (won't be used in HAVING)
                sums.append("    SUM(CASE WHEN c.attribute='location_id' AND (c.operator='*' OR c.comp_value='*') THEN 1 ELSE 0 END) AS loc_ok,\n");
            }

            // Time lower/upper OK
            if (hasTime) {
                sums.append("    SUM(CASE WHEN c.attribute='start_time' AND ( (c.operator='>=' AND c.comp_value <= ?) OR c.operator='*' OR c.comp_value='*' ) THEN 1 ELSE 0 END) AS t_lower_ok,\n")
                        .append("    SUM(CASE WHEN c.attribute='start_time' AND ( (c.operator='<=' AND c.comp_value >= ?) OR c.operator='*' OR c.comp_value='*' ) THEN 1 ELSE 0 END) AS t_upper_ok,\n");
                params.add(timeEnd);   // used by the ">=" lower-bound check
                params.add(timeStart); // used by the "<=" upper-bound check
            } else {
                sums.append("    SUM(0) AS t_lower_ok,\n")
                        .append("    SUM(0) AS t_upper_ok,\n");
            }

            // Date lower/upper OK
            if (hasDate) {
                sums.append("    SUM(CASE WHEN c.attribute='start_date' AND ( (c.operator='>=' AND c.comp_value <= ?) OR c.operator='*' OR c.comp_value='*' ) THEN 1 ELSE 0 END) AS d_lower_ok,\n")
                        .append("    SUM(CASE WHEN c.attribute='start_date' AND ( (c.operator='<=' AND c.comp_value >= ?) OR c.operator='*' OR c.comp_value='*' ) THEN 1 ELSE 0 END) AS d_upper_ok\n");
                params.add(dateEnd);   // used by the ">=" lower-bound check
                params.add(dateStart); // used by the "<=" upper-bound check
            } else {
                sums.append("    SUM(0) AS d_lower_ok,\n")
                        .append("    SUM(0) AS d_upper_ok\n");
            }

            // CTE #2: qualifying_policies
            sql.append(", qualifying_policies AS (\n")
                    .append("  SELECT\n")
                    .append("    c.policy_id,\n")
                    .append(sums)
                    .append("  FROM ").append(oc_table).append(" c\n")
                    .append("  JOIN filtered_policies fp ON fp.id = c.policy_id\n")
                    .append("  GROUP BY c.policy_id\n")
                    .append("  HAVING 1=1 ");

            // Apply HAVING only for enabled filters
            if (hasLocations) {
                sql.append(" AND (loc_has_any = 0 OR loc_ok >= 1) ");
            }
            if (hasTime) {
                sql.append(" AND (time_has_any = 0 OR (t_lower_ok >= 1 AND t_upper_ok >= 1)) ");
            }
            if (hasDate) {
                sql.append(" AND (date_has_any = 0 OR (d_lower_ok >= 1 AND d_upper_ok >= 1)) ");
            }
            sql.append(")\n");

            // Final SELECT (aliases kept in the same format your mapper expects)
            sql.append("SELECT\n")
                    .append("  up.id AS \"").append(policy_table).append(".id\",\n")
                    .append("  up.querier AS \"").append(policy_table).append(".querier\",\n")
                    .append("  up.purpose AS \"").append(policy_table).append(".purpose\",\n")
                    .append("  up.enforcement_action AS \"").append(policy_table).append(".enforcement_action\",\n")
                    .append("  up.inserted_at AS \"").append(policy_table).append(".inserted_at\",\n")
                    .append("  oc.id AS \"").append(oc_table).append(".id\",\n")
                    .append("  oc.policy_id AS \"").append(oc_table).append(".policy_id\",\n")
                    .append("  oc.attribute AS \"").append(oc_table).append(".attribute\",\n")
                    .append("  oc.attribute_type AS \"").append(oc_table).append(".attribute_type\",\n")
                    .append("  oc.operator AS \"").append(oc_table).append(".operator\",\n")
                    .append("  oc.comp_value AS \"").append(oc_table).append(".comp_value\"\n")
                    .append("FROM qualifying_policies qp\n")
                    .append("JOIN ").append(policy_table).append(" up ON up.id = qp.policy_id\n")
                    .append("JOIN ").append(oc_table).append(" oc ON oc.policy_id = up.id\n")
                    .append("ORDER BY up.id, oc.attribute, oc.comp_value");

            queryStm = connection.prepareStatement(sql.toString());

            // Bind params in order
            int idx = 1;
            for (Object o : params) {
                queryStm.setString(idx++, String.valueOf(o));
            }

            ResultSet rs = queryStm.executeQuery();
            if (!rs.next()) return null;

            String next = null;
            boolean skip = false;
            List<QuerierCondition> querierConditions = new ArrayList<>();
            List<ObjectCondition> objectConditions = new ArrayList<>();

            // ---------- Assembly logic (unchanged) ----------
            while (true) {
                if (!skip) {
                    id = rs.getString(policy_table + ".id");
                    purpose = rs.getString(policy_table + ".purpose");
                    action = rs.getString(policy_table + ".enforcement_action");
                    inserted_at = rs.getTimestamp(policy_table + ".inserted_at");
                    querier = rs.getString(policy_table + ".querier");

                    querierConditions = new ArrayList<>();
                    QuerierCondition qc1 = new QuerierCondition();
                    qc1.setPolicy_id(id);
                    qc1.setAttribute("policy_type");
                    qc1.setType(AttributeType.STRING);
                    List<BooleanPredicate> qbps1 = new ArrayList<>();
                    BooleanPredicate qbp1 = new BooleanPredicate();
                    qbp1.setOperator(Operation.EQ);
                    qbp1.setValue(querier_type);
                    qbps1.add(qbp1);
                    qc1.setBooleanPredicates(qbps1);
                    querierConditions.add(qc1);

                    QuerierCondition qc2 = new QuerierCondition();
                    qc2.setPolicy_id(id);
                    qc2.setAttribute("querier");
                    qc2.setType(AttributeType.STRING);
                    List<BooleanPredicate> qbps2 = new ArrayList<>();
                    BooleanPredicate qbp2 = new BooleanPredicate();
                    qbp2.setOperator(Operation.EQ);
                    qbp2.setValue(querier);
                    qbps2.add(qbp2);
                    qc2.setBooleanPredicates(qbps2);
                    querierConditions.add(qc2);

                    objectConditions = new ArrayList<>();
                }

                ObjectCondition oc = new ObjectCondition();
                oc.setAttribute(rs.getString(oc_table + ".attribute"));
                oc.setPolicy_id(rs.getString(oc_table + ".policy_id"));
                oc.setType(AttributeType.valueOf(rs.getString(oc_table + ".attribute_type")));
                List<BooleanPredicate> booleanPredicates = new ArrayList<>();
                BooleanPredicate bp1 = new BooleanPredicate();
                bp1.setOperator(convertOperator(rs.getString(oc_table + ".operator")));
                bp1.setValue(rs.getString(oc_table + ".comp_value"));
                rs.next();
                BooleanPredicate bp2 = new BooleanPredicate();
                bp2.setOperator(convertOperator(rs.getString(oc_table + ".operator")));
                bp2.setValue(rs.getString(oc_table + ".comp_value"));
                booleanPredicates.add(bp1);
                booleanPredicates.add(bp2);
                oc.setBooleanPredicates(booleanPredicates);
                objectConditions.add(oc);

                if (!rs.next()) {
                    BEPolicy bePolicy = new BEPolicy(id, objectConditions, querierConditions, purpose, action, inserted_at);
                    bePolicies.add(bePolicy);
                    break;
                }

                next = rs.getString(policy_table + ".id");
                if (!id.equalsIgnoreCase(next)) {
                    BEPolicy bePolicy = new BEPolicy(id, objectConditions, querierConditions, purpose, action, inserted_at);
                    bePolicies.add(bePolicy);
                    skip = false;
                } else {
                    skip = true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return bePolicies;
    }






    public BEPolicy retrievePolicy(String policy_id, String querier_type) {
        String id = null, purpose = null, action = null, querier = null;
        Timestamp inserted_at = null;
        List<QuerierCondition> querierConditions = new ArrayList<>();
        List<ObjectCondition> objectConditions = new ArrayList<>();

        String policy_table = null, oc_table = null;
        if (querier_type.equalsIgnoreCase("user")) {
            policy_table = "USER_POLICY";
            oc_table = "USER_POLICY_OBJECT_CONDITION";
        } else if (querier_type.equalsIgnoreCase("group")) {
            policy_table = "GROUP_POLICY";
            oc_table = "GROUP_POLICY_OBJECT_CONDITION";
        }
        PreparedStatement queryStm = null;
        try {
            queryStm = connection.prepareStatement("SELECT " + policy_table + ".id, " + policy_table + ".querier, " + policy_table + ".purpose, " +
                    policy_table + ".enforcement_action," + policy_table + ".inserted_at," + oc_table + ".id, " + oc_table + " .policy_id," + oc_table + ".attribute, " +
                    oc_table + ".attribute_type, " + oc_table + ".operator," + oc_table + ".comp_value " +
                    "FROM " + policy_table + ", " + oc_table +
                    " WHERE " + policy_table + ".id = " + oc_table + ".policy_id " +
                    "AND " + policy_table + ".id=? ");
            queryStm.setString(1, policy_id);
            ResultSet rs = queryStm.executeQuery();
            boolean skip = false;
            while (rs.next()) {
                if (!skip) {
                    id = rs.getString(policy_table + ".id");
                    purpose = rs.getString(policy_table + ".purpose");
                    action = rs.getString(policy_table + ".enforcement_action");
                    inserted_at = rs.getTimestamp(policy_table + ".inserted_at");
                    querier = rs.getString(policy_table + ".querier");

                    querierConditions = new ArrayList<>();
                    QuerierCondition qc1 = new QuerierCondition();
                    qc1.setPolicy_id(id);
                    qc1.setAttribute("policy_type");
                    qc1.setType(AttributeType.STRING);
                    List<BooleanPredicate> qbps1 = new ArrayList<>();
                    BooleanPredicate qbp1 = new BooleanPredicate();
                    qbp1.setOperator(Operation.EQ);
                    qbp1.setValue(querier_type);
                    qbps1.add(qbp1);
                    qc1.setBooleanPredicates(qbps1);
                    querierConditions.add(qc1);
                    QuerierCondition qc2 = new QuerierCondition();
                    qc2.setPolicy_id(id);
                    qc2.setAttribute("querier");
                    qc2.setType(AttributeType.STRING);
                    List<BooleanPredicate> qbps2 = new ArrayList<>();
                    BooleanPredicate qbp2 = new BooleanPredicate();
                    qbp2.setOperator(Operation.EQ);
                    qbp2.setValue(querier);
                    qbps2.add(qbp2);
                    qc2.setBooleanPredicates(qbps2);
                    querierConditions.add(qc2);
                    objectConditions = new ArrayList<>();
                    skip = true;
                }
                ObjectCondition oc = new ObjectCondition();
                oc.setAttribute(rs.getString(oc_table + ".attribute"));
                oc.setPolicy_id(rs.getString(oc_table + ".policy_id"));
                oc.setType(AttributeType.valueOf(rs.getString(oc_table + ".attribute_type")));
                List<BooleanPredicate> booleanPredicates = new ArrayList<>();
                BooleanPredicate bp1 = new BooleanPredicate();
                bp1.setOperator(convertOperator(rs.getString(oc_table + ".operator")));
                bp1.setValue(rs.getString(oc_table + ".comp_value"));
                rs.next();
                BooleanPredicate bp2 = new BooleanPredicate();
                bp2.setOperator(convertOperator(rs.getString(oc_table + ".operator")));
                bp2.setValue(rs.getString(oc_table + ".comp_value"));
                booleanPredicates.add(bp1);
                booleanPredicates.add(bp2);
                oc.setBooleanPredicates(booleanPredicates);
                objectConditions.add(oc);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new BEPolicy(id, objectConditions, querierConditions, purpose, action, inserted_at);
    }


    public List<BEPolicy> retrievePoliciesUsingViews(Integer querier) {
        List<BEPolicy> bePolicies = new ArrayList<>();
        if (querier == null) return bePolicies;

        // View already sets purpose + enforcement_action and validates ranges/roles.
        String sql =
                "SELECT id, querier, purpose, \"ownerEq\", \"locEq\", start_date, end_date, start_time, end_time, enforcement_action " +
                        "FROM v_pr_attendance " +
                        "WHERE querier = ? " +
                        "ORDER BY id";

        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);


            ps.setInt(1, querier);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null; // match your old behavior

            while (true) {
                String policyId = rs.getString("id");
                String querierVal = String.valueOf(rs.getObject("querier"));

                // -------- QuerierConditions --------
                List<QuerierCondition> querierConditions = new ArrayList<>();

                QuerierCondition qcQuerier = new QuerierCondition();
                qcQuerier.setPolicy_id(policyId);
                qcQuerier.setAttribute("querier");
                qcQuerier.setType(AttributeType.STRING);

                BooleanPredicate qbp = new BooleanPredicate();
                qbp.setOperator(Operation.EQ);
                qbp.setValue(querierVal);

                qcQuerier.setBooleanPredicates(java.util.Collections.singletonList(qbp));
                querierConditions.add(qcQuerier);

                // (Optional) purpose as QC if your pipeline expects it there; otherwise remove this block.
                QuerierCondition qcPurpose = new QuerierCondition();
                qcPurpose.setPolicy_id(policyId);
                qcPurpose.setAttribute("purpose");
                qcPurpose.setType(AttributeType.STRING);

                BooleanPredicate pbp = new BooleanPredicate();
                pbp.setOperator(Operation.EQ);
                pbp.setValue(rs.getString("purpose"));

                qcPurpose.setBooleanPredicates(java.util.Collections.singletonList(pbp));
                querierConditions.add(qcPurpose);

                // -------- ObjectConditions --------
                List<ObjectCondition> objectConditions = new ArrayList<>();

                Integer ownerEq = (Integer) rs.getObject("ownerEq");
                String locEq = rs.getString("locEq");
                java.sql.Date startDate = rs.getDate("start_date");
                java.sql.Date endDate = rs.getDate("end_date");
                java.sql.Time startTime = rs.getTime("start_time");
                java.sql.Time endTime = rs.getTime("end_time");

                // ownerEq (keeps your “duplicate twice” behavior)
                if (ownerEq != null) {
                    ObjectCondition oc = new ObjectCondition();
                    oc.setAttribute("ownerEq");
                    oc.setPolicy_id(policyId);
                    oc.setType(AttributeType.INTEGER);

                    BooleanPredicate bp1 = new BooleanPredicate();
                    bp1.setOperator(Operation.EQ);
                    bp1.setValue(String.valueOf(ownerEq));

                    BooleanPredicate bp2 = new BooleanPredicate();
                    bp2.setOperator(Operation.EQ);
                    bp2.setValue(String.valueOf(ownerEq));

                    oc.setBooleanPredicates(java.util.Arrays.asList(bp1, bp2));
                    objectConditions.add(oc);
                }

                // locEq (duplicate twice)
                if (locEq != null) {
                    ObjectCondition oc = new ObjectCondition();
                    oc.setAttribute("locEq");
                    oc.setPolicy_id(policyId);
                    oc.setType(AttributeType.STRING);

                    BooleanPredicate bp1 = new BooleanPredicate();
                    bp1.setOperator(Operation.EQ);
                    bp1.setValue(locEq);

                    BooleanPredicate bp2 = new BooleanPredicate();
                    bp2.setOperator(Operation.EQ);
                    bp2.setValue(locEq);

                    oc.setBooleanPredicates(java.util.Arrays.asList(bp1, bp2));
                    objectConditions.add(oc);
                }

                // date range -> attribute "date" with GE/LE
                if (startDate != null || endDate != null) {
                    ObjectCondition oc = new ObjectCondition();
                    oc.setAttribute("date");
                    oc.setPolicy_id(policyId);
                    oc.setType(AttributeType.DATE);

                    BooleanPredicate bp1 = new BooleanPredicate();
                    BooleanPredicate bp2 = new BooleanPredicate();

                    if (startDate != null && endDate != null) {
                        bp1.setOperator(convertOperator("GE"));
                        bp1.setValue(startDate.toString());
                        bp2.setOperator(convertOperator("LE"));
                        bp2.setValue(endDate.toString());
                    } else if (startDate != null) {
                        bp1.setOperator(convertOperator("GE"));
                        bp1.setValue(startDate.toString());
                        bp2.setOperator(convertOperator("GE"));
                        bp2.setValue(startDate.toString());
                    } else {
                        bp1.setOperator(convertOperator("LE"));
                        bp1.setValue(endDate.toString());
                        bp2.setOperator(convertOperator("LE"));
                        bp2.setValue(endDate.toString());
                    }

                    oc.setBooleanPredicates(java.util.Arrays.asList(bp1, bp2));
                    objectConditions.add(oc);
                }

                // time range -> attribute "time" with GE/LE
                if (startTime != null || endTime != null) {
                    ObjectCondition oc = new ObjectCondition();
                    oc.setAttribute("time");
                    oc.setPolicy_id(policyId);
                    oc.setType(AttributeType.TIME);

                    BooleanPredicate bp1 = new BooleanPredicate();
                    BooleanPredicate bp2 = new BooleanPredicate();

                    if (startTime != null && endTime != null) {
                        bp1.setOperator(convertOperator("GE"));
                        bp1.setValue(startTime.toString());
                        bp2.setOperator(convertOperator("LE"));
                        bp2.setValue(endTime.toString());
                    } else if (startTime != null) {
                        bp1.setOperator(convertOperator("GE"));
                        bp1.setValue(startTime.toString());
                        bp2.setOperator(convertOperator("GE"));
                        bp2.setValue(startTime.toString());
                    } else {
                        bp1.setOperator(convertOperator("LE"));
                        bp1.setValue(endTime.toString());
                        bp2.setOperator(convertOperator("LE"));
                        bp2.setValue(endTime.toString());
                    }

                    oc.setBooleanPredicates(java.util.Arrays.asList(bp1, bp2));
                    objectConditions.add(oc);
                }

                // Build BEPolicy from view-provided purpose/action
                String policyPurpose = rs.getString("purpose");
                String action = rs.getString("enforcement_action");
                Timestamp inserted_at = new Timestamp(System.currentTimeMillis());

                BEPolicy bePolicy = new BEPolicy(policyId, objectConditions, querierConditions, policyPurpose, action, inserted_at);
                bePolicies.add(bePolicy);

                if (!rs.next()) break;
            }

        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        } finally {
            try { if (ps != null) ps.close(); } catch (SQLException ignored) {}
        }

        return bePolicies;
    }

    public List<BEPolicy> retrievePoliciesUsingMVViews(Integer querier) {
        List<BEPolicy> bePolicies = new ArrayList<>();
        if (querier == null) return bePolicies;

        // View already sets purpose + enforcement_action and validates ranges/roles.
        String sql =
                "SELECT id, querier, purpose, \"ownerEq\", \"locEq\", start_date, end_date, start_time, end_time, enforcement_action " +
                        "FROM mv_pr_attendance " +
                        "WHERE querier = ? " +
                        "ORDER BY id";

        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);


            ps.setInt(1, querier);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null; // match your old behavior

            while (true) {
                String policyId = rs.getString("id");
                String querierVal = String.valueOf(rs.getObject("querier"));

                // -------- QuerierConditions --------
                List<QuerierCondition> querierConditions = new ArrayList<>();

                QuerierCondition qcQuerier = new QuerierCondition();
                qcQuerier.setPolicy_id(policyId);
                qcQuerier.setAttribute("querier");
                qcQuerier.setType(AttributeType.STRING);

                BooleanPredicate qbp = new BooleanPredicate();
                qbp.setOperator(Operation.EQ);
                qbp.setValue(querierVal);

                qcQuerier.setBooleanPredicates(java.util.Collections.singletonList(qbp));
                querierConditions.add(qcQuerier);

                // (Optional) purpose as QC if your pipeline expects it there; otherwise remove this block.
                QuerierCondition qcPurpose = new QuerierCondition();
                qcPurpose.setPolicy_id(policyId);
                qcPurpose.setAttribute("purpose");
                qcPurpose.setType(AttributeType.STRING);

                BooleanPredicate pbp = new BooleanPredicate();
                pbp.setOperator(Operation.EQ);
                pbp.setValue(rs.getString("purpose"));

                qcPurpose.setBooleanPredicates(java.util.Collections.singletonList(pbp));
                querierConditions.add(qcPurpose);

                // -------- ObjectConditions --------
                List<ObjectCondition> objectConditions = new ArrayList<>();

                Integer ownerEq = (Integer) rs.getObject("ownerEq");
                String locEq = rs.getString("locEq");
                java.sql.Date startDate = rs.getDate("start_date");
                java.sql.Date endDate = rs.getDate("end_date");
                java.sql.Time startTime = rs.getTime("start_time");
                java.sql.Time endTime = rs.getTime("end_time");

                // ownerEq (keeps your “duplicate twice” behavior)
                if (ownerEq != null) {
                    ObjectCondition oc = new ObjectCondition();
                    oc.setAttribute("ownerEq");
                    oc.setPolicy_id(policyId);
                    oc.setType(AttributeType.INTEGER);

                    BooleanPredicate bp1 = new BooleanPredicate();
                    bp1.setOperator(Operation.EQ);
                    bp1.setValue(String.valueOf(ownerEq));

                    BooleanPredicate bp2 = new BooleanPredicate();
                    bp2.setOperator(Operation.EQ);
                    bp2.setValue(String.valueOf(ownerEq));

                    oc.setBooleanPredicates(java.util.Arrays.asList(bp1, bp2));
                    objectConditions.add(oc);
                }

                // locEq (duplicate twice)
                if (locEq != null) {
                    ObjectCondition oc = new ObjectCondition();
                    oc.setAttribute("locEq");
                    oc.setPolicy_id(policyId);
                    oc.setType(AttributeType.STRING);

                    BooleanPredicate bp1 = new BooleanPredicate();
                    bp1.setOperator(Operation.EQ);
                    bp1.setValue(locEq);

                    BooleanPredicate bp2 = new BooleanPredicate();
                    bp2.setOperator(Operation.EQ);
                    bp2.setValue(locEq);

                    oc.setBooleanPredicates(java.util.Arrays.asList(bp1, bp2));
                    objectConditions.add(oc);
                }

                // date range -> attribute "date" with GE/LE
                if (startDate != null || endDate != null) {
                    ObjectCondition oc = new ObjectCondition();
                    oc.setAttribute("date");
                    oc.setPolicy_id(policyId);
                    oc.setType(AttributeType.DATE);

                    BooleanPredicate bp1 = new BooleanPredicate();
                    BooleanPredicate bp2 = new BooleanPredicate();

                    if (startDate != null && endDate != null) {
                        bp1.setOperator(convertOperator("GE"));
                        bp1.setValue(startDate.toString());
                        bp2.setOperator(convertOperator("LE"));
                        bp2.setValue(endDate.toString());
                    } else if (startDate != null) {
                        bp1.setOperator(convertOperator("GE"));
                        bp1.setValue(startDate.toString());
                        bp2.setOperator(convertOperator("GE"));
                        bp2.setValue(startDate.toString());
                    } else {
                        bp1.setOperator(convertOperator("LE"));
                        bp1.setValue(endDate.toString());
                        bp2.setOperator(convertOperator("LE"));
                        bp2.setValue(endDate.toString());
                    }

                    oc.setBooleanPredicates(java.util.Arrays.asList(bp1, bp2));
                    objectConditions.add(oc);
                }

                // time range -> attribute "time" with GE/LE
                if (startTime != null || endTime != null) {
                    ObjectCondition oc = new ObjectCondition();
                    oc.setAttribute("time");
                    oc.setPolicy_id(policyId);
                    oc.setType(AttributeType.TIME);

                    BooleanPredicate bp1 = new BooleanPredicate();
                    BooleanPredicate bp2 = new BooleanPredicate();

                    if (startTime != null && endTime != null) {
                        bp1.setOperator(convertOperator("GE"));
                        bp1.setValue(startTime.toString());
                        bp2.setOperator(convertOperator("LE"));
                        bp2.setValue(endTime.toString());
                    } else if (startTime != null) {
                        bp1.setOperator(convertOperator("GE"));
                        bp1.setValue(startTime.toString());
                        bp2.setOperator(convertOperator("GE"));
                        bp2.setValue(startTime.toString());
                    } else {
                        bp1.setOperator(convertOperator("LE"));
                        bp1.setValue(endTime.toString());
                        bp2.setOperator(convertOperator("LE"));
                        bp2.setValue(endTime.toString());
                    }

                    oc.setBooleanPredicates(java.util.Arrays.asList(bp1, bp2));
                    objectConditions.add(oc);
                }

                // Build BEPolicy from view-provided purpose/action
                String policyPurpose = rs.getString("purpose");
                String action = rs.getString("enforcement_action");
                Timestamp inserted_at = new Timestamp(System.currentTimeMillis());

                BEPolicy bePolicy = new BEPolicy(policyId, objectConditions, querierConditions, policyPurpose, action, inserted_at);
                bePolicies.add(bePolicy);

                if (!rs.next()) break;
            }

        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        } finally {
            try { if (ps != null) ps.close(); } catch (SQLException ignored) {}
        }

        return bePolicies;
    }

    public List<BEPolicy> retrievePoliciesForFlatPolicyTable(Integer querier) {
        List<BEPolicy> bePolicies = new ArrayList<>();
        if (querier == null) return bePolicies;

        // View already sets purpose + enforcement_action and validates ranges/roles.
        String sql =
                "SELECT " +
                        "  fp.id, fp.querier, fp.purpose, fp.ownereq, fp.\"loceq\", " +
                        "  fp.datege, fp.datele, fp.timege, fp.timele, fp.enforcement_action " +
                        "FROM flat_policy fp " +
                        "JOIN app_user a ON fp.ownereq = a.id " +
                        "JOIN app_user q ON fp.querier = q.id " +
                        "WHERE fp.querier = ? " +
                        "  AND q.user_profile::text = 'faculty'::text " +
                        "  AND (fp.datege IS NULL OR fp.datele IS NULL OR fp.datege <= fp.datele) " +
                        "  AND (fp.timege IS NULL OR fp.timele IS NULL OR fp.timege <= fp.timele) " +
                        "ORDER BY fp.id";

        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);


            ps.setInt(1, querier);

            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null; // match your old behavior

            while (true) {
                String policyId = rs.getString("id");
                String querierVal = String.valueOf(rs.getObject("querier"));

                // -------- QuerierConditions --------
                List<QuerierCondition> querierConditions = new ArrayList<>();

                QuerierCondition qcQuerier = new QuerierCondition();
                qcQuerier.setPolicy_id(policyId);
                qcQuerier.setAttribute("querier");
                qcQuerier.setType(AttributeType.STRING);

                BooleanPredicate qbp = new BooleanPredicate();
                qbp.setOperator(Operation.EQ);
                qbp.setValue(querierVal);

                qcQuerier.setBooleanPredicates(java.util.Collections.singletonList(qbp));
                querierConditions.add(qcQuerier);

                // (Optional) purpose as QC if your pipeline expects it there; otherwise remove this block.
                QuerierCondition qcPurpose = new QuerierCondition();
                qcPurpose.setPolicy_id(policyId);
                qcPurpose.setAttribute("purpose");
                qcPurpose.setType(AttributeType.STRING);

                BooleanPredicate pbp = new BooleanPredicate();
                pbp.setOperator(Operation.EQ);
                pbp.setValue(rs.getString("purpose"));

                qcPurpose.setBooleanPredicates(java.util.Collections.singletonList(pbp));
                querierConditions.add(qcPurpose);

                // -------- ObjectConditions --------
                List<ObjectCondition> objectConditions = new ArrayList<>();

                Integer ownerEq = (Integer) rs.getObject("ownerEq");
                String locEq = rs.getString("locEq");
                java.sql.Date startDate = rs.getDate("datege");
                java.sql.Date endDate = rs.getDate("datele");
                java.sql.Time startTime = rs.getTime("timege");
                java.sql.Time endTime = rs.getTime("timele");

                // ownerEq (keeps your “duplicate twice” behavior)
                if (ownerEq != null) {
                    ObjectCondition oc = new ObjectCondition();
                    oc.setAttribute("user_id");
                    oc.setPolicy_id(policyId);
                    oc.setType(AttributeType.INTEGER);

                    BooleanPredicate bp1 = new BooleanPredicate();
                    bp1.setOperator(Operation.EQ);
                    bp1.setValue(String.valueOf(ownerEq));

                    BooleanPredicate bp2 = new BooleanPredicate();
                    bp2.setOperator(Operation.EQ);
                    bp2.setValue(String.valueOf(ownerEq));

                    oc.setBooleanPredicates(java.util.Arrays.asList(bp1, bp2));
                    objectConditions.add(oc);
                }

                // locEq (duplicate twice)
                if (locEq != null) {
                    ObjectCondition oc = new ObjectCondition();
                    oc.setAttribute("location_id");
                    oc.setPolicy_id(policyId);
                    oc.setType(AttributeType.STRING);

                    BooleanPredicate bp1 = new BooleanPredicate();
                    bp1.setOperator(Operation.EQ);
                    bp1.setValue(locEq);

                    BooleanPredicate bp2 = new BooleanPredicate();
                    bp2.setOperator(Operation.EQ);
                    bp2.setValue(locEq);

                    oc.setBooleanPredicates(java.util.Arrays.asList(bp1, bp2));
                    objectConditions.add(oc);
                }

                // date range -> attribute "date" with GE/LE
                if (startDate != null || endDate != null) {
                    ObjectCondition oc = new ObjectCondition();
                    oc.setAttribute("start_date");
                    oc.setPolicy_id(policyId);
                    oc.setType(AttributeType.DATE);

                    List<BooleanPredicate> preds = new ArrayList<>();

                    if (startDate != null) {
                        BooleanPredicate bp = new BooleanPredicate();
                        bp.setOperator(convertOperator(">="));
                        bp.setValue(startDate.toString());
                        preds.add(bp);
                    }

                    if (endDate != null) {
                        BooleanPredicate bp = new BooleanPredicate();
                        bp.setOperator(convertOperator("<="));
                        bp.setValue(endDate.toString());
                        preds.add(bp);
                    }

                    if (preds.size() == 1) {
                        BooleanPredicate copy = new BooleanPredicate();
                        copy.setOperator(preds.get(0).getOperator());
                        copy.setValue(preds.get(0).getValue());
                        preds.add(copy);
                    }

                    oc.setBooleanPredicates(preds);
                    objectConditions.add(oc);
                }

                // time range -> attribute "time" with GE/LE
                if (startTime != null || endTime != null) {
                    ObjectCondition oc = new ObjectCondition();
                    oc.setAttribute("start_time");
                    oc.setPolicy_id(policyId);
                    oc.setType(AttributeType.TIME);

                    List<BooleanPredicate> preds = new ArrayList<>();

                    if (startTime != null) {
                        BooleanPredicate bp = new BooleanPredicate();
                        bp.setOperator(convertOperator(">="));
                        bp.setValue(startTime.toString());
                        preds.add(bp);
                    }

                    if (endTime != null) {
                        BooleanPredicate bp = new BooleanPredicate();
                        bp.setOperator(convertOperator("<="));
                        bp.setValue(endTime.toString());
                        preds.add(bp);
                    }

                    if (preds.size() == 1) {
                        BooleanPredicate copy = new BooleanPredicate();
                        copy.setOperator(preds.get(0).getOperator());
                        copy.setValue(preds.get(0).getValue());
                        preds.add(copy);
                    }

                    oc.setBooleanPredicates(preds);
                    objectConditions.add(oc);
                }

                // Build BEPolicy from view-provided purpose/action
                String policyPurpose = rs.getString("purpose");
                String action = rs.getString("enforcement_action");
                Timestamp inserted_at = new Timestamp(System.currentTimeMillis());

                BEPolicy bePolicy = new BEPolicy(policyId, objectConditions, querierConditions, policyPurpose, action, inserted_at);
                bePolicies.add(bePolicy);

                if (!rs.next()) break;
            }

        } catch (SQLException | NumberFormatException e) {
            e.printStackTrace();
        } finally {
            try { if (ps != null) ps.close(); } catch (SQLException ignored) {}
        }

        return bePolicies;
    }





}
