package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.repository.entity.LinkEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LinkEntityJpaRepository extends JpaRepository<LinkEntity, Long> {

    Optional<LinkEntity> findByUrl(String url);

    @Query("SELECT l FROM LinkEntity l ORDER BY l.lastCheckedAt ASC")
    List<LinkEntity> findTopByOrderByLastCheckedAtAsc(org.springframework.data.domain.Pageable pageable);
}
