package d021248.cfl.cmd;

import java.io.IOException;

public class TestCfCommand {
    public static void main(String[] args) throws InterruptedException, IOException {
        // CfCommand.logs("mkv-srv");
        var list = CfCommand.apps();
        System.out.println(list);
        // Thread.sleep(60_000);
        // Command.stopAll();
    }
}
