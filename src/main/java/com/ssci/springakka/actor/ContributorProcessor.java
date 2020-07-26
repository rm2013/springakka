package com.ssci.springakka.actor;


import akka.actor.UntypedAbstractActor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.ssci.springakka.messages.RepoMessage;
import com.ssci.springakka.messages.RepoProcessingResult;
import com.ssci.springakka.model.Contributor;
import com.ssci.springakka.utility.JSONUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Component("contributorProcessor")
@Scope("prototype")
public class ContributorProcessor extends UntypedAbstractActor {

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof RepoMessage) {
            // What data each actor process?
//            System.out.println("Line: " + ((LogLineMessage) message).getData());
            // Uncomment this line to see the thread number and the actor name relationship
//            System.out.println("Thread ["+Thread.currentThread().getId()+"] handling ["+ getSelf().toString()+"]");

            // get the message payload, this will be just one line from the log file
            String contributorsUrl = ((RepoMessage) message).getContributorsUrl();

            List<Contributor> contributors = getContributors(contributorsUrl,
                    ((RepoMessage) message).getAuthorization());

            if (contributors != null && contributors.size() > 0) {

                // tell the sender that we got a result using a new type of message
                this.getSender().tell(new RepoProcessingResult(contributors), this.getSelf());
            }
        } else {
            // ignore any other message type
            this.unhandled(message);
        }
    }

    private List<Contributor> getContributors(String contributorsUrl, String authorization)
            throws IOException, InterruptedException {
        List<Contributor> contributors = null;

        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest
                .newBuilder(URI.create(contributorsUrl))
                .header("Accept", "application/vnd.github.v3+json")
                .header("Authorization", authorization)
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 500)
            System.out.println("Project Not Avaialble");
        else {
            String contributionString = response.body();
            //System.out.println(contributionString);
            TypeReference<List<Contributor>> contributorsList = new TypeReference<List<Contributor>>() {
            };
            contributors = JSONUtils.convertFromJsonToList(contributionString, contributorsList);
            //System.out.println(contributors);
        }
        return contributors;
    }
}
