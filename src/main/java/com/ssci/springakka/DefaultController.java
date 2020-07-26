package com.ssci.springakka;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.dispatch.OnSuccess;
import akka.pattern.Patterns;
import akka.util.Timeout;
import com.ssci.springakka.actor.WorkerActor;
import com.ssci.springakka.di.SpringExtension;
import com.ssci.springakka.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.FiniteDuration;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
public class DefaultController {

    private static final int MAX_DELAY = 21474835;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private ActorSystem actorSystem;
    @Autowired
    private SpringExtension springExtension;
    private ActorRef workerActorRef;
    private ActorRef coordinator;
    private ActorRef projectCoordinator;
    private ActorRef addCoordinator;
    private ActorRef asyncAddCoordinator;

    @PostConstruct
    public void init() {
        workerActorRef = actorSystem.actorOf(springExtension.props("workerActor"), "worker-actor");
        coordinator = actorSystem.actorOf(springExtension.props("fileAnalysisActor"), "file-analysis-actor");
        projectCoordinator = actorSystem.actorOf(springExtension.props("projectAnalysisActor"), "project-analysis-actor");
        addCoordinator = actorSystem.actorOf(springExtension.props("addSetActor"), "add-actor");
        asyncAddCoordinator = actorSystem.actorOf(springExtension.props("asyncAddSetActor"), "async-add-actor");
    }

    @RequestMapping("/")
    public String index() {

        return "Welcome to Springboot Akka\nUse /loadActors/{actors} to run a sleep job for the specified actors\n" +
                "Use /analyzeLogFile to analyze an sample data file which is part of the project in data folder\n" +
                "Use /projectContributors/{tries} \n" +
                " /projectContributorsAkka/{tries} \n " +
                " /projectContributorsAsyncAkka/{tries} to find the contributors for kotlin project on github, \n" +
                "for this github credentials are needed. \n curl --location --request GET 'http://localhost:8080/projectContributors/1' \\\n" +
                "--header 'Authorization: Basic *******' base64 encoded \n" +
                " All of these use akka for the repos but varies for getting list of repos \n" +
                "Use /add/{sets} to perfom add for the number of sets specified\n" +
                "Use /asyncAdd/{sets} to perform add for the number of sets";
    }

    @RequestMapping("/loadActors/{actors}")
    public String loadActors(@PathVariable("actors") int actors) throws Exception {

        Instant start = Instant.now();
        for (int i = 0; i < actors; i++) {
            workerActorRef.tell(new WorkerActor.Request(), null);
        }

        FiniteDuration duration = FiniteDuration.create(actors * 2, TimeUnit.SECONDS);
        Future<Object> awaitable = Patterns.ask(workerActorRef, new WorkerActor.Response(), Timeout.durationToTimeout(duration));
        Await.result(awaitable, duration);
        long milli = Instant.now().toEpochMilli() - start.toEpochMilli();
        return "Loading" + actors + " Actors in " + milli + " milliseconds";
    }

    @RequestMapping("/analyzeLogFile")
    public String analyzeLogFile() throws Exception {

        Instant start = Instant.now();

        // Create a message including the file path
        FileAnalysisMessage msg = new FileAnalysisMessage("data/log.txt");

        FiniteDuration duration = FiniteDuration.create(1, TimeUnit.SECONDS);

        // Send a message to start processing the file. This is a synchronous call using 'ask' with a timeout.
        Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
        Future<Object> future = Patterns.ask(coordinator, msg, timeout);


        // Process the results
        final ExecutionContext ec = actorSystem.dispatcher();
        future.onSuccess(new OnSuccess<Object>() {
            @Override
            public void onSuccess(Object message) throws Throwable {
                if (message instanceof FileProcessedMessage) {
                    printResults((FileProcessedMessage) message);
                }
            }

            private void printResults(FileProcessedMessage message) {
                System.out.println("================================");
                System.out.println("||\tCount\t||\t\tIP");
                System.out.println("================================");

                Map<String, Long> result = new LinkedHashMap<>();

                // Sort by value and put it into the "result" map
                message.getData().entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .forEachOrdered(x -> result.put(x.getKey(), x.getValue()));

                // Print only if count > 50
                result.entrySet().stream().filter(entry -> entry.getValue() > 50).forEach(entry ->
                        System.out.println("||\t" + entry.getValue() + "   \t||\t" + entry.getKey())
                );
            }
        }, ec);

        //logger.info("Response: " + Await.result(future, duration));
        long milli = Instant.now().toEpochMilli() - start.toEpochMilli();
        return "analyzeLogFile Actors in " + milli + " milliseconds";
    }

