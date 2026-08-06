package com.skala.shopping.catalog.internal.web;

import com.skala.shopping.catalog.CategoryView;
import com.skala.shopping.catalog.internal.CategoryApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/categories")
class CategoryController {
    private final CategoryApplicationService service;
    CategoryController(CategoryApplicationService service){this.service=service;}
    @GetMapping List<CategoryView> list(){return service.getCategories();}
    @PostMapping CategoryView create(@Valid @RequestBody CategoryRequest request){return service.create(request.getName(),request.getDescription());}
    @PutMapping("/{id}") CategoryView update(@PathVariable UUID id,@Valid @RequestBody CategoryRequest request){return service.update(id,request.getName(),request.getDescription());}
    @DeleteMapping("/{id}") void delete(@PathVariable UUID id){service.delete(id);}
    public static final class CategoryRequest {
        @NotBlank @Size(max=100) private String name; @Size(max=500) private String description;
        public CategoryRequest(){} public String getName(){return name;} public void setName(String name){this.name=name;}
        public String getDescription(){return description;} public void setDescription(String value){description=value;}
    }
}
