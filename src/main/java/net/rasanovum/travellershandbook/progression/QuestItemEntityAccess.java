package net.rasanovum.travellershandbook.progression;

public interface QuestItemEntityAccess {
    int getQuestEligibleCount();

    void setQuestEligibleCount(int count);

    default void markPlayerDropped() {
        setQuestEligibleCount(0);
    }
}
