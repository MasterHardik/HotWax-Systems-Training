<#html>
<#--  Variables and Expressions  -->
<#assign tempVar = "TempValue>value</#assign>
<div>I have used the div here : ${tempVar}</div>


<#--  Control Structures  -->
<#if (tempVar)>
    <div>Hi i am in Control Structures</div>
</#if>

<#list collection as element>
    ${element}
</#list>

<@macro>myMacro Param</macro>

<@myMacro param="value"/>

<#include "header.footer.ftl"/>
<#import "macros.ftl" as alias/>


${string?upper_case}
${myList?size}
${myVar?if_exist}
${myVar?hasContent}
${myVar!""}

<#ftl extends="base.ftl">
<#tag customTag><#


<#/html>