package example.contract;

public final class ContractSigningServiceTest {
    public static void main(String[] args) {
        ContractRequest ready = new ContractRequest("EDU-1", "Learner", "READY_FOR_RELEASE", new byte[] {1});
        assert ready.readyForRelease();
        ContractRequest draft = new ContractRequest("EDU-1", "Learner", "DRAFT", new byte[] {1});
        assert !draft.readyForRelease();
        System.out.println("ContractSigningServiceTest: PASS");
    }
}
