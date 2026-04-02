package edusecure.edusecure.repository.space;

import java.util.UUID;

public interface SpaceSummaryProjection {

    UUID getId();

    String getName();

    String getCode();

    String getDescription();

    boolean isArchived();

    long getMemberCount();
}