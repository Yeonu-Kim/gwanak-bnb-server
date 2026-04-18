package boostcampsnu.gwanakbnbserver.controller;

import boostcampsnu.gwanakbnbserver.dto.room.CategoryResponse;
import boostcampsnu.gwanakbnbserver.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "카테고리 목록 API")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "카테고리 목록 조회")
    public List<CategoryResponse> getAll() {
        return categoryService.getAll();
    }
}
