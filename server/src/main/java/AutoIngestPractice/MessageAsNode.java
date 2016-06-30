import java.util.*;

/* Defines an object to hold information extracted from a FileDescriptorSet.DescriptorProto. 
 * Stored as information needed to construct Titan graph database "nodes", "properties", and outgoing and "edges" */

public class MessageAsNode {

   private String nodeLabel;
   private List<FieldInfo> singularProperties; // List of: [field_name, field_type, field_label]
   private List<String> edges; // List of: field_name. Edges are repeated fields.
   private List<MapInfo> mapProperties; // List of: [field_name, key_field_type, value_field_type]
   private List<FieldInfo> repeatedProperties; // not sure if these will ever exist, or how to handle these. List of: [field_name, field_type, field_label]

   // Constructor. Takes name of node.
   public MessageAsNode(String myNodeLabel) {
     this.nodeLabel = myNodeLabel;
     this.singularProperties = new ArrayList<FieldInfo>();
     this.edges = new ArrayList<String>();
     this.mapProperties = new ArrayList<MapInfo>();
     this.repeatedProperties = new ArrayList<FieldInfo>();
   }

   // Methods to add node properties

   public void addSingularProperty( FieldInfo property) {
       this.singularProperties.add(property);
   }

   public void addEdge( String myEdge) {
       this.edges.add(myEdge);
   }

   public void addMapProperty( MapInfo mapProperty) {
       this.mapProperties.add(mapProperty);
   }

   public void addRepeatedProperty( FieldInfo repeatedProperty) {
       this.repeatedProperties.add(repeatedProperty);
   }

   // Methods to obtain node properties

   public List<FieldInfo> getSingularProperties() {
     return this.singularProperties;
   }

   public List<String> getEdges() {
     return this.edges;
   }

   public List<MapInfo> getMapProperties() {
     return this.mapProperties;
   }

   public List<FieldInfo> getRepeatedProperties() {
     return this.repeatedProperties;
   }

   public String toString() {
       String singularPropertiesString = "";
       for (FieldInfo singularProperty: singularProperties) {
	   singularPropertiesString = singularPropertiesString + singularProperty.toString();
       }
       String repeatedPropertiesString = "";
       for (FieldInfo repeatedProperty: repeatedProperties) {
	   repeatedPropertiesString = repeatedPropertiesString + repeatedProperty.toString();
       }
       String mapPropertiesString = "";
       for (MapInfo mapProperty: mapProperties) {
	   mapPropertiesString = mapPropertiesString + mapProperty.toString();
       }
       String edgesString = "";
       for (String edge: edges) {
	   edgesString = edgesString + "\n    fieldName: " + edge;
       }


       return "\nnodeLabel: " + this.nodeLabel + "\n  singularProperties: " + singularPropertiesString + "\n  edges: " + edgesString + "\n  mapProperties: " + mapPropertiesString + "\n  repeatedProperties: " + repeatedPropertiesString;
   }
}
