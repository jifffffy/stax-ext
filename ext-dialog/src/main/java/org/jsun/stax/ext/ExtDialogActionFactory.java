package org.jsun.stax.ext;

import com.ibm.staf.service.stax.*;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ExtDialogActionFactory implements STAXActionFactory, STAXJobManagementHandler {

    public static final String EXT_DELAY = "dialog";

    private static Map<String, String> parameterMap = new HashMap();

    private static String dtdInfo =
            "\n" +
                    "<!--================= The Dialog Element ============================ -->\n" +
                    "<!--\n" +
                    "     弹出对话框与用户交互.\n" +
                    "-->\n" +
                    "<!ELEMENT dialog     (button)*>\n" +
                    "<!ATTLIST dialog\n" +
                    "          label         CDATA    #IMPLIED\n" +
                    "          width    CDATA    #IMPLIED\n" +
                    "          height     CDATA    #IMPLIED\n" +
                    "          x         CDATA    #IMPLIED\n" +
                    "          y        CDATA    #IMPLIED\n" +
                    "          text      CDATA    #IMPLIED" +
                    ">" +
                    "<!ELEMENT button     (#PCDATA)>" +
                    "<!ATTLIST function-import\n" +
                    "          name         CDATA    #IMPLIED\n" +
                    ">";

    public ExtDialogActionFactory() {
    }

    public ExtDialogActionFactory(STAX stax) {
        stax.registerJobManagementHandler(this);
    }

    public ExtDialogActionFactory(STAX stax, Map parmMap) throws STAXExtensionInitException {
        stax.registerJobManagementHandler(this);
        Iterator iter = parmMap.keySet().iterator();
        while (iter.hasNext()) {
            // Check if the parameter name is supported
            String key = (String) iter.next();
            if (DialogAttr.getValue(key) == null) {
                throw new STAXExtensionInitException("Unsupported attr name " + key);
            }
        }
        parameterMap.putAll(parmMap);
    }

    public String getParameter(String name) {
        return parameterMap.get(name);
    }

    @Override
    public String getDTDInfo() {
        return dtdInfo;
    }

    @Override
    public String getDTDTaskName() {
        return "dialog";
    }

    @Override
    public STAXAction parseAction(STAX staxService, STAXJob job, Node root) throws STAXException {
        ExtDialogAction dialogAction = new ExtDialogAction();

        dialogAction.setActionFactory(this);
        dialogAction.setLineNumber(root);
        dialogAction.setXmlFile(job.getXmlFile());
        dialogAction.setXmlMachine(job.getXmlMachine());

        NamedNodeMap attrs = root.getAttributes();

        for (int i = 0; i < attrs.getLength(); ++i) {
            Node thisAttr = attrs.item(i);

            String attrName = thisAttr.getNodeName();
            dialogAction.setElementInfo(new STAXElementInfo(
                    root.getNodeName(), attrName));

            if (thisAttr.getNodeName().equals("title")) {
                dialogAction.setTitle(STAXUtil.parseAndCompileForPython(thisAttr.getNodeValue(), dialogAction));
            } else if (thisAttr.getNodeName().equals("label")) {
                dialogAction.setLabel(STAXUtil.parseAndCompileForPython(thisAttr.getNodeValue(), dialogAction));
            } else if (thisAttr.getNodeName().equals("width")) {
                dialogAction.setWidth(STAXUtil.parseAndCompileForPython(thisAttr.getNodeValue(), dialogAction));
            } else if (thisAttr.getNodeName().equals("height")) {
                dialogAction.setHeight(STAXUtil.parseAndCompileForPython(thisAttr.getNodeValue(), dialogAction));
            } else if (thisAttr.getNodeName().equals("x")) {
                dialogAction.setX(STAXUtil.parseAndCompileForPython(thisAttr.getNodeValue(), dialogAction));
            } else if (thisAttr.getNodeName().equals("y")) {
                dialogAction.setY(STAXUtil.parseAndCompileForPython(thisAttr.getNodeValue(), dialogAction));
            } else if (thisAttr.getNodeName().equals("text")) {
                dialogAction.setText(STAXUtil.parseAndCompileForPython(thisAttr.getNodeValue(), dialogAction));
            }
        }

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            Node thisChild = children.item(i);

            if (thisChild.getNodeType() == Node.COMMENT_NODE) {
                /* Do nothing */
            } else if (thisChild.getNodeType() == Node.ELEMENT_NODE) {
                dialogAction.setLineNumber(thisChild);

                if (thisChild.getNodeName().equals("button")) {
                    handleButton(thisChild, dialogAction);
                }
            }
        }

        return dialogAction;
    }

    private void handleButton(Node root, ExtDialogAction action) throws STAXException {
        NodeList children = root.getChildNodes();

        for (int i = 0; i < children.getLength(); ++i) {
            Node thisChild = children.item(i);
            if (thisChild.getNodeType() == Node.COMMENT_NODE) {
                /* Do nothing */
            } else if (thisChild.getNodeType() == Node.TEXT_NODE) {
                action.setElementInfo(new STAXElementInfo(root.getNodeName()));
                action.addButton(STAXUtil.parseAndCompileForPython(thisChild.getNodeValue(), action));
            } else if (thisChild.getNodeType() == Node.CDATA_SECTION_NODE) {
                /* Do nothing */
            } else {
                action.setElementInfo(new STAXElementInfo(
                        root.getNodeName(), STAXElementInfo.NO_ATTRIBUTE_NAME,
                        STAXElementInfo.LAST_ELEMENT_INDEX,
                        "Contains invalid node type: " +
                                Integer.toString(thisChild.getNodeType())));

                throw new STAXInvalidXMLNodeTypeException(STAXUtil.formatErrorMessage(action), action);
            }
        }
    }

    @Override
    public void initJob(STAXJob job) {

    }

    @Override
    public void terminateJob(STAXJob job) {
        /* Do Nothing */
    }

    public enum DialogAttr {
        TITLE,
        LABEL,
        WIDTH,
        HEIGHT,
        X,
        Y,
        TEXT;

        public static String getValue(String name) {
            for (DialogAttr attr : values()) {
                if (attr.name().equalsIgnoreCase(name)) {
                    return attr.name();
                }
            }
            return null;
        }
    }
}
