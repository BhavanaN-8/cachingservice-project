package com.project.cache.controller;

import com.project.cache.domain.ApiResponse;
import com.project.cache.domain.EntityModel;
import com.project.cache.service.CacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;

/** REST endpoints for cache operations */
@RestController
@RequestMapping("/cache")
public class CacheController {
    private final CacheService cache;
    public CacheController(CacheService cache){ this.cache = cache; }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse> add(@Valid @RequestBody EntityModel entity){
        cache.add(entity);
        return ResponseEntity.ok(ApiResponse.success("Entity added to cache", entity, 200));
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<ApiResponse> get(@PathVariable String id){
        Optional<EntityModel> result = cache.get(id);
        return ResponseEntity.ok(ApiResponse.success("Entity retrieved", result.get(), 200));
    }

    @DeleteMapping("/remove/{id}")
    public ResponseEntity<ApiResponse> remove(@PathVariable String id){
        cache.remove(id);
        return ResponseEntity.ok(ApiResponse.success("Entity removed from cache and DB", id, 200));
    }

    @DeleteMapping("/removeAll")
    public ResponseEntity<ApiResponse> removeAll(){
        cache.removeAll();
        return ResponseEntity.ok(ApiResponse.success("All entities removed from cache and DB", null, 200));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse> clear(){
        cache.clear();
        return ResponseEntity.ok(ApiResponse.success("Cache cleared (DB untouched)", null, 200));
    }
}
