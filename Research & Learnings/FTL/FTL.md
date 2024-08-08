# FreeMarker Template Language (FTL)

FreeMarker Template Language (FTL) is a template engine used for generating text outputs based on templates and data models, commonly used in Java-based web applications. While HTML is a markup language used for structuring web pages, FTL introduces several additional features and concepts to enhance templating and dynamic content generation.

Here’s a list of features and concepts that are specific to FTL and not present in HTML:

## 1. Variables and Expressions

- **Variable Declaration**: Define variables using the `#assign` directive.

```ftl
<#assign myVar = "Hello World">
```

- **Expression Evaluation**: Evaluate expressions directly in the template.
```ftl
${myVar}
```
## 2. Control Structures

- **Conditionals**: Use #if, #elseif, and #else for conditional logic.

```ftl
<#if condition>
  ...
<#elseif anotherCondition>
  ...
<#else>
  ...
</#if>
```

- **Loops**: Iterate over collections using #list.
```ftl
<#list items as item>
  ${item}
</#list>
```

## 3. Macros

- **Define Macros**: Create reusable blocks of code with `#macro`.

```ftl
<#macro myMacro param>
  ...
</#macro>
```
- **Invoke Macros**: Call macros with specific parameters.

```ftl
<@myMacro param="value"/>
```

## 4. Directives

- **Include**: Include other templates with #include.

```ftl
<#include "header.ftl">
```

- **Import**: Import macros or functions with #import.

```ftl
<#import "macros.ftl" as myMacros>
```

## 5. Comments

- **Inline Comments**: Use `<#--` for comments.

```ftl
<#-- This is a comment -->
```

## 6. Text and Data Formatting

- **Date Formatting**: Format dates using the ?string built-in.

```ftl
${date?string("yyyy-MM-dd")}
```

- **Number Formatting**: Format numbers with ?string.

```ftl
${number?string("0.00")}
```

## 7. Built-in Functions

- **String Operations**: Functions like ?upper_case, ?lower_case.

```ftl
${string?upper_case}
```

- **Collection Operations**: Functions like ?size, ?sort.

```ftl
${myList?size}
```

- **Check if Exists**: Use ?if_exists to check if a variable exists.

```ftl
${myVar?if_exists}
```
- **Check for Content**: Use ?has_content to check if a variable or expression has content.

```ftl
${myVar?has_content}
```

- **Default Values**: Use default to provide a default value if the variable is null or undefined.

```ftl
${myVar!""}
```

## 8. Template Inheritance

**Extend**: Use #ftl and #include for template inheritance and composition.

```ftl
<#ftl extends="base.ftl">
```

## 9. Custom Tags and Functions

- **Define Custom Tags**: Extend FTL with custom tags.

```ftl
<#tag customTag>
  ...
</#tag>
```

- **Define Custom Functions**: Create and use custom functions in templates.

## 10. Escaping and Encoding

**Auto-escaping**: Handle automatic escaping of HTML to prevent XSS attacks.

```ftl
${htmlContent}
```

## 11. Error Handling

- **Exception Handling**: Catch and handle errors with #attempt and #recover.

```ftl
<#attempt>
  ...
<#recover>
  ...
</#attempt>
```

## 12. Data Model Integration

- **Access Data Model**: Integrate with Java data models, accessing fields and methods directly.

```ftl
${user.name}
```

These features make FTL a powerful templating engine for creating dynamic web content, extending beyond the static nature of HTML by allowing for complex logic, reusable components, and data-driven output.

## Example FTL Template

```ftl
<#-- Define a macro to display user information -->
<#macro displayUserInfo user>
    <#if user?has_content>
        <h2>User Information</h2>
        <p>Name: ${user.name!""}</p>
        <p>Email: ${user.email?default("No email provided")}</p>
        <p>Age: ${user.age?if_exists?default("Age not specified")}</p>
    <#else>
        <p>No user information available.</p>
    </#if>
</#macro>

<#-- Define a list of users for demonstration -->
<#assign users = [
    { "name": "John Doe", "email": "john@example.com", "age": 30 },
    { "name": "Jane Smith", "email": "jane@example.com" },
    {}
]>

<!DOCTYPE html>
<html>
<head>
    <title>User List</title>
</head>
<body>
    <h1>User List</h1>
    <#-- Loop through the list of users and display information using the macro -->
    <#list users as user>
        <@displayUserInfo user=user/>
        <hr>
    </#list>
</body>
</html>
```

## Explanation

- **Macro Definition:**

```ftl
<#macro displayUserInfo user>
```

The `displayUserInfo` macro is defined to handle displaying user information.

- **Conditional Checks**:

```ftl
<#if user?has_content>
```

This check determines if the `user` object has content.

- **Default Values**:

```ftl
${user.email?default("No email provided")}
```

Provides a default value if `user.email` is missing or `null`.

- **`?if_exists` Check**:

```ftl
${user.age?if_exists?default("Age not specified")}
```
Checks if `user.age` exists and provides a default value if it doesn’t.

- **Looping Through Users**:

```ftl
<#list users as user>
```

Loops through a list of user objects and applies the displayUserInfo macro.

This FTL template integrates various FreeMarker features to dynamically generate HTML content based on the presence and content of data fields, providing default values where necessary.



