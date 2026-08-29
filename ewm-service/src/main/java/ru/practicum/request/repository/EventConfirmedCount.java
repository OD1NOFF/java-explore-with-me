package ru.practicum.request.repository;

/** Проекция для батч-подсчёта подтверждённых заявок по нескольким событиям за один запрос. */
public interface EventConfirmedCount {

    Long getEventId();

    Long getCount();
}
