# UDM Party Assignment

## Activity 1: Web Tools - Entity Engine

### 1. Creating a person named Hardik2 Chouhan2
![Step 1-1](Images/A1S1.png)

![Step 1-2](Images/A3S1-2.png)

### 2. Assign Customer role to created person
![Step 2](Images/A1S2.png)

![Step 2](Images/A1S2-2.png)

### 3. Change name to Singh2
![Step 3](Images/A1S3.png)

![Step 3](Images/A1S3-2.png)

![Step 3](Images/A1S3-3.png)

### 4. Add Email
![Step 4](Images/A1S4.png)

![Step 4](Images/A1S4-2.png)

![Step 4](Images/A1S4-3.png)

### 5. Add billing phone number to customer profile
![Step 5](Images/A1S5.png)

![Step 5](Images/A1S5-2.png)

### 6. Add Shipping phone to customer
![Step 6](Images/A1S6.png)

![Step 6](Images/A1S6-2.png)

### 7. Add Shipping Address to Customer
![Step 7](Images/A1S7.png)

![Step 7](Images/A1S7-2.png)

![Step 7](Images/A1S7-3.png)

![Step 7](Images/A1S7-4.png)

![Step 7](Images/A1S7-5.png)

### 8. Add Billing Address to Customer
![Step 8](Images/A1S8.png)

![Step 8](Images/A1S8-2.png)

![Step 8](Images/A1S8-3.png)

![Step 8](Images/A1S8-4.png)

![Step 8](Images/A1S8-5.png)

### 9. Make Billing and shipping address same (Deleting the billing address first or can update and make the billing address same)
![Step 9](Images/A1S9.png)

![Step 9](Images/A1S9-2.png)

![Step 9](Images/A1S9-3.png)

### 10. Change purpose of billing to general correspondence address
![Step 10](Images/A1S10.png)

### 11. Delete (Expire) email address and create new
### 12. Delete (Expire) billing address and add new
### 13. Delete (Expire) shipping address and add new
![Step 13](Images/A1S13.png)

## Activity 2: Party Manager (Create option)

