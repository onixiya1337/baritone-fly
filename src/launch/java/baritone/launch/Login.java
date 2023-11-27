package baritone.launch;

import me.djtheredstoner.devauth.common.DevAuth;
import net.minecraft.launchwrapper.Launch;

public class Login {

    public static void main(String[] args) {
        DevAuth devAuth = new DevAuth();
        String[] argArray = devAuth.processArguments(args);

        Launch.main(argArray);
    }
}
