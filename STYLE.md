Spectra Java Style Guide
========================

When contributing to open source spectra java projects we request that the code conforms to the following style guide.  This speeds up code reviews, and make sure that we have a consistent style across all our open source software projects, regardless of who is contributing.

## Indentation
Code indentation must use `4` spaces.  If indenting twice, use `8` spaces.

Example:

```java
public void func() {
    if (true) {
        System.out.println("message");
    }
}
```

## Newline
The `\n` character should be used for all newlines.

## Use of `final` Keyword
All variables should use the `final` keyword if it is not being reassigned.

Example:
```java
public void func() {
    // var is not reasigned, and is therefore final
    final Object var = new Object();
    
    // var2 is reassigned, and is therefore not final
    Object var2 = new Object();
    var2 = new Object();
}
```

Method parameters should also have the `final` keyword prepended to them.
Example:
```java
public void func(final Object parameter1, final Object parameter2) {
    System.out.println("Message");
}
```

Another place where final should be used is in exception handling.

Example:
```java
try {
    aMethodCallThatThrowsAnException();
} catch(final Exception e) {
    LOG.error("Some message about the exception", e);
    handleExceptionSomehow();
}
```

## `{` Placement
The `{` should be placed at the end of the line with one space to the left, followed by a newline.

Example:
```java
public void func() {
    if (true) {
        System.out.println("Message");
    }
}
```

## Conditional Statements
Following a conditional statement i.e. (`if`, `while`, `for`) there must be one space followed by the conditional statement.

Example:
```java
public void func() {
    if (true) {
        System.out.println("Message")
        for (int i = 0; i < 10; i++) {
            System.out.println("Print " + i);
        }
    }
}
```

## Constants
Constant variables should be declared to use the `final` and `static` keywords, and it should be declared in all caps.  If the variable would be camel cased use `_` instead.

Example:
```java
public class Example {
    private static final Logger LOG = LoggerFactory.getLogger(Example.class);
    private static final String CONSTANT_VARIABLE = "constant variable";
    
    public void method() {
        LOG.info("Log something");
    }
}
```

## `final` Classes
If a class has all static methods, like a utils class, it should be declared `final`

Example:
```java
public final class Util {
    public static void doSomething() {
        System.out.println("static util method, in a final class");
    }
}
```
