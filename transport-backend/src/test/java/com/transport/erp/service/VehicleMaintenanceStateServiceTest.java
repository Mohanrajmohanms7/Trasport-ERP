package com.transport.erp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.transport.erp.model.Vehicle;
import com.transport.erp.repository.WorkOrderRepository;
import jakarta.persistence.Column;
import org.hibernate.annotations.Formula;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleMaintenanceStateServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @InjectMocks
    private VehicleMaintenanceStateService service;

    @Test
    @DisplayName("Maintenance state is only an IN_PROGRESS work order that is not deleted")
    void queryMatchesInProgressWorkOrdersOnly() throws Exception {
        Query query = WorkOrderRepository.class
                .getMethod("existsInProgressForVehicle", Long.class)
                .getAnnotation(Query.class);
        String jpql = query.value();
        assertTrue(jpql.contains("w.status = 'IN_PROGRESS'"));
        assertTrue(jpql.contains("w.isDeleted = false"));
        assertFalse(jpql.contains("'OPEN'"));
        assertFalse(jpql.contains("'COMPLETED'"));
        assertFalse(jpql.contains("'CANCELLED'"));
        assertFalse(jpql.contains("MaintenanceRequest"));

        Formula formula = Vehicle.class.getDeclaredField("underMaintenance").getAnnotation(Formula.class);
        assertTrue(formula.value().contains("wo.status = 'IN_PROGRESS'"));
        assertTrue(formula.value().contains("wo.is_deleted = false"));
        assertFalse(formula.value().contains("OPEN"));
        assertFalse(formula.value().contains("maintenance_request"));
        assertNull(Vehicle.class.getDeclaredField("underMaintenance").getAnnotation(Column.class));
    }

    @Test
    @DisplayName("No work order, or no matching IN_PROGRESS row, is not under maintenance")
    void absentOrNonBlockingWorkOrder() {
        when(workOrderRepository.existsInProgressForVehicle(1L)).thenReturn(false);
        assertFalse(service.isVehicleUnderMaintenance(1L));
        assertFalse(service.isVehicleUnderMaintenance(null));
        verify(workOrderRepository, never()).existsInProgressForVehicle(null);
    }

    @Test
    @DisplayName("An IN_PROGRESS work order, including more than one, keeps the vehicle blocked")
    void inProgressBlocksUntilNoneRemain() {
        when(workOrderRepository.existsInProgressForVehicle(1L)).thenReturn(true, false);
        assertTrue(service.isVehicleUnderMaintenance(1L));
        assertFalse(service.isVehicleUnderMaintenance(1L));
        verify(workOrderRepository, never()).findAll();
    }

    @Test
    @DisplayName("Another vehicle's work order is not included in a single-vehicle check")
    void otherVehicleIsSeparate() {
        when(workOrderRepository.existsInProgressForVehicle(2L)).thenReturn(false);
        assertFalse(service.isVehicleUnderMaintenance(2L));
        verify(workOrderRepository).existsInProgressForVehicle(2L);
    }

    @Test
    @DisplayName("Bulk maintenance state is one query and does not run for an empty page")
    void bulkQueryIsSingleAndSkipsEmptyInput() {
        assertTrue(service.findVehiclesUnderMaintenance(List.of()).isEmpty());
        assertTrue(service.findVehiclesUnderMaintenance(null).isEmpty());
        verify(workOrderRepository, never()).findVehicleIdsUnderMaintenance(org.mockito.ArgumentMatchers.any());

        when(workOrderRepository.findVehicleIdsUnderMaintenance(List.of(1L, 2L, 3L))).thenReturn(List.of(2L));
        Set<Long> blocked = service.findVehiclesUnderMaintenance(List.of(1L, 2L, 3L));
        assertEquals(Set.of(2L), blocked);
        verify(workOrderRepository).findVehicleIdsUnderMaintenance(List.of(1L, 2L, 3L));
        verify(workOrderRepository, never()).existsInProgressForVehicle(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Derived maintenance flag is returned to the client and cannot be mapped as a column")
    void flagSerializesReadOnly() throws Exception {
        Vehicle vehicle = new Vehicle();
        Field field = Vehicle.class.getDeclaredField("underMaintenance");
        field.setAccessible(true);
        field.setBoolean(vehicle, true);

        ObjectMapper mapper = new ObjectMapper();
        Hibernate6Module module = new Hibernate6Module();
        module.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
        mapper.registerModule(module);
        String json = mapper.writeValueAsString(vehicle);
        assertTrue(json.contains("\"underMaintenance\":true"));

        Vehicle read = mapper.readValue("{\"underMaintenance\":true,\"code\":\"V\",\"name\":\"Truck\"}", Vehicle.class);
        assertFalse(field.getBoolean(read));
    }
}
