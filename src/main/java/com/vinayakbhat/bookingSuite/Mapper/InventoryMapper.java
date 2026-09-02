package com.vinayakbhat.bookingSuite.Mapper;

import com.vinayakbhat.bookingSuite.Inventory.dto.InventoryResponse;
import com.vinayakbhat.bookingSuite.Inventory.entity.Inventory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryMapper {
    InventoryResponse toInventoryResponse(Inventory inventory);
}
