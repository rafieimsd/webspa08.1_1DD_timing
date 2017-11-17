package net.seleucus.wsp.checker;

/**
 *
 * @author masoud
 */
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.seleucus.wsp.client.WSConnection;
import net.seleucus.wsp.client.WSRequestBuilder;

import net.seleucus.wsp.config.WSConfiguration;
import net.seleucus.wsp.crypto.WebSpaEncoder;
import net.seleucus.wsp.db.WSDatabase;
import net.seleucus.wsp.main.WSVersion;
import net.seleucus.wsp.util.WSUtil;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WSCheckerListener extends TailerListenerAdapter {

    final static Logger LOGGER = LoggerFactory.getLogger(WSCheckerListener.class);

    private WSChecker myChecker;
    private WSDatabase myDatabase;
    private WSConfiguration myConfiguration;

    public WSCheckerListener(WSChecker myChecker) {

        this.myChecker = myChecker;
        this.myDatabase = myChecker.getWSDatabase();
        this.myConfiguration = myChecker.getWSConfiguration();

    }

    @Override
    public void handle(final String requestLine) {

        // Check if the line length is more than 65535 chars
        if (requestLine.length() > Character.MAX_VALUE) {
            return;
        }

        // Check if the regex pattern has been found
//        LOGGER.info("--- checker---requestLine: " + requestLine);
        Pattern wsPattern = Pattern.compile(myConfiguration.getLoginRegexForEachRequest());
        Matcher wsMatcher = wsPattern.matcher(requestLine);

        if (!wsMatcher.matches()
                || 2 != wsMatcher.groupCount()) {
            LOGGER.info("Regex Problem?");
            LOGGER.info("Request line is {}.", requestLine);
            LOGGER.info("The regex is {}.", myConfiguration.getLoginRegexForEachRequest());
            return;
        }

        final String ipAddress = wsMatcher.group(1);
        String webSpaRequest = wsMatcher.group(2);
        if (webSpaRequest.endsWith("/")) {
            webSpaRequest = webSpaRequest.substring(0, webSpaRequest.length() - 1);
        }

        // Nest the world away!
        LOGGER.info("--- hecker: The chars received are {}.", webSpaRequest);
        String[] requestInfo = processRequest(webSpaRequest);

        sendResponseToServer(requestInfo);

    }

    public boolean sendResponseToServer(String[] requestInfo) {

        String host = "http://192.168.1.67";                    //"http://10.20.205.248";//readLineRequired("Host [e.g. https://localhost/]");
        // todo read from file
        boolean isValidUser = (requestInfo[0].equals("11") && requestInfo[1].equals("11"));
        String newKnock = host + "/usid=" + requestInfo[0] + "?ppid=" + requestInfo[1] + "?isvalid=" + isValidUser + "/";
        WSConnection myConnection = new WSConnection(newKnock);
        LOGGER.info(myConnection.getActionToBeTaken());
        myConnection.sendRequest();

        // is the connection HTTPS
        if (myConnection.isHttps()) {
            try {

                LOGGER.info(myConnection.getCertSHA1Hash());

            } catch (NullPointerException npEx) {

                LOGGER.info("Couldn't get the SHA1 hash of the server certificate - probably a self signed certificate.");

                if (!WSUtil.hasMinJreRequirements(1, 7)) {
                    LOGGER.error("Be sure to run WebSpa with a JRE 1.7 or greater.");
                } else {
                    LOGGER.error("An exception was raised when reading the server certificate.");
                    npEx.printStackTrace();
                }
            }

//                final String trustChoice = readLineOptional("Continue connecting [Y/n]");
//
//                if (WSUtil.isAnswerPositive(trustChoice) || sendChoice.isEmpty()) {
            myConnection.sendRequest();
            LOGGER.info(myConnection.responseMessage());
            LOGGER.info("HTTPS Response Code: {}", myConnection.responseCode());

        } else {

            myConnection.sendRequest();
            LOGGER.info("--- response message: " + myConnection.responseMessage());
            LOGGER.info("--- HTTP Response Code: {}", myConnection.responseCode());

        }

        return false;
    }

    @Override
    public void handle(Exception arg0) {

    }

    @Override
    public void init(Tailer arg0) {
        // TODO Auto-generated method stub
    }

    private String[] processRequest(String webSpaRequest) {

        String result[] = new String[2];
        result[0] = webSpaRequest.substring(5, webSpaRequest.indexOf("?"));
        result[1] = webSpaRequest.substring(5 + result[0].length() + 1 + 5);
//        System.out.println("---server--- usid=" + result[0] + " ?ppid=" + result[1]);
        return result;

    }

}