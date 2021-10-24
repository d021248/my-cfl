package d021248.cfl.cmd;

import java.io.IOException;

public class TestCfCommand {
    public static void main(String[] args) throws InterruptedException, IOException {
        // CfCommand.logs("mkv-srv");
        // var list = CfCommand.apps();
        // list.stream().forEach(a -> System.out.println(a.name + " : " + a.urls));

        // System.out.println(list);

        // CfCommand.env("mkv-srv");
        var target = CfCommand.target();
        System.out.println(target.endpoint);
        // Thread.sleep(60_000);
        // Command.stopAll();
    }
}
