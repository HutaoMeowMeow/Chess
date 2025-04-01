package chessproject;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ChessProject {
    private JFrame frame;
    private JButton[][] buttons = new JButton[8][8];
    private String[][] board = new String[8][8];
    private int selectedRow = -1, selectedCol = -1;
    private boolean whiteTurn = true;
    private boolean kingInCheckNotified = false; // Track if we've already shown check notification
    
    public ChessProject() {
        frame = new JFrame("Chess Game");
        frame.setSize(600, 650); // Increased height to accommodate control panel
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Create a main panel with BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Create the chess board panel
        JPanel boardPanel = new JPanel(new GridLayout(8, 8));
        
        // Create control panel for buttons
        JPanel controlPanel = new JPanel();
        JButton replayButton = new JButton("New Game");
        replayButton.setFocusPainted(false);
        replayButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetGame();
            }
        });
        controlPanel.add(replayButton);
        
        // Add panels to main panel
        mainPanel.add(boardPanel, BorderLayout.CENTER);
        mainPanel.add(controlPanel, BorderLayout.SOUTH);
        
        // Add main panel to frame
        frame.add(mainPanel);

        initializeBoard();
        initializeUI(boardPanel);

        frame.setVisible(true);
        
        // Check if a king is in check at game start
        SwingUtilities.invokeLater(() -> {
            if (isKingInCheck('W')) {
                JOptionPane.showMessageDialog(frame, "White King is in check!");
                kingInCheckNotified = true;
            } else if (isKingInCheck('B')) {
                JOptionPane.showMessageDialog(frame, "Black King is in check!");
                kingInCheckNotified = true;
            }
        });
    }

    private void initializeBoard() {
        String[] pieces = {"R", "N", "B", "Q", "K", "B", "N", "R"};
        for (int i = 0; i < 8; i++) {
            board[0][i] = "B" + pieces[i];
            board[1][i] = "BP";
            board[6][i] = "WP";
            board[7][i] = "W" + pieces[i];
        }
        
        // Clear the middle rows
        for (int row = 2; row < 6; row++) {
            for (int col = 0; col < 8; col++) {
                board[row][col] = null;
            }
        }
    }

    private void initializeUI(JPanel boardPanel) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                buttons[row][col] = new JButton();
                buttons[row][col].setFont(new Font("Arial", Font.BOLD, 20));
                buttons[row][col].setFocusPainted(false);

                if ((row + col) % 2 == 0) {
                    buttons[row][col].setBackground(Color.WHITE);
                } else {
                    buttons[row][col].setBackground(Color.GRAY);
                }

                updateButton(row, col);

                int finalRow = row;
                int finalCol = col;
                buttons[row][col].addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        onButtonClick(finalRow, finalCol);
                    }
                });

                boardPanel.add(buttons[row][col]);
            }
        }
    }
    
    // Reset the game to initial state
    private void resetGame() {
        // Reset board state
        initializeBoard();
        
        // Update UI to match new board state
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                updateButton(row, col);
            }
        }
        
        // Reset game variables
        selectedRow = -1;
        selectedCol = -1;
        whiteTurn = true;
        kingInCheckNotified = false;
        
        // Reset highlights
        resetHighlights();
        
        // Optional: Display a message
        JOptionPane.showMessageDialog(frame, "Game has been reset. White's turn to play.");
    }

    private void updateButton(int row, int col) {
        if (board[row][col] != null) {
            setPieceImage(row, col, board[row][col]);
        } else {
            buttons[row][col].setIcon(null);
            buttons[row][col].setText("");
        }
    }

    private void setPieceImage(int row, int col, String piece) {
        String imagePath = "resources/" + piece + ".png";
        ImageIcon icon = new ImageIcon(imagePath);
        Image img = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
        buttons[row][col].setIcon(new ImageIcon(img));
        buttons[row][col].setText("");
    }

    private void onButtonClick(int row, int col) {
        char currentColor = whiteTurn ? 'W' : 'B';
        boolean currentKingInCheck = isKingInCheck(currentColor);
        
        if (selectedRow == -1 && selectedCol == -1) {
            // First click - selecting a piece
            if (board[row][col] != null) {
                char pieceColor = board[row][col].charAt(0);
                if ((whiteTurn && pieceColor == 'W') || (!whiteTurn && pieceColor == 'B')) {
                    selectedRow = row;
                    selectedCol = col;
                    highlightLegalMoves(row, col);
                    
                    // Only show the check notification once per turn, not on every piece selection
                    if (currentKingInCheck && !kingInCheckNotified) {
                        JOptionPane.showMessageDialog(frame, (whiteTurn ? "White" : "Black") + " King is in check! You must defend your king.");
                        kingInCheckNotified = true;
                    }
                }
            }
        } else {
            // Second click - trying to make a move
            // Clear any highlighting first
            resetHighlights();
            
            if (row == selectedRow && col == selectedCol) {
                // Clicking the same piece again deselects it
                selectedRow = -1;
                selectedCol = -1;
                return;
            }
            
            if (isValidMove(selectedRow, selectedCol, row, col)) {
                // Try to make the move
                if (tryMove(selectedRow, selectedCol, row, col)) {
                    // Move was successful
                    selectedRow = -1;
                    selectedCol = -1;
                    whiteTurn = !whiteTurn;
                    kingInCheckNotified = false; // Reset for next player's turn
                    
                    // Check for opponent's king in check or checkmate
                    char opponentColor = whiteTurn ? 'W' : 'B';
                    if (isKingInCheck(opponentColor)) {
                        if (isCheckmate(opponentColor)) {
                            JOptionPane.showMessageDialog(frame, (!whiteTurn ? "White" : "Black") + " wins! Checkmate!");
                        } else {
                            JOptionPane.showMessageDialog(frame, (opponentColor == 'W' ? "White" : "Black") + " King is in check!");
                        }
                    }
                }
                // If tryMove returns false, it will already have shown an appropriate message
            } else if (board[row][col] != null && board[row][col].charAt(0) == currentColor) {
                // Clicking a different piece of the same color
                selectedRow = row;
                selectedCol = col;
                highlightLegalMoves(row, col);
            } else {
                // Invalid move - deselect the piece
                selectedRow = -1;
                selectedCol = -1;
            }
        }
    }
    
    // Highlight legal moves for better user experience
    private void highlightLegalMoves(int row, int col) {
        if (board[row][col] == null) return;
        
        for (int toRow = 0; toRow < 8; toRow++) {
            for (int toCol = 0; toCol < 8; toCol++) {
                if (isValidMove(row, col, toRow, toCol)) {
                    // Try the move to see if it would leave the king in check
                    String originalSrc = board[row][col];
                    String originalDest = board[toRow][toCol];
                    
                    board[toRow][toCol] = originalSrc;
                    board[row][col] = null;
                    
                    char pieceColor = originalSrc.charAt(0);
                    boolean kingStillInCheck = isKingInCheck(pieceColor);
                    
                    // Revert the move
                    board[row][col] = originalSrc;
                    board[toRow][toCol] = originalDest;
                    
                    if (!kingStillInCheck) {
                        // Legal move - highlight the square
                        Color originalColor = buttons[toRow][toCol].getBackground();
                        buttons[toRow][toCol].setBackground(
                            originalColor == Color.WHITE ? new Color(173, 216, 230) : new Color(100, 149, 237)
                        );
                    }
                }
            }
        }
    }
    
    // Reset all square highlights
    private void resetHighlights() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                buttons[r][c].setBackground((r + c) % 2 == 0 ? Color.WHITE : Color.GRAY);
            }
        }
    }
    
    // Tries to make a move and returns true if successful
    private boolean tryMove(int fromRow, int fromCol, int toRow, int toCol) {
        String movingPiece = board[fromRow][fromCol];
        String capturedPiece = board[toRow][toCol];
        char pieceColor = movingPiece.charAt(0);
        
        // Store original state for castling
        boolean wasCastle = false;
        int rookFromRow = -1, rookFromCol = -1, rookToRow = -1, rookToCol = -1;
        String rookPiece = null;
        
        // Make the move
        if (movingPiece.charAt(1) == 'K' && Math.abs(fromCol - toCol) == 2) {
            // Castling
            wasCastle = true;
            rookFromRow = fromRow;
            rookFromCol = (toCol > fromCol) ? 7 : 0;
            rookToRow = fromRow;
            rookToCol = (toCol > fromCol) ? toCol - 1 : toCol + 1;
            rookPiece = board[rookFromRow][rookFromCol];
            
            board[toRow][toCol] = movingPiece;
            board[fromRow][fromCol] = null;
            board[rookToRow][rookToCol] = rookPiece;
            board[rookFromRow][rookFromCol] = null;
        } else {
            // Regular move
            board[toRow][toCol] = movingPiece;
            board[fromRow][fromCol] = null;
        }
        
        // Check if our king is in check after the move
        if (isKingInCheck(pieceColor)) {
            // Invalid move - revert it
            if (wasCastle) {
                board[fromRow][fromCol] = movingPiece;
                board[toRow][toCol] = null;
                board[rookFromRow][rookFromCol] = rookPiece;
                board[rookToRow][rookToCol] = null;
                updateButton(rookFromRow, rookFromCol);
                updateButton(rookToRow, rookToCol);
            } else {
                board[fromRow][fromCol] = movingPiece;
                board[toRow][toCol] = capturedPiece;
            }
            
            updateButton(fromRow, fromCol);
            updateButton(toRow, toCol);
            
            // Show appropriate message
            if (isKingInCheck(pieceColor)) {
                JOptionPane.showMessageDialog(frame, "Your king is in check! This move doesn't resolve the check.");
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid move: Your king would be in check!");
            }
            
            return false;
        }
        
        // Check for pawn promotion
        if (movingPiece.charAt(1) == 'P' && (toRow == 0 || toRow == 7)) {
            promotePawn(toRow, toCol);
        }
        
        // Update the UI
        updateButton(fromRow, fromCol);
        updateButton(toRow, toCol);
        if (wasCastle) {
            updateButton(rookFromRow, rookFromCol);
            updateButton(rookToRow, rookToCol);
        }
        
        return true;
    }
    
    // Handle pawn promotion
    private void promotePawn(int row, int col) {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(
            frame, 
            "Choose a piece for pawn promotion:", 
            "Pawn Promotion", 
            JOptionPane.DEFAULT_OPTION, 
            JOptionPane.QUESTION_MESSAGE, 
            null, 
            options, 
            options[0]
        );
        
        char color = board[row][col].charAt(0);
        char promotionPiece;
        
        switch (choice) {
            case 1: promotionPiece = 'R'; break;
            case 2: promotionPiece = 'B'; break;
            case 3: promotionPiece = 'N'; break;
            default: promotionPiece = 'Q'; break;
        }
        
        board[row][col] = color + "" + promotionPiece;
        updateButton(row, col);
    }

    private boolean isKingInCheck(char kingColor) {
        int kingRow = -1, kingCol = -1;

        // Locate the king
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                if (board[row][col] != null && board[row][col].equals(kingColor + "K")) {
                    kingRow = row;
                    kingCol = col;
                    break;
                }
            }
            if (kingRow != -1) break;
        }

        if (kingRow == -1 || kingCol == -1) return false; // King not found (shouldn't happen)

        // Check for attacks by opponent's pieces
        return isAttackedByPawn(kingRow, kingCol, kingColor) ||
               isAttackedByKnight(kingRow, kingCol, kingColor) ||
               isAttackedBySlidingPiece(kingRow, kingCol, kingColor, new char[]{'R', 'Q'}) ||  // Rooks & Queens
               isAttackedBySlidingPiece(kingRow, kingCol, kingColor, new char[]{'B', 'Q'}) ||  // Bishops & Queens
               isAttackedByKing(kingRow, kingCol, kingColor);
    }

    // Check if king is attacked by a pawn
    private boolean isAttackedByPawn(int kingRow, int kingCol, char kingColor) {
        int direction = (kingColor == 'W') ? -1 : 1;
        int attackRow = kingRow + direction;
        char enemyColor = (kingColor == 'W') ? 'B' : 'W';
        
        if (attackRow >= 0 && attackRow < 8) {
            if (kingCol > 0 && board[attackRow][kingCol - 1] != null && 
                board[attackRow][kingCol - 1].equals(enemyColor + "P")) {
                return true;
            }
            if (kingCol < 7 && board[attackRow][kingCol + 1] != null && 
                board[attackRow][kingCol + 1].equals(enemyColor + "P")) {
                return true;
            }
        }
        return false;
    }

    // Check if king is attacked by a knight
    private boolean isAttackedByKnight(int kingRow, int kingCol, char kingColor) {
        int[][] knightMoves = {{-2, -1}, {-2, 1}, {2, -1}, {2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}};
        char enemyColor = (kingColor == 'W') ? 'B' : 'W';

        for (int[] move : knightMoves) {
            int newRow = kingRow + move[0];
            int newCol = kingCol + move[1];
            if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8 &&
                board[newRow][newCol] != null && board[newRow][newCol].equals(enemyColor + "N")) {
                return true;
            }
        }
        return false;
    }

    // Check if king is attacked by a rook, bishop, or queen
    private boolean isAttackedBySlidingPiece(int kingRow, int kingCol, char kingColor, char[] pieceTypes) {
        int[][] directions = (pieceTypes[0] == 'R') ? new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}} // Rook & Queen (straight)
                                                 : new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}}; // Bishop & Queen (diagonal)

        char enemyColor = (kingColor == 'W') ? 'B' : 'W';

        for (int[] dir : directions) {
            int row = kingRow + dir[0];
            int col = kingCol + dir[1];

            while (row >= 0 && row < 8 && col >= 0 && col < 8) {
                if (board[row][col] != null) {
                    if (board[row][col].charAt(0) == enemyColor && 
                        (board[row][col].charAt(1) == pieceTypes[0] || board[row][col].charAt(1) == pieceTypes[1])) {
                        return true;
                    }
                    break; // Blocked by another piece
                }
                row += dir[0];
                col += dir[1];
            }
        }
        return false;
    }

    // Check if king is attacked by another king
    private boolean isAttackedByKing(int kingRow, int kingCol, char kingColor) {
        char enemyColor = (kingColor == 'W') ? 'B' : 'W';

        for (int r = -1; r <= 1; r++) {
            for (int c = -1; c <= 1; c++) {
                if (r == 0 && c == 0) continue; // Skip the center
                int newRow = kingRow + r;
                int newCol = kingCol + c;
                if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8 &&
                    board[newRow][newCol] != null && board[newRow][newCol].equals(enemyColor + "K")) {
                    return true;
                }
            }
        }
        return false;
    }

    // Check if a king is in checkmate
    private boolean isCheckmate(char kingColor) {
        // If king is not in check, it's not checkmate
        if (!isKingInCheck(kingColor)) {
            return false;
        }
        
        // Check if any piece can make a move that gets out of check
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                if (board[fromRow][fromCol] != null && board[fromRow][fromCol].charAt(0) == kingColor) {
                    // For each friendly piece
                    for (int toRow = 0; toRow < 8; toRow++) {
                        for (int toCol = 0; toCol < 8; toCol++) {
                            if (isValidMove(fromRow, fromCol, toRow, toCol)) {
                                // Try the move
                                String originalSrc = board[fromRow][fromCol];
                                String originalDest = board[toRow][toCol];
                                boolean wasCastle = false;
                                String rookPiece = null;
                                int rookFromRow = -1, rookFromCol = -1, rookToRow = -1, rookToCol = -1;
                                
                                // Handle castle
                                if (originalSrc.charAt(1) == 'K' && Math.abs(fromCol - toCol) == 2) {
                                    wasCastle = true;
                                    rookFromRow = fromRow;
                                    rookFromCol = (toCol > fromCol) ? 7 : 0;
                                    rookToRow = fromRow;
                                    rookToCol = (toCol > fromCol) ? toCol - 1 : toCol + 1;
                                    rookPiece = board[rookFromRow][rookFromCol];
                                    
                                    board[fromRow][fromCol] = null;
                                    board[toRow][toCol] = originalSrc;
                                    board[rookFromRow][rookFromCol] = null;
                                    board[rookToRow][rookToCol] = rookPiece;
                                } else {
                                    // Regular move
                                    board[toRow][toCol] = originalSrc;
                                    board[fromRow][fromCol] = null;
                                }
                                
                                boolean stillInCheck = isKingInCheck(kingColor);
                                
                                // Revert move
                                if (wasCastle) {
                                    board[fromRow][fromCol] = originalSrc;
                                    board[toRow][toCol] = null;
                                    board[rookFromRow][rookFromCol] = rookPiece;
                                    board[rookToRow][rookToCol] = null;
                                } else {
                                    board[fromRow][fromCol] = originalSrc;
                                    board[toRow][toCol] = originalDest;
                                }
                                
                                if (!stillInCheck) {
                                    return false; // Found a move that escapes check
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // No valid moves found that escape check
        return true;
    }

    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        String piece = board[fromRow][fromCol];
        if (piece == null) return false;
        
        // Can't move to a square with your own piece
        if (board[toRow][toCol] != null && board[toRow][toCol].charAt(0) == piece.charAt(0)) {
            return false;
        }
        
        char type = piece.charAt(1);

        switch (type) {
            case 'P': // Pawn
                int direction = piece.charAt(0) == 'W' ? -1 : 1;
                int startRow = piece.charAt(0) == 'W' ? 6 : 1;
                
                // Regular move forward
                if (toCol == fromCol && toRow == fromRow + direction && board[toRow][toCol] == null) {
                    return true;
                }
                
                // Double move from start position
                if (fromRow == startRow && toCol == fromCol && toRow == fromRow + 2 * direction &&
                    board[fromRow + direction][fromCol] == null && board[toRow][toCol] == null) {
                    return true;
                }
                
                // Capture diagonally
                if (Math.abs(toCol - fromCol) == 1 && toRow == fromRow + direction &&
                    board[toRow][toCol] != null && board[toRow][toCol].charAt(0) != piece.charAt(0)) {
                    return true;
                }
                
                return false;
                
            case 'R': // Rook
                return (fromRow == toRow || fromCol == toCol) && isPathClear(fromRow, fromCol, toRow, toCol);
                
            case 'B': // Bishop
                return Math.abs(fromRow - toRow) == Math.abs(fromCol - toCol) && isPathClear(fromRow, fromCol, toRow, toCol);
                
            case 'Q': // Queen
                return (fromRow == toRow || fromCol == toCol || Math.abs(fromRow - toRow) == Math.abs(fromCol - toCol)) && 
                       isPathClear(fromRow, fromCol, toRow, toCol);
                
            case 'K': // King
                // Normal king move (one square in any direction)
                if (Math.abs(fromRow - toRow) <= 1 && Math.abs(fromCol - toCol) <= 1) {
                    return true;
                }
                // Castling
                return canCastle(fromRow, fromCol, toRow, toCol);
                
            case 'N': // Knight
                return (Math.abs(fromRow - toRow) == 2 && Math.abs(fromCol - toCol) == 1) ||
                       (Math.abs(fromRow - toRow) == 1 && Math.abs(fromCol - toCol) == 2);
                
            default:
                return false;
        }
    }

    private boolean canCastle(int fromRow, int fromCol, int toRow, int toCol) {
        if (board[fromRow][fromCol] == null || board[fromRow][fromCol].charAt(1) != 'K') {
            return false; // Must be a king
        }

        if (fromRow != toRow || Math.abs(fromCol - toCol) != 2) {
            return false; // Must move horizontally by exactly 2 squares
        }

        char kingColor = board[fromRow][fromCol].charAt(0);
        if (isKingInCheck(kingColor)) {
            return false; // Can't castle while in check
        }

        int rookCol = (toCol > fromCol) ? 7 : 0; // Determine which rook (kingside or queenside)
        if (board[fromRow][rookCol] == null || board[fromRow][rookCol].charAt(1) != 'R' ||
            board[fromRow][rookCol].charAt(0) != kingColor) {
            return false; // Rook must be in the corner and of same color
        }

        // Check that the path between king and rook is clear
        int step = (toCol > fromCol) ? 1 : -1;
        for (int c = fromCol + step; c != rookCol; c += step) {
            if (board[fromRow][c] != null) {
                return false; // Path is blocked
            }
        }

        // Check that the king doesn't pass through check
        int midCol = fromCol + step; // Square the king passes through
        String originalKing = board[fromRow][fromCol];
        board[fromRow][fromCol] = null;
        board[fromRow][midCol] = originalKing;
        
        boolean passesCheck = isKingInCheck(kingColor);
        
        // Restore the king
        board[fromRow][midCol] = null;
        board[fromRow][fromCol] = originalKing;
        
        return !passesCheck;
    }

    private void performCastle(int fromRow, int fromCol, int toRow, int toCol) {
        int rookCol = (toCol > fromCol) ? 7 : 0;
        int newRookCol = (toCol > fromCol) ? toCol - 1 : toCol + 1;

        board[toRow][toCol] = board[fromRow][fromCol]; // Move king
        board[fromRow][fromCol] = null;
        board[toRow][newRookCol] = board[fromRow][rookCol]; // Move rook
        board[fromRow][rookCol] = null;

        updateButton(fromRow, fromCol);
        updateButton(toRow, toCol);
        updateButton(fromRow, rookCol);
        updateButton(toRow, newRookCol);
    }
    
    private boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol) {
        int rowStep = Integer.compare(toRow - fromRow, 0);
        int colStep = Integer.compare(toCol - fromCol, 0);
        
        int r = fromRow + rowStep;
        int c = fromCol + colStep;
        
        while (r != toRow || c != toCol) {
            if (board[r][c] != null) {
                return false; // Path is blocked
            }
            r += rowStep;
            c += colStep;
        }
        
        return true;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ChessProject();
        });
    }
}