import java.util.HashMap;
import java.util.Map;

/**
 * MultiPartFormData is a class that represents a multipart/form-data request.
 */
public class MultiPartFormData {

  private Map<String, String> fields;
  private Map<String, byte[]> files;

  public MultiPartFormData() {
    this.fields = new HashMap<>();
    this.files = new HashMap<>();
  }

  public void addField(String name, String value) {
    fields.put(name, value);
  }
  
  public void addFile(String name, byte[] data) {
    files.put(name, data);
  }

  public String getField(String name) {
    return fields.get(name);
  }

  public byte[] getFile(String name) {
    return files.get(name);
  }
}
