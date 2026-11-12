package karate;

import com.intuit.karate.junit5.Karate;

/**
 * Entry point for running all Karate BDD feature files (Core Behavior-Driven Development, 6.1.3).
 * Runs against a live instance of the backend (see karate-config.js for baseUrl).
 */
class KarateTestRunner {

    @Karate.Test
    Karate testAuth() {
        return Karate.run("auth").relativeTo(getClass());
    }

    @Karate.Test
    Karate testVehicles() {
        return Karate.run("vehicles").relativeTo(getClass());
    }
}