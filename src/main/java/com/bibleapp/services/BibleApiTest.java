package com.bibleapp.services;

/**
 * Terminal-only test harness for {@link BibleApiClient}.
 * Run with: mvn compile exec:java -Dexec.mainClass="com.bibleapp.services.BibleApiTest"
 */
public class BibleApiTest {

    public static void main(String[] args) {
        BibleApiClient client = new BibleApiClient();

        runCase("getPassage(\"john 3:16\")",
                () -> client.getPassage("john 3:16"));

        runCase("getPassage(\"matthew 5:3-12\", \"kjv\")",
                () -> client.getPassage("matthew 5:3-12", "kjv"));

        runCase("getRandomVerse(\"web\")",
                () -> client.getRandomVerse("web"));

        runCase("getRandomVerse(\"web\", \"NT\")",
                () -> client.getRandomVerse("web", "NT"));

        runCase("getPassage(\"xyz 999:1\")  [expected to fail]",
                () -> client.getPassage("xyz 999:1"));
    }

    private static void runCase(String label, ApiCall call) {
        System.out.println("=== " + label + " ===");
        try {
            BiblePassage passage = call.run();
            System.out.println(passage);
        } catch (BibleApiException e) {
            System.err.println("ERROR: " + e.getMessage());
        }
        System.out.println();
    }

    @FunctionalInterface
    private interface ApiCall {
        BiblePassage run() throws BibleApiException;
    }
}
