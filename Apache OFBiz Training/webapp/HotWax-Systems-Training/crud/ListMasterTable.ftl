<div class="screenlet-body">
        <h2>====== Hello from <i>ListMasterTable.ftl</i> para ======</h2>
  <#if masterTable?has_content>
    <table cellspacing=0 cellpadding=2 border=0 class="basic-table">
      <thead><tr>
        <th>${uiLabelMap.masterTableId}</th>
        <th>${uiLabelMap.MasterTableType}</th>
        <th>${uiLabelMap.MasterTableFirstName}</th>
        <th>${uiLabelMap.MasterTableLastName}</th>
        <th>${uiLabelMap.MasterTableComment}</th>
      </tr></thead>
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
  </#if>
</div>