package org.jbpm.process.test;

import java.lang.reflect.Constructor;

import org.drools.core.process.core.Work;
import org.drools.core.process.core.impl.WorkImpl;
import org.jbpm.workflow.core.Node;
import org.jbpm.workflow.core.NodeContainer;
import org.jbpm.workflow.core.impl.ConnectionImpl;
import org.jbpm.workflow.core.impl.NodeImpl;
import org.jbpm.workflow.core.node.WorkItemNode;

public class NodeCreator<T extends NodeImpl> {
    NodeContainer nodeContainer;
    Constructor<T> constructor;

    private static long idGen = 1;

    public NodeCreator(NodeContainer nodeContainer, Class<T> clazz) {
        this.nodeContainer = nodeContainer;
        this.constructor = (Constructor<T>) clazz.getConstructors()[0];
    }

    public T createNode(String name) throws Exception {
        T result = this.constructor.newInstance(new Object[0]);
        result.setId(idGen++);
        result.setName(name);
        this.nodeContainer.addNode(result);
        
        if( result instanceof WorkItemNode ) { 
            Work work = new WorkImpl();
            ((WorkItemNode) result).setWork(work);
        }
        return result;
    }

    public void setNodeContainer(NodeContainer newNodeContainer) {
        this.nodeContainer = newNodeContainer;
    }

    public static void connect(Node nodeOne, Node nodeTwo ) { 
        new ConnectionImpl(
                nodeOne, Node.CONNECTION_DEFAULT_TYPE, 
                nodeTwo, Node.CONNECTION_DEFAULT_TYPE
        );
    }
}