package edusecure.edusecure.entity.exam;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exam_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID examId;

    @Column(nullable = false)
    private UUID studentUserId;

    @Column(name = "result_value", nullable = false)
    private Integer value;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private UUID gradedByUserId;

    @Column(nullable = false)
    private Instant gradedAt;

    @Column
    private Instant lastModifiedAt;

    @Column
    private Instant publishedAt;
}