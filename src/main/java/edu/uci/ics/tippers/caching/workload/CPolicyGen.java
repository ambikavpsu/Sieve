
package edu.uci.ics.tippers.caching.workload;

import edu.uci.ics.tippers.common.AttributeType;
import edu.uci.ics.tippers.common.PolicyConstants;
import edu.uci.ics.tippers.dbms.mysql.MySQLConnectionManager;
import edu.uci.ics.tippers.dbms.postgresql.PGSQLConnectionManager;
import edu.uci.ics.tippers.fileop.Writer;
import edu.uci.ics.tippers.generation.policy.WiFiDataSet.PolicyGroupGen;
import edu.uci.ics.tippers.generation.policy.WiFiDataSet.PolicyUtil;
import edu.uci.ics.tippers.model.data.UserProfile;
import edu.uci.ics.tippers.model.policy.*;
import edu.uci.ics.tippers.persistor.FlatPolicyPersistor;
import edu.uci.ics.tippers.persistor.PolicyPersistor;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.time.temporal.ChronoUnit;
public class CPolicyGen {

    private Connection connection;

    Random r;
    PolicyPersistor polper;
    PolicyUtil pg;

    private HashMap<Integer, List<String>> user_groups;
    private HashMap<Integer, String> user_profiles;
    private HashMap<String, List<Integer>> group_members;
    private HashMap<Integer, List<String>> location_clusters; //Forming random clusters of locations

    private TimeStampPredicate workingHours;
    private TimeStampPredicate nightDuskHours;
    private TimeStampPredicate nightDawnHours;

    private String START_WORKING_HOURS;
    private int DURATION_WORKING_HOURS;
    private String START_NIGHT_DUSK_HOURS;
    private String START_NIGHT_DAWN_HOURS;
    private int DURATION_NIGHT_DUSK_HOURS;
    private int DURATION_NIGHT_DAWN_HOURS;

