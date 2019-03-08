package com.swisscom.allegro.escomparison;

import com.swisscom.allegro.escomparison.options.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;


@Slf4j
@SpringBootApplication
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Application {

    private static List<CompareTest> tests = new ArrayList<>();
    private static Scanner in = new Scanner(System.in);

    private final CustomerCompareTest customerCompareTest;
    private final SoiCompareTest soiCompareTest;
    private final BillSpecCompareTest billSpecCompareTest;
    private final QpiCompareTest qpiCompareTest;
    private final InventoryCompareTest inventoryCompareTest;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    private void init() {
        initOptions();
        showWelcomeMessage();

        Optional<CompareTest> test;

        do {
            showOptions();
            test = getTest(readOption());
            in.nextLine();
            long startTime = System.nanoTime();

            test.ifPresent(CompareTest::exec);

            long endTime = System.nanoTime();
            log.debug("Comparison took: {} sec", getTimeFromMsInSec((endTime - startTime)));
        } while (test.isPresent());
    }

    private static double getTimeFromMsInSec(double time) {
        return time / 1_000_000_000.0;
    }

    private static void showWelcomeMessage() {
        print("\nWelcome to Importer Tester V2.0");
        print("Note: To exit please select an invalid test.");
    }

    private void initOptions() {
        tests.add(customerCompareTest);
        tests.add(soiCompareTest);
        tests.add(billSpecCompareTest);
        tests.add(qpiCompareTest);
        tests.add(inventoryCompareTest);
    }

    private static void showOptions() {
        print("\nPlease select a test to run:");
        tests.forEach(option -> {
            int index = tests.indexOf(option) + 1;
            print(index + ". " + option.name, true);
        });
    }

    private static Optional<CompareTest> getTest(int no) {
        if (no < 0 || no > tests.size() - 1)
            return Optional.empty();

        return Optional.of(tests.get(no));
    }

    private static int readOption() {
        print("> ", false);
        return in.nextInt() - 1;
    }

    private static void print(String value) {
        print(value, true);
    }

    @SuppressWarnings("all")
    private static void print(String value, boolean willEndTheLine) {
        if (willEndTheLine)
            System.out.println(value);
        else System.out.print(value);
    }
}
