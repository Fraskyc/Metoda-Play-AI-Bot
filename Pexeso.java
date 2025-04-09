import java.awt.Point;
import java.util.*;

public class Pexeso {
    private static final int SIZE = 4;
    private static final String[][] board = new String[SIZE][SIZE];
    private static final boolean[][] revealed = new boolean[SIZE][SIZE];
    private static final Scanner scanner = new Scanner(System.in);

    private static int playerScore = 0;
    private static int botScore = 0;

    public static void main(String[] args) {
        initBoard();
        shuffleBoard();
        playGame();
        showFinalScore();
    }

    private static void initBoard() {
        List<String> cards = new ArrayList<>();
        for (char c = 'A'; c < 'A' + (SIZE * SIZE) / 2; c++) {
            cards.add(String.valueOf(c));
            cards.add(String.valueOf(c));
        }

        int index = 0;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = cards.get(index++);
                revealed[i][j] = false;
            }
        }
    }

    private static void shuffleBoard() {
        Random rand = new Random();
        for (int i = 0; i < SIZE * SIZE; i++) {
            int x1 = rand.nextInt(SIZE);
            int y1 = rand.nextInt(SIZE);
            int x2 = rand.nextInt(SIZE);
            int y2 = rand.nextInt(SIZE);

            String temp = board[x1][y1];
            board[x1][y1] = board[x2][y2];
            board[x2][y2] = temp;
        }
    }

    private static void playGame() {
        boolean playerTurn = true;
        AIBot bot = new AIBot(board, revealed);

        while (!gameOver()) {
            printBoard();

            if (playerTurn) {
                System.out.println("▶ Tah hráče:");
                int[] first = getPlayerChoice();
                reveal(first);
                bot.rememberCard(new Point(first[0], first[1]));
                printBoard();

                int[] second = getPlayerChoice();
                reveal(second);
                bot.rememberCard(new Point(second[0], second[1]));
                printBoard();

                if (isMatch(first, second)) {
                    System.out.println("✔ Správný pár!");
                    playerScore++;
                } else {
                    System.out.println("✘ Nesprávný pár.");
                    hide(first);
                    hide(second);
                    playerTurn = false;
                }

                bot.cleanupMemory(); 
            } else {
                System.out.println("▶ Tah bota:");

                Point first = bot.play(new HashSet<>());
                reveal(new int[]{first.x, first.y});
                bot.rememberCard(first);
                printBoard();
                wait(1000);

                Set<Point> chosen = new HashSet<>();
                chosen.add(first);
                Point second = bot.play(chosen);
                reveal(new int[]{second.x, second.y});
                bot.rememberCard(second);
                printBoard();
                wait(1000);

                if (isMatch(new int[]{first.x, first.y}, new int[]{second.x, second.y})) {
                    System.out.println("✔ Bot našel pár!");
                    botScore++;
                } else {
                    System.out.println("✘ Bot nenašel pár.");
                    hide(new int[]{first.x, first.y});
                    hide(new int[]{second.x, second.y});
                    playerTurn = true;
                }

                bot.cleanupMemory(); 
            }
        }
    }

    private static void wait(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {}
    }

    private static boolean gameOver() {
        return (playerScore + botScore) == (SIZE * SIZE) / 2;
    }

    private static void printBoard() {
        System.out.println("\n  0 1 2 3");
        for (int i = 0; i < SIZE; i++) {
            System.out.print(i + " ");
            for (int j = 0; j < SIZE; j++) {
                if (revealed[i][j]) {
                    System.out.print(board[i][j] + " ");
                } else {
                    System.out.print("* ");
                }
            }
            System.out.println();
        }
    }

    private static int[] getPlayerChoice() {
        while (true) {
            System.out.print("Zadej řádek a sloupec (např. 1 2): ");
            int row = scanner.nextInt();
            int col = scanner.nextInt();

            if (row >= 0 && row < SIZE && col >= 0 && col < SIZE && !revealed[row][col]) {
                return new int[]{row, col};
            } else {
                System.out.println("Neplatná volba, zkus znovu.");
            }
        }
    }

    private static void reveal(int[] pos) {
        revealed[pos[0]][pos[1]] = true;
    }

    private static void hide(int[] pos) {
        revealed[pos[0]][pos[1]] = false;
    }

    private static boolean isMatch(int[] a, int[] b) {
        return board[a[0]][a[1]].equals(board[b[0]][b[1]]);
    }

    private static void showFinalScore() {
        System.out.println("\n🎉 Konec hry!");
        System.out.println("Skóre hráče: " + playerScore);
        System.out.println("Skóre bota: " + botScore);

        if (playerScore > botScore) {
            System.out.println("🏆 Vyhrál jsi!");
        } else if (botScore > playerScore) {
            System.out.println("🤖 Bot vyhrál!");
        } else {
            System.out.println("🤝 Remíza!");
        }
    }

    // ----------- AI Bot -----------
    private static class AIBot {
        private final Map<String, List<Point>> memory = new HashMap<>();
        private final String[][] board;
        private final boolean[][] revealed;

        public AIBot(String[][] board, boolean[][] revealed) {
            this.board = board;
            this.revealed = revealed;
        }

        public void rememberCard(Point p) {
            String value = board[p.x][p.y];
            memory.putIfAbsent(value, new ArrayList<>());
            boolean alreadyKnown = memory.get(value).stream().anyMatch(pt -> pt.equals(p));
            if (!alreadyKnown) {
                memory.get(value).add(p);
            }
        }

        public void cleanupMemory() {
            Iterator<Map.Entry<String, List<Point>>> it = memory.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, List<Point>> entry = it.next();
                List<Point> valid = new ArrayList<>();
                for (Point p : entry.getValue()) {
                    if (!revealed[p.x][p.y]) {
                        valid.add(p);
                    }
                }
                if (valid.size() < 2) {
                    entry.setValue(valid); 
                }
                if (valid.isEmpty()) {
                    it.remove(); 
                }
            }
        }

        public Point play(Set<Point> alreadyChosen) {
            for (Map.Entry<String, List<Point>> entry : memory.entrySet()) {
                List<Point> points = entry.getValue();
                if (points.size() >= 2) {
                    for (Point p : points) {
                        if (!revealed[p.x][p.y] && !alreadyChosen.contains(p)) {
                            return p;
                        }
                    }
                }
            }

            for (Map.Entry<String, List<Point>> entry : memory.entrySet()) {
                for (Point p : entry.getValue()) {
                    if (!revealed[p.x][p.y] && !alreadyChosen.contains(p)) {
                        return p;
                    }
                }
            }

            Random rand = new Random();
            while (true) {
                int x = rand.nextInt(board.length);
                int y = rand.nextInt(board[0].length);
                Point p = new Point(x, y);
                if (!revealed[x][y] && !alreadyChosen.contains(p)) {
                    return p;
                }
            }
        }
    }
}
