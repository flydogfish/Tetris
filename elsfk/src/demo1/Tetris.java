package demo1;

import org.w3c.dom.xpath.XPathNamespace;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

// 俄罗斯方块主体
public class Tetris extends JPanel {

    //正在下落的方块
    private Tetromino currentOne = Tetromino.randomOne();
    //将要下落的方块
    private Tetromino nextOne = Tetromino.randomOne();
    //游戏主区域
    private Cell[][] wall = new Cell[27][14];
    //单元格值为xx像素
    private static final int CELL_SIZE = 29;
    //声明游戏分数池
    int[] scores_pool = {0, 1, 2, 5, 10};
    // 当前游戏得分
    private int totalScore = 0;
    // 当前消除行数
    private int totalLine = 0;
    //游戏状态
    public static final int PLAYING = 0;
    public static final int PAUSE = 1;
    public static final int GAMEOVER = 2;
    //当前游戏的状态
    private int game_state = 2;
    // 数组显示游戏状态
    String[] show_state = {"P[pause]", "C[continue]", "S[replay]"};


    //载入方块图片
    public static BufferedImage I;
    public static BufferedImage J;
    public static BufferedImage L;
    public static BufferedImage O;
    public static BufferedImage S;
    public static BufferedImage T;
    public static BufferedImage Z;
    public static BufferedImage backImage;

    static {
        try {
            I = ImageIO.read(new File("images/I.png"));
            J = ImageIO.read(new File("images/J.png"));
            L = ImageIO.read(new File("images/L.png"));
            O = ImageIO.read(new File("images/O.png"));
            S = ImageIO.read(new File("images/S.png"));
            T = ImageIO.read(new File("images/T.png"));
            Z = ImageIO.read(new File("images/Z.png"));
            backImage = ImageIO.read(new File("images/background.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(backImage, 0, 0, null);
        //偏移量
        g.translate(30, 45);
        // 绘制游戏主区域
        paintWall(g);
        // 绘制正在下落的四方格
        paintCurrentOne(g);
        // 绘制下一个将要的四方格
        paintNextOne(g);
        //绘制游戏得分
        paintScore(g);
        // 绘制当前游戏状态
        paintState(g);
    }

    public void start() {
        game_state = PLAYING;
        KeyListener l = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                switch (code) {
                    case KeyEvent.VK_DOWN:
                        sortDropAction();//下落一格
                        break;
                    case KeyEvent.VK_LEFT:
                        moveLeftAction();//左移
                        break;
                    case KeyEvent.VK_RIGHT:
                        moveRightAction();//右移
                        break;
                    case KeyEvent.VK_UP:
                        rotateRightAction();//旋转
                        break;
                    case KeyEvent.VK_SPACE:
                        handDropAction();//下落
                        break;
                    case KeyEvent.VK_P:
                        //判断游戏状态
                        if ( game_state == PLAYING){
                              game_state = PAUSE;
                        }
                        break;
                    case KeyEvent.VK_C:
                        if ( game_state == PAUSE){
                            game_state = PLAYING;
                        }
                        break;
                    case KeyEvent.VK_S:
                        //游戏重新开始
                        game_state = PLAYING;
                        wall = new Cell[27][14];
                        currentOne = Tetromino.randomOne();
                        nextOne = Tetromino.randomOne();
                        totalLine = 0;
                        totalScore = 0;
                        break;



                }
            }
        };

        //设置窗口焦点
        this.addKeyListener(l);
        this.requestFocus();

        while (true){
            //判断，当前游戏是游戏中，每隔0.5s下落
            if (game_state==PLAYING){
                try{
                    Thread.sleep(500);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                //判断能否下落
                if (canDrop()){
                    currentOne.softDrop();
                }else{
                    //嵌入
                    landToWall();
                    //消行
                    destroyLine();
                    //是否结束
                    if (isGameOver()){
                        game_state = GAMEOVER;
                    }else{
                        currentOne = nextOne;
                        nextOne = Tetromino.randomOne();
                    }
                }
            }
            repaint();
        }
    }

    //创建顺时针旋转
    public void rotateRightAction() {
        currentOne.rotateRight();
        //判断是否越界或重合
        if (outOfBounds() || coincide()) {
            currentOne.rotateLeft();
        }
    }

    //瞬间下落
    public void handDropAction() {
        //判断能否下落
        while (true) {
            if (canDrop()) {
                currentOne.softDrop();
            } else {
                break;
            }
        }
        //嵌入wall
        landToWall();
        //消行
        destroyLine();
        //是否结束
        if (isGameOver()) {
            game_state = GAMEOVER;
        } else {
            //游戏没结束，继续生成新的四方格
            currentOne = nextOne;
            nextOne = Tetromino.randomOne();
        }
    }

    //按键一次四方格下落一格
    public void sortDropAction() {
        //判断是否能够下落
        if (canDrop()) {
            //当前四方格下落一格
            currentOne.softDrop();
        } else {
            //将四方格子嵌入wall
            landToWall();
            //判断是否能消行
            destroyLine();
            //判断游戏是否结束
            if (isGameOver()) {
                game_state = GAMEOVER;
            } else {
                //游戏未结束，继续生成四方格
                currentOne = nextOne;
                nextOne = Tetromino.randomOne();
            }
        }
    }

    private void landToWall() {
        Cell[] cells = currentOne.cells;
        for (Cell cell : cells) {
            int row = cell.getRow();
            int col = cell.getCol();
            wall[row][col] = cell;
        }
    }

    //判断四方格能否下落
    public boolean canDrop() {
        Cell[] cells = currentOne.cells;
        for (Cell cell : cells) {
            int row = cell.getRow();
            int col = cell.getCol();
            //是否到底部
            if (row == wall.length - 1) {
                return false;
            } else if (wall[row + 1][col] != null) {
                return false;
            }
        }
        return true;
    }

    //消行方法
    public void destroyLine() {
        int line = 0;
        Cell[] cells = currentOne.cells;
        for (Cell cell : cells) {
            int row = cell.getRow();
            // 判断是否已满
            if (isFullLine(row)) {
                line++;
                for (int i = row; i > 0; i--) {
                    System.arraycopy(wall[i - 1], 0, wall[i], 0, wall[0].length);
                }
                wall[0] = new Cell[14];
            }
        }
        //分数池获取分数，累加到分数值
        totalScore += scores_pool[line];
        //统计消除总行数
        totalLine += line;

    }

    //判断当前行是否已满
    public boolean isFullLine(int row) {
        Cell[] cells = wall[row];
        for (Cell cell : cells) {
            if (cell == null) {
                return false;
            }
        }
        return true;
    }

    //判断游戏是否结束
    public boolean isGameOver() {
        Cell[] cells = nextOne.cells;
        for (Cell cell : cells) {
            int row = cell.getRow();
            int col = cell.getCol();
            if (wall[row][col] != null) {
                return true;
            }
        }
        return false;
    }

    private void paintState(Graphics g) {
        if (game_state == PLAYING) {
            g.drawString(show_state[game_state], 500, 400);
        } else if (game_state == PAUSE) {
            g.drawString(show_state[game_state], 500, 400);
        } else if (game_state == GAMEOVER) {
            g.drawString(show_state[game_state], 500, 400);
            g.setColor(Color.red);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 50));
            g.drawString("GAMEOVER !", 45, 400);
        }
    }