    @RequestMapping("/projectContributors/{tries}")
    public String projectContributors(@PathVariable int tries, @RequestHeader("Authorization") String authorization)
            throws Exception {

        Instant start = Instant.now();

        logger.info("Start epoc: " + start.toEpochMilli());
        // Create a message including the file path
        ProjectAnalysisMessage msg = new ProjectAnalysisMessage("kotlin", tries, authorization,
                false, false);
        startProjectAnalysis(start, msg);


        long milli = Instant.now().toEpochMilli() - start.toEpochMilli();
        return "projectContributors returned in" + milli + " milliseconds";
    }

    @RequestMapping("/projectContributorsAkka/{tries}")
    public String projectContributorsAkka(@PathVariable int tries, @RequestHeader("Authorization") String authorization)
            throws Exception {

        Instant start = Instant.now();

        logger.info("Start epoc: " + start.toEpochMilli());
        // Create a message including the file path
        ProjectAnalysisMessage msg = new ProjectAnalysisMessage("kotlin", tries,
                authorization, false, true);
        startProjectAnalysis(start, msg);


        long milli = Instant.now().toEpochMilli() - start.toEpochMilli();
        return "projectContributors returned in" + milli + " milliseconds";
    }

    @RequestMapping("/projectContributorsAsyncAkka/{tries}")
    public String projectContributorsAsyncAkka(@PathVariable int tries, @RequestHeader("Authorization") String authorization)
            throws Exception {

        Instant start = Instant.now();

        logger.info("Start epoc: " + start.toEpochMilli());
        // Create a message including the file path
        ProjectAnalysisMessage msg = new ProjectAnalysisMessage("kotlin", tries,
                authorization, true, true);
        startProjectAnalysis(start, msg);


        long milli = Instant.now().toEpochMilli() - start.toEpochMilli();
        return "projectContributors returned in" + milli + " milliseconds";
    }

    private void startProjectAnalysis(Instant start, ProjectAnalysisMessage msg) {
        // Send a message to start processing the file. This is a synchronous call using 'ask' with a timeout.
        Timeout timeout = new Timeout(30, TimeUnit.SECONDS);
        Future<Object> future = Patterns.ask(projectCoordinator, msg, timeout);

        // Process the results
        final ExecutionContext ec = actorSystem.dispatcher();
        future.onSuccess(new OnSuccess<Object>() {
            @Override
            public void onSuccess(Object message) throws Throwable {
                if (message instanceof ProjectProcessedMessage) {
                    //printResults((ProjectProcessedMessage) message);
                    long milli = Instant.now().toEpochMilli() - start.toEpochMilli();
                    logger.info("projectContributors in " + milli + "milliseconds");
                }
            }

            private void printResults(ProjectProcessedMessage message) {
                System.out.println("================================");
                System.out.println("||\tCount\t||\t\tLogin");
                System.out.println("================================");

                Map<String, Long> result = new LinkedHashMap<>();

                // Sort by value and put it into the "result" map
                message.getData().entrySet().stream()
                        .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                        .forEachOrdered(x -> result.put(x.getKey(), x.getValue()));

                result.entrySet().stream().forEach(entry ->
                        System.out.println("||\t" + entry.getValue() + "   \t||\t" + entry.getKey())
                );
            }
        }, ec);
    }

    @RequestMapping("/add/{sets}")
    public String addSets(@PathVariable int sets) throws Exception {

        Instant start = Instant.now();
        AddSetMessage msg = new AddSetMessage(sets);

        Timeout timeout = new Timeout(sets * 2 >= MAX_DELAY ? MAX_DELAY : sets * 2, TimeUnit.SECONDS);
        Future<Object> future = Patterns.ask(addCoordinator, msg, timeout);

        // Process the results
        final ExecutionContext ec = actorSystem.dispatcher();
        future.onSuccess(new OnSuccess<Object>() {
            @Override
            public void onSuccess(Object message) throws Throwable {
                if (message instanceof AddSetResult) {
                    printResults((AddSetResult) message);
                    long milli = Instant.now().toEpochMilli() - start.toEpochMilli();
                    logger.info("add " + sets + " sets took " + milli + " milliseconds");
                }
            }

            private void printResults(AddSetResult message) {
                System.out.println("================================");
                System.out.println("||\tAddendOne\t||\tAddendTwo\t||\tSum");
                System.out.println("================================");

                message.getAddItems().stream()
                        .forEach(entry ->
                                System.out.println("||\t" + entry.getAddendOne() +
                                        "\t||\t" + entry.getAddendTwo() + "\t||\t"
                                        + entry.getSum() + "\t"));
            }
        }, ec);
        long milli = Instant.now().toEpochMilli() - start.toEpochMilli();
        return "add " + sets + " sets took " + milli + " milliseconds";
    }

