package demo1;

public class S extends Tetromino {
    public S() {
        cells[0] = new Cell(0,4,Tetris.S);
        cells[1] = new Cell(0,3,Tetris.S);
        cells[2] = new Cell(1,3,Tetris.S);
        cells[3] = new Cell(1,4,Tetris.S);

        // 2种旋转状态
        states = new State[4];
        //初始化2种旋转状态的坐标
        states[0] = new State(
                0, 0,
                0, 1,
                1, -1,
                1, 0);
        states[1] = new State(
                0, 0,
                1, 0,
                -1, -1,
                0, -1);
    }
}