    public CPolicyGen() {
//        connection = MySQLConnectionManager.getInstance().getConnection();
        connection = PGSQLConnectionManager.getInstance().getConnection();
        r = new Random();
        polper = PolicyPersistor.getInstance();
        pg = new PolicyUtil();

        location_clusters = new HashMap<>();
        List<String> all_locations = pg.getAllLocations();
        for (int i = 0; i < 10; i++) {
            List<String> locations = new ArrayList<>();
            location_clusters.put(i, locations);
        }
        for (String loc : all_locations) {
            int cluster = r.nextInt(10);
            location_clusters.get(cluster).add(loc);
        }


        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config/execution/wifi_policy_gen.properties");
            Properties props = new Properties();
            if (inputStream != null) {
                props.load(inputStream);
                START_WORKING_HOURS = props.getProperty("w_start");
                DURATION_WORKING_HOURS = 120;
                workingHours = new TimeStampPredicate(pg.getDate("MIN"), pg.getDate("MAX"), START_WORKING_HOURS, DURATION_WORKING_HOURS);
                START_NIGHT_DUSK_HOURS = props.getProperty("n_dusk_start");
                DURATION_NIGHT_DUSK_HOURS = Integer.parseInt(props.getProperty("n_dusk_plus"));
                nightDuskHours = new TimeStampPredicate(pg.getDate("MIN"), pg.getDate("MAX"), START_NIGHT_DUSK_HOURS, DURATION_NIGHT_DUSK_HOURS);
                START_NIGHT_DAWN_HOURS = props.getProperty("n_dawn_start");
                DURATION_NIGHT_DAWN_HOURS = Integer.parseInt(props.getProperty("n_dawn_plus"));
                nightDawnHours = new TimeStampPredicate(pg.getDate("MIN"), pg.getDate("MAX"), START_NIGHT_DAWN_HOURS, DURATION_NIGHT_DAWN_HOURS);
            }
        } catch (IOException ie) {
            ie.printStackTrace();
        }
    }


    // Function select an element based on index and return an element
    private List<String> includeLocation(){
        if (Math.random() > (float) 1/location_clusters.size()){
            return location_clusters.get(r.nextInt(10));
        }
        else return null;
    }

    private LocalTime generateRandomStartTime(){
        final LocalTime START_RANGE = LocalTime.of(8, 0); // 8 AM
        final LocalTime END_RANGE = LocalTime.of(15, 0); // 3 PM
        final int INCREMENT_MINUTES = 30;
        Random random = new Random();
        int totalIncrements = (int) ChronoUnit.MINUTES.between(START_RANGE, END_RANGE) / INCREMENT_MINUTES;
        int randomIncrement = random.nextInt(totalIncrements + 1);
        return START_RANGE.plusMinutes(randomIncrement * INCREMENT_MINUTES);
    }

    private LocalTime generateRandomStartTimeVisitorSU(){
        final LocalTime START_RANGE = LocalTime.of(9, 0);
        final LocalTime END_RANGE = LocalTime.of(17, 0);
        final int INCREMENT_MINUTES = 30;
        Random random = new Random();
        int totalIncrements = (int) ChronoUnit.MINUTES.between(START_RANGE, END_RANGE) / INCREMENT_MINUTES;
        int randomIncrement = random.nextInt(totalIncrements + 1);
        return START_RANGE.plusMinutes(randomIncrement * INCREMENT_MINUTES);
    }

    private LocalTime generateRandomStartTimeSU(){
        final LocalTime START_RANGE = LocalTime.of(0, 0); // Midnight
        final LocalTime END_RANGE = LocalTime.of(23, 59); // Midnight
        final int INCREMENT_MINUTES = 30;
        Random random = new Random();
        int totalIncrements = (int) ChronoUnit.MINUTES.between(START_RANGE, END_RANGE) / INCREMENT_MINUTES;
        int randomIncrement = random.nextInt(totalIncrements + 1);
        return START_RANGE.plusMinutes(randomIncrement * INCREMENT_MINUTES);
    }

    public static LocalDate getRandomDateBetween(LocalDate startDate, LocalDate endDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        long randomDays = ThreadLocalRandom.current().nextLong(0, daysBetween + 1); // +1 to include endDate
        return startDate.plusDays(randomDays);
    }

    public static int getRandomDuration() {
        Random r = new Random();
        int duration = r.nextInt(300);
        if (duration < 30) {
            getRandomDuration();
        }
        return duration;
    }

    public BEPolicy generateRandomPolicies(int querier, int owner_id, String owner_group, String owner_profile,
                                     TimeStampPredicate tsPred, String location, String action, int flag) {
        String policyID = UUID.randomUUID().toString();
        List<QuerierCondition> querierConditions = new ArrayList<>(Arrays.asList(
                new QuerierCondition(policyID, "policy_type", AttributeType.STRING, Operation.EQ, "user"),
                new QuerierCondition(policyID, "querier", AttributeType.STRING, Operation.EQ, String.valueOf(querier))));
        List<ObjectCondition> objectConditions = new ArrayList<>();
        if (owner_id != 0) {
            ObjectCondition owner = new ObjectCondition(policyID, PolicyConstants.USERID_ATTR, AttributeType.STRING,
                    String.valueOf(owner_id), Operation.EQ);
            objectConditions.add(owner);
        }
        if (owner_group != null) {
            ObjectCondition ownerGroup = new ObjectCondition(policyID, PolicyConstants.GROUP_ATTR, AttributeType.STRING,
                    owner_group, Operation.EQ);
            objectConditions.add(ownerGroup);
        }
        if (owner_profile != null) {
            ObjectCondition ownerProfile = new ObjectCondition(policyID, PolicyConstants.PROFILE_ATTR, AttributeType.STRING,
                    owner_profile, Operation.EQ);
            objectConditions.add(ownerProfile);
        }
        if (tsPred != null) {
            ObjectCondition datePred = new ObjectCondition(policyID, PolicyConstants.START_DATE, AttributeType.DATE,
                    tsPred.getStartDate().toString(), Operation.GTE, tsPred.getEndDate().toString(), Operation.LTE);
            objectConditions.add(datePred);
            ObjectCondition timePred = new ObjectCondition(policyID, PolicyConstants.START_TIME, AttributeType.TIME,
                    tsPred.parseStartTime(), Operation.GTE, tsPred.parseEndTime(), Operation.LTE);
            objectConditions.add(timePred);
        }
        if (location != null) {
            ObjectCondition locationPred = new ObjectCondition(policyID, PolicyConstants.LOCATIONID_ATTR, AttributeType.STRING,
                    location, Operation.EQ);
            objectConditions.add(locationPred);
        }
        if (objectConditions.isEmpty()) {
            System.out.println("Empty Object Conditions");
        }
        if(flag == 1){
            return new BEPolicy(policyID, objectConditions, querierConditions, "attendance-control",
                    action, new Timestamp(System.currentTimeMillis()));
        }else {
            return new BEPolicy(policyID, objectConditions, querierConditions, "space-usage",
                    action, new Timestamp(System.currentTimeMillis()));
        }

    }


    /*
    This function generates policies for all the users sampled by CUserGen class.
    Variable numPolicies indicates the # of policies defined by each user
    Calling the function generateRandomPolicies with flag == 1, means we are generating policies for AC scenario
    Policy Holders can only be students(undergrad, graduate)
    Querier is faculty
    Location can only be classrooms
     */
    public List<BEPolicy> generatePoliciesforAC(List<CUserGen.User> users){

        List<BEPolicy> policies = new ArrayList<>();

        for (CUserGen.User user: users){

            int numPolicies = 10;
             for (int i = 0; i < numPolicies; i++) {
                workingHours.setStartTime(generateRandomStartTime());
                workingHours.setEndTime(workingHours.getStartTime().plus(120, ChronoUnit.MINUTES));
                if (i<numPolicies){

                    if(user.getUserProfile().equals("graduate")){
                        List<Integer> possibleQueriers = new ArrayList<>();
                        for (CUserGen.User u : users) {
                            if (u.getUserProfile().equals("faculty") && user.getUserGroup().equals(u.getUserGroup())) {
                                possibleQueriers.add(u.getId());
                            }
                        }
                        Random r = new Random();
                        int index = r.nextInt(possibleQueriers.size());
                        BEPolicy policy = generateRandomPolicies(possibleQueriers.get(index),user.getId(),
                                user.getUserGroup(),user.getUserProfile(), workingHours, user.getUserGroup(),
                                PolicyConstants.ACTION_ALLOW, 1);
                        policies.add(policy);

                    }
                    if(user.getUserProfile().equals("undergrad")){
                        List<Integer> possibleQueriers = new ArrayList<>();
                        for (CUserGen.User u : users) {
                            if (u.getUserProfile().equals("faculty") && user.getUserGroup().equals(u.getUserGroup())) {
                                possibleQueriers.add(u.getId());
                            }
                        }
                        Random r = new Random();
                        int index = r.nextInt(possibleQueriers.size());
                        BEPolicy policy = generateRandomPolicies(possibleQueriers.get(index),user.getId(),
                                user.getUserGroup(),user.getUserProfile(), workingHours, user.getUserGroup(),
                                PolicyConstants.ACTION_ALLOW, 1);
                        policies.add(policy);

                    }
                }else {
                    List<Integer> possibleQueriers = new ArrayList<>();
                    for (CUserGen.User u : users) {
                        if (u != user) {
                            possibleQueriers.add(u.getId());
                        }
                    }
                    Random r = new Random();
                    int index = r.nextInt(possibleQueriers.size());
                    BEPolicy policy = generateRandomPolicies(possibleQueriers.get(index), user.getId(),
                            user.getUserGroup(), user.getUserProfile(), workingHours, null,
                            PolicyConstants.ACTION_ALLOW, 1);
                    policies.add(policy);
                }
            }
        }

        /*
        To print policies generated and store it in the DB
         */
//        for (BEPolicy policy : policies) {
//            System.out.println(policy.toString());
//        }
//        System.out.println();
//        polper.insertPolicy(policies);
        return policies;
    }

    public List<BEPolicy> generatePoliciesPerQueriesforAC(List<CUserGen.User> users, int numPolicies){

        List<BEPolicy> policies = new ArrayList<>();
        List<Integer> possibleQueriers = new ArrayList<>();
        FlatPolicyPersistor flatpolper = new FlatPolicyPersistor();
        possibleQueriers.add(177);

        Writer writer = new Writer();
        StringBuilder row = new StringBuilder();
        String fileName = "policiesPGSQL.csv";
        boolean first = true;

        System.out.println("Running Policy Insertion Experiment");

        // Write header once
        String header = String.join(",",
                "user_id",
                "faculty_id",
                "course_name",
                "loceq",
                "start_date",
                "end_date",
                "start_time",
                "end_time"
        ) + "\n";
        writer.writeString(header, PolicyConstants.EXP_RESULTS_DIR, fileName);

//        for (CUserGen.User u : users) {
//            if (u.getUserProfile().equals("faculty")) {
//                possibleQueriers.add(u.getId());
//            }
//        }
        System.out.println("Total no. of Queriers: " + possibleQueriers.size());
        for (int i=0; i< possibleQueriers.size(); i++){

            for (int j = 0; j < numPolicies; j++) {
//                workingHours.setStartTime(generateRandomStartTime());
//                workingHours.setEndTime(workingHours.getStartTime().plus(120, ChronoUnit.MINUTES));

                boolean flag = false;
                while(!flag){
                    Random r = new Random();
                    int index = r.nextInt(users.size());
                    CUserGen.User user = users.get(index);
                    int randomDuration = getRandomDuration();
                    boolean duration = false;
                    if(user.getUserProfile().equals("undergrad") || user.getUserProfile().equals("graduate")){

                        workingHours.setStartTime(generateRandomStartTimeSU());
                        while (duration == false) {
                            if (workingHours.getStartTime().toSecondOfDay() + randomDuration < (24 * 60 * 60)) {
                                workingHours.setEndTime(workingHours.getStartTime().plus(randomDuration, ChronoUnit.MINUTES));
                                duration = true;
                            }
                            randomDuration = getRandomDuration();
                        }
                        LocalDate startSU = LocalDate.of(2018, 02, 01);
                        LocalDate endSU = LocalDate.of(2018, 04, 30);
                        LocalDate randomSUStart = getRandomDateBetween(startSU, endSU);
                        LocalDate randomSUEnd = getRandomDateBetween(randomSUStart, endSU);
                        workingHours.setStartDate(randomSUStart);
                        workingHours.setEndDate(randomSUEnd);


                        BEPolicy policy = generateRandomPolicies(possibleQueriers.get(i), user.getId(),
                                user.getUserGroup(), user.getUserProfile(), workingHours, user.getUserGroup(),
                                PolicyConstants.ACTION_ALLOW, 1);
                        policies.add(policy);
                        row.append(policy.fetchOwner()).append(",")
                                .append(policy.fetchQuerier()).append(",")
                                .append("analysis").append(",")
                                .append(policy.fetchLocation()).append(",")
                                .append(policy.fetchDate().get(0)).append(",")
                                .append(policy.fetchDate().get(1)).append(",")
                                .append(policy.fetchTime().get(0)).append(",")
                                .append(policy.fetchTime().get(1))
                                .append("\n");
                        // Writing results to file
                        if (!first) writer.writeString(row.toString(), PolicyConstants.EXP_RESULTS_DIR, fileName);
                        else first = false;

                        // Clearing StringBuilder for the next iteration
                        row.setLength(0);
                        flag = true;
                    }
                }


            }
        }

        /*
        To print policies generated and store it in the DB
         */
        for (BEPolicy policy : policies) {
            System.out.println(policy.toString());
        }
        System.out.println();
//        polper.insertPolicy(policies);
        flatpolper.insertPolicies(policies);
        return policies;
    }

