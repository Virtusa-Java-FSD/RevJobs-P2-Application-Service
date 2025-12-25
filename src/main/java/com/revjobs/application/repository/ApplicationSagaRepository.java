package com.revjobs.application.repository;

import com.revjobs.application.model.ApplicationSaga;
import com.revjobs.common.saga.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationSagaRepository extends JpaRepository<ApplicationSaga, Long> {
    Optional<ApplicationSaga> findByApplicationId(Long applicationId);
    List<ApplicationSaga> findByStatus(SagaStatus status);
}

