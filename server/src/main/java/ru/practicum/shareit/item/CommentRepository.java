package ru.practicum.shareit.item;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.shareit.item.model.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    // комментарии сразу для нескольких вещей; автора подгружаем тем же запросом
    @EntityGraph(attributePaths = "author")
    List<Comment> findByItemIdInOrderByCreatedDesc(List<Long> itemIds);
}