//   Generating default policies using template, hence sharing the same structure
    public List<BEPolicy> generateDefaultPoliciesPerQueriesforAC(List<CUserGen.User> users, int numPolicies){

        List<BEPolicy> policies = new ArrayList<>();
        List<Integer> possibleQueriers = new ArrayList<>();
        FlatPolicyPersistor flatpolper = new FlatPolicyPersistor();
        FlatPolicyPersistor defaultpolper = new FlatPolicyPersistor();

        possibleQueriers.add(177);

        System.out.println("Running Policy Insertion Experiment");

//        for (CUserGen.User u : users) {
//            if (u.getUserProfile().equals("faculty")) {
//                possibleQueriers.add(u.getId());
//            }
//        }
        System.out.println("Total no. of Queriers: " + possibleQueriers.size());
        for (int i = 0; i < possibleQueriers.size(); i++) {
            List<CUserGen.User> tempUsers = new ArrayList<>(users);
            Collections.shuffle(tempUsers);

            int count = 0;
            for (CUserGen.User user : tempUsers) {
                if (count >= numPolicies) break;

                if (user.getUserProfile().equals("undergrad") || user.getUserProfile().equals("graduate")) {
                    workingHours.setStartTime(LocalTime.of(9,0));
                    workingHours.setEndTime(LocalTime.of(12,0));
                    workingHours.setStartDate(LocalDate.of(2018,2,1));
                    workingHours.setEndDate(LocalDate.of(2018,4,30));

                    BEPolicy policy = generateRandomPolicies(
                            possibleQueriers.get(i),
                            user.getId(),
                            user.getUserGroup(),
                            user.getUserProfile(),
                            workingHours,
                            user.getUserGroup(),
                            PolicyConstants.ACTION_ALLOW,
                            1
                    );
                    policies.add(policy);

                    count++;
                }
            }
        }

        /*
        To print policies generated and store it in the DB
         */
        for (BEPolicy policy : policies) {
            System.out.println(policy.toString());
        }
        System.out.println();

        flatpolper.insertPolicies(policies);
        if (!policies.isEmpty()) {
            defaultpolper.insertPoliciesEnrollment(policies);
        }

        System.out.println("Default policies count: " + policies.size());
        return policies;
    }

    /* Generates a mixed policy set for attendance control by first creating a pool of
     default policies with fixed date/time windows, then adding custom policies on top
     with relaxed time windows. Only default policies are written to the CSV file,
     custom policies are inserted through custompolper, and the full set
     (default + custom) is inserted into the flat policy table.
     */
    public List<BEPolicy> generateDefaultAndCustomPoliciesPerQueriesforAC(
            List<CUserGen.User> users,
            int numDefaultPolicies,
            double customizationPercent) {

        List<BEPolicy> defaultPolicies = new ArrayList<>();
        List<BEPolicy> customPolicies = new ArrayList<>();
        List<BEPolicy> allPolicies = new ArrayList<>();
        List<Integer> possibleQueriers = new ArrayList<>();

        FlatPolicyPersistor flatpolper = new FlatPolicyPersistor();
        FlatPolicyPersistor custompolper = new FlatPolicyPersistor();
        FlatPolicyPersistor defaultpopler = new FlatPolicyPersistor();

        Writer writer = new Writer();
        StringBuilder row = new StringBuilder();
        String fileName = "defaultPoliciesPGSQL.csv";

        Random random = new Random();
        possibleQueriers.add(177);

        System.out.println("Running Default + Custom Policy Insertion Experiment");

        List<CUserGen.User> eligibleUsers = new ArrayList<>();
        Map<Integer, CUserGen.User> userMap = new HashMap<>();

        for (CUserGen.User u : users) {
            userMap.put(u.getId(), u);
            if (u.getUserProfile().equals("undergrad") || u.getUserProfile().equals("graduate")) {
                eligibleUsers.add(u);
            }
        }

        if (eligibleUsers.isEmpty()) {
            System.out.println("No eligible users found.");
            return allPolicies;
        }

        Collections.shuffle(eligibleUsers);

        int querier = possibleQueriers.get(0);
        int actualDefaultCount = Math.min(numDefaultPolicies, eligibleUsers.size());

        /*
         * Generates a mixed policy set for attendance control by first creating a pool of
         * default policies with fixed date/time windows, then adding custom policies on top
         * with relaxed time windows. Only default policies are written to the CSV file,
         * custom policies are inserted through custompolper, and the full set
         * (default + custom) is inserted into the flat policy table.
         */

        /*
         * Generate default policies
         * These are written to file and later also inserted into flat_policy
         */
        for (int i = 0; i < actualDefaultCount; i++) {
            CUserGen.User user = eligibleUsers.get(i);

            TimeStampPredicate wh = new TimeStampPredicate(
                    LocalDate.of(2018, 2, 1),
                    LocalDate.of(2018, 4, 30),
                    "09:00:00",
                    180
            );

            BEPolicy policy = generateRandomPolicies(
                    querier,
                    user.getId(),
                    user.getUserGroup(),
                    user.getUserProfile(),
                    wh,
                    user.getUserGroup(),
                    PolicyConstants.ACTION_ALLOW,
                    1
            );

            defaultPolicies.add(policy);
        }

        /*
         * Generate custom policies as add-ons
         * Example: 100 defaults + 10% customization = 10 extra custom policies
         * These are NOT written to file
         */
        int numCustomPolicies = (int) Math.floor(actualDefaultCount * customizationPercent / 100.0);

        for (int i = 0; i < numCustomPolicies; i++) {
            BEPolicy basePolicy = defaultPolicies.get(random.nextInt(defaultPolicies.size()));
            int ownerId = basePolicy.fetchOwner();
            CUserGen.User baseUser = userMap.get(ownerId);

            LocalDate startDate = basePolicy.fetchDate().get(0).toLocalDate();
            LocalDate endDate = basePolicy.fetchDate().get(1).toLocalDate();

            String customStart;
            int customDuration;

            // Current implementation: choose from a few predefined relaxed windows
            // for custom policies. This is intentionally simple.
            // This block can later be replaced with a randomized time-window generator.
            int mode = random.nextInt(3);
            switch (mode) {
                case 0:
                    customStart = "08:00:00";
                    customDuration = 240; // 08:00 - 12:00
                    break;
                case 1:
                    customStart = "09:00:00";
                    customDuration = 240; // 09:00 - 13:00
                    break;
                default:
                    customStart = "08:00:00";
                    customDuration = 300; // 08:00 - 13:00
                    break;
            }

            TimeStampPredicate wh = new TimeStampPredicate(
                    startDate,
                    endDate,
                    customStart,
                    customDuration
            );

            BEPolicy customPolicy = generateRandomPolicies(
                    querier,
                    baseUser.getId(),
                    baseUser.getUserGroup(),
                    baseUser.getUserProfile(),
                    wh,
                    baseUser.getUserGroup(),
                    PolicyConstants.ACTION_ALLOW,
                    1
            );

            customPolicies.add(customPolicy);
        }

        /*
         * Build the final policy set
         */
        allPolicies.addAll(defaultPolicies);
        allPolicies.addAll(customPolicies);

        for (BEPolicy policy : allPolicies) {
            System.out.println(policy.toString());
        }

        System.out.println("Default policies count: " + defaultPolicies.size());
        System.out.println("Custom policies count: " + customPolicies.size());
        System.out.println("Total policies count: " + allPolicies.size());

        /*
         * Persist policies
         * - only custom policies go to custom policy store
         * - all policies go to flat policy store
         */
        if (!customPolicies.isEmpty()) {
            custompolper.insertCustomPolicies(customPolicies);
        }

        if (!allPolicies.isEmpty()) {
            flatpolper.insertPolicies(allPolicies);
        }

        if (!defaultPolicies.isEmpty()) {
            defaultpopler.insertPoliciesEnrollment(defaultPolicies);
        }

        return allPolicies;
    }




    /*
    This function generates policies for all the users sampled by CUserGen class.
    Variable numPolicies indicates the # of policies defined by each user
    Calling the function generateRandomPolicies with flag == 2, means we are generating policies for SU scenario
    Policy Holders can only be anyone
    Querier is faculty or staff
    Location can only be anything except for the forbidden locations
     */
    public List<BEPolicy> generatePoliciesforSU(List<CUserGen.User> users){

        List<BEPolicy> policies = new ArrayList<>();
        List<Integer> possibleQueriers = new ArrayList<>();
        List<String> all_locations_SU = pg.getAllLocations();
//        for(int i = 0; i < all_locations_SU.size(); i++) {
//            System.out.println("Location: " + all_locations_SU.get(i));
//        }
        for (CUserGen.User u : users) {
            if (u.getUserProfile().equals("faculty")) {
                possibleQueriers.add(u.getId());
            }
            if (u.getUserProfile().equals("staff")) {
                possibleQueriers.add(u.getId());
            }
        }
//        for(int i = 0; i < possibleQueriers.size(); i++) {
//            System.out.println("Querier: " + possibleQueriers.get(i));
//        }
//        System.out.println("Size: " + possibleQueriers.size());
//        int count = 0;
        for (CUserGen.User user: users) {
            int numPolicies = 10;
            for (int i = 0; i < numPolicies; i++) {
                int randomDuration = getRandomDuration();
                boolean duration = false;
//                   for (CUserGen.User u : users) {
                if(user.getUserProfile().equals("visitor")){
                    workingHours.setStartTime(generateRandomStartTimeVisitorSU());
                    while (duration == false) {
                        if (workingHours.getStartTime().toSecondOfDay() + randomDuration < (24 * 60 * 60)) {
                            workingHours.setEndTime(workingHours.getStartTime().plus(randomDuration, ChronoUnit.MINUTES));
                            duration = true;
                        }
                        randomDuration = getRandomDuration();
                    }
                    LocalDate startSU = LocalDate.of(2018, 02, 01);
                    LocalDate endSU = LocalDate.of(2018, 04, 30);
                    LocalDate randomSUStart = getRandomDateBetween(startSU, endSU);
                    LocalDate randomSUEnd = getRandomDateBetween(randomSUStart, endSU);
                    workingHours.setStartDate(randomSUStart);
                    workingHours.setEndDate(randomSUEnd);
                    Random r = new Random();
                    int index = r.nextInt(possibleQueriers.size());
                    int locIndex = r.nextInt(all_locations_SU.size());
                    BEPolicy policy = generateRandomPolicies(possibleQueriers.get(index),user.getId(), user.getUserGroup(),user.getUserProfile(), workingHours, all_locations_SU.get(locIndex), PolicyConstants.ACTION_ALLOW, 2);
                    policies.add(policy);
//                    count++;

//                    System.out.println("Policy & Count: " + count + " " + policy.toString());
                }
                else {
                    workingHours.setStartTime(generateRandomStartTimeSU());
                    while (duration == false) {
                        if (workingHours.getStartTime().toSecondOfDay() + randomDuration < (24 * 60 * 60)) {
                            workingHours.setEndTime(workingHours.getStartTime().plus(randomDuration, ChronoUnit.MINUTES));
                            duration = true;
                        }
                        randomDuration = getRandomDuration();
                    }
                    LocalDate startSU = LocalDate.of(2018, 02, 01);
                    LocalDate endSU = LocalDate.of(2018, 04, 30);
                    LocalDate randomSUStart = getRandomDateBetween(startSU, endSU);
                    LocalDate randomSUEnd = getRandomDateBetween(randomSUStart, endSU);
                    workingHours.setStartDate(randomSUStart);
                    workingHours.setEndDate(randomSUEnd);
                    Random r = new Random();
                    int index = r.nextInt(possibleQueriers.size());
                    int locIndex = r.nextInt(all_locations_SU.size());
                    BEPolicy policy = generateRandomPolicies(possibleQueriers.get(index), user.getId(),
                            user.getUserGroup(), user.getUserProfile(), workingHours, all_locations_SU.get(locIndex),
                            PolicyConstants.ACTION_ALLOW, 2);
                    policies.add(policy);
     //               count++;
//                    System.out.println("Policy & Count: " + count + " " + policy.toString());
//                  }
                }
//                    else {
//                        List<Integer> possibleQueriers = new ArrayList<>();
//                        for (CUserGen.User u : users) {
//                            if (u != user) {
//                                possibleQueriers.add(u.getId());
//                            }
//                        }
//                        Random r = new Random();
//                        int index = r.nextInt(possibleQueriers.size());
//                        BEPolicy policy = generateRandomPolicies(possibleQueriers.get(index), user.getId(),
//                                user.getUserGroup(), user.getUserProfile(), workingHours, null,
//                                PolicyConstants.ACTION_ALLOW, 2);
//                        policies.add(policy);
//                    }

            }
        }
        /*
        To print policies generated and store it in the DB
         */
        for (BEPolicy policy : policies) {
            System.out.println(policy.toString());
        }
        System.out.println();
        polper.insertPolicy(policies);
        return policies;
    }

    /**
     * Generates a simple validation workload of default policies for attendance-control.
     *
     * Each generated policy always includes the owner/user-id predicate, while the
     * remaining predicates are varied using a small hard-coded pattern so that the
     * workload contains policies of different sizes. This is intended for early
     * validation experiments to study how policy size affects query execution in
     * Plan A, Plan B, and Plan C.
     *
     * Omitted predicates are passed as null and will not be added to the policy.
     */
    public List<BEPolicy> generateValidationPolicyWorkloadForAC(List<CUserGen.User> users, int numPolicies) {

        List<BEPolicy> policies = new ArrayList<>();
        List<Integer> possibleQueriers = new ArrayList<>();
        FlatPolicyPersistor flatpolper = new FlatPolicyPersistor();
        FlatPolicyPersistor defaultpolper = new FlatPolicyPersistor();

        possibleQueriers.add(177);

        System.out.println("Running Policy Insertion Experiment");
        System.out.println("Total no. of Queriers: " + possibleQueriers.size());

        for (int i = 0; i < possibleQueriers.size(); i++) {
            List<CUserGen.User> tempUsers = new ArrayList<>(users);
            Collections.shuffle(tempUsers);

            int count = 0;
            for (CUserGen.User user : tempUsers) {
                if (count >= numPolicies) break;

                if (user.getUserProfile().equals("undergrad") || user.getUserProfile().equals("graduate")) {

                    int shape = count % 6;

                    String ownerGroup = null;
                    String ownerProfile = null;
                    String location = null;
                    TimeStampPredicate tsPred = null;

                    if (shape == 0) {
                        // user-id only
                    }
                    else if (shape == 1) {
                        // user-id + location
                        location = user.getUserGroup();
                    }
                    else if (shape == 2) {
                        // user-id + date only
                        tsPred = new TimeStampPredicate(
                                LocalDate.of(2018, 2, 1),
                                LocalDate.of(2018, 4, 30),
                                "09:00:00",
                                180
                        );
                        tsPred.setStartTime(null);
                        tsPred.setEndTime(null);
                    }
                    else if (shape == 3) {
                        // user-id + time only
                        tsPred = new TimeStampPredicate(
                                LocalDate.of(2018, 2, 1),
                                LocalDate.of(2018, 4, 30),
                                "09:00:00",
                                180
                        );
                        tsPred.setStartDate(null);
                        tsPred.setEndDate(null);
                    }
                    else if (shape == 4) {
                        // user-id + date + time
                        tsPred = new TimeStampPredicate(
                                LocalDate.of(2018, 2, 1),
                                LocalDate.of(2018, 4, 30),
                                "09:00:00",
                                180
                        );
                    }
                    else {
                        // all predicates
                        ownerGroup = user.getUserGroup();
                        ownerProfile = user.getUserProfile();
                        location = user.getUserGroup();
                        tsPred = new TimeStampPredicate(
                                LocalDate.of(2018, 2, 1),
                                LocalDate.of(2018, 4, 30),
                                "09:00:00",
                                180
                        );
                    }

                    BEPolicy policy = buildPolicyWithOptionalPredicates(
                            possibleQueriers.get(i),
                            user.getId(),          // always present
                            ownerGroup,            // may be null
                            ownerProfile,          // may be null
                            tsPred,                // may be null / partial
                            location,              // may be null
                            PolicyConstants.ACTION_ALLOW
                    );

                    policies.add(policy);
                    count++;
                }
            }
        }

        for (BEPolicy policy : policies) {
            System.out.println(policy.toString());
        }
        System.out.println();

        flatpolper.insertPolicies(policies);
        if (!policies.isEmpty()) {
            defaultpolper.insertPoliciesEnrollment(policies);
        }

        System.out.println("Default policies count: " + policies.size());
        return policies;
    }

    /**
     * Builds one policy for the validation workload.
     *
     * The owner/user-id predicate is always included. Other predicates such as
     * owner group, owner profile, date, time, and location are added only if
     * their corresponding values are non-null.
     */
    public BEPolicy buildPolicyWithOptionalPredicates(int querier, int ownerId, String ownerGroup, String ownerProfile,
                                                      TimeStampPredicate tsPred, String location, String action) {
        String policyID = UUID.randomUUID().toString();

        List<QuerierCondition> querierConditions = new ArrayList<>(Arrays.asList(
                new QuerierCondition(policyID, "policy_type", AttributeType.STRING, Operation.EQ, "user"),
                new QuerierCondition(policyID, "querier", AttributeType.STRING, Operation.EQ, String.valueOf(querier))
        ));

        List<ObjectCondition> objectConditions = new ArrayList<>();

        // user-id is always present
        if (ownerId != 0) {
            ObjectCondition owner = new ObjectCondition(
                    policyID,
                    PolicyConstants.USERID_ATTR,
                    AttributeType.STRING,
                    String.valueOf(ownerId),
                    Operation.EQ
            );
            objectConditions.add(owner);
        }

        if (ownerGroup != null) {
            ObjectCondition ownerGroupCond = new ObjectCondition(
                    policyID,
                    PolicyConstants.GROUP_ATTR,
                    AttributeType.STRING,
                    ownerGroup,
                    Operation.EQ
            );
            objectConditions.add(ownerGroupCond);
        }

        if (ownerProfile != null) {
            ObjectCondition ownerProfileCond = new ObjectCondition(
                    policyID,
                    PolicyConstants.PROFILE_ATTR,
                    AttributeType.STRING,
                    ownerProfile,
                    Operation.EQ
            );
            objectConditions.add(ownerProfileCond);
        }

        if (tsPred != null) {
            if (tsPred.getStartDate() != null && tsPred.getEndDate() != null) {
                ObjectCondition datePred = new ObjectCondition(
                        policyID,
                        PolicyConstants.START_DATE,
                        AttributeType.DATE,
                        tsPred.getStartDate().toString(),
                        Operation.GTE,
                        tsPred.getEndDate().toString(),
                        Operation.LTE
                );
                objectConditions.add(datePred);
            }

            if (tsPred.getStartTime() != null && tsPred.getEndTime() != null) {
                ObjectCondition timePred = new ObjectCondition(
                        policyID,
                        PolicyConstants.START_TIME,
                        AttributeType.TIME,
                        tsPred.parseStartTime(),
                        Operation.GTE,
                        tsPred.parseEndTime(),
                        Operation.LTE
                );
                objectConditions.add(timePred);
            }
        }

        if (location != null) {
            ObjectCondition locationPred = new ObjectCondition(
                    policyID,
                    PolicyConstants.LOCATIONID_ATTR,
                    AttributeType.STRING,
                    location,
                    Operation.EQ
            );
            objectConditions.add(locationPred);
        }

        return new BEPolicy(
                policyID,
                objectConditions,
                querierConditions,
                "attendance-control",
                action,
                new Timestamp(System.currentTimeMillis())
        );
    }




    public void runExpreriment () {
        CPolicyGen cpg = new CPolicyGen();
        CUserGen cUserGen = new CUserGen(1);
        List<CUserGen.User> users = cUserGen.retrieveUserDataForAC();
        List<BEPolicy> policies = cpg.generateDefaultPoliciesPerQueriesforAC(users,1000);
//        List<BEPolicy> policies = cpg.generateDefaultAndCustomPoliciesPerQueriesforAC
//                (users, 2000, 50);
//        List<BEPolicy> policies = cpg.generateValidationPolicyWorkloadForAC(users,100);
        System.out.println("Total number of users: " + users.size());
        System.out.println("Total number of policies: " + policies.size());
    }

}

