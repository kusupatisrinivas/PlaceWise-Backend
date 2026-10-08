package com.placewise.repository;
import com.placewise.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByStudentIdAndDriveId(Long studentId, Long driveId);
    List<Application> findByStudentIdOrderByAppliedAtDesc(Long studentId);
    List<Application> findByDriveIdOrderByAppliedAtDesc(Long driveId);
    List<Application> findAllByOrderByAppliedAtDesc();
}
