# Structured Logging

Per Thoughtworks (https://www.thoughtworks.com/radar/techniques/structured-logging) we should log in a structured manner

...

### With Structured Logging And Preprocessor

```java
@LoggerSchema({Id.class, Qty.class, Event.class})  // note: the library preprocessor generates the schema
public class MyClass {
    private static final StructuredLogger<MyClassSchema> log = StructuredLoggerFactory.structured(MyClassSchema.class);  // note: the library auto-generates the schema instance class

    private void myOperation(String id, String eventName, int qty) {
    ...
    
        log.info("Something happened", schema -> schema.event(eventName).id(id).qty(qty));
    }
}
```

Logs similar to: `id=7892323 event=EventName qty=100 Something happened`

### With Just Structured Logging

```java

public interface LogSchema {
    LogSchema id(String id);
    
    LogSchema qty(int qty);
    
    LogSchema event(String name);
}

...

StructuredLogger<LogSchema> log = StructuredLoggerFactory.structured(LogSchema.class);  // note: the library auto-generates the schema instance class

...

private void myOperation(String id, String eventName, int qty) {
    ...
    
    log.info("Something happened", schema -> schema.event(eventName).id(id).qty(qty));
}
```

Logs similar to: `id=7892323 event=EventName qty=100 Something happened`

### Without Structured Logging

```java
Logger log = LoggerFactory.getLogger(...);

...

private void myOperation(String id, String eventName, int qty) {
    ...
    
    log.info("Something happened where id={} eventname={}and qty = {}", id, qty, eventName);    // note mistakes misspellings
}
```
