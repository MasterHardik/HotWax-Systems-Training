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
height: auto;
margin: 0 auto;
}
input[type="text"], textarea {
padding: 10px;
margin-bottom: 10px;
font-size: 16px;
width: 100%;
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
}
        input[type="submit"]:hover {
background-color: #0056b3;
}
.fplb {
font-size: 1rem;
font-weight: bold;
margin: 10px;
text-align: center;
}
.basic-form {
width: 100%;
}
.basic-table {
width: 100%;
}
.label {
font-size: 1rem;
font-weight: bold;
}


.inp_area{
    display:flex;
    justify-content:center;
    align-item:center;
}
</style>
</head>
<body>

<h1>Upload File</h1>


<!-- Form to upload XML file -->
<form action="<@ofbizUrl>/processXMLService</@ofbizUrl>" method="post" enctype="multipart/form-data">
    <div class="inp_area"><input type="file" name="fileName" /></div>
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
