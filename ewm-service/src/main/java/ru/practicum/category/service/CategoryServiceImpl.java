package ru.practicum.category.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryDto;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.util.OffsetPageRequest;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repository;
    private final EventRepository eventRepository;
    private final CategoryMapper mapper;

    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto dto) {
        if (repository.existsByName(dto.getName())) {
            throw new ConflictException("Category with name=" + dto.getName() + " already exists");
        }
        Category saved = repository.save(mapper.toEntity(dto));
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto dto) {
        Category category = getOrThrow(catId);
        if (repository.existsByNameAndIdNot(dto.getName(), catId)) {
            throw new ConflictException("Category with name=" + dto.getName() + " already exists");
        }
        category.setName(dto.getName());
        return mapper.toDto(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        getOrThrow(catId);
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("The category is not empty");
        }
        repository.deleteById(catId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategories(int from, int size) {
        return repository.findAll(OffsetPageRequest.of(from, size)).getContent().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategory(Long catId) {
        return mapper.toDto(getOrThrow(catId));
    }

    private Category getOrThrow(Long catId) {
        return repository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
    }
}
