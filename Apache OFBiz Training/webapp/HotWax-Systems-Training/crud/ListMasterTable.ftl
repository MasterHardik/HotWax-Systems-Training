<div>
    <table class="table calendar hover-bar" cellpadding=2 cellspacing=1 width=100%>
        <thead cellpadding=3 cellspacing=3>
                <th>${uiLabelMap.masterTableId}</th>
                <th>${uiLabelMap.masterTableType}</th>
                <th>${uiLabelMap.masterTableFirstName}</th>
                <th>${uiLabelMap.masterTableLastName}</th>
                <th>${uiLabelMap.masterTableComment}</th>
        </thead>
        <tbody>
            <#list masterTableList as masterTable>
                <tr>
                    <td>${masterTable.masterTableId}</td>
                    <td>${masterTable.getRelatedOne("masterTableType").get("description", locale)}</td>
                    <td>${masterTable.firstName?default("NA")}</td>
                    <td>${masterTable.lastName?default("NA")}</td>
                    <td>${masterTable.comments!}</td>
                </tr>
            </#list>
        </tbody>
    </table>
</div>