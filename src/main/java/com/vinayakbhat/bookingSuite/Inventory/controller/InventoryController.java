package com.vinayakbhat.bookingSuite.Inventory.controller;

import com.vinayakbhat.bookingSuite.Inventory.dto.InventoryResponse;
import com.vinayakbhat.bookingSuite.Inventory.dto.UpdateInventoryRequest;
import com.vinayakbhat.bookingSuite.Inventory.service.InventoryService;
import com.vinayakbhat.bookingSuite.common.security.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/inventory")
@PreAuthorize("hasAnyRole('HOTEL_MANAGER', 'ADMIN', 'OWNER')")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getInventoryByRoom(@PathVariable Long roomId){
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getInventoryByRoom(roomId)));}

    @PatchMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<String>> updateInventory(@PathVariable Long roomId,
                                                               @RequestBody UpdateInventoryRequest request){
        return ResponseEntity.ok(ApiResponse.success(inventoryService.updateInventory(roomId,request)));
    }

}
