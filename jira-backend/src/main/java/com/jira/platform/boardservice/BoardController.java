import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

  private final BoardService boardService;

  public BoardController(BoardService boardService) {
    this.boardService = boardService;
  }

  @GetMapping("/{boardId}")
  public ResponseEntity<BoardDTO> getBoard(@PathVariable String boardId) {
    return boardService.findById(boardId)
      .map(ResponseEntity::ok)
      .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/project/{projectId}")
  public ResponseEntity<List<BoardDTO>> getBoardsByProject(@PathVariable String projectId) {
    List<BoardDTO> boards = boardService.findByProject(projectId);
    return ResponseEntity.ok(boards);
  }

  @PostMapping
  public ResponseEntity<BoardDTO> createBoard(@RequestBody CreateBoardRequest request) {
    BoardDTO board = boardService.create(request);
    return ResponseEntity.status(201).body(board);
  }

  @PutMapping("/{boardId}")
  public ResponseEntity<BoardDTO> updateBoard(
      @PathVariable String boardId,
      @RequestBody UpdateBoardRequest request) {
    try {
      BoardDTO board = boardService.update(boardId, request);
      return ResponseEntity.ok(board);
    } catch (BoardNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{boardId}")
  public ResponseEntity<Void> deleteBoard(@PathVariable String boardId) {
    try {
      boardService.delete(boardId);
      return ResponseEntity.noContent().build();
    } catch (BoardNotFoundException e) {
      return ResponseEntity.notFound().build();
    }
  }
}

@RestController
@RequestMapping("/api/boards/{boardId}/columns")
public class BoardColumnController {

  private final ColumnService columnService;

  @GetMapping
  public ResponseEntity<List<ColumnDTO>> getColumns(@PathVariable String boardId) {
    return ResponseEntity.ok(columnService.findByBoard(boardId));
  }

  @PostMapping
  public ResponseEntity<ColumnDTO> createColumn(
      @PathVariable String boardId,
      @RequestBody CreateColumnRequest request) {
    ColumnDTO column = columnService.create(boardId, request);
    return ResponseEntity.status(201).body(column);
  }

  @PutMapping("/{columnId}")
  public ResponseEntity<ColumnDTO> updateColumn(
      @PathVariable String boardId,
      @PathVariable String columnId,
      @RequestBody UpdateColumnRequest request) {
    return ResponseEntity.ok(columnService.update(boardId, columnId, request));
  }

  @DeleteMapping("/{columnId}")
  public ResponseEntity<Void> deleteColumn(
      @PathVariable String boardId,
      @PathVariable String columnId) {
    columnService.delete(boardId, columnId);
    return ResponseEntity.noContent().build();
  }
}

@RestController
@RequestMapping("/api/boards/{boardId}/issues")
public class BoardIssueController {

  private final BoardIssueService issueService;

  @GetMapping
  public ResponseEntity<List<BoardIssueDTO>> getBoardIssues(
      @PathVariable String boardId,
      @RequestParam(required = false) String sprintId,
      @RequestParam(required = false) String versionId) {
    return ResponseEntity.ok(issueService.getBoardIssues(boardId, sprintId, versionId));
  }

  @PostMapping("/{issueId}/move")
  public ResponseEntity<Void> moveIssue(
      @PathVariable String boardId,
      @PathVariable String issueId,
      @RequestBody MoveIssueRequest request) {
    issueService.moveIssue(boardId, issueId, request);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/reorder")
  public ResponseEntity<Void> reorderIssues(
      @PathVariable String boardId,
      @RequestBody ReorderRequest request) {
    issueService.reorderIssues(boardId, request.issueIds());
    return ResponseEntity.ok().build();
  }
}