package uk.gov.hscic.appointment.slot;

import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface SlotRepository extends JpaRepository<SlotEntity, Long> {
    List<SlotEntity> findByScheduleReferenceAndEndDateTimeAfterAndStartDateTimeBeforeAndGpConnectBookableTrueAndBookableOrganizationsId(Long scheduleId, Date startDate, Date endDate, Long orgId);
    List<SlotEntity> findByScheduleReferenceAndEndDateTimeAfterAndStartDateTimeBeforeAndGpConnectBookableTrueAndBookableOrgTypes(Long scheduleId, Date startDate, Date endDate, String orgType);
}
