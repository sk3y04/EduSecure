package edusecure.edusecure.repository.space;

import edusecure.edusecure.entity.space.Space;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpaceRepository extends JpaRepository<Space, UUID> {

    boolean existsByCode(String code);

        Optional<Space> findByCode(String code);

        List<Space> findAllByCreatedByUserId(UUID createdByUserId);

    boolean existsByCodeAndIdNot(String code, UUID id);

    @Query("""
            select s.id as id,
                   s.name as name,
                   s.code as code,
                   s.description as description,
                   s.archived as archived,
                   count(sm.id) as memberCount
            from Space s
            left join SpaceMembership sm on sm.spaceId = s.id
            group by s.id, s.name, s.code, s.description, s.archived
            order by s.archived asc, s.name asc
            """)
    List<SpaceSummaryProjection> findAllSummaries();

    @Query("""
            select s.id as id,
                   s.name as name,
                   s.code as code,
                   s.description as description,
                   s.archived as archived,
                   count(sm.id) as memberCount
            from Space s
            left join SpaceMembership sm on sm.spaceId = s.id
            where s.createdByUserId = :createdByUserId
            group by s.id, s.name, s.code, s.description, s.archived
            order by s.archived asc, s.name asc
            """)
    List<SpaceSummaryProjection> findSummariesByCreatedByUserId(@Param("createdByUserId") UUID createdByUserId);

    @Query("""
            select s.id as id,
                   s.name as name,
                   s.code as code,
                   s.description as description,
                   s.archived as archived,
                   count(sm.id) as memberCount
            from Space s
            join SpaceMembership membership on membership.spaceId = s.id and membership.studentUserId = :studentUserId
            left join SpaceMembership sm on sm.spaceId = s.id
            group by s.id, s.name, s.code, s.description, s.archived
            order by s.archived asc, s.name asc
            """)
    List<SpaceSummaryProjection> findSummariesByStudentUserId(@Param("studentUserId") UUID studentUserId);
}