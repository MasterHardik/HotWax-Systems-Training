# Shopify API Documentation

## Authentication

All REST Admin API queries require a valid Shopify access token.

- **Token Generation**: 
  - Public and custom apps created in the Partner Dashboard generate tokens using OAuth.
  - Custom apps made in the Shopify admin are authenticated within the Shopify admin.
- **Client Libraries**: 
  - To simplify the authentication process, use one of the recommended Shopify client libraries.
- **Header**: 
  - Include your token as a `X-Shopify-Access-Token` header on all API queries.

### Access Scopes
To maintain security, apps must request specific access scopes during the installation process. Only request the data access necessary for your app's functionality.

Learn more about getting started with [authentication and building apps](https://shopify.dev/api/admin-rest).

## Endpoints and Requests

Admin REST API endpoints are organized by resource type. Different endpoints are used based on your app’s requirements.

### Endpoint Structure
All Admin REST API endpoints follow this pattern:

```mathematica
https://{store_name}.myshopify.com/admin/api/2024-10/{resource}.json
```

### Example Requests

#### POST - Create a New Product
Create a new product using the Product resource:
**POST** */admin/api/2024-10/products.json*

```mathematica
Replace `{store_name}` with your store’s domain and `{access_token}` with your generated access token.
```
#### GET - Retrieve a Single Product
Retrieve information for a single product:
**GET** */admin/api/2024-10/products/{product_id}.json*

```mathematica
Replace `{product_id}` with the ID of the product you want to retrieve.
```

#### PUT - Update a Product
Update a product using the Product resource:
**PUT** */admin/api/2024-10/products/{product_id}.json*

```mathematica
Replace `{product_id}` with the ID of the product you want to update.
```

#### DELETE - Delete a Product
Delete a product using the Product resource:
**DELETE** */admin/api/2024-10/products/{product_id}.json*

```mathematica
Replace `{product_id}` with the ID of the product you want to delete.
```

### API Versioning
The Admin API is versioned, with new releases occurring four times per year. Ensure you specify a supported version in the URL.

All REST endpoints support cursor-based pagination. All requests produce HTTP response status codes.

## Rate Limits

The REST Admin API has the following rate limits:

- **Limit**: 40 requests per app per store per minute.
- **Replenishment Rate**: 2 requests per second.
- **Shopify Plus Stores**: Rate limit increased by a factor of 10.

### Usage Limitations
- If the limit is exceeded, the API will return a `429 Too Many Requests` error.
- All REST API responses include the `X-Shopify-Shop-Api-Call-Limit` header, indicating the number of requests made and the total allowed per minute.
- A `429` response will also include a `Retry-After` header, indicating the number of seconds to wait before retrying the request.

---
For more details on the Shopify API, refer to the official [Shopify API documentation](https://shopify.dev/api).