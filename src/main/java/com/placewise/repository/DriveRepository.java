package com.placewise.repository;
import com.placewise.entity.PlacementDrive;
import org.springframework.data.jpa.repository.*;
public interface DriveRepository extends JpaRepository<PlacementDrive, Long>, JpaSpecificationExecutor<PlacementDrive> { }
