package com.ssci.springakka.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import com.ssci.springakka.messages.AddItemMessage;
import com.ssci.springakka.messages.AddItemResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Akka High level actor
 * <p>
 * This actor will be in charge of creating other actors and send them messages to coordinate the work.
 * It also receives the results and prints them once the processing is finished.
 */
@Component("addItemActor")
@Scope("prototype")
public class AddItemActor extends UntypedAbstractActor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
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
        if (message instanceof AddItemMessage) {

            Instant start = Instant.now();
            // stores a reference to the original sender to send back the results later on
            analyticsSender = this.getSender();

            {
                // creates a new actor per each repo
                Props props = Props.create(AddItemActor.class);
                ActorRef addItemActor = this.getContext().actorOf(props);
                //logger.info("AddItemActor: "+this+" msg: "+ ((AddItemMessage)message));
                ((AddItemMessage) message).setSum(((AddItemMessage) message).getAddendOne() +
                        ((AddItemMessage) message).getAddendTwo());
                //Thread.sleep(1000);
                this.getSender().tell(new AddItemResult((AddItemMessage) message), this.getSelf());
            }

        } else {
            // Ignore message
            this.unhandled(message);
        }
    }

}
