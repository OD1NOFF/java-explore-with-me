package ru.practicum.category.service;

import java.util.List;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;

public interface CategoryService {

    CategoryDto addCategory(NewCategoryDto dto);

    CategoryDto updateCategory(Long catId, CategoryDto dto);

    void deleteCategory(Long catId);

    List<CategoryDto> getCategories(int from, int size);

    CategoryDto getCategory(Long catId);
}
