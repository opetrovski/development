/*******************************************************************************
 *  Copyright FUJITSU LIMITED 2017
 *******************************************************************************/

package org.oscm.operatorsvc.client.ant;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.InitialContext;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Parameter;

import org.oscm.operatorsvc.client.OperatorClient;

/**
 * Ant Task implementation to drive the operator client.
 */
public class OperatorClientTask extends Task {

    private String username = null;

    private String password = null;

    private String command = null;

    private String contextFactory = "com.sun.enterprise.naming.SerialInitContextFactory";

    private String contextProviderUrl = "http://localhost:8080";

    private String orbInitialHost = "localhost";

    private String orbInitialPort = "3700";

    private final List<Parameter> parameters = new ArrayList<Parameter>();

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public void setContextFactory(String contextFactory) {
        this.contextFactory = contextFactory;
    }

    public void setContextProviderUrl(String contextProviderUrl) {
        this.contextProviderUrl = contextProviderUrl;
    }

    public void setOrbInitialHost(String orbInitialHost) {
        this.orbInitialHost = orbInitialHost;
    }

    public void setOrbInitialPort(String orbInitialPort) {
        this.orbInitialPort = orbInitialPort;
    }

    public void addParameter(Parameter parameter) {
        this.parameters.add(parameter);
    }

    @Override
    public void execute() throws BuildException {

        // Parameter Check:
        if (username == null) {
            throw new BuildException("username required");
        }
        if (password == null) {
            throw new BuildException("password required");
        }
        if (OperatorClient.getCommand(command) == null) {
            throw new BuildException(String.format(
                    "Unknown command '%s'. Available Commands are %s.",
                    command, OperatorClient.getCommands()));
        }

        final InitialContext initialContext;
        try {
            initialContext = new InitialContext(createContextProperties());
            OperatorClient.login(username, password);
        } catch (Exception e) {
            throw new BuildException(e);
        }

        final CharArrayWriter out = new CharArrayWriter();
        final CharArrayWriter err = new CharArrayWriter();

        final OperatorClient client = new OperatorClient(initialContext,
                new PrintWriter(out, true), new PrintWriter(err, true));
        client.execute(command, createArguments());

        if (out.size() > 0) {
            getProject().log(out.toString(), Project.MSG_INFO);
        }
        if (err.size() > 0) {
            getProject().log(err.toString(), Project.MSG_ERR);
        }

    }

    protected Properties createContextProperties() {
        final Properties properties = new Properties();
        properties.put("java.naming.factory.initial", contextFactory);
        properties.put("java.naming.provider.url", contextProviderUrl);
        properties.put("org.omg.CORBA.ORBInitialHost", orbInitialHost);
        properties.put("org.omg.CORBA.ORBInitialPort", orbInitialPort);
        return properties;
    }

    protected Map<String, String> createArguments() {
        final Map<String, String> args = new HashMap<String, String>();
        for (Parameter p : parameters) {
            args.put(p.getName(), p.getValue());
        }
        return args;
    }

}
