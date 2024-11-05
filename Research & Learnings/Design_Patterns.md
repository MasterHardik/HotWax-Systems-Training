# Common Design Patterns

Design patterns are general reusable solutions to commonly occurring problems in software design. They can be classified into **Creational**, **Structural**, and **Behavioral** patterns. Below are some of the most common design patterns in software engineering.


## **Creational Design Patterns**

These patterns deal with object creation mechanisms, trying to create objects in a way that is suitable for the situation.

1. **Singleton Pattern**
   - Ensures that a class has only one instance and provides a global point of access to that instance.
   - **Use case**: When you need to control access to shared resources, like a database or a configuration file.
   - **Example**: Logger class, Database connection pool.

2. **Factory Method Pattern**
   - Defines an interface for creating objects, but lets subclasses alter the type of objects that will be created.
   - **Use case**: When you have a superclass and want to allow different subclasses to create different objects.
   - **Example**: A document creation class that can produce different types of documents (Word, PDF).

3. **Abstract Factory Pattern**
   - Provides an interface for creating families of related or dependent objects without specifying their concrete classes.
   - **Use case**: When you need to create multiple objects, each of which belongs to a certain family (like GUI elements on different platforms).
   - **Example**: GUI framework with multiple themes (Windows, MacOS, Linux).

4. **Builder Pattern**
   - Separates the construction of a complex object from its representation, allowing the same construction process to create different representations.
   - **Use case**: When you need to create complex objects with multiple parts or options.
   - **Example**: A meal builder (starter, main, dessert) for a restaurant menu.

5. **Prototype Pattern**
   - Specifies the kinds of objects to create using a prototype instance and creates new objects by copying this prototype.
   - **Use case**: When object creation is expensive or complex, and you want to clone an existing object.
   - **Example**: Cloning an existing configuration to create a new one with slight modifications.

---

## **Structural Design Patterns**

These patterns focus on how to compose classes or objects to form larger structures while keeping them flexible and efficient.

1. **Adapter Pattern**
   - Allows incompatible interfaces to work together. It "adapts" one interface to another, making them compatible.
   - **Use case**: When you have a system that expects one interface but you need to integrate an incompatible class or interface.
   - **Example**: An adapter for a legacy class to work with a modern system.

2. **Bridge Pattern**
   - Decouples an abstraction from its implementation, allowing them to vary independently.
   - **Use case**: When you need to separate abstraction and implementation so that both can evolve independently.
   - **Example**: Separating the interface of a drawing shape (circle, rectangle) from the implementation (drawing in a window, console).

3. **Composite Pattern**
   - Composes objects into tree-like structures to represent part-whole hierarchies.
   - **Use case**: When you want to treat individual objects and compositions of objects uniformly.
   - **Example**: Filesystem where files and directories are both objects but directories contain multiple files (composite objects).

4. **Decorator Pattern**
   - Adds behavior to an object dynamically without affecting the behavior of other objects from the same class.
   - **Use case**: When you want to extend the functionality of an object at runtime without modifying its structure.
   - **Example**: Adding responsibilities (like scrollbars or borders) to a graphical window object.

5. **Facade Pattern**
   - Provides a simplified interface to a complex subsystem or a set of interfaces.
   - **Use case**: When you want to simplify the interaction with a complex system, providing a higher-level interface.
   - **Example**: A simplified interface for managing a complex home theater system (TV, speakers, DVD player).

6. **Flyweight Pattern**
   - Reduces memory usage by sharing common parts of an object. Instead of storing duplicate data, you store shared data once.
   - **Use case**: When you need to manage large numbers of objects that have some shared state.
   - **Example**: In a text editor, characters like "a", "b", "c" are shared across multiple places in the document to save space.

7. **Proxy Pattern**
   - Provides a surrogate or placeholder for another object. A proxy can control access to the original object.
   - **Use case**: When you want to control access to an object, such as lazy loading or access control.
   - **Example**: A virtual proxy to load large images only when they are needed.

---

## **Behavioral Design Patterns**

These patterns deal with object interactions and responsibilities. They help define the communication between objects in a way that makes the system more flexible and efficient.

1. **Chain of Responsibility Pattern**
   - Passes a request along a chain of handlers. Each handler either processes the request or passes it to the next handler in the chain.
   - **Use case**: When multiple objects can handle a request, but the handler is not known a priori.
   - **Example**: Event handling systems or logging systems with multiple levels (info, debug, error).

2. **Command Pattern**
   - Encapsulates a request as an object, thereby allowing users to parameterize clients with different requests, queue or log requests, and support undoable operations.
   - **Use case**: When you want to decouple the sender of a request from the object that processes it.
   - **Example**: A remote control for a TV where pressing a button (the command) sends a request to turn on or off the TV.

3. **Interpreter Pattern**
   - Defines a grammar for interpreting sentences in a language. It allows you to interpret and evaluate language expressions.
   - **Use case**: When you have a small language or set of expressions that need to be interpreted.
   - **Example**: SQL query parsing or mathematical expression evaluation.

4. **Iterator Pattern**
   - Provides a way to sequentially access elements of a collection without exposing its underlying representation.
   - **Use case**: When you need to traverse a collection but don’t want to expose its internal structure.
   - **Example**: Iterating over elements in a list, tree, or map.

5. **Mediator Pattern**
   - Defines an object that controls the communication between a set of objects, preventing direct communication between them.
   - **Use case**: When you have many objects interacting with each other, and you want to centralize control to reduce complexity.
   - **Example**: A chat application where the server mediates all communication between users.

6. **Memento Pattern**
   - Captures the internal state of an object without exposing it, allowing the object to be restored to that state later.
   - **Use case**: When you want to store and restore the state of an object without exposing its internals.
   - **Example**: Undo/redo operations in text editors or game save points.

7. **Observer Pattern**
   - Defines a one-to-many dependency between objects, so that when one object changes state, all dependent objects are notified and updated automatically.
   - **Use case**: When multiple objects need to be notified about changes to another object, such as updating UI components.
   - **Example**: Event handling systems, like the observer pattern in GUI frameworks (e.g., buttons or form controls).

8. **State Pattern**
   - Allows an object to alter its behavior when its internal state changes. The object will appear to change its class.
   - **Use case**: When an object needs to change its behavior based on its internal state, like a traffic light changing from red to green.
   - **Example**: A vending machine that behaves differently based on whether it's out of stock, has money inserted, or is dispensing an item.

9. **Strategy Pattern**
   - Defines a family of algorithms and allows the algorithm to be selected at runtime.
   - **Use case**: When you need to choose between different algorithms or behaviors based on context.
   - **Example**: A sorting strategy where you can choose between quicksort, mergesort, or bubblesort depending on the context.

10. **Template Method Pattern**
   - Defines the structure of an algorithm, deferring some steps to subclasses.
   - **Use case**: When you want to define a fixed sequence of steps and allow subclasses to provide their own implementation for certain steps.
   - **Example**: A base class that defines the skeleton of an algorithm, and subclasses implement specific steps like data reading or processing.

11. **Visitor Pattern**
   - Allows you to define new operations on an object structure without changing the classes of the elements on which it operates.
   - **Use case**: When you need to perform operations on objects of various classes in a structure (like an abstract syntax tree).
   - **Example**: A visitor that adds up all values in a tree structure, or performs some type of transformation on elements of a hierarchy.

---
