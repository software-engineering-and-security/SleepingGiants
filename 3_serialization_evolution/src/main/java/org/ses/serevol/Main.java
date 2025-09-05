package org.ses.serevol;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.ses.serevol.analysis.SerializableEvolution;
import java.io.File;
import java.io.IOException;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class.getName());

    public static void main(String[] args) throws IOException {

        if(args.length < 1) {
            logger.error("Please specify an input download file");
            System.exit(1);
        }

        boolean noSerializableFoundError = false;

        if (args.length == 1)
            noSerializableFoundError = SerializableEvolution.getEvolution(new File(args[0]), null);
        if (args.length == 2)
            noSerializableFoundError = SerializableEvolution.getEvolution(new File(args[0]), new File(args[1]));

        if (noSerializableFoundError) System.exit(1);
        System.exit(0);




    }


}
