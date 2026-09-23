package com.transport.erp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorizedVehicleResponse {
    private Long id;
    private String code;
    private String name;
    private Long companyId;
    private Long branchId;
}
