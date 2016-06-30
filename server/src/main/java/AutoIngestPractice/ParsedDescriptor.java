import com.google.protobuf.DescriptorProtos.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/* Defines an object to contain .proto schema Message information as Node information.
 * Also parses the relevant FileDescriptorSet containing the schema-to-be-parsed-and-described. */

public class ParsedDescriptor {

    private HashMap<String, MessageAsNode> messagesAsNodes;

    public ParsedDescriptor(String fileDescriptorSetPath) throws Exception {
	this.messagesAsNodes = new HashMap<String, MessageAsNode>();
	parseDescriptor(fileDescriptorSetPath);
    }

    // Prints out descriptor info and creates list of messages as nodes
    private void parseDescriptor(String fileDescriptorSetPath) throws Exception {
	// Read in the FileDescriptorSet.
	FileDescriptorSet fileDescriptorSet =
	    FileDescriptorSet.parseFrom(new FileInputStream(fileDescriptorSetPath));
	this.processFileDescriptorSet(fileDescriptorSet);
    }

    private void processFileDescriptorSet(FileDescriptorSet fileDescriptorSet) {
	    
	for (FileDescriptorProto fileDescriptorProto: fileDescriptorSet.getFileList()) {
	    // Process each FileDescriptorProto
	    System.out.println("File name: " + fileDescriptorProto.getName());
	    System.out.println("Package name: " + fileDescriptorProto.getPackage());
      
	    System.out.println("");

	    for(DescriptorProto descriptorProto: fileDescriptorProto.getMessageTypeList()) {
		// Process each message
		processMessage(descriptorProto, 0);
	    }
	}	
    } // end processFileDescriptorSet
  

    // Process message
    private void processMessage(DescriptorProto descriptorProto, int nestLevel) {
	//	String indent = new String(new char[nestLevel]).replace("\0", "  "); // for printing purposes only
	String messageName = descriptorProto.getName();
	MessageAsNode messageAsNode = new MessageAsNode(messageName);
	System.out.println("  Message name: " + messageName);

	for (FieldDescriptorProto fieldDescriptorProto: descriptorProto.getFieldList()) {
	    String fieldName = fieldDescriptorProto.getName();
	    String fieldType = fieldDescriptorProto.getType().toString();
	    String fieldLabel = fieldDescriptorProto.getLabel().toString();

	    System.out.println("    Field name: " + fieldName);
	    System.out.println("      Field type: " + fieldType);
	    System.out.println("      Field Label: " + fieldLabel);

	    if (fieldType == "TYPE_MESSAGE") {
		System.out.println("      Field type name: " + fieldDescriptorProto.getTypeName()); // likely a map. We will process the message later.
	    } else if (fieldLabel == "LABEL_REPEATED") {
		if (fieldName.endsWith("Edges")) {
		    messageAsNode.addEdge(fieldName); // process as edge
		} else {
		    messageAsNode.addRepeatedProperty(new FieldInfo(fieldName, fieldType, fieldLabel)); // process as repeatedProperty
		}
	    } else if (fieldLabel == "LABEL_OPTIONAL") {
		    messageAsNode.addSingularProperty(new FieldInfo(fieldName, fieldType, fieldLabel)); // process as singularProperty
	    } else {
		System.out.println("Warning: unrecognized Field Label " + fieldLabel);
	    }
	}
      
	// Any messages in the descriptor should technically represent maps, since we aren't supporting nested messages in the schema.
	for(DescriptorProto nestedDescriptorProto: descriptorProto.getNestedTypeList()) {
	    String nestedMessageName = nestedDescriptorProto.getName();
	    System.out.println("    Message name: " + messageName);    
	    assert nestedMessageName.endsWith("Entry"); // nested map info should have name that ends with "Entry"
	    String keyFieldType = "";
	    String valueFieldType = "";
	    for (FieldDescriptorProto fieldDescriptorProto: nestedDescriptorProto.getFieldList()) {
		String fieldName = fieldDescriptorProto.getName().toString();
		String fieldType = fieldDescriptorProto.getType().toString();
		System.out.println("      Field name: " + fieldName);
		System.out.println("        Field type: " + fieldType);
		if (fieldName.equals("key")) {
		    keyFieldType = fieldType;
		}
		else if (fieldName.equals("value")) {
		    valueFieldType = fieldType;
		} else {
		    System.out.println("      Unrecognized Nested Message Field Name " + fieldName);
		}
	    messageAsNode.addMapProperty(new MapInfo(nestedMessageName, keyFieldType, valueFieldType));
	    }

		
	} // end for loop
	// Now add the message to messagesAsNodes
	this.messagesAsNodes.put(messageName, messageAsNode);
    } // end processMessage

    // Get map of MessageAsNode
    public HashMap<String, MessageAsNode> getMessages() {
	return this.messagesAsNodes;
    }
}
