package util;


import parser.ConfigParser;

import java.io.File;
import java.util.HashMap;

/**
 * Created by joh-mue on 29/02/16.
 */
public class Config {
    private static Config instance;
    private HashMap<String, String> itemHash;

    public static final String HADOOP_CONF_DIR = "hadoop-conf-dir";
    public static final String FLINK_HOME = "flink-home-dir";
    public static final String SPARK_HOME = "spark-home-dir";
    public static final String LOG_DIR = "log-dir";
    public static final String OVERWRITE_LOGS = "overwriteLogs";

    public static final String NOTIFY_FREAMON = "notifyFreamon";
    public static final String AKKA_HOST = "akkaHost";
    public static final String AKKA_PORT = "akkaPort";
    public static final String FREAMON_MASTER_HOST = "freamonMasterHost";
    public static final String FREAMON_MASTER_PORT = "freamonMasterPort";
    public static final String FREAMON_MASTER_SYSTEM = "freamonMasterSystemName";
    public static final String FREAMON_MASTER_ACTOR = "freamonMasterActorName";

    public static final String DSTAT = "run-dstat";
    public static final String DSTAT_CMD = "dstat-cmd";
    public static final String SLAVES = "slaves";

    // TODO: not a proper singleton initialization
    public static void initializeConfig(File configFile) {
        if (instance == null) {
            instance = new Config(configFile);
        }
    }

    public static Config getInstance() {
        // i threw up
        try {
            if (instance == null) {
                throw new Exception("The Config was not initialized yet.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return instance;
    }

    private Config(File configFile) {
        itemHash = ConfigParser.parseConfig(configFile);
    }

    /**
     * Returns the value for a certain configuration item. Use the public constants of this class to retrieve the values.
     * <p/>
     * Throws an NoSuchElementException if key does not match an item in the configuration file
     *
     * @param key
     * @return
     */
    public String getConfigItem(String key) {
        String configItem = itemHash.get(key);
        if (configItem == null) {
            System.out.println("The item " + key + " was not set in your configuration file.");
            System.exit(0);
        }
        return configItem;
    }

    /**
     * Returns the boolean value for a certain configuration item. Use the public constants of this class to
     * retrieve the values.
     *
     * @param key
     * @return
     */
    private boolean getBooleanConfigItem(String key) {
        return getConfigItem(key).equalsIgnoreCase("true");
    }

    /**
     * Get the logging directory for an experiment by it's experiment name as configured in the config.xml
     *
     * @param experimentName
     * @return
     */
    public static File getLogDir(String experimentName) {
        // TODO: [009b]should not call getInstance but be static instead
        String baseLogDir = Config.getInstance().getConfigItem(LOG_DIR);
        if (baseLogDir.isEmpty()) {
            System.out.println("You did not set " + LOG_DIR + " in your configuration file.");
            System.exit(0);
        }
        return new File(baseLogDir + '/' + experimentName);
    }

    public String getHadoopConfDir() {
        return getConfigItem(HADOOP_CONF_DIR);
    }

    /**
     * If flink-home-dir is not set the program terminates.
     *
     * @return
     */
    public String getFlinkHome() {
        String flinkHome = getConfigItem(FLINK_HOME);
        if (flinkHome.isEmpty()) {
            System.out.println("You did not set " + FLINK_HOME + " in your configuration file.");
            System.exit(0);
        }
        return flinkHome;
    }

    /**
     * If spark-home-dir is not set the program terminates.
     *
     * @return
     */
    public String getSparkHome() {
        String sparkHome = getConfigItem(SPARK_HOME);
        if (sparkHome.isEmpty()) {
            System.out.println("You did not set " + SPARK_HOME + " in your configuration file.");
            System.exit(0);
        }
        return sparkHome;
    }

    /**
     * Returns true if logs should be overwritten if they already exist for any given job
     *
     * @return boolean indicating if logs should be overwriten
     */
    public boolean overwriteLogs() {
        return getBooleanConfigItem(OVERWRITE_LOGS);
    }

    /**
     * Returns true if freamon functionality should be executed. If notifyFreamon is not set, the default value 'false'
     * is returned.
     *
     * @return
     */
    public boolean notifyFreamon() {
        String configItem = itemHash.get(NOTIFY_FREAMON);
        if (configItem == null) {
            return false;
        } else {
            return configItem.equalsIgnoreCase("true");
        }
    }

    /**
     * Returns true if dstat should be executed. If dstat is not set, the default value 'false'
     * is returned.
     *
     * @return
     */
    public boolean runDstat() {
        String configItem = itemHash.get(DSTAT);
        if (configItem == null) {
            return false;
        } else {
            return configItem.equalsIgnoreCase("true");
        }
    }

    public String getDstatCmd() {
        String dstatCmd = getConfigItem(DSTAT_CMD);
        if (dstatCmd.isEmpty() || !runDstat()) {
            System.out.println("You did not set " + DSTAT_CMD + "or " + DSTAT + " in your configuration file.");
            System.exit(0);
        }
        return dstatCmd;
    }

    /**
     * Returns an array with all slaves nodes.
     *
     * @return Array with all slave hostnames
     */
    public String[] getSlaves() {
        String configItem = itemHash.get(SLAVES);
        if (configItem == null) {
            return null;
        } else {
            return configItem.split(" ");
        }
    }
}
