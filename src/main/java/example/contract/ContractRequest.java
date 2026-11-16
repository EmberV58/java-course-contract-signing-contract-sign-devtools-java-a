package example.contract;

public record ContractRequest(String courseCode, String learnerName, String state, byte[] pdf) {
    public boolean readyForRelease() {
        return "READY_FOR_RELEASE".equals(state);
    }
}
