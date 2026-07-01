package com.jira.platform.boardservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class BoardServiceImpl implements BoardService {

  private final BoardRepository boardRepository;
  private final BoardCacheManager cacheManager;
  private final BoardEventPublisher eventPublisher;

  @Override
  @Transactional(readOnly = true)
  public Optional<BoardDTO> findById(String boardId) {
    return boardRepository.findById(boardId)
      .map(this::toDTO)
      .map(dto -> {
        cacheManager.put(boardId, dto);
        return dto;
      });
  }

  @Override
  @Transactional(readOnly = true)
  public List<BoardDTO> findByProject(String projectId) {
    return boardRepository.findByProjectId(projectId).stream()
      .map(this::toDTO)
      .toList();
  }

  @Override
  @Transactional
  public BoardDTO create(CreateBoardRequest request) {
    Board board = new Board();
    board.setName(request.name());
    board.setType(request.type());
    board.setProjectId(request.projectId());
    board.setOwnerId(request.ownerId());
    board.setCardLayout(request.cardLayout() != null ? request.cardLayout() : "FULL");

    Board saved = boardRepository.save(board);
    BoardDTO dto = toDTO(saved);

    eventPublisher.publish(new BoardCreatedEvent(dto));
    return dto;
  }

  @Override
  @Transactional
  public BoardDTO update(String boardId, UpdateBoardRequest request) {
    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new BoardNotFoundException(boardId));

    if (request.name() != null) board.setName(request.name());
    if (request.description() != null) board.setDescription(request.description());
    if (request.cardLayout() != null) board.setCardLayout(request.cardLayout());

    Board saved = boardRepository.save(board);
    BoardDTO dto = toDTO(saved);

    cacheManager.evict(boardId);
    eventPublisher.publish(new BoardUpdatedEvent(dto));
    return dto;
  }

  @Override
  @Transactional
  public void delete(String boardId) {
    Board board = boardRepository.findById(boardId)
      .orElseThrow(() -> new BoardNotFoundException(boardId));
    boardRepository.delete(board);
    cacheManager.evict(boardId);
    eventPublisher.publish(new BoardDeletedEvent(boardId));
  }

  private BoardDTO toDTO(Board board) {
    return new BoardDTO(
      board.getId(),
      board.getName(),
      board.getDescription(),
      board.getType().name(),
      board.getCardLayout(),
      board.getOwnerId(),
      board.getProjectId(),
      board.getCreatedAt(),
      board.getUpdatedAt()
    );
  }
}

@Service
class BoardCacheManager {
  private final Cache<String, BoardDTO> cache;

  BoardCacheManager() {
    this.cache = Caffeine.newBuilder()
      .expireAfterWrite(5, TimeUnit.MINUTES)
      .maximumSize
      .build();
  }

  void put(String key, BoardDTO dto) { cache.put(key, dto); }
  void evict(String key) { cache.invalidate(key); }
  Optional<BoardDTO> get(String key) { return Optional.ofNullable(cache.getIfPresent(key)); }
}

@Service
class BoardEventPublisher {
  private final ApplicationEventPublisher publisher;

  BoardEventPublisher(ApplicationEventPublisher publisher) { this.publisher = publisher; }
  void publish(BoardEvent event) { publisher.publishEvent(event); }
}

record BoardDTO(String id, String name, String description, String type,
                String cardLayout, String ownerId, String projectId,
                Instant createdAt, Instant updatedAt) {}

sealed interface BoardEvent {}
record BoardCreatedEvent(BoardDTO board) implements BoardEvent {}
record BoardUpdatedEvent(BoardDTO board) implements BoardEvent {}
record BoardDeletedEvent(String boardId) implements BoardEvent {}