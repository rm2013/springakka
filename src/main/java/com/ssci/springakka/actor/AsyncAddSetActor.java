package com.ssci.springakka.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import com.ssci.springakka.messages.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import scala.concurrent.duration.Duration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Akka High level actor
 * <p>
 * This actor will be in charge of creating other actors and send them messages to coordinate the work.
 * It also receives the results and prints them once the processing is finished.
 */
@Component("asyncAddSetActor")
@Scope("prototype")
public class AsyncAddSetActor extends UntypedAbstractActor {

    private static final int NUM_ACTORS = 10000;
    private List<AddItemMessage> addItemMessages = new ArrayList<>();
    private long sets;
    private long processedCount;
    private ActorRef addSender = null;

    @Override
    public void onReceive(Object message) throws Exception {
        /*
            This actor can receive two different messages, ProjectAnalysisMessage or RepoProcessingResult, any
            other type will be discarded using the unhandled method
         */
        if (message instanceof AddSetMessage) {

            Instant start = Instant.now();
            // stores a reference to the original sender to send back the results later on
            addSender = this.getSender();
            sets = ((AddSetMessage) message).getSets();


            for (int i = 0; i < sets; i++) {

                // creates a new actor per each repo
                Props props = Props.create(AddItemActor.class);
                ActorRef addItemActor = this.getContext().actorOf(props);

                // sends a message to the new actor with the repo payload
                //addItemActor.tell(new AddItemMessage(1+i, 2+i), this.getSelf());
                this.getContext().getSystem().scheduler().scheduleOnce(
                        Duration.create(3000, TimeUnit.MILLISECONDS),
                        //this.getContext().getSelf(),
                        addItemActor,
                        new AddItemMessage(1 + i, 2 + i),
                        this.getContext().getDispatcher(),
                        this.getContext().getSelf());
            }

        } else if (message instanceof AddSetOptimizedMessage) {

            Instant start = Instant.now();
            // stores a reference to the original sender to send back the results later on
            addSender = this.getSender();
            sets = ((AddSetOptimizedMessage) message).getSets();

            //create only 10,000 actors max
            //for sets greater than 10,000, send messages to the same actors
            //13,405/10000 = 1.3405 i.e first 3405 will receive 2 msgs and remaining gets one
            //170005/10000 = 17.0005 i.e first 5 will get 18 msgs and remaining gets 17

            double numMsgsPerActor = ((double) sets) / NUM_ACTORS;
            double numActorsWithOneMore = (numMsgsPerActor - Math.floor(numMsgsPerActor)) * NUM_ACTORS;

            int addSeed = 0;
            for (int curActor = 0; curActor < NUM_ACTORS; curActor++) {
                // creates a new actor per each repo
                Props props = Props.create(AddItemActor.class);
                ActorRef addItemActor = this.getContext().actorOf(props);

                for (int j = 0; j < numMsgsPerActor; j++) {
                    // sends a message to the new actor
                    this.getContext().getSystem().scheduler().scheduleOnce(
                            Duration.create(3000, TimeUnit.MILLISECONDS),
                            //this.getContext().getSelf(),
                            addItemActor,
                            new AddItemMessage(addSeed++, addSeed++),
                            this.getContext().getDispatcher(),
                            this.getContext().getSelf());
                }
                if (curActor < numActorsWithOneMore) {
                    this.getContext().getSystem().scheduler().scheduleOnce(
                            Duration.create(3000, TimeUnit.MILLISECONDS),
                            //this.getContext().getSelf(),
                            addItemActor,
                            new AddItemMessage(addSeed++, addSeed++),
                            this.getContext().getDispatcher(),
                            this.getContext().getSelf());
                }
            }

        } else if (message instanceof AddItemResult) {

            // a result message is received after a repoProcessor actor has finished processing a line
            addItemMessages.add(((AddItemResult) message).getAddItemMessage());

            // if the file has been processed entirely, send a termination message to the main actor
            processedCount++;
            if (sets == processedCount) {
                // send done message
                addSender.tell(new AddSetResult(addItemMessages), ActorRef.noSender());
                processedCount = 0;
            }

        } else {
            // Ignore message
            this.unhandled(message);
        }
    }

}
