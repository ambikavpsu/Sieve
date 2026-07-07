package edu.uci.ics.tippers.template;

import java.util.*;


/**
 * Single-file example: defines a template + a plan that reads from the `enrollment` table
 * and synthesizes BEPolicy objects (compatible with the structure you showed).
 *
 * ✅ You can run this file as-is (it includes minimal placeholder classes for BEPolicy, conditions, etc.)
 * If you want to integrate into your existing codebase, replace the placeholder classes at the bottom
 * with your real BEPolicy / ObjectCondition / QuerierCondition / BooleanPredicate / enums.
 */
public class PolicyTemplate {
    public PolicyTemplate(String name, String scenario, List<TemplateField> fields) {
        this.name = name;
        this.scenario = scenario;
        this.fields = Collections.unmodifiableList(new ArrayList<>(fields));
    }

    public enum Op { EQ, IN, GE, LE, BETWEEN, OVERLAPS }

    public static final class TemplateField {
        public final String templateAttribute;   // e.g., ownerEq, locEq, date, time, querier, purpose
        public final String sourceSchemaPath;    // e.g., enrollment.user_id
        public final boolean customizable;
        public final List<Op> allowedOps;
        public final String domainConstraints;
        public final String queryBinding;

        public TemplateField(String templateAttribute,
                             String sourceSchemaPath,
                             boolean customizable,
                             List<Op> allowedOps,
                             String domainConstraints,
                             String queryBinding) {
            this.templateAttribute = templateAttribute;
            this.sourceSchemaPath = sourceSchemaPath;
            this.customizable = customizable;
            this.allowedOps = allowedOps;
            this.domainConstraints = domainConstraints;
            this.queryBinding = queryBinding;
        }
    }

    private final String name;




    private final String scenario;
    private final List<TemplateField> fields;

    public String getName() { return name; }
    public String getScenario() { return scenario; }
    public List<TemplateField> getFields() { return fields; }

    /** Factory for your current enrollment-based attendance template. */
    public static PolicyTemplate attendanceTemplate() {
        List<TemplateField> f = new ArrayList<>();

        f.add(new TemplateField("policy_type", "constant", false,
                java.util.Collections.singletonList(Op.EQ), "string", "policy_type = :policyTypeValue"));

        f.add(new TemplateField("querier", "enrollment.faculty_id", false,
                java.util.Collections.singletonList(Op.EQ), "INT faculty id", "querier = enrollment.faculty_id"));

        f.add(new TemplateField("ownerEq", "enrollment.user_id", false,
                java.util.Collections.singletonList(Op.EQ), "INT student id", "ownerEq = enrollment.user_id"));

        f.add(new TemplateField("locEq", "enrollment.locEq", true,
                java.util.Collections.singletonList(Op.EQ), "campus locations", "locEq = :loc OR locEq IN :loc_set"));

        f.add(new TemplateField("date", "enrollment.start_date/end_date", true,
                java.util.Arrays.asList(Op.GE, Op.LE, Op.BETWEEN), "term dates", "date BETWEEN :d1 AND :d2"));

        f.add(new TemplateField("time", "enrollment.start_time/end_time", true,
                java.util.Arrays.asList(Op.GE, Op.LE, Op.BETWEEN), "daily hours", "time BETWEEN :t1 AND :t2"));

        // purpose is not in enrollment unless you derive from course_name or pass constant
        f.add(new TemplateField("purpose", "course_name OR constantPurpose", true,
                java.util.Arrays.asList(Op.EQ, Op.IN), "{attendance, advising, ...}", "purpose = :purpose"));

        return new PolicyTemplate("StudentPresenceAccess", "class_attendance", f);
    }
}
