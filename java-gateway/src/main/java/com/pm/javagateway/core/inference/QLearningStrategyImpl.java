package com.pm.javagateway.core.inference;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.stereotype.Component;
import com.pm.javagateway.repositories.UserRepository;

import ch.qos.logback.core.joran.action.Action;
import reactor.core.publisher.Mono;

@Component
public class QLearningStrategyImpl implements InferenceStatergy {
    private final QTableRepository qTableRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    // the 4 core action the AI can take
    private static final List<String> Actions = List.of(
            "ACTION_EXPLORE", "ACTION_VALIDATE", "ACTION_LISTEN", "ACTION_CHANGE_TOPIC");
    // 20% of the time, try a random action to learn
    private static final double EPSILON = 0.2;

    public QLearningStrategyImpl(QTableRepository qTableRepository, UserRepository userRepository) {
        this.qTableRepository = qTableRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String getModeName() {
        return "V2_QLEARNING";
    }

    @Override
    public Mono<InferenceContext> buildContext(String userId, String userPrompt) {
        UUID id = UUID.fromString(userId);

        // define th state // ---> need to change
        String currentState = userPrompt.length() > 20 ? "LONG_PROMPT" : "SHORT_PROMPT";

        // UPDATE INFERENCE TIMESTAMP
        Mono<Void> updateTimeStamp = userRepository.updateLastInteraction(id, ZonedDateTime.now()).then();

        // select the mathemetically best action --> Epslion-greedy
        Mono<String> actionMono = selectAction(id, currentState);

        return updateTimeStamp.then(actionMono).map(action -> {
            // return the selected action inside the context to pass to Python!
            return new InferenceContext("System Context => Q-Learning Mode Active. State: " + currentState, action);
        });
    }

    private Mono<String> selectAction(UUID id, String state) {
        if (random.nextDouble() < EPSILON) {
            // pick a random action to see if the user like it
            String randomAction = Actions.get(random.nextInt(Actions.size()));
            System.out.println("Q-Learning: Exploring random action -> " + randomAction);
            return Mono.just(randomAction);
        } else {
            // EXPLOIT: Check the DB and pick the action with the highest Q-Value
            return Mono.zip(
                    qTableRepository.getQvalue(id, state, "ACTION_EXPLORE"),
                    qTableRepository.getQvalue(id, state, "ACTION_VALIDATE"),
                    qTableRepository.getQvalue(id, state, "ACTION_LISTEN"),
                    qTableRepository.getQvalue(id, state, "ACTION_CHANGE_TOPIC")).map(tuple -> {
                        double explore = tuple.getT1();
                        double validate = tuple.getT2();
                        double listen = tuple.getT3();
                        double changeTopic = tuple.getT4();

                        String bestAction = "ACTION_EXPLORE";
                        double maxQ = explore;

                        if (validate > maxQ) {
                            maxQ = validate;
                            bestAction = "ACTION_VALIDATE";
                        }
                        if (listen > maxQ) {
                            maxQ = listen;
                            bestAction = "ACTION_LISTEN";
                        }
                        if (changeTopic > maxQ) {
                            bestAction = "ACTION_CHANGE_TOPIC";
                        }
                        System.out
                                .println("Q-Learning: Exploiting best action -> " + bestAction + " (Q: " + maxQ + ")");
                        return bestAction;
                    });
        }
    }
}
