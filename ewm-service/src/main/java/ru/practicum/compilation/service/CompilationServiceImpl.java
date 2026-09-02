package ru.practicum.compilation.service;

import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.mapper.CompilationMapper;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.NotFoundException;
import ru.practicum.util.OffsetPageRequest;

@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository repository;
    private final EventRepository eventRepository;
    private final CompilationMapper mapper;

    @Override
    @Transactional
    public CompilationDto addCompilation(NewCompilationDto dto) {
        List<Event> events = (dto.getEvents() == null || dto.getEvents().isEmpty())
                ? List.of()
                : eventRepository.findAllByIdIn(dto.getEvents());
        Compilation saved = repository.save(mapper.toEntity(dto, events));
        return mapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        if (!repository.existsById(compId)) {
            throw new NotFoundException("Compilation with id=" + compId + " was not found");
        }
        repository.deleteById(compId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest dto) {
        Compilation compilation = repository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));

        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }
        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }
        if (dto.getEvents() != null) {
            compilation.setEvents(new HashSet<>(eventRepository.findAllByIdIn(dto.getEvents())));
        }
        return mapper.toDto(compilation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        return repository.findAllByPinned(pinned, OffsetPageRequest.of(from, size)).getContent().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilation(Long compId) {
        Compilation compilation = repository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
        return mapper.toDto(compilation);
    }
}
