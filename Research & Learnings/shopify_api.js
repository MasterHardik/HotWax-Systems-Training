let request =require('request');

let apiKey = ''
let apiPassToken = ''
let endpoint ='products'


let options ={
    'method':'GET',
    'url':`https://${apiKey}:${apiPassToken}@${storeName}.com/admin/[2022-10 version]/${endpoint}.json:`,
    'headers':{
        'Content-type':'application/json'
    }
}


request(options,function(error,response){
    if(error)throw new Error(error)
        console.log(response.body);
})

// ******** Shopify API URL Format ***********

// for products after giving permission from shopify create app and api config
    // https://apiKey:apiToken@storeName.com/admin/[2022-10 version]/products.json
// for orders 
    // https://apiKey:apiToken@storeName.com/admin/[2022-10 version]/orders.json
// for customers 
    // https://apiKey:apiToken@storeName.com/admin/[2022-10 version]/customers.json


// ******************************************


// npm i request <-- to install request package
