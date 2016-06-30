import java.util.*;

/* Defines an object to hold information extracted from a FileDescriptorSet.DescriptorProto.FieldDescriptorProto. 
 * Stored as three strings. */

public class FieldInfo {

  private String fieldName;
  private String fieldType;
  private String fieldLabel;

  // Constructor
  public FieldInfo(String fieldName, String fieldType, String fieldLabel) {
    this.fieldName = fieldName;
    this.fieldType = fieldType;
    this.fieldLabel = fieldLabel;
  }

  // Methods to obtain field info

  public String getFieldName() {
    return this.fieldName;
  }

  public String getFieldType() {
    return this.fieldType;
  }

  public String getFieldLabel() {
    return this.fieldLabel;
  }

   public String toString() {
      return "\n    fieldName: " + this.fieldName + "\n      fieldType: " + this.fieldType + "\n      fieldLabel: " + this.fieldLabel;
   }

}
