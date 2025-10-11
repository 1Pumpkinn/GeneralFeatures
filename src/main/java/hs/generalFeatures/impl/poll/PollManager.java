package hs.generalFeatures.impl.poll;

import java.util.List;
import java.util.UUID;

public class PollManager {
    private Poll activePoll;

    public Poll createPoll(String question, List<String> options, int durationSeconds, UUID creatorId) {
        activePoll = new Poll(question, options, durationSeconds, creatorId);
        return activePoll;
    }

    public Poll getActivePoll() {
        return activePoll;
    }

    public boolean hasActivePoll() {
        return activePoll != null && !activePoll.hasEnded();
    }

    public void clearPoll() {
        activePoll = null;
    }
}