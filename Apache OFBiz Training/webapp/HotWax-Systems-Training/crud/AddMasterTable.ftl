<div class="screenlet-body">
  <form id="createMasterTableEvent" method="post" action="<@ofbizUrl>createMasterTableEvent</@ofbizUrl>">
    <input type="hidden" name="addMasterTableFromFtl" value="Y"/>
    <fieldset>
      <div>
        <span class="label">${uiLabelMap.masterTableType}</span>
        <select name="masterTableTypeId" class='required'>
          <#list masterTableTypes as demoType>
            <option value='${demoType.masterTableTypeId}'>${demoType.description}</option>
          </#list>
        </select>*
      </div>
      <div>
        <span class="label">${uiLabelMap.masterTableFirstName}</span>
        <input type="text" name="firstName" id="firstName" class='required' maxlength="20" />*
      </div>
      <div>
        <span class="label">${uiLabelMap.masterTableLastName}</span>
        <input type="text" name="lastName" id="lastName" class='required' maxlength="20" />*
      </div>
      <div>
        <span class="label">${uiLabelMap.masterTableComment}</span>
        <input type="text" name="comments" id="comments" class='inputBox' size="60" maxlength="255" />
      </div>
    </fieldset>
    <input type="submit" value="${uiLabelMap.CommonAdd}" />
  </form>
</div>