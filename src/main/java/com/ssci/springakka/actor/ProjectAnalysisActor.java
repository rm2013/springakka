package com.ssci.springakka.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ssci.springakka.messages.ProjectAnalysisMessage;
import com.ssci.springakka.messages.ProjectProcessedMessage;
import com.ssci.springakka.messages.RepoMessage;
import com.ssci.springakka.messages.RepoProcessingResult;
import com.ssci.springakka.model.Contributor;
import com.ssci.springakka.model.Repo;
import com.ssci.springakka.utility.JSONUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static akka.http.javadsl.model.HttpRequest.create;

/**
 * Akka High level actor
 * <p>
 * This actor will be in charge of creating other actors and send them messages to coordinate the work.
 * It also receives the results and prints them once the processing is finished.
 */
@Component("projectAnalysisActor")
@Scope("prototype")
public class ProjectAnalysisActor extends UntypedAbstractActor {

    private static final HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    private static final String SERVICE_URL = "https://api.github.com";
    private Map<String, Long> ipMap = new HashMap<>();
    private long repoCount;
    private long processedCount;
    private ActorRef analyticsSender = null;

    @Override
    public void onReceive(Object message) throws Exception {
        /*
            This actor can receive two different messages, ProjectAnalysisMessage or RepoProcessingResult, any
            other type will be discarded using the unhandled method
         */
        if (message instanceof ProjectAnalysisMessage) {

            Instant start = Instant.now();
            //get repos
            List<Repo> repos;
            if (((ProjectAnalysisMessage) message).getAsync() && ((ProjectAnalysisMessage) message).getAkka()) {

                repos = getAkkaAsyncRepos(((ProjectAnalysisMessage) message).getProjectName(),
                        ((ProjectAnalysisMessage) message).getAuthorization());
            } else if (!((ProjectAnalysisMessage) message).getAsync() && ((ProjectAnalysisMessage) message).getAkka()) {
                repos = getAkkaRepos(((ProjectAnalysisMessage) message).getProjectName(),
                        ((ProjectAnalysisMessage) message).getAuthorization());
            } else { //if no akka irrespective of async
                repos = getRepos(((ProjectAnalysisMessage) message).getProjectName(),
                        ((ProjectAnalysisMessage) message).getAuthorization());
            }

            for (int i = 0; i < ((ProjectAnalysisMessage) message).getTries(); i++) {
                repos.addAll(repos);
            }

            repoCount = repos.size();
            long milli = Instant.now().toEpochMilli() - start.toEpochMilli();
            System.out.println("dutation: " + milli);

            processedCount = 0;

            // stores a reference to the original sender to send back the results later on
            analyticsSender = this.getSender();

            for (Repo repo : repos) {
                // creates a new actor per each repo
                Props props = Props.create(ContributorProcessor.class);
                ActorRef contributorProcessorActor = this.getContext().actorOf(props);

                // sends a message to the new actor with the repo payload
                contributorProcessorActor.tell(new RepoMessage(repo.getName(), repo.getContributorsUrl(),
                        ((ProjectAnalysisMessage) message).getAuthorization()), this.getSelf());
            }

        } else if (message instanceof RepoProcessingResult) {

            // a result message is received after a repoProcessor actor has finished processing a line
            List<Contributor> contributors = ((RepoProcessingResult) message).getContributors();

            // loop through the contributors and get increment contributor counter
            for (Contributor contributor : contributors) {
                Long count = ipMap.getOrDefault(contributor.getLogin(), 0L);

                ipMap.put(contributor.getLogin(), count + contributor.getContributions());
            }

            // if the file has been processed entirely, send a termination message to the main actor
            processedCount++;
            if (repoCount == processedCount) {
                // send done message
                analyticsSender.tell(new ProjectProcessedMessage(ipMap), ActorRef.noSender());
            }

        } else {
            // Ignore message
            this.unhandled(message);
        }
    }