    @RequestMapping("/addOptimize/{sets}")
    public String addOptimizeSets(@PathVariable int sets) throws Exception {

        Instant start = Instant.now();
        AddSetOptimizedMessage msg = new AddSetOptimizedMessage(sets);

        // Send a message to start processing. This is a synchronous call using 'ask' with a timeout.
        int secs = ((sets * 5) >= MAX_DELAY) ? MAX_DELAY - 1 : sets * 5;
        Timeout timeout = new Timeout(secs, TimeUnit.SECONDS);
        Future<Object> future = Patterns.ask(addCoordinator, msg, timeout);

        // Process the results
        final ExecutionContext ec = actorSystem.dispatcher();
        future.onSuccess(new OnSuccess<Object>() {
            @Override
            public void onSuccess(Object message) throws Throwable {
                if (message instanceof AddSetResult) {
                    //printResults((AddSetResult) message);
                    long milli = Instant.now().toEpochMilli() - start.toEpochMilli();
                    logger.info("add " + sets + " sets took " + milli + " milliseconds");
                }
            }

            private void printResults(AddSetResult message) {
                System.out.println("================================");
                System.out.println("||\tAddendOne\t||\tAddendTwo\t||\tSum");
                System.out.println("================================");

                message.getAddItems().stream()
                        .forEach(entry ->
                                System.out.println("||\t" + entry.getAddendOne() +
                                        "\t||\t" + entry.getAddendTwo() + "\t||\t"
                                        + entry.getSum() + "\t"));
            }
        }, ec);

        long milli = Instant.now().toEpochMilli() - start.toEpochMilli();
        return "add " + sets + " sets took " + milli + " milliseconds";
    }


    @RequestMapping("/asyncAdd/{sets}")
    public String asyncAddSets(@PathVariable int sets) throws Exception {

        Instant start = Instant.now();
        AddSetMessage msg = new AddSetMessage(sets);

        // Send a message to start processing. This is a synchronous call using 'ask' with a timeout.
        int secs = ((sets * 5) >= MAX_DELAY) ? MAX_DELAY - 1 : sets * 5;
        Timeout timeout = new Timeout(secs, TimeUnit.SECONDS);
        Future<Object> future = Patterns.ask(asyncAddCoordinator, msg, timeout);

        // Process the results
        final ExecutionContext ec = actorSystem.dispatcher();
        future.onSuccess(new OnSuccess<Object>() {
            @Override
            public void onSuccess(Object message) throws Throwable {
                if (message instanceof AddSetResult) {
                    //printResults((AddSetResult) message);
                    long milli = Instant.now().toEpochMilli() - start.toEpochMilli();
                    logger.info("add " + sets + " sets took " + milli + " milliseconds");
                }
            }

            private void printResults(AddSetResult message) {
                System.out.println("================================");
                System.out.println("||\tAddendOne\t||\tAddendTwo\t||\tSum");
                System.out.println("================================");

                message.getAddItems().stream()
                        .forEach(entry ->
                                System.out.println("||\t" + entry.getAddendOne() +
                                        "\t||\t" + entry.getAddendTwo() + "\t||\t"
                                        + entry.getSum() + "\t"));
            }
        }, ec);

        long milli = Instant.now().toEpochMilli() - start.toEpochMilli();
        return "add " + sets + " sets took " + milli + " milliseconds";
    }

    @RequestMapping("/asyncAddOptimize/{sets}")
    public String asyncAddOptimizeSets(@PathVariable int sets) throws Exception {

        Instant start = Instant.now();
        AddSetOptimizedMessage msg = new AddSetOptimizedMessage(sets);

        // Send a message to start processing. This is a synchronous call using 'ask' with a timeout.
        int secs = ((sets * 5) >= MAX_DELAY) ? MAX_DELAY - 1 : sets * 5;
        Timeout timeout = new Timeout(secs, TimeUnit.SECONDS);
        Future<Object> future = Patterns.ask(asyncAddCoordinator, msg, timeout);

        // Process the results
        final ExecutionContext ec = actorSystem.dispatcher();
        future.onSuccess(new OnSuccess<Object>() {
            @Override
            public void onSuccess(Object message) throws Throwable {
                if (message instanceof AddSetResult) {
                    //printResults((AddSetResult) message);
                    long milli = Instant.now().toEpochMilli() - start.toEpochMilli();
                    logger.info("add " + sets + " sets took " + milli + " milliseconds");
                }
            }

            private void printResults(AddSetResult message) {
                System.out.println("================================");
                System.out.println("||\tAddendOne\t||\tAddendTwo\t||\tSum");
                System.out.println("================================");

                message.getAddItems().stream()
                        .forEach(entry ->
                                System.out.println("||\t" + entry.getAddendOne() +
                                        "\t||\t" + entry.getAddendTwo() + "\t||\t"
                                        + entry.getSum() + "\t"));
            }
        }, ec);

        long milli = Instant.now().toEpochMilli() - start.toEpochMilli();
        return "add " + sets + " sets took " + milli + " milliseconds";
    }


}
