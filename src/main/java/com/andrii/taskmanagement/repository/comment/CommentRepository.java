package com.andrii.taskmanagement.repository.comment;

import com.andrii.taskmanagement.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