    private void paintScore(Graphics g) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        g.drawString("SCORES:" + totalScore, 500, 200);
        g.drawString("LINES:" + totalLine, 500, 300);
    }

    private void paintNextOne(Graphics g) {
        Cell[] cells = nextOne.cells;
        for (Cell cell : cells) {
            int x = cell.getCol() * CELL_SIZE + 450;
            int y = cell.getRow() * CELL_SIZE + 50;
            g.drawImage(cell.getImage(), x, y, null);
        }
    }

    private void paintCurrentOne(Graphics g) {
        Cell[] cells = currentOne.cells;
        for (Cell cell : cells) {
            int x = cell.getCol() * CELL_SIZE;
            int y = cell.getRow() * CELL_SIZE;
            g.drawImage(cell.getImage(), x, y, null);
        }
    }

    private void paintWall(Graphics g) {
        for (int i = 0; i < wall.length; i++) {
            for (int j = 0; j < wall[i].length; j++) {
                int x = j * CELL_SIZE;
                int y = i * CELL_SIZE;
                Cell cell = wall[i][j];
                // 判断当前格子是否有方块，没有就画矩形，有就填充图片
                if (cell == null) {
                    g.drawRect(x, y, CELL_SIZE, CELL_SIZE);
                } else {
                    g.drawImage(cell.getImage(), x, y, null);
                }
            }
        }

    }

    public boolean outOfBounds() {
        Cell[] cells = currentOne.cells;
        for (Cell cell : cells) {
            int col = cell.getCol();
            int row = cell.getRow();
            if (row < 0 || row > wall.length - 1
                    || col < 0 || col > wall[1].length - 1) {
                return true;
            }
        }
        return false;
    }

    // 判断方块是否重合
    public boolean coincide() {
        Cell[] cells = currentOne.cells;
        for (Cell cell : cells) {
            int col = cell.getCol();
            int row = cell.getRow();
            if (wall[row][col] != null) {
                return true;
            }
        }
        return false;
    }

    //按键一次四方格左移一格
    public void moveLeftAction() {
        currentOne.moveLeft();
        //判断是否越界或重合
        if (outOfBounds() || coincide()) {
            currentOne.moveRight();
        }
    }

    //按键一次四方格左移一格
    public void moveRightAction() {
        currentOne.moveRight();
        //判断是否越界或重合
        if (outOfBounds() || coincide()) {
            currentOne.moveLeft();
        }
    }

    public static void main(String[] args) {
        //创建窗口对象
        JFrame frame = new JFrame("TETRIS");
        //创建游戏界面
        Tetris panel = new Tetris();
        // 将面板添加到窗口中
        frame.add(panel);
        //设置可见
        frame.setVisible(true);
        //设置窗口尺寸
        frame.setSize(810, 940);
        //设置窗口居中
        frame.setLocationRelativeTo(null);
        //设置窗口关闭时程序终止
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //游戏主要逻辑封装在方法中
        panel.start();
    }
}
