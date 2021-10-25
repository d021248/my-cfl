package d021248.cfl.cmd;

import java.io.IOException;

public class TestCfCommand {
    public static void main(String[] args) throws InterruptedException, IOException {
        // CfCommand.logs("mkv-srv");
        // var list = CfCommand.apps();
        // list.stream().forEach(a -> System.out.println(a.name + " : " + a.urls));

        // System.out.println(list);

        // CfCommand.env("mkv-srv");

        Cf.logs();

        var env = Cf.env("mkv-srv");
        System.out.println(env);

        Thread.sleep(20_000);
        // Command.stopAll();

        var target = Cf.target();
        System.out.println(target.endpoint);

        Command.stopAll();
    }
}
