<!DOCTYPE html>
<html>
<head>
<title>Upload File or XML for Parsing</title>
<style>
h1 {
text-align: center;
margin: 1rem;
}
form {
display: flex;
justify-content: center;
flex-direction: column;
width: 400px;
margin: 0 auto;
background-color: #fff;
padding: 2rem;
border-radius: 0.5rem;
box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

input[type="text"],
        input[type="file"],
        textarea {
padding: 10px;
margin-bottom: 1rem;
font-size: 16px;
width: 100%;
border: 1px solid #ccc;
border-radius: 0.25rem;
transition: border-color 0.3s;
}

        input[type="text"]:focus,
        input[type="file"]:focus {
border-color: #007BFF;
outline: none;
}

        input[type="submit"] {
padding: 10px;
background-color: #007BFF;
color: white;
border: none;
cursor: pointer;
font-size: 16px;
border-radius: 0.5rem;
box-shadow: rgba(0, 0, 0, 0.16) 0px 1px 4px;
transition: background-color 0.3s;
}

        input[type="submit"]:hover {
background-color: #0056b3;
}

.inp_area {
display: flex;
flex-direction: column;
margin-bottom: 1rem;
}

.inp_area label {
margin-bottom: 0.5rem;
font-weight: bold;
color: #555;
}

.messages {
margin-top: 2rem;
background-color: #f8d7da;
color: #721c24;
padding: 1rem;
border-radius: 0.5rem;
border: 1px solid #f5c6cb;
}

.messages h3 {
margin-top: 0;
}

ul {
list-style-type: none;
padding-left: 0;
}

li {
margin-bottom: 0.5rem;
}
</style>
</head>
<body>

<h1>Upload File</h1>


<!-- Form to upload XML file -->
<form action="<@ofbizUrl>/processXMLService</@ofbizUrl>" method="post" enctype="multipart/form-data">
    <div class="inp_area">
        <label for="filePath">File Path:</label>
        <input type="text" name="filePath" id="filePath"/>
    </div>
    <div class="inp_area">
        <label for="fileName">Import File:</label>
        <input type="file" name="fileName" id="fileName"/>
    </div>
    <input type="submit" value="Upload"/>
</form>

<#-- Display messages after form submission -->
<#if messages?has_content>
    <div>
        <h3>Messages</h3>
        <ul>
            <#list messages as message>
                <li>${message}</li>
            </#list>
        </ul>
    </div>
</#if>

</body>
</html>