    private List<Repo> getAkkaAsyncRepos(String projectName, String authorization) {
        var ref = new Object() {
            List<Repo> repos = null;
        };
        ActorSystem actorSystem = this.getContext().system();
        akka.http.javadsl.model.HttpRequest httpRequest = getHttpRequest(projectName, authorization);

        Materializer mat = ActorMaterializer.create(actorSystem);

        final CompletionStage<akka.http.javadsl.model.HttpResponse> responseFuture =
                akka.http.javadsl.Http.get(actorSystem)
                        .singleRequest(httpRequest);


        TypeReference<List<Repo>> repoList = new TypeReference<List<Repo>>() {
        };

        CompletionStage<String> body = responseFuture.thenCompose(response ->
                response.entity().toStrict(86400000, mat)
        ).thenApply(entity ->
                entity.getData().utf8String())
                .whenComplete((input, exception) -> {
                    if (exception != null) {
                        System.out.println("exception occurs");
                        System.out.println(exception);
                    } else {
                        System.out.println("no exception, got result: " + input);
                        try {
                            ref.repos = JSONUtils.convertFromJsonToList(input, repoList);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                });
        try {
            String str = body.toCompletableFuture().get();
            System.out.println("String returned:" + str);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return ref.repos;
    }

    private List<Repo> getAkkaRepos(String projectName, String authorization) {
        var ref = new Object() {
            List<Repo> repos = null;
        };
        ActorSystem actorSystem = this.getContext().system();
        akka.http.javadsl.model.HttpRequest httpRequest = getHttpRequest(projectName, authorization);

        Materializer mat = ActorMaterializer.create(actorSystem);

        final CompletionStage<akka.http.javadsl.model.HttpResponse> responseFuture =
                akka.http.javadsl.Http.get(actorSystem)
                        .singleRequest(httpRequest);


        TypeReference<List<Repo>> repoList = new TypeReference<List<Repo>>() {
        };

        CompletionStage<String> body = responseFuture.thenCompose(response ->
                response.entity().toStrict(86400000, mat)
        ).thenApply(entity ->
                entity.getData().utf8String());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        body.whenComplete((input, exception) -> {
            if (exception != null) {
                System.out.println("exception occurs");
                System.err.println(exception);
            } else {
                System.out.println("no exception, got result: " + input);
                try {
                    ref.repos = JSONUtils.convertFromJsonToList(input, repoList);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return ref.repos;
    }

    private akka.http.javadsl.model.HttpRequest getHttpRequest(String projectName, String authorization) {
        akka.http.javadsl.model.HttpRequest httpRequest = create(SERVICE_URL + "/orgs/" + projectName + "/repos?per_page=100");
        akka.http.javadsl.model.HttpHeader httpHeaderAccept = akka.http.javadsl.model.HttpHeader.parse("Accept", "application/vnd.github.v3+json");
        akka.http.javadsl.model.HttpHeader httpHeaderAuth = akka.http.javadsl.model.HttpHeader.parse("Authorization", authorization);
        httpRequest.addHeader(httpHeaderAccept);
        httpRequest.addHeader(httpHeaderAuth);
        return httpRequest;
    }

    private List<Repo> getRepos(String projectName, String authorization)
            throws ExecutionException, InterruptedException, IOException {
        List<Repo> repos = null;

        HttpRequest request = HttpRequest
                .newBuilder(URI.create(SERVICE_URL + "/orgs/" + projectName + "/repos?per_page=100"))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Authorization", authorization)
                .GET()
                .build();
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 500)
            System.out.println("Project Not Avaialble");
        else if (response.statusCode() == 200) {
            String repoString = response.body();
            //System.out.println(repoString);
            TypeReference<List<Repo>> repoList = new TypeReference<List<Repo>>() {
            };
            repos = JSONUtils.convertFromJsonToList(repoString, repoList);
            //System.out.println(repos);
        } else {
            System.out.println("Project :" + response.statusCode());
        }


        return repos;
    }

}
