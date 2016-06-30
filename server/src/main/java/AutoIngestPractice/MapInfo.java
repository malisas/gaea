import java.util.*;

/* Defines an object to hold information extracted from a FileDescriptorSet.DescriptorProto.FieldDescriptorProto. 
 * Stored as three strings. */

public class MapInfo {

   private String fieldName;
   private String keyFieldType;
   private String valueFieldType;

   // Constructor
   public MapInfo(String fieldName, String keyFieldType, String valueFieldType) {
     this.fieldName = fieldName;
     this.keyFieldType = keyFieldType;
     this.valueFieldType = valueFieldType;
   }

   // Methods to obtain field info

   public String getFieldName() {
     return this.fieldName;
   }

   public String getKeyFieldType() {
     return this.keyFieldType;
   }

   public String getValueFieldType() {
     return this.valueFieldType;
   }

   public String toString() {
      return "\n    fieldName: " + this.fieldName + "\n      keyFieldType: " + this.keyFieldType + "\n      valueFieldType: " + this.valueFieldType;
   }
}
