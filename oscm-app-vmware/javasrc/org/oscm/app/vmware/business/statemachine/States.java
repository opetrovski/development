package org.oscm.app.vmware.business.statemachine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.oscm.app.v1_0.data.InstanceStatus;
import org.oscm.app.v1_0.data.ProvisioningSettings;
import org.oscm.app.vmware.business.statemachine.api.StateMachineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlRootElement(name = "states")
public class States {

    private static final Logger logger = LoggerFactory.getLogger(States.class);

    private List<State> states;
    private String clazz;

    @XmlAttribute
    public void setClass(String clazz) {
        this.clazz = clazz;
    }

    @XmlElement(name = "state")
    public List<State> getStates() {
        return states;
    }

    public void setStates(List<State> aStates) {
        this.states = aStates;
    }

    public String invokeAction(State state, String instanceId,
            ProvisioningSettings settings, InstanceStatus status)
                    throws StateMachineException {

        logger.info("Invoking action: " + state.getAction() + " for instance "
                + instanceId);

        try {
            Class<?> c = Class.forName(clazz);
            Object o = c.newInstance();

            Class<?>[] paramTypes = new Class[3];
            paramTypes[0] = String.class;
            paramTypes[1] = ProvisioningSettings.class;
            paramTypes[2] = InstanceStatus.class;

            String methodName = state.getAction();
            Method m;
            try {
                m = c.getMethod(methodName, paramTypes);
            } catch (@SuppressWarnings("unused") NoSuchMethodException e) {
                m = c.getSuperclass().getMethod(methodName, paramTypes);
            }
            return (String) m.invoke(o, instanceId, settings, status);
        } catch (ClassNotFoundException | InstantiationException
                | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException
                | SecurityException e) {
            throw new StateMachineException(
                    "Failed to call method " + state.getAction(), e);
        }
    }
}