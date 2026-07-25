package com.jira.cluster.archunit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Base class for cluster safety architectural rules.
 * Each service should create a subclass that imports its own packages:
 *
 * <pre>
 * {@literal @}AnalyzeClasses(packages = "com.jira.issue", importOptions = ImportOption.DoNotIncludeTests.class)
 * class IssueServiceClusterArchTest extends ClusterSafetyArchTest {}
 * </pre>
 */
public abstract class ClusterSafetyArchTest {

    /**
     * Rule R1: Every @Scheduled method must also have @SchedulerLock.
     * Without it, every node in the cluster executes the task independently,
     * causing duplicate processing.
     */
    public static final ArchRule SCHEDULED_MUST_HAVE_SCHEDULER_LOCK =
            methods()
                    .that().areAnnotatedWith(Scheduled.class)
                    .should(new ArchCondition<JavaMethod>("also be annotated with @SchedulerLock") {
                        @Override
                        public void check(JavaMethod method, ConditionEvents events) {
                            boolean hasSchedulerLock = method.isAnnotatedWith(SchedulerLock.class);
                            if (!hasSchedulerLock) {
                                events.add(SimpleConditionEvent.violated(method,
                                        method.getFullName() + " has @Scheduled but missing @SchedulerLock — " +
                                                "will execute on EVERY cluster node (Rule R1)"));
                            }
                        }
                    })
                    .because("In a cluster, @Scheduled without @SchedulerLock causes " +
                            "duplicate execution on every node (Rule R1)");

    /**
     * Rule R2: No mutable static fields of collection types in Spring beans.
     * Static collections are JVM-local and invisible to other cluster nodes.
     */
    public static final ArchRule NO_MUTABLE_STATIC_COLLECTIONS =
            noFields()
                    .that().areStatic()
                    .and().areNotFinal()
                    .and().haveRawType(java.util.Map.class)
                    .should().beDeclaredInClassesThat().areAnnotatedWith(org.springframework.stereotype.Service.class)
                    .orShould().beDeclaredInClassesThat().areAnnotatedWith(org.springframework.stereotype.Component.class)
                    .because("Mutable static collections are JVM-local and not shared across cluster nodes (Rule R2)");

    /**
     * Helper method to run rules against a service's classes.
     * Services can call this from their own tests.
     */
    public static void verifyClusterSafety(String... packages) {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(packages);

        SCHEDULED_MUST_HAVE_SCHEDULER_LOCK.check(classes);
        NO_MUTABLE_STATIC_COLLECTIONS.check(classes);
    }
}