1. Open [Party Manager](https://ofbiztraining.hotwaxsystems.com/partymgr/), click on 'party' and then use the 'Create Party' option.

### 1. Creating a person named Hardik Chouhan
![Step 2-1](Images/A2S1.png)

### 2. Create Customer role to Hardik Chouhan [10011] by clicking on roles
![Step 2-2](Images/A2S2.png)

### 3. Changing name to Hardik Singh Chouhan
![Step 2-3](Images/A2S3.png)

### 4. Add email address to customer profile: as contact info
![Step 2-4](Images/A2S4.png)

![Step 2-4](Images/A2S4-2.png)

### 5. Adding billing phone number (Purpose: billing)
![Step 2-5](Images/A2S5.png)

purpose billing

![Step 2-5](Images/A2S5-2.png)

### 6. Adding Shipping address to customer profile
![Step 2-6](Images/A2S6.png)

![Step 2-6](Images/A2S6-2.png)

### 7. Add Billing Address to customer profile
![Step 2-7](Images/A2S7.png)

![Step 2-7](Images/A2S7-2.png)

![Step 2-7](Images/A2S7-3.png)

### 8. Make billing & Shipping Address same: Expire the billing address updated by augmenting the billing address purpose to shipping address as well
![Step 2-8](Images/A2S8.png)

![Step 2-8](Images/A2S8-2.png)

![Step 2-8](Images/A2S8-3.png)

### 9. Change the purpose of billing address to General correspondence address
![Step 2-9](Images/A2S9.png)

![Step 2-9](Images/A2S9-2.png)

![Step 2-9](Images/A2S9-3.png)

### 10. Delete (Expire) email address and create new
![Step 2-10](Images/A2S10.png)

### 11. Expire billing address & create new
![Step 2-11](Images/A2S11.png)

![Step 2-11](Images/A2S11-2.png)

![Step 2-11](Images/A2S11-3.png)

![Step 2-11](Images/A2S11-4.png)

## Activity 3: Using XML - Import/Export

1. Inside Import/Export option, open XML Data Import.
![Step 3-0](Images/A3S0.png)

### 1. Creating a Party
```xml
<Party partyType="PERSON" partyId="666666"/>
```
![Step 3-1](Images/A3S1.png)

### 2. Making person named New Man
```xml
<Person firstName="New" lastName="Man" partyId="666666"/>
```
![Step 3-1](Images/A3S2.png)

### 3. Assign customer role to created person New Man
```xml
<PartyRole partyId="666666" roleTypeId="CUSTOMER"/>
```
![Step 3-1](Images/A3S3.png)

validating
![Step 3-1](Images/A3S3-2.png)

![Step 3-1](Images/A3S3-3.png)

### 4. Changing name of person named New Yellow Man
```xml
<Person middleName="Yellow" partyId="666666"/>
```
![Step 3-1](Images/A3S4.png)

![Step 3-1](Images/A3S4-2.png)

![Step 3-1](Images/A3S4-3.png)

### 5. Adding Email Address to Customer profile
```xml
<ContactMech contactMechId="CM4001" contactMechTypeId="EMAIL_ADDRESS" infoString="alex@gmail.com"/>
<PartyContactMech allowSolicitation="Y" contactMechId="CM4001" fromDate="2024-06-26" partyId="666666"/>
```
![Step 3-1](Images/A3S5.png)

![Step 3-1](Images/A3S4-2.png)

### 6. Add Billing phone to customer profile
```xml
<ContactMech contactMechId="CM4002" contactMechTypeId="TELECOM_NUMBER"/>
<PartyContactMechPurpose contactMechId="CM4002" contactMechPurposeTypeId="PHONE_BILLING" fromDate="2024-06-26" partyId="666666"/>
<TelecomNumber areaCode="801" contactMechId="CM4002" contactNumber="9898989898"/>
<PartyContactMech allowSolicitation="Y" contactMechId="CM4002" fromDate="2024-06-26 00:00:00.0" partyId="666666"/>
```
![Step 3-1](Images/A3S6.png)

![Step 3-1](Images/A3S6-2.png)

![Step 3-1](Images/A3S6-3.png)

![Step 3-1](Images/A3S6-4.png)

### 7. Add Shipping phone to customer profile
```xml
<ContactMech contactMechId="CM4003" contactMechTypeId="TELECOM_NUMBER"/>
<PartyContactMechPurpose contactMechId="CM4003" contactMechPurposeTypeId="PHONE_SHIPPING" fromDate="2024-06-26 00:00:00.0" partyId="666666"/>
<TelecomNumber areaCode="801" contactMechId="CM4003" contactNumber="7676767676"/>
<PartyContactMech allowSolicitation="Y" contactMechId="CM4003" fromDate="2024-06-26 00:00:00.0" partyId="666666"/>
```
![Step 3-1](Images/A3S7.png)

![Step 3-1](Images/A3S7-2.png)

![Step 3-1](Images/A3S7-3.png)

![Step 3-1](Images/A3S7-4.png)

validating data
![Step 3-1](Images/A3S7-5.png)

### 8. Add Shipping Address to customer profile
### 9. Add Billing Address to customer profile
### 10. Delete (Expire) email address and create new
```xml
<PartyContactMechPurpose contactMechId="CM4001" partyId="666666" thruDate="2024-06-26 01:00:00.0"/>
<ContactMech contactMechId="CM4004" contactMechTypeId="EMAIL_ADDRESS" infoString="newemail@example.com"/>
<PartyContactMech allowSolicitation="Y" contactMechId="CM4004" fromDate="2024-06-26" partyId="666666"/>
```
![Step 3-1](Images/A3S10.png)

### 11. Delete (Expire) billing address and add new